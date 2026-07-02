/**
 * Structured logging for Community API
 * 
 * Provides consistent log formatting with:
 * - Timestamp
 * - Log level
 * - Context fields (userId, jobId, etc.)
 * - Message
 * - Automatic redaction of sensitive data
 */

const SENSITIVE_PATTERNS = [
  /token/i,
  /password/i,
  /secret/i,
  /authorization/i,
  /api[_-]?key/i
];

function isSensitiveField(key) {
  return SENSITIVE_PATTERNS.some(pattern => pattern.test(key));
}

function redactSensitiveData(obj) {
  if (typeof obj !== 'object' || obj === null) {
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(redactSensitiveData);
  }

  const redacted = {};
  for (const [key, value] of Object.entries(obj)) {
    if (isSensitiveField(key)) {
      redacted[key] = '[REDACTED]';
    } else if (typeof value === 'object') {
      redacted[key] = redactSensitiveData(value);
    } else {
      redacted[key] = value;
    }
  }
  return redacted;
}

class Logger {
  constructor(context = {}) {
    this.context = context;
  }

  child(additionalContext) {
    return new Logger({ ...this.context, ...additionalContext });
  }

  _log(level, message, fields = {}) {
    const timestamp = new Date().toISOString();
    const logEntry = {
      timestamp,
      level,
      message,
      ...this.context,
      ...fields
    };

    const redacted = redactSensitiveData(logEntry);
    
    // In production, send to structured logging system (e.g., CloudWatch, Datadog)
    // For now, JSON to stdout/stderr
    const output = level === 'error' ? console.error : console.log;
    output(JSON.stringify(redacted));
  }

  debug(message, fields) {
    this._log('debug', message, fields);
  }

  info(message, fields) {
    this._log('info', message, fields);
  }

  warn(message, fields) {
    this._log('warn', message, fields);
  }

  error(message, fields) {
    this._log('error', message, fields);
  }
}

// Create root logger
export function createLogger(context = {}) {
  return new Logger(context);
}

// Default logger instance
export const logger = createLogger({ service: 'community-api' });

// Express/HTTP middleware to add request context to logger
export function requestLogger(request) {
  const requestId = request.headers['x-request-id'] || `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  const url = new URL(request.url, `http://${request.headers.host}`);
  
  return createLogger({
    service: 'community-api',
    requestId,
    method: request.method,
    path: url.pathname,
    ip: request.socket?.remoteAddress
  });
}
