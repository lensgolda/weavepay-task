(ns weavepay-task.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
  ::view-type
  (fn [db]
    (:view-type db)))

(re-frame/reg-sub
  ::alerts
  (fn [db [_ id]]
    (get-in db [:alerts id])))

(re-frame/reg-sub
  ::input-value
  (fn [db]
    (:input-value db)))

(re-frame/reg-sub
  ::articles
  (fn [db]
    (:articles db)))