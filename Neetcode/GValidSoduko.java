package Neetcode;

import java.util.HashSet;
import java.util.Set;

//2D Array and HashSet Question
//Leetcode 36 - Valid Sudoku
public class GValidSoduko {
    public static void main(String[] args) {
        char[][] board = {
                { '5', '3', '.', '1', '7', '.', '.', '.', '.' },
                { '6', '.', '.', '1', '9', '5', '.', '.', '.' },
                { '.', '9', '8', '.', '.', '.', '.', '6', '.' },
                { '8', '.', '.', '.', '6', '.', '.', '.', '3' },
                { '4', '.', '.', '8', '.', '3', '.', '.', '1' },
                { '7', '.', '.', '.', '2', '.', '.', '.', '6' },
                { '.', '6', '.', '.', '.', '.', '2', '8', '.' },
                { '.', '.', '.', '4', '1', '9', '.', '.', '5' },
                { '.', '.', '.', '.', '8', '.', '.', '7', '9' }
        };
        boolean isValid = isValidSudoku(board);
        System.out.println("Is the Sudoku board valid? " + isValid);
    }
    // Approach: We can use three sets to keep track of the numbers we have seen in
    // each row, column, and 3x3 box. As we iterate through the board, we check if
    // the current number has already been seen in the corresponding row, column, or
    // box. If it has, then the board is not valid. If it hasn't, we add it to the
    // respective sets. This approach has a time complexity of O(1) for each cell,
    // resulting in an overall time complexity of O(9*9) = O(1) since the board size
    // is fixed.
    // Box Logic : boxIndex = (row / 3) * 3 + (col / 3)

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

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                char val = board[r][c];

                if (Character.isDigit(val)) {
                    if (rowSets[r].contains(val)) {
                        return false;
                    }
                    rowSets[r].add(val);
                    if (columnSets[c].contains(val)) {
                        return false;
                    }
                    columnSets[c].add(val);

                    int boxIndex = r / 3 * 3 + c / 3;
                    if (boxSets[boxIndex].contains(val)) {
                        return false;
                    }
                    boxSets[boxIndex].add(val);
                }
            }
        }
        return true;
    }

    private static boolean isValidSudokuRowAndColumn(char[][] board) {

        for (int i = 0; i < 9; i++) {
            Set<Character> set = new HashSet<>();
            for (int j = 0; j < 9; j++) {
                if (Character.isDigit(board[i][j])) {
                    if (!set.add(board[i][j])) {
                        return false;
                    }
                }
            }

        }
        for (int j = 0; j < 9; j++) {
            Set<Character> set = new HashSet<>();
            for (int i = 0; i < 9; i++) {
                if (Character.isDigit(board[i][j])) {
                    if (!set.add(board[i][j])) {
                        return false;
                    }
                }
            }

        }
        return true;
    }
}
