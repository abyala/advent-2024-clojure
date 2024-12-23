(ns advent-2024-clojure.day22
  (:require [abyala.advent-utils-clojure.core :as c]))

(def mix bit-xor)
(defn prune [n] (rem n 16777216))

(defn secret-seq [seed]
  (let [secret (-> seed (* 64) (mix seed) prune)
        secret (-> secret (quot 32) (mix secret) prune)
        secret (-> secret (* 2048) (mix secret) prune)]
    (lazy-seq (cons seed (secret-seq secret)))))

(defn part1 [input]
  (transduce (map #(nth (secret-seq %) 2000)) + (c/split-longs input)))

(defn bids [seed]
    (reduce #(let [k (map first %2)]
               (if (%1 k) %1 (assoc %1 k (second (last %2)))))
            {}
            (->> (secret-seq seed)
                 (take 2000)
                 (map #(mod % 10))
                 (partition 2 1)
                 (map (fn [[a b]] [(- b a) b]))
                 (partition 4 1))))

(defn part2 [input]
  (->> (c/split-longs input)
       (map bids)
       (apply merge-with +)
       vals
       (apply max)))