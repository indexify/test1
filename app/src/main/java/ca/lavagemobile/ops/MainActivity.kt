package ca.lavagemobile.ops

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var cameraUri: Uri? = null
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    companion object {
        private const val REQ_GEO = 1001
        private const val REQ_FILE = 1002
        private const val APP_URL = "https://ops.lavagemobile.ca/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = true
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url.isNullOrBlank()) return false
                return when {
                    url.startsWith("https://ops.lavagemobile.ca/") -> false
                    url.startsWith("https://") || url.startsWith("http://") -> {
                        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))); true }
                        catch (_: Exception) { false }
                    }
                    else -> {
                        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))); true }
                        catch (_: Exception) { false }
                    }
                }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                if (origin == null || callback == null) return
                val granted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (granted) callback.invoke(origin, true, false)
                else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    requestPermissions(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), REQ_GEO)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback

                val mime = fileChooserParams?.acceptTypes
                    ?.firstOrNull { !it.isNullOrBlank() }
                    ?.takeIf { it != "*/*" } ?: "image/*"

                val openIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mime
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                }

                val intents = mutableListOf<Intent>()
                if (mime.startsWith("image/") || mime == "*/*") {
                    try {
                        val photoFile = File.createTempFile("lmops-photo-", ".jpg", cacheDir)
                        val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", photoFile)
                        cameraUri = uri
                        intents += Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, uri)
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } catch (_: Exception) {
                        cameraUri = null
                    }
                }

                val chooser = Intent.createChooser(openIntent, "Ajouter une photo").apply {
                    if (intents.isNotEmpty()) putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
                }
                startActivityForResult(chooser, REQ_FILE)
                return true
            }
        }

        web.loadUrl(APP_URL)
        setContentView(web)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_GEO) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            val origin = pendingGeoOrigin
            val callback = pendingGeoCallback
            if (origin != null && callback != null) callback.invoke(origin, granted, false)
            pendingGeoOrigin = null
            pendingGeoCallback = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_FILE) return
        val result = if (resultCode == RESULT_OK) {
            when {
                data?.clipData != null -> Array(data.clipData!!.itemCount) { i -> data.clipData!!.getItemAt(i).uri }
                data?.data != null -> arrayOf(data.data!!)
                cameraUri != null -> arrayOf(cameraUri!!)
                else -> null
            }
        } else null
        fileCallback?.onReceiveValue(result)
        fileCallback = null
        cameraUri = null
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.stopLoading()
            web.destroy()
        }
        super.onDestroy()
    }
}
