package id.rk.serverguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

class KeeperService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopProtection()
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val packageOne = intent?.getStringExtra(EXTRA_PACKAGE_ONE)
            ?: prefs.getString("package_one", null)
        val packageTwo = intent?.getStringExtra(EXTRA_PACKAGE_TWO)
            ?: prefs.getString("package_two", null)
        val keepCpu = intent?.getBooleanExtra(EXTRA_KEEP_CPU, prefs.getBoolean("keep_cpu", true))
            ?: prefs.getBoolean("keep_cpu", true)

        if (packageOne == null && packageTwo == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Promote immediately: Android requires foreground services to show a notification.
        startForeground(NOTIFICATION_ID, buildNotification(packageOne, packageTwo, keepCpu))
        updateWakeLock(keepCpu)
        prefs.edit().putBoolean("guard_active", true).apply()
        return START_STICKY
    }

    private fun updateWakeLock(keepCpu: Boolean) {
        if (!keepCpu) {
            releaseWakeLock()
            return
        }
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "$packageName:ServerGuardCpu"
                ).apply { setReferenceCounted(false) }
            }
            if (wakeLock?.isHeld != true) wakeLock?.acquire()
        } catch (_: Exception) {
            // The service remains visible; the system may still manage CPU sleep normally.
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: RuntimeException) {
            // Ignore a lock that the system has already released.
        } finally {
            wakeLock = null
        }
    }

    private fun buildNotification(packageOne: String?, packageTwo: String?, keepCpu: Boolean): Notification {
        val activeNames = listOfNotNull(
            packageOne?.let { appLabel(it) },
            packageTwo?.let { appLabel(it) }
        )
        val description = when (activeNames.size) {
            2 -> "${activeNames[0]} + ${activeNames[1]}"
            1 -> activeNames[0]
            else -> "Tidak ada APK dipilih"
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, KeeperService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("RK Server Guard aktif")
            .setContentText("$description • CPU ${if (keepCpu) "dijaga" else "normal"}")
            .setStyle(Notification.BigTextStyle().bigText(
                "Penjagaan aktif untuk aplikasi pilihan. Ini tidak memastikan aplikasi tujuan sedang menjalankan server."
            ))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Hentikan", stopIntent)
            .build()
    }

    private fun appLabel(packageName: String): String {
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Penjagaan server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan status penjagaan aplikasi server"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopProtection() {
        releaseWakeLock()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean("guard_active", false).apply()
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        releaseWakeLock()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean("guard_active", false).apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "id.rk.serverguard.action.START"
        const val ACTION_STOP = "id.rk.serverguard.action.STOP"
        const val EXTRA_PACKAGE_ONE = "extra.package.one"
        const val EXTRA_PACKAGE_TWO = "extra.package.two"
        const val EXTRA_KEEP_CPU = "extra.keep.cpu"
        private const val CHANNEL_ID = "server_guard_channel"
        private const val NOTIFICATION_ID = 2701
    }
}
