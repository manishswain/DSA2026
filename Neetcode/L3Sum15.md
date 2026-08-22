# LeetCode 15 - 3Sum

Given an integer array, find all unique triplets `[nums[i], nums[j], nums[k]]`
(distinct indices) that sum to zero. The result must not contain duplicate
triplets.

## Brute Force

Try every combination of three distinct indices `(i, j, k)` and check if
they sum to zero, adding the triplet to a result set (e.g., a `Set` of
sorted lists) to filter out duplicates. The intuition is the literal
definition of the problem — enumerate all possible triplets. It's correct
but extremely wasteful, and de-duplication requires normalizing (e.g.,
sorting) each found triplet and checking against previously found ones.

- Time: O(n^3) for the triple nested loop, plus overhead for
  de-duplication.
- Space: O(n) to O(n^2) depending on how many triplets are stored, plus
  space for a de-duplication set.

```java
private static List<List<Integer>> threeSumBruteForce(int[] nums) {
    Set<List<Integer>> seen = new HashSet<>();
    int n = nums.length;
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            for (int k = j + 1; k < n; k++) {
                if (nums[i] + nums[j] + nums[k] == 0) {
                    List<Integer> triplet = Arrays.asList(nums[i], nums[j], nums[k]);
                    triplet.sort(null);
                    seen.add(triplet);
                }
            }
        }
    }
    return new ArrayList<>(seen);
}
```

## Optimal Solution

Sort the array first. Then, for each index `i` (stopping once `nums[i] > 0`
since three sorted non-negative numbers can't sum to zero unless all are
zero), skip duplicate values of `nums[i]` to avoid duplicate triplets, and
use a two-pointer scan (`j = i + 1`, `k = n - 1`) over the remaining sorted
sub-array to find pairs whose sum equals `-nums[i]`. This reduces 3Sum to
repeated applications of the sorted two-pointer "Two Sum" technique. The
intuition: sorting turns "find a pair summing to a target" into an O(n)
two-pointer scan (as in Two Sum II), and fixing one number at a time while
skipping duplicates at both the outer and inner levels naturally avoids
generating duplicate triplets, without needing a de-duplication set.

- Time: O(n^2) — O(n log n) sort + O(n) outer loop, each doing an O(n)
  two-pointer scan.
- Space: O(n) for the sort (or O(log n) to O(n) depending on the sort
  implementation), plus O(n) for the sorted copy if not sorting in place;
  output space isn't counted separately.

```java
private static List<List<Integer>> threeSumOptimal(int[] nums) {
    List<List<Integer>> res = new ArrayList<>();
    Arrays.sort(nums);
    for (int i = 0; i < nums.length && nums[i] <= 0; i++) {
        if (i == 0 || nums[i] != nums[i - 1]) { // skip duplicate anchors
            twoSumTwoPointers(nums, i, res);
        }
    }
    return res;
}

private static void twoSumTwoPointers(int[] nums, int i, List<List<Integer>> res) {
    int j = i + 1, k = nums.length - 1;
    int target = -nums[i];
    while (j < k) {
        int sum = nums[j] + nums[k];
        if (sum < target) {
            j++;
        } else if (sum > target) {
            k--;
        } else {
            res.add(List.of(nums[i], nums[j], nums[k]));
            j++;
            k--;
            while (j < k && nums[j] == nums[j - 1]) j++; // skip duplicate j
        }
    }
}
```
