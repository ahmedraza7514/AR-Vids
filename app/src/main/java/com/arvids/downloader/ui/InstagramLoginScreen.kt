package com.arvids.downloader.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.arvids.downloader.extract.InstagramSession

/**
 * Instagram login inside the app. The user types their own ID and
 * password on Instagram's own login page (shown in the WebView below);
 * AR-Vids never sees or stores the password. Only the login session
 * cookies are kept on this phone, so reels can be fetched logged-in.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit
) {
    var loggedIn by remember { mutableStateOf(InstagramSession.isLoggedIn) }
    var checkMsg by remember { mutableStateOf("") }

    fun refreshSession(silent: Boolean = false): Boolean {
        val cookies = CookieManager.getInstance()
            .getCookie("https://www.instagram.com")
        InstagramSession.saveFromCookieString(cookies)
        val ok = InstagramSession.isLoggedIn
        if (ok && !loggedIn) {
            loggedIn = true
            checkMsg = "Login ho gaya — ab reels download hongi."
        } else if (!silent && !ok) {
            checkMsg = "Abhi login nahi hua — pehle Instagram mein login karo."
        }
        return ok
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B1E))
            .padding(16.dp)
    ) {
        Text(
            "Instagram Login",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Apni Instagram ID se login karo. Password Instagram ki apni page par likha jata hai — AR-Vids tumhara password nahi dekhta aur na save karta hai. Sirf login session save hota hai, taake reels download ho sakein.",
            color = Color(0xFFB0AEC9),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            refreshSession(silent = true)
                        }
                    }
                    loadUrl("https://www.instagram.com/accounts/login/")
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        if (checkMsg.isNotBlank()) {
            Text(checkMsg, color = Color(0xFF9EE6B8), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }
        Row {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Wapas")
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { if (refreshSession()) onLoggedIn() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text(if (loggedIn) "Done" else "Login check karo")
            }
        }
    }
}
