package LLD;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * TicTacToe — Low Level Design
 * --------------------------------------------------------------
 * Two-player console game on an NxN board (default 3x3).
 * Players place X / O alternately. X always starts.
 * Win = N matching symbols in a row, column, or diagonal.
 * Draw = board full with no winner.
 *
 * Design highlights:
 * - Single Responsibility: each class has one clear job.
 * - Strategy pattern: win-detection isolated behind WinStrategy (OCP).
 * - Efficient win-check: only inspect row/col/diagonal through the
 * last placed move (O(n) instead of scanning the full board).
 * - Turn management via Deque<Player>: peek = current, swap = next.
 * - Board is size-agnostic — easy to extend to 4x4, 5x5, etc.
 * - Console IO is kept out of the game core (Game knows nothing
 * about Scanner); only the driver (main) reads input.
 */
public class TicTacToeLLD {

    /*
     * ============================================================
     * 1. Symbol — the three possible marks on a cell
     * ============================================================
     */
    enum Symbol {
        X, O, EMPTY;

        boolean isEmpty() {
            return this == EMPTY;
        }
    }

    /*
     * ============================================================
     * 2. GameStatus — high-level outcome the game can be in
     * ============================================================
     */
    enum GameStatus {
        IN_PROGRESS, WIN, DRAW
    }

    /*
     * ============================================================
     * 3. Cell — immutable (row, col) coordinate
     * Using a value object avoids passing two ints everywhere
     * and makes the API self-documenting.
     * ============================================================
     */
    static final class Cell {
        private final int row;
        private final int col;

        Cell(int row, int col) {
            this.row = row;
            this.col = col;
        }

        int getRow() {
            return row;
        }

        int getCol() {
            return col;
        }

        @Override
        public String toString() {
            return "(" + row + "," + col + ")";
        }
    }

    /*
     * ============================================================
     * 4. Player — minimal identity (name + symbol)
     * Could later be subclassed into HumanPlayer / AIPlayer.
     * ============================================================
     */
    static final class Player {
        private final String name;
        private final Symbol symbol;

        Player(String name, Symbol symbol) {
            this.name = name;
            this.symbol = symbol;
        }

        String getName() {
            return name;
        }

        Symbol getSymbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return name + "(" + symbol + ")";
        }
    }

    /*
     * ============================================================
     * 5. WinStrategy — Strategy pattern
     * Decouples "what counts as a win" from the game loop,
     * so we can plug in different rules (Gomoku, Connect-N, etc.)
     * without touching TicTacToeGame.
     * ============================================================
     */
    interface WinStrategy {
        /**
         * @return the winning Symbol if the move at lastMove won,
         *         otherwise null (game may continue or be a draw).
         */
        Symbol checkWinner(Board board, Cell lastMove);
    }

    /*
     * ============================================================
     * 6. DefaultWinStrategy — standard row/col/diagonal check
     * Only inspects the line(s) passing through lastMove: O(n).
     * ============================================================
     */
    static final class DefaultWinStrategy implements WinStrategy {

        @Override
        public Symbol checkWinner(Board board, Cell lastMove) {
            int size = board.getSize();
            int r = lastMove.getRow();
            int c = lastMove.getCol();
            Symbol s = board.getSymbolAt(lastMove);
            if (s.isEmpty())
                return null;

            // --- row ---
            boolean rowWin = true;
            for (int j = 0; j < size; j++) {
                if (board.getSymbolAt(new Cell(r, j)) != s) {
                    rowWin = false;
                    break;
                }
            }
            if (rowWin)
                return s;

            // --- column ---
            boolean colWin = true;
            for (int i = 0; i < size; i++) {
                if (board.getSymbolAt(new Cell(i, c)) != s) {
                    colWin = false;
                    break;
                }
            }
            if (colWin)
                return s;

            // --- main diagonal (only if move is on it) ---
            if (r == c) {
                boolean diagWin = true;
                for (int i = 0; i < size; i++) {
                    if (board.getSymbolAt(new Cell(i, i)) != s) {
                        diagWin = false;
                        break;
                    }
                }
                if (diagWin)
                    return s;
            }

            // --- anti-diagonal (only if move is on it) ---
            if (r + c == size - 1) {
                boolean antiWin = true;
                for (int i = 0; i < size; i++) {
                    if (board.getSymbolAt(new Cell(i, size - 1 - i)) != s) {
                        antiWin = false;
                        break;
                    }
                }
                if (antiWin)
                    return s;
            }

            return null;
        }
    }

    /*
     * ============================================================
     * 7. Board — owns the grid, validates moves, renders itself
     * ============================================================
     */
    static final class Board {
        private final int size;
        private final Symbol[][] grid;

        Board(int size) {
            if (size < 3) {
                throw new IllegalArgumentException("Board size must be >= 3");
            }
            this.size = size;
            this.grid = new Symbol[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    grid[i][j] = Symbol.EMPTY;
                }
            }
        }

        int getSize() {
            return size;
        }

        Symbol getSymbolAt(Cell cell) {
            return grid[cell.getRow()][cell.getCol()];
        }

        boolean isValidMove(Cell cell) {
            return inBounds(cell) && grid[cell.getRow()][cell.getCol()].isEmpty();
        }

        void placeSymbol(Cell cell, Symbol symbol) {
            if (!isValidMove(cell)) {
                throw new IllegalStateException("Invalid move at " + cell);
            }
            grid[cell.getRow()][cell.getCol()] = symbol;
        }

        boolean isFull() {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (grid[i][j].isEmpty())
                        return false;
                }
            }
            return true;
        }

        /** Render the board to the console. */
        void display() {
            StringBuilder sb = new StringBuilder();
            String hSep = repeat("+---", size) + "+";
            for (int i = 0; i < size; i++) {
                sb.append(hSep).append('\n');
                sb.append("|");
                for (int j = 0; j < size; j++) {
                    String token = grid[i][j].isEmpty() ? " " : grid[i][j].name();
                    sb.append(" ").append(token).append(" |");
                }
                sb.append('\n');
            }
            sb.append(hSep);
            System.out.println(sb);
        }

        private boolean inBounds(Cell cell) {
            return cell.getRow() >= 0 && cell.getRow() < size
                    && cell.getCol() >= 0 && cell.getCol() < size;
        }

        private static String repeat(String s, int times) {
            StringBuilder sb = new StringBuilder(s.length() * times);
            for (int i = 0; i < times; i++)
                sb.append(s);
            return sb.toString();
        }
    }

    /*
     * ============================================================
     * 8. TicTacToeGame — orchestrates a match
     * Knows: the board, the two players, whose turn it is,
     * the win strategy, and the current GameStatus.
     * Does NOT know about input/output — the driver does that.
     * ============================================================
     */
    static final class TicTacToeGame {
        private final Board board;
        private final Deque<Player> turnQueue; // head = current player
        private final WinStrategy winStrategy;

        private GameStatus status = GameStatus.IN_PROGRESS;
        private Player winner;

        TicTacToeGame(Player p1, Player p2, int boardSize, WinStrategy winStrategy) {
            if (p1.getSymbol() == p2.getSymbol()) {
                throw new IllegalArgumentException("Players must use different symbols");
            }
            this.board = new Board(boardSize);
            this.turnQueue = new ArrayDeque<>();
            this.turnQueue.add(p1);
            this.turnQueue.add(p2);
            this.winStrategy = winStrategy;
        }

        /** Constructor overload with default 3x3 board & default strategy. */
        TicTacToeGame(Player p1, Player p2) {
            this(p1, p2, 3, new DefaultWinStrategy());
        }

        GameStatus getStatus() {
            return status;
        }

        Player getWinner() {
            return winner;
        }

        Player getCurrentPlayer() {
            return turnQueue.peek();
        }

        Board getBoard() {
            return board;
        }

        /**
         * Attempt a move for the current player at the given cell.
         * 
         * @return true if the move was accepted (game continues or ends
         *         via this move); false if the move was invalid.
         *         Throws nothing — invalid moves are reported via the return
         *         value so the driver can re-prompt.
         */
        boolean makeMove(Cell cell) {
            if (status != GameStatus.IN_PROGRESS) {
                return false;
            }
            Player current = turnQueue.peek();
            if (!board.isValidMove(cell)) {
                return false;
            }
            board.placeSymbol(cell, current.getSymbol());

            // 1) Did this move win?
            Symbol winningSymbol = winStrategy.checkWinner(board, cell);
            if (winningSymbol != null) {
                status = GameStatus.WIN;
                winner = (winningSymbol == current.getSymbol()) ? current : null;
                return true;
            }

            // 2) Or did it fill the board (draw)?
            if (board.isFull()) {
                status = GameStatus.DRAW;
                return true;
            }

            // 3) Otherwise rotate to the other player.
            turnQueue.add(turnQueue.poll());
            return true;
        }
    }

    /*
     * ============================================================
     * 9. Driver — main(): wires input/output to the game.
     * Kept separate from the game core so the engine is testable
     * without a console (unit tests can call makeMove() directly).
     * ============================================================
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Player p1 = new Player("Player 1", Symbol.X);
        Player p2 = new Player("Player 2", Symbol.O);
        TicTacToeGame game = new TicTacToeGame(p1, p2);

        System.out.println("=== Tic-Tac-Toe (3x3) ===");
        System.out.println(p1 + " vs " + p2 + " — " + p1 + " goes first.\n");
        game.getBoard().display();

        while (game.getStatus() == GameStatus.IN_PROGRESS) {
            Player current = game.getCurrentPlayer();
            System.out.print(current.getName()
                    + " (" + current.getSymbol() + ")"
                    + " — enter row and col (0-" + (game.getBoard().getSize() - 1) + "): ");

            int row, col;
            try {
                row = scanner.nextInt();
                col = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Please enter two integers.\n");
                scanner.nextLine(); // discard bad input
                continue;
            }

            boolean accepted = game.makeMove(new Cell(row, col));
            if (!accepted) {
                System.out.println("Invalid move. Cell out of bounds or already taken. Try again.\n");
                continue;
            }

            game.getBoard().display();
        }

        // --- Announce result ---
        switch (game.getStatus()) {
            case WIN:
                System.out.println("🏆 Winner: " + game.getWinner().getName()
                        + " (" + game.getWinner().getSymbol() + ")");
                break;
            case DRAW:
                System.out.println("It's a DRAW!");
                break;
            default:
                break;
        }

        scanner.close();
    }
}
