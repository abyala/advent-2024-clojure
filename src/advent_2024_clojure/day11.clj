(ns advent-2024-clojure.day11
  (:require [abyala.advent-utils-clojure.core :as c]))

(defn blink [stone]
  (let [stone-str (str stone)
        len (count stone-str)
        pivot (quot len 2)]
    (cond (zero? stone) [1]
          (even? len) (map parse-long [(subs stone-str 0 pivot) (subs stone-str pivot)])
          :else [(* stone 2024)])))

(defn blink-to-the-future [stone blinks history]
  (let [key [stone blinks]]
    (cond
      (history key) history
      (zero? blinks) (assoc history key 1)
      :else (reduce (fn [acc stone'] (let [history' (blink-to-the-future stone' (dec blinks) acc)]
                                       (update history' key + (history' [stone' (dec blinks)]))))
                    (assoc history key 0)
                    (blink stone)))))

(defn solve [blinks input]
  (let [stones (c/split-longs input)
        history (reduce #(blink-to-the-future %2 blinks %1) {} stones)]
    (transduce (map #(history [% blinks])) + stones)))

(defn part1 [input] (solve 25 input))
(defn part2 [input] (solve 75 input))