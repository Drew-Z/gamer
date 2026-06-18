export function formatRequestLog({ method, path, status, durationMs, ts }) {
  return JSON.stringify({
    ts: ts ?? new Date().toISOString(),
    method,
    path,
    status,
    durationMs
  });
}

const defaultWriter = (line) => console.log(line);

export function createRequestLogger(writer = defaultWriter) {
  return (entry) => writer(formatRequestLog(entry));
}
