# Merge Sort Dry Run with Heap and Stack Analysis

## Initial Array

```
arr = [38, 27, 43, 3, 9, 82, 10]  (indices 0-6, length=7)
```

---

## PHASE 1: DIVIDE (Recursion Tree)

```
                    mergeSort(0,6)
                    /           \
            mergeSort(0,3)    mergeSort(4,6)
            /        \           /      \
       mergeSort  mergeSort  mergeSort mergeSort
       (0,1)      (2,3)      (4,5)     (6,6)
       /   \      /   \      /   \       |
    (0,0) (1,1) (2,2) (3,3) (4,4) (5,5) (6,6)
```

### Recursion Call Sequence and Stack State:

**Step 1-2: Initial Call**

```
CALL STACK (growing):
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │ start < end? YES (0 < 6)
│ mid = 0 + 6/2 = 3              │
│ Calling: mergeSort(arr, 0, 3)   │
└─────────────────────────────────┘
```

**Step 3: First Left Recursion**

```
CALL STACK:
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 3)            │ start < end? YES (0 < 3)
│ mid = 0 + 3/2 = 1              │
│ Calling: mergeSort(arr, 0, 1)   │
└─────────────────────────────────┘
```

**Step 4: Continue Left**

```
CALL STACK:
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 3)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 1)            │ start < end? YES (0 < 1)
│ mid = 0 + 1/2 = 0              │
│ Calling: mergeSort(arr, 0, 0)   │
└─────────────────────────────────┘
```

**Step 5: Base Case - Left Leaf**

```
CALL STACK:
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 3)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 1)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 0)            │ start < end? NO (0 < 0 is false)
│ Return (BASE CASE)              │
└─────────────────────────────────┘
```

_Stack Pop_ → mergeSort(0,0) returns

**Step 6: Now Call Right of (0,1)**

```
CALL STACK:
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 3)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 1)            │ Calling: mergeSort(arr, 1, 1)
└─────────────────────────────────┘
```

**Step 7: Base Case - Right Leaf**

```
CALL STACK:
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 3)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 1)            │
├─────────────────────────────────┤
│ mergeSort(arr, 1, 1)            │ start < end? NO (1 < 1 is false)
│ Return (BASE CASE)              │
└─────────────────────────────────┘
```

_Stack Pop_ → mergeSort(1,1) returns

**Step 8: MERGE(arr, 0, 0, 1) - Merge [38] and [27]**

```
CALL STACK:
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 3)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 1)            │
├─────────────────────────────────┤
│ merge(arr, 0, 0, 1)             │
└─────────────────────────────────┘

HEAP ALLOCATION:
left[] = new int[0 - 0 + 1] = new int[1]  → left = [38]
right[] = new int[1 - 0] = new int[1]     → right = [27]

MERGE PROCESS:
i=0, j=0, k=0 (start=0)
Compare: left[0]=38 > right[0]=27?  NO
arr[0] = 27, k++, j++
i=0, j=1: j >= right.length, exit loop

Copy remaining from left:
arr[1] = 38, k++

RESULT: arr = [27, 38, 43, 3, 9, 82, 10]
```

_Stack Pop_ → merge returns, mergeSort(0,1) returns

**Step 9: Call mergeSort(2, 3)**

```
Continuing similar pattern...

CALL STACK:
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │
├─────────────────────────────────┤
│ mergeSort(arr, 0, 3)            │
├─────────────────────────────────┤
│ mergeSort(arr, 2, 3)            │
└─────────────────────────────────┘
```

**Step 10-11: Base cases for (2,2) and (3,3)**
Both return immediately (single elements)

**Step 12: MERGE(arr, 2, 2, 3) - Merge [43] and [3]**

```
HEAP ALLOCATION:
left[] = [43]
right[] = [3]

MERGE PROCESS:
Compare: 43 > 3?  YES
arr[2] = 3, k++, j++

Copy remaining from left:
arr[3] = 43

RESULT: arr = [27, 38, 3, 43, 9, 82, 10]
```

**Step 13: MERGE(arr, 0, 1, 3) - Merge [27, 38] and [3, 43]**

```
HEAP ALLOCATION:
left[] = new int[1 - 0 + 1] = new int[2]  → left = [27, 38]
right[] = new int[3 - 1] = new int[2]     → right = [3, 43]

MERGE PROCESS:
i=0, j=0, k=0 (start=0)
Compare: 27 > 3?   YES → arr[0] = 3, j++
Compare: 27 > 43?  NO  → arr[1] = 27, i++
Compare: 38 > 43?  NO  → arr[2] = 38, i++
i >= left.length, exit main loop

Copy remaining from right:
arr[3] = 43

RESULT: arr = [3, 27, 38, 43, 9, 82, 10]
```

**Step 14: Call mergeSort(4, 6)**

```
CALL STACK:
┌─────────────────────────────────┐
│ mergeSort(arr, 0, 6)            │
├─────────────────────────────────┤
│ mergeSort(arr, 4, 6)            │
└─────────────────────────────────┘

mid = 4 + 2/2 = 5
Calling: mergeSort(arr, 4, 5)
```

**Step 15-16: Base cases for (4,4) and (5,5)**
Both return immediately

**Step 17: MERGE(arr, 4, 4, 5) - Merge [9] and [82]**

```
HEAP ALLOCATION:
left[] = [9]
right[] = [82]

MERGE PROCESS:
Compare: 9 > 82?  NO
arr[4] = 9, i++
i >= left.length, exit

Copy remaining from right:
arr[5] = 82

RESULT: arr = [3, 27, 38, 43, 9, 82, 10]
```

**Step 18: Call mergeSort(6, 6)**
Base case - returns immediately

**Step 19: MERGE(arr, 4, 5, 6) - Merge [9, 82] and [10]**

```
HEAP ALLOCATION:
left[] = new int[5 - 4 + 1] = new int[2]  → left = [9, 82]
right[] = new int[6 - 5] = new int[1]     → right = [10]

MERGE PROCESS:
i=0, j=0, k=4 (start=4)
Compare: 9 > 10?   NO  → arr[4] = 9, i++
Compare: 82 > 10?  YES → arr[5] = 10, j++
j >= right.length, exit

Copy remaining from left:
arr[6] = 82

RESULT: arr = [3, 27, 38, 43, 9, 10, 82]
```

**Step 20: MERGE(arr, 0, 3, 6) - Merge [3, 27, 38, 43] and [9, 10, 82]**

```
HEAP ALLOCATION:
left[] = new int[3 - 0 + 1] = new int[4]   → left = [3, 27, 38, 43]
right[] = new int[6 - 3] = new int[3]      → right = [9, 10, 82]

MERGE PROCESS:
i=0, j=0, k=0 (start=0)

Iteration 1: Compare 3 > 9?   NO  → arr[0] = 3, i++ (i=1)
Iteration 2: Compare 27 > 9?  YES → arr[1] = 9, j++ (j=1)
Iteration 3: Compare 27 > 10? YES → arr[2] = 10, j++ (j=2)
Iteration 4: Compare 27 > 82? NO  → arr[3] = 27, i++ (i=2)
Iteration 5: Compare 38 > 82? NO  → arr[4] = 38, i++ (i=3)
Iteration 6: Compare 43 > 82? NO  → arr[5] = 43, i++ (i=4)
i >= left.length, exit

Copy remaining from right:
arr[6] = 82

FINAL RESULT: arr = [3, 9, 10, 27, 38, 43, 82]
```

---

## FINAL OUTPUT

```
Sorted array:
3 9 10 27 38 43 82
```

---

## Memory Analysis

### Time Complexity: O(n log n)

- **Divide Phase:** O(log n) - recursive depth
- **Merge Phase:** O(n) - at each level
- **Total:** O(n log n)

### Space Complexity: O(n)

- **Temporary Arrays:** At each merge, we create left[] and right[] arrays
- **Recursion Stack:** O(log n) - maximum depth
- **Total:** O(n) for temporary arrays

### Maximum Stack Depth Reached: 4

- mergeSort(0,6) → mergeSort(0,3) → mergeSort(0,1) → mergeSort(0,0) [depth=4]

### Total Merge Operations: 7

- 1 at level 3 (merging singles): 3 merges
- 1 at level 2 (merging pairs): 2 merges
- 1 at level 1 (merging groups): 2 merges
- 1 at level 0 (merging halves): 1 merge
- **Total: 7 merges = n-1**

### Heap Allocations During Execution:

```
1. merge(0,0,1):  left[1], right[1]  → 2 arrays
2. merge(2,2,3):  left[1], right[1]  → 2 arrays
3. merge(0,1,3):  left[2], right[2]  → 2 arrays
4. merge(4,4,5):  left[1], right[1]  → 2 arrays
5. merge(4,5,6):  left[2], right[1]  → 2 arrays
6. merge(0,3,6):  left[4], right[3]  → 2 arrays
Total: 12 temporary arrays allocated throughout execution
(Note: Each is garbage collected after merge completes)
```

---

## Key Observations

1. **Divide Phase is Free:** No swaps or comparisons, just recursive calls
2. **All Work Happens in Merge:** Actual sorting/comparisons happen during merge phase
3. **Stable Sort:** Equal elements maintain their relative order
4. **In-place Merge:** Not truly in-place (creates temporary arrays), but modifies original array
5. **Balanced Recursion:** Creates perfectly balanced tree for powers of 2, slightly unbalanced otherwise
