# Evaluate Reverse Polish Notation (LeetCode 150)

## Problem
Evaluate an arithmetic expression given in Reverse Polish Notation (postfix notation). Valid operators are `+`, `-`, `*`, `/`. Division between integers truncates toward zero.

Example: `tokens = ["2","1","+","3","*"]` → evaluates `(2 + 1) * 3 = 9`.

---

## Brute Force Approach
Repeatedly scan the token list left to right for the *first* operator, apply it to the two operands immediately before it, and replace those three tokens with the single result. Repeat until only one token remains.

```java
private static int evalRPNBruteForce(String[] tokens) {
    List<String> list = new ArrayList<>(Arrays.asList(tokens));
    Set<String> ops = Set.of("+", "-", "*", "/");

    while (list.size() > 1) {
        for (int i = 0; i < list.size(); i++) {
            if (ops.contains(list.get(i))) {
                int a = Integer.parseInt(list.get(i - 2));
                int b = Integer.parseInt(list.get(i - 1));
                int res = switch (list.get(i)) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    default -> a / b;
                };
                list.subList(i - 2, i + 1).clear();
                list.add(i - 2, String.valueOf(res));
                break;
            }
        }
    }
    return Integer.parseInt(list.get(0));
}
```

- **Time Complexity:** O(n²) — each of the ~n/2 reductions re-scans the (shrinking) list from the start to find the next operator, and each list mutation (`clear`/`add`) shifts elements.
- **Space Complexity:** O(n) — for the mutable copy of the token list.

---

## Optimal Approach (used in code)
```java
private static int evalRPN(String[] tokens) {
    Stack<Integer> st = new Stack<>();
    Set<String> set = Set.of("+", "-", "*", "/");
    for (String s : tokens) {
        if (!set.contains(s)) {
            st.push(Integer.valueOf(s));
        } else {
            int b = st.pop();
            int a = st.pop();
            switch (s) {
                case "+" -> st.push((a + b));
                case "-" -> st.push((a - b));
                case "*" -> st.push((a * b));
                case "/" -> st.push((a / b));
                default -> {
                }
            }
        }
    }
    return st.pop();
}
```

### Intuition
Postfix notation is *designed* for stack evaluation: by the time you encounter an operator, its two operands have always already appeared immediately before it (once earlier sub-expressions have already been collapsed). So there's no need to look backward/rescan for operands — they are simply "whatever is currently on top of the stack."

Scan left to right, one token at a time:
- A number is a value we haven't used yet → push it, it might be an operand for a later operator.
- An operator always applies to the **two most recently pushed, not-yet-used** values — which is precisely the top two elements of a stack. Pop them (`b` first since it's the more recent/right-hand operand, then `a`), compute, and push the result back — it becomes an operand for whatever comes later, just like a number token would.

Because each token triggers exactly one push (or one pop-pop-push), the whole expression is evaluated in a single linear pass, unlike brute force's repeated rescans of the shrinking token list.

- **Time Complexity:** O(n) — one pass, each token processed once.
- **Space Complexity:** O(n) — worst case (all numbers before any operator) the stack holds up to n/2 + 1 values.

---

## Dry Run
`tokens = ["2", "1", "+", "3", "*"]`

| token | action | stack (bottom → top) |
|-------|--------|------------------------|
| "2"   | push 2 | [2] |
| "1"   | push 1 | [2, 1] |
| "+"   | pop b=1, a=2 → push (2+1)=3 | [3] |
| "3"   | push 3 | [3, 3] |
| "*"   | pop b=3, a=3 → push (3*3)=9 | [9] |

**Result:** `9`
