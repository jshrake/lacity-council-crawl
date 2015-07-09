(ns city-council-scraper.core
  (:require [city-council-scraper.scrape :as scrape])
  (:require [net.cgrand.enlive-html :as html])
  (:require [clj-time.format])
  (:gen-class))

(defn store-vote-data
  [data]
  (println "Storing data:" data)
  true)

;; crawling juice
;; 0. determine the maximum vote id
;; this can be done by performing a binary search on the votedetails resource of the cityclerk.lacity.org host
;; when fetching an id that dosn't exist, the server returns a response header that contains server_error: true
;; the maximum vote id is between 1 and n (for some large n, n ~ 1 billion)
;; to find it, fetch urls and binary search until you find the largest url that doesn't return a server error
;; this should happen in no more than log_2 (n) requests (n ~ 1 billion, this is ~30 requests)

;; once we've acquired the maximum vote id m, we want to ensure we've scraped all vote ids between 1 and m
;; take a set difference between (set (range 1 m)) and the votes in our databse -- these are the new votes to fetch

;; for each vote to fetch
;; fetch it -> process it -> store it in our db
;; delay by amount specified in robots.txt (10 seconds)

;; this process can go on indefinitely (perhaps with delays of 5-10 minutes between cycles)
;;

(def crawl-delay 1)
(def loop-delay 5000)
(def initial-vote-id 1)
(def impossibly-large-vote-id 1000000000)

(defn vote-exists?
  "Determine if a given vote-id exists by fetching the html resource.
  If the vote does not exist, the response header will contain server_error: true
  otherwise, the vote exists"
  ; TODO (jshrake): implement
  [vote-id] (< vote-id 85000))

(defn find-max-vote
  "Binary search to find the most recent vote id on cityclerk.lacity.org.
  Note that this function needs to obey the crawl delay from robots.txt"
  [min-vote-id max-vote-id]
  (let [curr-min min-vote-id
        curr-max max-vote-id
        curr (int (/ (+ max-vote-id min-vote-id) 2))
        curr-exists? (vote-exists? curr)]
    (Thread/sleep crawl-delay)
    (cond
      (= (- curr-max curr-min) 1) curr-min
      curr-exists? (recur curr curr-max)
      (not curr-exists?) (recur curr-min curr))))

(defn seen-votes
  "Returns a set of vote ids that exist in the database"
  ; TODO (jshrake): implement
  []
  (set (range initial-vote-id 84500)))

(defn existing-votes
  "Returns a set of vote ids that exist on cityclerk.lacity.org.
  Assumes that the vote ids are contiguous between 1 and N"
  []
  (set (range 1 (find-max-vote initial-vote-id impossibly-large-vote-id))))

(defn unseen-votes
  "Returns a set of vote ids that exist on cityclerk.lacity.org
  but do not exist within our databse"
  []
  (clojure.set/difference (existing-votes) (seen-votes)))

(defn crawl-votes
  [votes]
   (doseq [vote votes]
     (println "Crawling vote:" vote)
     (->>
       (scrape/fetch-vote-html vote)
       scrape/extract-vote-data
       store-vote-data)
     (println "Sleeping for:" (/ crawl-delay 1000) "seconds")
     (println "-------------------")
     (Thread/sleep crawl-delay)))

(defn -main
  "I crawl unseen city council votes"
  [& args]
  (while true
    (println "Crawler starting to crawl")
    (crawl-votes (unseen-votes))
    (println "Crawler sleeping for" (/ loop-delay 1000) "seconds")
    (Thread/sleep loop-delay)))
