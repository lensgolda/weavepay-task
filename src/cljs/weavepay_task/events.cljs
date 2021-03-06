(ns weavepay-task.events
  (:require
   [re-frame.core :as re-frame]
   [weavepay-task.db :as db]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]))


(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
  ::switch-view
  (fn [db [_ type]]
    (assoc db :view-type type)))

(re-frame/reg-event-db
  ::close-alert
  (fn [db [_ alert-id]]
    (assoc-in db [:alerts alert-id] nil)))

(re-frame/reg-event-db
  ::process-response
  (fn [db [_ resp]]
    (js/console.log (clj->js resp))
    (-> db
      (assoc-in [:alerts :find-error] nil)
      (assoc :view-type :articles))))

(re-frame/reg-event-db
  ::process-articles-response
  (fn [db [_ {:keys [articles count] :as resp}]]
    (js/console.log (clj->js resp))
    (-> db
      (assoc-in [:alerts :find-error] nil)
      (assoc :view-type :articles
             :articles  articles
             :count     count))))

(re-frame/reg-event-db
  ::bad-response
  (fn [db [_ resp]]
    (js/console.log (clj->js resp))
    (assoc-in db [:alerts :find-error] (-> resp :response :error))))

(re-frame/reg-event-fx
  ::find
  (fn [{db :db} [_ words]]
    {:http-xhrio {:method :get
                  :uri "http://localhost:3000/find"
                  :format (ajax/json-request-format)
                  :params {:words words}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::process-response]
                  :on-failure [::bad-response]}
     :db db}))

(re-frame/reg-event-fx
  ::articles-list
  (fn [{db :db} [_ page amount]]
    {:http-xhrio {:method :get
                  :uri "http://localhost:3000/articles"
                  :format (ajax/json-request-format)
                  :params {:page page :amount amount}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::process-articles-response]
                  :on-failure [::bad-response]}
     :db db}))
