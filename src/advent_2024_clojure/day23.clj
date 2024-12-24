(ns advent-2024-clojure.day23
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]))

(defn parse-connections [input]
  (reduce (fn [acc [a b]] (-> acc
                              (update a (fnil conj (sorted-set)) b)
                              (update b (fnil conj (sorted-set)) a)))
          {}
          (partition 2 (re-seq #"\w+" input))))

(defn networks-with [connections n computer]
  (map #(set (conj % computer)) (c/unique-combinations n (connections computer))))

(defn shared-networks [connections n]
  (->> (keys connections)
       (mapcat (partial networks-with connections (dec n)))
       frequencies
       (filter #(= n (second %)))
       (map first)))

(defn part1 [input]
  (c/count-when (fn [network] (some #(str/starts-with? % "t") network))
                (shared-networks (parse-connections input) 3)))

(defn largest-network
  ([connections] (largest-network connections (-> connections vals first count inc)))
  ([connections n]
   (when (> n 1)
     (if-some [network (first (shared-networks connections n))]
       (str/join "," (sort network))
       (recur connections (dec n))))))

(defn part2 [input]
  (largest-network (parse-connections input)))
