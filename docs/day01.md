# Day 01: Historian Hysteria

* [Problem statement](https://adventofcode.com/2024/day/1)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day01.clj)

## Intro

Weeee! It's Advent time again! I'm so excited to be coding again; if you know what's going on in my life, you know that
I don't get to do a whole lot of it these days. Anyway, let's dive in to a rather fun first day, full of mapping
transformations.

## Part One

We're given an input file that has two numbers on each line. We don't need to look at each as a line per se, but rather
work with the two lists of first- and second-column numbers. So it's time for our first `parse-input` function of the
season!

```clojure
(defn parse-pairs [input]
  (->> (str/split-lines input)
       (map c/split-longs)
       ((juxt (partial map first) (partial map second)))))
```

Fooled you! It's `parse-pairs`, not `parse-input`. Oh we're in for a ride this year for sure.

The goal of this function is to take in the input as one big string, and return a vector of two lists of numbers. 
Before we go too far, I'm making use of my [Advent Utils](https://github.com/abyala/advent-utils-clojure) repo 
again this year, and boy am I glad I refreshed my memory the other day about what's in the
[core workspace](https://github.com/abyala/advent-utils-clojure/blob/main/src/abyala/advent_utils_clojure/core.clj)!
We're going to use the `split-longs` function, which maps a string into a list of the parsed numbers contained within
it.

Armed with that helper function, `parse-pairs` isn't too tough. First we call `split-lines` to parse the input into
each line, and then `map` each line using `split-longs` to have a list of two-element numeric lists. But now we want to
return a single pair of numeric lists. Whenever we take one thing and return a vector of things from different
functions, we call on `juxt`. In this case, we'll go through each pair of numbers and return a list of calling `first`
on each pair and a list of calling `second` on each pair. Thus, `(parse-pairs "1 2\n3 4\n5 6")` returns
`[(1 3 5) (2 4 6)]`.

Now we're going to solve this part in two ways, because my avid readers know how much I treasure a good transducer.

### Solution One

```clojure
(defn part1 [input]
  (let [[a b] (map sort (parse-pairs input))]
    (apply + (map (comp abs -) b a))))
```

This solution isn't bad, and it's probably the more readable one. The idea is to sort each of the two lists, then take
the distances between them (absolute value of the difference), and add up those distances. We call `(map sort 
(parse-pairs input))` to sort each of the paired lists, which we immediately destructure into the sorted lists `a` and
`b`. Then we call `(map (comp abs -) b a)` to go through each paired value from `a` and `b`, calculate the difference
and then the absolute value using the composite function `(comp abs -)`. Remember that in Clojure, you have to read
composite functions backwards - the subtraction (second) happens before the absolute value (first). Finally, 
`(apply +)` sums up the differences.

### Solution Two

```clojure
(defn part1 [input]
  (transduce (map #(abs (apply - %)))
             +
             (apply map vector (map sort (parse-pairs input)))))
```

Let's break down the transducer solution. First we look at the third argument to `transduce`, which is the input
collection. We again call `(map sort (parse-pairs input))` but this time we feed the two sorted lists into
`(apply map vector)` to construct a list of 2-element vectors, connecting each number from the first list with its
sorted value in the second list. We  use the reducing function `(map #(abs (apply - %)))` on each pair, mapping it
to the result of calling the absolute value of the difference. Finally, we use the transformation function `+` to sum
up the values.

Both solutions work. I'm going to keep the second one since it's only a single expression.

Onward to part 2!

## Part Two

This part is actually simpler than the first part, I think. This time, we don't want to sort the lists. Instead, we
multiply each value in the first list by the number of times it appears in the second, and then add up those
products. Transducers to the rescue!

```clojure
(defn part2 [input]
  (let [[a b] (parse-pairs input)
        freqs (frequencies b)]
    (transduce (map #(* (or (freqs %) 0) %))
               +
               a)))
```

To start, we'll again call `parse-pairs` (without sorting) and destructure the results into the sequences `a` and `b`,
as we did in the first solution for part 1. Then we'll call `frequencies` on the second list, which returns a map of
each entry in the list, mapped to the number of times it appears in said list. Finally, we call our `transduce`
function, going over each value in the `a` list and multiplying it by its corresponding value in the frequencies map,
or `0` if it doesn't appear in the latter. Then we sum the values up again.

I thought about some funky solution of combining two maps somehow, since in theory we could end up with the same value
in the `a` list multiple times. But that only exists in the sample data and not the real puzzle data, so it was
unnecessary.

Anyway, nice and easy first day.

---

## Refactoring with "sum" helper function

I always love reading [Todd Ginsberg's](https://github.com/tginsberg/advent-2024-kotlin) solutions to Advent puzzles,
because I enjoy seeing a Kotlin master at work. He often showcases the power of the Kotlin standard library, which
simplifies code by putting common helper functions into a shared library. I think I'd like to do some of that this year,
but since the Clojure language is loathe to add "filler" functions, and I don't want to use someone's random work to
build my code, I'm going to do it myself.

So I bring you... the `sum` function!

```clojure
; This is in the abyala.advent-utils-clojure.core package
(defn sum
  "Sums the values in a collection. If a function `f` is provided, then map `f` to each value in the collection
  before adding them together."
  ([coll] (apply + coll))
  ([f coll] (transduce (map f) + coll)))
```

I'm adding this function to the [Advent Utils](https://github.com/abyala/advent-utils-clojure) repo for reuse, but essentially this is a cleaner way to look at
adding up a collection, including using an optional transformational function. With this function at our disposal, the
code looks even simpler.

```clojure
(defn part1 [input]
  (->> (map sort (parse-pairs input))
       (apply map vector)
       (c/sum #(abs (apply - %)))))

(defn part2 [input]
  (let [[a b] (parse-pairs input)
        freqs (frequencies b)]
    (c/sum #(* (or (freqs %) 0) %) a)))
```

Both instances use the 2-arity version of the function, which under the hood does the same as before - call `transduce`
with the mapping function provided, and then add up the results. I realize from looking at this that it doesn't provide
the full flexibility of `transduce`, especially if we want to do more than call a mapping function. For instance, a
transduction can support mapping and filtering without creating any intermediate sequences, but I'll bet that 9 out of
10 times, I expect that one would only want to use `sum` with a mapping function, so I think it's good.

## Minor extra refactoring

Thanks to seeing [Tom Schady's solution](https://github.com/tschady/advent-of-code/blob/main/src/aoc/2024/d01.clj) on
the Clojurian Slack, I'm making a small refact oring to the `part2` function.

```clojure
(defn part2 [input]
  (let [[a b] (parse-pairs input)
        freqs (frequencies b)]
    (c/sum #(* % (get freqs % 0)) a)))
```

In my first solution, I used `(or (freqs %) 0)` to return the value of looking up a value in the `freqs` map, and
returning `nil` if it wasn't found. Instead, `(get freqs % 0)` accomplishes the same thing in a single expression and
is more descriptive of the intention. I also put the multiplication of `%` at the front of the expression, so
`(* % (get freqs % 0))` instead of `(* (get freqs % 0) %)` to let the eye have an easier time with the parentheses.