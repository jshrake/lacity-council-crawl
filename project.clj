(defproject lacity-council-crawl "0.1.0-SNAPSHOT"
  :description "A crawler and scraper of LA City Council Voting data"
  :url "https://github.com/jshrake/lacity-council-crawl"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [org.xerial/sqlite-jdbc "3.8.10.1"]
                 [enlive "1.1.5"]
                 [clj-http "1.1.2"]
                 [clj-time "0.9.0"]]
  :main ^:skip-aot lacity-council-crawl.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
