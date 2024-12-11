# Day 11: Plutonian Pebbles

* [Problem statement](https://adventofcode.com/2024/day/11)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day11.clj)
* [Memoized solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day11_memo.clj)

## Intro

This was a delightfully simple problem, no less because it was obvious to me during part 1 what was likely to happen
in part 2. I'll give you the "normal" solution, and then I'll do another solution that leverages some Clojure magic.

## Problem Statement and Solution Approach

We are given a list of numbers that represent stones, and a calculation of how each stone changes each time you blink
(aka for each generation). We need to project each stone 25 generations into the future, and then count the number of
stones that result. Oh yeah, during a blink, sometimes a stone splits in half.

Our strategy is to recognize that even though the puzzle makes clear that the stones always retain their order, that
doesn't really matter. In fact, we can calculate each stone in complete isolation if we want. Not only that, but there
are multiple ways to have the same stone show up multiple times within a generation. For instance, if we have the
stones `(1010 1020)` in generation 2, in generation 3 we'll have the stones `(10 10 10 20)`. As long as we know how
many stones will result from any `10` in the third generation, we can reuse that for the other two. Therefore, we want
to use the process of _memoization,_ or caching the results of expensive function calls.

We'll do this manually the first time, and then let Clojure abstract for us in the second implementation.

## Parts One and Two

To start, let's implement `blink`, which takes in a `stone` and returns a collection of stones it transforms into after
any given blink.

```clojure
(defn blink [stone]
  (let [stone-str (str stone)
        len (count stone-str)
        pivot (quot len 2)]
    (cond (zero? stone) [1]
          (even? len) (map parse-long [(subs stone-str 0 pivot) (subs stone-str pivot)])
          :else [(* stone 2024)])))
```

There's not much to this function; it's basically one big `cond` function. If the `stone` is a zero, return a vector
with only `1`. If it's an even-lengthed string, create two substrings at the midpoint, and return those strings
converted back into numbers. Otherwise, return a vector with just the stone multiplied by 2024.

Now we're going to implement `blink-to-the-future`, where the goal is to build out a history (a future prediction?) of
how many stones will result from a stone with a certain number of blinks remaining.

```clojure
(defn blink-to-the-future [stone blinks history]
  (let [key [stone blinks]]
    (cond
      (history key) history
      (zero? blinks) (assoc history key 1)
      :else (reduce (fn [acc stone'] (let [history' (blink-to-the-future stone' (dec blinks) acc)]
                                       (update history' key + (history' [stone' (dec blinks)]))))
                    (assoc history key 0)
                    (blink stone)))))
```

The function takes in not only the `stone` and `blinks` remaining, but also a `history`, which maps 
`{[stone blinks] n}` - that's our map of memoized results. The function will return the revised history, _not_ the
number of stones returned. We'll get to that later.

We'll bind `key` to `[stone blinks]` so we can simply our work with histories. And then we have a single `cond` call
again. If the `history` already knows how many stones come from this stone and number of blinks, then just return the
history; we have no more work to do. If there are no more blinks remaining, then the stone isn't going to transform,
so associate the key to `1`. Otherwise, we'll make recursive calls back in to `blink-to-the-future` for each of the
stones we get from `(blink stone)`. To simplify our code, this will actually be done within a `reduce` function that
works off `(assoc history key 0)`, since before looking at any child stones, we can say that the number of stones
produced at this key is zero. Then after calling `blink-to-the-future` with the child stones (remember to decrement
`blinks` since we'll have used one up in this generation), we take the revised `history'` that comes back from the
recursive call, look up the number of stones that the child produced, and add that to the value for the present stone's
key.

I'm not going to kid myself; let's just implement `solve` with both `part1` and `part2` at the same time.

```clojure
(defn solve [blinks input]
  (let [stones (c/split-longs input)
        history (reduce #(blink-to-the-future %2 blinks %1) {} stones)]
    (transduce (map #(history [% blinks])) + stones)))

(defn part1 [input] (solve 25 input))
(defn part2 [input] (solve 75 input))
```

The `solve` function parses the input by just calling `(c/split-longs input)`, and then generates the complete history
by reducing over `blink-to-the-future` for all stones, starting with an initially empty history. Now that we know what
each stone can do, we transduce across each of the original stones, mapping them into the `history` with the key
`[stone blinks]`, and adding the results. For `part1` we'll set `blinks` to `25`, and for `part2` we'll use `75`.

## Use Built-In Memoization

We can take the same basic structure from our initial solution, and instead of keeping our own history map, leverage
Clojure's built-in memoization. If we tell Clojure to memoize a function, it will retain a mapping of the set of
arguments passed in to a function to its result, and magically skip over the implementation every future time those
same arguments are passed in. The plus side is that it really simplifies the code. The downside is that those cached
values stay within the heap until the JVM is shut down or (I believe) the function itself is discarded.

We'll reuse the `blink` function, but instead of `blink-to-the-future`, we'll create `num-future-stones`.

```clojure
(def num-future-stones
  (memoize (fn [stone blinks]
             (if (zero? blinks) 1 (transduce (map #(num-future-stones % (dec blinks))) + (blink stone))))))
```

The first thing to note is that this is a `def`, not a `defn`. `memoize` takes in a function and returns another
function with memoization wrapped around it, and `num-future-stones` is that memoized function. But now, the function
only takes in the `stone` and `blinks` since we don't have to manage a `history` anymore. Similarly, we can have this
function return the actual number of stones generated for a `stone` and number of remaining `blinks`. If there are no
more blinks, return a `1` since this stone generated itself. Otherwise, we can look at the child stones called from
`(blink stone)`, `transduce` them into `num-future-stones`, and then since that returns a number, add them together.
It's really simple to look at by comparison.

How do we finish it up?

```clojure
(defn solve [blinks input]
  (transduce (map #(num-future-stones % blinks)) + (c/split-longs input)))

(defn part1 [input] (solve 25 input))
(defn part2 [input] (solve 75 input))
```

This is what we'd expect from any Advent problem - a single-line `transduce`. Split the input into long values, map
each to `num-future-stones` with the magic of memoization, and add the results.

You can compare and contrast the
[original solution](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day11.clj) with
the [built-in memoization solution](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day11_memo.clj)
to see which looks simpler and better to you.
