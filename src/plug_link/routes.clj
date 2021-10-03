(ns plug-link.routes
  "Routes to add to application to allow link/websocket communication"
  (:require [plug-link.server :as server]
            [taoensso.timbre :as log]))


(defn websocket-link
  ;;Doc string in core ns
  ([]
   (websocket-link {}))
  ([{:keys [path]
     :or   {path "/chsk"} :as opts}]
   (log/info "Init routes")
   [path {:get  (fn [req]                                   ;; Wrap in handler
                  (server/ring-ajax-get-or-ws-handshake req))
          :post (fn [req]
                  (server/ring-ajax-post req))}]))