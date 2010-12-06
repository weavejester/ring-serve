(ns ring.util.serve
  "Development web server for Ring handlers."
  (:import java.net.BindException
           org.mortbay.log.Logger)
  (:use clojure.java.browse
        ring.middleware.stacktrace
        ring.middleware.swank
        ring.adapter.jetty))

(deftype SilentLogger []
  Logger
  (debug [logger msg arg0 arg1])
  (debug [logger msg th])
  (getLogger [logger name] logger)
  (info [logger msg arg0 arg1])
  (isDebugEnabled [logger] false)
  (warn [logger msg arg0 arg1]))

;; Stop Jetty logging everything
(System/setProperty
  "org.mortbay.log.class"
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

(defn serve* [handler & [port]]
  (stop-server)
  (reset! jetty-server
          (if port
            (start-server handler port)
            (start-server handler)))
  (println "Started web server on port" (server-port))
  (browse-url (server-url))
  nil)

(defmacro serve
  "Start a development web server that runs the supplied handler in a
  background thread. Any changes to the handler will be automatically
  visible on the server."
  [handler & [port]]
  (if (symbol? handler)
    `(serve* (var ~handler) ~port)
    `(serve* ~handler ~port)))
