package com.hardycheng.androidtoolshub.tool

import android.util.Log
import java.util.*
import kotlin.concurrent.schedule

class FpsUtil(private val debug: Boolean = false): StopWatch(maxMarkCount = FRAME_COUNT_LIMIT) {

    companion object {
        private val TAG = FpsUtil::class.java.simpleName
        private const val FRAME_COUNT_LIMIT = 12
    }

    fun interface ReportListener {
        fun onReport(fps: Int)
    }

    var reportListener: ReportListener? = null
        set(value){
            field = value
            onReportParamUpdate()
        }
    var reportInterval: Long = 1000L
        set(value){
            field = value
            onReportParamUpdate()
        }
    var timerTask: TimerTask? = null
    var fps: Int = 0

    fun tick(){
        mark()
        // 直接使用已計算好的 totalInterval，避免重複計算
        val totalInterval = getTotalIntervalWithLock()
        fps = (markCount / (totalInterval / 1000000.0)).toInt()
        if(reportInterval <= 0){
            onReport(fps)
        }
    }

    private fun onReport(fps: Int){
        if(debug){
            Log.v(TAG, "onReport: $fps fps")
        }
        reportListener?.onReport(fps)
    }

    private fun onReportParamUpdate(){
        timerTask?.cancel()
        if(reportInterval > 0){
            timerTask = Timer().schedule(delay = 0, period = reportInterval) {
                onReport(fps)
                reset()
                start()
                fps = 0
            }
        }
    }

    init {
        onReportParamUpdate()
    }

}