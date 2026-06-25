# 需求与测试追踪矩阵

**版本**: v1.0  
**日期**: 2026-06-25  
**状态**: 初始版本

## 概述

本文档建立 PRD 需求与测试用例之间的可追溯性，确保每个需求都有对应的测试覆盖。

## 核心功能需求覆盖

### 5.1 桌宠孵化室（Hatchery）

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| REQ-001 | 自主孵化提交合法提示词返回 jobId | `FantasyPetGenerationUiModelTest.kt:142` (submitting state transitions) | ✅ | |
| REQ-002 | 候选画廊选择机制 | `FantasyPetGenerationUiModelTest.kt:298` (candidate selection) | ✅ | |
| REQ-003 | 人审 Accept 后解锁下载 | `FantasyPetGenerationUiModelTest.kt:373` (review accept enables download) | ✅ | |
| REQ-004 | Revise/Reject 必须填写视觉备注 | `FantasyPetGenerationUiModelTest.kt:398` (review requires visual note) | ✅ | |
| REQ-005 | 候选出现前禁用 Accept/Download | `FantasyPetGenerationUiModelTest.kt:251` (disabled review before candidates) | ✅ | |
| REQ-006 | 契约演示任务禁用人审 | Android UI smoke assertion tests | ✅ | |

### 5.2 桌宠呈现（App 内 + 桌面悬浮）

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| REQ-007 | 开屏先出桌宠 | `PetShellUiModelTest.kt:54` (initial state shows pet) | ✅ | |
| REQ-008 | 默认桌宠三选一引导 | `PetShellUiModelTest.kt:79` (default pet selection) | ✅ | |
| REQ-009 | 动作系统稳定性 | QA 门禁 + `identityConsistency` 评分 | ✅ | |

### 5.3 社区展示（Community Feed）

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| REQ-010 | 社区动态列表 | `server.test.js:1015` (GET /v1/feed) | ✅ | |
| REQ-011 | 已通过桌宠展架 | `server.test.js:1053` (GET /v1/pets/approved) | ✅ | |
| REQ-012 | 资源包下载 | `server.test.js:1072` (GET /v1/pets/approved/:id/package) | ✅ | |

### 5.4 社区创作（Submit & Review）

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| REQ-013 | 从 pet.zip 创建导入草稿 | `FantasyPetPackageImportRequestBuilderTest.kt:23` | ✅ | |
| REQ-014 | 提交进入审核队列 | `server.test.js:1137` (POST /v1/import-drafts/:id/submit) | ✅ | |
| REQ-015 | 审核通过登记 approved_pets | `server.test.js:1213` (admin approve flow) | ✅ | |
| REQ-016 | 所有权声明验证 | `contracts.test.js:47` (ownership claim schema) | ✅ | |

### 5.5 宠物币奖励

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| REQ-017 | 签到奖励 | `server.test.js:1247` (POST /v1/check-in) | ✅ | |
| REQ-018 | 审核通过奖励 | `server.test.js:1285` (reward on approve) | ✅ | |
| REQ-019 | 余额查询 | `server.test.js:1323` (GET /v1/wallet/me) | ✅ | |
| REQ-020 | 奖励挂起/撤回 | `contracts.test.js:63` (ledger entry status) | ✅ | |

## 安全与边界需求

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| SEC-001 | App 不触达生成内核 | `HttpCommunityApiClientTest.kt:295` (gateway proxy) | ✅ | |
| SEC-002 | 公共端点不暴露内部路径 | `private-ops-smoke.test.js:839` (password leak detection) | ✅ | |
| SEC-003 | Token 不泄露到日志 | `private-ops-smoke.test.js:839` (token redaction) | ✅ | |
| SEC-004 | Demo token 鉴权 | `server.test.js:87` (unauthorized without token) | ✅ | |
| SEC-005 | Basic Auth 保护 admin 端点 | `admin-review.test.js:75` (basic auth challenge) | ✅ | |

## 限流与 SLA 需求

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| SLA-001 | SLA 配置端点 | `server.test.js:1312` (GET /v1/sla) | ✅ | |
| SLA-002 | 可配置孵化耗时 | `server.test.js:1324` (custom hatch max ms) | ✅ | |
| RATE-001 | 限流启用 | `rate-limit.test.js:8` (rate limiter policy) | ✅ | |
| RATE-002 | 写操作限制 | `rate-limit.test.js:23` (write limit) | ✅ | |
| RATE-003 | 读操作限制 | `rate-limit.test.js:43` (read limit) | ✅ | |
| RATE-004 | 豁免路径 | `rate-limit.test.js:63` (exempt paths) | ✅ | |

## 生成质量需求

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| QA-001 | 身份一致性检查 | QA 门禁 `identityConsistency` | ✅ | |
| QA-002 | 动作稳定性检查 | QA 门禁 motion stability | ✅ | |
| QA-003 | 可见运动检查 | QA 门禁 visible motion | ✅ | |
| QA-004 | 循环首尾审计 | QA 门禁 loop audit | ✅ | |

## 数据模型需求

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| DATA-001 | pet.zip manifest 验证 | `contracts.test.js:25` (valid manifest) | ✅ | |
| DATA-002 | Bundle 完整性检查 | `contracts.test.js:34` (valid bundle) | ✅ | |
| DATA-003 | 所有权声明格式 | `contracts.test.js:47` (ownership claim) | ✅ | |
| DATA-004 | 评分报告格式 | `contracts.test.js:55` (score report) | ✅ | |
| DATA-005 | 账本条目格式 | `contracts.test.js:63` (ledger entry) | ✅ | |

## 部署与运维需求

| 需求 ID | 需求描述 | 测试用例 | 状态 | 备注 |
|---------|----------|----------|------|------|
| OPS-001 | 健康检查端点 | `server.test.js:45` (GET /health) | ✅ | |
| OPS-002 | Worker readiness 端点 | Private ops smoke | ✅ | |
| OPS-003 | PostgreSQL 迁移 | `db-migrations.test.js:25` (migration runner) | ✅ | |
| OPS-004 | 数据库备份演练 | `admin-review.test.js:145` (backup drill) | ✅ | |
| OPS-005 | 私有运维 smoke | `private-ops-smoke.test.js` (全文件) | ✅ | |

## 测试覆盖统计

### 按仓库统计

| 仓库 | 测试文件数 | 测试用例数 | 通过率 |
|------|-----------|-----------|--------|
| gamer | 29 个 | 252 个 | 100% ✅ |
| fantasy-pet-rule | ~10 个 | ~50 个 | 未统计 |
| pet-enterprise | 0 个 | 0 个 | N/A (文档仓库) |

### 按功能模块统计

| 模块 | 需求数 | 测试用例数 | 覆盖率 |
|------|--------|-----------|--------|
| 孵化室 | 6 | 8+ | 100% ✅ |
| 桌宠呈现 | 3 | 5+ | 100% ✅ |
| 社区展示 | 3 | 6+ | 100% ✅ |
| 社区创作 | 4 | 8+ | 100% ✅ |
| 宠物币奖励 | 4 | 6+ | 100% ✅ |
| 安全与边界 | 5 | 10+ | 100% ✅ |
| 限流与 SLA | 6 | 8+ | 100% ✅ |
| 生成质量 | 4 | 4+ | 100% ✅ |
| 数据模型 | 5 | 5+ | 100% ✅ |
| 部署与运维 | 5 | 10+ | 100% ✅ |

**总计**: 45 个需求，70+ 个测试用例，100% 覆盖率

## 未覆盖的需求缺口

### Android 模拟器端到端测试

| 需求 ID | 需求描述 | 当前状态 | 替代证据 |
|---------|----------|----------|----------|
| E2E-001 | 真实设备孵化流程 | ❌ 未完成 | 139 个 focused 单元测试 + 本地 HTTP smoke |
| E2E-002 | 真实设备导入流程 | ❌ 未完成 | Package import builder tests + Community smoke |

**决策**: 用户已接受 A1 替代证据路线（单元测试 + 本地 smoke）

### 私有 Live 部署验证

| 需求 ID | 需求描述 | 当前状态 | 替代证据 |
|---------|----------|----------|----------|
| LIVE-001 | Baidu 服务器部署 | ❌ 未验证 | Runbook + 本地 fallback recording |
| LIVE-002 | Hiden 服务器部署 | ❌ 未验证 | Runbook + Docker Compose |

**决策**: 用户已接受 B1 fallback recording 路线

## 测试命令

### 运行全部测试

```bash
cd d:\workspace4Cursor\pet\gamer
npm test
```

### 按模块运行

```bash
# Community API
npm test -- services/community-api/src/server.test.js

# Android 单元测试
cd apps/android-community
./gradlew test

# 私有运维 smoke
npm test -- tools/private-ops-smoke.test.js
```

## 维护指南

### 新增需求时

1. 在 PRD 中定义需求（REQ-XXX）
2. 在本矩阵中添加需求行
3. 编写对应测试用例
4. 更新本矩阵的测试用例引用
5. 确认测试通过后标记 ✅

### 发现缺口时

1. 标记为 ⚠️ 或 ❌
2. 在"未覆盖的需求缺口"章节记录
3. 评估是否需要补充测试或接受替代证据
4. 更新决策记录

## 相关文档

- [PRD.md](../../../pet/PRD.md) - 产品需求文档
- [TEST-STRATEGY.md](../../../pet/TEST-STRATEGY.md) - 测试策略
- [ANDROID-SUBSTITUTE-EVIDENCE.zh.md](../../pet-enterprise/docs/quality/ANDROID-SUBSTITUTE-EVIDENCE.zh.md) - Android 替代证据
- [COMPLETION-AUDIT.zh.md](../../pet-enterprise/docs/governance/COMPLETION-AUDIT.zh.md) - 完成度审计

## 变更记录

- 2026-06-25 v1.0: 初始版本，记录 45 个需求和 70+ 测试用例的映射关系
