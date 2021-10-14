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
;| USER/LINK MESSAGES

(defmulti incoming-msg
          "Dispatch of link messages.
          Allows us to perform something other than the default.
          E.g. hook message in frontend before it is dispatch via re-frame"
          event-dispatcher)


;; NOOP. Expected events wrapped in :chsk/recv to be user messages only, not Sente internal :chsk/ws-ping.
;; See https://github.com/ptaoussanis/sente/issues/391
(defmethod incoming-link-msg :chsk/ws-ping [_])


(defmethod incoming-link-msg :default [event]
  ;;NOTE: If you :require [plug-link.re-frame], it will hook up this :default to go to re-frame dispatch
  #?(:clj (log/error "Unhandled event" event)))


;|-------------------------------------------------
;| SENTE INTERNAL MESSAGES

(defmulti incoming-sente-internal-msg event-dispatcher)


(defmethod incoming-sente-msg :chsk/recv [event]            ;; Should only see these on client side
  (let [[_ wrapped-event] event]
    (incoming-link-msg wrapped-event)))


;;NOOP
(defmethod incoming-sente-msg :default [_])


;|-------------------------------------------------
;| FIRST RECEIVER

(defn dispatch-incoming-message
  "First receiver of Sente channel message.
  Enables e.g. logging before true dispatch starts."
  [{:keys [event] :as msg}]
  (log/debug "Got message ID" (:id msg))                    ;;TODO: Will probably disable this when everything works as it might spam too much
  (incoming-sente-msg event))


;|-------------------------------------------------
;| DISPATCH LOOP

(defn start-dispatch-loop [ch-chsk]
  (log/info "Init receive loop")
  (go-loop []
    (dispatch-incoming-message (<! ch-chsk))
    (recur)))