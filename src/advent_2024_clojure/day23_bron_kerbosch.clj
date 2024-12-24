(ns advent-2024-clojure.day23-bron-kerbosch
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]))

(defn parse-connections [input]
  (reduce (fn [acc [a b]] (-> acc
                              (update a (fnil conj (sorted-set)) b)
                              (update b (fnil conj (sorted-set)) a)))
          {}
          (partition 2 (re-seq #"\w+" input))))

(defn maximal-cliques
  ([connection-map] (maximal-cliques connection-map (keys connection-map)))
  ([neighbor-fn vertices]
   (letfn [(cliques [results r p x]
             (if (every? empty? [p x])
               (conj results r)
               (first (reduce (fn [[results' p' x'] v]
                                [(cliques results'
                                          (conj r v)
                                          (set/intersection p' (neighbor-fn v))
                                          (set/intersection x' (neighbor-fn v)))
                                 (disj p' v)
                                 (conj x' v)])
                              [results p x]
                              p))))]
     (cliques () #{} (set vertices) #{}))))

(defn part1 [input]
  (->> (parse-connections input)
       (maximal-cliques)
       (mapcat (partial c/unique-combinations 3))
       set
       (filter (partial some #(str/starts-with? % "t")))
       count))

(defn part2 [input]
  (->> (parse-connections input)
       (maximal-cliques)
       (sort-by count)
       last
       sort
       (str/join ",")))
