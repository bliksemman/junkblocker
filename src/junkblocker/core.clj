(ns junkblocker.core
  (:gen-class)
  (:import [java.net DatagramSocket DatagramPacket InetAddress])
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [junkblocker.dns :as dns]
   [junkblocker.rules :as rules]
   [junkblocker.logging :as logging]
   [clojure.tools.cli :as cli]))

(defn resolver [address]
  "Create a resolver that proxies a DNS request."
  (fn [query]
    (let [socket (DatagramSocket.)
          request-data (dns/encode query)
          request (DatagramPacket. request-data (alength request-data) (InetAddress/getByName address) 53)
          response-data (byte-array 8192)
          response (DatagramPacket. response-data (alength response-data))]
      (.send socket request)
      (.receive socket response)
      (.close socket)
      (dns/decode (.getData response)))))


(defn denied-response [query]
  (dns/deny query))

(defn handle-request [{:keys [deny? log lookup-query]} server-socket request]
  (let [message (:message request)
        query (dns/decode (:message request))
        domain (dns/domainname query)]
    (let [denied (deny? domain)
          query-response (if denied
                          (denied-response query)
                          (lookup-query query))
          response-data (dns/encode query-response)
          response (DatagramPacket. response-data (alength response-data) (:sender request))]
        (log domain (if denied :blocked :ok))
        (.send server-socket response))))


(defn start-server [{port :port :as conf}]
  (let [server-socket (DatagramSocket. port)]
    (loop []
      (let [receive-data (byte-array 8192)
            receive-packet (DatagramPacket. receive-data (alength receive-data))]
        (.receive server-socket receive-packet)
        (let [request {:message (.getData receive-packet)
                       :sender (.getSocketAddress receive-packet)}]
          (handle-request conf server-socket request)
          (recur))))))


;; CLI stuff
;; ---------

(def cli-options
  [["-h" "--help"]
   ["-c" "--conf PATH" "Path to config file."
    :default (clojure.java.io/resource "defaults.edn")
    :parse-fn clojure.java.io/as-file
    :validate [#(.exists %)
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
      (let [config (edn/read-string (slurp (:conf options)))]
        (when-let [blacklist (:black-list config)]
          (when-not (.exists (clojure.java.io/as-file blacklist))
            (exit 1 (str "ERROR: Blacklist not found; " blacklist))))
        (let [deny? (rules/load-rules config)
              lookup-query
              (resolver (get config :resolver "8.8.8.8"))
              log (logging/logger (:log config))]
          (println "Starting...")
          (start-server
            {:port (get config :port 53)
             :deny? deny?
             :log log
             :lookup-query lookup-query}))))))
