(ns advent-2024-clojure.day13
  (:require [abyala.advent-utils-clojure.core :as c]))

(def a-cost 3)
(def b-cost 1)

(defn parse-machine [lines]
  (let [[a-str b-str prize-string] lines
        [a-x a-y] (c/split-longs a-str)
        [b-x b-y] (c/split-longs b-str)
        [prize-x prize-y] (c/split-longs prize-string)]
    {:ax a-x, :ay a-y, :bx b-x :by b-y :prizex prize-x :prizey prize-y}))

(defn parse-input [input]
  (map parse-machine (c/split-blank-line-groups input)))

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