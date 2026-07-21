/**
 * Prometheus metrics instrumentation for Community API
 * 
 * Exports key metrics for monitoring:
 * - HTTP request duration and rate
 * - Rate limiting events
 * - Database operation status
 * - Fantasy Pet upstream health
 */

// Simple in-memory metrics store
// For production, consider prometheus-client or prom-client npm package
class MetricsCollector {
  constructor() {
    this.httpRequestsTotal = new Map(); // counter by method, path, status
    this.httpRequestDurationMs = new Map(); // histogram by method, path
    this.rateLimitExceeded = 0; // counter
    this.databaseErrors = 0; // counter
    this.fantasyPetUpstreamErrors = 0; // counter
    this.activeRequests = 0; // gauge
  }

  recordRequest(method, path, statusCode, durationMs) {
    const key = `${method}:${path}:${statusCode}`;
    this.httpRequestsTotal.set(key, (this.httpRequestsTotal.get(key) || 0) + 1);
    
    const histKey = `${method}:${path}`;
    if (!this.httpRequestDurationMs.has(histKey)) {
      this.httpRequestDurationMs.set(histKey, []);
    }
    this.httpRequestDurationMs.get(histKey).push(durationMs);
  }

  recordRateLimitExceeded() {
    this.rateLimitExceeded++;
  }

  recordDatabaseError() {
    this.databaseErrors++;
  }

  recordFantasyPetUpstreamError() {
    this.fantasyPetUpstreamErrors++;
  }

  incrementActiveRequests() {
    this.activeRequests++;
  }

  decrementActiveRequests() {
    this.activeRequests--;
  }

  // Export metrics in Prometheus text format
  toPrometheusText() {
    const lines = [];

    // HTTP requests total
    lines.push('# HELP community_http_requests_total Total HTTP requests by method, path, and status');
    lines.push('# TYPE community_http_requests_total counter');
    for (const [key, count] of this.httpRequestsTotal.entries()) {
      const [method, path, status] = key.split(':');
      lines.push(`community_http_requests_total{method="${method}",path="${path}",status="${status}"} ${count}`);
    }

    // HTTP request duration (simplified percentiles)
    lines.push('# HELP community_http_request_duration_ms HTTP request duration in milliseconds');
    lines.push('# TYPE community_http_request_duration_ms summary');
    for (const [key, durations] of this.httpRequestDurationMs.entries()) {
      const [method, path] = key.split(':');
      if (durations.length === 0) continue;
      
      const sorted = [...durations].sort((a, b) => a - b);
      const p50 = sorted[Math.floor(sorted.length * 0.5)] || 0;
      const p95 = sorted[Math.floor(sorted.length * 0.95)] || 0;
      const p99 = sorted[Math.floor(sorted.length * 0.99)] || 0;
      
      lines.push(`community_http_request_duration_ms{method="${method}",path="${path}",quantile="0.5"} ${p50}`);
      lines.push(`community_http_request_duration_ms{method="${method}",path="${path}",quantile="0.95"} ${p95}`);
      lines.push(`community_http_request_duration_ms{method="${method}",path="${path}",quantile="0.99"} ${p99}`);
      lines.push(`community_http_request_duration_ms_count{method="${method}",path="${path}"} ${durations.length}`);
    }

    // Rate limit exceeded
    lines.push('# HELP community_rate_limit_exceeded_total Total rate limit exceeded events');
    lines.push('# TYPE community_rate_limit_exceeded_total counter');
    lines.push(`community_rate_limit_exceeded_total ${this.rateLimitExceeded}`);

    // Database errors
    lines.push('# HELP community_database_errors_total Total database errors');
    lines.push('# TYPE community_database_errors_total counter');
    lines.push(`community_database_errors_total ${this.databaseErrors}`);

    // Fantasy Pet upstream errors
    lines.push('# HELP community_fantasy_pet_upstream_errors_total Total Fantasy Pet upstream errors');
    lines.push('# TYPE community_fantasy_pet_upstream_errors_total counter');
    lines.push(`community_fantasy_pet_upstream_errors_total ${this.fantasyPetUpstreamErrors}`);

    // Active requests
    lines.push('# HELP community_active_requests Current number of active requests');
    lines.push('# TYPE community_active_requests gauge');
    lines.push(`community_active_requests ${this.activeRequests}`);

    return lines.join('\n') + '\n';
  }

  // Reset duration histograms periodically to avoid memory growth
  resetHistograms() {
    this.httpRequestDurationMs.clear();
  }
}

// Singleton metrics collector
let metricsCollector = null;

export function getMetricsCollector() {
  if (!metricsCollector) {
    metricsCollector = new MetricsCollector();
    
    // Reset histograms every 5 minutes to prevent memory growth
    const resetTimer = setInterval(() => {
      metricsCollector.resetHistograms();
    }, 5 * 60_000);
    resetTimer.unref?.();
  }
  return metricsCollector;
}

// Middleware to instrument requests
export function instrumentRequest(handler) {
  return async (request, response) => {
    const metrics = getMetricsCollector();
    const startTime = Date.now();
    
    metrics.incrementActiveRequests();
    
    try {
      await handler(request, response);
      
      const duration = Date.now() - startTime;
      const path = new URL(request.url, `http://${request.headers.host}`).pathname;
      metrics.recordRequest(request.method, path, response.statusCode, duration);
    } catch (error) {
      const duration = Date.now() - startTime;
      const path = new URL(request.url, `http://${request.headers.host}`).pathname;
      metrics.recordRequest(request.method, path, 500, duration);
      throw error;
    } finally {
      metrics.decrementActiveRequests();
    }
  };
}

// Metrics endpoint handler
export function handleMetricsRequest(request, response) {
  const metrics = getMetricsCollector();
  response.writeHead(200, {
    'Content-Type': 'text/plain; version=0.0.4'
  });
  response.end(metrics.toPrometheusText());
}
