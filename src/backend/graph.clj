(ns backend.graph
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrel]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [clojurewerkz.neocons.rest.index :as ni]
            [clojurewerkz.neocons.rest.constraints :as nc]

            [compojure.core :refer :all]
            [ring.util.response :refer [response resource-response]]))

(def db-url "http://localhost:7474/db/data/")
(def conn (nr/connect db-url))

(def cqueries
  {:get-num-blk-indexed "MATCH (blk:BLOCK) RETURN COUNT(blk)"
   :get-num-tx-indexed "MATCH (tx:TX) RETURN COUNT(tx)"
   :get-inputs (str "MATCH (txin:TX)-[r1:OUTPUTS]->(details:TXOUT)-[r2:INPUTS]->(tx:TX)
                     WHERE tx.hash = {hash}
                     RETURN txin, details;")
   :get-until-coinbase (str "MATCH (in:TX)-[:OUTPUTS]->(details:TXOUT)-[:INPUTS]->(txint:TX)-[:INPUTS|OUTPUTS*]->(tx:TX)
                             WHERE tx.hash = {hash}
                             RETURN DISTINCT(ID(details)), in, details, txint AS tx
                             UNION ALL
                             MATCH (in:TX)-[r1:OUTPUTS]->(details:TXOUT)-[r2:INPUTS]->(tx:TX)
                             WHERE tx.hash = {hash}
                             RETURN DISTINCT(ID(details)), in, details, tx;")})

(defmacro gen-cqueries-fn
  [datamap]
  (cons `do
        (for [[qname qval] (eval datamap)]
          `(defn ~(symbol (name qname))
             ([conn# params#]
                (cy/tquery conn# ~qval params#))
             ([conn#]
                (cy/tquery conn# ~qval))))))

(gen-cqueries-fn cqueries)

(defn get-inputs-ex
  [conn params]
  (let [res (get-inputs conn params)
        extracted (map #(hash-map :hash (get-in % ["txin" :data :hash])
                                  :value (get-in % ["details" :data :value])) res)
        nodes (map #(:hash %) extracted)]
    {:nodes nodes :relations extracted}))

(defn get-until-coinbase-ex
  [conn params]
  (let [res (get-until-coinbase conn params)
        extracted (map #(hash-map :in {:hash (get-in % ["in" :data :hash])}
                                  :details {:value (get-in % ["details" :data :value])
                                            :index (get-in % ["details" :data :index])}
                                  :out {:hash (get-in % ["tx" :data :hash])}) res)
        nodes (set (map #(get-in % [:in :hash]) extracted))]
    {:nodes nodes :relations extracted}))

(defroutes graph-routes
           (GET "/tx/get-inputs/:hash" [hash] (response (get-inputs-ex conn {:hash hash})))
           (GET "/tx/get-until-coinbase/:hash" [hash] (response (get-until-coinbase-ex conn {:hash hash})))
           (GET "/stats" [] (response {:num-blk-indexed (-> (get-num-blk-indexed conn) (first) (vals) (first))
                                       :num-tx-indexed (-> (get-num-tx-indexed conn) (first) (vals) (first))})))