(ns backend.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [response resource-response]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]

            [backend.graph :refer [graph-routes]]))

(defroutes app-routes
  (context "/graph" [] graph-routes)
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (route/resources "/")
  (ANY "/*" [] (resource-response "index.html" {:root "public"}))
  (route/not-found (resource-response "index.html" {:root "public"})))

(def app
  (-> (handler/site app-routes)
      (wrap-json-body)
      (wrap-json-response)))