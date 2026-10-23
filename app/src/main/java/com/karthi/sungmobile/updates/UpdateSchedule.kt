package com.karthi.sungmobile.updates

import android.app.job.*
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.*

internal object UpdateSchedule {
    private const val PERIODIC = 9401
    private const val SOON = 9402
    fun configure(context: Context, enabled: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (!enabled) { scheduler.cancel(PERIODIC); scheduler.cancel(SOON); return }
        if (scheduler.getPendingJob(PERIODIC) == null) scheduler.schedule(builder(context, PERIODIC)
            .setPeriodic(12 * 60 * 60 * 1000L).setPersisted(true).build())
    }
    fun checkSoon(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (scheduler.getPendingJob(SOON) == null) scheduler.schedule(builder(context, SOON).build())
    }
    private fun builder(context: Context, id: Int) = JobInfo.Builder(id, ComponentName(context, UpdateJobService::class.java))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED).setRequiresBatteryNotLow(true)
        .setBackoffCriteria(30 * 60 * 1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
}

class UpdateJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val running = mutableMapOf<Int, Job>()
    override fun onStartJob(params: JobParameters): Boolean {
        if (!AppUpdates.get(this).enabled()) return false
        running[params.jobId] = scope.launch {
            val success = AppUpdates.get(this@UpdateJobService).check(automatic = true)
            running.remove(params.jobId)
            jobFinished(params, !success && AppUpdates.get(this@UpdateJobService).enabled())
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean {
        running.remove(params.jobId)?.cancel()
        return AppUpdates.get(this).enabled()
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
