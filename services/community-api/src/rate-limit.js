export function resolveClientIp(request) {
  const forwarded = request.headers?.["x-forwarded-for"];
  if (typeof forwarded === "string" && forwarded.trim() !== "") {
    return forwarded.split(",")[0].trim();
  }
  return request.socket?.remoteAddress ?? "unknown";
}

export function createRateLimiterPolicy({
  windowMs = 60_000,
  writeMax = 4,
  readMax = 60,
  exemptPaths = ["/health"]
} = {}) {
  const writeBuckets = new Map();
  const readBuckets = new Map();

  function evaluate(buckets, ip, max, now) {
    const bucket = buckets.get(ip);
    if (!bucket || now - bucket.windowStart >= windowMs) {
      buckets.set(ip, { windowStart: now, count: 1 });
      return {
        limited: 1 > max,
        remaining: Math.max(0, max - 1),
        retryAfterMs: windowMs
      };
    }

    bucket.count += 1;
    const limited = bucket.count > max;
    return {
      limited,
      remaining: Math.max(0, max - bucket.count),
      retryAfterMs: limited ? windowMs - (now - bucket.windowStart) : 0
    };
  }

  return {
    isExempt(path) {
      return exemptPaths.includes(path);
    },
    check(method, ip, now = Date.now()) {
      if (method === "POST") {
        return evaluate(writeBuckets, ip, writeMax, now);
      }
      return evaluate(readBuckets, ip, readMax, now);
    }
  };
}
