(ns advent-2024-clojure.day03-transducer
  (:require [abyala.advent-utils-clojure.core :as c]))

(defn parse-instruction [instruction]
  (or ({"do()" :enable, "don't()" :disable} instruction)
      (apply * (c/split-longs instruction))))

(defn shutoff-channel
  ([] (shutoff-channel :enable :disable))
  ([enable-token disable-token]
   (fn [xf]
     (let [enabled? (volatile! true)]
       (fn
         ([] (xf))
         ([result] (xf result))
         ([result input]
          (cond (= input enable-token) (do (vreset! enabled? true)
                                           (xf result))
                (= input disable-token) (do (vreset! enabled? false)
                                            (xf result))
                @enabled? (xf result input)
                :else (xf result))))))))

(defn solve [regex input]
  (transduce (comp (map parse-instruction) (shutoff-channel)) + (re-seq regex input)))

(defn part1 [input] (solve #"mul\(\d+,\d+\)" input))
(defn part2 [input] (solve #"mul\(\d+,\d+\)|do\(\)|don't\(\)" input))
