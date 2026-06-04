package com.gamer.community

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamer.community.api.CommunityRepository
import com.gamer.community.api.HttpCommunityApiClient
import com.gamer.community.ui.PetShellApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CommunityRepository(
            client = HttpCommunityApiClient(BuildConfig.COMMUNITY_API_BASE_URL)
        )

        setContent {
            PetShellApp(repository = repository)
        }
    }
}
