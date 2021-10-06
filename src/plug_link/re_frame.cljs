(ns plug-link.re-frame
  "Hook up re-frame subscriptions, events, fx and cofx"
  (:require [re-frame.core :as rf]
            [plug-link.dispatch :as dipatch]
            [plug-link.client :as client]
            [taoensso.timbre :as log]))

(log/debug "Init re-frame integration")

;|-------------------------------------------------
;| DISPATCH INCOMING

;; Let re-frame handle dispatch of all messages (that has not been overridden by a specific defmethod)
(defmethod dipatch/incoming-link-msg :default [event]
  (rf/dispatch event))


;|-------------------------------------------------
;| EVENTS - OUTGOING MESSAGE TO SERVER

(rf/reg-event-fx
  :link/send
  [rf/trim-v]
  (fn [{:keys [db]} [message]]
    (client/send! message)
    ;;TODO: Parse data and update db, dispatch, ...
    {}))


;|-------------------------------------------------
;| FX - OUTGOING MESSAGE (to server)


(rf/reg-fx
  :link-send
  (fn [event-vector]
    (client/send! event-vector)))
