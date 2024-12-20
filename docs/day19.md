# Day 19: Linen Layout

* [Problem statement](https://adventofcode.com/2024/day/19)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day19.clj)

## Intro

This was a nice, easy puzzle today, with a performance twist in part two that worked despite my thinking it wouldn't!
I enjoyed this puzzle because it gave me a motivational bump that I needed.

## Problem Statement and Solution Approach

We are given a list of "patterns" of cloth, and a list of "designs" we want to construct from an infinite supply of
those patterns. Our job in part one is to simply count the number of patterns we can create with some combination of
designs. In part two, we need to count the number of ways we can create any design; for each design that we can
create, sum up the number of ways to do so.

The solution for both parts requires a depth-first search, where we try to match the start of the design with any of
the available patterns, and then recursively do the same for the rest of the design. We'll also use a little memoizing
to get us the performance we need for part 2.

## Part One

As always, the first step is to parse the input data. We want to create a map with keys `:patterns` and `:designs`,
where each is a sequence of strings.

```clojure
(defn parse-input [input]
  (let [[[pattern-str] designs] (c/split-blank-line-groups input)]
    {:patterns (re-seq #"\w+" pattern-str) :designs designs}))
```

`(c/split-blank-line-groups inputs)` returns a tuple of a list of strings; the first element is a single-element list
of strings that contains the top line of patterns. The second is the list of strings, one for each line of designs.
First, note the extra level of destructuring in the `let` statement, for `[[pattern-str] designs]`, since the former
will strip out the first (and only) pattern string in the list, while the latter retains all of the designs. Then, to
get the individual patterns, we call `(re-seq #"\w+" pattern-str)` to pull out the word-strings (no commas or spaces).

Now we'll implement the recursive function `available?` to return if there is any way to make the design from the
patterns.

```clojure
(defn available? [patterns design]
  (or (str/blank? design)
      (some #(when (str/starts-with? design %)
               (available? patterns (subs design (count %))))
            patterns)))
```

This is an `or` statement with two conditions. If the `design` is blank, we've already assembled it with previous
patterns, so we're down. Otherwise, we'll use `(some f patterns)` to see if any of the `patterns` can produce a valid
result. To know if it's valid, the test function will see if the design begins with that pattern. If so, we'll return
the result of recursively calling `available?` on the substring of the design after the pattern. So then if any of the
recursive calls returns `true` for `available?`, then the whole function does.

Now we can wrap it up with `part1`.

```clojure
(defn part1 [input]
  (let [{:keys [patterns designs]} (parse-input input)]
    (c/count-when (partial available? patterns) designs)))
```

It's rather simple, really. After parsing the input, we call `c/count-when` on each of the designs, where the predicate
is if it is `available?` across all patterns.

Nice.  Let's move on.

## Part Two

Part two is similar to part 1, except now instead of being able to short-circuit by returning **_if_** a design can be
created, we have to return the number of ways that it can be done. Let's make an initial implementation that will
almost work.

```clojure
; So close, but won't quite work
(defn available-count [patterns design]
  (if (str/blank? design)
    1
    (transduce (comp (filter (partial str/starts-with? design))
                     (map #(available-count patterns (subs design (count %))))) + patterns)))
```

Structurally, it's the same as `available?`, in that it checks if the `design` is an empty string, and if not, it
checks the result of recursively calling itself with the results of applying each pattern to its front. Only this time,
instead of returning a boolean value, we need the number of ways to get to an answer. So the base case that answers how
many ways it takes to get to an empty string is always 1 (we're already there), and otherwise we add together the
result of the recursive calls using `transduce`.

This function works, but it's a bit slow, in that `part2` takes almost 30 seconds to complete. So to speed it up, let's
memoize it again.

```clojure
(def available-count
  (memoize (fn [patterns design]
             (if (str/blank? design)
               1
               (transduce (comp (filter (partial str/starts-with? design))
                                (map #(available-count patterns (subs design (count %))))) + patterns)))))
```

This is now a `def` instead of a `defn`, that wraps the original function in `memoize`. This makes the solution
lightning fast. Why does it work? Well this algorithm is depth-first, which means it will try to calculate the number
of ways to reach the final string as quickly as possible. By doing so, subsequent calls become increasingly likely to
recursively call `available-count` with known values, so the hit rate goes up.

As I'm writing this, I realize that another way to do this would be to calculate from the end of the string up to the
front, figuring out how many ways we can end up with an end-of-string we've already seen. This should let us do a
linear scan, rather than a tree-recursive scan, and not have to memoize it. Maybe I'll implement that someday. Anyway,
let's keep going.

```clojure
(defn part2 [input]
  (let [{:keys [patterns designs]} (parse-input input)]
    (transduce (map (partial available-count patterns)) + designs)))
```

With a fast `available-count` function, we're ready to complete `part2`. After parsing the data, we map each of the
designs to `available-count`, and use `transduce` to add them together.

Can we make a unified `solve` function? Yes. It's not my favorite solution, but it does get rid of the `available?`
function from part 1 since `available-count` is nice and fast, so let's do it.

```clojure
(defn solve [xform-fn input]
  (let [{:keys [patterns designs]} (parse-input input)]
    (transduce (map (comp xform-fn (partial available-count patterns))) + designs)))

(defn part1 [input] (solve (partial min 1) input))
(defn part2 [input] (solve identity input))
```

`solve` is almost the same as the `part2` function, except that after mapping each `design` to `available-count`, we
do another mapping function before adding the values together. For `part2`, we use `identity` since that will give us
the same value we saw above. For `part1`, we use the function `(partial min 1)` so that if `available-count` returns
anything other than a zero, it sets itself to 1. And the sum of 0 or 1 is the same as the count.

## Refactorings

### Rewrite available-acount without memoizing

I thought of a way to reimplement `available-count` that's fast (linear-time) and doesn't require caching or memoizing.
The idea is that we'll keep a look-ahead map of the `design` string's index to the number of paths leading up to it.
Then at each index `n` along the `design` string, starting at the beginning, find each pattern that matches the start
of the substring of the `design`, starting at `n`. For each match, add the number of paths already found to the
look-ahead map at index `(+ n (count pattern))`.

```clojure
(defn available-count [patterns design]
  (get (reduce (fn [acc n] (let [num-paths (acc n)
                                 test (subs design n)]
                             (if num-paths (apply merge-with + acc (keep #(when (str/starts-with? test %)
                                                                            {(+ n (count %)) num-paths})
                                                                         patterns))
                                           acc)))
               {0 1}
               (range (count design)))
       (count design) 0))
```

This is a simple `reduce` function of the form `(reduce f {0 1} (range (count design)))`, meaning that we're going
to use an initial state map of 0 to 1, since there's one path to the start of the string, and a driving collection of
`(range (count design))`. The for each index, see if the accumulated look-ahead has a value for that index. If not,
then there's no reason to look at the patterns at that index since there's no path to get there, so continue with
`acc`. If there is a value, then `(apply merge-with + acc pattern-matches)` where `pattern-matches` is a collection of
maps of `{next-n num-paths}` for each matching pattern. Finally, when we're all done, we call 
`(get look-ahead (count design) 0)` to get the number of paths to the full length of `design`, or `0` if we never found
a path to it.

The running time is barely slower than the memoized recursive function, but this requires less memory (no impact on the
JVM) and is relatively straightforward to understand.

There's another theoretical optimization we could make, but won't. If we arrange the `patterns` as a map of
`{pattern-length (patterns)}`, we can do fewer comparisons. If, for instance, we had the patterns `(a b c ab ac)` and
we found a match on the single-length pattern `a`, we don't need to check any other single-length pattern since only
one single-length pattern will match a single-length string. Similarly, if the 2-length pattern `ab` matched, there's
no reason to see if `ac` matched at that index too. It would be more efficient, I suppose, but is it worth it? 🤔

### Simplify parsing

We can make `parse-input` much easier to understand.

```clojure
(defn parse-input [input]
  (zipmap [:patterns :designs]
          (map (partial re-seq #"\w+") (c/split-by-blank-lines input))))
```

The single-line of patterns and the multiple lines of designs really are the same thing - a bunch of alphabetical 
strings. So if we use `(c/split-by-blank-lines input)` instead of `c/split-blank-line-groups`, we'll get back a list
of two strings - the patterns and designs. If we map each one to the regular expression to pull out all alphabetical
strings, we can `zipmap` the keys `:patterns` and `:designs` to the 2-element list of parsed string lists.