;;
;; # syobocal.irc
;; 
;; irc client of syoboi calendar.
;; it produce message-handling interface as dynamic bining.
;; 
;; #  Example of Usage
;; 
;; (use '[syobocal.irc :as irc])
;; (def IRC_CONF
;;      {:server {:name "irc.livedoor.ne.jp" :port 6667}
;;       :user   {:name "syobocalbot" :nick "syobocalbot"}})
;; 
;; (defn my-handler [conn who chan msg]
;;      (irc/notice conn chan (str "echo : " msg)))
;; (def irc/*irc-message-handler* my-handler)
;; (def node (irc/connect (:server IRC_CONF)))
;;
;; (irc/login node (:user IRC_CONF))
;; (irc/join node "#test")
;; (irc/privmsg node "#test" "hello world!")
;; (irc/notice node "#test" "this is notice message")
;; (irc/part node "#test")
;; (irc/quit node)
;;

(ns syobocal.irc
    (:import [java.net Socket]
            [java.io PrintWriter OutputStreamWriter InputStreamReader BufferedReader]))

(def IRC
    {:freenode {:name "irc.livedoor.ne.jp" :port 6667}
    :user {:name "animebot" :nick "animebot"}
    :encode "ISO-2022-JP"})

(declare conn-handler notice)

(defn message-handler
    "this is example of message handler which received"
    [conn who chan msg]
    (notice conn chan (str "WHO:" who ",CHAN:" chan ",MSG:" msg)))

(declare ^:dynamic *irc-message-handler*)

;(def ;^:dynamic
;    ^{:dynamic true}
;    *irc-message-handler*
;    "*irc-message-handler* is dynamic binding.
;    you could define any of your original funcs.
;    
;    For example::
;        (defn echo-message-handler
;            [conn who chan msg]
;            (notice conn chan (str \"echo : \" msg)))
;
;        (binding [*irc-message-handler* your-message-handler])
;    "
;    message-handler)

(defn connect
    "create irc socket
    usage : (connect {:name 'servername' :port 6667})
    "
    [server *irc-message-handler*]
    (let [
        socket (Socket. (:name server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket) (:encode IRC)))
        out (PrintWriter. (OutputStreamWriter. (.getOutputStream socket) (:encode IRC)))
        conn (ref {:in in :out out})]

        (doto (Thread. #(conn-handler conn *irc-message-handler*)) (.start))
        conn))

(defn write
    "write to irc socket connection"
    [conn msg]
    (doto (:out @conn)
        (.println (str msg "\r"))
        (.flush)))

;; 
;; irc commands
;; 

(defn login
    "login irc"
    [conn user]
    (write conn (str "NICK " (:nick user)))
    (write conn (str "USER " (:nick user) " 0 * :" (:name user))))

(defn join
    "join channel"
    [conn channel]
    (write conn (str "JOIN " channel)))

(defn privmsg
    "put private message"
    [conn chan msg]
    (write conn (str "PRIVMSG " chan " " msg)))

(defn quit
    "quit from irc"
    [conn]
    (write conn (str "QUIT")))

(defn part
    "part from irc channel"
    ([conn chan]
        (write conn (str "PART " chan)))
    ([conn]
        (write conn (str "PARTALL"))))

(defn notice
    "notice to irc channel"
    [conn chan msg]
    (do
    (println (str "msg " msg))
    (write conn (str "NOTICE " chan " " msg)))
    )

;;
;; event handler
;;

(defn message-parse
    "get message and return [who channel message]"
    [msg]
    (let [parsed (re-find #"^:([^!]+)!.*PRIVMSG\s+(\S+)\s+:(\S+)" msg)]
        (if parsed (rest parsed) parsed)))

(defn conn-handler
    "handle socket connection.
    it controles input message handling.
    irc-message-handler is dynamic and exportable with another package"
    [conn handler]
    (while (nil? (:exit @conn))
        (let [msg (.readLine (:in @conn))
              whochanmsg (message-parse msg)]
            (println msg)
            (cond
                (re-find #"^ERROR :Closing Link:" msg) 
                   (dosync (alter conn merge {:exit true}))
                (re-find #"^PING" msg)
                    (write conn (str "PONG " (re-find #":.*" msg)))
                whochanmsg
                    (let [who (first whochanmsg)
                          chan (second whochanmsg)
                          msg (last whochanmsg)]
                          (handler conn who chan msg))
               :else false 
                ))))
 
