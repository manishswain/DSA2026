package LLD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public class ChessLLD {

    private final ChessBoard board;
    private final EnumMap<Color, Position> kingPositions;
    private final List<Move> moveHistory;

    private Color currentTurn;
    private GameStatus status;

    public ChessLLD() {
        this.board = new ChessBoard();
        this.kingPositions = new EnumMap<>(Color.class);
        this.kingPositions.put(Color.WHITE, new Position(7, 4));
        this.kingPositions.put(Color.BLACK, new Position(0, 4));
        this.moveHistory = new ArrayList<>();
        this.currentTurn = Color.WHITE;
        this.status = GameStatus.WHITE_TO_MOVE;
        initializePieces();
    }

    public static void main(String[] args) {
        ChessLLD game = new ChessLLD();
        game.printBoard();

        game.makeMove(Position.of("e2"), Position.of("e4"));
        game.makeMove(Position.of("e7"), Position.of("e5"));

        System.out.println("Current turn: " + game.getCurrentTurn());
        System.out.println("Status: " + game.getStatus());
        System.out.println("White legal moves from e2: " + game.getLegalMoves(Position.of("e2")));
    }

    public ChessBoard getBoard() {
        return board;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public List<Move> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    public List<Move> getLegalMoves(Position from) {
        List<Move> moves = new ArrayList<>();
        Piece piece = board.getPiece(from);

        if (piece == null || piece.getColor() != currentTurn) {
            return moves;
        }

        for (int row = 0; row < ChessBoard.SIZE; row++) {
            for (int col = 0; col < ChessBoard.SIZE; col++) {
                Position to = new Position(row, col);
                if (piece.canMove(from, to, board) && !wouldKingBeInCheck(piece.getColor(), from, to, piece)) {
                    moves.add(new Move(from, to, piece));
                }
            }
        }

        return moves;
    }

    public boolean makeMove(Position from, Position to) {
        Piece piece = board.getPiece(from);

        if (piece == null || piece.getColor() != currentTurn || !piece.canMove(from, to, board)) {
            return false;
        }

        if (wouldKingBeInCheck(piece.getColor(), from, to, piece)) {
            return false;
        }

        Move move = new Move(from, to, piece);
        moveHistory.add(move);

        board.setPiece(to.row, to.col, piece);
        board.setPiece(from.row, from.col, null);

        if (piece.getType() == PieceType.KING) {
            kingPositions.put(piece.getColor(), to);
        }

        currentTurn = currentTurn == Color.WHITE ? Color.BLACK : Color.WHITE;
        updateStatus();

        return true;
    }

    public boolean isLegalMove(Position from, Position to) {
        Piece piece = board.getPiece(from);
        return piece != null
                && piece.getColor() == currentTurn
                && piece.canMove(from, to, board)
                && !wouldKingBeInCheck(piece.getColor(), from, to, piece);
    }

    public boolean isCheck(Color color) {
        Position kingPosition = kingPositions.get(color);
        return isSquareAttacked(kingPosition, color == Color.WHITE ? Color.BLACK : Color.WHITE);
    }

    public boolean isCheckmate(Color color) {
        return isCheck(color) && !hasAnyLegalMove(color);
    }

    public boolean isStalemate(Color color) {
        return !isCheck(color) && !hasAnyLegalMove(color);
    }

    public void printBoard() {
        System.out.println("  a b c d e f g h");
        for (int row = 0; row < ChessBoard.SIZE; row++) {
            System.out.print((8 - row) + " ");
            for (int col = 0; col < ChessBoard.SIZE; col++) {
                Piece piece = board.getPiece(row, col);
                System.out.print(piece == null ? "." : piece.getSymbol());
                System.out.print(" ");
            }
            System.out.println(" " + (8 - row));
        }
        System.out.println("  a b c d e f g h");
    }

    private void initializePieces() {
        board.setPiece(0, 0, new Rook(Color.BLACK));
        board.setPiece(0, 1, new Knight(Color.BLACK));
        board.setPiece(0, 2, new Bishop(Color.BLACK));
        board.setPiece(0, 3, new Queen(Color.BLACK));
        board.setPiece(0, 4, new King(Color.BLACK));
        board.setPiece(0, 5, new Bishop(Color.BLACK));
        board.setPiece(0, 6, new Knight(Color.BLACK));
        board.setPiece(0, 7, new Rook(Color.BLACK));

        for (int col = 0; col < ChessBoard.SIZE; col++) {
            board.setPiece(1, col, new Pawn(Color.BLACK));
            board.setPiece(6, col, new Pawn(Color.WHITE));
        }

        board.setPiece(7, 0, new Rook(Color.WHITE));
        board.setPiece(7, 1, new Knight(Color.WHITE));
        board.setPiece(7, 2, new Bishop(Color.WHITE));
        board.setPiece(7, 3, new Queen(Color.WHITE));
        board.setPiece(7, 4, new King(Color.WHITE));
        board.setPiece(7, 5, new Bishop(Color.WHITE));
        board.setPiece(7, 6, new Knight(Color.WHITE));
        board.setPiece(7, 7, new Rook(Color.WHITE));
    }

    private void updateStatus() {
        if (isCheckmate(Color.WHITE)) {
            status = GameStatus.BLACK_WIN;
            return;
        }

        if (isCheckmate(Color.BLACK)) {
            status = GameStatus.WHITE_WIN;
            return;
        }

        if (isStalemate(Color.WHITE) || isStalemate(Color.BLACK)) {
            status = GameStatus.DRAW;
            return;
        }

        status = currentTurn == Color.WHITE ? GameStatus.WHITE_TO_MOVE : GameStatus.BLACK_TO_MOVE;
    }

    private boolean hasAnyLegalMove(Color color) {
        for (int row = 0; row < ChessBoard.SIZE; row++) {
            for (int col = 0; col < ChessBoard.SIZE; col++) {
                Position from = new Position(row, col);
                Piece piece = board.getPiece(from);

                if (piece == null || piece.getColor() != color) {
                    continue;
                }

                for (int targetRow = 0; targetRow < ChessBoard.SIZE; targetRow++) {
                    for (int targetCol = 0; targetCol < ChessBoard.SIZE; targetCol++) {
                        Position to = new Position(targetRow, targetCol);
                        if (piece.canMove(from, to, board) && !wouldKingBeInCheck(color, from, to, piece)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean wouldKingBeInCheck(Color kingColor, Position from, Position to, Piece movingPiece) {
        ChessBoard simulatedBoard = ChessBoard.copyOf(board);
        simulatedBoard.setPiece(to.row, to.col, movingPiece);
        simulatedBoard.setPiece(from.row, from.col, null);

        Position simulatedKingPosition = kingPositions.get(kingColor);
        if (movingPiece.getType() == PieceType.KING) {
            simulatedKingPosition = to;
        }

        Color opponentColor = kingColor == Color.WHITE ? Color.BLACK : Color.WHITE;

        for (int row = 0; row < ChessBoard.SIZE; row++) {
            for (int col = 0; col < ChessBoard.SIZE; col++) {
                Piece piece = simulatedBoard.getPiece(row, col);
                if (piece != null && piece.getColor() == opponentColor) {
                    Position attackerPosition = new Position(row, col);
                    if (piece.canMove(attackerPosition, simulatedKingPosition, simulatedBoard)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isSquareAttacked(Position target, Color attackerColor) {
        for (int row = 0; row < ChessBoard.SIZE; row++) {
            for (int col = 0; col < ChessBoard.SIZE; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == attackerColor) {
                    Position attackerPosition = new Position(row, col);
                    if (piece.canMove(attackerPosition, target, board)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public enum Color {
        WHITE,
        BLACK
    }

    public enum PieceType {
        KING,
        QUEEN,
        ROOK,
        BISHOP,
        KNIGHT,
        PAWN
    }

    public enum GameStatus {
        WHITE_TO_MOVE,
        BLACK_TO_MOVE,
        WHITE_WIN,
        BLACK_WIN,
        DRAW
    }

    public static final class Position {
        private final int row;
        private final int col;

        public Position(int row, int col) {
            if (row < 0 || row >= ChessBoard.SIZE || col < 0 || col >= ChessBoard.SIZE) {
                throw new IllegalArgumentException("Position must be inside the chess board: " + row + ", " + col);
            }
            this.row = row;
            this.col = col;
        }

        public static Position of(String algebraicNotation) {
            if (algebraicNotation == null || algebraicNotation.length() != 2) {
                throw new IllegalArgumentException("Use notation like e2, d4, h7");
            }

            int col = Character.toLowerCase(algebraicNotation.charAt(0)) - 'a';
            int row = 8 - Character.getNumericValue(algebraicNotation.charAt(1));
            return new Position(row, col);
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        public String toAlgebraicNotation() {
            return "" + (char) ('a' + col) + (8 - row);
        }

        @Override
        public String toString() {
            return toAlgebraicNotation();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Position)) {
                return false;
            }
            Position other = (Position) object;
            return row == other.row && col == other.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }

    public static final class Move {
        private final Position from;
        private final Position to;
        private final Piece piece;

        public Move(Position from, Position to, Piece piece) {
            this.from = from;
            this.to = to;
            this.piece = piece;
        }

        public Position getFrom() {
            return from;
        }

        public Position getTo() {
            return to;
        }

        public Piece getPiece() {
            return piece;
        }

        public boolean isCapture() {
            return false;
        }

        @Override
        public String toString() {
            return piece.getColor() + " " + piece.getType() + " " + from + " -> " + to;
        }
    }

    public static final class ChessBoard {
        public static final int SIZE = 8;
        private final Piece[][] pieces;

        public ChessBoard() {
            this.pieces = new Piece[SIZE][SIZE];
        }

        private ChessBoard(Piece[][] pieces) {
            this.pieces = new Piece[SIZE][SIZE];
            for (int row = 0; row < SIZE; row++) {
                this.pieces[row] = Arrays.copyOf(pieces[row], SIZE);
            }
        }

        public static ChessBoard copyOf(ChessBoard board) {
            return new ChessBoard(board.pieces);
        }

        public Piece getPiece(int row, int col) {
            return pieces[row][col];
        }

        public Piece getPiece(Position position) {
            return pieces[position.getRow()][position.getCol()];
        }

        public void setPiece(int row, int col, Piece piece) {
            pieces[row][col] = piece;
        }

        public boolean isEmpty(Position position) {
            return getPiece(position) == null;
        }

        public boolean hasEnemyPiece(Position position, Color color) {
            Piece piece = getPiece(position);
            return piece != null && piece.getColor() != color;
        }

        public boolean isInside(Position position) {
            return position.getRow() >= 0
                    && position.getRow() < SIZE
                    && position.getCol() >= 0
                    && position.getCol() < SIZE;
        }

        public boolean isPathClear(Position from, Position to) {
            int rowStep = Integer.compare(to.getRow(), from.getRow());
            int colStep = Integer.compare(to.getCol(), from.getCol());

            int row = from.getRow() + rowStep;
            int col = from.getCol() + colStep;

            while (row != to.getRow() || col != to.getCol()) {
                if (getPiece(row, col) != null) {
                    return false;
                }
                row += rowStep;
                col += colStep;
            }

            return true;
        }
    }

    public abstract static class Piece {
        private final Color color;
        private final PieceType type;

        protected Piece(Color color, PieceType type) {
            this.color = color;
            this.type = type;
        }

        public Color getColor() {
            return color;
        }

        public PieceType getType() {
            return type;
        }

        public abstract boolean canMove(Position from, Position to, ChessBoard board);

        public abstract char getSymbol();
    }

    public static final class King extends Piece {
        public King(Color color) {
            super(color, PieceType.KING);
        }

        @Override
        public boolean canMove(Position from, Position to, ChessBoard board) {
            int rowDiff = Math.abs(from.getRow() - to.getRow());
            int colDiff = Math.abs(from.getCol() - to.getCol());
            return Math.max(rowDiff, colDiff) == 1;
        }

        @Override
        public char getSymbol() {
            return getColor() == Color.WHITE ? 'K' : 'k';
        }
    }

    public static final class Queen extends Piece {
        public Queen(Color color) {
            super(color, PieceType.QUEEN);
        }

        @Override
        public boolean canMove(Position from, Position to, ChessBoard board) {
            boolean isStraight = from.getRow() == to.getRow() || from.getCol() == to.getCol();
            boolean isDiagonal = Math.abs(from.getRow() - to.getRow()) == Math.abs(from.getCol() - to.getCol());
            return (isStraight || isDiagonal) && board.isPathClear(from, to);
        }

        @Override
        public char getSymbol() {
            return getColor() == Color.WHITE ? 'Q' : 'q';
        }
    }

    public static final class Rook extends Piece {
        public Rook(Color color) {
            super(color, PieceType.ROOK);
        }

        @Override
        public boolean canMove(Position from, Position to, ChessBoard board) {
            return (from.getRow() == to.getRow() || from.getCol() == to.getCol()) && board.isPathClear(from, to);
        }

        @Override
        public char getSymbol() {
            return getColor() == Color.WHITE ? 'R' : 'r';
        }
    }

    public static final class Bishop extends Piece {
        public Bishop(Color color) {
            super(color, PieceType.BISHOP);
        }

        @Override
        public boolean canMove(Position from, Position to, ChessBoard board) {
            return Math.abs(from.getRow() - to.getRow()) == Math.abs(from.getCol() - to.getCol())
                    && board.isPathClear(from, to);
        }

        @Override
        public char getSymbol() {
            return getColor() == Color.WHITE ? 'B' : 'b';
        }
    }

    public static final class Knight extends Piece {
        public Knight(Color color) {
            super(color, PieceType.KNIGHT);
        }

        @Override
        public boolean canMove(Position from, Position to, ChessBoard board) {
            int rowDiff = Math.abs(from.getRow() - to.getRow());
            int colDiff = Math.abs(from.getCol() - to.getCol());
            return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
        }

        @Override
        public char getSymbol() {
            return getColor() == Color.WHITE ? 'N' : 'n';
        }
    }

    public static final class Pawn extends Piece {
        public Pawn(Color color) {
            super(color, PieceType.PAWN);
        }

        @Override
        public boolean canMove(Position from, Position to, ChessBoard board) {
            if (from.equals(to)) {
                return false;
            }

            int direction = getColor() == Color.WHITE ? -1 : 1;
            int startingRow = getColor() == Color.WHITE ? 6 : 1;

            boolean oneStepForward = from.getRow() + direction == to.getRow()
                    && from.getCol() == to.getCol()
                    && board.isEmpty(to);

            boolean twoStepForward = from.getRow() == startingRow
                    && from.getRow() + 2 * direction == to.getRow()
                    && from.getCol() == to.getCol()
                    && board.isEmpty(to)
                    && board.isEmpty(new Position(from.getRow() + direction, from.getCol()));

            boolean diagonalCapture = from.getRow() + direction == to.getRow()
                    && Math.abs(from.getCol() - to.getCol()) == 1
                    && board.hasEnemyPiece(to, getColor());

            return oneStepForward || twoStepForward || diagonalCapture;
        }

        @Override
        public char getSymbol() {
            return getColor() == Color.WHITE ? 'P' : 'p';
        }
    }
}
