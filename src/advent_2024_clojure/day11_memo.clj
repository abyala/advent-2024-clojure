(ns advent-2024-clojure.day11-memo
  (:require [abyala.advent-utils-clojure.core :as c]))

(defn blink [stone]
  (let [stone-str (str stone)
        len (count stone-str)
        pivot (quot len 2)]
    (cond (zero? stone) [1]
          (even? len) (map parse-long [(subs stone-str 0 pivot) (subs stone-str pivot)])
          :else [(* stone 2024)])))

(def num-future-stones
  (memoize (fn [stone blinks]
             (if (zero? blinks) 1 (transduce (map #(num-future-stones % (dec blinks))) + (blink stone))))))

(defn solve [blinks input]
  (transduce (map #(num-future-stones % blinks)) + (c/split-longs input)))

(defn part1 [input] (solve 25 input))
(defn part2 [input] (solve 75 input))