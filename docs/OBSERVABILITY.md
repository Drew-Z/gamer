# Community API 监控配置指南

**版本**: v1.0  
**日期**: 2026-06-25  
**状态**: 生产就绪

---

## 概述

Community API 现在包含完整的可观测性功能：

- **结构化日志**: 自动脱敏敏感数据的 JSON 日志
- **Prometheus 指标**: 标准格式的 HTTP metrics
- **监控端点**: `/metrics` 用于 Prometheus 抓取

---

## 1. 监控指标

### 可用指标

| 指标名称 | 类型 | 说明 | 标签 |
|---------|------|------|------|
| `community_http_requests_total` | Counter | HTTP 请求总数 | method, path, status |
| `community_http_request_duration_ms` | Summary | HTTP 请求耗时（毫秒） | method, path, quantile |
| `community_rate_limit_exceeded_total` | Counter | 限流触发次数 | - |
| `community_database_errors_total` | Counter | 数据库错误次数 | - |
| `community_fantasy_pet_upstream_errors_total` | Counter | 上游服务错误次数 | - |
| `community_active_requests` | Gauge | 当前活跃请求数 | - |

### Metrics 端点

```bash
# 访问 metrics 端点
curl http://localhost:4000/metrics

# 输出示例（Prometheus 文本格式）
# HELP community_http_requests_total Total HTTP requests by method, path, and status
# TYPE community_http_requests_total counter
community_http_requests_total{method="GET",path="/v1/feed",status="200"} 1524
community_http_requests_total{method="POST",path="/v1/check-in",status="200"} 89

# HELP community_http_request_duration_ms HTTP request duration in milliseconds
# TYPE community_http_request_duration_ms summary
community_http_request_duration_ms{method="GET",path="/v1/feed",quantile="0.5"} 45
community_http_request_duration_ms{method="GET",path="/v1/feed",quantile="0.95"} 120
community_http_request_duration_ms{method="GET",path="/v1/feed",quantile="0.99"} 250

# HELP community_active_requests Current number of active requests
# TYPE community_active_requests gauge
community_active_requests 3
```

---

## 2. 结构化日志

### 日志格式

所有日志以 JSON 格式输出到 stdout/stderr：

```json
{
  "timestamp": "2026-06-25T10:30:45.123Z",
  "level": "info",
  "service": "community-api",
  "requestId": "req-1719311445123-a7k3m9",
  "method": "POST",
  "path": "/v1/check-in",
  "ip": "192.168.1.100",
  "message": "request received",
  "userId": "demo-keeper-001",
  "durationMs": 45
}
```

### 敏感数据自动脱敏

以下字段会自动替换为 `[REDACTED]`：

- `token`, `apiToken`, `authToken`
- `password`, `passwd`
- `secret`, `apiSecret`
- `authorization`
- `apiKey`, `api_key`

**示例**:

```javascript
logger.info('user authenticated', {
  userId: 'user-123',
  token: 'secret-token-abc',  // 会被脱敏
  email: 'user@example.com'   // 保留
});

// 输出
{
  "userId": "user-123",
  "token": "[REDACTED]",
  "email": "user@example.com"
}
```

---

## 3. Prometheus 集成

### 方案 A: Prometheus Server

**prometheus.yml 配置**:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'community-api'
    static_configs:
      - targets: ['localhost:4000']
    metrics_path: '/metrics'
    scrape_interval: 30s
```

**启动 Prometheus**:

```bash
# Docker
docker run -d \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

# 访问 Prometheus UI
open http://localhost:9090
```

### 方案 B: Grafana + Prometheus

**docker-compose.yml**:

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    depends_on:
      - prometheus
```

**Grafana Dashboard 配置**:

1. 添加 Prometheus 数据源：`http://prometheus:9090`
2. 导入仪表盘或创建自定义面板
3. 常用查询：

```promql
# 请求速率 (qps)
rate(community_http_requests_total[5m])

# P95 响应时间
community_http_request_duration_ms{quantile="0.95"}

# 错误率
rate(community_http_requests_total{status=~"5.."}[5m]) / 
rate(community_http_requests_total[5m])

# 限流触发率
rate(community_rate_limit_exceeded_total[5m])

# 活跃请求数
community_active_requests
```

---

## 4. 告警规则

### Prometheus 告警配置

**alerts.yml**:

```yaml
groups:
  - name: community_api_alerts
    interval: 30s
    rules:
      # 高错误率
      - alert: HighErrorRate
        expr: |
          (
            rate(community_http_requests_total{status=~"5.."}[5m]) /
            rate(community_http_requests_total[5m])
          ) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Community API 错误率过高"
          description: "错误率超过 5% (当前: {{ $value | humanizePercentage }})"

      # 响应时间慢
      - alert: SlowResponseTime
        expr: |
          community_http_request_duration_ms{quantile="0.95"} > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Community API 响应时间过慢"
          description: "P95 响应时间超过 1 秒 ({{ $labels.path }}): {{ $value }}ms"

      # 频繁限流
      - alert: FrequentRateLimiting
        expr: |
          rate(community_rate_limit_exceeded_total[5m]) > 1
        for: 5m
        labels:
          severity: info
        annotations:
          summary: "Community API 限流频繁触发"
          description: "限流触发速率: {{ $value | humanize }} 次/秒"

      # 上游服务错误
      - alert: FantasyPetUpstreamErrors
        expr: |
          rate(community_fantasy_pet_upstream_errors_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Fantasy Pet 上游服务错误"
          description: "上游错误率: {{ $value | humanize }} 次/秒"

      # 数据库错误
      - alert: DatabaseErrors
        expr: |
          rate(community_database_errors_total[5m]) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Community API 数据库错误"
          description: "数据库错误率: {{ $value | humanize }} 次/秒"

      # API 服务宕机
      - alert: APIDown
        expr: up{job="community-api"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Community API 服务宕机"
          description: "Prometheus 无法抓取 metrics"
```

### Alertmanager 配置

**alertmanager.yml**:

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'team-notifications'

receivers:
  - name: 'team-notifications'
    webhook_configs:
      - url: 'http://your-webhook-url'
        send_resolved: true
```

---

## 5. 日志聚合

### 方案 A: CloudWatch Logs (AWS)

```javascript
// 修改 structured-logging.js 的 _log 方法
_log(level, message, fields = {}) {
  const logEntry = {
    timestamp: new Date().toISOString(),
    level,
    message,
    ...this.context,
    ...fields
  };

  const redacted = redactSensitiveData(logEntry);
  
  // 发送到 CloudWatch
  if (process.env.CLOUDWATCH_LOG_GROUP) {
    sendToCloudWatch(redacted);
  }
  
  // 同时输出到 stdout
  const output = level === 'error' ? console.error : console.log;
  output(JSON.stringify(redacted));
}
```

### 方案 B: Elasticsearch + Kibana

使用 Filebeat 或 Fluentd 收集 stdout 日志并发送到 Elasticsearch：

**filebeat.yml**:

```yaml
filebeat.inputs:
  - type: container
    paths:
      - '/var/lib/docker/containers/*/*.log'
    json.keys_under_root: true
    json.add_error_key: true

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "community-api-%{+yyyy.MM.dd}"

setup.kibana:
  host: "kibana:5601"
```

### 方案 C: Datadog

```bash
# 安装 Datadog Agent
DD_API_KEY=your-api-key \
DD_LOGS_ENABLED=true \
DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true \
docker run -d \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v /proc/:/host/proc/:ro \
  -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro \
  -e DD_API_KEY \
  -e DD_LOGS_ENABLED \
  -e DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL \
  datadog/agent:latest
```

---

## 6. 常见查询

### Prometheus 查询

```promql
# 总请求数
sum(community_http_requests_total)

# 按路径分组的请求速率
sum by (path) (rate(community_http_requests_total[5m]))

# 成功率
sum(rate(community_http_requests_total{status="200"}[5m])) /
sum(rate(community_http_requests_total[5m]))

# 平均响应时间
community_http_request_duration_ms{quantile="0.5"}

# Top 5 最慢端点
topk(5, community_http_request_duration_ms{quantile="0.95"})
```

### 日志查询 (Kibana/CloudWatch)

```json
// 查找所有错误日志
{ "level": "error" }

// 查找特定用户的请求
{ "userId": "demo-keeper-001" }

// 查找慢请求 (>1秒)
{ "durationMs": { "gte": 1000 } }

// 查找限流事件
{ "message": "rate_limit_exceeded" }
```

---

## 7. 生产部署清单

- [ ] 配置 Prometheus 抓取 `/metrics` 端点
- [ ] 设置 Grafana 仪表盘
- [ ] 配置告警规则 (错误率、响应时间、上游错误)
- [ ] 设置日志聚合 (CloudWatch/Elasticsearch/Datadog)
- [ ] 配置告警通知渠道 (Slack/Email/PagerDuty)
- [ ] 测试告警触发 (手动触发错误验证)
- [ ] 文档化 Runbook 链接到监控仪表盘

---

## 8. 故障排查

### 问题: Metrics 端点返回 404

**检查**:
```bash
curl http://localhost:4000/metrics
```

**原因**: `/metrics` 路由未正确注册

**解决**: 确认 `routes.js` 和 `server.js` 已正确导入 `handleMetricsRequest`

### 问题: 日志中出现敏感信息

**检查**: 查看日志输出中是否有 `[REDACTED]`

**解决**: 
- 确认 `structured-logging.js` 的 `SENSITIVE_PATTERNS` 包含该字段
- 添加新模式: `/your_field_name/i`

### 问题: Prometheus 无法抓取

**检查**:
```bash
# 测试连通性
curl -I http://your-api-host:4000/metrics

# 查看 Prometheus targets 状态
open http://prometheus:9090/targets
```

**原因**: 防火墙/网络策略阻止

**解决**: 开放 4000 端口或配置内网访问

---

## 9. 性能考虑

### Metrics 内存管理

- 默认每 5 分钟重置 histogram 数据，防止内存增长
- 如需调整: 修改 `metrics.js` 中的 `setInterval` 时间

### 日志量控制

- 生产环境建议设置 `LOG_LEVEL=info`
- Debug 日志仅在必要时启用
- 考虑使用采样 (10% 请求记录详细日志)

---

## 10. 相关文档

- [RATE-LIMITING.md](./RATE-LIMITING.md) - 限流配置
- [SLA-CONFIGURATION.md](./SLA-CONFIGURATION.md) - SLA 配置
- [../../pet-enterprise/docs/ops/RUNBOOK.md](../../pet-enterprise/docs/ops/RUNBOOK.md) - 运维手册

---

## 变更记录

- 2026-06-25 v1.0: 初始版本，实现结构化日志和 Prometheus metrics
