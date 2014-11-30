(ns util.blockchain-csv-sync
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:import [com.google.bitcoin.utils BlockFileLoader]
           [com.google.bitcoin.core Block NetworkParameters Transaction TransactionInput TransactionOutput]
           [com.google.bitcoin.params MainNetParams]
           [clojurewerkz.neocons.rest Connection Neo4JEndpoint]))

(defn- main-chain
  []
  (seq (BlockFileLoader. (MainNetParams.)
                         (BlockFileLoader/getReferenceClientBlockFileList))))

(def headers {:blk-nodes ["hash" "date"]
              :blk-rels ["prevhash" "hash"]
              :tx-nodes ["hash" "outputs"]
              :tx-rels ["hash" "blkhash" "inputs"]})

(defn- blk-node
  [^Block blk]
  [(.getHashAsString blk) (.getTime (.getTime blk))])

(defn- blk-rel
  [^Block blk]
  [(.toString (.getPrevBlockHash blk)) (.getHashAsString blk)])

(defn- tx-node
  [^Transaction tx]
  [(.getHashAsString tx) (clojure.string/join "," (map (fn [ix ^TransactionOutput val] (str ix ":" (.getValue val)))
                                                       (range) (.getOutputs tx)))])

(defn- extract-info-tx-input
  [^TransactionInput in]
  (let [outpoint (.getOutpoint in)
        hash (-> outpoint (.getHash) (.toString))
        index (.getIndex outpoint)]
    (when (not (.isCoinBase in)) (str index ":" hash))))

(defn- tx-rel
  [^Transaction tx blkhash]
  [(.getHashAsString tx) blkhash (clojure.string/join "," (map extract-info-tx-input (.getInputs tx)))])

(defn gen-csv!
  [& {:keys [:folder] :or {:folder ""}}]
  (with-open [blk-nodes-file (io/writer (str folder "blk-nodes.csv"))
              blk-rels-file (io/writer (str folder "blk-rels.csv"))

              tx-nodes-file (io/writer (str folder "tx-nodes.csv"))
              tx-rels-file (io/writer (str folder "tx-rels.csv"))]
    (csv/write-csv blk-nodes-file [(headers :blk-nodes)])
    (csv/write-csv blk-rels-file [(headers :blk-rels)])
    (csv/write-csv tx-nodes-file [(headers :tx-nodes)])
    (csv/write-csv tx-rels-file [(headers :tx-rels)])
    (doseq [^Block blk (take 80000 (main-chain))]
      (csv/write-csv blk-nodes-file [(blk-node blk)])
      (csv/write-csv blk-rels-file [(blk-rel blk)])
      (csv/write-csv tx-nodes-file (map tx-node (.getTransactions blk)))
      (csv/write-csv tx-rels-file (map #(tx-rel %1 (.getHashAsString blk)) (.getTransactions blk))))))