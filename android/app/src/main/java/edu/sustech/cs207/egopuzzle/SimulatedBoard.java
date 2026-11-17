package edu.sustech.cs207.egopuzzle;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Consumer;

final class SimulatedBoard implements BoardLink, Serializable {
    private static final int[] GOAL = {1, 2, 3, 4, 5, 6, 7, 8, 0};
    private static final int[] PRACTICE = {1, 2, 3, 4, 5, 6, 7, 0, 8};
    private static final int COUNTER_MAX = 15;
    private final int[] tiles = new int[9];
    private Phase phase;
    private Feedback feedback;
    private int setupIndex;
    private int validMoves;
    private int invalidMoves;
    private int demoStep;
    private Snapshot previous;

    SimulatedBoard() {
        reset();
    }

    @Override
    public void send(Action action, int tile, Consumer<Snapshot> onState) {
        switch (action) {
            case SYNC -> { }
            case RESET -> reset();
            case PRACTICE -> loadPractice();
            case DEMO_NEXT -> nextDemo();
            case PLACE -> {
                if (phase == Phase.SETUP) place(tile);
            }
            case MOVE -> {
                if (demoStep == 0 && phase == Phase.PLAYING) move(tile);
            }
            case UNDO -> {
                if (demoStep == 0) undo();
            }
            case SHOW_STATS -> {
                if (demoStep == 0 && phase == Phase.WON) phase = Phase.STATS;
            }
        }
        onState.accept(snapshot());
    }

    private void reset() {
        Arrays.fill(tiles, -1);
        phase = Phase.SETUP;
        feedback = Feedback.NONE;
        setupIndex = validMoves = invalidMoves = demoStep = 0;
        previous = null;
    }

    private void place(int tile) {
        if (tile < 0 || tile > 8 || positionOf(tile) >= 0) {
            feedback = Feedback.BAD_INPUT;
            return;
        }
        tiles[setupIndex++] = tile;
        feedback = Feedback.NONE;
        if (setupIndex == tiles.length) updatePhase();
    }

    private void move(int tile) {
        previous = snapshot();
        int tileIndex = positionOf(tile);
        int empty = positionOf(0);
        if (tile <= 0 || tileIndex < 0 || !neighbors(tileIndex, empty)) {
            invalidMoves = Math.min(invalidMoves + 1, COUNTER_MAX);
            feedback = Feedback.INVALID_MOVE;
            return;
        }
        tiles[empty] = tile;
        tiles[tileIndex] = 0;
        validMoves = Math.min(validMoves + 1, COUNTER_MAX);
        feedback = Feedback.NONE;
        updatePhase();
    }

    private void updatePhase() {
        phase = Arrays.equals(tiles, GOAL) ? Phase.WON : Phase.PLAYING;
    }

    private void undo() {
        if (previous == null) return;
        for (int i = 0; i < tiles.length; i++) tiles[i] = previous.tileAt(i);
        phase = previous.phase;
        validMoves = previous.validMoves;
        invalidMoves = previous.invalidMoves;
        feedback = Feedback.UNDONE;
        previous = null;
    }

    private void loadPractice() {
        reset();
        System.arraycopy(PRACTICE, 0, tiles, 0, tiles.length);
        setupIndex = tiles.length;
        phase = Phase.PLAYING;
    }

    private void nextDemo() {
        switch (demoStep) {
            case 0 -> loadPractice();
            case 1 -> move(1);
            case 2 -> move(8);
            case 3 -> phase = Phase.STATS;
            case 4 -> reset();
        }
        if (phase != Phase.SETUP) demoStep++;
    }

    private int positionOf(int tile) {
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i] == tile) return i;
        }
        return -1;
    }

    private Snapshot snapshot() {
        int mask = 0;
        if (phase != Phase.SETUP) {
            int empty = positionOf(0);
            for (int i = 0; i < tiles.length; i++) {
                if (neighbors(i, empty)) mask |= 1 << (8 - tiles[i]);
            }
        }
        return new Snapshot(tiles, phase, feedback, setupIndex, validMoves, invalidMoves,
                mask, previous != null, demoStep);
    }

    private static boolean neighbors(int first, int second) {
        return Math.abs(first / 3 - second / 3) + Math.abs(first % 3 - second % 3) == 1;
    }
}
