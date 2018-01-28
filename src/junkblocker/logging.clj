(ns junkblocker.logging
  (:import [java.time LocalTime]))

(defn file-log [path]
  (let [output-stream (clojure.java.io/output-stream path
                                                     :append true)]
    (fn [domain state]
      (.write output-stream
              (.getBytes (str (.toSecondOfDay (LocalTime/now))
                              domain
                              (name state)
                              "\n")
                         "US-ASCII")))))

(defn print-log [domain state]
  (println "[" state "]" domain))

(defn logger [info]
  (if info (file-log info) print-log))

