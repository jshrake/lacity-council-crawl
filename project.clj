(defproject city-council-scraper "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [org.xerial/sqlite-jdbc "3.8.10.1"]
                 [enlive "1.1.5"]
                 [clj-http "1.1.2"]
                 [clj-time "0.9.0"]]
  :main ^:skip-aot city-council-scraper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
