(ns syobocal.core
    (:use [syobocal.scrape :as sc]
        [syobocal.irc :as irc]))

(def CONFIG
    {:server {:name "irc.livedoor.ne.jp" :port 6667}
    :user    {:name "animebot" :nick "animebot"}
    :channel ["#testes"]
    })

(defn anime
    "anime information handle"
    []
    (let [dat (sc/scrape-anime)]
        (sc/tvform-format dat)))

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
        (re-find #"^(anime.*)$" msg)
            (try 
                (irc/notice conn chan (read-string msg))
                (catch Exception e (println e)))
        :else true
        ))

(defn start
    "start to join irc.
    and reply syoboi calendar result."
    []
    (binding [syobocal.irc/*irc-message-handler* irc-handler]
        (let [node (irc/connect (CONFIG :server) irc-handler)]
            (irc/login node (CONFIG :user))
            (map #(irc/join node %) (CONFIG :channel))
            ))
            )

(defn -main
    "start to join irc.
    and reply syoboi calendar result."
    [& args]
    (start))

