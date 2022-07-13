package com.hardycheng.androidtoolshub

import android.util.Log
import java.util.*

open class StopWatch(private val maxTickCount: Int) {

    companion object {
        private val TAG = StopWatch::class.java.simpleName
        private fun create(maxTickCount: Int = -1): StopWatch{
            return StopWatch(maxTickCount)
        }
    }

    private val queue: Queue<Int> = LinkedList()
    private var tickCount = 0
    private var totalTime = 0
    private var lastTimestamp = 0L
    private var pauseTimestamp = 0L
    private var debug: Boolean = false

    fun mark(): Int{
        if(lastTimestamp == 0L){
            start()
            return 0
        }
        val currentTimestamp = System.currentTimeMillis()
        val currentInterval = (currentTimestamp - lastTimestamp).toInt()
        if(maxTickCount > 0 && queue.size >= maxTickCount) queue.poll()
        queue.offer(currentInterval)
        tickCount = queue.size
        totalTime += currentInterval
        if(debug) Log.d(TAG, "# mark ${queue.size}: $currentInterval, $totalTime")
        lastTimestamp = currentTimestamp
        return currentInterval
    }

    fun start(){
        if(debug) Log.d(TAG, "+++ start +++")
        if(pauseTimestamp > 0) {
            lastTimestamp = lastTimestamp + (System.currentTimeMillis() - pauseTimestamp)
            pauseTimestamp = 0
        } else {
            lastTimestamp = System.currentTimeMillis()
        }
    }

    fun pause(){
        if(debug) Log.d(TAG, "| pause |")
        pauseTimestamp = System.currentTimeMillis()
    }

    fun reset(){
        if(debug) Log.d(TAG, "--- reset ---")
        queue.clear()
        tickCount = 0
        totalTime = 0
        lastTimestamp = 0L
        pauseTimestamp = 0L
    }

    fun total(): Int{
        return totalTime
    }
}