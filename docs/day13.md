# Day 13: Claw Contraption

* [Problem statement](https://adventofcode.com/2024/day/13)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day13.clj)

## Intro

I solved part 1 of today's problem... really I did. Then it was apparent that the second part required some clever
math trick, so I waited to see what more patient / mathematically-inclined people did and used that hint. It turns out
that this one didn't require knowing some asinine formula, so it wasn't bad.

## Problem Statement and Solution Approach

We are given an input in groups of three lines, each corresponding to a claw machine. The first line tells us how far
in the `x` and `y` dimensions the claw moves when pushing button A, and the second line tells us for button B. The
third tells us the precise `x` and `y` dimensions where the prize is, and our goal is to push the A and B buttons the
correct number of times to perfectly align over the prize. Then once we know that, if it's even possible for that
machine, we calculate the number of tokens it costs to reach the prize, which is 3 for each A button push, and 1 for 
each B button push.

Initially I searched all possible values for pushing either A or B, calculating if there way a way to exactly hit the
puzzle, and then minimize the cost. That works fine for part 1, but not for part 2.

The mystery suggestion I found was "solve the simultaneous equations," and from that I got it.

So let's do some math! I use these variable names:
* `ax`, `ay`, `bx`, `by` - the number of `x` or `y` spaces the claw moves when pushing button `A` or `B`
* `prizex` and `prizey` - the `x` and `y` positions of the prize
* `numa` and `numb` - the number of times we push `A` or `B`

```
; initial functions
prizex = numa*ax + numb*bx
prizey = numa*ay + numb*by

; solve both equations for numa
numa = (prizex - numb*bx)/ax
     = (prizey - numb*by)/ay
     
; make both sides of the above equation equal each other (since they both equal numa), and solve for numb
(prizex - numb*bx)/ax = (prizey - numb*by)/ay
(prizex - numb*bx)*ay = (prizey - numb*by)*ax
numb*by*ax - numbb*bx*ay = prizey*ax - prizex*ay
numb = (prizey*ax - prizex*ay)/(by*ax - bx*ay)
```

We know the values for everything except for `numa` and `numb` for each machine, so all we'll do is plug in the values
for `numb`, then use that value to plug in to solve for `numa`, and if both values are integers, we found the correct
answers for both parts 1 and 2.

## Part One

First, we'll parse each machine.

```clojure
(defn parse-machine [lines]
  (let [[a-str b-str prize-string] lines
        [a-x a-y] (c/split-longs a-str)
        [b-x b-y] (c/split-longs b-str)
        [prize-x prize-y] (c/split-longs prize-string)]
    {:ax a-x, :ay a-y, :bx b-x :by b-y :prizex prize-x :prizey prize-y}))

(defn parse-input [input]
  (map parse-machine (c/split-blank-line-groups input)))
```

`parse-input` calls `(split-blank-line-groups input)` which, you may recall from the day 5 puzzle, creates a list of
lists of strings, separated by blank lines, where list of strings is itself split by lines. Basically, each "line
group" is the list of 3 strings that compose a machine.

Then `parse-machine` destructures the `lines` into `a-str`, `b-str`, and `prize-str`, and each one uses
`c/split-longs` to find the two numbers for their `x` and `y` values.

Really, everything else is the `cheapest-cost` function.

```clojure
(def a-cost 3)
(def b-cost 1)

(defn cheapest-cost [machine]
  (let [{:keys [ax ay bx by prizex prizey]} machine
        num-b (/ (- (* prizey ax) (* prizex ay))
                 (- (* by ax) (* bx ay)))
        num-a (/ (- prizex (* num-b bx))
                 ax)]
    (when (every? int? [num-a num-b])
      (+ (* num-a a-cost) (* num-b b-cost)))))
```

After destructuring the `machine` again, is calculates `num-b` and `num-a` (yes, I have a hyphen again since those are
programmatic variables and not mathematical ones) using the equations derived above. Then if both `num-a` and `num-b`
are integers, as we find with `(every? int? [num-a num-b])`, then we calculate the cost by multiplying the number of
key presses by their costs.

```clojure
(defn part1 [input] (transduce (keep cheapest-cost) + (parse-input input)))
```

Finally, our transduce function. The only thing that's a little unusual this time is the use of `(keep cheapest-cost)`
instead of `(map cheapest-cost)`, but that's because `cheapest-cost` can return `nil` if either `num-a` or `num-b` are
not integers.

## Part Two

For this part, we just have to make the prize values monstrously large. Since we're doing algebra, though, this doesn't
impact that algorithm in the slightest.

```clojure
; Option 1 - yucky and imperative
(defn apply-adjustment [machine]
  (reduce #(update %1 %2 + 10000000000000) machine [:prizex :prizey]))

; Option 2 - functional and fancy
(defn apply-adjustment [machine]
  (merge-with + machine {:prizex 10000000000000, :prizey 10000000000000}))

(defn part2 [input]
  (transduce (comp (map apply-adjustment) (keep cheapest-cost)) + (parse-input input)))
```

`apply-adjustment` adds the large value into a machine's `:prizex` and `:prizey` values. I implemented this two ways.
First, we can `reduce` over the keys `:prizex` and `:prizey`, and for each one we call
`(update map k + 10000000000000)`. That works, but it's not pretty, and we strive for pretty. So the second option is
`(merge-with + machine {:prizex 10000000000000, :prizey 10000000000000})`, which combines two maps, the initial
`machine` and one with only the offsets for `:prizex` and `:prizey`, and uses addition to merge the two together.

And then for `part2`, it's the same as `part1`, but calls `apply-adjustment`.

Obviously, it's time for the unifed `solve` function.

```clojure
(defn solve [map-fn input]
  (transduce (comp (map map-fn)
                   (keep cheapest-cost)) + (parse-input input)))

(defn part1 [input] (solve identity input))
(defn part2 [input] (solve apply-adjustment input))
```

If you've read any of my solutions this year, you should be able to figure this one out.

## Refactoring the parser

At the coffee shop this morning, I realized I can simplify the parsing logic, inlining `parse-input` and `parse-machine`
into a single function. We don't need to work with each line in the machine string separately; a machine has six
numbers, so we can just parse them out and use `zipmap` to throw them into a map. Note that we now use
`c/split-by-blank-lines` instead of `c/split-by-blank-line-groups` to work with the machine as a single string instead
of a sequence of strings.

```clojure
(defn parse-input [input]
  (map #(zipmap [:ax :ay :bx :by :prizex :prizey] (c/split-longs %))
       (c/split-by-blank-lines input)))
```