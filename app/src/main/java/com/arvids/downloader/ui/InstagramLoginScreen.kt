package com.arvids.downloader.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
 *
 * The WebView uses a normal mobile Chrome user-agent (no "wv" marker),
 * otherwise Instagram refuses to render its login page inside an app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit
) {
    var loggedIn by remember { mutableStateOf(InstagramSession.isLoggedIn) }
    var checkMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        // Normal mobile Chrome UA: Instagram blocks the
                        // default WebView UA (it contains "wv").
                        settings.userAgentString =
                            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                isLoading = true
                                loadError = false
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                isLoading = false
                                refreshSession(silent = true)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                if (request.isForMainFrame) {
                                    isLoading = false
                                    loadError = true
                                }
                            }
                        }
                        loadUrl("https://www.instagram.com/accounts/login/")
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF7C3AED))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Instagram login page load ho rahi hai...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
            if (loadError) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Login page load nahi hui. Internet check kar ke dobara try karo.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            loadError = false
                            isLoading = true
                            webView?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C3AED)
                        )
                    ) {
                        Text("Dobara try karo")
                    }
                }
            }
        }
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
