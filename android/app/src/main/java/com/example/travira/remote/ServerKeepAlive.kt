package com.example.travira.remote

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Periodically hits a lightweight Render endpoint so the free/hobby
 * service is less likely to spin down while the app process is alive.
 *
 * Render typically sleeps after ~15 minutes of no traffic — we ping
 * every [INTERVAL_MS] (default 8 minutes).
 *
 * Note: when the app is fully closed, use an external uptime monitor
 * (e.g. UptimeRobot / cron-job.org) on GET /api/ping for 24/7 wake.
 */
object ServerKeepAlive {

    private const val TAG = "TRAVIRA_KEEPALIVE"
    /** Under Render’s ~15 min idle window */
    private const val INTERVAL_MS = 8 * 60 * 1000L
    private const val RETRY_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var job: Job? = null

    fun start() {
        if (job?.isActive == true) {
            Log.d(TAG, "already running")
            return
        }
        job = scope.launch {
            Log.d(TAG, "started (every ${INTERVAL_MS / 1000}s)")
            // First ping soon so a cold server wakes as the app opens
            pingOnce()
            while (isActive) {
                delay(INTERVAL_MS)
                pingOnce()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        Log.d(TAG, "stopped")
    }

    private suspend fun pingOnce() {
        try {
            withContext(Dispatchers.IO) {
                val res = RetrofitInstance.keepAliveApi.ping()
                Log.d(TAG, "ping ok ts=${res.ts} pong=${res.pong}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "ping failed: ${e.message}")
            // Short retry once after failure (e.g. cold start timeout)
            try {
                delay(RETRY_MS)
                withContext(Dispatchers.IO) {
                    RetrofitInstance.keepAliveApi.health()
                }
                Log.d(TAG, "retry health ok")
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: Exception) {
                Log.w(TAG, "retry failed: ${e2.message}")
            }
        }
    }
}
