import test from "node:test";
import assert from "node:assert/strict";
import { getMetricsCollector, handleMetricsRequest } from "./metrics.js";

test("metrics collector records HTTP requests", () => {
  const metrics = getMetricsCollector();
  
  // Record some requests
  metrics.recordRequest("GET", "/v1/feed", 200, 45);
  metrics.recordRequest("GET", "/v1/feed", 200, 52);
  metrics.recordRequest("POST", "/v1/check-in", 200, 120);
  metrics.recordRequest("GET", "/v1/wallet/me", 500, 30);
  
  const output = metrics.toPrometheusText();
  
  // Check counters
  assert.match(output, /community_http_requests_total\{method="GET",path="\/v1\/feed",status="200"\} 2/);
  assert.match(output, /community_http_requests_total\{method="POST",path="\/v1\/check-in",status="200"\} 1/);
  assert.match(output, /community_http_requests_total\{method="GET",path="\/v1\/wallet\/me",status="500"\} 1/);
  
  // Check duration histogram
  assert.match(output, /community_http_request_duration_ms.*method="GET",path="\/v1\/feed"/);
});

test("metrics collector records rate limit events", () => {
  const metrics = getMetricsCollector();
  
  metrics.recordRateLimitExceeded();
  metrics.recordRateLimitExceeded();
  
  const output = metrics.toPrometheusText();
  assert.match(output, /community_rate_limit_exceeded_total/);
});

test("metrics collector tracks active requests", () => {
  const metrics = getMetricsCollector();
  
  const initialActive = metrics.activeRequests;
  
  metrics.incrementActiveRequests();
  metrics.incrementActiveRequests();
  assert.equal(metrics.activeRequests, initialActive + 2);
  
  metrics.decrementActiveRequests();
  assert.equal(metrics.activeRequests, initialActive + 1);
  
  const output = metrics.toPrometheusText();
  assert.match(output, /community_active_requests \d+/);
});

test("metrics endpoint returns Prometheus text format", () => {
  const metrics = getMetricsCollector();
  metrics.recordRequest("GET", "/health", 200, 10);
  
  const response = {
    writeHead: (status, headers) => {
      assert.equal(status, 200);
      assert.equal(headers['Content-Type'], 'text/plain; version=0.0.4');
    },
    end: (body) => {
      assert.match(body, /# HELP community_http_requests_total/);
      assert.match(body, /# TYPE community_http_requests_total counter/);
    }
  };
  
  handleMetricsRequest(null, response);
});

test("metrics collector calculates percentiles correctly", () => {
  const metrics = getMetricsCollector();
  
  // Add known distribution
  for (let i = 1; i <= 100; i++) {
    metrics.recordRequest("GET", "/test", 200, i);
  }
  
  const output = metrics.toPrometheusText();
  
  // P50 should be around 50ms
  assert.match(output, /community_http_request_duration_ms\{method="GET",path="\/test",quantile="0.5"\} 5\d/);
  
  // P95 should be around 95ms
  assert.match(output, /community_http_request_duration_ms\{method="GET",path="\/test",quantile="0.95"\} 9\d/);
  
  // Count should be 100
  assert.match(output, /community_http_request_duration_ms_count\{method="GET",path="\/test"\} 100/);
});
