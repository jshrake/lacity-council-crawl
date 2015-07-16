(defproject lacity-council-crawl "0.1.0-SNAPSHOT"
  :description "A crawler and scraper of LA City Council Voting data"
  :url "https://github.com/jshrake/lacity-council-crawl"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [enlive "1.1.5"]
                 [clj-http "1.1.2"]
                 [clj-time "0.9.0"]
                 [environ "1.0.0"]
                 [sonian/carica "1.1.0" :exclusions [[cheshire]]]]
  :plugins [[lein-environ "1.0.0"]]
  :main ^:skip-aot lacity-council-crawl.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.xerial/sqlite-jdbc "3.8.10.1"]]
                   :env {:config "dev_config.edn"}}
             :production {:dependencies [[org.postgresql/postgresql "9.4-1201-jdbc41"]]
                          :env {:config "production_config.edn"}}})
