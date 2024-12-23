# Day 22: Monkey Market

* [Problem statement](https://adventofcode.com/2024/day/22)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day22.clj)

## Intro

This puzzle seemed oddly straightforward to implement even though my algorithm is anything but efficient. Part two
takes about 25 seconds to complete, but I'm also writing this from my laptop that can't seem to even import libraries
from GitHub, so I'm not in a position to optimize right now!

## Problem Statement and Solution Approach

We're in a position of having to interact with the Monkey Exchange Market, doing some calculations to help buy and sell
the necessary number of bananas. In part 1, we get the instructions for how to take a starting number and generate a
sequence of secret numbers. We need to add up the 2000th secret number generated for each such starting seed value.
For this, we'll just generate an infinite sequence using lazy sequences, nothing fancy.

Then in part 2 we need to determine the best bid pattern our monkeys should use to get the most bananas. There's not
much to the algorithm, so I won't describe it right here.

## Part One

The main work we do in part one is creating an infinite sequence of secret numbers, based on an interestingly-worded
algorithm.

```clojure
(def mix bit-xor)
(defn prune [n] (rem n 16777216))

(defn secret-seq [seed]
  (let [secret (-> seed (* 64) (mix seed) prune)
        secret (-> secret (quot 32) (mix secret) prune)
        secret (-> secret (* 2048) (mix secret) prune)]
    (lazy-seq (cons seed (secret-seq secret)))))
```

First, we make definitions of `mix` and `prune`, since we'll use that for calculating the next secret, given the
wacky algorithm given. `prune` is simple - just `mod` the input value with the number `16777216`. Is there something
odd about that number that we can use for making the algorithm simpler? Maybe, I don't know. For the `mix` function,
there's a choice. We could always use `defn` with two arguments, and call `bit-xor` together. But in this case, since
`mix` and `bit-xor` are exactly the same thing, we'll just use `def` to bind the former function name to the latter
function.

Then the `secret-seq` function generates the infinite sequence of secrets, starting from an initial value, by way of a
recursive call and `lazy-seq`. Note that in the text description of `secret` and `prune`, they redefine the value of
`secret`, so in this case I mirrored this by rebinding `secret` after every `prune`. I did also do an implementation
using `reduce`, operating on the inputs `[[* 64] [quot 32][* 2048]]`, but it wasn't as easy to read and actually slowed
things down a little.

```clojure
(defn part1 [input]
  (transduce (map #(nth (secret-seq %) 2000)) + (c/split-longs input)))
```

Wrapping it all up, we'll `transduce` over the long values from the input string. For each one, we'll call
`secret-seq` and grab the 200th value, and then add them all up together.

## Part Two

This time, we're told that we need to give instructions to the monkeys on how to place their bids to maximize the
number of bananas they get. For each secret number, we look only at the ones digit, and we tell the monkey the sequence
of four digit diffs such that they choose the number of bananas at the end. We need to find the best such sequence.

I won't say that my solution is super clean, and there may be a "trick" to make this better but this is how we're going
to do it.

```clojure
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
```

The purpose of this function is to take in an initial secret, find the first 2000 following secrets, and return a map
of every 4-digit sequence of diffs, mapping to bananas found the first time that sequence occurs. This will all become
a `reduce` function, so let's start with the data to feed in to the `reduce`.

We want a sequence of every four diffs and their corresponding number of bananas. We'll start by taking the first 2000
secrets in the sequence, and then immediately mapping each value to its ones digit with `(map #(mod % 10) n)`. Then,
to avoid extra computations, we'll call `(partition 2 1 digits)` to find every pair of digits, and then we'll map those
pairs into a new tuple of `[diff b]`, where `diff` is the second value minus the first, and `b` is the second number.
Why? Because the final step will be to call `(partition 4 1)`, thus creating the final structure of 
`([diff0 n1] [diff1 n2] [diff2 n3] [diff3 n4])`.

With that all done, we call `reduce` on the sequence of 4-tuples. For each one, we call `(map first tuples)` to get the
four diffs in the format `(d0 d1 d2 d3)`; that will be the key for our lookup map. If the map doesn't have a value yet
for that sequence, then `assoc` it to the fourth value in the list. We only want the first lookup for each sequence.

Alright, so now `bids` takes in a starting `secret` and returns a map of `{4-diffs bananas}`. We're just about done.

```clojure
(defn part2 [input]
  (->> (c/split-longs input)
       (map bids)
       (apply merge-with +)
       vals
       (apply max)))
```

Now we split the input into each of its numbers using `split-longs`, and call `(map bids n)` to get a sequence of bid
maps. `(apply merge-with +)` combines them all by adding together their values for common keys; if two starting secrets
both result in bananas, we want to add them together. Finally, `vals` returns the sums of bananas, and `(apply max)`
returns the greatest number of them.