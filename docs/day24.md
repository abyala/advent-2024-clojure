# Day 24: Crossed Wires

* [Problem statement](https://adventofcode.com/2024/day/24)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day24.clj)

## Intro

Well this is one of those puzzles that I hate because you need know the little trick. Luckily, thanks once again to my
good friend [Todd Ginsberg](https://todd.ginsberg.com/post/advent-of-code/2024/day24/), I was able to skip the most
annoying parts and get back to the code. Part 1, though, was still fun!

## Problem Statement and Solution Approach

We're given a bunch of wires and AND, OR, and XOR gates, and our goal is to calculate all gates until every wire has
its value defined, and then calculate the binary value of the wires whose labels begin with a "z". Thankfully, each
wire only has a single gate in front of it, so we don't need to worry about wires changing values as gates power up.

## Part One

To start, let's parse the data. We want to take the input string, which is a series of lines for each wire, then a
blank line, and then a series of lines for each gate. Our goal is to make a map of
`{:wires {name value}, :gates ([w1 op w2 w3])}`, where `:wires` maps the name of each wire to its starting value, and
`:gates` is a sequence of every gate, represented simply as four strings.

```clojure
(defn parse-input [input]
  (let [[wire-str gate-str] (c/split-blank-line-groups input)]
    {:wires (reduce #(let [[wire v] (re-seq #"\p{Alnum}+" %2)]
                       (assoc %1 wire (parse-long v)))
                    {}
                    wire-str)
     :gates (mapv #(vec (re-seq #"\p{Alnum}+" %)) gate-str)}))
```

For each line of wires, I used the regular expression `"\p{Alnum}+"` for the POSIX pattern of all alphanumeric
characters. I believe this is the same as `"[a-zA-Z0-9]"` but why not use the real thing? For the wires, since each
line looks like `"x00: 1"`, we find the alphanumeric name of the wire first, and then the alphanumeric value of the
wire, which should always be 0 or 1, on which we call `parse-long` when placing it into the resulting map. For
`:gates`, we again just look at the alphanumeric values for the three wires and the operation, but we want both each
gate and the collection of gates to be vectors, since we'll be doing manipulations by index later on.

Now let's implement `process-gates`, which runs the circuit until all gates have produced their output.

```clojure
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
```

After destructuring the data, we check to see if there are any gates to process. If not, we return the `state` as it
came in. Otherwise, we make a recursive function call with `recur`, after running `reduce` on all gates. The starting
accumulator for the state clears out all gates; our reducing function will either apply the gate and discard it, or, if
its input wires aren't set yet, puts the gate back into the accumulated state. If both input wires are defined, as seen
with `(every? some? [v1 v2])`, then we associate in at location `[:wires g3]` the result of applying a bitwise and, or,
or xor to the two input wires.

Almost there. We need to be able to extract the binary value of the "z" wires from the resting state of the system. 
The function supports outputting the value of "x", "y", "z", or any other wire names, based on what we theoretically
need to do for part 2; I never actually used it for the "x" and "y" values, and I could, but I'm not going to bother.

```clojure
(defn extract-number [state label]
  (let [wires (:wires state)
        fmt (str label "%02d")]
    (reduce (fn [acc n]
              (if-some [v (wires (format fmt n))]
                (+ acc (bit-shift-left v n))
                (reduced acc)))
            0
            (range))))
```

The important part is the binding `fmt`, which gets set to `(str label "%02d")`, so looking for the "z" labels requires
applying `(format "z%02d" num)` for each num. That zero pads each number. The `reduce` function looks at every wire,
starting at `z00`, and adds the bit-shifted value of each wire on top of the accumulated amount. When the next wire
name doesn't map to the state, we use `(reduced acc)` to short-circuit the infinite loop.

```clojure
(defn part1 [input] (-> input parse-input process-gates (extract-number "z")))
```

To finish part 1, we parse the input, process all gates, and extract the value of the "z" gates.

## Part Two

Seriously, read Todd's write-up. I'm not going to explain what to do or why. I'll just explain my code.

```clojure
(defn swap-outputs [state [output-a output-b]]
  (let [gates (:gates state)
        index-a (c/index-of-first #(= output-a (last %)) gates)
        index-b (c/index-of-first #(= output-b (last %)) gates)]
    (when (and index-a index-b)
      (-> state
          (assoc-in [:gates index-a 3] output-b)
          (assoc-in [:gates index-b 3] output-a)))))
```

This function takes in the `state` and the names of the two output wires we want to swap, and we use `c/index-of-first`
to figure out which gate has each wire as its output. Once they're found, there are two `assoc-in` calls on those gates,
binding the value at index `3` (the fourth value in each gate vector) to the other's output gate name.

No pride here. The following function prints out the exact Graphviz code that Todd created, so that I could manually
inspect the diagram in a GraphViz viewer to determine which two gate outputs to swap. Truly, the only change between
my code and his is that I used light green instead of regular green.

```clojure
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
```

So here's my crappy part 2 code.

```clojure
(def part2-swaps [["gbs" "z29"] ["z22" "hwq"] ["wrm" "wss"] ["z08" "thm"]])

(defn part2 [input]
  (println (print-graphviz (reduce swap-outputs
                                   (parse-input input)
                                   part2-swaps)))
  (str/join "," (sort (flatten part2-swaps))))
```

I've hard-coded the four pairs of gates I had to swap, and then `part2` swaps them, prints the values out to `stdout`,
and then just joins the values to get the proper output.

Bleach. Disappointing puzzle.
