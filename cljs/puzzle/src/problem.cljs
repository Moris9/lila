(ns lichess.puzzle
  (:require [dommy.core :as dommy]
            [cljs.core.async :as async :refer [chan <! >! alts! put! close!]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn log! [& args] (.log js/console (apply pr-str args)))
(defn log-obj! [obj] (.log js/console obj))

(def static-domain (str "http://" (clojure.string/replace (.-domain js/document) #"^\w+" "static")))
(def puzzle-elem (sel1 "#puzzle"))
(def chessboard-elem (sel1 "#chessboard"))
(def initial-fen (dommy/attr chessboard-elem "data-fen"))
(def initial-move (dommy/attr chessboard-elem "data-move"))
(def lines (js->clj (js/JSON.parse (dommy/attr chessboard-elem "data-lines"))))
(def drop-chan (chan))
(def animation-delay 300)
(def chess (new js/Chess initial-fen))

(defn playing [] (dommy/has-class? puzzle-elem "playing"))

(defn apply-move
  ([orig, dest] (.move chess (clj->js {:from orig :to dest})))
  ([move] (let [[a, b, c, d] (seq move)] (apply-move (str a b) (str c d)))))

(defn delay-chan [fun duration] (let [ch (chan)] (js/setTimeout #(put! ch (or (fun) true)) duration) ch))
(defn await-chan [value duration] (let [ch (chan)] (js/setTimeout #(put! ch value) duration) ch))
(defn await-in [ch value duration] (js/setTimeout #(put! ch value) duration) ch)

(defn on-drop! [orig, dest]
  (if (and (playing) (apply-move orig dest))
    (put! drop-chan (str orig dest)) "snapback"))

(def chessboard
  (new js/ChessBoard "chessboard"
       (clj->js {:position initial-fen
                 :orientation (if (= "b" (.turn chess)) "white" "black")
                 :draggable true
                 :dropOffBoard "snapback"
                 :sparePieces false
                 :pieceTheme (str static-domain "/assets/images/chessboard/{piece}.png")
                 :moveSpeed animation-delay
                 :onDrop on-drop!})))

(defn set-position! [fen] (.position chessboard fen))

(defn try-move [progress move]
  (let [new-progress (conj progress move)
        new-lines (get-in lines new-progress)]
    (if new-lines [new-progress new-lines] false)))

(defn ai-play! [branch]
  (let [ch (chan) move (first (first branch))]
    (when-let [valid (apply-move move)]
      (go
        (set-position! (.fen chess))
        (await-in ch move (+ 50 animation-delay))))
    ch))

(defn end! [result] (-> puzzle-elem
                        (dommy/add-class! (str "complete " result))
                        (dommy/remove-class! "playing")))

(go
  (<! (await-chan true 1000))
  (apply-move initial-move)
  (set-position! (.fen chess))
  (dommy/add-class! puzzle-elem "playing")
  (loop [progress [] fen (.fen chess)]
    (let [move (<! drop-chan)]
      (if-let [[new-progress new-lines] (try-move progress move)]
        (do
          (set-position! (.fen chess))
          (<! (await-chan true (+ animation-delay 50)))
          (if (= new-lines "win")
            (end! "win")
            (let [aim (<! (ai-play! new-lines))]
              (if (= (get new-lines aim) "win")
                (end! "win")
                (recur (conj new-progress aim) (.fen chess))))))
        (do
          (.load chess fen)
          (<! (delay-chan #(set-position! fen) animation-delay))
          (<! (await-chan true (+ animation-delay 50)))
          (recur progress fen))))))
