(ns advent-2024-clojure.day18
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]
            [abyala.advent-utils-clojure.search :as s]))

(defn parse-input [input] (partition 2 (c/split-longs input)))

(defn path-size [max-box corrupted]
  (let [corrupted-set (set corrupted)
        target [max-box max-box]]
    (s/breadth-first-stateful (fn [seen [p len]] (if (= p target)
                                                   (s/done-searching len)
                                                   (let [points' (remove (fn [pair]
                                                                           (or (not-every? #(<= 0 % max-box) pair)
                                                                               (corrupted-set pair)
                                                                               (seen pair)))
                                                                         (p/neighbors p))]
                                                     (s/keep-searching (apply conj seen points')
                                                                       (mapv vector points' (repeat (inc len)))))))
                              #{p/origin}
                              [[p/origin 0]])))

(defn part1 [max-box num-steps input]
  (path-size max-box (take num-steps (parse-input input))))

(defn part2 [max-box input]
  (let [boxes (parse-input input)]
    (loop [low 0, high (dec (count boxes))]
      (if (= low high)
        (str/join "," (nth boxes (dec high)))
        (let [pivot (quot (+ low high) 2)]
          (if (path-size max-box (take pivot boxes))
            (recur (inc pivot) high)
            (recur low pivot)))))))
