package com.chess.engine.classic.player;

import com.chess.engine.classic.Alliance;
import com.chess.engine.classic.board.Board;
import com.chess.engine.classic.board.Move;
import com.chess.engine.classic.board.Move.MoveStatus;
import com.chess.engine.classic.board.MoveTransition;
import com.chess.engine.classic.pieces.King;
import com.chess.engine.classic.pieces.Piece;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.stream.Collectors;

public abstract class Player {

    protected final Board board;
    protected final King playerKing;
    protected final Collection<Move> legalMoves;
    protected final boolean isInCheck;

    Player(final Board board,
           final Collection<Move> playerLegals,
           final Collection<Move> opponentLegals) {
        this.board = board;
        this.playerKing = establishKing();
        this.isInCheck = !Player.calculateAttacksOnTile(this.playerKing.getPiecePosition(), opponentLegals).isEmpty();
        playerLegals.addAll(calculateKingCastles(playerLegals, opponentLegals));
        this.legalMoves = ImmutableList.copyOf(playerLegals);
    }

    // return if the king is in check or not (it have ways to escape )
    public boolean isInCheck() {
        return this.isInCheck;
    }

    // return if the king is in checkMate or not (it have not ways to escape )
    public boolean isInCheckMate() {
        return this.isInCheck && !hasEscapeMoves();
    }

    // return true if there are no ways to king to move
    public boolean isInStaleMate() {
        return !this.isInCheck && !hasEscapeMoves();
    }

    // return true if king is castled
    public boolean isCastled() {
        return this.playerKing.isCastled();
    }


    public boolean isKingSideCastleCapable() {
        return this.playerKing.isKingSideCastleCapable();
    }

    public boolean isQueenSideCastleCapable() {
        return this.playerKing.isQueenSideCastleCapable();
    }

    public King getPlayerKing() {
        return this.playerKing;
    }

    // returns all kings for this player
    private King establishKing() {
        return (King) getActivePieces().stream().filter(piece ->
                piece.getPieceType().isKing()).findAny().orElseThrow(RuntimeException::new);
    }

    // return true if piece can make move to escape
    private boolean hasEscapeMoves() {
        return this.legalMoves.stream().anyMatch(move -> makeMove(move).getMoveStatus().isDone());
    }

    // returns legal move
    public Collection<Move> getLegalMoves() {
        return this.legalMoves;
    }

    // get true if there are attack on this tile
    static Collection<Move> calculateAttacksOnTile(final int tile,
                                                   final Collection<Move> moves) {
        return moves.stream().filter(move -> move.getDestinationCoordinate() == tile)
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    // it make anew move (illegal OR DONE)
    public MoveTransition makeMove(final Move move) {
        if (!this.legalMoves.contains(move)) {
            return new MoveTransition(this.board, this.board, move, MoveStatus.ILLEGAL_MOVE);
        }
        final Board transitionedBoard = move.execute();
        final Collection<Move> kingAttacks = Player.calculateAttacksOnTile(
                transitionedBoard.currentPlayer().getOpponent().getPlayerKing().getPiecePosition(),
                transitionedBoard.currentPlayer().getLegalMoves());
        if (!kingAttacks.isEmpty()) {
            return new MoveTransition(this.board, this.board, move, MoveStatus.LEAVES_PLAYER_IN_CHECK);
        }
        return new MoveTransition(this.board, transitionedBoard, move, MoveStatus.DONE);
    }

    // it make undo for move
    public MoveTransition unMakeMove(final Move move) {
        return new MoveTransition(this.board, move.undo(), move, MoveStatus.DONE);
    }

    public abstract Collection<Piece> getActivePieces();
    public abstract Alliance getAlliance();
    public abstract Player getOpponent();
    protected abstract Collection<Move> calculateKingCastles(Collection<Move> playerLegals,
                                                             Collection<Move> opponentLegals);

}
