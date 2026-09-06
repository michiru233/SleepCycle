# BLOCKED.md

- ~~本机 `adb devices` 无可用设备~~ **已解决（2026-09-06）**：adb 位于 `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`，v1.9.0–v1.9.3 均已在模拟器（API 31+，kikoeru_test AVD）完成安装、启动、深浅色截图与视觉验收，无崩溃。
- 真实 Room SQLite migration 仍未在真机升级路径（旧版本原地覆盖安装、带旧数据启动）上验证；当前证据仍为显式 `Migration(1, 2)` SQL/版本契约测试与 Room 编译接线。v1.9.0–v1.9.3 均未改 schema，风险未变。
