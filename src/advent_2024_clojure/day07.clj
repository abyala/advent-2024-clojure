(ns advent-2024-clojure.day07
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.search :as search]))

(defn solveable? [commands nums]
  (let [[target a b & c] nums]
    (search/depth-first (list [a b c])
                        (fn [[v1 v2 [v3 & v4]]]
                          (when (and v2 (<= v1 target)) (map #(vector (% v1 v2) v3 v4) commands)))
                        (fn [[v1 v2]] (and (= v1 target) (nil? v2))))))

(defn solveable? [commands nums]
  (let [[target & others] nums]
    (some? (search/breadth-first [[target (reverse others)]]
                           (fn [[a [b & c]]]
                             (println "Looking at" a b c)
                             (when b (->> (map #(% a b) commands)
                                          (filter int?)
                                          (remove neg-int?)
                                          (map #(vector % c)))))
                           #(and (zero? (first %)) (nil? (second %)))))

    #_(zero? (reduce (fn [a b] (if (some #(let [v (% a b)]
                                            (and (int? v) (>= v 0))) commands)
                                 ))
                     target
                     (reverse others)))
    )

  )

(defn solve [commands input]
  (transduce (comp (map c/split-longs)
                   (filter (partial solveable? commands))
                   (map first))
             + (str/split-lines input)))

(defn solve [commands input]
  (->> (str/split-lines input)
       (pmap #(let [nums (c/split-longs %)] (if (solveable? commands nums) (first nums) 0)))
       (apply +)))

(defn part1 [input] (solve [+ *] input))
(defn part2 [input] (solve [+ * (comp parse-long str)] input))
(defn part1 [input] (solve [- /] input))
(defn part2 [input] (solve [- / #(when (str/ends-with? (str %1) (str %2))
                                   (parse-long (subs (str %1) 0 (count (str %2)))))]  input))

(comment
  (map #(% 156 6) [#(when (str/ends-with? (str %1) (str %2))
                      (parse-long (subs (str %1) 0 (count (str %2)))))]
       [- / (fn [v1 v2]
              (let [[s1 s2] (map str [v1 v2])]
                (when (str/ends-with? s1 s2)
                  (parse-long (subs s1 0 (- (count s1) (count s2)))))))])
  )