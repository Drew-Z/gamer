import {
  users
} from "../../../packages/community-contracts/src/index.js";
import { createCommunityStore } from "./store.js";

const json = (status, body) => ({ status, body });
const defaultStore = createCommunityStore();

export function handleCommunityRequest(method, requestUrl, options = {}) {
  const url = new URL(requestUrl, "http://localhost");
  const store = options.store ?? defaultStore;
  const body = options.body ?? {};
  const currentUserId = body.userId ?? users[0].id;

  if (method === "GET" && url.pathname === "/health") {
    return json(200, {
      ok: true,
      service: "community-api"
    });
  }

  if (method === "GET" && url.pathname === "/v1/feed") {
    return json(200, store.getFeed());
  }

  if (method === "GET" && url.pathname === "/v1/wallet/me") {
    return json(200, store.getWallet(currentUserId));
  }

  if (method === "POST" && url.pathname === "/v1/check-in") {
    const date = body.date ?? new Date().toISOString().slice(0, 10);
    return json(200, store.claimDailyCheckIn(currentUserId, date));
  }

  if (method === "GET" && url.pathname === "/v1/submissions") {
    return json(200, store.listSubmissions());
  }

  if (method === "POST" && url.pathname === "/v1/submissions") {
    const submission = store.createSubmission({
      petId: body.petId,
      userId: currentUserId,
      ownershipClaimId: body.ownershipClaimId,
      scoreReportId: body.scoreReportId
    });
    return json(201, submission);
  }

  if (method === "POST" && url.pathname === "/v1/admin/reviews") {
    const review = store.reviewSubmission({
      submissionId: body.submissionId,
      status: body.status,
      reviewer: body.reviewer ?? "admin-local",
      rewardAmount: Number(body.rewardAmount ?? 0)
    });

    if (review.error) {
      return json(404, review);
    }

    return json(200, review);
  }

  if (method === "GET" && url.pathname === "/v1/me") {
    return json(200, store.getMe());
  }

  return json(404, {
    error: "not_found",
    method,
    path: url.pathname
  });
}
