(defproject net.clojars.mokr/plug-link "0.1.0-SNAPSHOT"
  :description "Websocket based communication link between backend and frontend"
  :url "https://github.com/mokr/plug-link"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [org.clojure/clojurescript "1.10.879" :scope "provided"]
                 [org.clojure/core.async "1.3.622"]
                 [com.taoensso/sente "1.16.2"]
                 [com.taoensso/timbre "5.1.2"]
                 [re-frame "1.2.0" :scope "provided"]]
  :repl-options {:init-ns plug-link.core})

