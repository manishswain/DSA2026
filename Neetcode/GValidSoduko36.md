# LeetCode 36 - Valid Sudoku

Given a 9x9 Sudoku board (partially filled, empty cells marked `.`), determine
whether the currently filled cells satisfy Sudoku validity rules: each row,
each column, and each of the nine 3x3 sub-boxes must contain each digit 1-9
at most once. You are only validating the current state, not solving the
puzzle.

## Brute Force

For every filled cell, scan its entire row, its entire column, and its 3x3
box to check whether the same digit appears elsewhere. The intuition is
simply "directly re-check the rule definition" — for each digit placed, walk
every other cell that shares a row/column/box with it and compare values.
This repeats a lot of work: the same row is rescanned for every cell in it.

- Time: O(1) technically since the board is fixed at 9x9, but conceptually
  O(N^2) work per cell → O(N^4) ~ O(81 * 27) comparisons for a general N x N
  board.
- Space: O(1) extra space.

```java
private static boolean isValidBruteForce(char[][] board) {
    int N = 9;
    for (int r = 0; r < N; r++) {
        for (int c = 0; c < N; c++) {
            char val = board[r][c];
            if (val == '.') continue;
            // check row
            for (int cc = 0; cc < N; cc++) {
                if (cc != c && board[r][cc] == val) return false;
            }
            // check column
            for (int rr = 0; rr < N; rr++) {
                if (rr != r && board[rr][c] == val) return false;
            }
            // check box
            int boxRow = (r / 3) * 3, boxCol = (c / 3) * 3;
            for (int rr = boxRow; rr < boxRow + 3; rr++) {
                for (int cc = boxCol; cc < boxCol + 3; cc++) {
                    if ((rr != r || cc != c) && board[rr][cc] == val) return false;
                }
            }
        }
    }
    return true;
}
```

**Example:** A 9x9 board where row 0 is `['5','3','.','.','7','.','.','.','.']` and all other cells are `.`, except `board[1][0] = '5'` (a duplicate `5` in column 0).

**Dry Run:**
```
r=0, c=0, val='5': check row 0 -> no other '5' in row; check column 0 -> board[1][0]='5' matches -> return false
```
Output: `false`

## Optimal Solution

Instead of re-scanning neighbors for every cell, do a single pass over the
board and maintain one `HashSet<Character>` per row, per column, and per box
(27 sets total). For each filled cell, check if the digit is already present
in the corresponding row/column/box set — if so, the board is invalid;
otherwise add it. The key intuition: validity only requires "have I seen
this digit before in this row/column/box?", which a set answers in O(1),
so tracking "seen so far" replaces the need to rescan.

The box a cell belongs to is computed as `boxIndex = (row / 3) * 3 + (col / 3)`,
which maps the 9 3x3 boxes to indices 0-8 in row-major order.

- Time: O(1) since the board size is fixed at 81 cells (generally O(N^2) for
  an N x N board — one pass over all cells).
- Space: O(1) fixed (27 sets bounded by 9 digits each) — generally O(N) for
  an N x N board.

```java
private static boolean isValidSudoku(char[][] board) {
    int N = 9;
    HashSet<Character>[] rowSets = new HashSet[N];
    HashSet<Character>[] columnSets = new HashSet[N];
    HashSet<Character>[] boxSets = new HashSet[N];
    for (int r = 0; r < N; r++) {
        rowSets[r] = new HashSet<>();
        columnSets[r] = new HashSet<>();
        boxSets[r] = new HashSet<>();
    }

    for (int r = 0; r < N; r++) {
        for (int c = 0; c < N; c++) {
            char val = board[r][c];
            if (!Character.isDigit(val)) continue;

            if (!rowSets[r].add(val)) return false;
            if (!columnSets[c].add(val)) return false;

            int boxIndex = (r / 3) * 3 + (c / 3);
            if (!boxSets[boxIndex].add(val)) return false;
        }
    }
    return true;
}
```

**Example:** Same board as above — row 0 is `['5','3','.','.','7','.','.','.','.']`, all other cells `.`, except `board[1][0] = '5'`.

**Dry Run:**
```
r=0, c=0, val='5': rowSets[0].add('5') -> true; columnSets[0].add('5') -> true; boxIndex=(0/3)*3+(0/3)=0; boxSets[0].add('5') -> true
r=0, c=1, val='3': rowSets[0].add('3') -> true; columnSets[1].add('3') -> true; boxSets[0].add('3') -> true
r=0, c=4, val='7': rowSets[0].add('7') -> true; columnSets[4].add('7') -> true; boxIndex=(0/3)*3+(4/3)=1; boxSets[1].add('7') -> true
r=1, c=0, val='5': rowSets[1].add('5') -> true; columnSets[0].add('5') -> false (already present) -> return false
```
Output: `false`
