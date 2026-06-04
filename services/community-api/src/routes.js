import {
  checkInState,
  feedPosts,
  reviewQueue,
  submissions,
  users,
  wallet
} from "../../../packages/community-contracts/src/index.js";

const json = (status, body) => ({ status, body });

export function handleCommunityRequest(method, requestUrl) {
  const url = new URL(requestUrl, "http://localhost");

  if (method === "GET" && url.pathname === "/health") {
    return json(200, {
      ok: true,
      service: "community-api"
    });
  }

  if (method === "GET" && url.pathname === "/v1/feed") {
    return json(200, {
      items: feedPosts,
      nextCursor: "fixture-page-2"
    });
  }

  if (method === "GET" && url.pathname === "/v1/wallet/me") {
    return json(200, wallet);
  }

  if (method === "POST" && url.pathname === "/v1/check-in") {
    return json(200, {
      checkIn: checkInState,
      wallet
    });
  }

  if (method === "GET" && url.pathname === "/v1/submissions") {
    return json(200, {
      submissions,
      reviewQueue
    });
  }

  if (method === "GET" && url.pathname === "/v1/me") {
    return json(200, users[0]);
  }

  return json(404, {
    error: "not_found",
    method,
    path: url.pathname
  });
}
