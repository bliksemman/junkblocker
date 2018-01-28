(def project 'junkblocker)
(def version "0.1.0")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.8.0"]
                            [adzerk/boot-test "RELEASE" :scope "test"]
                            [io.djy/boot-github "0.1.3" :scope "test"]
                            [aleph "0.4.3"]
                            [com.rpl/specter "1.0.4"]
                            [org.clojure/tools.cli "0.3.5"]
                            [org.clojure/core.cache "0.6.5"]
                            [dnsjava/dnsjava "2.1.8"]])
                            
(task-options!
 aot {:namespace   #{'junkblocker.core}}
 pom {:project     project
      :version     version
      :description "DNS server which blocks lookups (configurable)."
      :url         "https://github.com/bliksemman/junkblocker"
      :scm         {:url "https://github.com/bliksemman/junkblocker"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 jar {:main        'junkblocker.core
      :file        (str "junkblocker-" version ".jar")})

(require '[io.djy.boot-github :refer (push-version-tag create-release)])

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (pom) (uber) (jar) (target :dir dir))))

(deftask run
  "Run the project."
  [a args ARG [str] "the arguments for the application."]
  (require '[junkblocker.core :as app])
  (apply (resolve 'app/-main) args))

(require '[adzerk.boot-test :refer [test]])
                            

(deftask release
  "Create a release and push it to GitHub."
  []
  (comp
    (build)
    (push-version-tag :version version) 
    (create-release :version version :changelog true
                    :assets #{(str "target/junkblocker-" version ".jar")})))
