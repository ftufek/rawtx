(ns util.blockchain-sync
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.batch :as nb]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [clojurewerkz.neocons.rest.constraints :as nc])
  (:import [com.google.bitcoin.utils BlockFileLoader]
           [com.google.bitcoin.core Block NetworkParameters Transaction TransactionInput TransactionOutput]
           [com.google.bitcoin.params MainNetParams]
           [clojurewerkz.neocons.rest Connection Neo4JEndpoint])
  (:gen-class))

(defn- main-chain
  []
  (seq (BlockFileLoader. (MainNetParams.)
                         (BlockFileLoader/getReferenceClientBlockFileList))))

(def uniqid (atom 0))
(defn- get-uniqid!
  []
  (swap! uniqid inc))

(defn- extract-info-tx-input
       [^TransactionInput in]
       (let [outpoint (.getOutpoint in)
             hash (-> outpoint (.getHash) (.toString))
             index (.getIndex outpoint)]
         (when (not (.isCoinBase in)) (str index ":" hash))))

(defn- batch-tx-out-node
  [tx-id ix ^TransactionOutput txout]
  (let [txout-id (get-uniqid!)]
    [{:method "POST"
      :to "/node"
      :body {:index ix :value (.getValue txout)}
      :id txout-id}
     {:method "POST"
      :to (str "{" txout-id "}/labels")
      :body "TXOUT"}
     {:method "POST"
      :to (str "{" tx-id "}/relationships")
      :body {:to (str "{" txout-id "}") :type "OUTPUTS"}}]))

(defn- batch-tx-node
  [blk-id ^Transaction tx]
  (let [tx-id (get-uniqid!)
        txout-ops (mapcat #(batch-tx-out-node tx-id %1 %2) (range) (.getOutputs tx))]
    (into [{:method "POST"
            :to "/node"
            :body {:hash (.getHashAsString tx)
                   :txinputs (clojure.string/join "," (map extract-info-tx-input (.getInputs tx)))}
            :id tx-id}
           {:method "POST"
            :to (str "{" tx-id "}/labels")
            :body "TX"}
           {:method "POST"
            :to (str "{" blk-id "}/relationships")
            :body {:to (str "{" tx-id "}") :type "CONTAINS"}}] txout-ops)))

(defn- partition-ops
  [^Connection conn blk-seq]
  (for [^Block blk blk-seq]
    (let [blk-id (get-uniqid!)
          tx-ops (mapcat #(batch-tx-node blk-id %) (.getTransactions blk))]
      (into [{:method "POST"
              :to "/node"
              :body {:hash (.getHashAsString blk)
                     :date (.getTime (.getTime blk))
                     :prevblkhash (-> blk .getPrevBlockHash .toString)}
              :id blk-id}
             {:method "POST"
              :to (str "{" blk-id "}/labels")
              :body "BLOCK"}] tx-ops))))

(defn- setup-db
  [^Connection conn]
  (cy/tquery conn "CREATE (genesis:BLOCK {hash: \"0000000000000000000000000000000000000000000000000000000000000000\"});"))

(defn- setup-indexes
  [^Connection conn]
  (println "Creating indexes")
  (nc/create-unique conn "BLOCK" :hash)
  (cy/tquery conn "CREATE INDEX ON :TX(hash)"))

(defn- connect-tx-inputs
  [conn]
  (cy/tquery conn (str "MATCH (tx:TX)
                        WHERE HAS (tx.inputs)
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

(defn chain-sync
  [conn]
  (setup-db conn)
  (doseq [ops (map #(partition-ops conn %) (partition-all 4 (main-chain)))]
    (nb/perform conn ops))
  ;(setup-indexes conn)
  )

(defn -main
  [& args]
  (chain-sync (nr/connect (first args))))