(ns junkblocker.dns
  (:import [org.xbill.DNS Message Flags Rcode])
  (:require [clojure.string :as str]))


(defn decode [bytes]
  (Message. bytes))


(defn encode [message]
  (.toWire message))


(defn domainname [message]
  (.. message
      getQuestion
      getName
      (toString true)))


(defn deny [message]
  (let [message (. message clone)]
    (doto (.getHeader message)
      (.setFlag (. Flags QR))
      (.setFlag (. Flags RA))
      (.setRcode (. Rcode NXDOMAIN)))
    message))


(defn server-error [message]
  (let [message (. message clone)]
    (doto (.getHeader message)
      (.setFlag (. Flags QR))
      (.setFlag (. Flags RA))
      (.setRcode (. Rcode SERVFAIL)))
    message))


(defn response-from 
  "Creates a new response for the given query based on the given
  response."
  [response query]
  (let [message (.clone response)]
    (doto (.getHeader message)
      (.setID (.. query getHeader getID)))
    message))

