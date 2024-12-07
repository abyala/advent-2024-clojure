# Day 07: Bridge Repair

* [Problem statement](https://adventofcode.com/2024/day/7)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day07.clj)

## Intro

I loved today's puzzle, both because it was very straightforward to me and also because I got stuck behind a stupid
bug for a long time, and the fix made me feel really silly. Plus the final implementation is super succinct, in part
because I get to reuse a library function I was really hoping I would get to use this season. Score!

## Part One

Our input is a bunch of lines with the form `v1: v2 v3 v4...`, where the first number is the "test value" and the rest
are numbers that need to be added or multiplied together (in left-to-right order, not with precedence). The goal is
to find which lines have test values that can be reached one way or another from the others (in order), and then to add
up those test values.

There's actually very little code; it's just two functions, but there's a bunch inside it.

```clojure
(defn solveable? [nums]
  (let [[target a b & c] nums]
    (search/depth-first (list [a b c])
                        (fn [[v1 v2 [v3 & v4]]]
                          (when (and v2 (<= v1 target))
                            [[(+ v1 v2) v3 v4] [(* v1 v2) v3 v4]]))
                        (fn [[v1 v2]] (and (= v1 target) (nil? v2))))))
```

The `solveable?` function takes in a sequence of numbers, where the first is the target and the rest are the numbers to
manipulate. In the initial `let` call, we extract out the target, the first two numbers (the ones we'll either add or
multiply), and the rest using `& c`.

Then we call an exciting function in my
[abyala.advent-utils-clojure.search](https://github.com/abyala/advent-utils-clojure/blob/main/src/abyala/advent_utils_clojure/search.clj)
package, called `depth-first`, which does, you guessed it, a depth first search! I think of it as using a reduce, but
the input collection changes as you progress. This function takes in three values:
1. `initial-vals` - a collection of the starting search vals we know about.
2. `next-vals-fn` - the function to call to add additional possible values into the search space after we look at a
search value.
3. `pred` - the predicate to apply to determine whether any given value in the search space is what we're looking for.

`initial-vals` is simple enough to understand. To start, we only know to check all of the values in the original `nums`
sequence after the target. So we'll pass in a list with only one value, the vector of the first two numbers and the
reamining numbers `(c)` as a sequence.

The `pred` function is pretty simple too. Given one of the `[a b c]` vectors we just described, if the first value is
the target ("test value") and the second value is `nil`, it means we've calculated the entire line and ended up with
the correct value. Search complete!

The `next-vals-fn` really isn't all that bad. First, we only look for new values if there is a second value in the
vector (I'll explain in a moment), and if the first value (the one we use for comparison in the `pred`) isn't already
too large, since addition and multiplication of positive numbers should never decrease. But if that's the case, then
we will always create two new values into the search space - we either add together the first two numbers or we
multiply them. So to create the two `[a b c]` vectors, the new `a` is the sum or product of the first two, the new `b`
is what was the head of the remaining sequence, and the new sequence is the tail of the previous one. Note that to
avoid collisions, this function destructures the `[a b c]` vector into `[v1 v2 [v3 & v4]]`. Remember that the third
value is a sequence, which is why we wrap `[v3 & v4]` in brackets. 

Once we're written that, we're pretty much there.

```clojure
(defn part1 [input]
  (transduce (comp (map c/split-longs)
                   (filter solveable?)
                   (map first))
             + (str/split-lines input)))
```

For `part1`, we parse the input by splitting its lines apart, and we transduce the values with addition as the reducing
function. The transformation parses each line using `(map c/split-longs)`, then it filters for only the `solveable?`
lines, and then `(map first)` pulls out the "test value" to get us our answer.

## Part Two

Oh, part 2. It's just like part 1, except that now we can concatenate the string values of the first two terms, instead
of only being able to add or multiple them.  This should be quick

```clojure
(defn solveable? [commands nums]
  (let [[target a b & c] nums]
    (search/depth-first (list [a b c])
                        (fn [[v1 v2 [v3 & v4]]]
                          (when (and v2 (<= v1 target)) (map #(vector (% v1 v2) v3 v4) commands)))
                        (fn [[v1 v2]] (and (= v1 target) (nil? v2))))))
```

The `solveable?` function only changes to receive the commands that it must support - addition and multiplication, and
possibly concatenation. Then, instead of making a 2-element collection from adding and multiplying the first two
values, it calls `(map #(vector (% v1 v2) v3 v4) commands)`. It's not very common to pass function calls into a map,
but it works really nicely here.

Let's keep going; it's about to all be crystal clear.

```clojure

(defn solve [commands input]
  (transduce (comp (map c/split-longs)
                   (filter (partial solveable? commands))
                   (map first))
             + (str/split-lines input)))

(defn part1 [input] (solve [+ *] input))
(defn part2 [input] (solve [+ * (comp parse-long str)] input))
```

Ah, the `solve` function. Given a sequence of commands and the input string, we pretty much do what `part1` used to do,
except that we pass `commands` in to the new `solveable?` function. Then `part1` calls `solve` with only `[+ *]`,
while `part2` also passes in concatenation, the result of calling `str` on the two values and then parsing them again
using `parse-long`.

That's it! It's nice and compact, really fast, and I got some terrific out of my `depth-search` function. Note that
`breadth-first` worked for `part1` but not `part2` because I foolishly used the `concat` function in my implementation,
and I need to figure out how to not use it here. But I saw no reason why I shouldn't prefer `depth-first` anyway.
