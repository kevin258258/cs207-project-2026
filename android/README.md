# EGO1 数字华容道 · 安卓端

当前版本使用模拟板。手机发出操作，收到模拟板确认的棋盘状态后更新界面。App 支持手动摆盘、提示、有效和无效步数、撤回、通关统计、复位及课程的五步演示。

## 安装和演练

从 [安卓测试 ZIP](deliverables/EGO1-Puzzle-Android-0.1.zip) 解压并安装调试 APK，或用 Android Studio 打开本目录并运行 `app`。构建需要 JDK 17、Android SDK 36 和 Build Tools 36.1.0。

```powershell
.\gradlew.bat assembleDebug
```

1. 点「练习关」，棋盘为 `123456708`，`5`、`7` 和 `8` 显示为浅绿色。
2. 点 `1`，棋盘原样，有效步数 `0`、无效步数 `1`。
3. 点 `8`，棋盘变为 `123456780`，有效步数 `1`、无效步数 `1`，显示通关。
4. 点「查看统计」显示结算，点「复位」进入手动摆盘。每点一次编号就确认一格，`0` 表示空格。

「开始演示」也能逐步重放摆盘、无效移动、有效移动并通关、显示统计、复位。练习模式下「撤回」会恢复最近一次移动前的棋盘和两个计数，包括无效移动。模拟板把每种步数限制在 `0–15`，到 `15` 后保持，以对应板上每种步数各 4 位 LED。这个溢出行为需要跟板端约定。

## 板端联调

仓库的 FPGA 已有 [UART 协议](../docs/DESIGN.zh-CN.md#uart-协议)。当前 App 运行模拟板。接入实板时，需按该协议读取状态帧，再和实板确认计数、展示和复位行为。

- `BoardLink` 约定 App 提交哪些操作，以及状态回传需要包含哪些内容。`SimulatedBoard` 提供本地反馈，`MainActivity` 根据返回的状态刷新界面。
- 拿到通信模块和协议后，App 端接收 FPGA 确认的棋盘、提示位、有效和无效计数、游戏阶段以及操作结果。连接时请求完整状态，板载键发生操作后也需要把新状态发到 App。
- 实机验收时先用手机点 `1` 和 `8`，核对手机状态、数码管和 LED。再从开发板操作一次，确认手机也能收到更新。

## 验证规则

用 JDK 17 在项目根目录运行下面的命令即可，无需启动模拟器。

```powershell
$classes = Join-Path $env:TEMP 'cs207-board-check'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$model = 'app/src/main/java/edu/sustech/cs207/egopuzzle'
javac -encoding UTF-8 -d $classes "$model/BoardLink.java" "$model/SimulatedBoard.java" tools/BoardSmokeCheck.java
java -cp $classes edu.sustech.cs207.egopuzzle.BoardSmokeCheck
```
