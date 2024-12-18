(ns advent-2024-clojure.day17
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]))

(defn parse-input [input]
  (let [[a b c & program] (c/split-longs input)]
    {:a a :b b :c c :program (vec program) :ip 0 :output []}))

(defn combo-operand [computer operand]
  (case operand 4 (:a computer)
                5 (:b computer)
                6 (:c computer)
                operand))

(defn move-ip [computer] (update computer :ip + 2))

(defn divide [register computer operand]
  #_(println "\tDividing" (:a computer) "to 2^" (combo-operand computer operand)
             "to make" (long (quot (:a computer) (Math/pow 2 (combo-operand computer operand)))))
  (-> computer
      (assoc register (long (quot (:a computer) (Math/pow 2 (combo-operand computer operand)))))
      (move-ip)))

(defn adv [computer operand] (divide :a computer operand))
(defn bdv [computer operand] (divide :b computer operand))
(defn cdv [computer operand] (divide :c computer operand))

(defn bxl [computer operand]
  (-> computer
      (update :b bit-xor operand)
      (move-ip)))

(defn bst [computer operand]
  (-> computer
      (assoc :b (mod (combo-operand computer operand) 8))
      (move-ip)))

(defn jnz [computer operand]
  (if (zero? (:a computer))
    (move-ip computer)
    (assoc computer :ip operand)))

(defn bxc [computer _]
  (-> computer
      (update :b bit-xor (:c computer))
      (move-ip)))

(defn out [computer operand]
  (-> computer
      (update :output conj (mod (combo-operand computer operand) 8))
      (move-ip)))

(defn next-command [computer]
  (let [{:keys [program ip]} computer]
    (when (< ip (count program))
      (let [[op operand] (map program [ip (inc ip)])]
        (([adv bxl bst jnz bxc out bdv cdv] op) computer operand)))))

(defn run-to-completion [computer]
  (last (take-while some? (iterate next-command computer))))

(defn print-output [computer] (str/join "," (:output computer)))

(defn part1 [input]
  (let [computer (parse-input input)]
    (print-output (run-to-completion computer))))
