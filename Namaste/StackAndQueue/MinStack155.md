# Min Stack (LeetCode 155)

## Problem
Design a stack that supports `push`, `pop`, `top`, and retrieving the minimum element (`getMin`) — all in **O(1)** time.

Example:
```
push(-2); push(0); push(-3);
getMin() -> -3
pop();
top()    -> 0
getMin() -> -2
```

---

## Brute Force Approach
Use a plain stack for values, and for `getMin`, scan the entire stack to find the minimum on demand (or, alternatively, keep a second sorted structure like a `TreeMap<Long, Integer>` count-map that's updated on every push/pop).

```java
class MinStackBruteForce {
    private Stack<Long> stack = new Stack<>();

    public void push(long value) { stack.push(value); }
    public void pop() { stack.pop(); }
    public long top() { return stack.peek(); }

    public long getMin() {
        long min = Long.MAX_VALUE;
        for (long v : stack) {
            min = Math.min(min, v);
        }
        return min;
    }
}
```

- **Time Complexity:** `push`/`pop`/`top` are O(1), but **`getMin` is O(n)** — it rescans the whole stack every call.
- **Space Complexity:** O(n) for the stack itself.

---

## Optimal Approach (used in code)
```java
class MinStack {
    private Stack<Long[]> stack;

    public MinStack() {
        stack = new Stack<>();
    }

    public void push(long value) {
        if (stack.isEmpty()) {
            this.stack.push(new Long[] { value, value });
        } else {
            long minVal = Math.min(value, stack.peek()[1]);
            this.stack.push(new Long[] { value, minVal });
        }
    }

    public void pop() {
        this.stack.pop();
    }

    public long top() {
        return this.stack.peek()[0];
    }

    public long getMin() {
        return this.stack.peek()[1];
    }
}
```

### Intuition
The brute force is slow because "the minimum of the stack" changes as elements are pushed/popped, and re-deriving it from scratch every call throws away work we already did on previous calls.

Instead, notice: **the minimum of the stack *up to and including* the current top never needs to be recomputed once known** — it only ever needs to be compared against the *new* value being pushed. So we can cache, at every stack level, "what is the minimum of everything at-or-below me" right at push time.

Concretely, each stack frame stores a **pair**: `(actual value pushed, minimum of the stack so far including this value)`.
- On `push`, the new minimum is simply `min(newValue, previousMinimum)` — O(1), no rescanning.
- On `pop`, we discard the top frame — the minimum "reverts" automatically to whatever was cached one level below, because that frame already remembers the correct minimum *as of that point in history*.
- `getMin` and `top` both just read the top frame — O(1).

This is a classic **"stack that remembers auxiliary state per frame"** pattern: instead of a single global variable for the min (which would be wrong after a pop, since we wouldn't know the previous min), the min is tied to *each stack level*, so popping naturally rolls back to the correct historical value.

- **Time Complexity:** O(1) for all four operations.
- **Space Complexity:** O(n) — one extra `long` cached per element (same asymptotic space as brute force, just used smarter).

---

## Dry Run
Operations: `push(-2)`, `push(0)`, `push(-3)`, `getMin()`, `pop()`, `top()`, `getMin()`

| operation | stack (bottom → top) as (value, minSoFar) | return |
|-----------|--------------------------------------------|--------|
| push(-2)  | (-2,-2) | — |
| push(0)   | (-2,-2), (0,-2) | — |
| push(-3)  | (-2,-2), (0,-2), (-3,-3) | — |
| getMin()  | (unchanged) | **-3** (top frame's cached min) |
| pop()     | (-2,-2), (0,-2) | — |
| top()     | (unchanged) | **0** (top frame's value) |
| getMin()  | (unchanged) | **-2** (top frame's cached min — correctly reverted) |
