(ns syobocal.core
    (:use [syobocal.rss :as rss]
        [syobocal.irc :as irc]))

(def CONFIG
    (load-string (slurp "config.clj")))

(def ^:dynamic *irc-connect* false)

(defn anime
    "anime information handle"
    []
    (let [dat (rss/rss-anime)]
        (rss/tvform-format dat)))

(defn irc-handler
    "
    handle irc response.
    ex.
        (anime)
        (anime :from \"22:00\" :to \"02:00\")
    "
    [conn who chan msg]
    (cond
        (re-find #"echo" msg)
            (do
                (println (str "echo [" chan msg "]"))
                (irc/notice conn chan (str "echo [" msg "]"))
            )
        (re-find #"^(.*)$" msg)
            (try 
                (irc/notice conn chan (load-string msg))
                (catch Exception e (println e)))
        :else true
        ))

;(defn irc-notice
;    [chan msg]
;    (irc/notice *irc-connect* chan msg))

(defn start
    "start to join irc.
    and reply syoboi calendar result."
    []
    (binding [syobocal.irc/*irc-message-handler* irc-handler]
    (let     [irc-connect (irc/connect (CONFIG :server) irc-handler *ns*)]
        (irc/login irc-connect (CONFIG :user))
        (map #(irc/join irc-connect %) (CONFIG :channel))
        )))

(defn -main
    "start to join irc.
    and reply syoboi calendar result."
    [& args]
    (start))

