import { readFile as defaultReadFile } from "node:fs/promises";

export class StateSourceError extends Error {
  constructor(code, message, status = 400) {
    super(message);
    this.name = "StateSourceError";
    this.code = code;
    this.status = status;
  }
}

export async function resolveFantasyPetRuleState(payload = {}, options = {}) {
  if (Object.hasOwn(payload, "state")) {
    return payload.state;
  }

  if (typeof payload.statePath !== "string" || payload.statePath.trim() === "") {
    throw new StateSourceError(
      "state_missing",
      "Provide either state or statePath for fantasy-pet-rule state."
    );
  }

  const readFile = options.readFile ?? defaultReadFile;
  let rawState;

  try {
    rawState = await readFile(payload.statePath, "utf8");
  } catch (error) {
    throw new StateSourceError(
      "state_file_unreadable",
      error instanceof Error
        ? `Unable to read statePath: ${error.message}`
        : "Unable to read statePath."
    );
  }

  try {
    return JSON.parse(rawState);
  } catch (error) {
    throw new StateSourceError(
      "state_file_invalid_json",
      error instanceof Error
        ? `Unable to parse statePath JSON: ${error.message}`
        : "Unable to parse statePath JSON."
    );
  }
}
