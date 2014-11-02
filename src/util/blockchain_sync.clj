(ns util.blockchain-sync
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrel]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [clojurewerkz.neocons.rest.index :as ni]
            [clojurewerkz.neocons.rest.constraints :as nc]

            [taoensso.timbre :as timbre])
  (:import (com.google.bitcoin.utils BlockFileLoader)
           (com.google.bitcoin.core Block NetworkParameters Transaction TransactionInput)
           (com.google.bitcoin.params MainNetParams))
  (:gen-class))

(timbre/refer-timbre)

(defn- main-chain
  []
  (seq (BlockFileLoader. (MainNetParams.)
                         (BlockFileLoader/getReferenceClientBlockFileList))))

(defn- extract-info-tx-input
  [^TransactionInput in]
  (let [outpoint (.getOutpoint in)
        hash (-> outpoint (.getHash) (.toString))
        index (.getIndex outpoint)]
    (if (.isCoinBase in)
      "coinbase"
      (str index ":" hash))))

(defn- extract-info-tx
  [^Transaction tx blk-hash]
  {:hash    (.getHashAsString tx)
   :blkhash blk-hash
   :type    "tx"
   :inputs  (vec (map extract-info-tx-input (.getInputs tx)))
   :outputs (map #(str %1 ":" (.getValue %2)) (range) (.getOutputs tx))})

(defn- extract-info-blk
  [^Block blk]
  (let [hash (.getHashAsString blk)
        prev-hash (-> blk .getPrevBlockHash .toString)
        date (.getTime blk)

        txs (map #(extract-info-tx % hash) (.getTransactions blk))]
    [{:hash hash :date date :prevhash prev-hash :type "block"}
     txs]))

(defn- set-tx-labels
  [conn]
  (cy/tquery conn "MATCH (tx {type: 'tx'}) SET tx:TX, tx.type = NULL RETURN COUNT(tx);"))

(defn- create-tx-indexes
  [conn]
  (cy/tquery conn "CREATE INDEX ON :TX(hash)")
  ;(nc/create-unique conn "TX" :hash) Created problems when chainforking happens
  )

(defn- set-blk-labels
  [conn]
  (cy/tquery conn "MATCH (blk {type: 'block'}) SET blk:BLOCK, blk.type = NULL RETURN COUNT(blk);"))

(defn- create-blk-indexes
  [conn]
  (nc/create-unique conn "BLOCK" :hash))

(defn- connect-blocks
  [conn]
  ;first remove the prevhash for the genesis block
  (cy/tquery conn (str "MATCH (genesis:BLOCK)
                        WHERE HAS (genesis.prevhash)
                            AND id(genesis) = 0
                            AND replace(genesis.prevhash, '0', '') = ''
                        SET genesis.prevhash = NULL
                        RETURN COUNT(genesis);"))

  ;connect the remaining blocks
  (cy/tquery conn (str "MATCH (blk:BLOCK)
                        WHERE HAS (blk.prevhash)
                            MATCH (prevblk:BLOCK {hash: blk.prevhash})
                            CREATE (prevblk)-[r:NEXT_BLOCK]->(blk)
                            SET blk.prevhash = NULL
                        RETURN COUNT(blk);")))

(defn- connect-tx-to-blocks
  [conn]
  (cy/tquery conn (str "MATCH (tx:TX)
                        WHERE HAS (tx.blkhash)
                            MATCH (blk:BLOCK {hash: tx.blkhash})
                            CREATE (blk)-[r:CONTAINS]->(tx)
                            SET tx.blkhash = NULL
                        RETURN COUNT(tx);")))

(defn- remove-coinbase-inputs
  [conn]
  (cy/tquery conn (str "MATCH (cb:TX {inputs: ['coinbase']})
                        SET cb.inputs = NULL
                        RETURN COUNT(cb);")))

(defn- create-tx-outputs
  [conn]
  (cy/tquery conn (str "MATCH (tx:TX)
                        WHERE HAS (tx.outputs)
                        WITH tx, [output IN tx.outputs | SPLIT(output, ':')] AS outs
                          FOREACH(output IN outs |
                            CREATE (tx)-[r:OUTPUTS]->(out:TXOUT {index: TOINT(output[0]), value: TOINT(output[1])}))
                        SET tx.outputs = NULL
                        RETURN COUNT(tx);")))

(defn- connect-tx-inputs
  [conn]
  (cy/tquery conn (str "MATCH (tx:TX)
                        WHERE HAS (tx.hash)
                        WITH tx
                        UNWIND tx.inputs AS input
                        WITH tx, SPLIT(input, ':') AS input
                        MATCH (txin:TX {hash: input[1]})-[r:OUTPUTS]->(txout:TXOUT {index: TOINT(input[0])})
                        CREATE (txout)-[:INPUTS]->(tx)
                        RETURN COUNT(tx);"))
  (cy/tquery conn (str "MATCH (tx:TX)
                        WHERE HAS (tx.hash)
                        SET tx.inputs = NULL
                        RETURN COUNT(tx);")))

(defn- connect-txs
  [conn]
  (doto conn (create-tx-outputs) (connect-tx-inputs)))

(defn- process-partition
  "Partition is a collection of blocks (a partition from the main-chain)"
  [conn blk-partition]
  (let [blk-seq (map extract-info-blk blk-partition)
        blks-only (map first blk-seq)
        _ (println (str "Processing partition starting with " (:hash (first blks-only))))

        txs-only (flatten (map second blk-seq))]
    (doto conn
      (nn/create-batch blks-only)
      (nn/create-batch txs-only)
      (set-blk-labels)
      (set-tx-labels)
      (connect-blocks)
      (connect-tx-to-blocks)
      (remove-coinbase-inputs)
      (connect-txs))))

(defn chain-sync
  [conn]
  (let [ _ (println "Creating indexes")
         _ (doto conn
             (create-blk-indexes)
             (create-tx-indexes))

         partitioned (partition-all 50 (main-chain))
        _ (doall (map #(process-partition conn %) partitioned))]
    {}))

(defn -main
  [& args]
  (chain-sync (nr/connect (first args))))