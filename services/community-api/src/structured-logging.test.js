import test from "node:test";
import assert from "node:assert/strict";
import { createLogger, requestLogger } from "./structured-logging.js";

test("logger redacts sensitive fields", () => {
  let logged = null;
  const originalLog = console.log;
  console.log = (msg) => { logged = JSON.parse(msg); };
  
  try {
    const logger = createLogger({ service: 'test' });
    logger.info('test message', {
      token: 'secret-token-123',
      password: 'my-password',
      normalField: 'visible'
    });
    
    assert.equal(logged.token, '[REDACTED]');
    assert.equal(logged.password, '[REDACTED]');
    assert.equal(logged.normalField, 'visible');
    assert.equal(logged.message, 'test message');
  } finally {
    console.log = originalLog;
  }
});

test("logger includes timestamp and level", () => {
  let logged = null;
  const originalLog = console.log;
  console.log = (msg) => { logged = JSON.parse(msg); };
  
  try {
    const logger = createLogger({ service: 'test' });
    logger.info('test message');
    
    assert.ok(logged.timestamp);
    assert.equal(logged.level, 'info');
    assert.equal(logged.service, 'test');
  } finally {
    console.log = originalLog;
  }
});

test("logger.child creates child logger with merged context", () => {
  let logged = null;
  const originalLog = console.log;
  console.log = (msg) => { logged = JSON.parse(msg); };
  
  try {
    const parent = createLogger({ service: 'test', parentField: 'parent' });
    const child = parent.child({ childField: 'child' });
    child.info('child message');
    
    assert.equal(logged.service, 'test');
    assert.equal(logged.parentField, 'parent');
    assert.equal(logged.childField, 'child');
    assert.equal(logged.message, 'child message');
  } finally {
    console.log = originalLog;
  }
});

test("logger outputs error level to stderr", () => {
  let logged = null;
  const originalError = console.error;
  console.error = (msg) => { logged = JSON.parse(msg); };
  
  try {
    const logger = createLogger({ service: 'test' });
    logger.error('error message', { errorCode: 500 });
    
    assert.equal(logged.level, 'error');
    assert.equal(logged.message, 'error message');
    assert.equal(logged.errorCode, 500);
  } finally {
    console.error = originalError;
  }
});

test("requestLogger creates logger with request context", () => {
  const request = {
    method: 'GET',
    url: '/v1/feed?limit=10',
    headers: {
      host: 'localhost:4000',
      'x-request-id': 'test-req-123'
    },
    socket: {
      remoteAddress: '127.0.0.1'
    }
  };
  
  let logged = null;
  const originalLog = console.log;
  console.log = (msg) => { logged = JSON.parse(msg); };
  
  try {
    const logger = requestLogger(request);
    logger.info('request received');
    
    assert.equal(logged.requestId, 'test-req-123');
    assert.equal(logged.method, 'GET');
    assert.equal(logged.path, '/v1/feed');
    assert.equal(logged.ip, '127.0.0.1');
  } finally {
    console.log = originalLog;
  }
});

test("requestLogger generates request ID if not provided", () => {
  const request = {
    method: 'POST',
    url: '/v1/check-in',
    headers: {
      host: 'localhost:4000'
    },
    socket: {
      remoteAddress: '192.168.1.1'
    }
  };
  
  let logged = null;
  const originalLog = console.log;
  console.log = (msg) => { logged = JSON.parse(msg); };
  
  try {
    const logger = requestLogger(request);
    logger.info('request received');
    
    assert.ok(logged.requestId);
    assert.match(logged.requestId, /^req-\d+-[a-z0-9]+$/);
  } finally {
    console.log = originalLog;
  }
});

test("logger redacts nested sensitive fields", () => {
  let logged = null;
  const originalLog = console.log;
  console.log = (msg) => { logged = JSON.parse(msg); };
  
  try {
    const logger = createLogger({ service: 'test' });
    logger.info('nested test', {
      user: {
        id: 'user-123',
        apiKey: 'secret-key',
        profile: {
          name: 'Test User',
          password: 'nested-password'
        }
      }
    });
    
    assert.equal(logged.user.id, 'user-123');
    assert.equal(logged.user.apiKey, '[REDACTED]');
    assert.equal(logged.user.profile.name, 'Test User');
    assert.equal(logged.user.profile.password, '[REDACTED]');
  } finally {
    console.log = originalLog;
  }
});
