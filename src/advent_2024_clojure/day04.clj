(ns advent-2024-clojure.day04
  (:require [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]))

(def diag-directions [[1 1] [1 -1] [-1 -1] [-1 1]])
(def all-directions (concat p/cardinal-directions diag-directions))
(def x-mas-neighbors #{[\M \M \S \S] [\M \S \S \M] [\S \S \M \M] [\S \M \M \S]})

(defn four-paths [coord]
  (map (fn [dir] (take 4 (iterate #(p/move % dir) coord))) all-directions))

(defn num-xmas-paths [points point]
  (if (= (points point) \X)
    (c/count-when #(= [\X \M \A \S] (map points %)) (four-paths point))
    0))

(defn x-mas? [points point]
  (and (= (points point) \A)
       (x-mas-neighbors (map (comp points #(p/move point %)) diag-directions))))

(defn part1 [input]
  (let [points (p/parse-to-char-coords-map input)]
    (c/sum (partial num-xmas-paths points) (keys points))))

(defn part2 [input]
  (let [points (p/parse-to-char-coords-map input)]
    (c/count-when (partial x-mas? points) (keys points))))