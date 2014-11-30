(defproject rawtx "0.1.0-SNAPSHOT"
  :description "graphical blockchain exploration"
  :url "www.rawtx.com"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [compojure "1.1.8"]
                 [org.apache.httpcomponents/httpclient "4.3.2"]
                 [org.clojure/java.jdbc "0.3.4"]
                 [org.postgresql/postgresql "9.3-1101-jdbc4"]
                 [com.taoensso/timbre "3.3.1"]
                 [environ "0.5.0"]
                 [ragtime "0.3.7"]
                 [com.google/bitcoinj "0.11.3"]
                 [clojurewerkz/neocons "3.0.0"]
                 [ring/ring-json "0.3.1"]]
  :plugins [[lein-ring "0.8.12"]
            [lein-autoreload "0.1.0"]
            [ragtime/ragtime.lein "0.3.7"]]
  :ring {:handler backend.handler/app}
  :aot [util.blockchain-sync]
  :main util.blockchain-sync
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost/coincartier?user=coincartier&password=coincartier"}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
