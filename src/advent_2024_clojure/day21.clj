(ns advent-2024-clojure.day21
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]))

(def activate-button \A)
(def numeric-keypad (dissoc (p/parse-to-char-coords-map "789\n456\n123\nX0A") [0 3]))
(def directional-keypad (dissoc (p/parse-to-char-coords-map "X^A\n<v>") [0 0]))
(def dir-char {[0 -1] \^ [0 1] \v [-1 0] \< [1 0] \>})
(defn location-of [keypad v] ((set/map-invert keypad) v))

(def shortest-connections
  (memoize (fn [keypad start end]
             (let [[start-loc end-loc] (map (partial location-of keypad) [start end])]
               (loop [options [[start-loc #{start-loc} ""]], successes #{}]
                 (if-not (seq options)
                   successes
                   (let [[loc seen path] (first options)
                         options' (into (subvec options 1)
                                         (keep (fn [dir] (let [p' (p/move loc dir)]
                                                           (when (and (keypad p')
                                                                      (not (seen p')))
                                                             [p' (conj seen p') (str path (dir-char dir))])))
                                               p/cardinal-directions))]
                     (if (and (seq successes) (> (count path) (count (first successes))))
                       successes
                       (recur options' (if (= loc end-loc) (conj successes (str path activate-button)) successes))))))))))

(def path-cost
  (memoize (fn [keypad num-intermediates path]
             (if (zero? num-intermediates)
               (count path)
               (c/sum (fn [[from to]] (transduce (map (partial path-cost directional-keypad (dec num-intermediates)))
                                                 min Long/MAX_VALUE
                                                 (shortest-connections keypad from to)))
                      (partition 2 1 (str activate-button path)))))))

(defn complexity [num-intermediates code]
  (* (path-cost numeric-keypad (inc num-intermediates) code)
     (first (c/split-longs code))))

(defn solve [num-intermediates input]
  (transduce (map (partial complexity num-intermediates)) + (str/split-lines input)))

(defn part1 [input] (solve 2 input))
(defn part2 [input] (solve 25 input))
