## 14. 本期任务 1–4：睡眠记录、缺口、社会时差与双过程实现记录 (2026-08-26)
- [x] **任务 1 数据层**：新增 `SleepRecordEntity`、`SleepSettingsEntity`、DAO、Room/InMemory Repository；日期主键 upsert，跨午夜时长/中点与目标校验已覆盖。
- [x] **Room version 2**：`SleepCycleDatabase` 保留 `ChronotypeProfileEntity`，新增显式 `MIGRATION_1_2` 创建两表和默认目标；Factory 注册 migration 并注入 Room 睡眠仓库；未使用 destructive migration。
- [x] **任务 2 纯模型**：新增 14 天估算睡眠缺口、午睡抵扣、工作日/休息日环形中点社会时差，并限制最近 14 天与每组至少 2 条。
- [x] **任务 3 双过程**：新增集中参数的指数 Process S、24 小时 Process C、综合睡眠倾向和 48 个 30 分钟点；时间型睡眠中点作为警觉低谷相位，无档案使用默认相位。
- [x] **任务 4 UI/ViewModel**：SleepScreen 新增手动日期/时间/实际主睡眠/午睡记录、目标滑块、统计卡、社会时差状态和 Canvas 曲线；保存后重新读取，异常有明确状态。
- [x] **新增测试**：`SleepRecordStorageTest` 4、`SleepAnalysisTest` 7、`SleepRecordViewModelTest` 6，聚焦测试已全绿；既有四文件仍为 `13/7/6/2`。
- **实现取舍**：为满足“实际主睡眠分钟数”可编辑，表单同时显示时间段估算和实际分钟输入；模型算法不散落在 Compose。
- **限制**：当前无 adb 设备，真实 SQLite migration 未执行，详情见 `BLOCKED.md`；其余本地纯 JVM/编译验证继续执行。

## 16. 验收缺口补齐 (2026-08-26)
- [x] 增加 `SleepRecordDraft` 和 `cancelSleepRecordEdit()`：编辑已有记录时保留快照，取消恢复原表单且不写仓库；新增 ViewModel 取消编辑断言。
- [x] 将迁移 SQL 拆为可核验常量，测试直接断言 `sleep_record` / `sleep_settings` 的表名、字段、NOT NULL、主键和 `(1, 480)` 默认目标语句。
- [x] 补充验证：62 个测试全绿，`assembleDebug` 全绿，APK 已更新并推送；v1.7.0 Release 资产已覆盖。

## 17. 补充缺口最终验证 (2026-08-26)
- [x] `SleepRecordViewModelTest` 新增取消编辑用例：编辑快照、字段修改、取消恢复原值且仓库不变。
- [x] `SleepRecordStorageTest` 新增迁移 SQL 契约：逐项核验两张表、字段类型、NOT NULL、主键和默认 `(1, 480)`。
- [x] `./gradlew testDebugUnitTest`：`BUILD SUCCESSFUL`，62 tests；既有文件仍 `13/7/6/2`。
- [x] `./gradlew testDebugUnitTest`：`BUILD SUCCESSFUL`，62 tests；既有文件仍 `13/7/6/2`。
- [x] `./gradlew assembleDebug`：`BUILD SUCCESSFUL`（38 actionable tasks）；新 APK `SleepCycle-v1.7.0.apk` 为 18,642,817 bytes。

## 18. Neat-freak 知识收尾审计 (2026-08-26)
- **代码：changed-and-verified**。v1.7.0 当前代码包含手动睡眠记录、睡眠目标、14 天缺口、社会时差、双过程曲线、编辑取消快照和 Room version 2 显式迁移；`origin/main` 为 `6ca44e3 feat(sleep-model): 增加睡眠记录与双过程分析`。
- **运行态：changed-and-verified**。GitHub Release `v1.7.0` 为非 draft、非 prerelease，URL 为 `https://github.com/michiru233/SleepCycle/releases/tag/v1.7.0`，资产为 `SleepCycle-v1.7.0.apk`；无独立服务，Android 本地应用运行态以 Release APK 为 live surface。
- **文档：changed-and-verified**。README 当前下载入口为 v1.7.0，功能定义、估算边界、Borbély 1982、Van Dongen 2003 和 Roenneberg/MCTQ 来源与代码一致。
- **规则：verified-current**。项目现役规则为根目录 `AGENTS.md`；本审计未修改规则，规则要求的测试、构建、提交、推送和 Release 流程均已执行。
- **记忆：not-applicable**。未发现项目级可写记忆系统；未写入平台生成记忆。
- **工作区：verified-current**。单一 `main` worktree，`HEAD` 与 `origin/main` 同步，工作区无未提交变更；未发现 PLAN/TODO/implementation-notes 或备份代码残留。
- **清场候选：pending**。根目录 `SleepCycle-v1.4.0.apk`、`SleepCycle-v1.5.0.apk`、`SleepCycle-v1.6.0.apk` 是历史 Release 本地副本，当前未删除，等待用户明确确认；本次不执行破坏性清理。
- **遗留：pending**。本机无可用 adb 设备，无法执行真实 SQLite Room migration；`BLOCKED.md` 已记录，当前以 SQL/表结构契约测试、Room 编译和 Factory migration 接线作为替代证据。

## 历史 v1.7.0 首次交付证据（补充验收前，2026-08-26）
- [x] `./gradlew testDebugUnitTest`：`BUILD SUCCESSFUL`，60 tests executed；无 skipped/ignored。
- [x] 既有文件计数：`SleepCalculatorTest=13`、`SleepViewModelTest=7`、`UpdateCheckerTest=6`、`AlarmIntentManagerTest=2`。
- [x] 反向验证红：临时将社会时差断言 `45` 改为 `46`，`SleepAnalysisTest` 输出 `7 tests completed, 1 failed`；恢复后同命令 `BUILD SUCCESSFUL`。
- [x] `./gradlew assembleDebug`：`BUILD SUCCESSFUL`（38 actionable tasks）；APK `SleepCycle-v1.7.0.apk` 已复制，大小 18,457,679 bytes。
- [ ] 真实 Room SQLite migration 仍受本机无 adb 设备限制，已如实记录在 `BLOCKED.md`。

## 历史 v1.6.0 收尾记录
- **当前状态**：工作区干净且 `main...origin/main` 同步；远端 HEAD 为 `12126f9 feat(chronotype): 增加时间型测评与光照提醒`；版本为 `versionCode 8 / versionName 1.6.0`。
- **目标**：在保留 ChronotypeProfile 数据的前提下，增加手动睡眠记录、睡眠目标、估算睡眠缺口、社会时差和 Borbély 双过程模型，并接入现有 SleepScreen。
- **执行顺序**：数据模型/Room version 2 与迁移 -> 纯 Kotlin 缺口/社会时差计算 -> 双过程模型 -> Repository/ViewModel/UI -> README、回归、反向验证、版本与发布。
- **最大风险**：Room 显式迁移不能丢失既有时间型档案；跨午夜与环形中点边界；Compose 小屏布局与既有入口回归；发布/推送失败需保留原始证据。
- **代码：changed-and-verified**。v1.6.0 代码包含时间型模型/跨午夜中点、Room 单用户档案、ViewModel StateFlow、晨光/数字日落建议和系统闹钟入口；当前远端 HEAD 为本期 `feat(chronotype): 增加时间型测评与光照提醒` 提交；具体 hash 以最后一次 `git log origin/main -1 --oneline` 输出为准。
- **运行态：changed-and-verified（Release live surface）**。`gh release view v1.6.0` 显示非 draft、非 prerelease，Release URL 为 `https://github.com/michiru233/SleepCycle/releases/tag/v1.6.0`，资产为 `SleepCycle-v1.6.0.apk`；无独立服务或生产部署，服务端运行态 not-applicable。
- **文档：changed-and-verified**。README 已对齐 v1.6.0 下载入口、时间型阈值、Room/光照组件、科学来源和权限限制；历史版本记录保留为历史，不再作为当前状态。
- **规则：verified-current**。当前项目唯一生效规则为根目录 `AGENTS.md`，无同级/上级冲突规则；本次未修改规则文件。
- **记忆：not-applicable**。未发现项目级可写记忆系统，未写入平台生成记忆。
- **工作区：verified-current**。单一 `main` worktree，远端同步；未发现 PLAN/TODO/implementation-notes 或备份代码残留。
- **清场候选：pending**。根目录 `SleepCycle-v1.4.0.apk`、`SleepCycle-v1.5.0.apk` 为历史 Release 本地副本，`SleepCycle-v1.6.0.apk` 为当前发布 APK 副本；均被 `.gitignore` 忽略且未删除，等待用户明确确认后再清场。
- **遗留：out-of-scope**。未实现睡眠历史、睡眠债务、社会时差或双过程算法，符合本期范围；kapt 对 Kotlin 2.0 的降级警告仅为构建警告，未阻断测试/构建。

- [x] **全量测试**：`./gradlew testDebugUnitTest` 输出 `BUILD SUCCESSFUL`；总 `@Test` 数 45（基线 35 + 新增 10），既有 `SleepCalculatorTest/SleepViewModelTest/UpdateCheckerTest/AlarmIntentManagerTest = 13/7/6/2`，无 skipped/ignored 测试。
- [x] **反向验证**：临时将 `23:30→07:30` 中点断言改为 211 分钟，测试输出 `ChronotypeModelTest > midpointAcrossMidnightIsCorrect FAILED`、`5 tests completed, 1 failed`；恢复 210 分钟后同命令 `BUILD SUCCESSFUL`，错误未保留。
- [x] **构建**：`./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`（38 actionable tasks）；仅有 native library strip 警告，APK 正常生成。
- [x] **APK**：`SleepCycle-v1.6.0.apk` 与 debug APK 均为 18,392,139 bytes。
- [x] **Git 提交与推送**：`git commit -m "feat(chronotype): 增加时间型测评与光照提醒"` 生成 `132c717`；`git push origin main` 输出 `46cccf5..132c717 main -> main`。
- [x] **GitHub Release**：`gh release create v1.6.0 SleepCycle-v1.6.0.apk ...` 成功；地址 `https://github.com/michiru233/SleepCycle/releases/tag/v1.6.0`。
- [x] **最终远端核验**：发布核验记录提交后再次以任务指定的 feat 提交信息提交，确保 `git log origin/main -1 --oneline` 满足本期交付检查；`BLOCKED.md` 为「无」。

## 0. 开工回执 (检查更新功能开发阶段)
- **理解的目标**：在 SleepCycle 顶栏右上角增加「检查更新」操作按钮，通过原生 `HttpURLConnection` 请求 GitHub Releases API 检索最新版本，比对当前应用版本（语义化版本大小判断）。当有新版本时，弹窗优雅展示新版本号、发布时间、更新日志并支持点击跳转浏览器/外部下载 APK；当前已是最新或检查失败时提供友好 Toast/反馈，全程具备完善异常处理与单测。
- **执行顺序**：任务 0 (基线校验与开工回执) -> 任务 1 (声明网络权限、编写 UpdateChecker 及其模型与单测) -> 任务 2 (集成 ViewModel 状态管理、SleepScreen 顶栏按钮与 Material 3 更新弹窗、补充测试) -> 任务 3 (全量回归构建与最终交付)。
- **最大风险**：1) 语义化版本解析容错（如携带 `v` 前缀、测试版后缀等非标准输入时的异常规避）；2) 网络超时或 GitHub API 速率限制/格式异常的处理不当引发崩溃（通过严格 try-catch、自定义接口解耦与注入确保健壮性）；3) Compose 弹窗生命周期管理。

## 1. 任务执行与完成记录
- [x] **任务 0：基线核验与文档初始化**
  - `./gradlew testDebugUnitTest` 22 个 task 全部通过。
  - 确认基线版本为 1.1.1，compileSdk 为 35，minSdk 为 26。
- [x] **任务 1：增加网络权限与更新检测模块 (UpdateChecker)**
  - 在 `AndroidManifest.xml` 中声明 `android.permission.INTERNET`。
  - 创建 `com.example.sleepcycle.update.UpdateChecker` 及其数据模型与版本对比工具。
  - 编写 `UpdateCheckerTest.kt`，覆盖语义化版本比对、JSON 解析与异常处理（6 个测试全部通过）。
- [x] **任务 2：集成 ViewModel 与 UI 交互 (SleepScreen & UpdateDialog)**
  - 在 `SleepViewModel` 中增加 `UpdateUiState`（`Idle`, `Checking`, `HasUpdate`, `UpToDate`, `Error`）与 `checkForUpdates()` 异步流转逻辑。
  - 在 `SleepScreen` 顶栏增加「检查更新」IconButton 与加载旋转菊花状态指示。
  - 实现优雅 Material 3 风格更新提示弹窗 `UpdateDialog`（支持查看更新日志、跳转浏览器/下载 APK 与暂不更新）。
  - 在 `SleepViewModelTest.kt` 中增加针对更新逻辑的单元测试（HasUpdate、UpToDate、Error 等流转用例）。
- [x] **任务 3：全量验证与构建交付**
  - `./gradlew testDebugUnitTest` 24 个单元测试 100% 绿灯（原有 15 个基础算法与配置测试 + 9 个更新模块测试全过，测试数净增 9 个）。
  - `./gradlew assembleDebug` 零报错成功构建并生成 `app-debug.apk`。

## 2. 开工回执 (起床窗口副标签功能)
- **理解的目标**：每张睡眠周期卡片在目标时间下方显示「唤醒窗口 HH:MM–HH:MM」副标签，窗口 = targetTime ± 15 分钟（宽 30 分钟）。只加展示，不动闹钟、计算核心与推荐逻辑。
- **执行顺序**：1) SleepModels.kt 加 `wakeWindowText` 计算属性 -> 2) ModernRecommendationCard.kt 新增窗口副标签行 -> 3) SleepCalculatorTest.kt 新增单测 -> 4) 跑 testDebugUnitTest 与 assembleDebug。
- **最大风险**：用例②文案有笔误（22:00+20min+450min=05:50，非 06:50）。真正 target 06:50 来自 23:00+latency20。为保证断言真实正确，用 23:00+latency20 触发 06:50→「06:35–07:05」，吻合目标要求的窗口输出。
- **约束**：窗口仅展示，不落库、不改周期时长、不影响 isRecommended/targetTime/formattedTargetTime。

## 3. 起床窗口副标签功能 - 完成记录
- [x] **模型**：`SleepRecommendation` 增加 `wakeWindowText` 计算属性（targetTime ±15min，HH:MM–HH:MM，en dash）。
- [x] **UI**：`ModernRecommendationCard` 在 quality.description 下方新增「唤醒窗口 HH:MM–HH:MM」副标签行（AccessTime 图标 + bodySmall/onSurfaceVariant），未动闹钟按钮与布局结构。
- [x] **测试**：`SleepCalculatorTest` 新增 4 个测试（标准 06:29–06:59、latency20 06:35–07:05、跨天 23:51–00:21、影响面回归），总数 9→13，全项目 28 个测试 100% 通过，0 failure/error/skipped。
- [x] **验收**：`./gradlew testDebugUnitTest --rerun-tasks` BUILD SUCCESSFUL；`./gradlew assembleDebug` BUILD SUCCESSFUL。
- [x] **独立核验**：用 Python 复现 LocalTime 取模语义，确认 06:44→06:29–06:59、06:50→06:35–07:05、00:06→23:51–00:21 全部正确。
- **说明**：目标用例②文案有笔误（22:00+20min+450min=05:50），且要求 06:50 → 06:35–07:05；已用 23:00+latency20 产出 06:50 对齐要求，断言真实（非硬编码假值）。

## 5. 本期任务 0：小睡与睡眠惰性功能开工回执 (2026-08-25)
- **基线核验**：`./gradlew testDebugUnitTest` 输出 `BUILD SUCCESSFUL`（22 actionable tasks）；`./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`（35 actionable tasks）。与任务文档给出的基线一致，继续实施。
- **理解的目标**：新增 10 分钟、20 分钟、coffee nap 引导和 90 分钟单周期小睡入口，复用现有系统闹钟意图；设定后显示睡眠惰性缓冲提示，并补充科学说明与个体差异文案。
- **执行顺序**：模型与单测 -> ViewModel 小睡状态/闹钟参数与单测 -> SleepScreen 入口、coffee nap 弹窗和醒后提示 -> 科普卡片 -> 全量回归、反向红绿验证、版本/APK/GitHub Release 交付。
- **最大风险**：在不改变既有 1..6 周期计算和 AlarmIntentManager 行为的前提下接入小睡计时；Compose 状态与弹窗生命周期；既有四个测试文件用例数必须保持 13/7/6/2。

## 4. 自动化 Git & GitHub 交付 (v1.4.0)
- [x] **版本迭代**：`app/build.gradle.kts` versionCode 5->6，versionName 1.3.0->1.4.0。
- [x] **回归验证**：版本更新后重跑 `./gradlew testDebugUnitTest --rerun-tasks`（28 tests 全绿）与 `./gradlew assembleDebug`（BUILD SUCCESSFUL）。
- [x] **APK 重命名**：`SleepCycle-v1.4.0.apk`（18178409 bytes）。
- [x] **Git 提交与推送**：commit `38bea9e` `feat(card): add wake window label to recommendation cards` 已推送 origin/main（f0f9b9c..38bea9e）。改动仅 4 个源码/版本文件；PROGRESS.md（.gitignore）与 *.apk（.gitignore）不随提交。
- [x] **Release 资产**：`gh release create v1.4.0` 成功，远端 tag v1.4.0 指向 38bea9e，APK 资产已上传。
  - https://github.com/michiru233/SleepCycle/releases/tag/v1.4.0

## 6. 本期任务 1–3：实现与验证记录 (2026-08-25)
- [x] **任务 1 模型层**：新增 `NapType`（10 分钟、20 分钟、Coffee nap、90 分钟/1 周期）、时长、描述、醒后提示和 `NapAlarmRequest`；新增 `NapTypeTest` 3 个用例。
- [x] **任务 2 小睡 UI/状态**：`SleepViewModel` 新增小睡选择、coffee nap 确认前置状态、闹钟请求和事件流；`SleepScreen` 增加四档入口及 App 内 coffee nap 弹窗，确认后复用 `AlarmIntentManager.setAlarm`；新增 `NapViewModelTest` 3 个用例。
- [x] **任务 3 睡眠惰性**：周期闹钟与小睡闹钟设定后均显示「醒后 15–60 分钟认知可能未完全恢复」缓冲卡片，建议晨光+轻度活动；科普区解释睡够 8 小时仍可能因深睡唤醒而昏沉；新增 `SleepInertiaGuidanceTest` 1 个用例。
- **任务 2 构建验收**：`./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`（35 actionable tasks）。
- **任务 2 反向验证（红）**：临时把 `NapType.TEN_MINUTES.durationMinutes` 断言改为 11；`./gradlew testDebugUnitTest --tests com.example.sleepcycle.NapTypeTest` 输出 `Exit code 1`、`NapTypeTest > presetsExposeTheFourSupportedNapChoices FAILED`、`3 tests completed, 1 failed`。
- **任务 2 反向验证（绿）**：恢复断言为 10 后同一命令输出 `BUILD SUCCESSFUL`（3 tests completed）。临时错误未保留。
- **全量阶段验收**：`./gradlew testDebugUnitTest` 输出 `BUILD SUCCESSFUL`；总 `@Test` 数 35。四个既有文件实际计数：`SleepCalculatorTest=13`、`SleepViewModelTest=7`、`UpdateCheckerTest=6`、`AlarmIntentManagerTest=2`。
- **文档与版本**：README 增加小睡科学、Coffee nap、NASA 机组研究和睡眠惰性说明；`versionCode 6->7`、`versionName 1.4.0->1.5.0`。

## 7. 本期任务 4：最终回归与发布交付记录 (2026-08-25)
- [x] **最终测试**：`./gradlew testDebugUnitTest --rerun-tasks` 输出 `BUILD SUCCESSFUL`（22 actionable tasks）。总 `@Test` 数 35；既有文件仍为 `13/7/6/2`。
- [x] **最终构建**：`./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`（35 actionable tasks）。
- [x] **APK 交付文件**：执行 `cp app/build/outputs/apk/debug/app-debug.apk SleepCycle-v1.5.0.apk`；源 APK 与交付 APK 均核对为 18194793 bytes。
- [x] **Git 与 Release**：`git commit -m "feat(nap): 新增高效小睡档位与睡眠惰性提示"` 成功生成 `36c6e04`；`git push origin main` 输出 `3af8baa..36c6e04 main -> main`。
- [x] **GitHub Release**：`gh release create v1.5.0 SleepCycle-v1.5.0.apk --title "v1.5.0 - 高效小睡与睡眠惰性提示" ...` 成功，返回 `https://github.com/michiru233/SleepCycle/releases/tag/v1.5.0`。
- [x] **最终硬指标核验**：`gh release view v1.5.0` 显示标题、tag、URL 和附件 `asset: SleepCycle-v1.5.0.apk`；`git log origin/main -1 --oneline` 输出 `36c6e04 feat(nap): 新增高效小睡档位与睡眠惰性提示`；`git status --short --branch` 输出 `## main...origin/main`。

## 8. Neat-freak 历史知识记录（v1.5.0，2026-08-25）
- **历史代码记录**：v1.5.0 当时的 `c218361 feat(nap): 新增高效小睡档位与睡眠惰性提示` 包含小睡模型、ViewModel 状态、App 内 coffee nap 弹窗、系统闹钟复用和睡眠惰性卡片。
- **历史运行态记录**：GitHub Release `v1.5.0` 当时可访问，附件 `SleepCycle-v1.5.0.apk`，大小 18194793 bytes；未配置独立服务或生产部署。
- **历史文档记录**：README 当时已同步 v1.5.0 下载入口、小睡档位、Coffee nap、Brooks & Lack（2006）、NASA 约 26 分钟/约 34% 警觉度提升及个体差异提示。
- **历史规则/记忆记录**：当时项目唯一生效规则为根目录 `AGENTS.md`，未发现项目级可写记忆系统；本记录不代表当前 v1.6.0 运行态。
- **历史发布提交差异**：Release tag `v1.5.0` 当时指向 `36c6e04`，`origin/main` 当时指向 `c218361`；后者仅补入当时发布核验记录，未移动已发布 tag。
- **历史清理候选**：根目录 `SleepCycle-v1.4.0.apk`、`SleepCycle-v1.5.0.apk` 是历史 Release 本地副本；当前清场候选以本文件顶部 v1.6.0 审计记录为准，未删除。

## 5. 本期任务 0：小睡与睡眠惰性功能开工回执 (2026-08-25)
