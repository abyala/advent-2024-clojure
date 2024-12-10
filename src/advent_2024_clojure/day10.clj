(ns advent-2024-clojure.day10
  (:require [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]))

(defn parse-input [input]
  (let [points (p/parse-to-char-coords-map c/parse-int-char input)]
    (reduce-kv (fn [acc p c] (cond-> (assoc-in acc [:outgoing p] (filter #(= (points %) (inc c))
                                                                         (p/neighbors p)))
                                     (= c 0) (update :trail-heads conj p)))
               {:points points, :outgoing {}, :trail-heads ()}
               points)))

(defn all-paths-to-destination [data]
  (reduce-kv (fn [acc p v] (assoc acc p (if (= v 9) (list p)
                                                    (mapcat acc (get-in data [:outgoing p])))))
             {}
             (sort-by (comp - second) (:points data))))

(defn solve [f input]
  (let [data (parse-input input)
        reachable (all-paths-to-destination data)]
    (transduce (map (comp count f reachable)) + (:trail-heads data))))

(defn part1 [input] (solve set input))
(defn part2 [input] (solve identity input))