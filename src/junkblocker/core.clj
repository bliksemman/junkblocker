(ns junkblocker.core
  (:gen-class)
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [junkblocker.server :as server]
   [junkblocker.fs :as fs]
   [junkblocker.rules :as rules]
   [junkblocker.logging :as logging]
   [clojure.tools.cli :as cli]))

;; CLI stuff
;; ---------

(def cli-options
  [["-h" "--help"]
   ["-c" "--conf PATH" "Path to config file."
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
      
(defn default-config []
  (clojure.java.io/resource "defaults.edn"))

(defn create-server-config [options]
  (let [config-file (get options :conf (default-config))
        config (edn/read-string (slurp config-file))]
    (when-let [blacklist (:black-list config)]
      (when-not (.exists (clojure.java.io/as-file blacklist))
        ; FIXME: make exception / return error
        (exit 1 (str "ERROR: Blacklist not found; " blacklist))))
    (let [deny? (rules/load-rules config)
          lookup-query
          (server/resolver (get config :resolver "8.8.8.8"))
          log (logging/logger (:log config))]
      {:port (get config :port 53)
       :host (get config :host "")       
       :deny? deny?
       :log log
       :lookup-query lookup-query})))

(defn reload-server-config [config-atom options]
  (println "Reloading conf")
  (let [conf (create-server-config options)]
    (swap! config-atom merge conf)))

(defn -main [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [conf (create-server-config options)
            config-atom (atom conf)]
        (when-let [conf (:conf options)]    
          (fs/watch 
            #(reload-server-config config-atom options)
            (.toPath conf)))
        (println "Starting...")
        (server/start config-atom)))))

