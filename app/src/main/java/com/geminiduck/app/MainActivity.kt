package com.geminiduck.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: View
    private lateinit var btnRetry: Button

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingSharedText: String? = null

    companion object {
        private const val GEMINI_URL = "https://gemini.google.com"
        // Czystszy User-Agent bez sygnatur WebView ('wv', 'Version/4.0'), omijający błąd 403 Google OAuth
        private const val CHROME_MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uris = when {
                data?.clipData != null -> {
                    val clipData = data.clipData!!
                    Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
            fileUploadCallback?.onReceiveValue(uris)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    private val requestRecordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingPermissionRequest?.let { request ->
                request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
            }
        } else {
            pendingPermissionRequest?.deny()
            Toast.makeText(this, getString(R.string.permission_denied_mic), Toast.LENGTH_SHORT).show()
        }
        pendingPermissionRequest = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupCookies()
        setupWebView()
        setupBackNavigation()

        handleSendIntent(intent)

        webView.loadUrl(GEMINI_URL)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSendIntent(intent)
    }

    private fun handleSendIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        if (intent.type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            if (!text.isNullOrBlank()) {
                pendingSharedText = text
                injectPendingSharedText()
            }
        } else if (intent.type?.startsWith("image/") == true) {
            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }

            if (imageUri != null) {
                try {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val clip = android.content.ClipData.newUri(contentResolver, "Shared Image", imageUri)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(this, "Obraz skopiowany do schowka! Możesz wkleić go w czacie Gemini lub załączyć plik.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun injectPendingSharedText() {
        val text = pendingSharedText ?: return
        if (webView.url?.contains("gemini.google.com") == true) {
            pendingSharedText = null
            val escapedText = JSONObject.quote(text)
            webView.evaluateJavascript("window.insertSharedText && window.insertSharedText($escapedText);", null)
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        progressBar = findViewById(R.id.progressBar)
        errorView = findViewById(R.id.errorView)
        btnRetry = findViewById(R.id.btnRetry)

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // SwipeRefresh tylko jeśli strona jest przewinięta na samą górę
        webView.viewTreeObserver.addOnScrollChangedListener {
            swipeRefreshLayout.isEnabled = (webView.scrollY == 0)
        }

        btnRetry.setOnClickListener {
            errorView.visibility = View.GONE
            webView.visibility = View.VISIBLE
            webView.reload()
        }
    }

    private fun setupCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        // Bypassing Google OAuth 403 disallowed_useragent
        settings.userAgentString = CHROME_MOBILE_UA

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val uri = Uri.parse(url)
                val host = uri.host ?: ""

                // Pozwalamy na ładowanie domen Google w WebView (logowanie, Gemini, gstatic itp.)
                if (host.contains("google.com") || host.contains("gstatic.com") || host.contains("googleusercontent.com")) {
                    return false
                }

                // Wszystkie linki zewnętrzne otwieramy w domyślnej przeglądarce telefonu
                return try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(browserIntent)
                    true
                } catch (e: Exception) {
                    false
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefreshLayout.isRefreshing = false

                if (url?.contains("gemini.google.com") == true) {
                    injectCustomStylesAndScripts()
                    injectPendingSharedText()
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                if (failingUrl?.contains("gemini.google.com") == true) {
                    webView.visibility = View.GONE
                    errorView.visibility = View.VISIBLE
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            // Obsługa mikrofonu (WebRTC / głos Gemini)
            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val requestedResources = request.resources

                if (requestedResources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    val hasAudioPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasAudioPermission) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    } else {
                        pendingPermissionRequest = request
                        requestRecordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                } else {
                    request.deny()
                }
            }

            // Obsługa przesyłania plików i zdjęć
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }

                return try {
                    filePickerLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    fileUploadCallback = null
                    false
                }
            }
        }
    }

    private fun injectCustomStylesAndScripts() {
        try {
            // 1. Wstrzyknięcie stylu CSS (AMOLED Black)
            val cssContent = assets.open("gemini_style.css").bufferedReader().use { it.readText() }
            val encodedCss = Base64.encodeToString(cssContent.toByteArray(), Base64.NO_WRAP)
            val cssInjectionScript = """
                (function() {
                    var parent = document.head || document.documentElement;
                    var style = document.getElementById('geminiduck-custom-css');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'geminiduck-custom-css';
                        style.type = 'text/css';
                        style.innerHTML = window.atob('$encodedCss');
                        parent.appendChild(style);
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(cssInjectionScript, null)

            // 2. Wstrzyknięcie skryptu JavaScript (Usprawnienia UX)
            val jsContent = assets.open("gemini_tweaks.js").bufferedReader().use { it.readText() }
            webView.evaluateJavascript(jsContent, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        webView.destroy()
        super.onDestroy()
    }
}
