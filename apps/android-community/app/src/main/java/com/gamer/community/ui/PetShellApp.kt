package com.gamer.community.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder
import com.gamer.community.firstSpritesheetFrame
import com.gamer.community.horizontalSpritesheetFrames
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
import com.gamer.community.generation.GenerationReviewLoopPhase
import com.gamer.community.generation.GenerationReviewLoopUiState
import com.gamer.community.generation.HttpFantasyPetGenerationClient
import com.gamer.community.generation.PetPreviewDownloadResult
import com.gamer.community.generation.PetGenerationJobResponseDto
import com.gamer.community.generation.PetGenerationPackageImportCandidate
import com.gamer.community.generation.REVIEW_NOTE_SUGGESTIONS
import com.gamer.community.generation.SelectedActionReviewConsoleUiState
import com.gamer.community.generation.selectedActionReviewConsoleUiState
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
import com.gamer.community.generation.generationReviewLoopUiState
import com.gamer.community.generation.initialGenerationJobHistory
import com.gamer.community.generation.persistedGenerationJobId
import com.gamer.community.generation.packageDownloadFailureMessage
import com.gamer.community.generation.packageDownloadStartedMessage
import com.gamer.community.generation.packageDownloadSuccessMessage
import com.gamer.community.generation.packageImportDraftFailureCandidate
import com.gamer.community.generation.packageImportDraftSuccessCandidate
import com.gamer.community.generation.packageImportCandidateMessage
import com.gamer.community.generation.packageImportInProgressCandidate
import com.gamer.community.generation.packageReadyShelfStatus
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
import com.gamer.community.petshell.DefaultDesktopPet
import com.gamer.community.petshell.FeedDirection
import com.gamer.community.petshell.FeedPost
import com.gamer.community.petshell.HatchSla
import com.gamer.community.petshell.PetAction
import com.gamer.community.petshell.PetShellController
import com.gamer.community.petshell.PetShellState
import com.gamer.community.petshell.ShellPhase
import com.gamer.community.petshell.motionSheetFor
import com.gamer.community.petshell.selectedDefaultDesktopPet
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

private object GamerUiTokens {
    object ColorRole {
        val Ink = Color(0xFF142136)
        val Muted = Color(0xFF5D6E87)
        val Subtle = Color(0xFF465A73)
        val Line = Color(0xFFE2EEF8)
        val Raised = Color(0xFFFFFFFF)
        val Disabled = Color(0xFFA7B1C2)
        val ShellBackground = Color(0xFFF8FCFF)
        val DarkSurface = Color(0xFF0B7487)
        val DarkRaised = Color(0xFF1597AA)
        val DarkLine = Color(0xFF7BD8E3)
        val Identity = Color(0xFF00B8AA)
        val IdentityDark = Color(0xFF064E4A)
        val IdentitySoft = Color(0xFFD8FFF7)
        val Reward = Color(0xFFFF9B2F)
        val RewardDark = Color(0xFF8A4200)
        val RewardSoft = Color(0xFFFFEED2)
        val Review = Color(0xFF4F8CFF)
        val ReviewDark = Color(0xFF1D4ED8)
        val ReviewSoft = Color(0xFFEAF3FF)
        val Mystery = Color(0xFFB962F7)
        val Success = Color(0xFF10A66B)
        val Warning = Color(0xFFE03B2D)
        val WarningSoft = Color(0xFFFFF4D8)
        val ChannelText = Color(0xFFFFFFFF)
        val HatchSurface = Color(0xFFFFFFF6)
        val HatchLine = Color(0xFFFFD58B)
        val NeutralPill = Color(0xFFF1F7FC)
        val EggShell = Color(0xFFFFD892)
        val EggShellIdle = Color(0xFFFFF4DF)
        val EggCrack = Color(0xFFD66A00)
    }

    object Shape {
        val Card = RoundedCornerShape(8.dp)
        val Control = RoundedCornerShape(8.dp)
        val Tight = RoundedCornerShape(6.dp)
    }

    object Space {
        val Xs = 3.dp
        val Sm = 6.dp
        val Md = 8.dp
        val Lg = 10.dp
        val Xl = 12.dp
    }
}

private val GamerColorScheme = lightColorScheme(
    primary = GamerUiTokens.ColorRole.Identity,
    onPrimary = Color.White,
    primaryContainer = GamerUiTokens.ColorRole.IdentitySoft,
    onPrimaryContainer = GamerUiTokens.ColorRole.IdentityDark,
    secondary = GamerUiTokens.ColorRole.Reward,
    onSecondary = Color.White,
    secondaryContainer = GamerUiTokens.ColorRole.RewardSoft,
    onSecondaryContainer = Color(0xFF4C2605),
    background = GamerUiTokens.ColorRole.ShellBackground,
    onBackground = GamerUiTokens.ColorRole.Ink,
    surface = Color.White,
    onSurface = GamerUiTokens.ColorRole.Ink,
    surfaceVariant = Color(0xFFEFF4F8),
    onSurfaceVariant = GamerUiTokens.ColorRole.Subtle,
    outline = Color(0xFF8C94A1)
)

@Composable
fun PetShellApp(
    repository: CommunityRepository,
    generationService: FantasyPetGenerationService,
    initialGenerationDescription: String = "",
    openDesktopPetOnStart: Boolean = false,
    openProfileOnStart: Boolean = false,
    canShowDesktopPetOverlay: () -> Boolean = { false },
    canPostDesktopPetNotification: () -> Boolean = { true },
    onRequestDesktopPetOverlayPermission: () -> Unit = {},
    onRequestDesktopPetNotificationPermission: () -> Unit = {},
    onStartDesktopPetOverlay: () -> Unit = {},
    onStopDesktopPetOverlay: () -> Unit = {},
    onResetDesktopPetOverlayPosition: () -> Unit = {},
    onRefreshDesktopPetNotification: () -> Unit = {},
    onRefreshDesktopPetOverlayPreview: () -> Unit = {}
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
    var defaultDesktopPetInitialized by remember {
        mutableStateOf(
            uiPrefs.getBoolean("defaultDesktopPetInitialized", false) ||
                uiPrefs.getString("defaultDesktopPetId", "").orEmpty().isNotBlank()
        )
    }
    var state by remember {
        val initialState = PetShellController.initialState(
            selectedDefaultDesktopPetId = uiPrefs.getString("defaultDesktopPetId", "").orEmpty()
        )
        mutableStateOf(
            if (openProfileOnStart) {
                PetShellController.openCommunity(initialState)
            } else {
                initialState
            }
        )
    }
    var language by remember {
        mutableStateOf(parsePetShellLanguage(uiPrefs.getString("language", null)))
    }
    val strings = remember(language) {
        petShellStrings(language)
    }
    var selectedTab by remember {
        mutableStateOf(if (openProfileOnStart) PetShellTab.Profile else PetShellTab.Community)
    }
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

    fun changeDefaultDesktopPet(petId: String) {
        val nextState = PetShellController.selectDefaultDesktopPet(state, petId)
        state = nextState
        defaultDesktopPetInitialized = true
        uiPrefs.edit()
            .putString("defaultDesktopPetId", nextState.selectedDefaultDesktopPetId)
            .putBoolean("defaultDesktopPetInitialized", true)
            .apply()
    }

    fun completeDefaultDesktopPetInitialization(petId: String) {
        val nextState = PetShellController.selectDefaultDesktopPet(state, petId)
        state = PetShellController.openDesktopPet(nextState)
        defaultDesktopPetInitialized = true
        uiPrefs.edit()
            .putString("defaultDesktopPetId", nextState.selectedDefaultDesktopPetId)
            .putBoolean("defaultDesktopPetInitialized", true)
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
                .ifBlank { "Enter a task name to poll." }
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

    fun startHatchJob(
        descriptionOverride: String?,
        appJobIdOverride: String? = null,
        startMessage: String
    ) {
        val effectiveDescription = descriptionOverride ?: generationDescription
        val effectiveAppJobId = appJobIdOverride ?: generationAppJobId
        scope.launch {
            generationMessage = startMessage
            packageDownloadMessage = ""
            packageImportCandidate = null
            readyPackageImportDraft = null
            clearPackageImportSubmissionTracking()
            when (val result = generationService.createJob(
                description = effectiveDescription,
                appJobId = effectiveAppJobId,
                bodyShape = generationBodyShape,
                referencesText = generationReferences
            )) {
                is ApiCallResult.Success -> {
                    applyGenerationJobUpdate(result.value)
                    persistGenerationJobId(
                        requestedAppJobId = effectiveAppJobId,
                        job = result.value
                    )
                }
                is ApiCallResult.Failure -> {
                    generationMessage = "Generation request failed: ${result.reason}"
                }
            }
        }
    }

    val latestPetShellState by rememberUpdatedState(state)

    LaunchedEffect(repository) {
        val result = repository.loadInitialCommunity()
        state = PetShellController.applyCommunityLoad(
            state = state,
            posts = result.posts,
            approvedPets = result.approvedPets,
            walletBalance = result.walletBalance,
            checkInClaimed = result.checkInClaimed,
            pendingSubmissionCount = result.pendingSubmissionCount,
            latestSubmission = result.latestSubmission,
            hatchSla = result.hatchSla,
            usedFallback = result.usedFallback,
            message = result.message
        )
    }

    LaunchedEffect(
        state.petAction,
        state.feedIndex,
        state.approvedPetIndex,
        state.walletBalance,
        state.checkInClaimed
    ) {
        val action = state.petAction
        val delayMillis = action.returnToIdleDelayMillis() ?: return@LaunchedEffect
        delay(delayMillis)
        val currentState = latestPetShellState
        if (currentState.petAction == action) {
            state = currentState.copy(petAction = PetAction.Idle)
        }
    }

    LaunchedEffect(state.selectedDefaultDesktopPetId, state.defaultDesktopPets) {
        val selectedPet = state.selectedDefaultDesktopPet()
        uiPrefs.edit()
            .putString("desktopPetOverlayPreviewAssetPath", selectedPet?.previewAssetPath.orEmpty())
            .putString("desktopPetOverlayMotionSheetAssetPath", selectedPet?.idleMotionSheetAssetPath.orEmpty())
            .putInt("desktopPetOverlayMotionFrameCount", selectedPet?.idleMotionFrameCount ?: 0)
            .putString("desktopPetOverlayPreviewUrl", "")
            .putString("desktopPetOverlayPetName", desktopPetOverlayDisplayName(selectedPet))
            .apply()
        if (desktopPetOverlayRunning) {
            onRefreshDesktopPetOverlayPreview()
        }
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
        Surface(modifier = Modifier.fillMaxSize(), color = GamerUiTokens.ColorRole.ShellBackground) {
            if (!defaultDesktopPetInitialized) {
                DefaultDesktopPetOnboardingScreen(
                    state = state,
                    language = language,
                    strings = strings,
                    onLanguageChange = ::changeLanguage,
                    onComplete = ::completeDefaultDesktopPetInitialization
                )
            } else when (state.phase) {
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
                    packageImportCandidate = packageImportCandidate,
                    readyPackageImportDraft = readyPackageImportDraft,
                    packageImportSubmissionId = packageImportSubmissionId,
                    packageImportSubmissionMessage = packageImportSubmissionMessage,
                    onTabSelected = { selectedTab = it },
                    onLanguageChange = ::changeLanguage,
                    onDirectPetLaunchChange = ::changeDirectPetLaunch,
                    onDesktopPetOverlayAutoShowChange = ::changeDesktopPetOverlayAutoShow,
                    onRequestDesktopPetOverlayPermission = ::requestDesktopPetOverlayPermission,
                    onRequestDesktopPetNotificationPermission = ::requestDesktopPetNotificationPermission,
                    onStartDesktopPetOverlay = ::startDesktopPetOverlay,
                    onStopDesktopPetOverlay = ::stopDesktopPetOverlay,
                    onResetDesktopPetOverlayPosition = ::resetDesktopPetOverlayPosition,
                    onDefaultDesktopPetSelected = ::changeDefaultDesktopPet,
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
                            walletBalance = state.walletBalance,
                            hatchSla = state.hatchSla,
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
                            onMysteryHatch = {
                                val mysteryPrompt = strings.hatcheryMysteryPrompt(hatcheryRandomSeed())
                                generationDescription = mysteryPrompt
                                generationAppJobId = ""
                                generationReferences = ""
                                startHatchJob(
                                    descriptionOverride = mysteryPrompt,
                                    appJobIdOverride = "",
                                    startMessage = "Creating mystery hatch job..."
                                )
                            },
                            onCreateJob = {
                                startHatchJob(
                                    descriptionOverride = null,
                                    startMessage = "Creating generation job..."
                                )
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
                                                    latestSubmission =
                                                        refreshedCommunity.latestSubmission,
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
                                                    latestSubmission =
                                                        refreshedCommunity.latestSubmission,
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
private fun DefaultDesktopPetOnboardingScreen(
    state: PetShellState,
    language: PetShellLanguage,
    strings: PetShellStrings,
    onLanguageChange: (PetShellLanguage) -> Unit,
    onComplete: (String) -> Unit
) {
    var selectedPetId by remember(state.defaultDesktopPets) {
        mutableStateOf(state.selectedDefaultDesktopPetId.ifBlank {
            state.defaultDesktopPets.firstOrNull()?.id.orEmpty()
        })
    }
    val selectedPet = state.defaultDesktopPets.firstOrNull { it.id == selectedPetId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEAFBF7),
                        Color(0xFFFFF7EA),
                        Color(0xFFF7FBFF)
                    )
                )
            )
            .semantics {
                contentDescription = strings.defaultDesktopPetOnboardingContentDescription
            }
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 64.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.defaultDesktopPetOnboardingTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.defaultDesktopPetOnboardingDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = GamerUiTokens.ColorRole.Subtle,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = GamerUiTokens.Shape.Card,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    selectedPet?.let { pet ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GamerUiTokens.ColorRole.IdentitySoft.copy(alpha = 0.54f)),
                            contentAlignment = Alignment.Center
                        ) {
                            DefaultDesktopPetPreviewArtwork(
                                pet = pet,
                                action = PetAction.Idle,
                                strings = strings,
                                modifier = Modifier.size(170.dp)
                            )
                        }
                    }
                    for (pet in state.defaultDesktopPets) {
                        DefaultDesktopPetStarterCard(
                            pet = pet,
                            selected = pet.id == selectedPetId,
                            strings = strings,
                            onClick = { selectedPetId = pet.id }
                        )
                    }
                }
            }
            Button(
                onClick = { onComplete(selectedPetId) },
                enabled = selectedPetId.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.defaultDesktopPetOnboardingAction)
            }
            Text(
                text = strings.defaultDesktopPetOnboardingFootnote,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Muted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
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
private fun DefaultDesktopPetStarterCard(
    pet: DefaultDesktopPet,
    selected: Boolean,
    strings: PetShellStrings,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (selected) {
            GamerUiTokens.ColorRole.IdentitySoft
        } else {
            Color(0xFFF8FAFC)
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                GamerUiTokens.ColorRole.Identity
            } else {
                GamerUiTokens.ColorRole.Line
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DefaultDesktopPetPreviewArtwork(
                pet = pet,
                action = PetAction.Idle,
                strings = strings,
                modifier = Modifier.size(72.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = strings.defaultDesktopPetName(pet),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.defaultDesktopPetStarterLine(pet),
                    style = MaterialTheme.typography.labelMedium,
                    color = GamerUiTokens.ColorRole.Subtle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DesktopPetSettingPill(
                label = if (selected) {
                    strings.defaultDesktopPetSelected
                } else {
                    strings.defaultDesktopPetSelect
                },
                accent = if (selected) {
                    GamerUiTokens.ColorRole.Identity
                } else {
                    GamerUiTokens.ColorRole.Muted
                }
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
    val selectedPet = state.selectedDefaultDesktopPet()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3FFF8),
                        Color(0xFFFFF5E6),
                        GamerUiTokens.ColorRole.ShellBackground
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
                .padding(top = 58.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.desktopPetModeTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink
            )
            Text(
                text = strings.desktopPetModeSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GamerUiTokens.ColorRole.Subtle,
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
            DesktopPetHomeSummary(
                state = state,
                selectedPet = selectedPet,
                strings = strings
            )
            if (selectedPet == null) {
                DesktopPetRemoteSyncStrip(strings = strings)
            } else {
                DefaultDesktopPetSignalStrip(
                    pet = selectedPet,
                    strings = strings
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
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
        )
        LanguageToggle(
            language = language,
            strings = strings,
            onLanguageChange = onLanguageChange,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding(),
            compact = true
        )
    }
}

@Composable
private fun DesktopPetStage(
    state: PetShellState,
    selectedPet: DefaultDesktopPet?,
    strings: PetShellStrings
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.94f),
        shape = GamerUiTokens.Shape.Card,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DefaultDesktopPetPreviewArtwork(
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
private fun DesktopPetHomeSummary(
    state: PetShellState,
    selectedPet: DefaultDesktopPet?,
    strings: PetShellStrings
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8FAFC),
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, Color(0xFFE4E7EC))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.desktopPetHomeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101828),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.desktopPetHomeDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF667085),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DesktopPetHomeMetric(
                    label = strings.desktopPetActivePetMetric,
                    value = selectedPet?.let(strings::defaultDesktopPetName)
                        ?: strings.desktopPetActivePetMissing,
                    accent = Color(0xFF0F766E),
                    modifier = Modifier.weight(1f)
                )
                DesktopPetHomeMetric(
                    label = strings.desktopPetWalletMetric,
                    value = strings.walletBalance(state.walletBalance),
                    accent = Color(0xFFF97316),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DesktopPetHomeMetric(
                    label = strings.desktopPetCheckInMetric,
                    value = if (state.checkInClaimed) {
                        strings.desktopPetCheckInDone
                    } else {
                        strings.desktopPetCheckInReady
                    },
                    accent = Color(0xFF2F63D6),
                    modifier = Modifier.weight(1f)
                )
                DesktopPetHomeMetric(
                    label = strings.desktopPetPendingReviewMetric,
                    value = strings.quickActionReviewStatus(state.pendingSubmissionCount),
                    accent = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DesktopPetHomeMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(GamerUiTokens.Shape.Card)
            .background(accent.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DefaultDesktopPetSignalStrip(
    pet: DefaultDesktopPet?,
    strings: PetShellStrings
) {
    val previewReady = pet?.previewAssetPath?.isNotBlank() == true
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.92f),
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ApprovedPetSignalToken(
                    label = strings.defaultDesktopPetElementMetric,
                    value = pet?.let(strings::defaultDesktopPetElementLabel) ?: "-",
                    accent = GamerUiTokens.ColorRole.Identity,
                    modifier = Modifier.weight(1f)
                )
                ApprovedPetSignalToken(
                    label = strings.defaultDesktopPetMotionMetric,
                    value = pet?.let(strings::defaultDesktopPetMotionLabel) ?: "-",
                    accent = GamerUiTokens.ColorRole.Review,
                    modifier = Modifier.weight(1f)
                )
                ApprovedPetSignalToken(
                    label = strings.defaultDesktopPetPreviewMetric,
                    value = if (previewReady) {
                        strings.defaultDesktopPetPreviewReady
                    } else {
                        strings.defaultDesktopPetPreviewPending
                    },
                    accent = if (previewReady) GamerUiTokens.ColorRole.Reward else GamerUiTokens.ColorRole.Muted,
                    modifier = Modifier.weight(1f)
                )
            }
            if (pet != null) {
                Text(
                    text = strings.defaultDesktopPetSourceLine(pet),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.desktopPetOpenCommunityContentDescription
                        }
                ) {
                    Text(
                        text = strings.desktopPetOpenCommunity,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onOpenGenerate,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.desktopPetOpenGenerateContentDescription
                        }
                ) {
                    Text(
                        text = strings.desktopPetOpenGenerate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onOpenProfile,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = strings.desktopPetOpenProfileContentDescription
                        }
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
    packageImportCandidate: PetGenerationPackageImportCandidate?,
    readyPackageImportDraft: ImportDraftDto?,
    packageImportSubmissionId: String,
    packageImportSubmissionMessage: String,
    onTabSelected: (PetShellTab) -> Unit,
    onLanguageChange: (PetShellLanguage) -> Unit,
    onDirectPetLaunchChange: (Boolean) -> Unit,
    onDesktopPetOverlayAutoShowChange: (Boolean) -> Unit,
    onRequestDesktopPetOverlayPermission: () -> Unit,
    onRequestDesktopPetNotificationPermission: () -> Unit,
    onStartDesktopPetOverlay: () -> Unit,
    onStopDesktopPetOverlay: () -> Unit,
    onResetDesktopPetOverlayPosition: () -> Unit,
    onDefaultDesktopPetSelected: (String) -> Unit,
    onEnterDesktopPet: () -> Unit,
    onNavigate: (FeedDirection) -> Unit,
    onShowcaseNavigate: (FeedDirection) -> Unit,
    onCheckIn: () -> Unit,
    generationContent: @Composable () -> Unit
) {
    Scaffold(
        containerColor = GamerUiTokens.ColorRole.ShellBackground,
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
                    packageImportCandidate = packageImportCandidate,
                    readyPackageImportDraft = readyPackageImportDraft,
                    packageImportSubmissionId = packageImportSubmissionId,
                    packageImportSubmissionMessage = packageImportSubmissionMessage,
                    onDirectPetLaunchChange = onDirectPetLaunchChange,
                    onDesktopPetOverlayAutoShowChange = onDesktopPetOverlayAutoShowChange,
                    onRequestDesktopPetOverlayPermission = onRequestDesktopPetOverlayPermission,
                    onRequestDesktopPetNotificationPermission = onRequestDesktopPetNotificationPermission,
                    onStartDesktopPetOverlay = onStartDesktopPetOverlay,
                    onStopDesktopPetOverlay = onStopDesktopPetOverlay,
                    onResetDesktopPetOverlayPosition = onResetDesktopPetOverlayPosition,
                    onDefaultDesktopPetSelected = onDefaultDesktopPetSelected,
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
                    selectedIconColor = GamerUiTokens.ColorRole.Identity,
                    selectedTextColor = GamerUiTokens.ColorRole.Identity,
                    indicatorColor = GamerUiTokens.ColorRole.IdentitySoft,
                    unselectedIconColor = GamerUiTokens.ColorRole.Muted,
                    unselectedTextColor = GamerUiTokens.ColorRole.Muted
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
    val ink = if (selected) GamerUiTokens.ColorRole.Identity else GamerUiTokens.ColorRole.Muted
    Surface(
        modifier = Modifier.size(width = 46.dp, height = 32.dp),
        color = if (selected) GamerUiTokens.ColorRole.IdentitySoft else GamerUiTokens.ColorRole.NeutralPill,
        contentColor = ink,
        shape = GamerUiTokens.Shape.Card,
        border = if (selected) BorderStroke(1.dp, GamerUiTokens.ColorRole.Identity.copy(alpha = 0.30f)) else null
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
            .clip(GamerUiTokens.Shape.Card)
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
        val currentPost = if (state.posts.isEmpty()) {
            null
        } else {
            state.posts[state.feedIndex.coerceIn(state.posts.indices)]
        }
        if (currentPost == null) {
            FeedEmptyBlock(
                strings = strings,
                onCreatePet = onCreatePet
            )
        } else {
            FeedPostBlock(
                post = currentPost,
                strings = strings
            )
            FeedControls(
                strings = strings,
                onNavigate = onNavigate
            )
        }
    }
}

@Composable
private fun CommunityStatusSummary(
    state: PetShellState,
    strings: PetShellStrings
) {
    val remoteSynced = state.remoteCommunitySynced
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
                    color = GamerUiTokens.ColorRole.Ink
                )
                CommunityStatusPill(
                    label = if (remoteSynced) {
                        strings.communityStatusRemoteSynced
                    } else {
                        strings.communityStatusRemoteUnavailable
                    },
                    accent = if (remoteSynced) GamerUiTokens.ColorRole.Identity else GamerUiTokens.ColorRole.Reward
                )
                CommunityStatusPill(
                    label = strings.communityStatusHumanReview,
                    accent = GamerUiTokens.ColorRole.Review
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CommunitySummaryToken(
                    label = strings.profileWalletSummaryTitle,
                    value = strings.walletBalance(state.walletBalance),
                    accent = GamerUiTokens.ColorRole.Identity,
                    modifier = Modifier.weight(1f)
                )
                CommunitySummaryToken(
                    label = strings.dailyCheckIn,
                    value = if (state.checkInClaimed) strings.checkedIn else strings.quickActionCheckInDetail,
                    accent = GamerUiTokens.ColorRole.Reward,
                    modifier = Modifier.weight(1f)
                )
                CommunitySummaryToken(
                    label = strings.profileApprovedPetsMetric,
                    value = state.approvedPets.size.toString(),
                    accent = GamerUiTokens.ColorRole.Review,
                    modifier = Modifier.weight(1f)
                )
                CommunitySummaryToken(
                    label = strings.quickActionReview,
                    value = strings.quickActionReviewStatus(state.pendingSubmissionCount),
                    accent = GamerUiTokens.ColorRole.Subtle,
                    modifier = Modifier.weight(1f)
                )
            }
            LatestSubmissionActionNotice(
                text = strings.latestSubmissionAction(state.latestSubmission),
                strings = strings
            )
        }
    }
}

@Composable
private fun LatestSubmissionActionNotice(
    text: String,
    strings: PetShellStrings
) {
    if (text.isBlank()) {
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.latestSubmissionActionContentDescription
            },
        color = GamerUiTokens.ColorRole.ReviewSoft.copy(alpha = 0.7f),
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Review.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = GamerUiTokens.ColorRole.ReviewDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CommunityStatusPill(
    label: String,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = GamerUiTokens.Shape.Control,
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
            .clip(GamerUiTokens.Shape.Control)
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 24.dp)
                .clip(GamerUiTokens.Shape.Control)
                .background(accent)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink,
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
        color = GamerUiTokens.ColorRole.DarkSurface,
        shape = GamerUiTokens.Shape.Card,
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
                    color = if (index == 0) GamerUiTokens.ColorRole.IdentitySoft else GamerUiTokens.ColorRole.DarkRaised,
                    shape = GamerUiTokens.Shape.Tight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (index == 0) GamerUiTokens.ColorRole.IdentityDark else GamerUiTokens.ColorRole.ChannelText,
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
            container = GamerUiTokens.ColorRole.WarningSoft,
            content = GamerUiTokens.ColorRole.RewardDark,
            enabled = !state.checkInClaimed,
            onClick = onCheckIn,
            modifier = Modifier.weight(1f)
        )
        CommunityQuickActionTile(
            icon = CommunityQuickActionIcon.Generate,
            title = strings.quickActionGenerate,
            detail = strings.quickActionGenerateDetail,
            container = GamerUiTokens.ColorRole.IdentitySoft,
            content = GamerUiTokens.ColorRole.IdentityDark,
            onClick = onCreatePet,
            modifier = Modifier.weight(1f)
        )
        CommunityQuickActionTile(
            icon = CommunityQuickActionIcon.Review,
            title = strings.quickActionReview,
            detail = strings.quickActionReviewStatus(state.pendingSubmissionCount),
            container = GamerUiTokens.ColorRole.ReviewSoft,
            content = GamerUiTokens.ColorRole.ReviewDark,
            onClick = onReview,
            modifier = Modifier.weight(1f)
        )
        CommunityQuickActionTile(
            icon = CommunityQuickActionIcon.Showcase,
            title = strings.quickActionShowcase,
            detail = strings.quickActionShowcaseDetail,
            container = GamerUiTokens.ColorRole.RewardSoft,
            content = GamerUiTokens.ColorRole.RewardDark,
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
            .clip(GamerUiTokens.Shape.Card)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) container else GamerUiTokens.ColorRole.Line,
        shape = GamerUiTokens.Shape.Card,
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
                color = if (enabled) content else GamerUiTokens.ColorRole.Disabled
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) content else GamerUiTokens.ColorRole.Disabled,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) content.copy(alpha = 0.72f) else GamerUiTokens.ColorRole.Disabled,
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
            .clip(GamerUiTokens.Shape.Control)
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
                            GamerUiTokens.ColorRole.IdentitySoft,
                            GamerUiTokens.ColorRole.ReviewSoft,
                            GamerUiTokens.ColorRole.RewardSoft
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
                            DefaultDesktopPetPreviewArtwork(
                                pet = state.selectedDefaultDesktopPet(),
                                action = state.petAction,
                                strings = strings,
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
                            color = GamerUiTokens.ColorRole.Ink
                        )
                        Text(
                            text = strings.generationStudioHeroSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = GamerUiTokens.ColorRole.Subtle,
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
        color = Color.White.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink,
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
    packageImportCandidate: PetGenerationPackageImportCandidate?,
    readyPackageImportDraft: ImportDraftDto?,
    packageImportSubmissionId: String,
    packageImportSubmissionMessage: String,
    onDirectPetLaunchChange: (Boolean) -> Unit,
    onDesktopPetOverlayAutoShowChange: (Boolean) -> Unit,
    onRequestDesktopPetOverlayPermission: () -> Unit,
    onRequestDesktopPetNotificationPermission: () -> Unit,
    onStartDesktopPetOverlay: () -> Unit,
    onStopDesktopPetOverlay: () -> Unit,
    onResetDesktopPetOverlayPosition: () -> Unit,
    onDefaultDesktopPetSelected: (String) -> Unit,
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
            activeDesktopPet = state.selectedDefaultDesktopPet(),
            defaultDesktopPets = state.defaultDesktopPets,
            selectedDefaultDesktopPetId = state.selectedDefaultDesktopPetId,
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
            onDefaultDesktopPetSelected = onDefaultDesktopPetSelected,
            onEnterDesktopPet = onEnterDesktopPet
        )
        ProfilePetShelf(
            state = state,
            strings = strings,
            packageImportCandidate = packageImportCandidate,
            readyPackageImportDraft = readyPackageImportDraft,
            packageImportSubmissionId = packageImportSubmissionId,
            packageImportSubmissionMessage = packageImportSubmissionMessage,
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
                        GamerUiTokens.ColorRole.DarkSurface,
                        GamerUiTokens.ColorRole.Identity,
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
                shape = GamerUiTokens.Shape.Card
            ) {
                Box(modifier = Modifier.padding(6.dp)) {
                    DefaultDesktopPetPreviewArtwork(
                        pet = state.selectedDefaultDesktopPet(),
                        action = state.petAction,
                        strings = strings,
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
        shape = GamerUiTokens.Shape.Card,
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
                    accent = GamerUiTokens.ColorRole.Identity,
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricToken(
                    label = strings.profileApprovedPetsMetric,
                    value = state.approvedPets.size.toString(),
                    accent = GamerUiTokens.ColorRole.Review,
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricToken(
                    label = strings.quickActionCheckIn,
                    value = if (state.checkInClaimed) strings.checkedIn else strings.quickActionCheckInDetail,
                    accent = GamerUiTokens.ColorRole.Reward,
                    modifier = Modifier.weight(1f)
                )
            }
            LatestSubmissionActionNotice(
                text = strings.latestSubmissionAction(state.latestSubmission),
                strings = strings
            )
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
            .clip(GamerUiTokens.Shape.Card)
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
                    .clip(GamerUiTokens.Shape.Control)
                    .background(accent)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Ink,
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
    packageImportCandidate: PetGenerationPackageImportCandidate?,
    readyPackageImportDraft: ImportDraftDto?,
    packageImportSubmissionId: String,
    packageImportSubmissionMessage: String,
    onCreatePet: () -> Unit
) {
    val hasApprovedPets = state.approvedPets.isNotEmpty()
    val selectedPet = state.approvedPets.selectedApprovedPet(state.approvedPetIndex)
    val shelfStatus = packageReadyShelfStatus(
        packageImportCandidate = packageImportCandidate,
        readyPackageImportDraft = readyPackageImportDraft,
        packageImportSubmissionId = packageImportSubmissionId,
        packageImportSubmissionMessage = packageImportSubmissionMessage,
        approvedPets = state.approvedPets
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.profilePetShelfContentDescription
            },
        color = Color.White,
        shape = GamerUiTokens.Shape.Card,
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
                if (hasApprovedPets) {
                    ApprovedPetPreviewArtwork(
                        pet = selectedPet,
                        action = state.petAction,
                        strings = strings,
                        modifier = Modifier.size(82.dp)
                    )
                }
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
                        text = approvedPetDisplayName(
                            pet = selectedPet,
                            emptyLabel = if (selectedPet == null) {
                                strings.desktopPetActivePetMissing
                            } else {
                                strings.desktopPetActivePetReady
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = GamerUiTokens.ColorRole.Subtle
                    )
                    Text(
                        text = strings.approvedPetShowcaseDetail(
                            pets = state.approvedPets,
                            selectedIndex = state.approvedPetIndex
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = GamerUiTokens.ColorRole.Muted
                    )
                }
            }
            if (hasApprovedPets) {
                ApprovedPetSignalStrip(
                    pet = selectedPet,
                    strings = strings
                )
                LatestApprovedShelfNotice(
                    text = strings.latestApprovedShelfLine(
                        latestSubmission = state.latestSubmission,
                        approvedPets = state.approvedPets
                    )
                )
            }
            if (shelfStatus.visible) {
                ProfilePackageReadyShelfStatusStrip(
                    label = strings.profilePackageReadyShelfLabel(shelfStatus.label),
                    detail = strings.profilePackageReadyShelfDetail(shelfStatus.detail),
                    tone = shelfStatus.tone
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
private fun ProfilePackageReadyShelfStatusStrip(
    label: String,
    detail: String,
    tone: String
) {
    val accent = when (tone) {
        "approved" -> GamerUiTokens.ColorRole.Success
        "pending" -> GamerUiTokens.ColorRole.Review
        "ready" -> GamerUiTokens.ColorRole.Identity
        "package" -> GamerUiTokens.ColorRole.Reward
        else -> GamerUiTokens.ColorRole.Muted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GamerUiTokens.Shape.Card)
            .background(accent.copy(alpha = 0.10f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 5.dp, height = 40.dp)
                .clip(GamerUiTokens.Shape.Control)
                .background(accent)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Subtle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
        color = GamerUiTokens.ColorRole.DarkSurface,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.DarkLine)
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
    activeDesktopPet: DefaultDesktopPet?,
    defaultDesktopPets: List<DefaultDesktopPet>,
    selectedDefaultDesktopPetId: String,
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
    onDefaultDesktopPetSelected: (String) -> Unit,
    onEnterDesktopPet: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = GamerUiTokens.Shape.Card,
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
                color = GamerUiTokens.ColorRole.Ink
            )
            DesktopPetActivePreviewStatus(
                strings = strings,
                activeDesktopPet = activeDesktopPet
            )
            DefaultDesktopPetSelector(
                strings = strings,
                defaultDesktopPets = defaultDesktopPets,
                selectedDefaultDesktopPetId = selectedDefaultDesktopPetId,
                onDefaultDesktopPetSelected = onDefaultDesktopPetSelected
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
private fun DesktopPetActivePreviewStatus(
    strings: PetShellStrings,
    activeDesktopPet: DefaultDesktopPet?
) {
    val previewReady = activeDesktopPet?.previewAssetPath?.isNotBlank() == true
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.desktopPetOverlayActivePreviewContentDescription
            },
        color = GamerUiTokens.ColorRole.IdentitySoft.copy(alpha = 0.54f),
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Identity.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactDesktopPetPreviewThumbnail(
                pet = activeDesktopPet,
                strings = strings,
                modifier = Modifier
                    .size(42.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = strings.desktopPetOverlayActivePreviewTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Identity
                )
                Text(
                    text = activeDesktopPet?.let(strings::defaultDesktopPetName)
                        ?: strings.desktopPetOverlayActivePreviewMissing,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DesktopPetSettingPill(
                label = if (previewReady) {
                    strings.desktopPetOverlayActivePreviewReady
                } else {
                    strings.desktopPetOverlayActivePreviewPending
                },
                accent = if (previewReady) Color(0xFF0F766E) else Color(0xFF667085),
                modifier = Modifier.weight(0.9f)
            )
        }
    }
}

@Composable
private fun CompactDesktopPetPreviewThumbnail(
    pet: DefaultDesktopPet?,
    strings: PetShellStrings,
    modifier: Modifier = Modifier
) {
    DefaultDesktopPetPreviewArtwork(
        pet = pet,
        action = PetAction.Idle,
        strings = strings,
        modifier = modifier,
        loading = {
            StatusIconMark(
                action = PetAction.Idle,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

@Composable
private fun DefaultDesktopPetSelector(
    strings: PetShellStrings,
    defaultDesktopPets: List<DefaultDesktopPet>,
    selectedDefaultDesktopPetId: String,
    onDefaultDesktopPetSelected: (String) -> Unit
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.defaultDesktopPetSelectorTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Ink
                )
                Text(
                    text = strings.defaultDesktopPetSelectorDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (pet in defaultDesktopPets) {
                    val selected = pet.id == selectedDefaultDesktopPetId
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 118.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDefaultDesktopPetSelected(pet.id) },
                        color = if (selected) {
                            GamerUiTokens.ColorRole.IdentitySoft.copy(alpha = 0.72f)
                        } else {
                            Color.White
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) {
                                GamerUiTokens.ColorRole.Identity
                            } else {
                                GamerUiTokens.ColorRole.Line
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DefaultDesktopPetPreviewArtwork(
                                pet = pet,
                                action = PetAction.Idle,
                                strings = strings,
                                modifier = Modifier.size(58.dp)
                            )
                            Text(
                                text = strings.defaultDesktopPetName(pet),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GamerUiTokens.ColorRole.Ink,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.defaultDesktopPetElementLabel(pet),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) {
                                    GamerUiTokens.ColorRole.Identity
                                } else {
                                    GamerUiTokens.ColorRole.Muted
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
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
    val autoShowReady = permissionGranted && notificationPermissionGranted
    val autoShowStatusAccent = when {
        autoShowEnabled && autoShowReady -> Color(0xFF0F766E)
        autoShowEnabled -> Color(0xFFB42318)
        else -> Color(0xFF667085)
    }
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
                    maxLines = 2,
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
            DesktopPetSettingPill(
                label = strings.systemDesktopPetAutoShowStatus(
                    enabled = autoShowEnabled,
                    ready = autoShowReady
                ),
                accent = autoShowStatusAccent,
                modifier = Modifier.fillMaxWidth()
            )
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
                        DefaultDesktopPetAvatar(
                            pet = state.selectedDefaultDesktopPet(),
                            action = state.petAction,
                            strings = strings
                        )
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
                    latestSubmission = state.latestSubmission,
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
                if (hasApprovedPets) {
                    ApprovedPetPreviewArtwork(
                        pet = selectedPet,
                        action = state.petAction,
                        strings = strings,
                        modifier = Modifier.size(82.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = approvedPetDisplayName(
                            pet = selectedPet,
                            emptyLabel = if (selectedPet == null) {
                                strings.desktopPetActivePetMissing
                            } else {
                                strings.desktopPetActivePetReady
                            }
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
                        color = GamerUiTokens.ColorRole.Muted
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
        color = GamerUiTokens.ColorRole.Raised,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Line)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ApprovedPetSignalToken(
                    label = strings.approvedPetScoreMetric,
                    value = pet?.totalScore?.toString() ?: "-",
                    accent = GamerUiTokens.ColorRole.Reward,
                    modifier = Modifier.weight(1f)
                )
                ApprovedPetSignalToken(
                    label = strings.approvedPetMotionMetric,
                    value = pet?.motionSheetCount?.toString() ?: "-",
                    accent = GamerUiTokens.ColorRole.Review,
                    modifier = Modifier.weight(1f)
                )
                ApprovedPetSignalToken(
                    label = strings.approvedPetPreviewMetric,
                    value = if (previewReady) {
                        strings.approvedPetPreviewReady
                    } else {
                        strings.approvedPetPreviewPending
                    },
                    accent = if (previewReady) GamerUiTokens.ColorRole.Identity else GamerUiTokens.ColorRole.Muted,
                    modifier = Modifier.weight(1f)
                )
            }
            if (pet != null) {
                Text(
                    text = strings.approvedPetSourceLine(pet),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LatestApprovedShelfNotice(text: String) {
    if (text.isBlank()) {
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GamerUiTokens.ColorRole.IdentitySoft,
        shape = GamerUiTokens.Shape.Control,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Identity)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GamerUiTokens.ColorRole.IdentityDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            .clip(GamerUiTokens.Shape.Card)
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GamerUiTokens.ColorRole.Muted,
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
            accent = GamerUiTokens.ColorRole.Identity,
            modifier = Modifier.weight(1f)
        )
        ShowcasePathStep(
            label = strings.showcasePathReview,
            accent = GamerUiTokens.ColorRole.Review,
            modifier = Modifier.weight(1f)
        )
        ShowcasePathStep(
            label = strings.showcasePathPublish,
            accent = GamerUiTokens.ColorRole.Reward,
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
        shape = GamerUiTokens.Shape.Card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 22.dp)
                    .clip(GamerUiTokens.Shape.Control)
                    .background(accent)
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeedEmptyBlock(
    strings: PetShellStrings,
    onCreatePet: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.communityPostCardContentDescription
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = GamerUiTokens.Shape.Card
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIconMark(
                action = PetAction.Idle,
                modifier = Modifier.size(52.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = strings.communityFeedEmptyTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Ink
                )
                Text(
                    text = strings.communityFeedEmptyDetail,
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onCreatePet) {
                Text(strings.profileCreatePetAction)
            }
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
        shape = GamerUiTokens.Shape.Card
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GamerUiTokens.ColorRole.Raised,
                shape = GamerUiTokens.Shape.Card,
                border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Line)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIconMark(
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
                            color = GamerUiTokens.ColorRole.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = post.petId,
                            style = MaterialTheme.typography.labelSmall,
                            color = GamerUiTokens.ColorRole.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.communityFeedSignalTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = GamerUiTokens.ColorRole.Identity,
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
                color = GamerUiTokens.ColorRole.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = GamerUiTokens.ColorRole.Subtle,
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
        color = GamerUiTokens.ColorRole.IdentitySoft.copy(alpha = 0.62f),
        shape = GamerUiTokens.Shape.Control
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                drawCircle(
                    color = GamerUiTokens.ColorRole.Identity,
                    radius = size.minDimension * 0.28f,
                    center = Offset(size.width * 0.5f, size.height * 0.42f)
                )
                drawRoundRect(
                    color = GamerUiTokens.ColorRole.Identity,
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
                    color = GamerUiTokens.ColorRole.Identity,
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Identity,
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
        color = GamerUiTokens.ColorRole.DarkSurface,
        shape = GamerUiTokens.Shape.Card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 28.dp)
                    .clip(GamerUiTokens.Shape.Control)
                    .background(GamerUiTokens.ColorRole.Reward)
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
        color = GamerUiTokens.ColorRole.Raised,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Line)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (label in labels.take(3)) {
                Text(
                    text = strings.feedMetadataLabel(label),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (labels.size > 3) {
                Text(
                    text = "+${labels.size - 3}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Identity
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
            startColor = GamerUiTokens.ColorRole.Identity,
            endColor = GamerUiTokens.ColorRole.Review,
            accentColor = GamerUiTokens.ColorRole.Reward,
            titleColor = Color.White,
            subtitleColor = Color(0xFFEFFFFB)
        )
        PetShellTab.Generate -> PetShellHeaderBackgroundSpec(
            startColor = GamerUiTokens.ColorRole.ReviewDark,
            endColor = GamerUiTokens.ColorRole.Reward,
            accentColor = GamerUiTokens.ColorRole.IdentitySoft,
            titleColor = Color.White,
            subtitleColor = Color(0xFFFFF7E8)
        )
        PetShellTab.Profile -> PetShellHeaderBackgroundSpec(
            startColor = GamerUiTokens.ColorRole.Reward,
            endColor = GamerUiTokens.ColorRole.DarkRaised,
            accentColor = GamerUiTokens.ColorRole.RewardSoft,
            titleColor = Color.White,
            subtitleColor = Color(0xFFFFFAEF)
        )
    }

@Composable
private fun StatusIconMark(
    action: PetAction,
    modifier: Modifier = Modifier
) {
    val accent = petActionAccent(action)
    Box(
        modifier = modifier
            .clip(GamerUiTokens.Shape.Card)
            .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.82f),
                topLeft = Offset(size.width * 0.24f, size.height * 0.20f),
                size = Size(size.width * 0.52f, size.height * 0.60f),
                cornerRadius = CornerRadius(size.minDimension * 0.08f, size.minDimension * 0.08f)
            )
            drawLine(
                color = accent,
                start = Offset(size.width * 0.36f, size.height * 0.40f),
                end = Offset(size.width * 0.64f, size.height * 0.40f),
                strokeWidth = size.minDimension * 0.05f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = accent.copy(alpha = 0.76f),
                start = Offset(size.width * 0.36f, size.height * 0.52f),
                end = Offset(size.width * 0.58f, size.height * 0.52f),
                strokeWidth = size.minDimension * 0.05f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = accent.copy(alpha = 0.54f),
                start = Offset(size.width * 0.36f, size.height * 0.64f),
                end = Offset(size.width * 0.52f, size.height * 0.64f),
                strokeWidth = size.minDimension * 0.05f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun petActionAccent(action: PetAction): Color =
    when (action) {
        PetAction.Reward -> GamerUiTokens.ColorRole.Reward
        PetAction.Review -> GamerUiTokens.ColorRole.Review
        PetAction.FeedNext,
        PetAction.FeedPrevious,
        PetAction.FeedSkip,
        PetAction.ShowcaseNext,
        PetAction.ShowcasePrevious -> GamerUiTokens.ColorRole.Identity
        else -> GamerUiTokens.ColorRole.Muted
    }

private fun PetAction.returnToIdleDelayMillis(): Long? =
    when (this) {
        PetAction.Idle,
        PetAction.Review -> null
        PetAction.FeedNext,
        PetAction.FeedPrevious,
        PetAction.FeedSkip,
        PetAction.ShowcaseNext,
        PetAction.ShowcasePrevious,
        PetAction.Reward -> 2_200L
    }

private fun DrawScope.drawImmersiveHeaderPattern(spec: PetShellHeaderBackgroundSpec) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.08f)
            )
        )
    )
    drawCircle(
        color = spec.accentColor.copy(alpha = 0.30f),
        radius = size.minDimension * 0.64f,
        center = Offset(size.width * 0.92f, size.height * 0.12f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.16f),
        radius = size.minDimension * 0.42f,
        center = Offset(size.width * 0.08f, size.height * 0.92f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f),
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
        color = Color.White.copy(alpha = 0.12f)
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
        color = Color.White.copy(alpha = 0.34f),
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
        color = spec.accentColor.copy(alpha = 0.32f),
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
private fun HatcheryOverviewPanel(
    strings: PetShellStrings,
    walletBalance: Int,
    hatchSla: HatchSla,
    job: PetGenerationJobResponseDto?,
    candidateCount: Int,
    selectedCandidateDownloadId: String,
    onMysteryHatch: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.hatcheryOverviewContentDescription
            },
        color = GamerUiTokens.ColorRole.HatchSurface,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.HatchLine),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(GamerUiTokens.Space.Xl),
            verticalArrangement = Arrangement.spacedBy(GamerUiTokens.Space.Lg)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GamerUiTokens.Space.Lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HatcheryEggBadge(
                    active = job != null,
                    modifier = Modifier.size(62.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(GamerUiTokens.Space.Xs)
                ) {
                    Text(
                        text = strings.hatcheryOverviewTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GamerUiTokens.ColorRole.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.hatcheryOverviewDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = GamerUiTokens.ColorRole.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HatcheryWalletBadge(
                    label = strings.hatcheryWalletLabel,
                    value = strings.walletBalance(walletBalance)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GamerUiTokens.Space.Md)
            ) {
                HatcheryModeToken(
                    title = strings.hatcheryReserveRandomTitle,
                    detail = strings.hatcheryReserveRandomDetail,
                    status = strings.hatcheryModeComingSoon,
                    accent = GamerUiTokens.ColorRole.Identity,
                    active = false,
                    modifier = Modifier.weight(1f)
                )
                HatcheryModeToken(
                    title = strings.hatcheryMysteryRandomTitle,
                    detail = strings.hatcheryMysteryRandomDetail,
                    status = strings.hatcheryModeActive,
                    accent = GamerUiTokens.ColorRole.Mystery,
                    active = true,
                    actionLabel = strings.hatcheryMysteryAction,
                    actionContentDescription = strings.hatcheryMysteryActionContentDescription,
                    onAction = onMysteryHatch,
                    modifier = Modifier.weight(1f)
                )
                HatcheryModeToken(
                    title = strings.hatcheryCustomTitle,
                    detail = strings.hatcheryCustomDetail,
                    status = strings.hatcheryModeActive,
                    accent = GamerUiTokens.ColorRole.Reward,
                    active = true,
                    modifier = Modifier.weight(1f)
                )
            }
            HatcherySlaRow(strings = strings, hatchSla = hatchSla)
            HatcheryFinePathNotice(strings = strings)
            HatcheryProgressRail(
                strings = strings,
                job = job,
                candidateCount = candidateCount,
                selectedCandidateDownloadId = selectedCandidateDownloadId
            )
        }
    }
}

@Composable
private fun HatcheryEggBadge(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val shellColor = if (active) {
        GamerUiTokens.ColorRole.EggShell
    } else {
        GamerUiTokens.ColorRole.EggShellIdle
    }
    val crackColor = if (active) GamerUiTokens.ColorRole.EggCrack else GamerUiTokens.ColorRole.Line
    Canvas(modifier = modifier) {
        drawOval(
            color = Color(0x26000000),
            topLeft = Offset(size.width * 0.18f, size.height * 0.82f),
            size = Size(size.width * 0.64f, size.height * 0.12f)
        )
        drawOval(
            color = shellColor,
            topLeft = Offset(size.width * 0.18f, size.height * 0.05f),
            size = Size(size.width * 0.64f, size.height * 0.82f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.72f),
            radius = size.minDimension * 0.12f,
            center = Offset(size.width * 0.38f, size.height * 0.28f)
        )
        val stroke = size.minDimension * 0.055f
        drawLine(
            color = crackColor,
            start = Offset(size.width * 0.46f, size.height * 0.34f),
            end = Offset(size.width * 0.56f, size.height * 0.45f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = crackColor,
            start = Offset(size.width * 0.56f, size.height * 0.45f),
            end = Offset(size.width * 0.48f, size.height * 0.56f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = crackColor,
            start = Offset(size.width * 0.48f, size.height * 0.56f),
            end = Offset(size.width * 0.6f, size.height * 0.66f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun HatcheryWalletBadge(
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .clip(GamerUiTokens.Shape.Control)
            .background(Color.White.copy(alpha = 0.84f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Muted,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HatcheryModeToken(
    title: String,
    detail: String,
    status: String,
    accent: Color,
    active: Boolean,
    actionLabel: String? = null,
    actionContentDescription: String = "",
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val container = if (active) Color.White.copy(alpha = 0.84f) else Color.White.copy(alpha = 0.58f)
    val borderColor = if (active) accent.copy(alpha = 0.34f) else GamerUiTokens.ColorRole.Line
    Surface(
        modifier = modifier
            .heightIn(min = 128.dp),
        color = container,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (active) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(GamerUiTokens.Space.Md),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HatcheryModeIcon(
                    accent = accent,
                    active = active,
                    modifier = Modifier.size(28.dp)
                )
                HatcheryModeStatusPill(
                    status = status,
                    accent = accent,
                    active = active
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(GamerUiTokens.Space.Xs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .semantics {
                            if (actionContentDescription.isNotBlank()) {
                                contentDescription = actionContentDescription
                            }
                        },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(34.dp))
            }
        }
    }
}

@Composable
private fun HatcherySlaRow(
    strings: PetShellStrings,
    hatchSla: HatchSla
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.72f),
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.HatchLine)
    ) {
        Column(
            modifier = Modifier.padding(GamerUiTokens.Space.Md),
            verticalArrangement = Arrangement.spacedBy(GamerUiTokens.Space.Sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.hatcherySlaTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Ink
                )
                Text(
                    text = strings.hatcheryPollingSla(
                        hatchSla.suggestedPollIntervalMs,
                        hatchSla.consecutivePollFailuresBeforeSlowNotice
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GamerUiTokens.Space.Sm)
            ) {
                HatcherySlaChip(
                    text = strings.hatcheryReserveSla(hatchSla.reserveEggMaxMs),
                    accent = GamerUiTokens.ColorRole.Identity,
                    modifier = Modifier.weight(1f)
                )
                HatcherySlaChip(
                    text = strings.hatcheryMysterySla(hatchSla.mysteryEggMaxMs),
                    accent = GamerUiTokens.ColorRole.Mystery,
                    modifier = Modifier.weight(1f)
                )
                HatcherySlaChip(
                    text = strings.hatcheryCustomSla(hatchSla.customHatchMaxMs),
                    accent = GamerUiTokens.ColorRole.Reward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HatcherySlaChip(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(GamerUiTokens.Shape.Control)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.24f), GamerUiTokens.Shape.Control),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = GamerUiTokens.ColorRole.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HatcheryModeStatusPill(
    status: String,
    accent: Color,
    active: Boolean
) {
    val background = if (active) accent.copy(alpha = 0.12f) else GamerUiTokens.ColorRole.NeutralPill
    val textColor = if (active) accent else GamerUiTokens.ColorRole.Muted
    Box(
        modifier = Modifier
            .clip(GamerUiTokens.Shape.Control)
            .background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HatcheryModeIcon(
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val shellColor = if (active) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        drawCircle(
            color = shellColor,
            radius = size.minDimension * 0.48f,
            center = Offset(size.width * 0.5f, size.height * 0.5f)
        )
        drawOval(
            color = if (active) accent.copy(alpha = 0.90f) else GamerUiTokens.ColorRole.Disabled,
            topLeft = Offset(size.width * 0.30f, size.height * 0.18f),
            size = Size(size.width * 0.40f, size.height * 0.58f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.78f),
            radius = size.minDimension * 0.07f,
            center = Offset(size.width * 0.43f, size.height * 0.33f)
        )
        drawOval(
            color = Color(0x26000000),
            topLeft = Offset(size.width * 0.28f, size.height * 0.72f),
            size = Size(size.width * 0.44f, size.height * 0.09f)
        )
    }
}

@Composable
private fun HatcheryFinePathNotice(strings: PetShellStrings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.72f),
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Line)
    ) {
        Text(
            text = strings.hatcheryFinePathNotice,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = GamerUiTokens.ColorRole.Subtle
        )
    }
}

@Composable
private fun HatcheryProgressRail(
    strings: PetShellStrings,
    job: PetGenerationJobResponseDto?,
    candidateCount: Int,
    selectedCandidateDownloadId: String
) {
    val steps = listOf(
        strings.hatcheryStepEgg,
        strings.hatcheryStepPrompt,
        strings.hatcheryStepIncubating,
        strings.hatcheryStepReview,
        strings.hatcheryStepShelf
    )
    val activeStep = hatcheryActiveStep(
        job = job,
        candidateCount = candidateCount,
        selectedCandidateDownloadId = selectedCandidateDownloadId
    )
    val progress = ((activeStep + 1).toFloat() / steps.size.toFloat()).coerceIn(0.18f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.hatcheryProgressTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink
            )
            Text(
                text = strings.hatcheryProgressStatus(activeStep),
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Muted,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(GamerUiTokens.Shape.Control)
                .background(GamerUiTokens.ColorRole.Line)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(GamerUiTokens.Shape.Control)
                    .background(GamerUiTokens.ColorRole.Identity)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GamerUiTokens.Space.Sm)) {
            for ((index, label) in steps.withIndex()) {
                HatcheryProgressStep(
                    label = label,
                    active = index == activeStep,
                    completed = index < activeStep,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HatcheryProgressStep(
    label: String,
    active: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        active -> GamerUiTokens.ColorRole.Identity
        completed -> GamerUiTokens.ColorRole.Success
        else -> GamerUiTokens.ColorRole.Muted
    }
    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
    )
}

private fun hatcheryActiveStep(
    job: PetGenerationJobResponseDto?,
    candidateCount: Int,
    selectedCandidateDownloadId: String
): Int {
    job ?: return 0
    if (canShowPackageDownload(job)) {
        return 4
    }
    if (selectedCandidateDownloadId.isNotBlank()) {
        return 3
    }
    if (candidateCount > 0) {
        return 3
    }
    return when (effectiveProgressStatus(job)) {
        "ready-for-download" -> 4
        "waiting-for-review",
        "revision-requested",
        "candidate-rejected" -> 3
        "queued",
        "processing",
        "waiting-for-worker-output",
        "packaging" -> 2
        else -> 1
    }
}

private fun hatcheryRandomSeed(): Int =
    (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

@Composable
private fun GenerationPanel(
    strings: PetShellStrings,
    walletBalance: Int,
    hatchSla: HatchSla,
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
    onMysteryHatch: () -> Unit,
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
        HatcheryOverviewPanel(
            strings = strings,
            walletBalance = walletBalance,
            hatchSla = hatchSla,
            job = job,
            candidateCount = candidates.size,
            selectedCandidateDownloadId = selectedCandidateDownloadId,
            onMysteryHatch = onMysteryHatch
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = strings.generationBriefPanelContentDescription
                },
            color = Color.White,
            shape = GamerUiTokens.Shape.Card,
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
                color = GamerUiTokens.ColorRole.Muted
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GamerUiTokens.Shape.Card)
                    .background(GamerUiTokens.ColorRole.Raised)
                    .semantics {
                        contentDescription = strings.generationPromptCanvasContentDescription
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
            GenerationBriefStageHeader(
                title = strings.generationPromptStageTitle,
                detail = strings.generationPromptStageHint,
                accent = GamerUiTokens.ColorRole.Identity,
                actionLabel = strings.generationPromptIdeaAction,
                actionContentDescription = strings.generationPromptIdeaContentDescription,
                onAction = {
                    onDescriptionChange(strings.hatcheryRandomPrompt(hatcheryRandomSeed()))
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
                accent = GamerUiTokens.ColorRole.Review
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
                accent = GamerUiTokens.ColorRole.Reward
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
                    accent = GamerUiTokens.ColorRole.Warning,
                    container = GamerUiTokens.ColorRole.WarningSoft
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
                accent = GamerUiTokens.ColorRole.Subtle
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
                    color = GamerUiTokens.ColorRole.Warning
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
            color = GamerUiTokens.ColorRole.IdentitySoft.copy(alpha = 0.38f),
            shape = GamerUiTokens.Shape.Card,
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
                color = GamerUiTokens.ColorRole.Subtle
            )
            val summaryLine = job?.let { generationProgressSummaryLine(it) }.orEmpty()
            if (summaryLine.isNotBlank() && summaryLine != message) {
                Text(
                    text = strings.generationMessage(summaryLine),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Success
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
                    color = GamerUiTokens.ColorRole.EggCrack
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
                    color = GamerUiTokens.ColorRole.Warning
                )
            }
            if (workerReadinessMessage.isNotBlank()) {
                Text(
                    text = strings.generationMessage(workerReadinessMessage),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted
                )
            }
            if (progressSteps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (step in progressSteps) {
                        Text(
                            text = "${strings.progressLabel(step.label)}: ${strings.progressStatus(step.status)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GamerUiTokens.ColorRole.Subtle
                        )
                        if (step.message.isNotBlank()) {
                            Text(
                                text = strings.generationMessage(step.message),
                                style = MaterialTheme.typography.labelSmall,
                                color = GamerUiTokens.ColorRole.Muted
                            )
                        }
                    }
                }
            }
            if (job != null) {
                Text(
                    text = strings.jobLabel(job.appJobId),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted
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
            shape = GamerUiTokens.Shape.Card,
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
                accent = GamerUiTokens.ColorRole.Review
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

            SelectedActionReviewConsole(
                strings = strings,
                state = selectedActionReviewConsoleUiState(
                    job = job,
                    candidates = candidates,
                    selectedCandidateDownloadId = selectedCandidateDownloadId
                )
            )

            if (candidates.isNotEmpty()) {
                GenerationReviewStageHeader(
                    title = strings.reviewNotesStageTitle,
                    detail = strings.reviewNotesPlaceholder,
                    accent = GamerUiTokens.ColorRole.Reward
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GamerUiTokens.ColorRole.Raised,
                    shape = GamerUiTokens.Shape.Card,
                    border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Line)
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
                accent = GamerUiTokens.ColorRole.Identity
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = strings.generationReviewActionDockContentDescription
                    },
                color = GamerUiTokens.ColorRole.Raised,
                shape = GamerUiTokens.Shape.Card,
                border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Line)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GenerationReviewLoopNotice(
                        strings = strings,
                        state = generationReviewLoopUiState(
                            job = job,
                            candidates = candidates,
                            selectedCandidateDownloadId = selectedCandidateDownloadId
                        )
                    )
                    GenerationDeliveryStatusStrip(
                        strings = strings,
                        selectedCandidateDownloadId = selectedCandidateDownloadId,
                        selectedCandidateLabel = selectedCandidateDeliveryLabel(
                            candidates = candidates,
                            selectedCandidateDownloadId = selectedCandidateDownloadId,
                            strings = strings
                        ),
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
                                color = GamerUiTokens.ColorRole.Muted
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
                            color = GamerUiTokens.ColorRole.Subtle
                        )
                    }
                    if (packageImportCandidateMessage.isNotBlank()) {
                        Text(
                            text = strings.generationMessage(packageImportCandidateMessage),
                            style = MaterialTheme.typography.labelSmall,
                            color = GamerUiTokens.ColorRole.Success
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
                            color = GamerUiTokens.ColorRole.Subtle
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun GenerationReviewLoopNotice(
    strings: PetShellStrings,
    state: GenerationReviewLoopUiState
) {
    val accent = when (state.phase) {
        GenerationReviewLoopPhase.PackageReady,
        GenerationReviewLoopPhase.AcceptedPackaging -> GamerUiTokens.ColorRole.Success
        GenerationReviewLoopPhase.ReadyForHumanReview,
        GenerationReviewLoopPhase.SelectMotionCandidate -> GamerUiTokens.ColorRole.Review
        GenerationReviewLoopPhase.ReworkRequested -> GamerUiTokens.ColorRole.Warning
        GenerationReviewLoopPhase.WaitingForServer,
        GenerationReviewLoopPhase.WaitingForCandidate -> GamerUiTokens.ColorRole.Reward
        GenerationReviewLoopPhase.Empty,
        GenerationReviewLoopPhase.PackageLocked -> GamerUiTokens.ColorRole.Muted
    }
    val container = when (state.phase) {
        GenerationReviewLoopPhase.PackageReady,
        GenerationReviewLoopPhase.AcceptedPackaging -> GamerUiTokens.ColorRole.IdentitySoft.copy(alpha = 0.54f)
        GenerationReviewLoopPhase.ReadyForHumanReview,
        GenerationReviewLoopPhase.SelectMotionCandidate -> GamerUiTokens.ColorRole.ReviewSoft
        GenerationReviewLoopPhase.ReworkRequested -> GamerUiTokens.ColorRole.WarningSoft
        GenerationReviewLoopPhase.WaitingForServer,
        GenerationReviewLoopPhase.WaitingForCandidate -> GamerUiTokens.ColorRole.RewardSoft.copy(alpha = 0.62f)
        GenerationReviewLoopPhase.Empty,
        GenerationReviewLoopPhase.PackageLocked -> Color.White
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.generationReviewLoopStatusContentDescription
            },
        color = container,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 30.dp)
                        .clip(GamerUiTokens.Shape.Control)
                        .background(accent)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = strings.generationReviewLoopTitle(state.title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GamerUiTokens.ColorRole.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.generationReviewLoopDetail(state.detail),
                        style = MaterialTheme.typography.labelSmall,
                        color = GamerUiTokens.ColorRole.Subtle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ReviewStatePill(
                    text = strings.generationReviewLoopAction(state.primaryAction),
                    selected = state.phase == GenerationReviewLoopPhase.ReadyForHumanReview ||
                        state.phase == GenerationReviewLoopPhase.PackageReady
                )
            }
            if (state.selectedActionId.isNotBlank()) {
                Text(
                    text = strings.generationReviewLoopSelectedAction(state.selectedActionId),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamerUiTokens.ColorRole.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SelectedActionReviewConsole(
    strings: PetShellStrings,
    state: SelectedActionReviewConsoleUiState
) {
    if (!state.visible) {
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = strings.selectedActionReviewConsoleContentDescription
            },
        color = GamerUiTokens.ColorRole.ReviewSoft,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Review.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings.selectedActionReviewConsoleTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink
            )
            Text(
                text = strings.selectedActionReviewStateTitle(state.title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Review,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.selectedActionReviewHeadline(state.actionId),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GamerUiTokens.ColorRole.Review,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ReviewStatePill(
                    text = strings.selectedActionReviewNextStep(state.nextStep),
                    selected = state.available
                )
            }
            Text(
                text = strings.selectedActionReviewConsoleHint,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = strings.selectedActionReviewSource(state.source),
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Subtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            for (check in state.checks) {
                SelectedActionReviewCheckRow(
                    title = strings.selectedActionReviewCheckTitle(check.title),
                    detail = strings.selectedActionReviewCheckDetail(check.detail)
                )
            }
        }
    }
}

@Composable
private fun SelectedActionReviewCheckRow(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GamerUiTokens.Shape.Card)
            .background(Color.White.copy(alpha = 0.74f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(width = 4.dp, height = 28.dp)
                .clip(GamerUiTokens.Shape.Control)
                .background(GamerUiTokens.ColorRole.Review)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GamerUiTokens.ColorRole.Ink
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Subtle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GenerationDeliveryStatusStrip(
    strings: PetShellStrings,
    selectedCandidateDownloadId: String,
    selectedCandidateLabel: String,
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
                strings.deliveryReviewTargetValue(selectedCandidateLabel)
            },
            accent = GamerUiTokens.ColorRole.Review,
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
            accent = GamerUiTokens.ColorRole.Reward,
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
            accent = GamerUiTokens.ColorRole.Identity,
            active = communityDraftReady,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun selectedCandidateDeliveryLabel(
    candidates: List<CandidateGalleryItem>,
    selectedCandidateDownloadId: String,
    strings: PetShellStrings
): String {
    val selectedIndex = candidates.indexOfFirst { candidate ->
        candidate.targetDownloadId == selectedCandidateDownloadId
    }
    if (selectedIndex < 0) {
        return ""
    }
    val selected = candidates[selectedIndex]
    return selected.actionId.trim()
        .takeIf { it.isNotBlank() }
        ?.let { actionId -> strings.candidateActionSummary(actionId) }
        ?: strings.candidateTitle(selected.title, selectedIndex)
}

@Composable
private fun GenerationDeliveryStatusToken(
    label: String,
    value: String,
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = if (active) accent else GamerUiTokens.ColorRole.Muted
    Surface(
        modifier = modifier.height(52.dp),
        color = if (active) accent.copy(alpha = 0.12f) else Color.White,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.30f) else GamerUiTokens.ColorRole.Line)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Muted,
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
                .clip(GamerUiTokens.Shape.Control)
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
                color = GamerUiTokens.ColorRole.Ink
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Muted,
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
        shape = GamerUiTokens.Shape.Card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 24.dp)
                    .clip(GamerUiTokens.Shape.Control)
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
                .clip(GamerUiTokens.Shape.Control)
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
                color = GamerUiTokens.ColorRole.Ink
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Muted,
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
        color = if (selected) GamerUiTokens.ColorRole.ReviewSoft else GamerUiTokens.ColorRole.Raised,
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(
            1.dp,
            if (selected) GamerUiTokens.ColorRole.Review.copy(alpha = 0.38f) else GamerUiTokens.ColorRole.Line
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
                    color = GamerUiTokens.ColorRole.Ink
                )
                ReviewStatePill(
                    text = when {
                        candidate.reviewed -> strings.candidateReviewedStatus
                        selected -> strings.candidateSelectedStatus
                        else -> strings.candidateAvailableStatus
                    },
                    selected = selected && !candidate.reviewed
                )
            }
            Text(
                text = strings.candidateActionFocus(candidate.actionId),
                style = MaterialTheme.typography.labelSmall,
                color = GamerUiTokens.ColorRole.Subtle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            RemoteCandidatePreview(
                candidate = candidate,
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
                enabled = !selected && !candidate.reviewed
            ) {
                Text(
                    when {
                        candidate.reviewed -> strings.reviewedCandidateLocked
                        selected -> strings.selectedForReview
                        else -> strings.selectCandidate
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
        color = if (selected) GamerUiTokens.ColorRole.Review else GamerUiTokens.ColorRole.NeutralPill,
        shape = GamerUiTokens.Shape.Control
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else GamerUiTokens.ColorRole.Muted,
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
        color = GamerUiTokens.ColorRole.RewardSoft.copy(alpha = 0.62f),
        shape = GamerUiTokens.Shape.Card,
        border = BorderStroke(1.dp, GamerUiTokens.ColorRole.Reward.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIconMark(
                action = PetAction.Review,
                modifier = Modifier.size(54.dp)
            )
            Text(
                text = strings.generationReviewWaitingForCandidate,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = GamerUiTokens.ColorRole.RewardDark
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
    if (canShowPackageDownload(job)) {
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
private fun RemoteCandidatePreview(
    candidate: CandidateGalleryItem,
    strings: PetShellStrings
) {
    if (candidate.mediaType.equals("text/html", ignoreCase = true)) {
        RemoteCandidateWebPreview(
            previewUrl = candidate.previewUrl,
            strings = strings
        )
    } else {
        RemoteCandidateImage(
            previewUrl = candidate.previewUrl,
            strings = strings
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RemoteCandidateWebPreview(
    previewUrl: String,
    strings: PetShellStrings
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFF3F7))
            .semantics {
                contentDescription = strings.candidatePreviewContentDescription
            },
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            if (previewUrl.isBlank()) {
                webView.loadData(
                    "<html><body>${strings.previewUnavailable}</body></html>",
                    "text/html",
                    "UTF-8"
                )
            } else if (webView.url != previewUrl) {
                webView.loadUrl(previewUrl)
            }
        }
    )
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
            .height(150.dp),
        animateHorizontalSpritesheet = true
    )
}

@Composable
private fun ApprovedPetPreviewArtwork(
    pet: ApprovedPet?,
    action: PetAction,
    strings: PetShellStrings,
    modifier: Modifier = Modifier,
    loading: @Composable (() -> Unit)? = null
) {
    val previewUrl = approvedPetPreviewUrl(pet)

    if (previewUrl.isBlank()) {
        StatusIconMark(action = action, modifier = modifier)
        return
    }

    RemotePreviewImage(
        previewUrl = previewUrl,
        strings = strings,
        modifier = modifier,
        cropFirstSpritesheetFrame = true,
        loading = loading,
        unavailable = {
            StatusIconMark(action = action, modifier = Modifier.fillMaxSize())
        }
    )
}

@Composable
private fun DefaultDesktopPetPreviewArtwork(
    pet: DefaultDesktopPet?,
    action: PetAction,
    strings: PetShellStrings,
    modifier: Modifier = Modifier,
    loading: @Composable (() -> Unit)? = null
) {
    val previewAssetPath = pet?.previewAssetPath.orEmpty()
    val motionSheet = pet?.motionSheetFor(action)

    if (previewAssetPath.isBlank()) {
        StatusIconMark(action = action, modifier = modifier)
        return
    }

    if (motionSheet != null) {
        LocalMotionSheetImage(
            assetPath = motionSheet.assetPath,
            frameCount = motionSheet.frameCount,
            loop = motionSheet.loop,
            strings = strings,
            modifier = modifier,
            loading = loading,
            unavailable = {
                LocalAssetPreviewImage(
                    assetPath = previewAssetPath,
                    strings = strings,
                    modifier = Modifier.fillMaxSize(),
                    loading = loading,
                    unavailable = {
                        StatusIconMark(action = action, modifier = Modifier.fillMaxSize())
                    }
                )
            }
        )
        return
    }

    LocalAssetPreviewImage(
        assetPath = previewAssetPath,
        strings = strings,
        modifier = modifier,
        loading = loading,
        unavailable = {
            StatusIconMark(action = action, modifier = Modifier.fillMaxSize())
        }
    )
}

@Composable
private fun LocalAssetPreviewImage(
    assetPath: String,
    strings: PetShellStrings,
    modifier: Modifier = Modifier,
    loading: @Composable (() -> Unit)? = null,
    unavailable: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    var image by remember(assetPath) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(assetPath) { mutableStateOf(false) }

    LaunchedEffect(context, assetPath) {
        image = null
        failed = false
        if (assetPath.isBlank()) {
            failed = true
            return@LaunchedEffect
        }
        image = withContext(Dispatchers.Default) {
            runCatching {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
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
        } else if (!failed && loading != null) {
            loading()
        } else if (failed && unavailable != null) {
            unavailable()
        } else {
            Text(
                text = if (failed) strings.previewUnavailable else strings.loadingPreview,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LocalMotionSheetImage(
    assetPath: String,
    frameCount: Int,
    loop: Boolean,
    strings: PetShellStrings,
    modifier: Modifier = Modifier,
    frameDurationMillis: Long = 83L,
    loading: @Composable (() -> Unit)? = null,
    unavailable: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    var frames by remember(assetPath, frameCount) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var frameIndex by remember(assetPath, frameCount) { mutableStateOf(0) }
    var failed by remember(assetPath, frameCount) { mutableStateOf(false) }

    LaunchedEffect(context, assetPath, frameCount) {
        frames = emptyList()
        frameIndex = 0
        failed = false
        if (assetPath.isBlank() || frameCount <= 1) {
            failed = true
            return@LaunchedEffect
        }

        frames = withContext(Dispatchers.Default) {
            runCatching {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)
                        ?.horizontalSpritesheetFrames(frameCount)
                        ?.map(Bitmap::asImageBitmap)
                        .orEmpty()
                }
            }.getOrDefault(emptyList())
        }
        failed = frames.isEmpty()
    }

    LaunchedEffect(assetPath, frames.size, frameDurationMillis, loop) {
        frameIndex = 0
        if (frames.size <= 1) {
            return@LaunchedEffect
        }

        if (loop) {
            while (true) {
                delay(frameDurationMillis)
                frameIndex = (frameIndex + 1) % frames.size
            }
        } else {
            for (nextFrameIndex in 1 until frames.size) {
                delay(frameDurationMillis)
                frameIndex = nextFrameIndex
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFF3F7)),
        contentAlignment = Alignment.Center
    ) {
        val currentFrames = frames
        if (currentFrames.isNotEmpty()) {
            Image(
                bitmap = currentFrames[frameIndex.coerceIn(0, currentFrames.lastIndex)],
                contentDescription = strings.candidatePreviewContentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (!failed && loading != null) {
            loading()
        } else if (failed && unavailable != null) {
            unavailable()
        } else {
            Text(
                text = if (failed) strings.previewUnavailable else strings.loadingPreview,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RemotePreviewImage(
    previewUrl: String,
    strings: PetShellStrings,
    modifier: Modifier = Modifier,
    cropFirstSpritesheetFrame: Boolean = false,
    animateHorizontalSpritesheet: Boolean = false,
    frameDurationMillis: Long = 120L,
    loading: @Composable (() -> Unit)? = null,
    unavailable: @Composable (() -> Unit)? = null
) {
    var image by remember(previewUrl) { mutableStateOf<ImageBitmap?>(null) }
    var frames by remember(previewUrl) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var frameIndex by remember(previewUrl) { mutableStateOf(0) }
    var failed by remember(previewUrl) { mutableStateOf(false) }
    val previewDownloader = remember { FantasyPetPreviewDownloader() }

    LaunchedEffect(previewUrl) {
        image = null
        frames = emptyList()
        frameIndex = 0
        failed = false
        if (previewUrl.isBlank()) {
            failed = true
            return@LaunchedEffect
        }
        val decodedFrames = when (val result = previewDownloader.download(previewUrl)) {
            is PetPreviewDownloadResult.Success -> {
                withContext(Dispatchers.Default) {
                    val decoded = BitmapFactory.decodeByteArray(
                        result.bytes,
                        0,
                        result.bytes.size
                    )
                    when {
                        decoded == null -> emptyList()
                        cropFirstSpritesheetFrame -> listOf(decoded.firstSpritesheetFrame().asImageBitmap())
                        animateHorizontalSpritesheet -> {
                            val frameCount = decoded.inferredHorizontalFrameCount()
                            decoded.horizontalSpritesheetFrames(frameCount)
                                .map(Bitmap::asImageBitmap)
                                .ifEmpty { listOf(decoded.asImageBitmap()) }
                        }
                        else -> listOf(decoded.asImageBitmap())
                    }
                }
            }
            is PetPreviewDownloadResult.Failure -> emptyList()
        }
        frames = decodedFrames
        image = decodedFrames.firstOrNull()
        failed = decodedFrames.isEmpty()
    }

    LaunchedEffect(previewUrl, frames.size, frameDurationMillis) {
        frameIndex = 0
        if (frames.size <= 1) {
            return@LaunchedEffect
        }

        while (true) {
            delay(frameDurationMillis)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFF3F7)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = frames
            .takeIf { it.isNotEmpty() }
            ?.get(frameIndex.coerceIn(0, frames.lastIndex))
            ?: image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = strings.candidatePreviewContentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (!failed && loading != null) {
            loading()
        } else if (failed && unavailable != null) {
            unavailable()
        } else {
            Text(
                text = if (failed) strings.previewUnavailable else strings.loadingPreview,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085)
            )
        }
    }
}

private fun Bitmap.inferredHorizontalFrameCount(): Int {
    if (width < height * 2 || height <= 0) {
        return 1
    }

    val exactSquareFrames = width / height
    return exactSquareFrames.coerceIn(1, 24)
}

@Composable
private fun DefaultDesktopPetAvatar(
    pet: DefaultDesktopPet?,
    action: PetAction,
    strings: PetShellStrings
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DefaultDesktopPetPreviewArtwork(
            pet = pet,
            action = action,
            strings = strings,
            modifier = Modifier
                .size(96.dp)
                .semantics {
                    contentDescription = strings.petAvatarContentDescription
                }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = strings.petActionLabel(action),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF475467),
            textAlign = TextAlign.Center
        )
    }
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
    approvedPetDisplayName(
        pet = pets.selectedApprovedPet(selectedIndex),
        emptyLabel = "Awaiting approved pet"
    )

internal fun approvedPetShowcaseDetail(
    pets: List<ApprovedPet>,
    selectedIndex: Int
): String {
    val pet = pets.selectedApprovedPet(selectedIndex) ?: return "Approved imports will appear here."
    return "Reviewed / score ${pet.totalScore} / ${pet.motionSheetCount} motion sheets"
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
    val pet = pets.selectedApprovedPet(selectedIndex) ?: return "Remote preview pending"
    return if (pet.hasSafeApprovedPreviewReference()) {
        "Remote preview ready"
    } else {
        "Remote preview pending"
    }
}

internal fun approvedPetShowcasePackage(
    pets: List<ApprovedPet>,
    selectedIndex: Int
): String {
    val pet = pets.selectedApprovedPet(selectedIndex) ?: return "Package pending"
    return if (pet.exportArtifactPath.trim().let { it.isNotBlank() && it.isSafeAssetDisplayText() }) {
        "Package ready"
    } else {
        "Package pending"
    }
}

private fun ApprovedPet.hasSafeApprovedPreviewReference(): Boolean =
    previewUrl.trim().let { it.isNotBlank() && approvedPetExplicitPreviewUrl(it, com.gamer.community.BuildConfig.COMMUNITY_API_BASE_URL).isNotBlank() } ||
        targetDownloadId.trim().let { it.isNotBlank() && PUBLIC_ARTIFACT_ID.matches(it) && it.isSafeAssetDisplayText() } ||
        previewPath.trim().let { it.isNotBlank() && it.isSafeAssetDisplayText() }

internal fun approvedPetPreviewUrl(
    pet: ApprovedPet?,
    baseUrl: String = com.gamer.community.BuildConfig.COMMUNITY_API_BASE_URL
): String {
    if (pet == null) {
        return ""
    }

    val explicitPreviewUrl = pet.previewUrl.trim()
    val resolvedPreviewUrl = approvedPetExplicitPreviewUrl(explicitPreviewUrl, baseUrl)
    if (resolvedPreviewUrl.isNotBlank()) {
        return resolvedPreviewUrl
    }

    if (pet.sourceKind != "fantasy-pet-rule") {
        return ""
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

internal fun desktopPetOverlayDisplayName(pet: ApprovedPet?): String {
    val displayName = pet?.displayName
        ?.trim()
        ?.replace(WHITESPACE_RUN, " ")
        .orEmpty()
    return if (
        displayName.isNotBlank() &&
        !displayName.startsWith("http://", ignoreCase = true) &&
        !displayName.startsWith("https://", ignoreCase = true) &&
        !displayName.isSafePublicArtifactRoute() &&
        displayName.isSafeAssetDisplayText() &&
        !PUBLIC_ARTIFACT_DOWNLOAD_ID.matches(displayName) &&
        !TECHNICAL_DISPLAY_NAME.containsMatchIn(displayName)
    ) {
        displayName.take(36)
    } else {
        ""
    }
}

internal fun desktopPetOverlayDisplayName(pet: DefaultDesktopPet?): String {
    val displayName = pet?.displayName
        ?.trim()
        ?.replace(WHITESPACE_RUN, " ")
        .orEmpty()
    return if (
        displayName.isNotBlank() &&
        displayName.isSafeAssetDisplayText() &&
        !TECHNICAL_DISPLAY_NAME.containsMatchIn(displayName)
    ) {
        displayName.take(36)
    } else {
        ""
    }
}

private fun approvedPetDisplayName(
    pet: ApprovedPet?,
    emptyLabel: String
): String =
    desktopPetOverlayDisplayName(pet).ifBlank { emptyLabel }

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
private val PUBLIC_ARTIFACT_DOWNLOAD_ID = Regex("artifact-\\d+", RegexOption.IGNORE_CASE)
private val TECHNICAL_DISPLAY_NAME = Regex(
    "(issue-|job-|run-|draft-|submission-|artifact-|/artifacts/|pet-generation-jobs)",
    RegexOption.IGNORE_CASE
)
private val WHITESPACE_RUN = Regex("\\s+")

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
    "server-proof-summary.json",
    "server-proof-summary",
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
