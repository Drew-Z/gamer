package com.gamer.community.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gamer.community.api.CommunityRepository
import com.gamer.community.api.HttpCommunityApiClient
import com.gamer.community.petshell.FeedDirection
import com.gamer.community.petshell.FeedPost
import com.gamer.community.petshell.PetAction
import com.gamer.community.petshell.PetShellController
import com.gamer.community.petshell.PetShellState
import com.gamer.community.petshell.ShellPhase
import kotlinx.coroutines.launch

@Composable
fun PetShellApp(repository: CommunityRepository) {
    var state by remember { mutableStateOf(PetShellController.initialState()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository) {
        val result = repository.loadInitialCommunity()
        state = PetShellController.applyCommunityLoad(
            state = state,
            posts = result.posts,
            walletBalance = result.walletBalance,
            usedFallback = result.usedFallback,
            message = result.message
        )
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F8FA)) {
            when (state.phase) {
                ShellPhase.LaunchBubble -> LaunchBubbleScreen(
                    state = state,
                    onBubbleTapped = {
                        state = PetShellController.onBubbleTapped(state)
                    }
                )

                ShellPhase.Community -> CommunityScreen(
                    state = state,
                    onNavigate = { direction ->
                        state = PetShellController.navigateFeed(state, direction)
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
    PetShellApp(repository = repository)
}

@Composable
private fun LaunchBubbleScreen(
    state: PetShellState,
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PetAvatar(action = state.petAction)
            Spacer(modifier = Modifier.height(18.dp))
            SpeechBubble(
                text = state.speechBubble,
                modifier = Modifier.clickable(onClick = onBubbleTapped)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tap the bubble to enter",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085)
            )
        }
    }
}

@Composable
private fun CommunityScreen(
    state: PetShellState,
    onNavigate: (FeedDirection) -> Unit,
    onCheckIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gamer Community",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pet-first feed prototype",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF667085)
                )
            }
            WalletPill(balance = state.walletBalance)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PetAvatar(action = state.petAction)
            SpeechBubble(
                text = state.speechBubble,
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = state.currentPost.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val metadataLabels = feedPostMetadataLabels(state.currentPost)
                if (metadataLabels.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (label in metadataLabels) {
                            MetadataPill(label = label)
                        }
                    }
                }
                Text(
                    text = state.currentPost.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF344054)
                )
                Text(
                    text = "${state.currentPost.authorName} · ${state.currentPost.petId} · ${state.currentPost.reactionCount} reactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF667085)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onNavigate(FeedDirection.Previous) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Prev")
            }
            Button(
                onClick = { onNavigate(FeedDirection.Next) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
            }
            Button(
                onClick = { onNavigate(FeedDirection.Skip) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
        }

        Button(
            onClick = onCheckIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.checkInClaimed
        ) {
            Text(if (state.checkInClaimed) "Checked in" else "Daily check-in")
        }
    }
}

@Composable
private fun PetAvatar(action: PetAction) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFD166), Color(0xFFEF476F))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PET",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = action.name,
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
private fun WalletPill(balance: Int) {
    Surface(
        color = Color(0xFF101828),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$balance petcoin",
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
