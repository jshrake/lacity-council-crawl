(ns city-council-scraper.core
  (:require [net.cgrand.enlive-html :as html])
  (:require [clj-time.format])
  (:gen-class))

(defn fetch-url [url]
  "Fetches the enlive parsed html corresponding to the url"
  (html/html-resource (java.net.URL. url)))

(defn vote-details-url [vote-id]
  "Returns the url for a specific city council vote"
  (str "http://cityclerk.lacity.org/cvvs/search/votedetails.cfm?voteid=" vote-id))

(defn vote-details-html [vote-id]
  "Returns the enlive parsed html for a specific city council vote"
  (fetch-url (vote-details-url vote-id)))


;; We define some selectors for extracting specific data from the vote-details page
;; The selectors are hand constructed from the firefox inspector developer tool

(defn nth-row [n]
  "Returns an enlive selector for selecting the nth row of a table"
  [:tr (html/nth-child n)])

(defn nth-col [n]
  "Returns an enlive selector for selecting the nth column of a table"
  [:td (html/nth-child n)])

(def meeting-date-selector 
  "Unique enlive DOM selector for the vote-details-html meeting date"
;.tablebg > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2)
  [:.tablebg [:table (html/nth-child 1)] (nth-row 1) (nth-row 2) (nth-row 1) (nth-col 2)])

(def meeting-type-selector
  "Unique enlive DOM selector for the vote-details-html meeting type"
;.tablebg > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(2)
 [[:table (html/nth-child 1)] (nth-row 1) (nth-row 2) (nth-row 2) (nth-col 2)])

(def council-file-number-selector
  "Unique enlive DOM selector for the vote-details-html council file number"
;.tablebg > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(10) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2)
  [[:table (html/nth-child 1)] (nth-row 1) (nth-row 10) (nth-row 1) (nth-col 2)])

(def item-description-selector
  "Unique enlive DOM selector for the vote-details-html council file number"
;span.mytext
  [:span.mytext])

; Districs are listed as a comma-delimited list
; Example with multiple districts: http://cityclerk.lacity.org/cvvs/search/votedetails.cfm?voteid=59203
(def pertinent-district-selector
  "Unique enlive DOM selector for the vote-details-html pertinent district list"
;.tablebg > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(18) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(1)
  [[:table (html/nth-child 1)] (nth-row 1) (nth-row 18) (nth-row 2) (nth-col 1)])

; For acquiring the council member vote detail,
; abuse the evenclass/oddclass class tags on the table rows
(def voter-row-selector [#{:tr.evenclass :tr.oddclass}])
(def voter-names-selector (conj voter-row-selector (nth-col 1)))
(def voter-districts-selector (conj voter-row-selector (nth-col 2)))
(def voter-votes-selector (conj voter-row-selector (nth-col 3)))

(defn select-first [node selector]
  "Extract the html/text content from the first item in the selected list"
  (html/text (first (html/select node selector))))

(def meeting-date-formatter
  "The format of the meeting date"
  (clj-time.format/formatter "EEEE MMMM dd, yyyy"))

(defn to-int [s]
  (Integer. (re-find #"\d+" s)))

(def parse-pertinent-districts
  "Parses the pertinent districts string to a list of ints.
   Specifically converts comma-delimited string: 'CD1,CD11,CD5' -> '(1 11 5)"
  (comp 
    (partial map to-int)
    (partial filter #(not= "none" (clojure.string/lower-case %)))
    (partial map clojure.string/trim)
    #(clojure.string/split % #",")))

(defn extract-data [node]
  (let [s (partial select-first node)
        voter-names (map (comp clojure.string/lower-case html/text) (html/select node voter-names-selector))
        voter-districts (map (comp to-int html/text) (html/select node voter-districts-selector))
        voter-votes (map (comp keyword clojure.string/lower-case clojure.string/trim html/text) (html/select node voter-votes-selector))
        voter-data (map list voter-names voter-districts voter-votes)]
  {:date (clj-time.format/parse meeting-date-formatter (s meeting-date-selector))
   :type (keyword (clojure.string/lower-case(s meeting-type-selector)))
   :file-number (s council-file-number-selector)
   :description (s item-description-selector)
   :pertinent-districts (parse-pertinent-districts (s pertinent-district-selector))
   :votes (map (partial zipmap [:name :district :vote]) voter-data)}))

(defn fetch-vote-html
  [id]
  (println "Fetching vote html for vote-id:" id)
  id)

(defn extract-vote-data
  [id]
  (println "Fetching vote-id:" id)
  id)

(defn store-vote-data
  [data]
  (println "Storing data:" data)
  true)

;; Crawling juice
;; 0. Determine the maximum vote id
;; This can be done by performing a binary search on the votedetails resource of the cityclerk.lacity.org host
;; When fetching an id that dosn't exist, the server returns a response header that contains server_error: true
;; The maximum vote id is between 1 and N (For some large N, N ~ 1 billion)
;; To find it, fetch urls and binary search until you find the largest url that doesn't return a server error
;; This should happen in no more than log_2 (N) requests (N ~ 1 billion, this is ~30 requests)

;; Once we've acquired the maximum vote id M, we want to ensure we've scraped all vote ids between 1 and M
;; Take a set difference between (set (range 1 M)) and the votes in our databse -- these are the new votes to fetch

;; For each vote to fetch
;; Fetch it -> Process it -> Store it in our DB
;; Delay by amount specified in robots.txt (10 seconds)

;; This process can go on indefinitely (perhaps with delays of 5-10 minutes between cycles)
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
       (fetch-vote-html vote)
       extract-vote-data
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
