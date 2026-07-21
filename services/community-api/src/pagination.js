/**
 * Shared pagination helpers for Community API list endpoints.
 *
 * Uses opaque offset-based cursors encoded as base64url JSON. Offset
 * pagination is sufficient for the current demo-scale lists and behaves
 * identically across the in-memory, file, and Postgres stores. Cursors are
 * opaque to clients: they must round-trip the value, not construct it.
 */

export const DEFAULT_PAGE_LIMIT = 20;
export const MAX_PAGE_LIMIT = 100;

const encodeCursor = (offset) =>
  Buffer.from(JSON.stringify({ offset }), "utf8").toString("base64url");

const decodeCursor = (cursor) => {
  if (typeof cursor !== "string" || cursor.trim() === "") {
    return 0;
  }

  try {
    const decoded = JSON.parse(Buffer.from(cursor, "base64url").toString("utf8"));
    const offset = Number(decoded?.offset);
    if (Number.isInteger(offset) && offset >= 0) {
      return offset;
    }
  } catch {
    return 0;
  }

  return 0;
};

const normalizeLimit = (limit) => {
  const parsed = Number(limit);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return DEFAULT_PAGE_LIMIT;
  }
  return Math.min(Math.floor(parsed), MAX_PAGE_LIMIT);
};

/**
 * Resolve pagination params into a normalized { limit, offset } pair.
 * Accepts either a raw cursor string or a numeric offset.
 */
const normalizeOffset = (offset) => {
  const parsed = Number(offset);
  if (!Number.isInteger(parsed) || parsed < 0) {
    return 0;
  }
  return parsed;
};

export const resolvePagination = ({ cursor, limit, offset } = {}) => ({
  limit: normalizeLimit(limit),
  offset: cursor ? decodeCursor(cursor) : normalizeOffset(offset)
});

/**
 * Parse pagination params from a URL's search params.
 * Supports ?cursor=<opaque>&limit=<n>.
 */
export const parsePaginationFromUrl = (url) =>
  resolvePagination({
    cursor: url.searchParams.get("cursor"),
    limit: url.searchParams.get("limit"),
    offset: url.searchParams.get("offset")
  });

/**
 * Paginate an in-memory array. Returns the page slice plus the next opaque
 * cursor (null when the list is exhausted) and a hasMore flag.
 */
export const paginateArray = (items, { limit, offset }) => {
  const safeOffset = Number.isInteger(offset) && offset >= 0 ? offset : 0;
  const safeLimit = normalizeLimit(limit);
  const page = items.slice(safeOffset, safeOffset + safeLimit);
  const nextOffset = safeOffset + page.length;
  const hasMore = nextOffset < items.length;

  return {
    page,
    nextCursor: hasMore ? encodeCursor(nextOffset) : null,
    hasMore
  };
};

/**
 * Build the next cursor for keyset/offset queries when the backend returns
 * one extra row beyond the requested limit to detect more pages.
 */
export const buildNextCursor = ({ offset, limit, returnedCount }) => {
  const safeOffset = Number.isInteger(offset) && offset >= 0 ? offset : 0;
  const safeLimit = normalizeLimit(limit);
  const hasMore = returnedCount > safeLimit;
  return {
    hasMore,
    nextCursor: hasMore ? encodeCursor(safeOffset + safeLimit) : null
  };
};
