(ns weavepay-task.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::view-type
  (fn [db]
    (:view-type db)))

(re-frame/reg-sub
  ::alerts
  (fn [db [_ id]]
    (get-in db [:alerts id])))

(re-frame/reg-sub
  ::articles
  (fn [db]
    (:articles db)))