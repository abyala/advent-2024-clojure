(ns advent-2024-clojure.day10
  (:require [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]))

(defn trail-heads [points]
  (keep #(when (zero? (second %)) (first %)) points))

(defn neighbors [points p]
  (filter #(= (points %) (inc (points p))) (p/neighbors p)))

(defn all-paths-to-destination [points]
  (reduce-kv (fn [acc p v] (assoc acc p (if (= v 9) (list p)
                                                    (mapcat acc (neighbors points p)))))
             {}
             (sort-by (comp - second) points)))

(defn solve [f input]
  (let [points (p/parse-to-char-coords-map c/parse-int-char input)
        reachable (all-paths-to-destination points)]
    (transduce (map (comp count f reachable)) + (trail-heads points))))

(defn part1 [input] (solve set input))
(defn part2 [input] (solve identity input))