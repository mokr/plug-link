(ns plug-link.dispatch
  "Handles dispatch of incoming messages in both backend and frontend.

  In backend:
   - This is where you hook up dispatch of specific messages.

  In frontend:
   - You only add specific dispatch if you want to avoid the default re-frame event dispatch."
  (:require [taoensso.timbre :as log])
  #?(:clj  (:require
             [clojure.core.async :refer [go go-loop <! >! put! chan]])
     :cljs (:require
             [cljs.core.async :as async :refer [<! >! put! chan] :refer-macros [go go-loop]])))


;|-------------------------------------------------
;| EVENT DISPATCHER FOR MULTI FUNCTIONS

(defn- event-dispatcher
  "Dispatch on event name (first entry in event vector).
  Note: Works for both sente and link messages"
  [event]
  (first event))


;|-------------------------------------------------
;| FRONTEND WRAPPED USER/LINK MESSAGES
#?(:cljs
   (defmulti incoming-wrapped-msg
             "Dispatch of link messages.
             Allows us to perform something other than the default.
             E.g. hook message in frontend before it is dispatch via re-frame"
             event-dispatcher))

;; NOOP. Expected events wrapped in :chsk/recv to be user messages only, not Sente internal :chsk/ws-ping.
;; See https://github.com/ptaoussanis/sente/issues/391
#?(:cljs
   (defmethod incoming-wrapped-msg :chsk/ws-ping [_]))


;;Frontend note: If you :require [plug-link.re-frame], it will hook up this :default to go to re-frame dispatch
;(defmethod incoming-wrapped-msg :default [event]
;  (log/error "Unhandled event" event))


;|-------------------------------------------------
;| SENTE INTERNAL MESSAGES

(defmulti incoming-sente-msg
          "Dispatch of the 'top-level' messages on sente websocket link.
          User messages are wrapped in :chsk/recv that are unwrapped and
          sent to separate link message dispatcher."
          event-dispatcher)

;; Frontend only: Relay wrapped messages to separate dispatch
#?(:cljs
   (defmethod incoming-sente-msg :chsk/recv [event]         ;; Should only see these on client side
     (let [[_ wrapped-event] event]
       (incoming-wrapped-msg wrapped-event))))


;;NOOP
(defmethod incoming-sente-msg :default [_])


;|-------------------------------------------------
;| FIRST RECEIVER

(defn temp-debug-logging [{:keys [id event] :as msg}]
  (case id
    :chsk/ws-ping nil
    :chsk/recv (log/debug "Got :chsk/recv wrapped event" (some-> event second first))
    (log/debug "Got message ID" id)))


(defn handle-incoming-sente-message
  "First receiver of Sente channel message.
  Enables e.g. logging before dispatch of event."
  [{:keys [event] :as msg}]
  ;(temp-debug-logging msg)                                  ;;DEBUG
  (incoming-sente-msg event))


;|-------------------------------------------------
;| DISPATCH LOOP

(defn start-dispatch-loop [ch-chsk]
  (log/info "Init receive loop")
  (go-loop []
    (handle-incoming-sente-message (<! ch-chsk))
    (recur)))