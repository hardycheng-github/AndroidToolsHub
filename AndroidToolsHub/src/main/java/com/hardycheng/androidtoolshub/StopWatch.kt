package com.hardycheng.androidtoolshub

import android.util.Log
import java.util.*

class StopWatch(private val label: String = "default", private val maxTickCount: Int = -1) {

    companion object {
        private val TAG = StopWatch::class.java.simpleName
        const val STATE_STOP = 0
        const val STATE_START = 1
        const val STATE_PAUSE = 2
        fun create(label: String = "default", maxTickCount: Int = -1): StopWatch{
            return StopWatch(label, maxTickCount)
        }
    }

    class Mark(val id: Int, val interval: Int, val total: Int, val timestamp: Long){
        override fun toString(): String {
            return "[$id] interval $interval, total $total, timestamp $timestamp"
        }
    }

    private val queue: Queue<Mark> = LinkedList()
    private var tickCount = 0
    private var totalTime = 0
    private var lastTimestamp = 0L
    private var pauseTimestamp = 0L
    private var debug: Boolean = true
    var state: Int = STATE_STOP

    fun mark(): Mark{
        if(lastTimestamp == 0L || pauseTimestamp > 0L) start()
        val currentTimestamp = System.currentTimeMillis()
        val currentInterval = (currentTimestamp - lastTimestamp).toInt()
        totalTime += currentInterval
        val mark = Mark(queue.size, currentInterval, totalTime, currentTimestamp)
        if(maxTickCount > 0 && queue.size >= maxTickCount) queue.poll()
        queue.offer(mark)
        tickCount = queue.size
        if(debug) Log.d(TAG, "#$label mark $mark")
        lastTimestamp = currentTimestamp
        return mark
    }

    fun start(){
        toState(STATE_START)
        if(debug) Log.d(TAG, "#$label start")
        if(pauseTimestamp > 0) {
            lastTimestamp += (System.currentTimeMillis() - pauseTimestamp)
            pauseTimestamp = 0
        } else {
            lastTimestamp = System.currentTimeMillis()
        }
    }

    fun pause(){
        toState(STATE_PAUSE)
        if(debug) Log.d(TAG, "#$label pause")
        pauseTimestamp = System.currentTimeMillis()
    }

    fun reset(): List<Mark>{
        toState(STATE_STOP)
        val list = queue.toList()
        if(debug) Log.d(TAG, "#$label reset")
        queue.clear()
        tickCount = 0
        totalTime = 0
        lastTimestamp = 0L
        pauseTimestamp = 0L
        return list
    }

    fun total(): Int{
        if(lastTimestamp == 0L) return 0
        return totalTime +
            if(pauseTimestamp > 0L) (pauseTimestamp-lastTimestamp).toInt()
            else (System.currentTimeMillis()-lastTimestamp).toInt()
    }

    private fun toState(newState: Int){
        if(state != newState){
            if(debug) Log.v(TAG, "#$label onStateChange: $state -> $newState")
            state = newState
        }
    }
}