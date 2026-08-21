# Rotting Oranges (LeetCode 994)

## Problem
Given a `grid` where each cell is:
- `0` — empty cell
- `1` — fresh orange
- `2` — rotten orange

Every minute, any fresh orange adjacent (4-directionally: up/down/left/right) to a rotten orange becomes rotten. Return the minimum number of minutes until no fresh orange remains. If that is impossible, return `-1`.

Example: `grid = [[2,1,1],[1,1,0],[0,1,1]]` → answer `4`.

---

## Brute Force Approach
Simulate minute-by-minute: on each pass, scan the *entire* grid, and for every rotten orange found, mark its fresh neighbors to become rotten "next minute" (using a temporary copy so we don't rot oranges in the same pass that were only just infected). Repeat until a full pass produces no new rot. Count the passes.

```java
private static int orangesRottingBruteForce(int[][] grid) {
    int rows = grid.length, cols = grid[0].length;
    int minutes = 0;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

    while (true) {
        int[][] next = deepCopy(grid);
        boolean rottedAny = false;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 2) {
                    for (int[] d : dirs) {
                        int nr = r + d[0], nc = c + d[1];
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] == 1) {
                            next[nr][nc] = 2;
                            rottedAny = true;
                        }
                    }
                }
            }
        }

        if (!rottedAny) break;
        grid = next;
        minutes++;
    }

    for (int[] row : grid) {
        for (int cell : row) {
            if (cell == 1) return -1;
        }
    }
    return minutes;
}
```

- **Time Complexity:** O(rows × cols) per minute, and up to O(rows × cols) minutes in the worst case → **O((rows × cols)²)**. Every pass re-scans the whole grid even though only the rotten cells from the previous minute can spread further.
- **Space Complexity:** O(rows × cols) — for the grid copy each minute.

---

## Optimal Approach (multi-source BFS)
```java
private static int orangesRotting(int[][] grid) {
    int rows = grid.length, cols = grid[0].length;
    Queue<int[]> queue = new LinkedList<>();
    int freshCount = 0;

    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            if (grid[r][c] == 2) {
                queue.offer(new int[]{r, c});
            } else if (grid[r][c] == 1) {
                freshCount++;
            }
        }
    }

    if (freshCount == 0) return 0;

    int minutes = 0;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

    while (!queue.isEmpty() && freshCount > 0) {
        int size = queue.size();
        minutes++;
        for (int i = 0; i < size; i++) {
            int[] cell = queue.poll();
            for (int[] d : dirs) {
                int nr = cell[0] + d[0], nc = cell[1] + d[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] == 1) {
                    grid[nr][nc] = 2;
                    freshCount--;
                    queue.offer(new int[]{nr, nc});
                }
            }
        }
    }

    return freshCount == 0 ? minutes : -1;
}
```

### Intuition
The brute force wastes time re-scanning the whole grid every minute even though only cells that *just* turned rotten can infect new neighbors this minute — everything else is already rotten, empty, or too far away to matter yet.

This is exactly the shape of a **multi-source BFS**: instead of one starting point, every initially-rotten orange is a BFS source, and they all expand outward "in parallel," level by level. Each BFS level corresponds exactly to one minute passing, because BFS visits nodes in increasing order of distance from the sources — and here "distance" *is* minutes.

Concretely:
1. Push every rotten orange into the queue up front (all sources start at minute 0), and count the fresh oranges so we know when we're done.
2. Process the queue **one full level (minute) at a time** — capture `queue.size()` before the inner loop so we only expand the oranges that rotted in the *previous* minute, not ones we're adding *this* minute.
3. For each rotten orange in the current level, rot its fresh neighbors immediately (in-place — no need for a temporary copy, since BFS never revisits a cell), decrement the fresh count, and enqueue them as next level's sources.
4. Increment `minutes` once per level, not per cell.
5. Stop when the queue empties. If `freshCount` reached `0`, every reachable fresh orange rotted — return `minutes`. Otherwise some oranges were unreachable — return `-1`.

Because every cell is enqueued and processed exactly once, we never redo work the way brute force's full-grid rescans do — the BFS frontier itself tells us which cells are relevant this minute.

- **Time Complexity:** O(rows × cols) — every cell is visited once.
- **Space Complexity:** O(rows × cols) — queue can hold up to all cells in the worst case.

---

## Dry Run
`grid = [[2,1,1],[1,1,0],[0,1,1]]`

Initial queue (rotten sources): `(0,0)` → `freshCount = 6`

**Minute 1** (process level with `(0,0)`):
- `(0,0)` rots `(0,1)` and `(1,0)` → both become `2`, enqueued. `freshCount = 4`.

Grid: `[[2,2,1],[2,1,0],[0,1,1]]`

**Minute 2** (process level with `(0,1)`, `(1,0)`):
- `(0,1)` rots `(0,2)` and `(1,1)` → `freshCount = 2`.
- `(1,0)` has no fresh neighbors left to rot.

Grid: `[[2,2,2],[2,2,0],[0,1,1]]`

**Minute 3** (process level with `(0,2)`, `(1,1)`):
- `(1,1)` rots `(2,1)` → `freshCount = 1`.
- `(0,2)` has no fresh neighbors.

Grid: `[[2,2,2],[2,2,0],[0,2,1]]`

**Minute 4** (process level with `(2,1)`):
- `(2,1)` rots `(2,2)` → `freshCount = 0`.

Grid: `[[2,2,2],[2,2,0],[0,2,2]]`

Queue empties, `freshCount == 0` → **Result: `4`**.
