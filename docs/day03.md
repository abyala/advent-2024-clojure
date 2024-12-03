# Day 03: Mull It Over

* [Problem statement](https://adventofcode.com/2024/day/3)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day03.clj)
* [Reusable transducer code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day03_transducer.clj)

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

## Refactor a custom transducer

So Clojure has this concept of a [transducers](https://clojure.org/reference/transducers), which are "composable
algorithmic transformations [that] are independent from the context of their input and output sources and specify only
the essence of the transformation in terms of an individual element." What that means is that they represent only one
part of an algorithm and therefore can be used in all sorts of places, like a standalone `filter` function or a
`filter` that applies to every element flowing through a channel. I thought it would be fun to create a custom
_stateful_ transducer that represents the state of the North Pole Toboggan Rental Shop computer for part 3,
taking in string tokens from however they come in, and not caring whether they're going to be added or multiplied or
thrown together into a collection at the end.

I think the easiest way to look at it is in terms of its use, before looking at its construction.

```clojure
(defn solve [regex input]
  (transduce (instruction-processor) + (re-seq regex input)))

(defn part1 [input] (solve #"mul\(\d+,\d+\)" input))
(defn part2 [input] (solve #"mul\(\d+,\d+\)|do\(\)|don't\(\)" input))
```

So the goal is to make _something_ that we'll throw into the `transduce` function, whose purpose is to process each
`"mul(x,y)"`, `"do()"`, and `"don't()"` string, spitting out only the multiplied values (while retaining its state of
being enabled or disabled), such that the `transduce` function does a `+` for its accumulation.  In that vein, you
could imagine applying the same computation in any of these ways:

```clojure
(transduce (instruction-processor) +       (re-seq regex input))
(transduce (instruction-processor) *       (re-seq regex input))
(transduce (instruction-processor) str     (re-seq regex input))
(transduce (instruction-processor) conj [] (re-seq regex input))
```

So let's make a transducer!

```clojure
(defn instruction-processor []
  (fn [xf]
    (let [enabled? (volatile! true)]
      (fn
        ([] (xf))
        ([result] (xf result))
        ([result input]
         (cond (= input "don't()") (do (vreset! enabled? false)
                                       (xf result))
               (= input "do()") (do (vreset! enabled? true)
                                    (xf result))
               @enabled? (xf result (apply * (c/split-longs input)))
               :else (xf result)))))))
```

First off, the transducer is a function, like `(filter even?)` or `(take 5)`, but it returns a function that takes in
the next nested transformer and produces yet another function. It's a little dizzying, but that's just how it works.
The idea is that you can compose multiple transducers together, so `(instruction-processor)` kicks things off (it takes
no arguments for now), and then is usable in an environment in which it receives a downstream function, and then it has
to work on the data as it flows.

For the most nested function, note that not all transducers need to support all three arities, but mine does. You'll
also note that is retains state in the form of a volatile boolean called `enabled?`, enclosed within the function
definition, so it's inaccessible to the outside. I could have used an atom instead of a volatile variable, but since
I'm not worried about shared state, using a mutable value made more sense. So let's go through the three arities:

* Zero arity: This is the optional initializing function to call if the transducer does not start with an initialized 
value. In the above examples, the `conj` use case initializes the `transduce` function with an empty vector, but the
others examples do not have initialized values, so the 0-arity function will be called for all examples except for
the `conj` one.
* Two arity: This is the "step" arity, the workhorse of the transducer. It takes in the current result of the entire
transduction process, the input (current value that's being processed, after any other previous transformations), and
its responsibility is to do whatever it needs to do before sending the result down to the next transformer, `xf`. So
in this case, if the input is either `"do()"` or `"don't()"`, we change the value of the stateful `enabled?` to the 
proper value before calling `(xf result)`. If it's a `"mul(x,y)"` command and the multiplier is enabled, then we pass
the transformed value through to the `xf` function, converting it from the string `"mul(x,y)"` to the actual numeric
value. And in the default case, where it's a `"mul(x,y)"` value but the processor is disabled, it again calls through
to `xf` without pushing the next value down, since we want to skip it. So the point is that we **_always_** call `xf`
from this function, but we only pass an input value down if we want it to be processed by the next processor in the
chain.
* One arity: This is the completion function, used at the end to do any final state cleanups. You could imagine that
the transducer might hold a database connection or a file handle, so it would close those resources before termination.
In our case, there's nothing to do, so again call `(xf result)` to send the final value down the line.

## Refactor to a better transducer

Let's take the example of the transducer and make it even more reusable. What we really have here is a channel that
can be enabled or disabled, and it either sends values through it or discards them if it's disabled. We can extract
that logic away from the String parsing and the multiplication logic, and this should really show off how transducers
are composable.

Let's start with our new transducer.

```clojure
(defn shutoff-channel []
  (fn [xf]
    (let [enabled? (volatile! true)]
      (fn
        ([] (xf))
        ([result] (xf result))
        ([result input]
         (cond (= input :enable) (do (vreset! enabled? true)
                                     (xf result))
               (= input :disable) (do (vreset! enabled? false)
                                      (xf result))
               @enabled? (xf result input)
               :else (xf result)))))))
```

This transducer, called `shutoff-channel`, accepts values of either `:enable`, `:disable`, or anything else as a value
to potentially pass through. `:enable` and `:disable` mutate the `enabled?` volatile state, and otherwise we either call
`(xf result input)` if we're enabled or `(xf result`) if we're not.

Before going further, let's see how we could use this.

```clojure
; Produces [:a :b :c 4 5 6]
(transduce (shutoff-channel) conj [] [:a :b :c :disable 1 2 3 :enable 4 5 6])

; Produces ":a:b:c456"
(transduce (shutoff-channel) str [:a :b :c :disable 1 2 3 :enable 4 5 6])
```

So we can see here that we don't need to do anything with String parsing or multiplication by using the
`shutoff-channel` all by itself and with a vector.

Now let's handle our second concern - processing each String token.

```clojure
(defn parse-instruction [instruction]
  (or ({"do()" :enable, "don't()" :disable} instruction)
      (apply * (c/split-longs instruction))))
```

This function assumes that the instruction will either be a `"do()"`, `"don't()"`, or `"mul(x,y)"` string. The first
part checks to see if it finds the value in a map of `"do()"` to `:enable` and `"don't()"` to `:disable`, and if not,
then it does the multiplication logic.

Finally, we can reimplement our solve function, with some new reuse to boot.

```clojure
(defn solve [regex input]
  (transduce (comp (map parse-instruction) (shutoff-channel)) + (re-seq regex input)))

(defn part1 [input] (solve #"mul\(\d+,\d+\)" input))
(defn part2 [input] (solve #"mul\(\d+,\d+\)|do\(\)|don't\(\)" input))
```

Note that the `part1` and `part2` functions look like they did before - they just call `solve` with the appropriate
regex pattern. But now, the `transduce` transformation function is a composition of `(map parse-instruction)` to
transform the input string into its proper interpretted value, followed by the `shutoff-channel` to optionally send the
value through to the reducing function.

You could imagine the `shutoff-channel` now could be used for multiple purposes. In fact, if we thought this
`shutoff-channel` would be truly reusable, we could even make it customizable such that a caller could define which
tokens should turn it on or off by an execution like `(shutoff-channel :enable :disable)`. So you could see this:

```clojure
; Returns [:a :b :c 4 5 6]
(transduce (shutoff-channel) conj [] [:a :b :c :disable 1 2 3 :enable 4 5 6])

; Also returns [:a :b :c 4 5 6]
(transduce (shutoff-channel "Be mighty" :go-away) conj [] [:a :b :c :go-away 1 2 3 "Be mighty" 4 5 6])
```

How would we write that? Given the transducer we've already made, it's really quite simple to implement a multi-arity
`shutoff-channel` implementation. Note that this changes only the top-level call to `shutoff-channel` itself; the
nested function it returns is still the same one that takes in a transformation function and returns the multi-arity
function, but now it compares each `input` to the `enable-token` and `disable-token`.

```clojure
(defn shutoff-channel
  ([] (shutoff-channel :enable :disable))
  ([enable-token disable-token]
   (fn [xf]
     (let [enabled? (volatile! true)]
       (fn
         ([] (xf))
         ([result] (xf result))
         ([result input]
          (cond (= input enable-token) (do (vreset! enabled? true)
                                      (xf result))
                (= input disable-token) (do (vreset! enabled? false)
                                       (xf result))
                @enabled? (xf result input)
                :else (xf result))))))))
```

Alright, that's enough of transducers for a while. I just love this pattern so I get carried away.