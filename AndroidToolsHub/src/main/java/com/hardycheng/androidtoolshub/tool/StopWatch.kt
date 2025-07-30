package com.hardycheng.androidtoolshub.tool

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.*

open class StopWatch(private val label: String = "default",
                     private val maxMarkCount: Int = -1,
                     private val debug: Boolean = false,
) {

    companion object {
        private val TAG = StopWatch::class.java.simpleName
        const val STATE_STOP = 0
        const val STATE_START = 1
        const val STATE_PAUSE = 2
        fun create(label: String = "default", maxTickCount: Int = -1): StopWatch {
            return StopWatch(label, maxTickCount)
        }
    }

    fun interface StateObserver {
        fun onStateChanged(oldState:Int, newState: Int)
    }

    var stateObserver: StateObserver? = null

    class Mark(val id: Int, val interval: Int, val total: Int, val timestamp: Long){
        override fun toString(): String {
            return "[$id] interval $interval, total $total, timestamp $timestamp"
        }
    }

    protected val queue: Queue<Mark> = LinkedList()
    protected var markCount = 0
    private var totalTime = 0
    private var lastTimestamp = 0L
    private var pauseTimestamp = 0L
    var state: Int = STATE_STOP

    open fun mark(): Mark {
        if(state != STATE_START) start()
        val currentTimestamp = (System.nanoTime() / 1000)
        val currentInterval = (currentTimestamp - lastTimestamp).toInt()
        totalTime += currentInterval
        val mark = Mark(queue.size, currentInterval, totalTime, currentTimestamp)
        if(maxMarkCount > 0 && queue.size >= maxMarkCount) queue.poll()
        queue.offer(mark)
        markCount = queue.size
        if(debug) Log.d(TAG, "#$label mark $mark")
        lastTimestamp = currentTimestamp
        return mark
    }

    fun start(){
        val lastState = toState(STATE_START)
        if(lastState == STATE_START) return
        if(debug) Log.d(TAG, "#$label start")
        if(lastState == STATE_PAUSE) {
            lastTimestamp += ((System.nanoTime() / 1000) - pauseTimestamp)
            pauseTimestamp = 0
        } else {
            queue.clear()
            lastTimestamp = (System.nanoTime() / 1000)
        }
    }

    fun pause(){
        if(toState(STATE_PAUSE) == STATE_PAUSE) return
        if(debug) Log.d(TAG, "#$label pause")
        pauseTimestamp = (System.nanoTime() / 1000)
    }

    fun reset(): List<Mark>{
        if(toState(STATE_STOP) == STATE_STOP) return queue.toList()
        if(debug) Log.d(TAG, "#$label reset")
        markCount = 0
        totalTime = 0
        lastTimestamp = 0L
        pauseTimestamp = 0L
        return queue.toList()
    }

    fun total(): Int{
        return when(state){
            STATE_START -> totalTime+((System.nanoTime() / 1000)-lastTimestamp).toInt()
            STATE_PAUSE -> totalTime+(pauseTimestamp-lastTimestamp).toInt()
            else -> 0
        }
    }

    fun bindToLifecycle(owner: LifecycleOwner, autoStart: Boolean = false, autoPause: Boolean = true){
        owner.lifecycle.addObserver(object:DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                if(autoStart) start()
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                if(autoPause) pause()
            }
        })
    }

    private fun toState(newState: Int): Int{
        val lastState = state
        if(state != newState){
            if(debug) Log.v(TAG, "#$label onStateChange: $state -> $newState")
            state = newState
            stateObserver?.onStateChanged(lastState, newState)
        }
        return lastState
    }
}