(ns plug-link.core
  (:require [plug-link.dispatch :as dispatch])
  #?(:clj (:refer-clojure :exclude [send]))
  #?(:clj  (:require
             [plug-link.server :as server]
             [plug-link.routes :as routes])
     :cljs (:require
             [plug-link.client :as client])))


#?(:clj  (def init
           "Initialize server part of websocket based communications link.

           Options:
           --------
           :adapter      [opt] default: Undertow -- Provide one from sente if needed.
           :user-id-fn   [opt] default: :identity from request map

           Sente doc has more details."
           server/init)

   :cljs (def init
           "Initialize client part of websocket based communications link.

           Options:
           --------
           :path           [opt] default: /chsk        -- If altered, remember to provide the to routes fn.
           :type           [opt] default: :ws
           :packer         [opt] default: Transit
           :csrf-token     [opt] default: js/csrfToken -- Either populate csrfToken in e.g. backend template or provide actual token here
           :backoff-ms-fn  [opt] OBS: default is a custom function not the Sente default.

           Notes:
           - Sente doc has more info about most of these.
           - For re-frame support, require [plug-link.re-frame]"
           client/init))


;|-------------------------------------------------
;| ROUTES

#?(:clj
   (def routes
     "Routes to hook up websocket link

     Options:
     --------
     :path           [opt]  '/chsk' (default). If altered, remember to provide the same to init fn.
     :packer         [opt]* transit (default)*
     :csrf-token     [opt]* js/csrfToken (default) -- Either populate csrfToken in e.g. backend template, or provide token here
     :backoff-ms-fn  [opt]* default is a custom function not the Sente default. See sente doc for how to make one.
     :type           [opt]* default :ws -- See sente doc for more info

     Look to Luminus for how websocket routes should be handles in regards to middleware"
     routes/websocket-link))


;|-------------------------------------------------
;| SEND

(def send
  "Send message over link"
  #?(:clj  server/send
     :cljs client/send))

#?(:clj
   (def broadcast server/broadcast))


;|-------------------------------------------------
;| DISPATCH

#?(:cljs
   (def dispatch-incoming-sente-msg
     "Multimethod dispatching 'top-level' Sente events.
     Clients typically don't need to add any dispatchers here
     as (wrapped) user messages are dispatched by 'dispatch-incoming-msg'."
     dispatch/incoming-sente-msg))


(def dispatch-incoming-msg
  "Multimethod dispatching user/app link event message.
   Arg is an event vector: [event-id ,,,]

  Frontend:
  - If re-frame is not required as described in doc string of init fn,
    you need to either handle :default case or all messages explicitly."
  #?(:clj
     ;; Backend: Incoming messages are not wrapped in :chsk/recv
     dispatch/incoming-sente-msg
     :cljs
     ;; Frontend: Incoming messages are wrapped in :chsk/recv for easy relay to e.g. re-frame
     dispatch/incoming-wrapped-msg))
