package com.gamer.community.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.net.URLEncoder
import com.gamer.community.firstSpritesheetFrame
import com.gamer.community.api.ApiCallResult
import com.gamer.community.api.CommunityRepository
import com.gamer.community.api.HttpCommunityApiClient
import com.gamer.community.api.ImportDraftDto
import com.gamer.community.generation.CONTRACT_DEMO_PROGRESS_MESSAGE
import com.gamer.community.generation.CandidateGalleryItem
import com.gamer.community.generation.DEFAULT_GENERATION_MESSAGE
import com.gamer.community.generation.FantasyPetGenerationService
import com.gamer.community.generation.FantasyPetPackageImportRequestBuilder
import com.gamer.community.generation.FantasyPetPreviewDownloader
import com.gamer.community.generation.GENERATION_BODY_SHAPE_OPTIONS
import com.gamer.community.generation.GenerationProgressStepItem
import com.gamer.community.generation.HttpFantasyPetGenerationClient
import com.gamer.community.generation.PetPreviewDownloadResult
import com.gamer.community.generation.PetGenerationJobResponseDto
import com.gamer.community.generation.PetGenerationPackageImportCandidate
import com.gamer.community.generation.REVIEW_NOTE_SUGGESTIONS
import com.gamer.community.generation.appendReviewNoteSuggestion
import com.gamer.community.generation.canClearGenerationJob
import com.gamer.community.generation.canCreateGenerationJob
import com.gamer.community.generation.canPollGenerationJob
import com.gamer.community.generation.canRefreshPackageImportSubmission
import com.gamer.community.generation.canShowPackageDownload
import com.gamer.community.generation.canSubmitPackageImportDraft
import com.gamer.community.generation.canSubmitReviewDecision
import com.gamer.community.generation.clearedGenerationJobUiState
import com.gamer.community.generation.generationJobIdForResume
import com.gamer.community.generation.generationJobHistoryAfterPersist
import com.gamer.community.generation.generationPollDelayMillis
import com.gamer.community.generation.generationContractDemoNotice
import com.gamer.community.generation.generationProgressSummaryLine
import com.gamer.community.generation.generationCreateValidationMessage
import com.gamer.community.generation.generationJobHistoryAfterRemove
import com.gamer.community.generation.initialGenerationJobHistory
import com.gamer.community.generation.persistedGenerationJobId
import com.gamer.community.generation.packageDownloadFailureMessage
import com.gamer.community.generation.packageDownloadStartedMessage
import com.gamer.community.generation.packageDownloadSuccessMessage
import com.gamer.community.generation.packageImportDraftFailureCandidate
import com.gamer.community.generation.packageImportDraftSuccessCandidate
import com.gamer.community.generation.packageImportCandidateMessage
import com.gamer.community.generation.packageImportInProgressCandidate
import com.gamer.community.generation.packageImportSubmissionIdForResume
import com.gamer.community.generation.packageImportSubmissionFailureMessage
import com.gamer.community.generation.packageImportSubmissionCommunityRefreshMessage
import com.gamer.community.generation.packageImportSubmissionResumeMessage
import com.gamer.community.generation.packageImportSubmissionStartedMessage
import com.gamer.community.generation.packageImportSubmissionSuccessMessage
import com.gamer.community.generation.pollGenerationJobValidationMessage
import com.gamer.community.generation.recentGenerationJobResumeId
import com.gamer.community.generation.selectedCandidateAfterJobRefresh
import com.gamer.community.generation.shouldPollGenerationJob
import com.gamer.community.generation.reviewNotesValidationMessage
import com.gamer.community.generation.serializedGenerationJobHistory
import com.gamer.community.generation.generationServerWorkerWaitNotice
import com.gamer.community.generation.effectiveProgressStatus
import com.gamer.community.petshell.ApprovedPet
import com.gamer.community.petshell.FeedDirection
import com.gamer.community.petshell.FeedPost
import com.gamer.community.petshell.PetAction
import com.gamer.community.petshell.PetShellController
import com.gamer.community.petshell.PetShellState
import com.gamer.community.petshell.ShellPhase
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class PetShellTab {
    Community,
    Generate,
    Profile
}

private enum class CommunityQuickActionIcon {
    CheckIn,
    Generate,
    Review,
    Showcase
}

private enum class FeedControlGlyph {
    Previous,
    Next,
    Skip
}

internal data class PetShellHeaderBackgroundSpec(
    val startColor: Color,
    val endColor: Color,
    val accentColor: Color,
    val titleColor: Color,
    val subtitleColor: Color
)

private val GamerColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F3EE),
    onPrimaryContainer = Color(0xFF0D3430),
    secondary = Color(0xFFF97316),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE2C7),
    onSecondaryContainer = Color(0xFF4C2605),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF101828),
    surface = Color.White,
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEFF4F8),
    onSurfaceVariant = Color(0xFF475467),
    outline = Color(0xFF8C94A1)
)

@Composable
fun PetShellApp(
    repository: CommunityRepository,
    generationService: FantasyPetGenerationService,
    initialGenerationDescription: String = "",
    canShowDesktopPetOverlay: () -> Boolean = { false },
    canPostDesktopPetNotification: () -> Boolean = { true },
    onRequestDesktopPetOverlayPermission: () -> Unit = {},
    onRequestDesktopPetNotificationPermission: () -> Unit = {},
    onStartDesktopPetOverlay: () -> Unit = {},
    onStopDesktopPetOverlay: () -> Unit = {},
    onResetDesktopPetOverlayPosition: () -> Unit = {},
    onRefreshDesktopPetNotification: () -> Unit = {}
) {
    val context = LocalContext.current
    val generationPrefs = remember(context) {
        context.getSharedPreferences("fantasy-pet-generation", Context.MODE_PRIVATE)
    }
    val uiPrefs = remember(context) {
        context.getSharedPreferences("pet-shell-ui", Context.MODE_PRIVATE)
    }
    var directPetLaunchEnabled by remember {
        mutableStateOf(uiPrefs.getBoolean("directPetLaunchEnabled", false))
    }
    var desktopPetOverlayAutoShowEnabled by remember {
        mutableStateOf(uiPrefs.getBoolean("desktopPetOverlayAutoShowEnabled", false))
    }
    var desktopPetOverlayPermissionGranted by remember {
        mutableStateOf(canShowDesktopPetOverlay())
    }
    var desktopPetNotificationPermissionGranted by remember {
        mutableStateOf(canPostDesktopPetNotification())
    }
    var desktopPetOverlayRunning by remember {
        mutableStateOf(uiPrefs.getBoolean("desktopPetOverlayRunning", false))
    }
    var state by remember {
        mutableStateOf(
            PetShellController.initialState(skipLaunchBubble = directPetLaunchEnabled)
        )
    }
    var language by remember {
        mutableStateOf(parsePetShellLanguage(uiPrefs.getString("language", null)))
    }
    val strings = remember(language) {
        petShellStrings(language)
    }
    var selectedTab by remember { mutableStateOf(PetShellTab.Community) }
    var generationDescription by remember(initialGenerationDescription) {
        mutableStateOf(initialGenerationDescription)
    }
    var generationAppJobId by remember {
        mutableStateOf(generationPrefs.getString("appJobId", "").orEmpty())
    }
    var generationJobHistory by remember {
        mutableStateOf(
            initialGenerationJobHistory(
                savedAppJobId = generationPrefs.getString("appJobId", "").orEmpty(),
                rawHistory = generationPrefs.getString("appJobHistory", "").orEmpty()
            )
        )
    }
    var generationBodyShape by remember { mutableStateOf("balanced") }
    var generationReferences by remember { mutableStateOf("") }
    var generationJob by remember { mutableStateOf<PetGenerationJobResponseDto?>(null) }
    var selectedCandidateDownloadId by remember { mutableStateOf("") }
    var reviewNotes by remember { mutableStateOf("") }
    var generationMessage by remember { mutableStateOf(DEFAULT_GENERATION_MESSAGE) }
    var packageDownloadMessage by remember { mutableStateOf("") }
    var packageImportCandidate by remember { mutableStateOf<PetGenerationPackageImportCandidate?>(null) }
    var readyPackageImportDraft by remember { mutableStateOf<ImportDraftDto?>(null) }
    var packageImportSubmissionId by remember {
        mutableStateOf(
            packageImportSubmissionIdForResume(
                generationPrefs.getString("packageImportSubmissionId", "").orEmpty()
            ).orEmpty()
        )
    }
    var packageImportSubmissionMessage by remember {
        mutableStateOf(packageImportSubmissionResumeMessage(packageImportSubmissionId))
    }
    var workerReadinessMessageText by remember { mutableStateOf("") }
    val packageImportRequestBuilder = remember { FantasyPetPackageImportRequestBuilder() }
    val scope = rememberCoroutineScope()

    fun changeLanguage(nextLanguage: PetShellLanguage) {
        language = nextLanguage
        uiPrefs.edit()
            .putString("language", nextLanguage.preferenceValue)
            .apply()
        if (desktopPetOverlayRunning) {
            onRefreshDesktopPetNotification()
        }
    }

    fun changeDirectPetLaunch(enabled: Boolean) {
        directPetLaunchEnabled = enabled
        uiPrefs.edit()
            .putBoolean("directPetLaunchEnabled", enabled)
            .apply()
    }

    fun refreshDesktopPetOverlayState(): Boolean {
        val overlayGranted = canShowDesktopPetOverlay()
        val notificationGranted = canPostDesktopPetNotification()
        desktopPetOverlayPermissionGranted = overlayGranted
        desktopPetNotificationPermissionGranted = notificationGranted
        desktopPetOverlayRunning = overlayGranted &&
            notificationGranted &&
            uiPrefs.getBoolean("desktopPetOverlayRunning", desktopPetOverlayRunning)
        return overlayGranted && notificationGranted
    }

    fun requestMissingDesktopPetPermission() {
        refreshDesktopPetOverlayState()
        when {
            !desktopPetOverlayPermissionGranted -> onRequestDesktopPetOverlayPermission()
            !desktopPetNotificationPermissionGranted -> onRequestDesktopPetNotificationPermission()
        }
    }

    fun changeDesktopPetOverlayAutoShow(enabled: Boolean) {
        desktopPetOverlayAutoShowEnabled = enabled
        uiPrefs.edit()
            .putBoolean("desktopPetOverlayAutoShowEnabled", enabled)
            .apply()
        if (enabled && !refreshDesktopPetOverlayState()) {
            requestMissingDesktopPetPermission()
        }
    }

    fun requestDesktopPetOverlayPermission() {
        onRequestDesktopPetOverlayPermission()
        refreshDesktopPetOverlayState()
    }

    fun requestDesktopPetNotificationPermission() {
        onRequestDesktopPetNotificationPermission()
        refreshDesktopPetOverlayState()
    }

    fun startDesktopPetOverlay() {
        if (refreshDesktopPetOverlayState()) {
            onStartDesktopPetOverlay()
            desktopPetOverlayRunning = true
        } else {
            requestMissingDesktopPetPermission()
        }
    }

    fun stopDesktopPetOverlay() {
        onStopDesktopPetOverlay()
        desktopPetOverlayRunning = false
    }

    fun resetDesktopPetOverlayPosition() {
        onResetDesktopPetOverlayPosition()
        refreshDesktopPetOverlayState()
    }

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshDesktopPetOverlayState()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    fun openAppTab(tab: PetShellTab) {
        selectedTab = tab
        state = PetShellController.openCommunity(state)
    }

    fun clearPackageImportSubmissionTracking(clearMessage: Boolean = true) {
        packageImportSubmissionId = ""
        if (clearMessage) {
            packageImportSubmissionMessage = ""
        }
        generationPrefs.edit()
            .remove("packageImportSubmissionId")
            .apply()
    }

    fun persistPackageImportSubmissionTracking(submissionId: String) {
        val safeSubmissionId = packageImportSubmissionIdForResume(submissionId)
        if (safeSubmissionId == null) {
            clearPackageImportSubmissionTracking()
            return
        }

        packageImportSubmissionId = safeSubmissionId
        generationPrefs.edit()
            .putString("packageImportSubmissionId", safeSubmissionId)
            .apply()
    }

    suspend fun applyGenerationJobUpdate(job: PetGenerationJobResponseDto) {
        val displayJob = when (val artifactResult = generationService.refreshJobArtifacts(job)) {
            is ApiCallResult.Success -> artifactResult.value
            is ApiCallResult.Failure -> job
        }
        generationJob = displayJob
        selectedCandidateDownloadId = selectedCandidateAfterJobRefresh(
            candidates = generationService.candidateGalleryItems(displayJob),
            currentSelectedCandidateDownloadId = selectedCandidateDownloadId
        )
        generationMessage = generationService.generationProgressMessage(displayJob)
        if (!canShowPackageDownload(displayJob)) {
            packageImportCandidate = null
            readyPackageImportDraft = null
            clearPackageImportSubmissionTracking()
        }
    }

    fun persistGenerationJobId(requestedAppJobId: String, job: PetGenerationJobResponseDto) {
        val appJobIdToPersist = persistedGenerationJobId(
            requestedAppJobId = requestedAppJobId,
            job = job
        )
        if (appJobIdToPersist.isBlank()) {
            return
        }

        val updatedHistory = generationJobHistoryAfterPersist(
            existingAppJobIds = generationJobHistory,
            requestedAppJobId = requestedAppJobId,
            job = job
        )
        if (appJobIdToPersist !in updatedHistory) {
            return
        }

        generationAppJobId = appJobIdToPersist
        generationJobHistory = updatedHistory
        generationPrefs.edit()
            .putString("appJobId", appJobIdToPersist)
            .putString("appJobHistory", serializedGenerationJobHistory(updatedHistory))
            .apply()
    }

    fun pollGenerationJob(appJobId: String, startMessage: String) {
        val trimmedAppJobId = appJobId.trim()
        if (!canPollGenerationJob(trimmedAppJobId)) {
            generationMessage = pollGenerationJobValidationMessage(trimmedAppJobId)
                .ifBlank { "Enter an app job id to poll." }
            return
        }

        generationAppJobId = trimmedAppJobId
        scope.launch {
            generationMessage = startMessage
            packageDownloadMessage = ""
            packageImportCandidate = null
            readyPackageImportDraft = null
            clearPackageImportSubmissionTracking()
            when (val result = generationService.pollJob(trimmedAppJobId)) {
                is ApiCallResult.Success -> {
                    applyGenerationJobUpdate(result.value)
                    persistGenerationJobId(
                        requestedAppJobId = trimmedAppJobId,
                        job = result.value
                    )
                }
                is ApiCallResult.Failure -> {
                    generationMessage = "Generation poll failed: ${result.reason}"
                }
            }
        }
    }

    LaunchedEffect(repository) {
        val result = repository.loadInitialCommunity()
        state = PetShellController.applyCommunityLoad(
            state = state,
            posts = result.posts,
            approvedPets = result.approvedPets,
            walletBalance = result.walletBalance,
            checkInClaimed = result.checkInClaimed,
            pendingSubmissionCount = result.pendingSubmissionCount,
            usedFallback = result.usedFallback,
            message = result.message
        )
    }

    LaunchedEffect(state.approvedPets, state.approvedPetIndex) {
        val selectedPet = state.approvedPets.selectedApprovedPet(state.approvedPetIndex)
        uiPrefs.edit()
            .putString("desktopPetOverlayPreviewUrl", approvedPetPreviewUrl(selectedPet))
            .apply()
    }

    LaunchedEffect(Unit) {
        val resumeJobId = generationJobIdForResume(generationAppJobId, generationJob)
            ?: return@LaunchedEffect
        generationMessage = "Resuming generation job..."
        when (val result = generationService.pollJob(resumeJobId)) {
            is ApiCallResult.Success -> {
                applyGenerationJobUpdate(result.value)
            }
            is ApiCallResult.Failure -> {
                generationMessage = "Saved generation job unavailable: ${result.reason}"
            }
        }
    }

    LaunchedEffect(generationJob?.appJobId, generationJob?.progressStatus, generationJob?.nextAction) {
        val job = generationJob ?: return@LaunchedEffect
        if (!shouldPollGenerationJob(job)) return@LaunchedEffect

        delay(generationPollDelayMillis(job))
        when (val result = generationService.pollJob(job.appJobId)) {
            is ApiCallResult.Success -> {
                applyGenerationJobUpdate(result.value)
            }
            is ApiCallResult.Failure -> {
                generationMessage = "Generation poll failed: ${result.reason}"
            }
        }
    }

    MaterialTheme(colorScheme = GamerColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F8FA)) {
            when (state.phase) {
                ShellPhase.LaunchBubble -> LaunchBubbleScreen(
                    state = state,
                    language = language,
                    strings = strings,
                    onLanguageChange = ::changeLanguage,
                    onBubbleTapped = {
                        state = PetShellController.onBubbleTapped(state)
                    }
                )

                ShellPhase.DesktopPet -> DesktopPetScreen(
                    state = state,
                    language = language,
                    strings = strings,
                    directPetLaunchEnabled = directPetLaunchEnabled,
                    onLanguageChange = ::changeLanguage,
                    onPetNavigate = { direction ->
                        state = PetShellController.navigateApprovedPet(state, direction)
                    },
                    onOpenCommunity = { openAppTab(PetShellTab.Community) },
                    onOpenGenerate = { openAppTab(PetShellTab.Generate) },
                    onOpenProfile = { openAppTab(PetShellTab.Profile) }
                )

                ShellPhase.Community -> CommunityScreen(
                    state = state,
                    selectedTab = selectedTab,
                    language = language,
                    strings = strings,
                    directPetLaunchEnabled = directPetLaunchEnabled,
                    desktopPetOverlayAutoShowEnabled = desktopPetOverlayAutoShowEnabled,
                    desktopPetOverlayPermissionGranted = desktopPetOverlayPermissionGranted,
                    desktopPetNotificationPermissionGranted = desktopPetNotificationPermissionGranted,
                    desktopPetOverlayRunning = desktopPetOverlayRunning,
                    onTabSelected = { selectedTab = it },
                    onLanguageChange = ::changeLanguage,
                    onDirectPetLaunchChange = ::changeDirectPetLaunch,
                    onDesktopPetOverlayAutoShowChange = ::changeDesktopPetOverlayAutoShow,
                    onRequestDesktopPetOverlayPermission = ::requestDesktopPetOverlayPermission,
                    onRequestDesktopPetNotificationPermission = ::requestDesktopPetNotificationPermission,
                    onStartDesktopPetOverlay = ::startDesktopPetOverlay,
                    onStopDesktopPetOverlay = ::stopDesktopPetOverlay,
                    onResetDesktopPetOverlayPosition = ::resetDesktopPetOverlayPosition,
                    onEnterDesktopPet = {
                        state = PetShellController.openDesktopPet(state)
                    },
                    onNavigate = { direction ->
                        state = PetShellController.navigateFeed(state, direction)
                    },
                    onShowcaseNavigate = { direction ->
                        state = PetShellController.navigateApprovedPet(state, direction)
                    },
                    onCheckIn = {
                        scope.launch {
                            val result = repository.claimDailyCheckIn()
                            state = PetShellController.applyCheckInResult(
                                state = state,
                                walletBalance = result.walletBalance,
                                claimed = result.claimed,
                                rewardAmount = result.rewardAmount,
                                usedFallback = result.usedFallback,
                                message = result.message
                            )
                        }
                    },
                    generationContent = {
                        GenerationPanel(
                            strings = strings,
                            description = generationDescription,
                            onDescriptionChange = { generationDescription = it },
                            appJobId = generationAppJobId,
                            onAppJobIdChange = { generationAppJobId = it },
                            bodyShape = generationBodyShape,
                            onBodyShapeChange = { generationBodyShape = it },
                            references = generationReferences,
                            onReferencesChange = { generationReferences = it },
                            recentAppJobIds = generationJobHistory,
                            onRemoveRecentJob = { appJobId ->
                                val appJobIdToRemove = appJobId.trim()
                                val updatedHistory = generationJobHistoryAfterRemove(
                                    existingAppJobIds = generationJobHistory,
                                    appJobIdToRemove = appJobIdToRemove
                                )
                                generationJobHistory = updatedHistory
                                if (generationAppJobId.trim() == appJobIdToRemove) {
                                    generationAppJobId = ""
                                }
                                generationPrefs.edit()
                                    .putString(
                                        "appJobHistory",
                                        serializedGenerationJobHistory(updatedHistory)
                                    )
                                    .also { editor ->
                                        if (generationAppJobId.isBlank()) {
                                            editor.remove("appJobId")
                                        }
                                    }
                                    .apply()
                                generationMessage = "Recent generation job removed."
                            },
                            onResumeRecentJob = { appJobId ->
                                val resumeAppJobId = recentGenerationJobResumeId(appJobId)
                                if (resumeAppJobId == null) {
                                    generationMessage = "Recent generation job is unavailable."
                                } else {
                                    pollGenerationJob(
                                        appJobId = resumeAppJobId,
                                        startMessage = "Resuming recent generation job..."
                                    )
                                }
                            },
                            job = generationJob,
                            message = generationMessage,
                            packageDownloadMessage = packageDownloadMessage,
                            packageImportCandidateMessage = packageImportCandidateMessage(packageImportCandidate),
                            packageImportSubmissionMessage = packageImportSubmissionMessage,
                            workerReadinessMessage = workerReadinessMessageText,
                            candidates = generationJob
                                ?.let { generationService.candidateGalleryItems(it) }
                                .orEmpty(),
                            progressSteps = generationJob
                                ?.let { generationService.generationProgressStepItems(it) }
                                .orEmpty(),
                            selectedCandidateDownloadId = selectedCandidateDownloadId,
                            onSelectCandidate = { selectedCandidateDownloadId = it },
                            reviewNotes = reviewNotes,
                            onReviewNotesChange = { reviewNotes = it },
                            reviewNoteSuggestions = REVIEW_NOTE_SUGGESTIONS,
                            onAppendReviewNoteSuggestion = { suggestion ->
                                reviewNotes = appendReviewNoteSuggestion(reviewNotes, suggestion)
                            },
                            canClearJob = canClearGenerationJob(generationAppJobId, generationJob),
                            canSubmitPackageImportDraft = canSubmitPackageImportDraft(readyPackageImportDraft),
                            canRefreshPackageImportSubmission = canRefreshPackageImportSubmission(
                                packageImportSubmissionId
                            ),
                            onCreateJob = {
                                scope.launch {
                                    generationMessage = "Creating generation job..."
                                    packageDownloadMessage = ""
                                    packageImportCandidate = null
                                    readyPackageImportDraft = null
                                    clearPackageImportSubmissionTracking()
                                    when (val result = generationService.createJob(
                                        description = generationDescription,
                                        appJobId = generationAppJobId,
                                        bodyShape = generationBodyShape,
                                        referencesText = generationReferences
                                    )) {
                                        is ApiCallResult.Success -> {
                                            applyGenerationJobUpdate(result.value)
                                            persistGenerationJobId(
                                                requestedAppJobId = generationAppJobId,
                                                job = result.value
                                            )
                                        }
                                        is ApiCallResult.Failure -> {
                                            generationMessage = "Generation request failed: ${result.reason}"
                                        }
                                    }
                                }
                            },
                            onClearJob = {
                                val cleared = clearedGenerationJobUiState()
                                generationJob = null
                                generationAppJobId = cleared.appJobId
                                selectedCandidateDownloadId = cleared.selectedCandidateDownloadId
                                reviewNotes = cleared.reviewNotes
                                generationMessage = cleared.message
                                packageDownloadMessage = ""
                                packageImportCandidate = null
                                readyPackageImportDraft = null
                                clearPackageImportSubmissionTracking()
                                generationPrefs.edit()
                                    .remove("appJobId")
                                    .apply()
                            },
                            onCheckWorkerReadiness = {
                                scope.launch {
                                    workerReadinessMessageText = "Checking generation service..."
                                    workerReadinessMessageText =
                                        generationService.checkGenerationServiceStatusMessage()
                                }
                            },
                            onReviewDecision = { decision ->
                                val job = generationJob
                                if (job == null) {
                                    generationMessage = "Create a generation job before review."
                                } else {
                                    scope.launch {
                                        generationMessage = "Submitting human review..."
                                        packageDownloadMessage = ""
                                        packageImportCandidate = null
                                        readyPackageImportDraft = null
                                        clearPackageImportSubmissionTracking()
                                        when (val result = generationService.submitReviewDecisionForJob(
                                            job = job,
                                            targetDownloadId = selectedCandidateDownloadId,
                                            decision = decision,
                                            notesText = reviewNotes
                                        )) {
                                            is ApiCallResult.Success -> {
                                                applyGenerationJobUpdate(result.value)
                                                reviewNotes = ""
                                            }
                                            is ApiCallResult.Failure -> {
                                                generationMessage = "Review failed: ${result.reason}"
                                            }
                                        }
                                    }
                                }
                            },
                            onPollJob = {
                                pollGenerationJob(
                                    appJobId = generationAppJobId,
                                    startMessage = "Polling generation job..."
                                )
                            },
                            onDownloadPackage = {
                                val job = generationJob
                                if (job == null) {
                                    generationMessage = "Create a generation job before download."
                                } else {
                                    scope.launch {
                                        val downloadingMessage = packageDownloadStartedMessage()
                                        generationMessage = downloadingMessage
                                        packageDownloadMessage = downloadingMessage
                                        readyPackageImportDraft = null
                                        clearPackageImportSubmissionTracking()
                                        val outputDirectory = context.getExternalFilesDir(
                                            Environment.DIRECTORY_DOWNLOADS
                                        ) ?: context.filesDir
                                        when (val result = generationService.downloadPackageToFile(
                                            job = job,
                                            outputDirectory = outputDirectory
                                        )) {
                                            is ApiCallResult.Success -> {
                                                val successMessage = packageDownloadSuccessMessage(result.value.name)
                                                val pendingImportCandidate = generationService.packageImportCandidate(
                                                    job = job,
                                                    selectedCandidateDownloadId = selectedCandidateDownloadId,
                                                    packageFileName = result.value.name,
                                                    packageByteCount = result.value.length()
                                                )
                                                generationMessage = successMessage
                                                packageDownloadMessage = successMessage
                                                packageImportCandidate = pendingImportCandidate

                                                if (pendingImportCandidate != null) {
                                                    packageImportCandidate = packageImportInProgressCandidate(
                                                        pendingImportCandidate
                                                    )
                                                    when (val requestResult = withContext(Dispatchers.IO) {
                                                        packageImportRequestBuilder.buildRequest(
                                                            packageFile = result.value,
                                                            targetDownloadId = pendingImportCandidate.targetDownloadId
                                                        )
                                                    }) {
                                                        is ApiCallResult.Success -> {
                                                            when (val importResult =
                                                                repository.createImportDraftFromFantasyPetPackage(
                                                                    requestResult.value
                                                                )
                                                            ) {
                                                                is ApiCallResult.Success -> {
                                                                    readyPackageImportDraft = importResult.value
                                                                    clearPackageImportSubmissionTracking()
                                                                    packageImportCandidate =
                                                                        packageImportDraftSuccessCandidate(
                                                                            pendingImportCandidate,
                                                                            importResult.value
                                                                        )
                                                                }
                                                                is ApiCallResult.Failure -> {
                                                                    readyPackageImportDraft = null
                                                                    clearPackageImportSubmissionTracking()
                                                                    packageImportCandidate =
                                                                        packageImportDraftFailureCandidate(
                                                                            pendingImportCandidate,
                                                                            importResult.reason
                                                                        )
                                                                }
                                                            }
                                                        }
                                                        is ApiCallResult.Failure -> {
                                                            readyPackageImportDraft = null
                                                            clearPackageImportSubmissionTracking()
                                                            packageImportCandidate = packageImportDraftFailureCandidate(
                                                                pendingImportCandidate,
                                                                requestResult.reason
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            is ApiCallResult.Failure -> {
                                                val failureMessage = packageDownloadFailureMessage(result.reason)
                                                packageImportCandidate = null
                                                readyPackageImportDraft = null
                                                clearPackageImportSubmissionTracking()
                                                generationMessage = failureMessage
                                                packageDownloadMessage = failureMessage
                                            }
                                        }
                                    }
                                }
                            },
                            onSubmitPackageImport = {
                                val draftToSubmit = readyPackageImportDraft
                                if (draftToSubmit == null || !canSubmitPackageImportDraft(draftToSubmit)) {
                                    packageImportSubmissionMessage = packageImportSubmissionFailureMessage(
                                        "import_draft_not_ready"
                                    )
                                } else {
                                    scope.launch {
                                        val startedMessage = packageImportSubmissionStartedMessage()
                                        packageImportSubmissionMessage = startedMessage
                                        when (val submitResult =
                                            repository.submitImportDraftToCommunity(draftToSubmit)
                                        ) {
                                            is ApiCallResult.Success -> {
                                                readyPackageImportDraft = submitResult.value.draft
                                                persistPackageImportSubmissionTracking(
                                                    submitResult.value.submission.id
                                                )
                                                packageImportSubmissionMessage =
                                                    packageImportSubmissionSuccessMessage(submitResult.value)
                                                val refreshedCommunity = repository.loadInitialCommunity()
                                                state = PetShellController.applyCommunityLoad(
                                                    state = state,
                                                    posts = refreshedCommunity.posts,
                                                    approvedPets = refreshedCommunity.approvedPets,
                                                    walletBalance = refreshedCommunity.walletBalance,
                                                    checkInClaimed = refreshedCommunity.checkInClaimed,
                                                    pendingSubmissionCount =
                                                        refreshedCommunity.pendingSubmissionCount,
                                                    usedFallback = refreshedCommunity.usedFallback,
                                                    message = refreshedCommunity.message
                                                )
                                            }
                                            is ApiCallResult.Failure -> {
                                                packageImportSubmissionMessage =
                                                    packageImportSubmissionFailureMessage(submitResult.reason)
                                            }
                                        }
                                    }
                                }
                            },
                            onRefreshPackageImportSubmission = {
                                val submissionId = packageImportSubmissionId
                                if (!canRefreshPackageImportSubmission(submissionId)) {
                                    packageImportSubmissionMessage = packageImportSubmissionFailureMessage(
                                        "submission_id_required"
                                    )
                                } else {
                                    scope.launch {
                                        packageImportSubmissionMessage = "Refreshing community submission..."
                                        when (val statusResult = repository.getSubmissionStatus(submissionId)) {
                                            is ApiCallResult.Success -> {
                                                val refreshedCommunity = repository.loadInitialCommunity()
                                                packageImportSubmissionMessage =
                                                    packageImportSubmissionCommunityRefreshMessage(
                                                        statusResult.value,
                                                        refreshedCommunity
                                                    )
                                                if (statusResult.value.status.trim() in setOf(
                                                        "approved",
                                                        "rejected",
                                                        "revoked"
                                                    )
                                                ) {
                                                    clearPackageImportSubmissionTracking(clearMessage = false)
                                                }
                                                state = PetShellController.applyCommunityLoad(
                                                    state = state,
                                                    posts = refreshedCommunity.posts,
                                                    approvedPets = refreshedCommunity.approvedPets,
                                                    walletBalance = refreshedCommunity.walletBalance,
                                                    checkInClaimed = refreshedCommunity.checkInClaimed,
                                                    pendingSubmissionCount =
                                                        refreshedCommunity.pendingSubmissionCount,
                                                    usedFallback = refreshedCommunity.usedFallback,
                                                    message = refreshedCommunity.message
                                                )
                                            }
                                            is ApiCallResult.Failure -> {
                                                packageImportSubmissionMessage =
                                                    packageImportSubmissionFailureMessage(statusResult.reason)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PetShellApp() {
    val repository = remember {
        CommunityRepository(
            client = HttpCommunityApiClient(com.gamer.community.BuildConfig.COMMUNITY_API_BASE_URL)
        )
    }
    val generationService = remember {
        FantasyPetGenerationService(
            client = HttpFantasyPetGenerationClient(com.gamer.community.BuildConfig.FANTASY_PET_API_BASE_URL),
            apiBaseUrl = com.gamer.community.BuildConfig.FANTASY_PET_API_BASE_URL
        )
    }
    PetShellApp(repository = repository, generationService = generationService)
}

@Composable
private fun LaunchBubbleScreen(
    state: PetShellState,
    language: PetShellLanguage,
    strings: PetShellStrings,
    onLanguageChange: (PetShellLanguage) -> Unit,
    onBubbleTapped: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE9F4FF), Color(0xFFF9FAFC))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        LanguageToggle(
            language = language,
            strings = strings,
            onLanguageChange = onLanguageChange,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PetAvatar(action = state.petAction, strings = strings)
            Spacer(modifier = Modifier.height(30.dp))
            SpeechBubble(
                text = strings.speechBubble(state.speechBubble),
                modifier = Modifier
                    .semantics {
                        contentDescription = strings.launchBubbleEnterContentDescription
                    }
                    .clickable(onClick = onBubbleTapped)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.launchEnterHint,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085)
            )
        }
    }
}

@Composable
private fun DesktopPetScreen(
    state: PetShellState,
    language: PetShellLanguage,
    strings: PetShellStrings,
    directPetLaunchEnabled: Boolean,
    onLanguageChange: (PetShellLanguage) -> Unit,
    onPetNavigate: (FeedDirection) -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenGenerate: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val selectedPet = state.approvedPets.selectedApprovedPet(state.approvedPetIndex)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D3430),
                        Color(0xFF123D39),
                        Color(0xFFF1F5F9)
                    )
                )
            )
            .semantics {
                contentDescription = strings.desktopPetModeContentDescription
            }
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 58.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.desktopPetModeTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = strings.desktopPetModeSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD7F3EE),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            DesktopPetModeStatusRail(
                strings = strings,
                directPetLaunchEnabled = directPetLaunchEnabled
            )
            DesktopPetStage(
                state = state,
                selectedPet = selectedPet,
                strings = strings
            )
            if (selectedPet == null) {
                DesktopPetRemoteSyncStrip(strings = strings)
            } else {
                ApprovedPetSignalStrip(
                    pet = selectedPet,
                    strings = strings
                )
                DesktopPetBrowseControls(
                    strings = strings,
                    onPetNavigate = onPetNavigate
                )
            }
            DesktopPetActionDock(
                strings = strings,
                onOpenCommunity = onOpenCommunity,
                onOpenGenerate = onOpenGenerate,
                onOpenProfile = onOpenProfile
            )
        }
        WalletPill(
            balance = state.walletBalance,
            strings = strings,
            modifier = Modifier.align(Alignment.TopStart)
        )
        LanguageToggle(
            language = language,
            strings = strings,
            onLanguageChange = onLanguageChange,
            modifier = Modifier.align(Alignment.TopEnd),
            compact = true
        )
    }
}

@Composable
private fun DesktopPetStage(
    state: PetShellState,
    selectedPet: ApprovedPet?,
    strings: PetShellStrings
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ApprovedPetPreviewArtwork(
                pet = selectedPet,
                action = state.petAction,
                strings = strings,
                modifier = Modifier.size(166.dp)
            )
            SpeechBubble(
                text = strings.speechBubble(state.speechBubble),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = strings.desktopPetReadyLine,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF344054),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DesktopPetBrowseControls(
    strings: PetShellStrings,
    onPetNavigate: (FeedDirection) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onPetNavigate(FeedDirection.Previous) },
            modifier = Modifier.weight(1f)
        ) {
            Text(strings.petPrev)
        }
        Button(
            onClick = { onPetNavigate(FeedDirection.Next) },
            modifier = Modifier.weight(1f)
        ) {
            Text(strings.petNext)
        }
    }
}

@Composable
private fun DesktopPetActionDock(
    strings: PetShellStrings,
    onOpenCommunity: () -> Unit,
    onOpenGenerate: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF101828),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF24314A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.desktopPetActionDockTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenCommunity,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.desktopPetOpenCommunity,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onOpenGenerate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.desktopPetOpenGenerate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onOpenProfile,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.desktopPetOpenProfile,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopPetModeStatusRail(
    strings: PetShellStrings,
    directPetLaunchEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DesktopPetStatusPill(
            label = strings.desktopPetRemoteStatus,
            accent = Color(0xFFACE4D9),
            modifier = Modifier.weight(1f)
        )
        DesktopPetStatusPill(
            label = strings.desktopPetReviewStatus,
            accent = Color(0xFF60A5FA),
            modifier = Modifier.weight(1f)
        )
        DesktopPetStatusPill(
            label = strings.directPetLaunchStatus(directPetLaunchEnabled),
            accent = Color(0xFFFFB86B),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DesktopPetStatusPill(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(38.dp),
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DesktopPetRemoteSyncStrip(strings: PetShellStrings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF7FBFA),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFACE4D9))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = 42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F766E))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = strings.desktopPetSyncTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D3430)
                )
                Text(
                    text = strings.desktopPetSyncDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475467),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CommunityScreen(
    state: PetShellState,
    selectedTab: PetShellTab,
    language: PetShellLanguage,
    strings: PetShellStrings,
    directPetLaunchEnabled: Boolean,
    desktopPetOverlayAutoShowEnabled: Boolean,
    desktopPetOverlayPermissionGranted: Boolean,
    desktopPetNotificationPermissionGranted: Boolean,
    desktopPetOverlayRunning: Boolean,
    onTabSelected: (PetShellTab) -> Unit,
    onLanguageChange: (PetShellLanguage) -> Unit,
    onDirectPetLaunchChange: (Boolean) -> Unit,
    onDesktopPetOverlayAutoShowChange: (Boolean) -> Unit,
    onRequestDesktopPetOverlayPermission: () -> Unit,
    onRequestDesktopPetNotificationPermission: () -> Unit,
    onStartDesktopPetOverlay: () -> Unit,
    onStopDesktopPetOverlay: () -> Unit,
    onResetDesktopPetOverlayPosition: () -> Unit,
    onEnterDesktopPet: () -> Unit,
    onNavigate: (FeedDirection) -> Unit,
    onShowcaseNavigate: (FeedDirection) -> Unit,
    onCheckIn: () -> Unit,
    generationContent: @Composable () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF1F5F9),
        bottomBar = {
            PetShellBottomNavigation(
                selectedTab = selectedTab,
                strings = strings,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CommunityHeader(
                state = state,
                selectedTab = selectedTab,
                language = language,
                strings = strings,
                onLanguageChange = onLanguageChange
            )
            when (selectedTab) {
                PetShellTab.Community -> CommunityHome(
                    state = state,
                    strings = strings,
                    onNavigate = onNavigate,
                    onShowcaseNavigate = onShowcaseNavigate,
                    onCheckIn = onCheckIn,
                    onCreatePet = { onTabSelected(PetShellTab.Generate) }
                )
                PetShellTab.Generate -> GenerationWorkspace(
                    state = state,
                    strings = strings,
                    generationContent = generationContent
                )
                PetShellTab.Profile -> ProfileWorkspace(
                    state = state,
                    strings = strings,
                    directPetLaunchEnabled = directPetLaunchEnabled,
                    desktopPetOverlayAutoShowEnabled = desktopPetOverlayAutoShowEnabled,
                    desktopPetOverlayPermissionGranted = desktopPetOverlayPermissionGranted,
                    desktopPetNotificationPermissionGranted = desktopPetNotificationPermissionGranted,
                    desktopPetOverlayRunning = desktopPetOverlayRunning,
                    onDirectPetLaunchChange = onDirectPetLaunchChange,
                    onDesktopPetOverlayAutoShowChange = onDesktopPetOverlayAutoShowChange,
                    onRequestDesktopPetOverlayPermission = onRequestDesktopPetOverlayPermission,
                    onRequestDesktopPetNotificationPermission = onRequestDesktopPetNotificationPermission,
                    onStartDesktopPetOverlay = onStartDesktopPetOverlay,
                    onStopDesktopPetOverlay = onStopDesktopPetOverlay,
                    onResetDesktopPetOverlayPosition = onResetDesktopPetOverlayPosition,
                    onCheckIn = onCheckIn,
                    onCreatePet = { onTabSelected(PetShellTab.Generate) },
                    onEnterDesktopPet = onEnterDesktopPet
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun PetShellBottomNavigation(
    selectedTab: PetShellTab,
    strings: PetShellStrings,
    onTabSelected: (PetShellTab) -> Unit
) {
    NavigationBar(containerColor = Color.White) {
        for (tab in PetShellTab.entries) {
            val selected = selectedTab == tab
            NavigationBarItem(
                modifier = Modifier.semantics {
                    contentDescription = tab.contentDescription(strings)
                },
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    PetShellTabIcon(tab = tab, selected = selected)
                },
                label = { Text(tab.label(strings)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0F766E),
                    selectedTextColor = Color(0xFF0F766E),
                    indicatorColor = Color(0xFFD7F3EE),
                    unselectedIconColor = Color(0xFF667085),
                    unselectedTextColor = Color(0xFF667085)
                )
            )
        }
    }
}

@Composable
private fun PetShellTabIcon(
    tab: PetShellTab,
    selected: Boolean
) {
    val ink = if (selected) Color(0xFF0F766E) else Color(0xFF667085)
    Surface(
        modifier = Modifier.size(width = 46.dp, height = 32.dp),
        color = if (selected) Color(0xFFD7F3EE) else Color(0xFFF2F4F7),
        contentColor = ink,
        shape = RoundedCornerShape(8.dp),
        border = if (selected) BorderStroke(1.dp, Color(0xFFACE4D9)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(24.dp)) {
                when (tab) {
                    PetShellTab.Community -> drawCommunityTabIcon(ink)
                    PetShellTab.Generate -> drawGenerateTabIcon(ink)
                    PetShellTab.Profile -> drawProfileTabIcon(ink)
                }
            }
        }
    }
}

@Composable
private fun CommunityHeader(
    state: PetShellState,
    selectedTab: PetShellTab,
    language: PetShellLanguage,
    strings: PetShellStrings,
    onLanguageChange: (PetShellLanguage) -> Unit
) {
    val backgroundSpec = petShellHeaderBackgroundSpec(selectedTab)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        backgroundSpec.startColor,
                        backgroundSpec.endColor
                    ),
                    start = Offset.Zero,
                    end = Offset(900f, 520f)
                )
            )
            .semantics {
                contentDescription = "gamer-immersive-header-backdrop"
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawImmersiveHeaderPattern(backgroundSpec)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = petShellTabHeaderTitle(selectedTab, strings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = backgroundSpec.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = petShellTabHeaderSubtitle(selectedTab, strings),
                    style = MaterialTheme.typography.bodySmall,
                    color = backgroundSpec.subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HeaderUtilityDock(
                balance = state.walletBalance,
                language = language,
                strings = strings,
                onLanguageChange = onLanguageChange
            )
        }
    }
}

@Composable
private fun HeaderUtilityDock(
    balance: Int,
    language: PetShellLanguage,
    strings: PetShellStrings,
    onLanguageChange: (PetShellLanguage) -> Unit
) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = strings.headerUtilityDockContentDescription
        },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        WalletPill(balance = balance, strings = strings)
        LanguageToggle(
            language = language,
            strings = strings,
            onLanguageChange = onLanguageChange,
            compact = true
        )
    }
}

@Composable
private fun CommunityHome(
    state: PetShellState,
    strings: PetShellStrings,
    onNavigate: (FeedDirection) -> Unit,
    onShowcaseNavigate: (FeedDirection) -> Unit,
    onCheckIn: () -> Unit,
    onCreatePet: () -> Unit
) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = strings.communityHomeContentDescription
        },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PetCompanionStrip(
            state = state,
            strings = strings,
            onNextPost = { onNavigate(FeedDirection.Next) },
            onShowcase = { onShowcaseNavigate(FeedDirection.Next) }
        )
        CommunityStatusSummary(
            state = state,
            strings = strings
        )
        CommunityChannelRail(strings = strings)
        CommunityQuickActions(
            state = state,
            strings = strings,
            onCheckIn = onCheckIn,
            onCreatePet = onCreatePet,
            onReview = { onNavigate(FeedDirection.Next) },
            onShowcase = { onShowcaseNavigate(FeedDirection.Next) }
        )
        ApprovedPetShowcaseBlock(
            state = state,
            strings = strings,
            onShowcaseNavigate = onShowcaseNavigate,
            onCreatePet = onCreatePet
        )
        FeedPostBlock(
            post = state.currentPost,
            strings = strings
        )
        FeedControls(
            strings = strings,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun CommunityStatusSummary(
    state: PetShellState,
    strings: PetShellStrings
) {
    val remoteSynced = state.speechBubble != "Local fallback active."
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.communityStatusSummaryContentDescription
            },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.communityStatusTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101828)
                )
                CommunityStatusPill(
                    label = if (remoteSynced) {
                        strings.communityStatusRemoteSynced
                    } else {
                        strings.communityStatusLocalFallback
                    },
                    accent = if (remoteSynced) Color(0xFF0F766E) else Color(0xFFF97316)
                )
                CommunityStatusPill(
                    label = strings.communityStatusHumanReview,
                    accent = Color(0xFF2F63D6)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CommunitySummaryToken(
                    label = strings.profileWalletSummaryTitle,
                    value = strings.walletBalance(state.walletBalance),
                    accent = Color(0xFF0F766E),
                    modifier = Modifier.weight(1f)
                )
                CommunitySummaryToken(
                    label = strings.dailyCheckIn,
                    value = if (state.checkInClaimed) strings.checkedIn else strings.quickActionCheckInDetail,
                    accent = Color(0xFFF97316),
                    modifier = Modifier.weight(1f)
                )
                CommunitySummaryToken(
                    label = strings.profileApprovedPetsMetric,
                    value = state.approvedPets.size.toString(),
                    accent = Color(0xFF2F63D6),
                    modifier = Modifier.weight(1f)
                )
                CommunitySummaryToken(
                    label = strings.quickActionReview,
                    value = strings.quickActionReviewStatus(state.pendingSubmissionCount),
                    accent = Color(0xFF475467),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CommunityStatusPill(
    label: String,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CommunitySummaryToken(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF667085),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CommunityChannelRail(strings: PetShellStrings) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.communityChannelRailContentDescription
            },
        color = Color(0xFF101828),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val channels = listOf(
                strings.communityChannelRecommended,
                strings.communityChannelCreations,
                strings.communityChannelGuides,
                strings.communityChannelEvents
            )
            channels.forEachIndexed { index, label ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    color = if (index == 0) Color(0xFFD7F3EE) else Color(0xFF1D2939),
                    shape = RoundedCornerShape(7.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (index == 0) Color(0xFF0D3430) else Color(0xFFEFF4F8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityQuickActions(
    state: PetShellState,
    strings: PetShellStrings,
    onCheckIn: () -> Unit,
    onCreatePet: () -> Unit,
    onReview: () -> Unit,
    onShowcase: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.communityQuickActionsContentDescription
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CommunityQuickActionTile(
            icon = CommunityQuickActionIcon.CheckIn,
            title = if (state.checkInClaimed) strings.checkedIn else strings.quickActionCheckIn,
            detail = strings.quickActionCheckInDetail,
            container = Color(0xFFFFF1D6),
            content = Color(0xFF7A3E00),
            enabled = !state.checkInClaimed,
            onClick = onCheckIn,
            modifier = Modifier.weight(1f)
        )
        CommunityQuickActionTile(
            icon = CommunityQuickActionIcon.Generate,
            title = strings.quickActionGenerate,
            detail = strings.quickActionGenerateDetail,
            container = Color(0xFFD7F3EE),
            content = Color(0xFF0D3430),
            onClick = onCreatePet,
            modifier = Modifier.weight(1f)
        )
        CommunityQuickActionTile(
            icon = CommunityQuickActionIcon.Review,
            title = strings.quickActionReview,
            detail = strings.quickActionReviewStatus(state.pendingSubmissionCount),
            container = Color(0xFFE7F0FF),
            content = Color(0xFF173B73),
            onClick = onReview,
            modifier = Modifier.weight(1f)
        )
        CommunityQuickActionTile(
            icon = CommunityQuickActionIcon.Showcase,
            title = strings.quickActionShowcase,
            detail = strings.quickActionShowcaseDetail,
            container = Color(0xFFFFE2C7),
            content = Color(0xFF6F2F00),
            onClick = onShowcase,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CommunityQuickActionTile(
    icon: CommunityQuickActionIcon,
    title: String,
    detail: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) container else Color(0xFFE4E7EC),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            QuickActionGlyph(
                icon = icon,
                color = if (enabled) content else Color(0xFF98A2B3)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) content else Color(0xFF98A2B3),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) content.copy(alpha = 0.72f) else Color(0xFF98A2B3),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionGlyph(
    icon: CommunityQuickActionIcon,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.46f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            drawQuickActionGlyph(icon = icon, color = color)
        }
    }
}

private fun DrawScope.drawQuickActionGlyph(
    icon: CommunityQuickActionIcon,
    color: Color
) {
    when (icon) {
        CommunityQuickActionIcon.CheckIn -> {
            drawCircle(
                color = color,
                radius = size.minDimension * 0.38f,
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                style = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.34f, size.height * 0.52f),
                end = Offset(size.width * 0.46f, size.height * 0.64f),
                strokeWidth = size.minDimension * 0.1f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.46f, size.height * 0.64f),
                end = Offset(size.width * 0.68f, size.height * 0.38f),
                strokeWidth = size.minDimension * 0.1f,
                cap = StrokeCap.Round
            )
        }
        CommunityQuickActionIcon.Generate -> {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            drawCircle(color = color, radius = size.minDimension * 0.12f, center = center)
            val rays = listOf(
                Offset(0f, -1f),
                Offset(1f, 0f),
                Offset(0f, 1f),
                Offset(-1f, 0f)
            )
            for (ray in rays) {
                drawLine(
                    color = color,
                    start = Offset(
                        center.x + ray.x * size.width * 0.24f,
                        center.y + ray.y * size.height * 0.24f
                    ),
                    end = Offset(
                        center.x + ray.x * size.width * 0.42f,
                        center.y + ray.y * size.height * 0.42f
                    ),
                    strokeWidth = size.minDimension * 0.1f,
                    cap = StrokeCap.Round
                )
            }
        }
        CommunityQuickActionIcon.Review -> {
            drawCircle(
                color = color,
                radius = size.minDimension * 0.26f,
                center = Offset(size.width * 0.42f, size.height * 0.42f),
                style = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.62f, size.height * 0.62f),
                end = Offset(size.width * 0.82f, size.height * 0.82f),
                strokeWidth = size.minDimension * 0.1f,
                cap = StrokeCap.Round
            )
        }
        CommunityQuickActionIcon.Showcase -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * 0.18f, size.height * 0.22f),
                size = Size(size.width * 0.64f, size.height * 0.52f),
                cornerRadius = CornerRadius(size.width * 0.08f, size.width * 0.08f),
                style = Stroke(width = size.minDimension * 0.1f)
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.32f, size.height * 0.82f),
                end = Offset(size.width * 0.68f, size.height * 0.82f),
                strokeWidth = size.minDimension * 0.1f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun GenerationWorkspace(
    state: PetShellState,
    strings: PetShellStrings,
    generationContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = strings.generationWorkspaceContentDescription
        },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0D3430),
                            Color(0xFF0F766E),
                            Color(0xFFFFA24D)
                        )
                    )
                )
                .semantics {
                    contentDescription = strings.generationStudioHeroContentDescription
                }
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.padding(4.dp)) {
                            PetArtworkBadge(
                                action = state.petAction,
                                modifier = Modifier
                                    .size(60.dp)
                                    .semantics {
                                        contentDescription = strings.petAvatarContentDescription
                                    }
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = strings.generationStudioHeroTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = strings.generationStudioHeroSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        SpeechBubble(text = strings.speechBubble(state.speechBubble))
                    }
                }
                GenerationSafetyStrip(strings = strings)
            }
        }
        generationContent()
    }
}

@Composable
private fun GenerationSafetyStrip(strings: PetShellStrings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GenerationSafetyStep(
            label = strings.generationFlowBriefStep,
            accent = Color(0xFFACE4D9),
            modifier = Modifier.weight(1f)
        )
        GenerationSafetyStep(
            label = strings.generationFlowReviewStep,
            accent = Color(0xFF60A5FA),
            modifier = Modifier.weight(1f)
        )
        GenerationSafetyStep(
            label = strings.generationFlowPackageStep,
            accent = Color(0xFFFFB86B),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GenerationSafetyStep(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(34.dp),
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileWorkspace(
    state: PetShellState,
    strings: PetShellStrings,
    directPetLaunchEnabled: Boolean,
    desktopPetOverlayAutoShowEnabled: Boolean,
    desktopPetOverlayPermissionGranted: Boolean,
    desktopPetNotificationPermissionGranted: Boolean,
    desktopPetOverlayRunning: Boolean,
    onDirectPetLaunchChange: (Boolean) -> Unit,
    onDesktopPetOverlayAutoShowChange: (Boolean) -> Unit,
    onRequestDesktopPetOverlayPermission: () -> Unit,
    onRequestDesktopPetNotificationPermission: () -> Unit,
    onStartDesktopPetOverlay: () -> Unit,
    onStopDesktopPetOverlay: () -> Unit,
    onResetDesktopPetOverlayPosition: () -> Unit,
    onCheckIn: () -> Unit,
    onCreatePet: () -> Unit,
    onEnterDesktopPet: () -> Unit
) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = strings.profileWorkspaceContentDescription
        },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfileKeeperHero(
            state = state,
            strings = strings
        )
        ProfileWalletSummary(
            state = state,
            strings = strings
        )
        ProfileDesktopPetSettings(
            strings = strings,
            directPetLaunchEnabled = directPetLaunchEnabled,
            desktopPetOverlayAutoShowEnabled = desktopPetOverlayAutoShowEnabled,
            desktopPetOverlayPermissionGranted = desktopPetOverlayPermissionGranted,
            desktopPetNotificationPermissionGranted = desktopPetNotificationPermissionGranted,
            desktopPetOverlayRunning = desktopPetOverlayRunning,
            onDirectPetLaunchChange = onDirectPetLaunchChange,
            onDesktopPetOverlayAutoShowChange = onDesktopPetOverlayAutoShowChange,
            onRequestDesktopPetOverlayPermission = onRequestDesktopPetOverlayPermission,
            onRequestDesktopPetNotificationPermission = onRequestDesktopPetNotificationPermission,
            onStartDesktopPetOverlay = onStartDesktopPetOverlay,
            onStopDesktopPetOverlay = onStopDesktopPetOverlay,
            onResetDesktopPetOverlayPosition = onResetDesktopPetOverlayPosition,
            onEnterDesktopPet = onEnterDesktopPet
        )
        ProfilePetShelf(
            state = state,
            strings = strings,
            onCreatePet = onCreatePet
        )
        ProfileActionDock(
            state = state,
            strings = strings,
            onCheckIn = onCheckIn,
            onCreatePet = onCreatePet
        )
    }
}

@Composable
private fun ProfileKeeperHero(
    state: PetShellState,
    strings: PetShellStrings
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF101828),
                        Color(0xFF0F766E),
                        Color(0xFFFFA24D)
                    )
                )
            )
            .semantics {
                contentDescription = strings.profileKeeperHeroContentDescription
            }
            .padding(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.padding(6.dp)) {
                    PetArtworkBadge(
                        action = state.petAction,
                        modifier = Modifier.size(86.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = strings.profileKeeperName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.profileKeeperRole,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CompactSpeechBubble(text = strings.profileWorkspaceSubtitle)
            }
        }
    }
}

@Composable
private fun ProfileWalletSummary(
    state: PetShellState,
    strings: PetShellStrings
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.profileWalletSummaryContentDescription
            },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.profileWalletSummaryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                WalletPill(balance = state.walletBalance, strings = strings)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMetricToken(
                    label = strings.profileWalletSummaryTitle,
                    value = strings.walletBalance(state.walletBalance),
                    accent = Color(0xFF0F766E),
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricToken(
                    label = strings.profileApprovedPetsMetric,
                    value = state.approvedPets.size.toString(),
                    accent = Color(0xFF2F63D6),
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricToken(
                    label = strings.quickActionCheckIn,
                    value = if (state.checkInClaimed) strings.checkedIn else strings.quickActionCheckInDetail,
                    accent = Color(0xFFF97316),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMetricToken(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.11f))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(width = 26.dp, height = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085),
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101828),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ProfilePetShelf(
    state: PetShellState,
    strings: PetShellStrings,
    onCreatePet: () -> Unit
) {
    val hasApprovedPets = state.approvedPets.isNotEmpty()
    val selectedPet = state.approvedPets.selectedApprovedPet(state.approvedPetIndex)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.profilePetShelfContentDescription
            },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ApprovedPetPreviewArtwork(
                    pet = selectedPet,
                    action = state.petAction,
                    strings = strings,
                    modifier = Modifier.size(82.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = strings.profilePetShelfTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.approvedPetRegistrySummary(state.approvedPets),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.approvedPetShowcaseTitle(
                            pets = state.approvedPets,
                            selectedIndex = state.approvedPetIndex
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475467)
                    )
                    Text(
                        text = strings.approvedPetShowcaseDetail(
                            pets = state.approvedPets,
                            selectedIndex = state.approvedPetIndex
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF667085)
                    )
                }
            }
            if (hasApprovedPets) {
                ApprovedPetSignalStrip(
                    pet = selectedPet,
                    strings = strings
                )
            }
            if (!hasApprovedPets) {
                ShowcaseEmptyPath(strings = strings)
                Button(
                    onClick = onCreatePet,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.profileCreatePetAction)
                }
            }
        }
    }
}

@Composable
private fun ProfileActionDock(
    state: PetShellState,
    strings: PetShellStrings,
    onCheckIn: () -> Unit,
    onCreatePet: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.profileActionDockContentDescription
            },
        color = Color(0xFF101828),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF24314A))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.profileActionDockTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCheckIn,
                    modifier = Modifier.weight(1f),
                    enabled = !state.checkInClaimed
                ) {
                    Text(if (state.checkInClaimed) strings.checkedIn else strings.dailyCheckIn)
                }
                Button(
                    onClick = onCreatePet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.profileCreatePetAction)
                }
            }
        }
    }
}

@Composable
private fun ProfileDesktopPetSettings(
    strings: PetShellStrings,
    directPetLaunchEnabled: Boolean,
    desktopPetOverlayAutoShowEnabled: Boolean,
    desktopPetOverlayPermissionGranted: Boolean,
    desktopPetNotificationPermissionGranted: Boolean,
    desktopPetOverlayRunning: Boolean,
    onDirectPetLaunchChange: (Boolean) -> Unit,
    onDesktopPetOverlayAutoShowChange: (Boolean) -> Unit,
    onRequestDesktopPetOverlayPermission: () -> Unit,
    onRequestDesktopPetNotificationPermission: () -> Unit,
    onStartDesktopPetOverlay: () -> Unit,
    onStopDesktopPetOverlay: () -> Unit,
    onResetDesktopPetOverlayPosition: () -> Unit,
    onEnterDesktopPet: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.desktopPetSettingsTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828)
            )
            DirectPetLaunchSetting(
                strings = strings,
                enabled = directPetLaunchEnabled,
                onEnabledChange = onDirectPetLaunchChange
            )
            SystemDesktopPetSetting(
                strings = strings,
                autoShowEnabled = desktopPetOverlayAutoShowEnabled,
                permissionGranted = desktopPetOverlayPermissionGranted,
                notificationPermissionGranted = desktopPetNotificationPermissionGranted,
                overlayRunning = desktopPetOverlayRunning,
                onAutoShowChange = onDesktopPetOverlayAutoShowChange,
                onRequestPermission = onRequestDesktopPetOverlayPermission,
                onRequestNotificationPermission = onRequestDesktopPetNotificationPermission,
                onStartOverlay = onStartDesktopPetOverlay,
                onStopOverlay = onStopDesktopPetOverlay,
                onResetPosition = onResetDesktopPetOverlayPosition
            )
            Button(
                onClick = onEnterDesktopPet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.enterDesktopPetMode)
            }
        }
    }
}

@Composable
private fun SystemDesktopPetSetting(
    strings: PetShellStrings,
    autoShowEnabled: Boolean,
    permissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    overlayRunning: Boolean,
    onAutoShowChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onResetPosition: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE4E7EC))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings.systemDesktopPetTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828)
            )
                Text(
                    text = strings.systemDesktopPetDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DesktopPetSettingPill(
                    label = if (permissionGranted) {
                        strings.desktopPetOverlayPermissionGranted
                    } else {
                        strings.desktopPetOverlayPermissionMissing
                    },
                    accent = if (permissionGranted) Color(0xFF0F766E) else Color(0xFFB42318),
                    modifier = Modifier.weight(1f)
                )
                DesktopPetSettingPill(
                    label = if (notificationPermissionGranted) {
                        strings.desktopPetNotificationPermissionGranted
                    } else {
                        strings.desktopPetNotificationPermissionMissing
                    },
                    accent = if (notificationPermissionGranted) Color(0xFF0F766E) else Color(0xFFB42318),
                    modifier = Modifier.weight(1f)
                )
            }
            DesktopPetSettingPill(
                label = if (overlayRunning) {
                    strings.desktopPetOverlayRunning
                } else {
                    strings.desktopPetOverlayStopped
                },
                accent = if (overlayRunning) Color(0xFFF97316) else Color(0xFF667085),
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = strings.desktopPetOverlayToggleContentDescription
                    },
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE4E7EC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.systemDesktopPetAutoShowTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF101828)
                        )
                        Text(
                            text = strings.systemDesktopPetAutoShowDetail,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF667085),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        modifier = Modifier.semantics {
                            contentDescription = strings.desktopPetOverlayToggleContentDescription
                        },
                        checked = autoShowEnabled,
                        onCheckedChange = onAutoShowChange
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.desktopPetOverlayPermissionContentDescription
                        }
                ) {
                    Text(
                        if (permissionGranted) {
                            strings.desktopPetOverlayManagePermission
                        } else {
                            strings.desktopPetOverlayRequestPermission
                        }
                    )
                }
                TextButton(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.desktopPetNotificationPermissionContentDescription
                        }
                ) {
                    Text(
                        if (notificationPermissionGranted) {
                            strings.desktopPetNotificationManagePermission
                        } else {
                            strings.desktopPetNotificationRequestPermission
                        }
                    )
                }
            }
            TextButton(
                onClick = onResetPosition,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = strings.desktopPetOverlayResetPositionContentDescription
                    }
            ) {
                Text(strings.desktopPetOverlayResetPosition)
            }
            Button(
                onClick = if (overlayRunning) onStopOverlay else onStartOverlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (overlayRunning) {
                            strings.desktopPetOverlayStopContentDescription
                        } else {
                            strings.desktopPetOverlayStartContentDescription
                        }
                    }
            ) {
                Text(
                    if (overlayRunning) {
                        strings.desktopPetOverlayHide
                    } else {
                        strings.desktopPetOverlayShow
                    }
                )
            }
        }
    }
}

@Composable
private fun DesktopPetSettingPill(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(32.dp),
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DirectPetLaunchSetting(
    strings: PetShellStrings,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.directPetLaunchToggleContentDescription
            },
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE4E7EC))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = strings.directPetLaunchTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101828)
                )
                Text(
                    text = strings.directPetLaunchDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                modifier = Modifier.semantics {
                    contentDescription = strings.directPetLaunchToggleContentDescription
                },
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

@Composable
private fun PetCompanionStrip(
    state: PetShellState,
    strings: PetShellStrings,
    onNextPost: () -> Unit,
    onShowcase: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D3430),
                        Color(0xFF0F766E),
                        Color(0xFFFFA24D)
                    )
                )
            )
            .semantics {
                contentDescription = strings.communityPetCompanionStripContentDescription
            }
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        PetAvatar(action = state.petAction, strings = strings)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.communityPetCommandTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = strings.communityPetCommandDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE8FFFA),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    SpeechBubble(text = strings.speechBubble(state.speechBubble))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommunityCommandButton(
                    label = strings.communityPetCommandNextPost,
                    container = Color.White,
                    content = Color(0xFF0D3430),
                    onClick = onNextPost,
                    modifier = Modifier.weight(1f)
                )
                CommunityCommandButton(
                    label = strings.communityPetCommandShowcase,
                    container = Color(0xFFFFE2C7),
                    content = Color(0xFF6F2F00),
                    onClick = onShowcase,
                    modifier = Modifier.weight(1f)
                )
            }
            CommunityCommandStatus(
                text = strings.communityCommandStatus(
                    approvedPetCount = state.approvedPets.size,
                    pendingSubmissionCount = state.pendingSubmissionCount,
                    checkInClaimed = state.checkInClaimed
                )
            )
        }
    }
}

@Composable
private fun CommunityCommandStatus(
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE8FFFA),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CommunityCommandButton(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = container,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ApprovedPetShowcaseBlock(
    state: PetShellState,
    strings: PetShellStrings,
    onShowcaseNavigate: (FeedDirection) -> Unit,
    onCreatePet: () -> Unit
) {
    val hasApprovedPets = state.approvedPets.isNotEmpty()
    val selectedPet = state.approvedPets.selectedApprovedPet(state.approvedPetIndex)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.communityShowcasePanelContentDescription
            },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ApprovedPetPreviewArtwork(
                    pet = selectedPet,
                    action = state.petAction,
                    strings = strings,
                    modifier = Modifier.size(82.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = strings.approvedPetShowcaseTitle(
                            pets = state.approvedPets,
                            selectedIndex = state.approvedPetIndex
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.approvedPetShowcaseDetail(
                            pets = state.approvedPets,
                            selectedIndex = state.approvedPetIndex
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085)
                    )
                    MetadataPill(
                        label = strings.approvedPetShowcasePosition(
                            pets = state.approvedPets,
                            selectedIndex = state.approvedPetIndex
                        )
                    )
                }
            }
            if (hasApprovedPets) {
                ApprovedPetSignalStrip(
                    pet = selectedPet,
                    strings = strings
                )
            }
            if (!hasApprovedPets) {
                ShowcaseEmptyPath(strings = strings)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasApprovedPets) {
                    Button(
                        onClick = { onShowcaseNavigate(FeedDirection.Previous) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.petPrev)
                    }
                    Button(
                        onClick = { onShowcaseNavigate(FeedDirection.Next) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.petNext)
                    }
                } else {
                    Button(
                        onClick = onCreatePet,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.profileCreatePetAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApprovedPetSignalStrip(
    pet: ApprovedPet?,
    strings: PetShellStrings
) {
    val previewReady = pet?.let { approvedPetPreviewUrl(it).isNotBlank() } == true
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE4E7EC))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ApprovedPetSignalToken(
                    label = strings.approvedPetScoreMetric,
                    value = pet?.totalScore?.toString() ?: "-",
                    accent = Color(0xFFF97316),
                    modifier = Modifier.weight(1f)
                )
                ApprovedPetSignalToken(
                    label = strings.approvedPetMotionMetric,
                    value = pet?.motionSheetCount?.toString() ?: "-",
                    accent = Color(0xFF2F63D6),
                    modifier = Modifier.weight(1f)
                )
                ApprovedPetSignalToken(
                    label = strings.approvedPetPreviewMetric,
                    value = if (previewReady) {
                        strings.approvedPetPreviewReady
                    } else {
                        strings.approvedPetPreviewPending
                    },
                    accent = if (previewReady) Color(0xFF0F766E) else Color(0xFF667085),
                    modifier = Modifier.weight(1f)
                )
            }
            if (pet != null) {
                Text(
                    text = strings.approvedPetSourceLine(pet),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ApprovedPetSignalToken(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF667085),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShowcaseEmptyPath(strings: PetShellStrings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ShowcasePathStep(
            label = strings.showcasePathGenerate,
            accent = Color(0xFF0F766E),
            modifier = Modifier.weight(1f)
        )
        ShowcasePathStep(
            label = strings.showcasePathReview,
            accent = Color(0xFF2F63D6),
            modifier = Modifier.weight(1f)
        )
        ShowcasePathStep(
            label = strings.showcasePathPublish,
            accent = Color(0xFFF97316),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShowcasePathStep(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(42.dp),
        color = accent.copy(alpha = 0.11f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent)
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeedPostBlock(
    post: FeedPost,
    strings: PetShellStrings
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.communityPostCardContentDescription
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE4E7EC))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PetArtworkBadge(
                        action = PetAction.Review,
                        modifier = Modifier.size(52.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF101828),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = post.petId,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF667085),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.communityFeedSignalTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    FeedReactionPill(
                        count = post.reactionCount,
                        label = strings.feedReactionLabel
                    )
                }
            }
            val metadataLabels = feedPostMetadataLabels(post)
            if (metadataLabels.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (label in metadataLabels.take(2)) {
                        MetadataPill(label = strings.feedMetadataLabel(label))
                    }
                }
            }
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF344054),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val auditLabels = feedPostAuditLabels(post)
            if (auditLabels.isNotEmpty()) {
                FeedArtifactSummary(labels = auditLabels, strings = strings)
            }
            FeedPerspectiveHint(strings = strings)
        }
    }
}

@Composable
private fun FeedReactionPill(
    count: Int,
    label: String
) {
    Surface(
        color = Color(0xFFE7F8F2),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                drawCircle(
                    color = Color(0xFF0F766E),
                    radius = size.minDimension * 0.28f,
                    center = Offset(size.width * 0.5f, size.height * 0.42f)
                )
                drawRoundRect(
                    color = Color(0xFF0F766E),
                    topLeft = Offset(size.width * 0.27f, size.height * 0.62f),
                    size = Size(size.width * 0.46f, size.height * 0.18f),
                    cornerRadius = CornerRadius(size.width * 0.08f, size.width * 0.08f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E),
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0F766E),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FeedPerspectiveHint(strings: PetShellStrings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF101828),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFA24D))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = strings.communityFeedSignalTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = strings.communityFeedSignalDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.76f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FeedArtifactSummary(
    labels: List<String>,
    strings: PetShellStrings
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE4E7EC))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (label in labels.take(3)) {
                Text(
                    text = strings.feedMetadataLabel(label),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (labels.size > 3) {
                Text(
                    text = "+${labels.size - 3}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
            }
        }
    }
}

@Composable
private fun FeedControls(
    strings: PetShellStrings,
    onNavigate: (FeedDirection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.communityFeedControlsContentDescription
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeedControlButton(
            glyph = FeedControlGlyph.Previous,
            label = strings.prev,
            onClick = { onNavigate(FeedDirection.Previous) },
            modifier = Modifier.weight(1f)
        )
        FeedControlButton(
            glyph = FeedControlGlyph.Next,
            label = strings.next,
            onClick = { onNavigate(FeedDirection.Next) },
            modifier = Modifier.weight(1f)
        )
        FeedControlButton(
            glyph = FeedControlGlyph.Skip,
            label = strings.skip,
            onClick = { onNavigate(FeedDirection.Skip) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeedControlButton(
    glyph: FeedControlGlyph,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                drawFeedControlGlyph(glyph = glyph, color = Color.White)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun DrawScope.drawFeedControlGlyph(
    glyph: FeedControlGlyph,
    color: Color
) {
    when (glyph) {
        FeedControlGlyph.Previous -> {
            drawChevron(color = color, direction = -1f, xOffset = 0.56f)
        }
        FeedControlGlyph.Next -> {
            drawChevron(color = color, direction = 1f, xOffset = 0.44f)
        }
        FeedControlGlyph.Skip -> {
            drawChevron(color = color, direction = 1f, xOffset = 0.34f)
            drawChevron(color = color, direction = 1f, xOffset = 0.66f)
        }
    }
}

private fun DrawScope.drawChevron(
    color: Color,
    direction: Float,
    xOffset: Float
) {
    val centerX = size.width * xOffset
    val top = Offset(centerX - direction * size.width * 0.18f, size.height * 0.26f)
    val middle = Offset(centerX + direction * size.width * 0.18f, size.height * 0.5f)
    val bottom = Offset(centerX - direction * size.width * 0.18f, size.height * 0.74f)
    drawLine(
        color = color,
        start = top,
        end = middle,
        strokeWidth = size.minDimension * 0.12f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = middle,
        end = bottom,
        strokeWidth = size.minDimension * 0.12f,
        cap = StrokeCap.Round
    )
}

@Composable
private fun GenerationStudioStatusDock(
    strings: PetShellStrings,
    job: PetGenerationJobResponseDto?,
    candidateCount: Int,
    selectedCandidateDownloadId: String
) {
    val statusText = job
        ?.let { strings.progressStatus(effectiveProgressStatus(it)) }
        ?: strings.generationMessage("Waiting")
    val downloadText = if (job?.let { canShowPackageDownload(it) } == true) {
        strings.generationFlowPackageStep
    } else {
        strings.generationMessage("Waiting")
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.generationStudioStatusDockContentDescription
            },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GenerationStatusTile(
                label = strings.generationFlowBriefStep,
                value = statusText,
                container = Color(0xFFEAF5F0),
                content = Color(0xFF0D3430),
                modifier = Modifier.weight(1f)
            )
            GenerationStatusTile(
                label = strings.generationFlowCandidateStep,
                value = candidateCount.toString(),
                container = Color(0xFFE7F0FF),
                content = Color(0xFF173B73),
                modifier = Modifier.weight(1f)
            )
            GenerationStatusTile(
                label = strings.generationFlowReviewStep,
                value = if (selectedCandidateDownloadId.isBlank()) {
                    strings.generationMessage("Waiting")
                } else {
                    "1"
                },
                container = Color(0xFFFFF7ED),
                content = Color(0xFF7A3B07),
                modifier = Modifier.weight(1f)
            )
            GenerationStatusTile(
                label = strings.generationFlowPackageStep,
                value = downloadText,
                container = Color(0xFFF2F4F7),
                content = Color(0xFF344054),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GenerationStatusTile(
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(62.dp),
        color = container,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = content,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

private fun PetShellTab.label(strings: PetShellStrings): String =
    when (this) {
        PetShellTab.Community -> strings.communityTabLabel
        PetShellTab.Generate -> strings.generateTabLabel
        PetShellTab.Profile -> strings.profileTabLabel
    }

private fun PetShellTab.contentDescription(strings: PetShellStrings): String =
    when (this) {
        PetShellTab.Community -> strings.communityTabContentDescription
        PetShellTab.Generate -> strings.generateTabContentDescription
        PetShellTab.Profile -> strings.profileTabContentDescription
    }

private fun PetShellTab.iconLabel(strings: PetShellStrings): String =
    when (this) {
        PetShellTab.Community -> strings.communityTabIconLabel
        PetShellTab.Generate -> strings.generateTabIconLabel
        PetShellTab.Profile -> strings.profileTabIconLabel
    }

internal fun petShellTabHeaderTitle(tab: PetShellTab, strings: PetShellStrings): String =
    when (tab) {
        PetShellTab.Community -> strings.communityTitle
        PetShellTab.Generate -> strings.generationWorkspaceTitle
        PetShellTab.Profile -> strings.profileWorkspaceTitle
    }

internal fun petShellTabHeaderSubtitle(tab: PetShellTab, strings: PetShellStrings): String =
    when (tab) {
        PetShellTab.Community -> strings.communitySubtitle
        PetShellTab.Generate -> strings.generationWorkspaceSubtitle
        PetShellTab.Profile -> strings.profileWorkspaceSubtitle
    }

internal fun petShellHeaderBackgroundSpec(tab: PetShellTab): PetShellHeaderBackgroundSpec =
    when (tab) {
        PetShellTab.Community -> PetShellHeaderBackgroundSpec(
            startColor = Color(0xFF0F766E),
            endColor = Color(0xFF173B73),
            accentColor = Color(0xFFF97316),
            titleColor = Color.White,
            subtitleColor = Color(0xFFD7F3EE)
        )
        PetShellTab.Generate -> PetShellHeaderBackgroundSpec(
            startColor = Color(0xFF173B73),
            endColor = Color(0xFF0D3430),
            accentColor = Color(0xFF60A5FA),
            titleColor = Color.White,
            subtitleColor = Color(0xFFE7F0FF)
        )
        PetShellTab.Profile -> PetShellHeaderBackgroundSpec(
            startColor = Color(0xFF7A3E00),
            endColor = Color(0xFF0F766E),
            accentColor = Color(0xFFFFB86B),
            titleColor = Color.White,
            subtitleColor = Color(0xFFFFF1D6)
        )
    }

@Composable
private fun PetArtworkBadge(
    action: PetAction,
    modifier: Modifier = Modifier
) {
    val palette = petAvatarPalette(action)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDesktopPetMascot(
                palette = palette,
                action = action
            )
        }
    }
}

private fun DrawScope.drawImmersiveHeaderPattern(spec: PetShellHeaderBackgroundSpec) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.16f)
            )
        )
    )
    drawCircle(
        color = spec.accentColor.copy(alpha = 0.24f),
        radius = size.minDimension * 0.64f,
        center = Offset(size.width * 0.92f, size.height * 0.12f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        radius = size.minDimension * 0.42f,
        center = Offset(size.width * 0.08f, size.height * 0.92f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.13f),
        topLeft = Offset(size.width * 0.06f, size.height * 0.70f),
        size = Size(size.width * 0.88f, size.height * 0.16f),
        cornerRadius = CornerRadius(size.height * 0.08f, size.height * 0.08f)
    )
    drawPath(
        path = Path().apply {
            moveTo(size.width * 0.18f, size.height * 1.02f)
            lineTo(size.width * 0.56f, size.height * 0.46f)
            lineTo(size.width * 0.78f, size.height * 1.02f)
            close()
        },
        color = Color.White.copy(alpha = 0.08f)
    )
    drawPath(
        path = Path().apply {
            moveTo(size.width * 0.04f, size.height * 0.72f)
            cubicTo(
                size.width * 0.26f,
                size.height * 0.30f,
                size.width * 0.52f,
                size.height * 1.03f,
                size.width * 0.94f,
                size.height * 0.36f
            )
        },
        color = Color.White.copy(alpha = 0.28f),
        style = Stroke(width = size.height * 0.04f, cap = StrokeCap.Round)
    )
    drawPath(
        path = Path().apply {
            moveTo(size.width * 0.54f, size.height * 0.18f)
            cubicTo(
                size.width * 0.64f,
                size.height * 0.46f,
                size.width * 0.86f,
                size.height * 0.38f,
                size.width,
                size.height * 0.66f
            )
        },
        color = spec.accentColor.copy(alpha = 0.26f),
        style = Stroke(width = size.height * 0.022f, cap = StrokeCap.Round)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f),
        topLeft = Offset(size.width * 0.78f, size.height * 0.30f),
        size = Size(size.width * 0.10f, size.height * 0.10f),
        cornerRadius = CornerRadius(size.height * 0.025f, size.height * 0.025f)
    )
    drawRoundRect(
        color = spec.accentColor.copy(alpha = 0.42f),
        topLeft = Offset(size.width * 0.83f, size.height * 0.47f),
        size = Size(size.width * 0.07f, size.height * 0.07f),
        cornerRadius = CornerRadius(size.height * 0.02f, size.height * 0.02f)
    )
    drawCircle(
        color = spec.accentColor.copy(alpha = 0.72f),
        radius = size.minDimension * 0.035f,
        center = Offset(size.width * 0.70f, size.height * 0.72f)
    )
}

private fun DrawScope.drawCommunityTabIcon(color: Color) {
    val roof = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.48f)
        lineTo(size.width * 0.5f, size.height * 0.28f)
        lineTo(size.width * 0.75f, size.height * 0.48f)
    }
    drawPath(
        path = roof,
        color = color,
        style = Stroke(width = size.width * 0.08f, cap = StrokeCap.Round)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.32f, size.height * 0.48f),
        size = Size(size.width * 0.36f, size.height * 0.28f),
        cornerRadius = CornerRadius(size.width * 0.05f, size.width * 0.05f),
        style = Stroke(width = size.width * 0.07f)
    )
}

private fun DrawScope.drawGenerateTabIcon(color: Color) {
    val center = Offset(size.width * 0.5f, size.height * 0.5f)
    drawCircle(color = color, radius = size.width * 0.13f, center = center)
    val rays = listOf(
        Offset(0f, -1f),
        Offset(0.82f, -0.58f),
        Offset(0.92f, 0.38f),
        Offset(0f, 1f),
        Offset(-0.92f, 0.38f),
        Offset(-0.82f, -0.58f)
    )
    for (ray in rays) {
        drawLine(
            color = color,
            start = Offset(
                center.x + ray.x * size.width * 0.23f,
                center.y + ray.y * size.height * 0.23f
            ),
            end = Offset(
                center.x + ray.x * size.width * 0.35f,
                center.y + ray.y * size.height * 0.35f
            ),
            strokeWidth = size.width * 0.07f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawProfileTabIcon(color: Color) {
    drawCircle(
        color = color,
        radius = size.width * 0.13f,
        center = Offset(size.width * 0.5f, size.height * 0.4f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.3f, size.height * 0.58f),
        size = Size(size.width * 0.4f, size.height * 0.18f),
        cornerRadius = CornerRadius(size.width * 0.12f, size.width * 0.12f)
    )
}

@Composable
private fun GenerationPanel(
    strings: PetShellStrings,
    description: String,
    onDescriptionChange: (String) -> Unit,
    appJobId: String,
    onAppJobIdChange: (String) -> Unit,
    bodyShape: String,
    onBodyShapeChange: (String) -> Unit,
    references: String,
    onReferencesChange: (String) -> Unit,
    recentAppJobIds: List<String>,
    onRemoveRecentJob: (String) -> Unit,
    onResumeRecentJob: (String) -> Unit,
    job: PetGenerationJobResponseDto?,
    message: String,
    packageDownloadMessage: String,
    packageImportCandidateMessage: String,
    packageImportSubmissionMessage: String,
    workerReadinessMessage: String,
    candidates: List<CandidateGalleryItem>,
    progressSteps: List<GenerationProgressStepItem>,
    selectedCandidateDownloadId: String,
    onSelectCandidate: (String) -> Unit,
    reviewNotes: String,
    onReviewNotesChange: (String) -> Unit,
    reviewNoteSuggestions: List<String>,
    onAppendReviewNoteSuggestion: (String) -> Unit,
    canClearJob: Boolean,
    onCreateJob: () -> Unit,
    onClearJob: () -> Unit,
    onCheckWorkerReadiness: () -> Unit,
    onReviewDecision: (String) -> Unit,
    onPollJob: () -> Unit,
    onDownloadPackage: () -> Unit,
    canSubmitPackageImportDraft: Boolean,
    onSubmitPackageImport: () -> Unit,
    canRefreshPackageImportSubmission: Boolean,
    onRefreshPackageImportSubmission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = strings.generationBriefPanelContentDescription
                },
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
            Text(
                text = strings.generationBriefPanelTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.generationPublicApiBoundaryNotice,
                modifier = Modifier.semantics {
                    contentDescription = strings.generationPublicApiBoundaryContentDescription
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF667085)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .semantics {
                        contentDescription = strings.generationPromptCanvasContentDescription
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
            GenerationBriefStageHeader(
                title = strings.generationPromptStageTitle,
                detail = strings.generationPromptStageHint,
                accent = Color(0xFF0F766E),
                actionLabel = strings.generationPromptIdeaAction,
                actionContentDescription = strings.generationPromptIdeaContentDescription,
                onAction = {
                    onDescriptionChange(strings.generationPromptIdeaText)
                }
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generation-description")
                    .semantics {
                        contentDescription = strings.generationDescriptionContentDescription
                },
                label = { Text(strings.descriptionLabel + strings.requiredFieldSuffix) },
                minLines = 1,
                maxLines = 2
            )
            GenerationBriefStageHeader(
                title = strings.generationTaskStageTitle,
                detail = strings.generationTaskStageHint,
                accent = Color(0xFF2F63D6)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = appJobId,
                    onValueChange = onAppJobIdChange,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.appJobIdContentDescription
                        },
                    label = { Text(strings.appJobIdLabel) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = references,
                    onValueChange = onReferencesChange,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.referenceUrlsContentDescription
                        },
                    label = { Text(strings.referenceUrlsLabel) },
                    minLines = 1
                )
            }
            GenerationBriefStageHeader(
                title = strings.generationBodyStageTitle,
                detail = strings.generationBodyStageHint,
                accent = Color(0xFFF97316)
            )
            BodyShapeSegmentedControl(
                selectedBodyShape = bodyShape,
                strings = strings,
                onBodyShapeChange = onBodyShapeChange
            )
            val createValidationMessage = generationCreateValidationMessage(
                description = description,
                bodyShape = bodyShape,
                referencesText = references,
                appJobId = appJobId
            )
            if (createValidationMessage.isNotBlank() && description.isNotBlank()) {
                GenerationInlineNotice(
                    text = strings.generationMessage(createValidationMessage),
                    accent = Color(0xFFB42318),
                    container = Color(0xFFFFF3F0)
                )
            }
            Button(
                onClick = onCreateJob,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generation-create"),
                enabled = canCreateGenerationJob(
                    description = description,
                    bodyShape = bodyShape,
                    referencesText = references,
                    appJobId = appJobId
                )
            ) {
                Text(strings.createGenerationJob)
            }
            GenerationBriefStageHeader(
                title = strings.generationRunStageTitle,
                detail = strings.generationRunStageHint,
                accent = Color(0xFF475467)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCheckWorkerReadiness,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.checkGenerationServiceContentDescription
                        }
                ) {
                    Text(
                        text = strings.checkGenerationService,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = onPollJob,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.pollJobContentDescription
                        },
                    enabled = canPollGenerationJob(appJobId)
                ) {
                    Text(
                        text = strings.pollJob,
                        textAlign = TextAlign.Center
                    )
                }
            }
            val pollValidationMessage = pollGenerationJobValidationMessage(appJobId)
            if (appJobId.isNotBlank() && pollValidationMessage.isNotBlank()) {
                Text(
                    text = strings.generationMessage(pollValidationMessage),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB42318)
                )
            }
            if (recentAppJobIds.isNotEmpty()) {
                Text(
                    text = strings.recentJobs,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (recentAppJobId in recentAppJobIds) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recentAppJobId,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475467)
                            )
                            TextButton(
                                onClick = { onResumeRecentJob(recentAppJobId) },
                                enabled = appJobId.trim() != recentAppJobId
                            ) {
                                Text(strings.resume)
                            }
                            TextButton(
                                onClick = { onRemoveRecentJob(recentAppJobId) }
                            ) {
                                Text(strings.remove)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onClearJob,
                modifier = Modifier.fillMaxWidth(),
                enabled = canClearJob
            ) {
                Text(strings.clearSavedJob)
            }
            }
        }
        }

        GenerationFlowRail(
            strings = strings,
            job = job
        )
        GenerationStudioStatusDock(
            strings = strings,
            job = job,
            candidateCount = candidates.size,
            selectedCandidateDownloadId = selectedCandidateDownloadId
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = strings.generationRuntimeConsoleContentDescription
                },
            color = Color(0xFFF7FBFA),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Text(
                text = strings.generationMessage(message),
                modifier = Modifier.semantics {
                    if (message == CONTRACT_DEMO_PROGRESS_MESSAGE) {
                        contentDescription = strings.contractDemoNoLiveWorkerContentDescription
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475467)
            )
            val summaryLine = job?.let { generationProgressSummaryLine(it) }.orEmpty()
            if (summaryLine.isNotBlank() && summaryLine != message) {
                Text(
                    text = strings.generationMessage(summaryLine),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF157A52)
                )
            }
            val serverWorkerWaitNotice = job?.let { generationServerWorkerWaitNotice(it) }.orEmpty()
            if (serverWorkerWaitNotice.isNotBlank()) {
                Text(
                    text = strings.generationMessage(serverWorkerWaitNotice),
                    modifier = Modifier.semantics {
                        contentDescription = strings.serverWorkerWaitNoticeContentDescription
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB54708)
                )
            }
            val contractDemoNotice = job?.let { generationContractDemoNotice(it) }.orEmpty()
            if (contractDemoNotice.isNotBlank()) {
                Text(
                    text = strings.generationMessage(contractDemoNotice),
                    modifier = Modifier.semantics {
                        contentDescription = strings.contractDemoNoticeContentDescription
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB42318)
                )
            }
            if (workerReadinessMessage.isNotBlank()) {
                Text(
                    text = strings.generationMessage(workerReadinessMessage),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085)
                )
            }
            if (progressSteps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (step in progressSteps) {
                        Text(
                            text = "${strings.progressLabel(step.label)}: ${strings.progressStatus(step.status)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475467)
                        )
                        if (step.message.isNotBlank()) {
                            Text(
                                text = strings.generationMessage(step.message),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF667085)
                            )
                        }
                    }
                }
            }
            if (job != null) {
                Text(
                    text = strings.jobLabel(job.appJobId),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085)
                )
            }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = strings.generationReviewDeskContentDescription
                },
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Text(
                text = strings.generationReviewDeskTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            GenerationReviewStageHeader(
                title = strings.candidateGalleryTitle,
                detail = if (candidates.isNotEmpty()) {
                    strings.candidateReadyForInspection
                } else {
                    strings.candidateWaitingForInspection
                },
                accent = Color(0xFF2F63D6)
            )
            if (candidates.isNotEmpty()) {
                for ((index, candidate) in candidates.withIndex()) {
                    CandidateInspectionCard(
                        candidate = candidate,
                        title = strings.candidateTitle(candidate.title, index),
                        selected = selectedCandidateDownloadId == candidate.targetDownloadId,
                        strings = strings,
                        onSelectCandidate = onSelectCandidate
                    )
                }
            } else {
                GenerationWaitingCandidateNotice(strings = strings)
            }

            if (candidates.isNotEmpty()) {
                GenerationReviewStageHeader(
                    title = strings.reviewNotesStageTitle,
                    detail = strings.reviewNotesPlaceholder,
                    accent = Color(0xFFF97316)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE4E7EC))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = reviewNotes,
                            onValueChange = onReviewNotesChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = strings.reviewNotesContentDescription
                                },
                            label = { Text(strings.reviewNotesLabel) },
                            placeholder = { Text(strings.reviewNotesPlaceholder) },
                            minLines = 2
                        )
                        ReviewNoteSuggestionGrid(
                            strings = strings,
                            suggestions = reviewNoteSuggestions,
                            onAppendReviewNoteSuggestion = onAppendReviewNoteSuggestion
                        )
                    }
                }
            }
            GenerationReviewStageHeader(
                title = strings.deliveryActionsTitle,
                detail = strings.deliveryActionsHint,
                accent = Color(0xFF0F766E)
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = strings.generationReviewActionDockContentDescription
                    },
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE4E7EC))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GenerationDeliveryStatusStrip(
                        strings = strings,
                        selectedCandidateDownloadId = selectedCandidateDownloadId,
                        packageReady = job?.let { canShowPackageDownload(it) } == true,
                        communityDraftReady = canSubmitPackageImportDraft
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val acceptEnabled = canSubmitReviewDecision(
                            job = job,
                            selectedCandidateDownloadId = selectedCandidateDownloadId,
                            decision = "accept",
                            notesText = reviewNotes
                        )
                        val reviseEnabled = canSubmitReviewDecision(
                            job = job,
                            selectedCandidateDownloadId = selectedCandidateDownloadId,
                            decision = "revise",
                            notesText = reviewNotes
                        )
                        val rejectEnabled = canSubmitReviewDecision(
                            job = job,
                            selectedCandidateDownloadId = selectedCandidateDownloadId,
                            decision = "reject",
                            notesText = reviewNotes
                        )
                        Button(
                            onClick = { onReviewDecision("accept") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("review-accept")
                                .semantics {
                                    contentDescription = strings.reviewAcceptContentDescription
                                },
                            enabled = acceptEnabled
                        ) {
                            Text(
                                text = strings.reviewAccept,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { onReviewDecision("revise") },
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = strings.reviewReviseContentDescription
                                },
                            enabled = reviseEnabled
                        ) {
                            Text(
                                text = strings.reviewRevise,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { onReviewDecision("reject") },
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = strings.reviewRejectContentDescription
                                },
                            enabled = rejectEnabled
                        ) {
                            Text(
                                text = strings.reviewReject,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (candidates.isNotEmpty()) {
                        val reviewNotesMessage = reviewNotesValidationMessage("revise", reviewNotes)
                        if (selectedCandidateDownloadId.isNotBlank() && reviewNotesMessage.isNotBlank()) {
                            Text(
                                text = strings.generationMessage(reviewNotesMessage),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF667085)
                            )
                        }
                    }
                    Button(
                        onClick = onDownloadPackage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("package-download")
                            .semantics {
                                contentDescription = strings.packageDownloadContentDescription
                            },
                        enabled = job?.let { canShowPackageDownload(it) } == true
                    ) {
                        Text(strings.downloadPetZip)
                    }
                    if (packageDownloadMessage.isNotBlank()) {
                        Text(
                            text = strings.generationMessage(packageDownloadMessage),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475467)
                        )
                    }
                    if (packageImportCandidateMessage.isNotBlank()) {
                        Text(
                            text = strings.generationMessage(packageImportCandidateMessage),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF157A52)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSubmitPackageImport,
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = strings.submitCommunityReviewContentDescription
                                },
                            enabled = canSubmitPackageImportDraft
                        ) {
                            Text(
                                text = strings.submitToCommunityReview,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onRefreshPackageImportSubmission,
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = strings.refreshCommunitySubmissionContentDescription
                                },
                            enabled = canRefreshPackageImportSubmission
                        ) {
                            Text(
                                text = strings.refreshCommunitySubmission,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (packageImportSubmissionMessage.isNotBlank()) {
                        Text(
                            text = strings.generationMessage(packageImportSubmissionMessage),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475467)
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun GenerationDeliveryStatusStrip(
    strings: PetShellStrings,
    selectedCandidateDownloadId: String,
    packageReady: Boolean,
    communityDraftReady: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GenerationDeliveryStatusToken(
            label = strings.deliveryReviewTargetStatus,
            value = if (selectedCandidateDownloadId.isBlank()) {
                strings.deliveryStatusWaiting
            } else {
                strings.deliveryStatusSelected
            },
            accent = Color(0xFF2F63D6),
            active = selectedCandidateDownloadId.isNotBlank(),
            modifier = Modifier.weight(1f)
        )
        GenerationDeliveryStatusToken(
            label = strings.deliveryPackageStatus,
            value = if (packageReady) {
                strings.deliveryStatusPackageReady
            } else {
                strings.deliveryStatusPackageLocked
            },
            accent = Color(0xFFF97316),
            active = packageReady,
            modifier = Modifier.weight(1f)
        )
        GenerationDeliveryStatusToken(
            label = strings.deliveryCommunityStatus,
            value = if (communityDraftReady) {
                strings.deliveryStatusCommunityReady
            } else {
                strings.deliveryStatusCommunityWaiting
            },
            accent = Color(0xFF0F766E),
            active = communityDraftReady,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GenerationDeliveryStatusToken(
    label: String,
    value: String,
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = if (active) accent else Color(0xFF667085)
    Surface(
        modifier = modifier.height(52.dp),
        color = if (active) accent.copy(alpha = 0.12f) else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.30f) else Color(0xFFE4E7EC))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF667085),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GenerationBriefStageHeader(
    title: String,
    detail: String,
    accent: Color,
    actionLabel: String? = null,
    actionContentDescription: String = "",
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828)
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF667085),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                modifier = Modifier.semantics {
                    if (actionContentDescription.isNotBlank()) {
                        contentDescription = actionContentDescription
                    }
                },
                onClick = onAction
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun GenerationInlineNotice(
    text: String,
    accent: Color,
    container: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = container,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent)
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GenerationReviewStageHeader(
    title: String,
    detail: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101828)
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF667085),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CandidateInspectionCard(
    candidate: CandidateGalleryItem,
    title: String,
    selected: Boolean,
    strings: PetShellStrings,
    onSelectCandidate: (String) -> Unit
) {
    Surface(
        color = if (selected) Color(0xFFE7F0FF) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFF9DBBFF) else Color(0xFFE4E7EC)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101828)
                )
                ReviewStatePill(
                    text = if (selected) {
                        strings.candidateSelectedStatus
                    } else {
                        strings.candidateAvailableStatus
                    },
                    selected = selected
                )
            }
            RemoteCandidateImage(
                previewUrl = candidate.previewUrl,
                strings = strings
            )
            Button(
                onClick = { onSelectCandidate(candidate.targetDownloadId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("candidate-select-${candidate.targetDownloadId}")
                    .semantics {
                        contentDescription =
                            strings.candidateSelectContentDescription(candidate.targetDownloadId)
                    },
                enabled = !selected
            ) {
                Text(
                    if (selected) {
                        strings.selectedForReview
                    } else {
                        strings.selectCandidate
                    }
                )
            }
        }
    }
}

@Composable
private fun ReviewStatePill(
    text: String,
    selected: Boolean
) {
    Surface(
        color = if (selected) Color(0xFF2F63D6) else Color(0xFFEFF3F7),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else Color(0xFF667085),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GenerationWaitingCandidateNotice(strings: PetShellStrings) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.generationReviewWaitingContentDescription
            },
        color = Color(0xFFFFF7ED),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFED7AA))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PetArtworkBadge(
                action = PetAction.Review,
                modifier = Modifier.size(54.dp)
            )
            Text(
                text = strings.generationReviewWaitingForCandidate,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A3B07)
            )
        }
    }
}

@Composable
private fun ReviewNoteSuggestionGrid(
    strings: PetShellStrings,
    suggestions: List<String>,
    onAppendReviewNoteSuggestion: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in suggestions.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (suggestion in row) {
                    val displaySuggestion = strings.reviewNoteSuggestion(suggestion)
                    ReviewNoteSuggestionChip(
                        text = displaySuggestion,
                        onClick = { onAppendReviewNoteSuggestion(displaySuggestion) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReviewNoteSuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFD0D5DD))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF344054),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun GenerationFlowRail(
    strings: PetShellStrings,
    job: PetGenerationJobResponseDto?
) {
    val activeStep = generationFlowActiveStep(job)
    val steps = listOf(
        strings.generationFlowBriefStep,
        strings.generationFlowCandidateStep,
        strings.generationFlowReviewStep,
        strings.generationFlowPackageStep
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.generationFlowRailContentDescription
            },
        color = Color(0xFF0D3430),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings.generationFlowRailTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((index, label) in steps.withIndex()) {
                    GenerationFlowStepPill(
                        label = label,
                        active = index == activeStep,
                        completed = index < activeStep,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerationFlowStepPill(
    label: String,
    active: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val background = when {
        active -> Color(0xFFD7F3EE)
        completed -> Color(0xFF184E48)
        else -> Color(0xFF123D39)
    }
    val contentColor = when {
        active -> Color(0xFF0D3430)
        completed -> Color.White
        else -> Color(0xFFBFD8D4)
    }
    Surface(
        modifier = modifier.height(34.dp),
        color = background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun generationFlowActiveStep(job: PetGenerationJobResponseDto?): Int {
    job ?: return 0
    if (job.downloadReady || job.nextAction == "download-package") {
        return 3
    }
    return when (effectiveProgressStatus(job)) {
        "ready-for-download",
        "packaging" -> 3
        "waiting-for-review",
        "revision-requested",
        "candidate-rejected" -> 2
        "queued",
        "processing",
        "waiting-for-worker-output" -> 1
        else -> 0
    }
}

@Composable
private fun BodyShapeSegmentedControl(
    selectedBodyShape: String,
    strings: PetShellStrings,
    onBodyShapeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFF3F7))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (shape in GENERATION_BODY_SHAPE_OPTIONS) {
            val selected = shape == selectedBodyShape
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBodyShapeChange(shape) },
                color = if (selected) Color.White else Color.Transparent,
                shadowElevation = if (selected) 1.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.bodyShapeLabel(shape),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color(0xFF101828) else Color(0xFF667085),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteCandidateImage(
    previewUrl: String,
    strings: PetShellStrings
) {
    RemotePreviewImage(
        previewUrl = previewUrl,
        strings = strings,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    )
}

@Composable
private fun ApprovedPetPreviewArtwork(
    pet: ApprovedPet?,
    action: PetAction,
    strings: PetShellStrings,
    modifier: Modifier = Modifier
) {
    val previewUrl = approvedPetPreviewUrl(pet)

    if (previewUrl.isBlank()) {
        PetArtworkBadge(action = action, modifier = modifier)
        return
    }

    RemotePreviewImage(
        previewUrl = previewUrl,
        strings = strings,
        modifier = modifier,
        cropFirstSpritesheetFrame = true,
        fallback = {
            PetArtworkBadge(action = action, modifier = Modifier.fillMaxSize())
        }
    )
}

@Composable
private fun RemotePreviewImage(
    previewUrl: String,
    strings: PetShellStrings,
    modifier: Modifier = Modifier,
    cropFirstSpritesheetFrame: Boolean = false,
    fallback: @Composable (() -> Unit)? = null
) {
    var image by remember(previewUrl) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(previewUrl) { mutableStateOf(false) }
    val previewDownloader = remember { FantasyPetPreviewDownloader() }

    LaunchedEffect(previewUrl) {
        image = null
        failed = false
        if (previewUrl.isBlank()) {
            failed = true
            return@LaunchedEffect
        }
        image = when (val result = previewDownloader.download(previewUrl)) {
            is PetPreviewDownloadResult.Success -> {
                withContext(Dispatchers.Default) {
                    val decoded = BitmapFactory.decodeByteArray(
                        result.bytes,
                        0,
                        result.bytes.size
                    )
                    val displayBitmap = if (cropFirstSpritesheetFrame) {
                        decoded?.firstSpritesheetFrame()
                    } else {
                        decoded
                    }
                    displayBitmap?.asImageBitmap()
                }
            }
            is PetPreviewDownloadResult.Failure -> null
        }
        failed = image == null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFF3F7)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = strings.candidatePreviewContentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (failed && fallback != null) {
            fallback()
        } else {
            Text(
                text = if (failed) strings.previewUnavailable else strings.loadingPreview,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085)
            )
        }
    }
}

@Composable
private fun PetAvatar(
    action: PetAction,
    strings: PetShellStrings
) {
    val palette = petAvatarPalette(action)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(96.dp)
                .semantics {
                    contentDescription = strings.petAvatarContentDescription
                }
        ) {
            drawDesktopPetMascot(
                palette = palette,
                action = action
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = strings.petActionLabel(action),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF475467),
            textAlign = TextAlign.Center
        )
    }
}

private data class PetAvatarPalette(
    val auraStart: Color,
    val auraEnd: Color,
    val body: Color,
    val bodyShade: Color,
    val accent: Color
)

private fun petAvatarPalette(action: PetAction): PetAvatarPalette =
    when (action) {
        PetAction.AppLoading,
        PetAction.BubbleOpen -> PetAvatarPalette(
            auraStart = Color(0xFFD7F3EE),
            auraEnd = Color(0xFFFFE2C7),
            body = Color(0xFF58C7B2),
            bodyShade = Color(0xFF0F766E),
            accent = Color(0xFFF97316)
        )
        PetAction.Reward -> PetAvatarPalette(
            auraStart = Color(0xFFFFE8B6),
            auraEnd = Color(0xFFFFC6D4),
            body = Color(0xFFFFB84D),
            bodyShade = Color(0xFFF97316),
            accent = Color(0xFFEF476F)
        )
        PetAction.Review -> PetAvatarPalette(
            auraStart = Color(0xFFE7F0FF),
            auraEnd = Color(0xFFD7F3EE),
            body = Color(0xFF76A9FA),
            bodyShade = Color(0xFF2F80ED),
            accent = Color(0xFF0F766E)
        )
        else -> PetAvatarPalette(
            auraStart = Color(0xFFFFD166),
            auraEnd = Color(0xFFEF476F),
            body = Color(0xFFFF8F70),
            bodyShade = Color(0xFFEF476F),
            accent = Color(0xFFFFD166)
        )
    }

private fun DrawScope.drawDesktopPetMascot(
    palette: PetAvatarPalette,
    action: PetAction
) {
    val width = size.width
    val height = size.height
    val centerX = width * 0.5f
    val hop = when (action) {
        PetAction.FeedNext,
        PetAction.FeedPrevious,
        PetAction.FeedSkip,
        PetAction.ShowcaseNext,
        PetAction.ShowcasePrevious -> -height * 0.04f
        else -> 0f
    }
    val look = when (action) {
        PetAction.FeedPrevious,
        PetAction.ShowcasePrevious -> -1f
        PetAction.FeedNext,
        PetAction.FeedSkip,
        PetAction.ShowcaseNext -> 1f
        else -> 0f
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.auraStart, palette.auraEnd),
            center = Offset(centerX, height * 0.44f),
            radius = width * 0.52f
        ),
        radius = width * 0.48f,
        center = Offset(centerX, height * 0.48f)
    )
    drawOval(
        color = Color(0x33000000),
        topLeft = Offset(width * 0.24f, height * 0.78f),
        size = Size(width * 0.52f, height * 0.1f)
    )

    val tail = Path().apply {
        moveTo(width * 0.72f, height * 0.52f + hop)
        cubicTo(
            width * 0.94f,
            height * 0.42f + hop,
            width * 0.88f,
            height * 0.24f + hop,
            width * 0.72f,
            height * 0.31f + hop
        )
    }
    drawPath(
        path = tail,
        color = palette.bodyShade,
        style = Stroke(width = width * 0.1f, cap = StrokeCap.Round)
    )

    val leftEar = Path().apply {
        moveTo(width * 0.31f, height * 0.35f + hop)
        lineTo(width * 0.38f, height * 0.14f + hop)
        lineTo(width * 0.48f, height * 0.34f + hop)
        close()
    }
    val rightEar = Path().apply {
        moveTo(width * 0.52f, height * 0.34f + hop)
        lineTo(width * 0.63f, height * 0.14f + hop)
        lineTo(width * 0.7f, height * 0.35f + hop)
        close()
    }
    drawPath(leftEar, palette.bodyShade)
    drawPath(rightEar, palette.bodyShade)

    drawOval(
        color = palette.body,
        topLeft = Offset(width * 0.24f, height * 0.29f + hop),
        size = Size(width * 0.52f, height * 0.5f)
    )
    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(width * 0.38f, height * 0.52f + hop),
        size = Size(width * 0.24f, height * 0.2f)
    )
    drawCircle(
        color = Color(0xFF101828),
        radius = width * 0.045f,
        center = Offset(width * (0.41f + look * 0.03f), height * 0.46f + hop)
    )
    drawCircle(
        color = Color(0xFF101828),
        radius = width * 0.045f,
        center = Offset(width * (0.59f + look * 0.03f), height * 0.46f + hop)
    )
    drawCircle(
        color = Color.White,
        radius = width * 0.015f,
        center = Offset(width * (0.425f + look * 0.03f), height * 0.445f + hop)
    )
    drawCircle(
        color = Color.White,
        radius = width * 0.015f,
        center = Offset(width * (0.605f + look * 0.03f), height * 0.445f + hop)
    )
    drawCircle(
        color = palette.accent,
        radius = width * 0.035f,
        center = Offset(width * 0.5f, height * 0.54f + hop)
    )
    drawRoundRect(
        color = Color(0x33000000),
        topLeft = Offset(width * 0.42f, height * 0.63f + hop),
        size = Size(width * 0.16f, height * 0.025f),
        cornerRadius = CornerRadius(width * 0.02f, width * 0.02f)
    )
}

@Composable
private fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF101828)
        )
    }
}

@Composable
private fun CompactSpeechBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF101828),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LanguageToggle(
    language: PetShellLanguage,
    strings: PetShellStrings,
    onLanguageChange: (PetShellLanguage) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (compact) {
        CompactLanguageToggle(
            language = language,
            strings = strings,
            onLanguageChange = onLanguageChange,
            modifier = modifier
        )
        return
    }

    Surface(
        modifier = modifier,
        color = Color(0xFFEFF3F7),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                modifier = Modifier.semantics {
                    contentDescription = strings.chineseLanguageToggleContentDescription
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                onClick = { onLanguageChange(PetShellLanguage.Chinese) }
            ) {
                Text(
                    text = strings.chineseLanguageLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (language == PetShellLanguage.Chinese) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
                )
            }
            TextButton(
                modifier = Modifier.semantics {
                    contentDescription = strings.englishLanguageToggleContentDescription
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                onClick = { onLanguageChange(PetShellLanguage.English) }
            ) {
                Text(
                    text = strings.englishLanguageLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (language == PetShellLanguage.English) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactLanguageToggle(
    language: PetShellLanguage,
    strings: PetShellStrings,
    onLanguageChange: (PetShellLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.74f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactLanguageSegment(
                label = strings.chineseLanguageLabel,
                selected = language == PetShellLanguage.Chinese,
                contentDescription = strings.chineseLanguageToggleContentDescription,
                onClick = { onLanguageChange(PetShellLanguage.Chinese) }
            )
            CompactLanguageSegment(
                label = strings.englishLanguageLabel,
                selected = language == PetShellLanguage.English,
                contentDescription = strings.englishLanguageToggleContentDescription,
                onClick = { onLanguageChange(PetShellLanguage.English) }
            )
        }
    }
}

@Composable
private fun CompactLanguageSegment(
    label: String,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFFE0F3EE) else Color.Transparent)
            .semantics {
                this.contentDescription = contentDescription
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color(0xFF0F766E) else Color(0xFF475467),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun WalletPill(
    balance: Int,
    strings: PetShellStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF101828),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = strings.walletBalance(balance),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
private fun MetadataPill(label: String) {
    Surface(
        color = Color(0xFFE8F3EE),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF157A52)
        )
    }
}

internal fun feedPostMetadataLabels(post: FeedPost): List<String> =
    listOfNotNull(post.sourceLabel, post.rewardLabel)

internal fun feedPostAuditLabels(post: FeedPost): List<String> =
    listOfNotNull(
        post.importDraftLabel,
        post.submissionLabel,
        post.scoreReportLabel,
        post.importSourceLabel,
        post.importPreviewLabel,
        post.exportArtifactLabel,
        post.motionSheetLabel
    ).filter { it.isSafeAssetDisplayText() }

internal fun approvedPetRegistrySummary(pets: List<ApprovedPet>): String =
    if (pets.isEmpty()) {
        "No approved pets yet"
    } else {
        "${pets.size} approved pet${if (pets.size == 1) "" else "s"}"
    }

internal fun approvedPetShowcaseTitle(
    pets: List<ApprovedPet>,
    selectedIndex: Int
): String =
    pets.selectedApprovedPet(selectedIndex)?.displayName ?: "Awaiting approved pet"

internal fun approvedPetShowcaseDetail(
    pets: List<ApprovedPet>,
    selectedIndex: Int
): String {
    val pet = pets.selectedApprovedPet(selectedIndex) ?: return "Approved imports will appear here."
    return "${pet.sourceKind} / score ${pet.totalScore} / ${pet.motionSheetCount} motion sheets"
}

internal fun approvedPetShowcasePosition(
    pets: List<ApprovedPet>,
    selectedIndex: Int
): String {
    if (pets.isEmpty()) return "No showcase selection"
    val displayIndex = if (selectedIndex in pets.indices) selectedIndex else 0
    return "Pet ${displayIndex + 1} of ${pets.size}"
}

internal fun approvedPetShowcaseAsset(
    pets: List<ApprovedPet>,
    selectedIndex: Int
): String {
    val pet = pets.selectedApprovedPet(selectedIndex) ?: return "Preview asset pending"
    if (!pet.previewPath.isSafeAssetDisplayText()) {
        return "Preview asset pending"
    }
    return "Preview ${pet.previewPath}"
}

internal fun approvedPetShowcasePackage(
    pets: List<ApprovedPet>,
    selectedIndex: Int
): String {
    val pet = pets.selectedApprovedPet(selectedIndex) ?: return "Package artifact pending"
    return if (pet.exportArtifactPath.isBlank() || !pet.exportArtifactPath.isSafeAssetDisplayText()) {
        "Package artifact pending"
    } else {
        "Package ${pet.exportArtifactPath}"
    }
}

internal fun approvedPetPreviewUrl(
    pet: ApprovedPet?,
    baseUrl: String = com.gamer.community.BuildConfig.COMMUNITY_API_BASE_URL
): String {
    if (pet == null || pet.sourceKind != "fantasy-pet-rule") {
        return ""
    }

    val explicitPreviewUrl = pet.previewUrl.trim()
    val resolvedPreviewUrl = approvedPetExplicitPreviewUrl(explicitPreviewUrl, baseUrl)
    if (resolvedPreviewUrl.isNotBlank()) {
        return resolvedPreviewUrl
    }

    val appJobId = pet.sourceAppJobId.trim().ifBlank { pet.petId.trim() }
    val targetDownloadId = pet.targetDownloadId.trim().ifBlank { pet.previewPath.trim() }
    if (
        appJobId.isBlank() ||
        !PUBLIC_ARTIFACT_ID.matches(targetDownloadId) ||
        !targetDownloadId.isSafeAssetDisplayText()
    ) {
        return ""
    }

    return "${baseUrl.trimEnd('/')}/pet-generation-jobs/${appJobId.pathSegment()}/artifacts/${targetDownloadId.pathSegment()}"
}

private fun approvedPetExplicitPreviewUrl(previewUrl: String, baseUrl: String): String {
    if (previewUrl.isBlank()) {
        return ""
    }

    if (previewUrl.startsWith("/") && previewUrl.isSafePublicArtifactRoute()) {
        return "${baseUrl.trimEnd('/')}$previewUrl"
    }

    val normalizedBaseUrl = baseUrl.trimEnd('/')
    if (
        previewUrl.startsWith(normalizedBaseUrl) &&
        previewUrl.removePrefix(normalizedBaseUrl).isSafePublicArtifactRoute()
    ) {
        return previewUrl
    }

    return ""
}

private fun List<ApprovedPet>.selectedApprovedPet(selectedIndex: Int): ApprovedPet? {
    if (isEmpty()) return null
    return this[if (selectedIndex in indices) selectedIndex else 0]
}

private fun String.pathSegment(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

private fun String.isSafeAssetDisplayText(): Boolean {
    val trimmed = trim()
    if (trimmed.isBlank()) {
        return true
    }

    val lower = trimmed.lowercase()
    return !WINDOWS_ASSET_PATH.containsMatchIn(trimmed) &&
        !lower.startsWith("file:") &&
        !lower.startsWith("/") &&
        !lower.contains("\\") &&
        INTERNAL_ASSET_MARKERS.none { marker -> lower.contains(marker) }
}

private fun String.isSafePublicArtifactRoute(): Boolean {
    val trimmed = trim()
    val lower = trimmed.lowercase()
    return trimmed.startsWith("/pet-generation-jobs/") &&
        trimmed.contains("/artifacts/") &&
        !trimmed.contains("\\") &&
        !trimmed.contains("..") &&
        !lower.startsWith("file:") &&
        !WINDOWS_ASSET_PATH.containsMatchIn(trimmed) &&
        INTERNAL_ASSET_MARKERS.none { marker -> lower.contains(marker) }
}

private val WINDOWS_ASSET_PATH = Regex("(^|\\s)[A-Za-z]:[\\\\/]")

private val PUBLIC_ARTIFACT_ID = Regex("[A-Za-z0-9._-]{1,128}")

private val INTERNAL_ASSET_MARKERS = listOf(
    "server_run.json",
    "artifact-index.json",
    "resolution-map",
    "desktop-pet-casebook-audit.json",
    "desktop-pet-stage-gate-report.json",
    "desktop-pet-learning-memory.json",
    "human-feedback-context.json",
    "genericagent-orchestrator-task.json",
    "codex-worker-task.json",
    "codex-worker-task.output.json",
    "strategy-plan.json",
    "codex-generation-directives.json",
    "server-generation-learning-drill.json",
    "server-generation-regression-report.json",
    "learning-ledger.jsonl",
    "route-policy-decision.json",
    "genericagent-ledger-suggestions.json",
    "genericagent-ledger-import.json",
    "stage-gate-ledger-import.json",
    "learning-drill",
    "learningprogress",
    "codexgenerationdirectiveresponse",
    "codexgenerationdirectiveresponsepresentcount",
    "codexgenerationdirectiveresponsesummary",
    "codexqaevidence",
    "directivehistoryresponse",
    "narrowedrepairfocus",
    "gadirectivehistoryresponse",
    "gadirectivehistoryresponsepresentcount",
    "gadirectivehistoryaddressedgenerationdirectivetext",
    "gadirectivehistorynarrowedrepairfocus",
    "gadirectivehistorynarrowedrepairfocuscounts",
    "directivehistorynarrowedrepairfocuscountdeltas",
    "repeateddirectivehistorynarrowedrepairfocus",
    "codex-action-attempt-n-server-imagegen-001",
    "stagegaterepair",
    "stagegaterepairrequests",
    "stagegatestatus",
    "regression-report",
    "agent-review.json",
    "orchestration-review.json",
    "runs/",
    "runs\\",
    "secret/"
)
