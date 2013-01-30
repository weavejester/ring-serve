(ns ring.util.serve
  "Development web server for Ring handlers."
  (:import java.net.BindException
           org.eclipse.jetty.util.log.Logger)
  (:use clojure.java.browse
        ring.middleware.stacktrace
        ring.middleware.swank
        ring.adapter.jetty))

(deftype SilentLogger []
  Logger
  (getLogger [logger name] logger)
  (isDebugEnabled [logger] false))

;; Stop Jetty logging everything
(System/setProperty
  "org.eclipse.jetty.util.log.class"
  "ring.util.serve.SilentLogger")

(defonce jetty-server (atom nil))

(defn stop-server
  "Stop the server started by the serve macro."
  []
  (when @jetty-server
    (.stop @jetty-server)
    (println "Stopped web server")))

(defn- try-ports [func ports]
  (try (func (first ports))
       (catch BindException ex
         (if-let [ports (next ports)]
           (try-ports func ports)
           (throw ex)))))

(def suitable-ports (range 3000 3011))

(defn- start-server
  ([handler]
     (try-ports #(start-server handler %) suitable-ports))
  ([handler port]
     (run-jetty (-> handler wrap-swank wrap-stacktrace)
                {:port port, :join? false})))

(defn- server-connector []
  (first (.getConnectors @jetty-server)))

(defn server-port []
  (.getPort (server-connector)))

(defn server-url []
  (let [connector (server-connector)
        host      (or (.getHost connector) "0.0.0.0")
        port      (.getPort connector)]
    (str "http://" host ":" port)))

(defn serve* [handler & [port headless?]]
  (stop-server)
  (reset! jetty-server
          (if port
            (start-server handler port)
            (start-server handler)))
  (println "Started web server on port" (server-port))
  (when-not headless?
    (browse-url (server-url)))
  nil)

(defmacro serve
  "Start a development web server that runs the supplied handler in a
  background thread. Any changes to the handler will be automatically
  visible on the server."
  [handler & [port]]
  (if (symbol? handler)
    `(serve* (var ~handler) ~port)
    `(serve* ~handler ~port)))

(defmacro serve-headless
  "Start a development web server that runs the supplied handler in a
  background thread. Any changes to the handler will be automatically
  visible on the server.  A web browser is NOT invoked!"
  [handler & [port]]
  (if (symbol? handler)
    `(serve* (var ~handler) ~port true)
    `(serve* ~handler ~port true)))
