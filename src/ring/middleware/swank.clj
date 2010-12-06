(ns ring.middleware.swank
  "Middleware for working with Emacs swank connections.")

;; The context we're compiling in doesn't necessarily have swank-clojure
;; as a dependency. We need to create the middleware at runtime when
;; we can detect whether the swank.core.connection class is available.

(defn- make-swank-middleware []
  (eval
   '(fn [handler]
      (let [conn swank.core.connection/*current-connection*]
        (fn [request]
          (swank.core.connection/with-connection conn
            (handler request)))))))

(defn wrap-swank
  "If this session is connected to a swank server, ensure that the handler
  has access to the swank connection when called by the web server. If this
  session does not have a swank connection, this middleware returns the handler
  unchanged."
  [handler]
  (if (find-ns 'swank.core.connection)
    (let [middleware (make-swank-middleware)]
      (middleware handler))
    handler))
