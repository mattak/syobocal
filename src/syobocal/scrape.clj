;;
;; # syobocal.scrape
;; 
;; scrape from syoboi calendar rss feed.
;;
;; # Example of Usage
;; 
;; (def dat (scrape-rss))
;; (tvform-put dat)
;; 

(ns syobocal.scrape
    (:import [java.io ByteArrayInputStream]
        [java.io StringReader]
        [java.text SimpleDateFormat]
        [org.xml.sax InputSource])
    (:require [clj-http.client :as client]
        [clojure.java.io :as io]
        [clojure.xml :as xml]
        [clojure.zip :as zip]
        [clojure.data.zip.xml :as dzx]
        ))

(def SYOBOI
    {:rss "http://cal.syoboi.jp/rss.php"
    :rss2 "http://cal.syoboi.jp/rss2.php"
    :in-dateformat "yyyy-MM-dd'T'hh:mm:ss"
    :out-dateformat "hh:mm"
    :default-publisher #"(AT-X|TBS|TOKYO MX|NHK|MBS|テレビ東京|ニコニコ|アニマックス|フジ|tvk)"
    :title-maxlen 15
    })

(defn str2xmlzip
    "parse string as xml-zipped "
    [s]
    (zip/xml-zip (xml/parse (InputSource. (StringReader. s)))))

(defn date-parse
    "return Date object from rss datetime format such as 2012-10-12'T'12:09:10+09:00
    TODO: handle timezone +09:00"
    [datestr]
    (.parse (SimpleDateFormat. (SYOBOI :in-dateformat)) datestr))

(defn date-convert
    "convert rss datetime to time"
    [^String date-str]
    (.format
        (SimpleDateFormat. (SYOBOI :out-dateformat))
        (date-parse date-str)))

(defn str-shorten
    "shorten string as helloworld to hello..."
    [s maxlen]
    (str (subs s 0 maxlen) "..."))

;; rss data
;; --------
;; get rss from syoboi calendar site.
;; then parse and get links of channel.

(defn rss-get
    "extract rss element.
     it returns xml data"
    [url]
    (str2xmlzip (:body (client/get url))))

(defn rss-title
    "get title from rss xml-zip"
    [xmlzip]
    (dzx/xml-> xmlzip :item :title dzx/text))

(defn rss-publisher
    "get publisher from rss xml-zip"
    [xmlzip]
    (dzx/xml-> xmlzip :item :dc:publisher dzx/text))

(defn rss-starttime
    "get starttime from rss xml-zip"
    [xmlzip]
    (dzx/xml-> xmlzip :item :tv:feed :tv:startDatetime dzx/text))

(defn rss-endtime
    "get endtime from rss xml-zip"
    [xmlzip]
    (dzx/xml-> xmlzip :item :tv:feed :tv:endDatetime dzx/text))

(defn rss-genre
    "get genre from rss xml-zip"
    [xmlzip]
    (dzx/xml-> xmlzip :item :tv:feed :tv:genre dzx/text))

(defn rss-link
    "get link from rss xml-zip"
    [xmlzip]
    (dzx/xml-> xmlzip :item :link dzx/text))

;; parsed tvform
;; -------------

(defn tvform
    "restruct tvform from xml-ziped sequence to hashmap data structure
    it returns hash sequence which composed by title, genre, from, publisher, link."
    [xmlzip]
    (map
        #(hash-map :title %1 :genre %2 :from %3 :publisher %4 :link %5)
        (rss-title xmlzip)
        (rss-genre xmlzip)
        (rss-starttime xmlzip)
        (rss-publisher xmlzip)
        (rss-link xmlzip)))

(defmacro tvform-filter
    "filter as matched regular expression"
    [elem re-ex tvform]
    `(clojure.core/filter
        (clojure.core/fn [s#]
            (clojure.core/cond
                (clojure.core/re-find ~re-ex (s# ~elem)) true
                :else false))
        ~tvform))

(defn tvform-format
    "tostring tvform element"
    [elem]
    (format "%s [%s] %s~"
        (str-shorten (:title elem) (SYOBOI :title-maxlen))
        (:publisher elem)
        (date-convert (:from elem))))

(defn tvform-put
    "output formatted tvform."
    [tvform-lst]
    (clojure.string/join ",  "
        (map #(tvform-format %) tvform-lst)))


;; scrape html data
;; ----------------
;; get tv program data from program detail page.

(defn scrape-anime
    "get rss data from web and filter it"
    []
    (let [xmlzip (rss-get (SYOBOI :rss))
          formed (tvform xmlzip)
          cleaned (tvform-filter :genre #"アニメ$" formed)
          cleaned2 (tvform-filter :publisher (SYOBOI :default-publisher) cleaned)]
        cleaned2))

