(ns junkblocker.fs
  (:gen-class)
  (:import [java.nio.file WatchService Paths FileSystems]))

(def ENTRY_MODIFY java.nio.file.StandardWatchEventKinds/ENTRY_MODIFY)


(defn watch [path callback]
  (let [file-system (FileSystems/getDefault)
        watcher (.newWatchService file-system)]
    (.register path watcher (into-array [ENTRY_MODIFY]))
    (.start (Thread.
              (fn []
                (loop []
                  (let [key (.take watcher)]
                    (.pollEvents key)
                    (.reset key)
                    (callback)
                    (recur))))))))
