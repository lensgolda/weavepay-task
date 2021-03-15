(ns weavepay-task.interceptors
  (:require [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.log :as log]
            [clojure.string :as str]))

(def log-request
  (interceptor
    {:name  ::log-request
     :enter (fn [context]
              (let [{:keys [uri request-method params]} (:request context)]
                (log/info
                  :method (str/capitalize (name request-method))
                  :uri uri
                  :params params)
                context))}))

(defn using
  [key <component>]
  (interceptor
    {:name ::using
     :enter (fn [context]
              (assoc-in context [:request :component (keyword key)] <component>))}))
