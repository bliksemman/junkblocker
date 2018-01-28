(ns junkblocker.rules
  (:require [clojure.edn :as edn]
            [com.rpl.specter :as specter]
            [junkblocker.hostfile :as hostfile])
  (:import [java.time LocalTime]))

(defn deny-pattern [pattern]
  (let [re (re-pattern pattern)]
    (fn [domain]
      (-> re
          (re-matches domain)
          boolean))))

(defn rule-list [rules]
  (fn [domain]
    (some #(% domain) rules)))

(defn deny-all [domains]
  (let [domains (set domains)]
    (fn [domain]
      (contains? domains domain))))

(defn deny-all-patterns [patterns]
  (-> (map deny-pattern patterns)
      rule-list))

(defn some-rules
  "Combine multiple rules into one rule set."
  [rules]
  (fn [domain] (some #(% domain) rules)))

(defn and-rules
  "Require that all rules trigger."
  [rules]
  (fn [domain]
    (reduce (fn [cur rule] (and cur (rule domain))) true rules)))

(defn within-time-range?
  "Check if the given time is within the time range.

  Assumes that time 23:00 is within 15:00 and 7:00.
  "
  [time start end]
  (if (< end start)
    (not (within-time-range? time end start))
    (and (<= start time) (<= time end))))

(defn current-time-within-range?
  "Return true if the current time is inside the specified range."
  [start end]
  (-> (LocalTime/now)
      .toSecondOfDay
      (within-time-range? start end)))

(defn- time-in-seconds
  "Convert a string with a time (13:25) to seconds."
  [t]
  (let [[hours minutes] (map #(if % (Integer/parseInt %) 0)
                             (rest (re-find #"(\d+)(?::(\d+))?" t)))]
    (+ (* minutes 60) (* hours 3600))))

(defn- time-range-rule [{:keys [from to domains]}]
  (let [domains (set domains)
        start (time-in-seconds from)
        end (time-in-seconds to)]
    (fn [domain]
      (and (contains? domains domain)
           (current-time-within-range? start end)))))

(defn flatten-time-range [{:keys [:from :to :domains]}]
  (map #(assoc {:from from :to to} :domain %) domains))

(defn group-time-ranges-by-domain [time-ranges]
  (group-by :domain
            (mapcat flatten-time-range time-ranges)))

(defn deny-outside-time-range [time-ranges]
  (let [time-ranges-in-sec
        (specter/transform [specter/ALL (specter/multi-path :from :to)]
                           time-in-seconds
                           time-ranges)
        domain-map (group-time-ranges-by-domain time-ranges-in-sec)]
    (fn [domain]
      (if-let [time-ranges (get domain-map domain)]
        (not (some (fn [{:keys [from to]}] (current-time-within-range? from to))
                   time-ranges))))))

(defn load-rules [conf]
  (some-rules
   (filter identity 
           [(deny-all (:blocked conf))
            (deny-outside-time-range (:allow-during conf))
            (when-let [black-list (:black-list conf)]
              (deny-all (hostfile/load-blacklist black-list)))])))
