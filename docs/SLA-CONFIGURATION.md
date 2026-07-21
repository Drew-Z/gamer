# Community API SLA 配置文档

## 概述

Community API 通过 `/v1/sla` 端点向客户端（特别是 Android App）提供 SLA 承诺，包括孵化耗时预期、轮询策略和失败阈值。

## API 端点

### GET /v1/sla

返回当前配置的 SLA 参数。

**响应示例**：

```json
{
  "schema": "gamer.sla.v1",
  "hatch": {
    "reserveEggMaxMs": 120000,
    "mysteryEggMaxMs": 600000,
    "customHatchMaxMs": 900000
  },
  "polling": {
    "suggestedIntervalMs": 3000,
    "maxAttempts": 5,
    "baseBackoffMs": 2000
  },
  "failureThresholds": {
    "consecutivePollFailuresBeforeSlowNotice": 3
  }
}
```

## 配置参数详解

### 1. 孵化耗时 (hatch)

| 字段 | 默认值 | 说明 | 用户体验 |
|------|--------|------|----------|
| `reserveEggMaxMs` | `120000` (2分钟) | 备用蛋孵化最大耗时 | "稍等片刻，约2分钟内完成" |
| `mysteryEggMaxMs` | `600000` (10分钟) | 神秘蛋孵化最大耗时 | "惊喜需要等待，约10分钟" |
| `customHatchMaxMs` | `900000` (15分钟) | 自主孵化最大耗时 | "定制需要时间，约15分钟" |

**环境变量配置**：

```bash
# .env.local
SLA_HATCH_RESERVE_MS=120000
SLA_HATCH_MYSTERY_MS=600000
SLA_HATCH_CUSTOM_MS=900000
```

### 2. 轮询策略 (polling)

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `suggestedIntervalMs` | `3000` (3秒) | 建议轮询间隔 |
| `maxAttempts` | `5` | 最大重试次数 |
| `baseBackoffMs` | `2000` (2秒) | 退避基准时间 |

**退避算法**：

```kotlin
// Android 实现示例
fun calculateBackoff(attempt: Int, baseMs: Int): Long {
    return min(baseMs * (1 shl attempt), 60000L) // 指数退避，最大60秒
}

// 重试间隔：2s → 4s → 8s → 16s → 32s
```

### 3. 失败阈值 (failureThresholds)

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `consecutivePollFailuresBeforeSlowNotice` | `3` | 连续失败3次后显示慢响应提示 |

**用户体验**：

```kotlin
if (consecutiveFailures >= sla.failureThresholds.consecutivePollFailuresBeforeSlowNotice) {
    showToast("服务器响应较慢，请稍候...")
}
```

## Android 集成指南

### 1. 获取 SLA

在 App 启动或进入孵化页面时获取 SLA：

```kotlin
class CommunityRepository(private val client: CommunityApiClient) {
    suspend fun loadInitialCommunity(): InitialCommunityResult {
        val slaResult = client.getCommunitySla()
        
        val hatchSla = when (slaResult) {
            is ApiCallResult.Success -> slaResult.value.toHatchSla()
            is ApiCallResult.Failure -> HatchSla.default() // 降级到默认值
        }
        
        return InitialCommunityResult(
            hatchSla = hatchSla,
            // ... 其他字段
        )
    }
}

fun CommunitySlaDto.toHatchSla(): HatchSla = HatchSla(
    customHatchMaxMs = hatch.customHatchMaxMs,
    suggestedPollIntervalMs = polling.suggestedIntervalMs,
    consecutivePollFailuresBeforeSlowNotice = 
        failureThresholds.consecutivePollFailuresBeforeSlowNotice
)
```

### 2. 显示预期耗时

在孵化 UI 中显示 SLA 预期：

```kotlin
@Composable
fun HatcherySlaNotice(sla: HatchSla) {
    Column {
        Text("预计耗时：")
        Text("备用蛋 ≤ ${sla.reserveEggMaxMs / 60000} 分钟")
        Text("神秘蛋 ≤ ${sla.mysteryEggMaxMs / 60000} 分钟")
        Text("自主孵化 ≤ ${sla.customHatchMaxMs / 60000} 分钟")
        
        Text("建议每 ${sla.suggestedPollIntervalMs / 1000} 秒轮询")
    }
}
```

### 3. 实现轮询逻辑

```kotlin
class GenerationPollingViewModel(
    private val repository: CommunityRepository,
    private val sla: HatchSla
) {
    private var consecutiveFailures = 0
    
    fun startPolling(jobId: String) {
        viewModelScope.launch {
            var attempt = 0
            
            while (attempt < sla.maxAttempts) {
                val result = repository.pollGenerationJob(jobId)
                
                when (result) {
                    is Success -> {
                        consecutiveFailures = 0
                        if (result.value.isComplete) break
                    }
                    is Failure -> {
                        consecutiveFailures++
                        
                        if (consecutiveFailures >= 
                            sla.consecutivePollFailuresBeforeSlowNotice) {
                            showSlowNotice()
                        }
                    }
                }
                
                val backoff = calculateBackoff(attempt, sla.baseBackoffMs)
                delay(backoff)
                attempt++
            }
        }
    }
}
```

## 服务端配置示例

### 生产环境（默认）

```bash
# .env.local
# 使用默认值，无需配置
```

### 高性能环境

如果 Worker 性能较好，可以缩短 SLA：

```bash
SLA_HATCH_CUSTOM_MS=600000  # 10分钟（原15分钟）
```

### 低配环境

如果资源受限，可以延长 SLA：

```bash
SLA_HATCH_CUSTOM_MS=1200000  # 20分钟
SLA_POLLING_INTERVAL_MS=5000  # 5秒轮询（降低服务器压力）
```

## 监控与告警

### 建议监控指标

1. **SLA 达成率**：
   ```sql
   SELECT 
     COUNT(CASE WHEN duration_ms <= sla_max_ms THEN 1 END) * 100.0 / COUNT(*) AS sla_rate
   FROM generation_jobs
   WHERE status = 'completed'
   ```

2. **P95/P99 孵化耗时**：
   ```sql
   SELECT 
     PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY duration_ms) AS p95,
     PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY duration_ms) AS p99
   FROM generation_jobs
   WHERE status = 'completed'
   ```

### 告警规则

```yaml
# Prometheus 告警规则示例
- alert: HatchSlaViolation
  expr: |
    rate(generation_duration_seconds{status="completed"}[5m]) > 900
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "孵化耗时超过 SLA (>15分钟)"
```

## 未来增强

- [ ] 动态 SLA：根据当前 Worker 负载实时调整
- [ ] 分级 SLA：VIP 用户更短的 SLA
- [ ] 队列位置反馈：告诉用户前面还有几个任务
- [ ] 预估完成时间：基于历史数据预测

## 相关文档

- [PRD §13 目标与成功指标](../../../pet-enterprise/docs/requirements/PRD.md#13-目标与成功指标)
- [ROADMAP.md TD-03](../../../ROADMAP.md)
- [Android Generation UI Model](../apps/android-community/app/src/main/java/com/gamer/community/generation/FantasyPetGenerationUiModel.kt)

## 测试验证

运行测试确认 SLA 配置生效：

```bash
# 单元测试
npm test -- --test-name-pattern="SLA"

# 集成测试
node --test services/community-api/src/server.test.js
```

**预期结果**：
- `/v1/sla` 返回正确的配置值
- Android 测试中 `CommunitySlaDto` 解析成功
- 轮询逻辑遵循 SLA 间隔
