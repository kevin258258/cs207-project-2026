package edu.sustech.cs207.egopuzzle;

import static edu.sustech.cs207.egopuzzle.BoardLink.Action;
import static edu.sustech.cs207.egopuzzle.BoardLink.Feedback;
import static edu.sustech.cs207.egopuzzle.BoardLink.Phase;

final class BoardSmokeCheck {
    private final SimulatedBoard board = new SimulatedBoard();
    private BoardLink.Snapshot state;

    public static void main(String[] args) {
        BoardSmokeCheck check = new BoardSmokeCheck();
        check.practiceAndUndo();
        check.setupAndEdge();
        check.fullDemo();
        System.out.println("PASS: practice, undo, setup, adjacency, counter limit, demo and reset");
    }

    private void practiceAndUndo() {
        send(Action.PRACTICE);
        require(boardIs("123456708") && state.hints == 0b00001011, "practice layout and hints");
        send(Action.MOVE, 1);
        require(boardIs("123456708") && state.validMoves == 0 && state.invalidMoves == 1
                && state.feedback == Feedback.INVALID_MOVE, "invalid click");
        send(Action.UNDO);
        require(boardIs("123456708") && state.invalidMoves == 0, "undo invalid click");
        send(Action.MOVE, 5);
        require(boardIs("123406758") && state.validMoves == 1, "legal click");
        send(Action.UNDO);
        require(boardIs("123456708") && state.validMoves == 0 && !state.canUndo, "undo legal click");
        send(Action.MOVE, 8);
        require(boardIs("123456780") && state.validMoves == 1 && state.phase == Phase.WON, "win");
        send(Action.SHOW_STATS);
        require(state.phase == Phase.STATS && state.validMoves == 1, "post-win statistics");
    }

    private void setupAndEdge() {
        send(Action.RESET);
        send(Action.PLACE, 1);
        send(Action.PLACE, 1);
        require(state.setupIndex == 1 && state.feedback == Feedback.BAD_INPUT, "reject duplicate");
        send(Action.PLACE, 9);
        require(state.setupIndex == 1 && state.feedback == Feedback.BAD_INPUT, "reject out of range");
        for (int tile : new int[]{2, 0, 3, 4, 5, 6, 7, 8}) send(Action.PLACE, tile);
        require(boardIs("120345678") && state.movable(2) && state.movable(5) && !state.movable(3), "edge hints");
        for (int i = 0; i < 16; i++) send(Action.MOVE, 3);
        require(boardIs("120345678") && state.invalidMoves == 15, "no row wrap and 4-bit counter limit");
    }

    private void fullDemo() {
        send(Action.DEMO_NEXT);
        require(state.demoStep == 1 && state.validMoves == 0 && state.invalidMoves == 0
                && boardIs("123456708"), "demo setup");
        send(Action.DEMO_NEXT);
        require(state.demoStep == 2 && state.invalidMoves == 1 && boardIs("123456708"), "demo invalid move");
        send(Action.DEMO_NEXT);
        require(state.demoStep == 3 && state.validMoves == 1 && state.invalidMoves == 1
                && state.phase == Phase.WON && state.hints == 0b00000101 && boardIs("123456780"), "demo win");
        send(Action.DEMO_NEXT);
        require(state.demoStep == 4 && state.phase == Phase.STATS, "demo statistics");
        send(Action.DEMO_NEXT);
        require(state.demoStep == 0 && state.phase == Phase.SETUP && state.setupIndex == 0
                && state.validMoves == 0 && state.invalidMoves == 0 && state.hints == 0, "demo reset");
        for (int i = 0; i < 9; i++) require(state.tileAt(i) == -1, "empty after reset");
    }

    private void send(Action action) {
        send(action, 0);
    }

    private void send(Action action, int tile) {
        board.send(action, tile, snapshot -> state = snapshot);
    }

    private boolean boardIs(String expected) {
        for (int i = 0; i < 9; i++) {
            if (state.tileAt(i) != expected.charAt(i) - '0') return false;
        }
        return true;
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
