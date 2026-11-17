package edu.sustech.cs207.egopuzzle;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static edu.sustech.cs207.egopuzzle.BoardLink.Action;
import static edu.sustech.cs207.egopuzzle.BoardLink.Phase;

public final class MainActivity extends Activity {
    private static final int PAPER = 0xFFF7F4EB;
    private static final int INK = 0xFF182B26;
    private static final int MUTED = 0xFF4C655D;
    private static final int PANEL = 0xFFE6EDE6;
    private static final int TILE = 0xFFFFFBF0;
    private static final int MINT = 0xFFB7E9CC;
    private static final int ACCENT = 0xFF175A43;
    private static final int ERROR = 0xFF992D1C;

    private final Button[] cells = new Button[9];
    private BoardLink board;
    private TextView validValue;
    private TextView invalidValue;
    private TextView status;
    private TextView tip;
    private LinearLayout keypad;
    private Button undo;
    private Button stats;
    private Button demo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        board = restoreBoard(savedInstanceState);
        createScreen();
        send(Action.SYNC, 0);
    }

    @SuppressWarnings("deprecation")
    private BoardLink restoreBoard(Bundle state) {
        if (state != null && state.getSerializable("simulated_board") instanceof SimulatedBoard saved) {
            return saved;
        }
        return new SimulatedBoard();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (board instanceof SimulatedBoard simulated) outState.putSerializable("simulated_board", simulated);
    }

    private void createScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAPER);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            applyInsets(view, insets);
            return insets;
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(18));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        TextView source = label("EGO1  ·  模拟板", 13, ACCENT, true);
        content.addView(source);
        TextView title = label("数字华容道", 33, INK, true);
        if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        add(content, title, 2);

        LinearLayout counters = row();
        validValue = addCounter(counters, "有效步数", 0);
        invalidValue = addCounter(counters, "无效步数", 8);
        add(content, counters, 12);
        status = label("", 15, MUTED, true);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(content, status, 12);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        grid.setBackground(shape(INK, 20));
        grid.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(content, grid, 8);
        int cellHeight = Math.max(62, Math.min(102, (getResources().getConfiguration().screenWidthDp - 74) / 3));
        for (int r = 0; r < 3; r++) {
            LinearLayout line = row();
            add(grid, line, r == 0 ? 0 : 6);
            for (int c = 0; c < 3; c++) {
                int index = r * 3 + c;
                Button cell = button("", 31, TILE, INK, null);
                cells[index] = cell;
                addWeighted(line, cell, cellHeight, c == 0 ? 0 : 6);
            }
        }

        tip = label("浅绿色棋子可以移动", 13, MUTED, false);
        add(content, tip, 7);

        keypad = new LinearLayout(this);
        keypad.setOrientation(LinearLayout.VERTICAL);
        add(content, keypad, 8);
        for (int r = 0; r < 2; r++) {
            LinearLayout line = row();
            add(keypad, line, r == 0 ? 0 : 5);
            for (int i = r * 5; i < Math.min(r * 5 + 5, 9); i++) {
                int tile = i;
                Button key = button(String.valueOf(tile), 18, PANEL, INK, v -> send(Action.PLACE, tile));
                if (tile == 0) key.setContentDescription("0，空格");
                addWeighted(line, key, 48, tile % 5 == 0 ? 0 : 5);
            }
            if (r == 1) addWeighted(line, new View(this), 48, 5);
        }

        LinearLayout controls = row();
        addWeighted(controls, button("练习关", 15, ACCENT, 0xFFFFFFFF, v -> send(Action.PRACTICE, 0)), 48, 0);
        undo = button("撤回", 15, PANEL, INK, v -> send(Action.UNDO, 0));
        addWeighted(controls, undo, 48, 6);
        addWeighted(controls, button("复位", 15, PANEL, INK, v -> send(Action.RESET, 0)), 48, 6);
        add(content, controls, 10);

        LinearLayout footer = row();
        demo = button("开始演示", 15, PANEL, ACCENT, v -> send(Action.DEMO_NEXT, 0));
        stats = button("查看统计", 15, ACCENT, 0xFFFFFFFF, v -> send(Action.SHOW_STATS, 0));
        addWeighted(footer, demo, 48, 0);
        addWeighted(footer, stats, 48, 6);
        add(content, footer, 6);
        setContentView(scroll);
    }

    @SuppressWarnings("deprecation")
    private void applyInsets(View view, WindowInsets insets) {
        view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
    }

    private void send(Action action, int tile) {
        board.send(action, tile, confirmed -> runOnUiThread(() -> render(confirmed)));
    }

    private void render(BoardLink.Snapshot state) {
        boolean setup = state.phase == Phase.SETUP;
        boolean interactive = state.phase == Phase.PLAYING && state.demoStep == 0;
        boolean badInput = state.feedback == BoardLink.Feedback.BAD_INPUT;
        boolean invalidMove = state.feedback == BoardLink.Feedback.INVALID_MOVE;
        for (int i = 0; i < cells.length; i++) {
            Button cell = cells[i];
            int tile = state.tileAt(i);
            boolean rejected = badInput && i == state.setupIndex;
            boolean movable = state.movable(tile);
            cell.setText(rejected ? "E" : tile > 0 ? String.valueOf(tile) : "");
            cell.setTextColor(rejected ? ERROR : INK);
            cell.setBackground(buttonBackground(rejected ? 0xFFFFDDD3 : movable ? MINT : tile > 0 ? TILE : 0xFF30443C));
            cell.setContentDescription(rejected ? "第 " + (i + 1) + " 格，输入错误"
                    : tile < 0 ? "第 " + (i + 1) + " 格，未设置"
                    : tile == 0 ? "空格" : "棋子 " + tile + (movable ? "，可移动" : ""));
            cell.setOnClickListener(interactive && tile > 0 ? view -> send(Action.MOVE, tile) : null);
            cell.setClickable(interactive && tile > 0);
        }
        validValue.setText(String.valueOf(state.validMoves));
        validValue.setContentDescription("有效步数 " + state.validMoves);
        invalidValue.setText(String.valueOf(state.invalidMoves));
        invalidValue.setContentDescription("无效步数 " + state.invalidMoves);
        keypad.setVisibility(setup ? View.VISIBLE : View.GONE);
        tip.setVisibility(setup ? View.GONE : View.VISIBLE);
        undo.setEnabled(state.canUndo && state.demoStep == 0);
        undo.setAlpha(undo.isEnabled() ? 1f : 0.55f);
        stats.setVisibility(state.phase == Phase.WON && state.demoStep == 0 ? View.VISIBLE : View.GONE);
        demo.setText(state.demoStep == 0 ? "开始演示" : "演示下一步 · " + state.demoStep + "/5");
        status.setTextColor(badInput || invalidMove ? ERROR : ACCENT);
        status.setText(switch (state.phase) {
            case SETUP -> badInput ? "编号重复，请重新输入第 " + (state.setupIndex + 1) + " 格"
                    : "设置第 " + (state.setupIndex + 1) + " / 9 格 · 0 为空格";
            case PLAYING -> invalidMove ? "无法移动 · 棋子必须紧邻空格"
                    : state.feedback == BoardLink.Feedback.UNDONE ? "已撤回上一步" : "点击与空格相邻的棋子";
            case WON -> "已通关";
            case STATS -> "通关统计 · 有效 " + state.validMoves + " 步，无效 " + state.invalidMoves + " 步";
        });
    }

    private TextView addCounter(LinearLayout row, String name, int gap) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(7), dp(12), dp(7));
        panel.setBackground(shape(PANEL, 13));
        panel.addView(label(name, 13, MUTED, false));
        TextView value = label("0", 23, INK, true);
        panel.addView(value);
        addWeighted(row, panel, -2, gap);
        return value;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    private Button button(String text, int size, int background, int foreground, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(size);
        button.setTextColor(foreground);
        button.setAllCaps(false);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(2), 0, dp(2), 0);
        button.setGravity(Gravity.CENTER);
        button.setStateListAnimator(null);
        button.setBackground(buttonBackground(background));
        if (click != null) button.setOnClickListener(click);
        return button;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private RippleDrawable buttonBackground(int color) {
        return new RippleDrawable(ColorStateList.valueOf(0x33175A43), shape(color, 12), null);
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private void add(LinearLayout parent, View child, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(gap);
        parent.addView(child, params);
    }

    private void addWeighted(LinearLayout parent, View child, int height, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height < 0 ? height : dp(height), 1f);
        params.setMarginStart(dp(gap));
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
