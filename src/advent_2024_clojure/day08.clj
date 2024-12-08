(ns advent-2024-clojure.day08
  (:require [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]))

(defn antinodes-beside [points [ant1 ant2]]
  (filter points [(p/move ant2 (p/coord-distance ant1 ant2))
                  (p/move ant1 (p/coord-distance ant2 ant1))]))

(defn antinodes-in-line [points [ant1 ant2]]
  (into (take-while points (iterate #(p/move % (p/coord-distance ant1 ant2)) ant2))
        (take-while points (iterate #(p/move % (p/coord-distance ant2 ant1)) ant1))))

(defn solve [f input]
  (let [points (p/parse-to-char-coords-map input)]
   (->> (dissoc (group-by second points) \.)
        (mapcat (comp c/unique-combinations (partial map first) second))
        (mapcat (partial f points))
        set
        count)))

(defn part1 [input] (solve antinodes-beside input))
(defn part2 [input] (solve antinodes-in-line input))
