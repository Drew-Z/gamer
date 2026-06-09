package com.gamer.community

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamer.community.api.CommunityRepository
import com.gamer.community.api.HttpCommunityApiClient
import com.gamer.community.generation.FantasyPetGenerationService
import com.gamer.community.generation.HttpFantasyPetGenerationClient
import com.gamer.community.ui.PetShellApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyGamerSystemBars(window)

        val repository = CommunityRepository(
            client = HttpCommunityApiClient(BuildConfig.COMMUNITY_API_BASE_URL)
        )
        val generationService = FantasyPetGenerationService(
            client = HttpFantasyPetGenerationClient(BuildConfig.FANTASY_PET_API_BASE_URL),
            apiBaseUrl = BuildConfig.FANTASY_PET_API_BASE_URL
        )

        setContent {
            PetShellApp(repository = repository, generationService = generationService)
        }
    }
}
