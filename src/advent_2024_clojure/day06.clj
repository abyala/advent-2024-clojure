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

(defn guard-path [points guard-loc guard-dir]
  (letfn [(next-step [guard-loc, dir]
            (let [loc' (p/move guard-loc dir)]
              (case (points loc')
                :space (lazy-seq (cons [guard-loc dir] (next-step loc' dir)))
                :block (lazy-seq (cons [guard-loc dir] (next-step guard-loc (turn-right dir))))
                (list [guard-loc dir]))))]
    (next-step guard-loc guard-dir)))

(defn guard-stuck? [points guard-loc guard-dir prev-path]
  (true? (reduce (fn [seen' loc-dir] (if (seen' loc-dir) (reduced true) (conj seen' loc-dir)))
                 (set prev-path)
                 (guard-path points guard-loc guard-dir))))

(defn possible-obstacles [points guard]
  (->> (guard-path points guard north)
       (partition 2 1)
       (reduce (fn [[options prev-path seen :as acc] [[loc0 dir0] [loc1 _]]]
                 (let [path' (conj prev-path [loc0 dir0])]
                   (if (seen loc1) (assoc acc 1 path')
                                   [(conj options {:guard-loc loc0, :guard-dir dir0, :obstacle loc1, :prev-path prev-path})
                                    path'
                                    (conj seen loc1)])))
               [() [] #{}])
       first))

(defn part1 [input]
  (let [{:keys [points guard]} (parse-input input)]
    (->> (guard-path points guard north) (map first) set count)))

(defn part2 [input]
  (let [{:keys [points guard]} (parse-input input)]
    (c/count-when (fn [{:keys [guard-loc guard-dir obstacle prev-path]}]
                    (guard-stuck? (assoc points obstacle :block) guard-loc guard-dir prev-path))
                  (possible-obstacles points guard))))