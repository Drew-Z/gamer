import { execFileSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";

const DEFAULT_FANTASY_PET_RULE_ROOT = resolve(process.cwd(), "..", "fantasy-pet-rule");
const CODEX_BUNDLED_PYTHON = join(
  process.env.USERPROFILE ?? "",
  ".cache",
  "codex-runtimes",
  "codex-primary-runtime",
  "dependencies",
  "python",
  "python.exe"
);
const DEFAULT_ANDROID_COMMUNITY_ROOT = join(
  process.cwd(),
  "apps",
  "android-community",
  "app",
  "src",
  "main",
  "java",
  "com",
  "gamer",
  "community"
);
const ALLOWED_UNHANDLED_PUBLIC_ENDPOINT_PATHS = [];

export function buildFantasyPetPublicApiCoverageReport(options = {}) {
  const contract = options.contract ?? loadFantasyPetAppApiContract(options);
  const handoff = options.handoff ?? loadFantasyPetAppHandoffRecord(options);
  const sources = readAndroidFantasyPetSources(options.androidCommunityRoot);
  const combinedSource = Object.values(sources).join("\n");
  const createRequestSchema = contract.examples?.createJobRequest?.schema;
  const reviewDecisionSchema = contract.examples?.reviewDecisionRequest?.schema;
  const createFields = contract.requestFields?.[createRequestSchema] ?? {};
  const unhandledPublicEndpointPaths = publicEndpointPathsNotRepresented(
    combinedSource,
    contract.publicEndpoints ?? []
  );

  return {
    contractSchema: contract.schema,
    missingSchemas: missingStrings(
      combinedSource,
      [createRequestSchema, reviewDecisionSchema].filter(Boolean)
    ),
    missingBodyShapes: missingStrings(
      sources.service,
      createFields.limits?.bodyShapes ?? []
    ),
    missingReviewDecisions: missingStrings(
      sources.service,
      contract.reviewDecisions ?? []
    ),
    missingProgressStatuses: missingStrings(
      sources.service,
      contract.progressStatuses ?? []
    ),
    missingNextActions: missingStrings(
      sources.service,
      contract.nextActions ?? []
    ),
    missingGenerationStages: generationStagesCoveredByPassThrough(sources)
      ? []
      : contract.responseFields?.generationProgress?.currentStageValues ?? [],
    missingPublicEndpointPaths: missingEndpointPaths(
      combinedSource,
      contract.publicEndpoints ?? []
    ),
    unhandledPublicEndpointPaths,
    unexpectedUnhandledPublicEndpointPaths: unhandledPublicEndpointPaths.filter(
      (path) => !ALLOWED_UNHANDLED_PUBLIC_ENDPOINT_PATHS.includes(path)
    ),
    internalHandoffArtifactsWithoutAndroidMarkers: missingInternalHandoffArtifactMarkers(
      combinedSource,
      handoff.internalArtifactsNotForApp ?? []
    ),
    internalAuditPolicyFieldsWithoutAndroidMarkers: missingInternalAuditPolicyFieldMarkers(
      combinedSource,
      handoff.internalAuditPolicy ?? {}
    ),
    adminEndpointReferences: referencedAdminEndpoints(
      combinedSource,
      contract.adminEndpoints ?? []
    ),
    candidateArtifactReviewGate:
      sources.service.includes('kind == "candidate"') &&
      sources.service.includes("review_target_must_be_candidate"),
    packageDownloadIsGated:
      sources.service.includes("downloadReady || job.nextAction == \"download-package\"") &&
      sources.service.includes("package_not_ready"),
    communityImportUiControlsReachable:
      sources.ui.includes("repository.createImportDraftFromFantasyPetPackage") &&
      sources.ui.includes("repository.submitImportDraftToCommunity") &&
      sources.ui.includes("submitCommunityReviewContentDescription") &&
      sources.ui.includes("refreshCommunitySubmissionContentDescription") &&
      sources.strings.includes("generation-submit-community-review-button") &&
      sources.strings.includes("generation-refresh-community-submission-button"),
    serverWorkerWaitNoticeReachable:
      sources.service.includes("generationServerWorkerWaitNotice") &&
      sources.ui.includes("generationServerWorkerWaitNotice") &&
      sources.ui.includes("serverWorkerWaitNoticeContentDescription") &&
      sources.strings.includes("generation-server-worker-wait-notice"),
    usesTargetDownloadId:
      combinedSource.includes("targetDownloadId") &&
      !combinedSource.includes("targetOutput"),
    securityBoundary: securityBoundary(contract.security ?? {}),
  };
}

export function loadFantasyPetAppApiContract(options = {}) {
  const fantasyPetRuleRoot =
    options.fantasyPetRuleRoot ??
    process.env.FANTASY_PET_RULE_ROOT ??
    DEFAULT_FANTASY_PET_RULE_ROOT;
  const scriptPath = join(fantasyPetRuleRoot, "tools", "build_app_api_contract.py");

  if (!existsSync(scriptPath)) {
    throw new Error(`fantasy_pet_contract_builder_missing:${scriptPath}`);
  }

  const stdout = execPythonFile([scriptPath], {
    cwd: join(fantasyPetRuleRoot, "tools"),
  });
  return JSON.parse(stdout);
}

export function loadFantasyPetAppHandoffRecord(options = {}) {
  const fantasyPetRuleRoot =
    options.fantasyPetRuleRoot ??
    process.env.FANTASY_PET_RULE_ROOT ??
    DEFAULT_FANTASY_PET_RULE_ROOT;
  const handoffPath = join(
    fantasyPetRuleRoot,
    "app-handoff",
    "gamer-app-generation-flow-record.json"
  );

  if (!existsSync(handoffPath)) {
    throw new Error(`fantasy_pet_handoff_missing:${handoffPath}`);
  }

  return JSON.parse(readFileSync(handoffPath, "utf8"));
}

function execPythonFile(args, options) {
  const commands = pythonCommandCandidates();
  const errors = [];

  for (const command of commands) {
    try {
      return execFileSync(command.executable, [...command.args, ...args], {
        ...options,
        encoding: "utf8",
        stdio: ["ignore", "pipe", "pipe"],
      });
    } catch (error) {
      errors.push(`${command.executable} ${command.args.join(" ")}: ${error.status ?? error.code ?? error.message}`);
    }
  }

  throw new Error(`python_unavailable_for_fantasy_pet_contract:${errors.join("; ")}`);
}

function pythonCommandCandidates() {
  return [
    process.env.FANTASY_PET_CONTRACT_PYTHON,
    process.env.PYTHON,
    existsSync(CODEX_BUNDLED_PYTHON) ? CODEX_BUNDLED_PYTHON : "",
    "python",
  ]
    .filter(Boolean)
    .map((executable) => ({ executable, args: [] }))
    .concat([{ executable: "py", args: ["-3"] }]);
}

function readAndroidFantasyPetSources(androidCommunityRoot = DEFAULT_ANDROID_COMMUNITY_ROOT) {
  const generationRoot = join(androidCommunityRoot, "generation");

  return {
    dto: readRequiredFile(join(generationRoot, "FantasyPetGenerationDtos.kt")),
    service: readRequiredFile(join(generationRoot, "FantasyPetGenerationService.kt")),
    client: readRequiredFile(join(generationRoot, "HttpFantasyPetGenerationClient.kt")),
    ui: readRequiredFile(join(androidCommunityRoot, "ui", "PetShellApp.kt")),
    strings: readRequiredFile(join(androidCommunityRoot, "ui", "PetShellStrings.kt")),
  };
}

function readRequiredFile(path) {
  if (!existsSync(path)) {
    throw new Error(`android_generation_source_missing:${path}`);
  }
  return readFileSync(path, "utf8");
}

function missingStrings(source, values) {
  return values.filter((value) => !source.includes(`"${value}"`));
}

function generationStagesCoveredByPassThrough(sources) {
  return (
    sources.dto.includes("val currentStage: String") &&
    sources.service.includes("generationProgress.message") &&
    sources.service.includes("generationProgress.steps")
  );
}

function missingEndpointPaths(source, endpoints) {
  return endpoints
    .filter((endpoint) => endpoint.public !== false)
    .map((endpoint) => endpoint.path)
    .filter((path) => path.startsWith("/pet-generation-jobs") || path === "/worker-readiness")
    .filter((path) => !pathFragments(path).every((fragment) => source.includes(fragment)));
}

function publicEndpointPathsNotRepresented(source, endpoints) {
  return endpoints
    .filter((endpoint) => endpoint.public !== false)
    .map((endpoint) => endpoint.path)
    .filter((path) => !pathFragments(path).every((fragment) => source.includes(fragment)));
}

function missingInternalHandoffArtifactMarkers(source, internalArtifacts) {
  const lowerSource = source.toLowerCase();
  return internalArtifacts
    .map((artifact) => artifact.split(/[\\/]/u).at(-1)?.toLowerCase() ?? "")
    .filter(isConcreteMarker)
    .filter((marker) => !lowerSource.includes(marker));
}

export function missingInternalAuditPolicyFieldMarkers(source, internalAuditPolicy) {
  const lowerSource = source.toLowerCase();
  return [...internalAuditPolicyFieldMarkers(internalAuditPolicy)]
    .sort()
    .filter((marker) => !lowerSource.includes(marker));
}

function internalAuditPolicyFieldMarkers(internalAuditPolicy) {
  const markers = new Set();
  const fieldValues = Object.values(internalAuditPolicy)
    .filter(Array.isArray)
    .flatMap(internalAuditPolicyStringValues);
  for (const value of fieldValues) {
    for (const marker of auditFieldMarkers(value)) {
      markers.add(marker);
    }
  }
  return markers;
}

function internalAuditPolicyStringValues(value) {
  if (Array.isArray(value)) {
    return value.flatMap(internalAuditPolicyStringValues);
  }
  if (value && typeof value === "object") {
    return Object.values(value).flatMap(internalAuditPolicyStringValues);
  }
  return typeof value === "string" ? [value] : [];
}

function auditFieldMarkers(value) {
  return value
    .split(/[./\\[\]]+/u)
    .map((part) => part.replace(/[^A-Za-z0-9-]+/gu, ""))
    .filter(Boolean)
    .map((part) => part.toLowerCase())
    .filter(isDomainAuditFieldMarker);
}

function isDomainAuditFieldMarker(marker) {
  const exactMarkers = new Set([
    "caseid",
    "referencetype",
    "strengthstopreserve",
    "reviewlessons",
  ]);
  const domainTerms = [
    "casebook",
    "codex",
    "genericagent",
    "hardfailure",
    "ledger",
    "learning",
    "memory",
    "needsrevision",
    "qualitygate",
    "regression",
    "repair",
    "route",
    "stagegate",
  ];
  return exactMarkers.has(marker) || domainTerms.some((term) => marker.includes(term));
}

function referencedAdminEndpoints(source, endpoints) {
  const contractAdminReferences = endpoints
    .map((endpoint) => endpoint.path)
    .filter((path) => source.includes(path));
  const forbiddenAdminTerms = [
    "/admin/",
    "server-worker-cycle",
    "agent-outputs",
  ].filter((term) => source.includes(term));

  return [...new Set([...contractAdminReferences, ...forbiddenAdminTerms])];
}

function isConcreteMarker(marker) {
  return /[a-z0-9]/u.test(marker);
}

function securityBoundary(security) {
  return {
    exposesInternalPaths: security.exposesInternalPaths,
    exposesRawPrompt: security.exposesRawPrompt,
    exposesWorkerCommands: security.exposesWorkerCommands,
    exposesSecrets: security.exposesSecrets,
    appMayInvokeAgentsDirectly: security.appMayInvokeAgentsDirectly,
    requiresHumanReview: security.requiresHumanReview,
    adminEndpointsDisabledByDefault: security.adminEndpointsDisabledByDefault,
  };
}

function pathFragments(path) {
  return path
    .split(/\{[^}]+\}/u)
    .filter((fragment) => fragment.length > 0);
}
