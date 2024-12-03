# Day 03: Mull It Over

* [Problem statement](https://adventofcode.com/2024/day/3)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day03.clj)

## Intro

I know I said yesterday's puzzle was cute, but **this** puzzle was **really** cute! It might be one of the smallest
solutions I remember doing!

## Part One

We're given one big string and need to find all instructions of type `mul(x,y)`, which we then multiply and add
together. Because this is such a small solution, I'm just going to provide them both here - I don't even bother
putting the function body on separate lines from the `defn`!

```clojure
(defn multiply [instruction] (apply * (c/split-longs instruction)))

(defn part1 [input] (transduce (map multiply) + (re-seq #"mul\(\d+,\d+\)" input)))
```

The `multiply` function takes in the entire instruction (the whole `mul(x,y)`) and returns the result of multiplying
the numeric strings together. We use `split-longs` once again to pull out the two numbers within the instruction,
thus avoiding needing to do a regex or looking for the parentheses and comma, and then `(apply * ...)` multiplies them
together.

Then `part1` uses a transducer, surprise surprise. We start with `re-seq` to return a sequence of all matching regular
expressions from the input, where the regex looks for an uncorreupted multiplication instrunction. Then the transducer
calls `multiply` on each instruction, and adds the values together. What could be simpler?

## Part Two

For part two, we need to handle the adder moving from an enabled state to disabled and back again, based on when we
find the instructions `do()` and `don't()`, complete with the apostrophe. This amounts to a single `reduce` function
call!

```clojure
(defn part2 [input]
  (second (reduce (fn [[enabled? _ :as acc] instruction]
                    (cond
                      (str/starts-with? instruction "don't") (assoc acc 0 false)
                      (str/starts-with? instruction "do") (assoc acc 0 true)
                      enabled? (update acc 1 + (multiply instruction))
                      :else acc))
                  [true 0]
                  (re-seq #"mul\(\d+,\d+\)|do\(\)|don't\(\)" input))))
```

First, we expand our regex in the `re-seq` function to support all three possible instruction types. Then, we run a 
reducer with an accumulated state of `[enabled-or-disabled sum]`, starting as `[true 0]`. Within the reduction function,
we use one of my favorite syntaxes - `[enabled? _ :as acc]`, which destructures the accumulator to pull out the first
parameter as `enabled?`, but the `:as` keyword allows us to still keep a binding for the entire thing. Then it's a
simple matter of determining which instruction we're looking at, and how to update the accumulated state. At the very
end, we call `second` because we don't want to return the accumulated state, just the accumulated sum.

Nice! I'm submitting this code with the full knowledge that sometime tomorrow, I will be creating a stateful transducer
for reasons unclear to me. I guarantee the result will be much larger than this code!