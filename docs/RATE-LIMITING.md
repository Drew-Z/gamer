# Community API 限流配置指南

## 概述

Community API 包含内置的速率限制功能，用于防止滥用和保护服务器资源。

## 配置参数

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `COMMUNITY_RATE_LIMIT_ENABLED` | `1` (启用) | 是否启用限流 |
| `COMMUNITY_RATE_LIMIT_WINDOW_MS` | `60000` (1分钟) | 时间窗口（毫秒） |
| `COMMUNITY_RATE_LIMIT_WRITE_MAX` | `4` | 写操作限制（次/窗口/IP） |
| `COMMUNITY_RATE_LIMIT_READ_MAX` | `60` | 读操作限制（次/窗口/IP） |

## 工作原理

### 1. 请求分类

- **写操作** (POST/PUT/DELETE)：创建、修改、删除资源
  - 例如：签到、创建孵化任务、提交审核
  - 默认限制：4 次/分钟/IP

- **读操作** (GET)：查询、浏览资源
  - 例如：获取社区动态、查询余额、轮询任务状态
  - 默认限制：60 次/分钟/IP

### 2. IP 识别

- 优先使用 `X-Forwarded-For` 头（反向代理场景）
- 回退到 socket remoteAddress

### 3. 豁免路径

以下路径不受限流限制：
- `/health` - 健康检查

## 超限响应

当请求超过限制时，API 返回：

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Retry-After: 45

{
  "error": "rate_limit_exceeded",
  "retryAfterMs": 45000
}
```

**响应头**：
- `Retry-After`: 重试等待秒数
- HTTP 状态码：429

## 使用场景

### 生产部署（推荐）

```bash
# .env.local
COMMUNITY_RATE_LIMIT_ENABLED=1
COMMUNITY_RATE_LIMIT_WINDOW_MS=60000
COMMUNITY_RATE_LIMIT_WRITE_MAX=4
COMMUNITY_RATE_LIMIT_READ_MAX=60
```

### 开发/测试环境

```bash
# 开发时可选择禁用以方便调试
COMMUNITY_RATE_LIMIT_ENABLED=0
```

### 高流量场景调整

如果用户量大，可以适当放宽限制：

```bash
COMMUNITY_RATE_LIMIT_WRITE_MAX=10
COMMUNITY_RATE_LIMIT_READ_MAX=120
```

## Android 客户端建议

### 轮询策略

为避免触发限流，建议：

1. **孵化任务轮询**
   - 初始间隔：3 秒
   - 失败后退避：2秒 → 4秒 → 8秒 → 16秒
   - 最大重试次数：5 次

2. **社区动态刷新**
   - 用户主动下拉刷新：无限制
   - 自动刷新：≥ 30 秒间隔

3. **余额查询**
   - 仅在必要时查询（签到后、审核通过后）
   - 避免轮询余额

### 错误处理

```kotlin
// 处理 429 响应
when (response.code) {
    429 -> {
        val retryAfter = response.headers["Retry-After"]?.toIntOrNull() ?: 60
        delay(retryAfter * 1000L)
        retry()
    }
}
```

## 监控指标

建议监控以下指标：

- 429 响应数量（按端点）
- 被限流的 IP 地址
- 平均请求间隔
- 峰值 QPS

## 故障排查

### 频繁触发限流

**症状**：用户反馈操作失败，日志中大量 429 响应

**排查步骤**：

1. 检查是否有异常 IP：
   ```bash
   grep "429" logs/access.log | cut -d' ' -f1 | sort | uniq -c | sort -rn
   ```

2. 检查客户端轮询间隔是否过短

3. 考虑提高限制或优化客户端逻辑

### 限流未生效

**症状**：刷量行为未被阻止

**排查步骤**：

1. 确认 `COMMUNITY_RATE_LIMIT_ENABLED=1`
2. 检查反向代理是否正确传递 `X-Forwarded-For`
3. 查看日志确认 IP 识别是否正确

## 安全建议

1. **生产环境必须启用**：防止刷量和 DDoS
2. **配合鉴权使用**：限流不能替代身份验证
3. **监控异常流量**：定期检查被限流的 IP
4. **动态调整阈值**：根据实际用户行为优化限制

## 未来增强

考虑的优化方向：

- [ ] 按用户 ID 限流（而不是 IP）
- [ ] 不同端点不同限制
- [ ] Redis 存储（支持分布式部署）
- [ ] 动态阈值（根据服务器负载）
- [ ] IP 黑白名单

## 相关文档

- [Community API 规范](../docs/api/community-api.md)
- [安全威胁模型](../../pet-enterprise/docs/security/THREAT-MODEL.md)
- [运维 Runbook](../../pet-enterprise/docs/ops/RUNBOOK.md)
