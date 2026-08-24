# LeetCode 253 - Meeting Rooms II

Given an array of meeting time intervals `intervals` where `intervals[i] = [start_i, end_i]`, return the minimum number of conference rooms required to hold all the meetings.

## Approach 1: Min-Heap (Priority Queue)

**Intuition:** Sort meetings by start time, then walk through them in order. Keep a min-heap of the end times of meetings currently "in progress" (i.e., using a room). For each new meeting, first check if the room that frees up earliest (heap top) has already ended by the time this meeting starts — if so, reuse that room by popping it off the heap. Then push the current meeting's end time onto the heap (either into the freed slot or as a new room). The heap size at any point represents rooms in use; the maximum heap size seen is the answer.

**Time Complexity:** O(n log n) — sorting plus heap operations.
**Space Complexity:** O(n) — heap can hold up to n end times.

```java
private static int minMeetingRoomsHeap(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    PriorityQueue<Integer> endTimes = new PriorityQueue<>();

    for (int[] interval : intervals) {
        if (!endTimes.isEmpty() && endTimes.peek() <= interval[0]) {
            endTimes.poll();
        }
        endTimes.offer(interval[1]);
    }
    return endTimes.size();
}
```

## Optimal Solution: Chronological Ordering (Sorted Starts/Ends)

**Intuition:** A room is needed whenever a meeting starts, and a room is freed whenever a meeting ends. We don't actually need to track *which* meeting occupies *which* room — only the count of overlapping meetings at any instant. So separate the start times and end times into two arrays and sort each independently (pairing with original intervals is not needed since we only care about counts, not identity). Then merge-walk both sorted arrays with two pointers, similar to merging two sorted lists: if the next start happens before (or at the same time as processing) the next end, a new room is required (`rooms++`); otherwise a room is freed (`rooms--`). Track the maximum `rooms` value seen — that's the minimum number of rooms needed. This avoids the heap entirely, using only sorting.

Note: `start[s] < end[e]` (strict) means a meeting that starts exactly when another ends does NOT need a new room, since the ending meeting frees its room before/at the same instant.

**Time Complexity:** O(n log n) — dominated by sorting the two arrays.
**Space Complexity:** O(n) — two auxiliary arrays for start/end times (O(1) extra if sorting in place, excluding sort's own space).

```java
private static int minMeetingRooms(int[][] intervals) {
    int n = intervals.length;
    int[] start = new int[n];
    int[] end = new int[n];

    for (int i = 0; i < n; i++) {
        start[i] = intervals[i][0];
        end[i] = intervals[i][1];
    }

    Arrays.sort(start);
    Arrays.sort(end);

    int rooms = 0, maxRooms = 0;
    int s = 0, e = 0;

    while (s < n) {
        if (start[s] < end[e]) {
            rooms++;
            s++;
        } else {
            rooms--;
            e++;
        }
        maxRooms = Math.max(maxRooms, rooms);
    }

    return maxRooms;
}
```

**Example:** `intervals = [[0,30],[5,10],[15,20]]`

`start = [0, 5, 15]` (sorted)
`end = [10, 20, 30]` (sorted)

**Dry Run:**
```
s=0, e=0, rooms=0, maxRooms=0

start[0]=0 < end[0]=10 -> rooms++  rooms=1  s=1   maxRooms=1
start[1]=5 < end[0]=10 -> rooms++  rooms=2  s=2   maxRooms=2
start[2]=15 < end[0]=10? No (15 >= 10) -> rooms--  rooms=1  e=1   maxRooms=2
start[2]=15 < end[1]=20 -> rooms++  rooms=2  s=3   maxRooms=2

s == n(3) -> loop ends
```
Output: `2`

(Meeting `[0,30]` overlaps with `[5,10]`, needing 2 rooms; `[15,20]` fits into the room freed by `[5,10]`.)
