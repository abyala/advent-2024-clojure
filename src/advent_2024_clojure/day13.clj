(ns advent-2024-clojure.day13
  (:require [abyala.advent-utils-clojure.core :as c]))

(def a-cost 3)
(def b-cost 1)

(defn parse-input [input]
  (map #(zipmap [:ax :ay :bx :by :prizex :prizey] (c/split-longs %))
       (c/split-by-blank-lines input)))

(defn cheapest-cost [machine]
  (let [{:keys [ax ay bx by prizex prizey]} machine
        num-b (/ (- (* prizey ax) (* prizex ay))
                 (- (* by ax) (* bx ay)))
        num-a (/ (- prizex (* num-b bx))
                 ax)]
    (when (every? int? [num-a num-b])
      (+ (* num-a a-cost) (* num-b b-cost)))))

(defn apply-adjustment [machine]
  (merge-with + machine {:prizex 10000000000000, :prizey 10000000000000}))

(defn solve [map-fn input]
  (transduce (comp (map map-fn) (keep cheapest-cost)) + (parse-input input)))

(defn part1 [input] (solve identity input))
(defn part2 [input] (solve apply-adjustment input))