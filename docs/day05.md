# Day 05: Print Queue

* [Problem statement](https://adventofcode.com/2024/day/5)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day05.clj)
* [Compact solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day05_compact.clj)

## Intro

I enjoyed today's puzzle. This was an exercise in a little parsing, a lot of reuse of my
[advent-utils-clojure](https://github.com/abyala/advent-utils-clojure/) library, and a whole lot of composed functions.

## Part One

In this puzzle, we are given a set of page-ordering rules, specifying which page must precede another page, and a set
of "updates," which we will rename "page-lists" because `update` is a significant Clojure function. We don't use that
function in my solution, but I don't like overloading such an important term.

### Parsing

Let's start with parsing. I want to take in the input string and return a map of 
`{:rules {page #{pages-to-the-right}}, :page-lists ()}`.

```clojure
(defn parse-input [input]
  (let [[rule-str page-list-str] (c/split-blank-line-groups input)]
    {:rules      (reduce (fn [m [left right]] (c/map-conj hash-set m left right))
                         {}
                         (map c/split-longs rule-str))
     :page-lists (map (comp vec c/split-longs) page-list-str)}))
```

This solution leverages three utility functions. First, `split-blank-line-groups` takes a string and returns a sequence
of line group strings that were separated by a fully blank line. This will be used to create two sequences of strings,
one containing the rules, the other containing the page-lists. Second we have `map-conj`, a funny little function 
I use as a form of a lazy map. Its use sounds like "conj v2 onto the collection that exists at `(get m v1)`, and if it
doesn't have such a value, then make one." The default implementation creates a new vector, but here we pass in the
`hash-set` function to make sure it creates a new set into which the new value can be added. And of course we have our
good friend `split-longs` again.

So now let's look at what the function does. To pase the rules, we take the multi-line `rule-str` that
`split-blank-line-groups` created, mapping each one with `split-longs` into a sequence of two numbers. Then we `reduce`
those numeric pairs, adding them into growing map where the key is the page number on the left, and the right is a set
of all pages that must be to its right.

Finally, to parse the page-lists, we just do a mapping of each page-list string by splitting it into its numeric pieces,
and then converting those sequence of numbers into vectors for easier manipulation later.

### Order Checking

Next we check to see if a given page-list is in the correct order, based on the rules.

```clojure
(defn correct-order? [rules page-list]
  (some? (reduce #(if (some %1 (rules %2))
                    (reduced nil)
                    (conj %1 %2))
                 #{}
                 page-list)))
```

I did this with another `reduce` that accumulates a set of all pages seen for each iteration. For each page, we check
to see if there is a rule requiring it to be to the left of any previously seen page. If so, we short-circuit out of
the `reduce` by calling `(reduced nil)`. Now we don't need to wrap that in `(some? ...)` since `nil` is falsey and any
set is truthy, but I thought calling `some?` to return a nice clean boolean would be easier to understand.

### Finishing it up

We're almost done with part 1. First, we'll need a quick utility function to return the middle page of a page-list.

```clojure
(defn middle-index [page-list]
  {:pre [(odd? (count page-list))]}
  (page-list (/ (dec (count page-list)) 2)))
```

The instructions don't tell us what the middle of an even-lengthed page-list would be (do we return the second or third
element of a 4-page page-list?), so I made the assumption that they must all have an odd length. I added this as an
assertion, which is implemented as a simple map right after the function arguments. Clojure supports an optional map of
pre- and/or post-processors using the keys `:pre` and `:post`. If provided, they bind to a vector of predicates that
must all return true to avoid failing the assertion. Luckily, ours did!

So all this needs to do then is decrement the size of the `page-list` and cut it in half, and then return the value of
`page-list` at that index. Remember up above when we converted the parsed page-lists into vectors using `(comp vec)`?
This is part of why we did it. Note that a vector under the hood really is just a map (it's a map of an index to the
value at that index), so we can use the `page-list` vector as the function. So in a sense, `(page-list idx)` says to
return the value contained at that index in the vector.

```clojure
(defn part1 [input]
  (let [{:keys [rules page-lists]} (parse-input input)]
    (transduce (comp (filter (partial correct-order? rules))
                     (map middle-index))
               + page-lists)))
```

Finally, we parse the input and transduce. We know we're going to work on each page-list and add up the values when
we're done. The transformation function itself is compound. First, we filter each page-list to make sure we only take
the ones in the correct order. Then after that, we extract out its middle index. All done!

## Part Two

For part 2, we need to fix any page-lists in the wrong order by moving pages around to comply with the rules. I 
implemented this a few ways, but the simplest was to write a `reorder` function that just sorted the pages.

```clojure
(defn reorder [rules page-list]
  (vec (sort (fn [v1 v2] (cond ((rules v1 #{}) v2) -1
                               ((rules v2 #{}) v1) 1
                               :else 0))
             page-list)))
```

The function is really just calling `(vec (sort comparator page-list))`, sorting the pages and converting the result
back into a vector again, so the bulk of the logic is the comparator itself. In this case, we don't know whether any
two pages have a rule saying if it needs to be on the left of the other, or if there is no rule at all. So we check
both conditions, returning either `-1` or `1` if we found such a rule, or `0` if there is no rule saying either way.
Note that the `get` function can be called either as `(get map key)` or `(get map key default-value)`, but you also
don't need to explicitly use the function `get` since the map can be the function. I stumbled upon the fact that this
format works with providing the default value if the map doesn't contain the key, so that was a fun surprise.

So armed, we can implement `part2`.

```clojure
(defn part2 [input]
  (let [{:keys [rules page-lists]} (parse-input input)]
    (transduce (comp (remove (partial correct-order? rules))
                     (map (comp middle-index (partial reorder rules))))
               + page-lists)))
```

This looks similar in structure to `part1`. We again parse the data and transduce over the page-lists. This time, the
transformation function uses `remove` instead of `filter` to get rid of the rules in the correct order, and then its
map first reorders the page-list before calculating the `middle-index` that gets added together.

So that's it! I'm not going to make a single unifying `solve` function this time, since that would only break clarity.

Note, however, that with this implementation of `reorder`, we **_could_** reimplement `correct-order?` to see if a
page-list has the same value as its reordered value, since reordering a page-list in the correct order shouldn't make
any changes, but that's kind of silly.

```clojure
; Shown for fun; I don't think this is worth doing.
(defn correct-order? [rules page-list]
  (= page-list (reorder rules page-list)))
```

## Refactor to always use comparators

Ok fine - I did a reimplementation to use the `reorder` and its comparator for both parts 1 and 2, and I placed it in
the [day05-compact](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day05_compact.clj)
namespace. Let's quickly have a look.

To start, I keep the same implementations of `parse-input` and `reorder`, and I scrapped the `correct-order?` and
`middle-index` functions. The idea now is that for each page-list, we will _always_ reorder it and return both whether
it originally was correct and what the new middle value is. For readability, I threw the results into a map with keys
`:correct?` and `:middle`.

```clojure
(defn process [rules page-list]
  (let [reordered (reorder rules page-list)]
    {:correct? (= page-list reordered), :middle (reordered (quot (count reordered) 2))}))
```

Then we just have to do our solution, and now I can justify a unified `solve` function with a common transducer:

```clojure
(defn solve [already-correct? input]
  (let [{:keys [rules page-lists]} (parse-input input)]
    (transduce (comp (map (partial process rules))
                     (filter #(= already-correct? (:correct? %)))
                     (map :middle))
               + page-lists)))

(defn part1 [input] (solve true input))
(defn part2 [input] (solve false input))
```

The `solve` function parses the input into the rules and page-lists, and then transduces over the page-lists by summing
them together. In the transformation function, we first process each rule, and then we filter the results based on
whether we wanted the ones that were originally correct or not. Finally, we pull out the middle value. Part 1 works
with the ones that were already correct, and part 2 the ones that weren't.