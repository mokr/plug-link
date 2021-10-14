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
(defmethod dipatch/incoming-msg :default [event]
  (rf/dispatch event))


;|-------------------------------------------------
;| EVENTS - OUTGOING MESSAGE TO SERVER

(rf/reg-event-fx
  :link/send
  [rf/trim-v]
  (fn [_ [event-vector]]
    {:link-send event-vector}))


;|-------------------------------------------------
;| FX - OUTGOING MESSAGE (to server)

(rf/reg-fx
  :link-send
  (fn [event-vector]
    (client/send! event-vector)))
