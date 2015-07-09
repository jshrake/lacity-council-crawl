(ns city-council-scraper.scrape
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

