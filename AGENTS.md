# 项目工作规范与工作流

## 自动化 Git & GitHub 交付规范
每次完成功能开发（Feature）、优化（Refactor）或缺陷修复（Bugfix）后：
1. **代码质量验证**：
   - 运行 `./gradlew testDebugUnitTest` 确保单元测试 100% 通过。
   - 确保 `./gradlew assembleDebug` 正常编译通过。
2. **Git 提交与推送**：
   - 检查工作区变动，编写符合规范的 Commit Message（如 `feat: ...` / `fix: ...` / `refactor: ...`）。
   - 自动提交变更并推送到远端仓库 `origin/main`：
     ```bash
     git add .
     git commit -m "<type>: <description>"
     git push origin main
     ```
3. **版本迭代与 Release（重大/里程碑版本更新时）**：
   - 适时更新 `app/build.gradle.kts` 中的 `versionCode` 与 `versionName`。
   - 打包 APK 并通过 `gh release create` 更新 Release 资产。
