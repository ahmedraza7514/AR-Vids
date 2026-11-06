package com.arvids.downloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.arvids.downloader.ui.ArVidsTheme
import com.arvids.downloader.ui.HomeScreen
import com.arvids.downloader.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            ArVidsTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Shared text from Instagram / YouTube / Facebook / X share sheet.
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            val url = Regex("https?://\\S+").find(text)?.value ?: text
            openLink(url)
            return
        }
        // Link tapped in a browser or chat (ACTION_VIEW deep link).
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.data?.toString() ?: return
            openLink(url)
        }
    }

    private fun openLink(url: String) {
        viewModel.setLink(url)
        viewModel.detectLink(url)
    }
}
