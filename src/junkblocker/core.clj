(ns junkblocker.core
  (:gen-class)
  (:require
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [aleph.udp :as udp]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [junkblocker.dns :as dns]
   [junkblocker.rules :as rules]
   [junkblocker.logging :as logging]
   [clojure.tools.cli :as cli]))

(defn resolver [address]
  "Create a resolver that proxies a DNS request."
  (fn [query]
    (d/let-flow [client-socket (udp/socket {})]
      (d/chain
       (s/put! client-socket
               {:host    address
                :port    53
                :message (dns/encode query)})
       (fn [_] (s/take! client-socket))
       :message
       dns/decode))))


(defn denied-response [query]
  (dns/deny query))

(defn handle-request [{:keys [deny? log lookup-query]} server-socket request]
  (let [message (:message request)
        query (dns/decode (:message request))
        domain (dns/domainname query)]
    (->
     (d/chain
      (let [denied (deny? domain)]
        (log domain (if denied :blocked :ok))
        (if denied
          (d/success-deferred (denied-response query))
          (lookup-query query)))
      dns/encode
      #(s/put! server-socket
               {:socket-address (:sender request)
                :message %}))
     (d/catch Exception #(println "whoops, that didn't work:" %)))))


(defn start-server [{port :port :as conf}]
  (let [server-socket @(udp/socket {:port port})]
    (s/consume (partial handle-request conf server-socket) server-socket)))

;; CLI stuff
;; ---------

(def cli-options
  [["-h" "--help"]
   ["-c" "--conf PATH" "Path to config file."
    :validate [#(.exists (clojure.java.io/as-file %))
               "Config file does not exists."]]])

(defn usage [summary]
  (->> ["Run a DNS proxy server. It can block certain requests based on "
        "several conditions."
        ""
        "Usage: program-name [options] action"
        ""
        "Options:"
        summary]
       (clojure.string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map

  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      :else
      {:options options})))

(defn -main [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [config (edn/read-string (slurp (:conf options)))
            deny? (rules/load-rules config)
            lookup-query
            (resolver (get config :resolver "8.8.8.8"))
            log (logging/logger (:log config))
            server (start-server
                    {:port (get config :port 53)
                     :deny? deny?
                     :log log
                     :lookup-query lookup-query})]
        (println "Starting...")
        @server))))
