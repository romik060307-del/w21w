package id.rk.serverguard

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.Collator
import java.util.Locale

private const val PREFS_NAME = "server_guard_prefs"
private const val REQUEST_NOTIFICATIONS = 410

class MainActivity : Activity() {
    private lateinit var serverOneSpinner: Spinner
    private lateinit var serverTwoSpinner: Spinner
    private lateinit var keepCpuSwitch: Switch
    private lateinit var statusText: TextView
    private lateinit var apps: List<AppEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 23, 34)
        window.navigationBarColor = Color.rgb(16, 23, 34)
        window.decorView.systemUiVisibility = 0

        apps = loadLaunchableApps()
        buildScreen()
        restoreSelections()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) refreshStatus()
    }

    private fun buildScreen() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(16, 23, 34))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        content.addView(text("RK SERVER GUARD", 13, Color.rgb(105, 210, 196), true))
        content.addView(space(8))
        content.addView(text("Jaga server saat layar mati", 27, Color.WHITE, true))
        content.addView(space(8))
        content.addView(text(
            "Pilih sampai dua APK server. Buka masing-masing server terlebih dahulu agar prosesnya mulai, lalu aktifkan penjaga ini sebelum layar dipadamkan.",
            14, Color.rgb(190, 200, 214), false
        ))
        content.addView(space(20))

        statusText = text("Penjagaan belum aktif", 14, Color.rgb(255, 220, 145), true).apply {
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = rounded(Color.rgb(49, 43, 31), 14)
        }
        content.addView(statusText, matchWidth())
        content.addView(space(18))

        val cardOne = serverCard("SERVER 1", "Pilih APK server pertama", true)
        serverOneSpinner = cardOne.first
        content.addView(cardOne.second)
        content.addView(space(12))

        val cardTwo = serverCard("SERVER 2", "Pilih APK server kedua", false)
        serverTwoSpinner = cardTwo.first
        content.addView(cardTwo.second)
        content.addView(space(18))

        val powerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(Color.rgb(28, 39, 54), 16)
        }
        keepCpuSwitch = Switch(this).apply {
            text = "Jaga CPU saat layar mati"
            textSize = 15f
            setTextColor(Color.WHITE)
            isChecked = true
            showText = false
        }
        powerCard.addView(keepCpuSwitch, matchWidth())
        powerCard.addView(space(7))
        powerCard.addView(text(
            "Aktifkan agar proses penjaga tetap mendapat waktu CPU. Ini dapat membuat baterai lebih cepat habis.",
            12, Color.rgb(176, 190, 205), false
        ))
        content.addView(powerCard, matchWidth())
        content.addView(space(12))

        val batteryButton = makeButton("Izinkan RK Server Guard berjalan saat tidur", accent = false) {
            requestOwnBatteryExemption()
        }
        content.addView(batteryButton, matchWidth())
        content.addView(space(8))

        val startButton = makeButton("MULAI PENJAGAAN", accent = true) {
            requestNotificationPermissionThenStart()
        }
        content.addView(startButton, matchWidth())
        content.addView(space(9))

        val stopButton = makeButton("HENTIKAN PENJAGAAN", accent = false) {
            stopProtection()
        }
        content.addView(stopButton, matchWidth())
        content.addView(space(18))

        val warning = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            background = rounded(Color.rgb(38, 47, 61), 14)
        }
        warning.addView(text("PENTING", 12, Color.rgb(255, 205, 125), true))
        warning.addView(space(6))
        warning.addView(text(
            "Aplikasi ini menjaga CPU dan menyediakan jalan pintas ke pengaturan APK pilihan. Android tetap dapat menghentikan aplikasi server lain. Untuk hasil paling kuat, layanan server harus dipasang langsung di dalam kode APK server tersebut. Status penjagaan di sini bukan bukti bahwa server tujuan sedang merespons.",
            12, Color.rgb(202, 211, 222), false
        ))
        content.addView(warning, matchWidth())
        content.addView(space(14))
        content.addView(text(
            "Tips Vivo: buka Info APK untuk masing-masing server, lalu izinkan aktivitas latar belakang / penggunaan baterai tanpa pembatasan jika pengaturan itu tersedia. Aktifkan juga Autostart bila tersedia.",
            12, Color.rgb(160, 175, 193), false
        ))

        scroll.addView(content)
        outer.addView(scroll, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        setContentView(outer)
    }

    private fun serverCard(title: String, subtitle: String, first: Boolean): Pair<Spinner, View> {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(Color.rgb(28, 39, 54), 16)
        }
        card.addView(text(title, 12, Color.rgb(105, 210, 196), true))
        card.addView(space(4))
        card.addView(text(subtitle, 15, Color.WHITE, true))
        card.addView(space(10))

        val spinner = Spinner(this).apply {
            adapter = AppEntryAdapter(this@MainActivity, apps)
            background = rounded(Color.rgb(43, 56, 74), 10)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        card.addView(spinner, LinearLayout.LayoutParams(matchParentWidth(), dp(50)))
        card.addView(space(10))

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val openButton = makeSmallButton("Buka APK") {
            openSelectedApp(if (first) serverOneSpinner else serverTwoSpinner)
        }
        val settingsButton = makeSmallButton("Info APK / baterai") {
            openSelectedAppSettings(if (first) serverOneSpinner else serverTwoSpinner)
        }
        buttons.addView(openButton, LinearLayout.LayoutParams(0, dp(44), 1f))
        buttons.addView(spaceHorizontal(8))
        buttons.addView(settingsButton, LinearLayout.LayoutParams(0, dp(44), 1.35f))
        card.addView(buttons)
        return spinner to card
    }

    private fun loadLaunchableApps(): List<AppEntry> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = packageManager.queryIntentActivities(query, 0)
        val collator = Collator.getInstance(Locale.getDefault())
        val result = resolved
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isBlank()) null else AppEntry(label, packageName)
            }
            .filter { it.packageName != this.packageName }
            .distinctBy { it.packageName }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
        return listOf(AppEntry("— Pilih aplikasi server —", null)) + result
    }

    private fun restoreSelections() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        keepCpuSwitch.isChecked = prefs.getBoolean("keep_cpu", true)
        selectPackage(serverOneSpinner, prefs.getString("package_one", null))
        selectPackage(serverTwoSpinner, prefs.getString("package_two", null))
    }

    private fun selectPackage(spinner: Spinner, packageName: String?) {
        if (packageName == null) return
        val adapter = spinner.adapter as AppEntryAdapter
        val index = (0 until adapter.count).firstOrNull { adapter.getItem(it)?.packageName == packageName }
        if (index != null) spinner.setSelection(index)
    }

    private fun selectedPackage(spinner: Spinner): String? =
        (spinner.selectedItem as? AppEntry)?.packageName

    private fun requestNotificationPermissionThenStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
            return
        }
        startProtection()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                startProtection()
            } else {
                toast("Izin notifikasi diperlukan agar status penjagaan terlihat.")
            }
        }
    }

    private fun startProtection() {
        val one = selectedPackage(serverOneSpinner)
        val two = selectedPackage(serverTwoSpinner)
        if (one == null && two == null) {
            toast("Pilih minimal satu APK server terlebih dahulu.")
            return
        }
        if (one != null && one == two) {
            toast("Pilih dua APK yang berbeda, atau kosongkan Server 2.")
            return
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putString("package_one", one)
            .putString("package_two", two)
            .putBoolean("keep_cpu", keepCpuSwitch.isChecked)
            .apply()

        val serviceIntent = Intent(this, KeeperService::class.java).apply {
            action = KeeperService.ACTION_START
            putExtra(KeeperService.EXTRA_PACKAGE_ONE, one)
            putExtra(KeeperService.EXTRA_PACKAGE_TWO, two)
            putExtra(KeeperService.EXTRA_KEEP_CPU, keepCpuSwitch.isChecked)
        }
        try {
            startForegroundService(serviceIntent)
            toast("Penjagaan dimulai. Periksa notifikasi RK Server Guard.")
            refreshStatus(forceActive = true)
        } catch (error: Exception) {
            toast("Gagal memulai penjagaan: ${error.message ?: "izin atau sistem menolak"}")
        }
    }

    private fun stopProtection() {
        startService(Intent(this, KeeperService::class.java).setAction(KeeperService.ACTION_STOP))
        refreshStatus(forceActive = false)
        toast("Perintah menghentikan penjagaan dikirim.")
    }

    private fun openSelectedApp(spinner: Spinner) {
        val packageName = selectedPackage(spinner)
        if (packageName == null) {
            toast("Pilih APK terlebih dahulu.")
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) {
            toast("APK tidak memiliki halaman utama yang bisa dibuka.")
            return
        }
        try {
            startActivity(launch)
        } catch (_: Exception) {
            toast("Tidak bisa membuka APK tersebut.")
        }
    }

    private fun openSelectedAppSettings(spinner: Spinner) {
        val packageName = selectedPackage(spinner)
        if (packageName == null) {
            toast("Pilih APK terlebih dahulu.")
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            toast("Pengaturan aplikasi tidak dapat dibuka.")
        }
    }

    private fun requestOwnBatteryExemption() {
        if (Build.VERSION.SDK_INT < 23) {
            toast("Pengaturan ini tidak diperlukan pada versi Android ini.")
            return
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            toast("RK Server Guard sudah dikecualikan dari optimasi baterai.")
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun refreshStatus(forceActive: Boolean? = null) {
        val active = forceActive ?: isKeeperServiceRunning()
        statusText.text = if (active) {
            "● Penjagaan aktif — lihat notifikasi untuk menghentikan"
        } else {
            "○ Penjagaan belum aktif"
        }
        statusText.setTextColor(if (active) Color.rgb(133, 230, 190) else Color.rgb(255, 220, 145))
        statusText.background = rounded(
            if (active) Color.rgb(25, 57, 49) else Color.rgb(49, 43, 31), 14
        )
    }

    @Suppress("DEPRECATION")
    private fun isKeeperServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == KeeperService::class.java.name
        }
    }

    private fun makeButton(label: String, accent: Boolean, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (accent) Color.rgb(13, 30, 34) else Color.WHITE)
            background = rounded(
                if (accent) Color.rgb(105, 210, 196) else Color.rgb(48, 64, 84), 12
            )
            setOnClickListener { action() }
        }

    private fun makeSmallButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 11f
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(48, 64, 84), 10)
            setPadding(dp(4), 0, dp(4), 0)
            setOnClickListener { action() }
        }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun space(height: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun spaceHorizontal(width: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(width), 1)
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun matchParentWidth(): Int = LinearLayout.LayoutParams.MATCH_PARENT
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private data class AppEntry(val label: String, val packageName: String?)

    private class AppEntryAdapter(
        private val context: Context,
        private val items: List<AppEntry>
    ) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): AppEntry = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View =
            makeTextView(position, convertView, false)

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View =
            makeTextView(position, convertView, true)

        private fun makeTextView(position: Int, recycled: View?, dropdown: Boolean): View {
            val view = (recycled as? TextView) ?: TextView(context).apply {
                setPadding(
                    (14 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt()
                )
                textSize = 14f
                gravity = Gravity.CENTER_VERTICAL
            }
            view.text = items[position].label
            view.setTextColor(if (dropdown) Color.WHITE else Color.WHITE)
            if (dropdown) view.setBackgroundColor(Color.rgb(35, 48, 66))
            return view
        }
    }
}
