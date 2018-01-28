(ns junkblocker.hostfile
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))


(defn parse-host-file
  "Parse a `host` file into a seq of domains."
  [f]
  (->> (slurp f)
      (string/split-lines)
      (filter #(string/starts-with? % "0.0.0.0 "))
      (map #(string/replace % #"0\.0\.0\.0 " ""))))


(defn load-blacklist
  "Return list of default blocked domains."
  [path]
  (parse-host-file (io/file path)))
