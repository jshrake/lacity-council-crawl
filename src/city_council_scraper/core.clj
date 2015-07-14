(ns city-council-scraper.core
  (:require [city-council-scraper.scrape :as scrape])
  (:require [clojure.java.jdbc :as sql])
  (:require [clj-http.client :as client])
  (:require [clj-time.coerce])
  (:gen-class))

(def dev-db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "db/votes.sqlite3"})

(def production-db
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "db/votes.sqlite3"})

(def db dev-db)
(def robots-txt-delay 10000)
(def crawl-pause 5000)
(def initial-vote-id 1)
(def max-drift 100)

(defn max-vote-seen
  "Returns the largest vote id that exists in the database"
  [db]
  (let [result (sql/query db ["select max(id) as maxid from vote"])]
    (or (:maxid (first result)) initial-vote-id)))

(defn delayed-call
  "Call func with args after delay-time. This is blocking!"
  [delay-time func & args]
  (Thread/sleep delay-time)
  (apply func args))

(defn store-council-member [db id nom district]
  (try (sql/insert! db :council_member {:id id :name nom :district district})
    (catch Exception e)))

(defn store-council-member-vote [db council-id vote-id outcome]
  (sql/insert! db :council_member_vote {:council_member_id council-id :vote_id vote-id :outcome outcome}))

(defn store
  [db data]
  (when data
    (println "******************")
    (println "Storing data" data)
    (sql/insert! db :vote {:id (:id data)
                           :agenda (:agenda-item data)
                           :date (clj-time.coerce/to-long (:date data))
                           :type (name (:type data))
                           :description (:description data)
                           :file (:file-number data)})
    (doseq
      [vote (:votes data)]
      (let [nom (:name vote)
            district (:district vote)
            outcome (name (:vote vote))
            unique_id (hash-combine district nom)]
        (store-council-member db unique_id nom district)
        (store-council-member-vote db unique_id (:id data) outcome)))
    (doseq
      [district (:pertinent-districts data)]
      (sql/insert! db :impacted_district {:vote_id (:id data) :district district}))))


(defn ensure-table
  [database table-name & specs]
  (try (sql/db-do-commands database
                           (apply sql/create-table-ddl table-name specs))
       (catch Exception e (println e))))

(defn -main
  "I crawl unseen city council votes"
  [& args]
  (ensure-table db :council_member
                [:id :int :primary :key]
                [:name :text]
                [:district :int]
                ["UNIQUE (name, district)"])

  (ensure-table db :vote
                [:id :int :primary :key]
                [:agenda :text]
                [:date :int]
                [:type :text]
                [:description :text]
                [:file :text])

  (ensure-table db :council_member_vote
                [:id :int :primary :key]
                [:council_member_id :int :references "council_member (id)"]
                [:vote_id :int :references "vote (id)"]
                [:outcome :text])

  (ensure-table db :impacted_district
                [:id :int :primary :key]
                [:vote_id :int :references "vote (id)"]
                [:district :int])
  (println "Crawler starting to crawl")
  (while true
    (let [max-seen (max-vote-seen db)
          begin (+ max-seen 1)
          end (+ max-seen max-drift)]
      (println "Crawling from" begin "to" end)
      (doseq [vote (range begin end)]
        (-> vote
          ((partial delayed-call robots-txt-delay scrape/fetch-vote))
          ((partial store db)))))
      (println "Crawler sleeping for" (/ crawl-pause 1000) "seconds")
      (Thread/sleep crawl-pause)))
