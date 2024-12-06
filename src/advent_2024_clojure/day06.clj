(ns advent-2024-clojure.day06
  (:require [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]))

(def north [0 -1])
(def south [0 1])
(def west [-1 0])
(def east [1 0])

(defn parse-input [input]
  (let [points (p/parse-to-char-coords-map {\. :space \# :block \^ :guard} input)
        guard-pos (first (c/first-when #(= :guard (second %)) points))]
    {:points (assoc points guard-pos :space), :guard guard-pos}))

(defn turn-right [dir]
  ({north east, east south, south west, west north} dir))

(defn guard-path [points guard]
  (letfn [(next-step [guard-loc, dir]
            (let [loc' (p/move guard-loc dir)]
              (case (points loc')
                :space (lazy-seq (cons [guard-loc dir] (next-step loc' dir)))
                :block (lazy-seq (cons [guard-loc dir] (next-step guard-loc (turn-right dir))))
                (list [guard-loc dir]))))]
    (next-step guard north)))

(defn guard-stuck? [points guard]
  (true? (reduce (fn [seen loc-dir] (if (seen loc-dir) (reduced true) (conj seen loc-dir)))
                 #{}
                 (guard-path points guard))))

(defn part1 [input]
  (let [{:keys [points guard]} (parse-input input)]
    (->> (guard-path points guard) (map first) set count)))

(defn part2 [input]
  (let [{:keys [points guard]} (parse-input input)
        candidates (disj (set (map first (guard-path points guard))) guard)]
    (->> candidates
         (map #(assoc points % :block))
         (c/count-when #(guard-stuck? % guard)))))

; 53 seconds to start
