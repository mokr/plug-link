# plug-link

Goals, priorities & notes:

* Provide a websocket link between frontend and backend.
* Reduce boilerplate.
* Tailor for use in re-frame based app
* Currently supports only a single link fe<->be

Status:

* Experimental
* Work in progress
* Breakage likely

# USAGE

### Hook it up

Backend:

```clojure
(ns your.core
  (:require [plug-link.core :as link]))


(link/init)
```

```clojure 
(ns your.handler
  (:require [plug-link.core :as link]))

;; Assuming Luminus based project (using reitit for routing),
;; add this to routes after `(home/routes)`  
(link/routes)

```

Frontend (assuming re-frame support):

```clojure
(ns your.core
  (:require [plug-link.core :as link]
    [plug-link.re-frame]))


(link/init)
```

### Send messages

Backend:

```clojure
(ns your.app
  (:require [plug-link.core :as link]))


(link/send!)
(link/broadcast!)
```

Frontend:

```clojure
(ns your.app
  (:require [plug-link.core :as link]
    [re-frame.core :as rf])) ; Note: >evt is: (def >evt re-frame.core/dispatch)

(link/send! [:some/event {:hello "server"}])

;; Re-frame via event
(>evt [:link/msg [:some/event {:hello "server"}]])

;; Re-frame via effect
(rf/reg-event-fx
  :some/action
  (fn [_ _]
    {:link/msg [:some/event {:hello "server"}]}))
```

# LICENCE

Copyright © 2021 Morten Kristoffersen

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such
availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU
Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.
