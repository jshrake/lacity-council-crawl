(ns lacity-council-crawl.core
  (:require [lacity-council-crawl.scrape :as scrape])
  (:require [clojure.java.jdbc :as sql])
  (:require [clj-http.client :as client])
  (:require [clj-time.coerce])
  (:require [clojure.tools.logging :as log])
  (:require [environ.core :as environ])
  (:require [carica.core :as carica])
  (:gen-class))

(def config (carica/configurer (carica/resources (environ/env :config))))
(def override-config (carica/overrider config))

(def db (config :db))
(def robots-txt-delay 10000) ; in ms, see: http://lacity.org/robots.txt
(def crawl-pause 5000) ; in ms
(def initial-vote-id 0)
(def max-drift 100)

(defn max-vote-seen
  "Returns the largest vote id that exists in the database
  or initial-vote-id if the database is empty"
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
  (log/info "Attempting to store vote data" data)
  (when data
    (let [vote-id (:id data)]
      (sql/insert! db :vote {:id vote-id
                             :agenda (:agenda-item data)
                             :date (clj-time.coerce/to-long (:date data))
                             :type (name (:type data))
                             :description (:description data)})
      ; Associate this vote with all file numbers listed
      (doseq [file (:file-numbers data)]
        (try (sql/insert! db :council_file {:id file}) (catch Exception e))
        (sql/insert! db :vote_council_file {:vote_id vote-id :file_id file}))

      ; Associate this vote with all council districts listed
      (doseq
        [district (:pertinent-districts data)]
        (sql/insert! db :impacted_district {:vote_id vote-id :district district}))

      ; Associate this vote with all city counciles and their vote
      (doseq
        [vote (:votes data)]
        (let [nom (:name vote)
              district (:district vote)
              outcome (name (:vote vote))
              unique-id (hash-combine district nom)]
          (store-council-member db unique-id nom district)
          (store-council-member-vote db unique-id vote-id outcome))))))

(defn ensure-table
  [database table-name & specs]
  (try (sql/db-do-commands database
                           (apply sql/create-table-ddl table-name specs))
       (catch Exception e (println e))))

(defn -main
  "I crawl unseen city council votes"
  [& args]
  (log/info "LA City Council Vote Scraper is online!")
  (ensure-table db :council_member
                [:id :bigint :primary :key]
                [:name :text]
                [:district :int]
                ["UNIQUE (name, district)"])

  (ensure-table db :vote
                [:id :int :primary :key]
                [:agenda :text]
                [:date :bigint]
                [:type :text]
                [:description :text])

  (ensure-table db :council_member_vote
                [:id :serial :primary :key]
                [:council_member_id :bigint :references "council_member (id)"]
                [:vote_id :int :references "vote (id)"]
                [:outcome :text])

  (ensure-table db :impacted_district
                [:id :serial :primary :key]
                [:vote_id :int :references "vote (id)"]
                [:district :int])

  (ensure-table db :council_file
                [:id :text :primary :key])

  (ensure-table db :vote_council_file
                [:id :serial :primary :key]
                [:vote_id :int :references "vote (id)"]
                [:file_id :text :references "council_file (id)"])
  (while true
    (let [max-seen (max-vote-seen db)
          begin (+ max-seen 1)
          end (+ max-seen max-drift)]
      (doseq [vote (range begin end)]
        (log/info "Fetching vote id" vote)
        (-> vote
          ((partial delayed-call robots-txt-delay scrape/fetch-vote))
          ((partial store db)))))
      (log/info "Crawler sleeping for" (/ crawl-pause 1000) "seconds")
      (Thread/sleep crawl-pause))
  (log/info "LA City Council Vote Scraper is shutting down!"))
