(ns util.blockchain-csv-sync
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:import [com.google.bitcoin.utils BlockFileLoader]
           [com.google.bitcoin.core Block Transaction TransactionInput TransactionOutput]
           [com.google.bitcoin.params MainNetParams]))

(defn- main-chain []
  (seq (BlockFileLoader. (MainNetParams.)
                         (BlockFileLoader/getReferenceClientBlockFileList))))

(defn- extract-info-tx-input
  [^TransactionInput in]
  (let [outpoint (.getOutpoint in)
        hash (-> outpoint (.getHash) (.toString))
        index (.getIndex outpoint)]
    (when (not (.isCoinBase in)) (str index ":" hash))))

(defn gen-csv!
  [& {:keys [:folder] :or {:folder ""}}]
  (with-open [blks-file (io/writer (str folder "blks.csv"))
              txs-file (io/writer (str folder "txs.csv"))]
    (.write blks-file "hash,date,prevhash\n")
    (.write txs-file "hash,outputs,blkhash,inputs\n")
    (doseq [^Block blk (main-chain)]
      (let [blkhash (.getHashAsString blk)]
        (.write blks-file (str blkhash "," (.getTime (.getTime blk)) "," (.toString (.getPrevBlockHash blk)) "\n"))
        (doseq [^Transaction tx (.getTransactions blk)]
          (.write txs-file (.getHashAsString tx))
          (.write txs-file ",\"")
          (.write txs-file (clojure.string/join "," (map (fn [ix ^TransactionOutput val] (str ix ":" (.getValue val)))
                                                         (range) (.getOutputs tx))))
          (.write txs-file "\",")
          (.write txs-file blkhash)
          (.write txs-file ",\"")
          (.write txs-file (clojure.string/join "," (map extract-info-tx-input (.getInputs tx))))
          (.write txs-file "\"\n"))))))