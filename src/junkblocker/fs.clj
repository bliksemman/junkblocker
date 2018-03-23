(ns junkblocker.fs
  (:gen-class)
  (:import [java.nio.file WatchService Paths FileSystems]))

(def ENTRY_MODIFY java.nio.file.StandardWatchEventKinds/ENTRY_MODIFY)

(defn directory [path]
  "Return the path if it's a directory or the parent."
  (if (-> path .toFile .isDirectory)
    path
    (.getParent path)))

(defn watch [callback path]
  (let [file-system (FileSystems/getDefault)
        watcher (.newWatchService file-system)
        path (.toAbsolutePath path)
        dir-path (directory path)]
    (.register dir-path watcher (into-array [ENTRY_MODIFY]))
    (.start (Thread.
              (fn []
                (loop []
                  (let [key (.take watcher)]
                    (doall (for [event (.pollEvents key)]
                              (let [event-path (-> event .context .toAbsolutePath)]
                                (when (= event-path path)
                                  (callback)))))
                    (.reset key)
                    (recur))))))))
