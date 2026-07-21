import assert from "node:assert/strict";
import test from "node:test";
import {
  DEFAULT_PAGE_LIMIT,
  MAX_PAGE_LIMIT,
  buildNextCursor,
  paginateArray,
  parsePaginationFromUrl,
  resolvePagination
} from "./pagination.js";

const makeUrl = (search) => new URL(`http://localhost/v1/feed${search}`);

test("resolvePagination defaults to first page with default limit", () => {
  const { limit, offset } = resolvePagination();
  assert.equal(limit, DEFAULT_PAGE_LIMIT);
  assert.equal(offset, 0);
});

test("resolvePagination clamps limit to the max page size", () => {
  const { limit } = resolvePagination({ limit: 9999 });
  assert.equal(limit, MAX_PAGE_LIMIT);
});

test("resolvePagination falls back to defaults on invalid limit", () => {
  assert.equal(resolvePagination({ limit: "abc" }).limit, DEFAULT_PAGE_LIMIT);
  assert.equal(resolvePagination({ limit: -5 }).limit, DEFAULT_PAGE_LIMIT);
  assert.equal(resolvePagination({ limit: 0 }).limit, DEFAULT_PAGE_LIMIT);
});

test("resolvePagination ignores malformed cursors", () => {
  assert.equal(resolvePagination({ cursor: "not-a-cursor" }).offset, 0);
  assert.equal(resolvePagination({ cursor: "" }).offset, 0);
  assert.equal(resolvePagination({ cursor: null }).offset, 0);
});

test("resolvePagination accepts numeric offsets when cursor is absent", () => {
  assert.equal(resolvePagination({ offset: 3 }).offset, 3);
  assert.equal(resolvePagination({ offset: "4" }).offset, 4);
  assert.equal(resolvePagination({ offset: -1 }).offset, 0);
  assert.equal(resolvePagination({ offset: "abc" }).offset, 0);
});

test("paginateArray slices the page and emits an opaque next cursor", () => {
  const items = Array.from({ length: 5 }, (_, index) => index);
  const firstPage = paginateArray(items, { limit: 2, offset: 0 });

  assert.deepEqual(firstPage.page, [0, 1]);
  assert.equal(firstPage.hasMore, true);
  assert.ok(typeof firstPage.nextCursor === "string" && firstPage.nextCursor.length > 0);

  const secondPage = paginateArray(items, resolvePagination({ cursor: firstPage.nextCursor, limit: 2 }));
  assert.deepEqual(secondPage.page, [2, 3]);
  assert.equal(secondPage.hasMore, true);

  const lastPage = paginateArray(items, resolvePagination({ cursor: secondPage.nextCursor, limit: 2 }));
  assert.deepEqual(lastPage.page, [4]);
  assert.equal(lastPage.hasMore, false);
  assert.equal(lastPage.nextCursor, null);
});

test("paginateArray returns no cursor when the list fits in one page", () => {
  const result = paginateArray([1, 2], { limit: 20, offset: 0 });
  assert.deepEqual(result.page, [1, 2]);
  assert.equal(result.hasMore, false);
  assert.equal(result.nextCursor, null);
});

test("parsePaginationFromUrl reads cursor and limit query params", () => {
  const { limit, offset } = parsePaginationFromUrl(makeUrl("?limit=5"));
  assert.equal(limit, 5);
  assert.equal(offset, 0);
});

test("parsePaginationFromUrl reads offset when cursor is absent", () => {
  const { limit, offset } = parsePaginationFromUrl(makeUrl("?limit=5&offset=3"));
  assert.equal(limit, 5);
  assert.equal(offset, 3);
});

test("buildNextCursor signals more pages when an extra row is returned", () => {
  const more = buildNextCursor({ offset: 0, limit: 2, returnedCount: 3 });
  assert.equal(more.hasMore, true);
  assert.ok(more.nextCursor);

  const done = buildNextCursor({ offset: 0, limit: 2, returnedCount: 2 });
  assert.equal(done.hasMore, false);
  assert.equal(done.nextCursor, null);
});

test("cursors round-trip through encode/decode via resolvePagination", () => {
  const { nextCursor } = paginateArray([0, 1, 2, 3], { limit: 2, offset: 0 });
  const decoded = resolvePagination({ cursor: nextCursor, limit: 2 });
  assert.equal(decoded.offset, 2);
});
