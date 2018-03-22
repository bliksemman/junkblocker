(defproject junkblocker "0.2.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.rpl/specter "1.0.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/core.cache "0.6.5"]
                 [dnsjava/dnsjava "2.1.8"]]
  :main ^:skip-aot junkblocker.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
