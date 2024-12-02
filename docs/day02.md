# Day 02: Red-Nosed Reports

* [Problem statement](https://adventofcode.com/2024/day/2)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day02.clj)

## Intro

Well today's was a cute little puzzle, arguably simpler than yesterday's. Let's do it.

## Part One

We're given an input file with multiple lines of numbers; each line is considered a "record," and each number within
that line a "level." Our task is to count the number of records that are "safe," meaning that all values either
increment or decrement, and the difference between each adjance number is between 1 and 3, inclusively.

Normally I would start with parsing the data, but it's so simple that we won't bother with it just yet. Instead, let's
look at the `safe?` function instead.

```clojure
(defn safe? [report]
  (let [diffs (map (partial apply -) (partition 2 1 report))]
    (or (every? #(<= -3 % -1) diffs)
        (every? #(<= 1 % 3) diffs))))
```

Given a report (a sequence of numbers), we need to check if every gap between adjancent numbers is within a safe
increasing or decreasing range. I went with clarity for this function instead of being too clever. First we create the
binding `diffs` to represent the difference between adjacent values. `(partition 2 1 report)` creates pairwise vectors
of values, and then `(map (partial apply - ) partitions)` calculates the differences. Then we check to see if all 
differences are either between 1 and 3, or between -3 and -1.

Then we're ready to solve the puzzle, Pat.

```clojure
(defn part1 [input]
  (c/count-when safe? (map c/split-longs (str/split-lines input))))
```

Now here I'm using two functions from my 
[Advent Utils core namespace](https://github.com/abyala/advent-utils-clojure/blob/main/src/abyala/advent_utils_clojure/core.clj):
`split-longs`, which we saw on day 1, and `count-when`. `count-when` is a simple helper function that filters a
collection by a sequence and then counts the number of results. I created this function last year, again to mirror what
the Kotlin stdlib provides, and I think it's super clear. So in this case, after splitting the input by each line and
mapping each line into a sequence of numbers, we count the number of lines where that sequence is safe.

## Part Two

Hmm... a report we considered to be unsafe before can be safe if we can remove a single value and end up with a safe
report. Let's write `safe-with-dampener?` to accomplish this.

```clojure
(defn safe-with-dampener? [report]
  (some safe? (cons report (c/unique-combinations (dec (count report)) report))))
```

First off, I'm using yet another function from my core library - `unique-combinations`, which returns all possible
sub-collections of a collection with a given length. How convenient for us, since I know the algorithm puts together
sub-vectors from left to right, so it won't mess up the ordering. I could have done the same thing by manually
joining two subvectors for each element removed, but I don't have to so I won't!

What's left is `(some safe? (cons report combinations))`. We know that a report can be safe without removing any
elements, so we'll use `cons` to attach the original report onto the front of the collection of combinations, and then
see if `safe?` returns `true` for any of those reports.

Finally, we can solve part 2.

```clojure
(defn part2 [input]
  (c/count-when safe-with-dampener? (map c/split-longs (str/split-lines input))))
```

Uh oh - that looks just like part 1! Let's refactor them to use a common `solve` function.

```clojure
(defn solve [with-dampener? input]
  (c/count-when (if with-dampener? safe-with-dampener? safe?)
                (map c/split-longs (str/split-lines input))))

(defn part1 [input] (solve false input))
(defn part2 [input] (solve true input))
```

`solve` takes in a boolean value `with-dampener?` to decide whether to call `safe?` or `safe-with-dampener?`. Whichever
predicate it uses, it does the same parsing/splitting logic, and the same call to `count-when`. There - it's nice and
clean to look at. So pretty.

### Refactorings

### Avoid using unique-combinations

My friend [Matt Kunn](https://github.com/mtkuhn) correctly pointed out that my use of `unique-combinations` may not be
wise, since there's nothing in the API which guarantees the solution retains the original collection's order. So in
this case, it may make sense to be more explicit. Fair enough, let's do it.

```clojure
; New library function in the abyala.advent-utils-clojure.core namespace 
(defn remove-each-subrange
  "Returns the result of every way to remove a subrange from a collection, defaulting to a removal length of 1.
  The length must be a positive integer or it will fail with an assertion error. The function will return an empty
  list if the value of `n` is at least the size of the input collection."
  ([coll] (remove-each-subrange 1 coll))
  ([n coll]
   {:pre [(pos-int? n)]}
   (if (< n (count coll))
     (map #(concat (take % coll) (drop (+ n %) coll))
          (range (- (count coll) n -1)))
     ())))
```

The `remove-each-subrange` function takes a collection and an optional number of values to extract out, defaulting to
one. Using a combination of `take` and `drop`, it returns all possible ways to remove a subrange of length `n` from the
input collection. I made this function generalizable, in case I ever wanted to remove more than a single value from the
middle.

Armed with this function, we can now re-implement the `safe-with-dampener?` function.

```clojure
(defn safe-with-dampener? [report]
  (some safe? (c/remove-each-subrange report)))
```

Now we just call `remove-each-subrange` instead of `unique-combinations`. We no longer need to count the target length
of each resulting collection, since this function requires passing in the length of the subranges to _remove_ instead
of the length of the _output_ values.

But wait, you say - why do we no longer need to call `(cons report ...)` in the front? Well, as I read on the
Clojurian Slack, if a record is safe in its entirety, then it must be safe if you remove the first value as well. So
this makes the code even that much more succinct and readable.