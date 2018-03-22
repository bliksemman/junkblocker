(ns junkblocker.server
    (:import [java.net DatagramSocket DatagramPacket InetAddress])
    (:require
     [clojure.edn :as edn]
     [clojure.string :as str]
     [junkblocker.dns :as dns]
     [junkblocker.rules :as rules]
     [junkblocker.logging :as logging]))
  
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
  
(defn start [port conf]
  (let [server-socket (DatagramSocket. port)]
    (loop []
      (let [receive-data (byte-array 8192)
            receive-packet (DatagramPacket. receive-data (alength receive-data))]
        (.receive server-socket receive-packet)
        (let [request {:message (.getData receive-packet)
                       :sender (.getSocketAddress receive-packet)}]
          (handle-request @conf server-socket request)
          (recur))))))
  
