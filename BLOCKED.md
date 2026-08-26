# BLOCKED.md

- 本机 `adb devices` 无可用设备，未能执行真实 Android/SQLite Room migration 测试；当前已用显式 `Migration(1, 2)` SQL/版本契约测试、Room 编译和 Factory `addMigrations` 接线核验替代。需要设备或 CI emulator 才能进一步验证旧 `chronotype_profile` 行在真实升级中的保留。
- 本次人工 UI 验证同样受环境限制：执行 `adb devices` 返回 `zsh:3: command not found: adb`，未能启动/安装/操作 Android 模拟器；代码级导航契约、编译和单元测试已完成。
