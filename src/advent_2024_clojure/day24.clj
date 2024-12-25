(ns advent-2024-clojure.day24
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]))

(defn parse-input [input]
  (let [[wire-str gate-str] (c/split-blank-line-groups input)]
    {:wires (reduce #(let [[wire v] (re-seq #"\p{Alnum}+" %2)]
                       (assoc %1 wire (parse-long v)))
                    {}
                    wire-str)
     :gates (mapv #(vec (re-seq #"\p{Alnum}+" %)) gate-str)}))

(defn process-gates [state]
  (let [{:keys [wires gates]} state]
    (if (seq gates)
      (let [state' (reduce (fn [acc [g1 op g2 g3 :as gate]]
                             (let [[v1 v2] (map wires [g1 g2])]
                               (if (every? some? [v1 v2])
                                 (assoc-in acc [:wires g3] (({"AND" bit-and "OR" bit-or "XOR" bit-xor} op)
                                                            v1 v2))
                                 (update acc :gates conj gate))))
                           (assoc state :gates ())
                           gates)]
        (when (not= state state')
          (recur state')))
      state)))

(defn process-gates [state]
  (let [{:keys [wires gates]} state]
    (if (seq gates)
      (recur (reduce (fn [acc [g1 op g2 g3 :as gate]]
                       (let [[v1 v2] (map wires [g1 g2])]
                         (if (every? some? [v1 v2])
                           (assoc-in acc [:wires g3] (({"AND" bit-and "OR" bit-or "XOR" bit-xor} op)
                                                      v1 v2))
                           (update acc :gates conj gate))))
                     (assoc state :gates ())
                     gates))
      state)))

(defn extract-number [state label]
  (let [wires (:wires state)
        fmt (str label "%02d")]
    (reduce (fn [acc n]
              (if-some [v (wires (format fmt n))]
                (+ acc (bit-shift-left v n))
                (reduced acc)))
            0
            (range))))

(defn part1 [input] (-> input parse-input process-gates (extract-number "z")))

(defn swap-outputs [state [output-a output-b]]
  (let [gates (:gates state)
        index-a (c/index-of-first #(= output-a (last %)) gates)
        index-b (c/index-of-first #(= output-b (last %)) gates)]
    (when (and index-a index-b)
      (-> state
          (assoc-in [:gates index-a 3] output-b)
          (assoc-in [:gates index-b 3] output-a)))))

(defn print-graphviz [state]
  (let [gates (:gates state)
        z (str/join " -> " (sort (keep #(when (str/starts-with? (last %) "z") (last %)) gates)))
        x (str/replace z "z" "x")
        y (str/replace z "z" "y")
        {:keys [AND OR XOR]} (into {}
                                   (map (fn [[op op-gates]] [(keyword op) (map last op-gates)])
                                        (group-by #(nth % 1) gates)))]
    (str "\ndigraph G {\n"
         "  subgraph {\n"
         "    node [style=filled,color=lightgreen]\n"
         "    " z "\n"
         "  }\n"
         "  subgraph {\n"
         "    node [style=filled,color=gray]\n"
         "    " x "\n"
         "  }\n"
         "  subgraph {\n"
         "    node [style=filled,color=gray]\n"
         "    " y "\n"
         "  }\n"
         "  subgraph {\n"
         "    node [style=filled,color=pink]\n"
         "    " (str/join " " AND) "\n"
         "  }\n"
         "  subgraph {\n"
         "    node [style=filled,color=yellow]\n"
         "    " (str/join " " OR) "\n"
         "  }\n"
         "  subgraph {\n"
         "    node [style=filled,color=lightblue]\n"
         "    " (str/join " " XOR) "\n"
         "  }\n"
         (apply str (map (fn [[g1 _ g2 g3]] (str "    " g1 "-> " g3 "\n    " g2 " -> " g3 "\n")) gates))
         "}\n")))

(def part2-swaps [["gbs" "z29"] ["z22" "hwq"] ["wrm" "wss"] ["z08" "thm"]])

(defn part2 [input]
  (println (print-graphviz (reduce swap-outputs
                                   (parse-input input)
                                   part2-swaps)))
  (str/join "," (sort (flatten part2-swaps))))
