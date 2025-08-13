package com.hardycheng.androidtoolshub.tool

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

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
    
    // 維護 totalInterval 避免重複計算
    protected var totalInterval: Int = 0
    
    // 使用 ReentrantReadWriteLock 來保護 queue 操作
    private val queueLock = ReentrantReadWriteLock()
    private val readLock = queueLock.readLock()
    private val writeLock = queueLock.writeLock()

    open fun mark(): Mark {
        if(state != STATE_START) start()
        val currentTimestamp = (System.nanoTime() / 1000)
        val currentInterval = (currentTimestamp - lastTimestamp).toInt()
        totalTime += currentInterval
        val mark = Mark(queue.size, currentInterval, totalTime, currentTimestamp)
        
        // 使用寫鎖保護 queue 的修改操作
        writeLock.lock()
        try {
            if(maxMarkCount > 0 && queue.size >= maxMarkCount) {
                val removedMark = queue.poll()
                // 移除舊的 interval
                if (removedMark != null) {
                    totalInterval -= removedMark.interval
                }
            }
            queue.offer(mark)
            // 加入新的 interval
            totalInterval += mark.interval
            markCount = queue.size
        } finally {
            writeLock.unlock()
        }
        
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
            // 使用寫鎖保護 queue 的清空操作
            writeLock.lock()
            try {
                queue.clear()
                totalInterval = 0  // 重置 totalInterval
            } finally {
                writeLock.unlock()
            }
            lastTimestamp = (System.nanoTime() / 1000)
        }
    }

    fun pause(){
        if(toState(STATE_PAUSE) == STATE_PAUSE) return
        if(debug) Log.d(TAG, "#$label pause")
        pauseTimestamp = (System.nanoTime() / 1000)
    }

    fun reset(): List<Mark>{
        if(toState(STATE_STOP) == STATE_STOP) {
            // 使用讀鎖保護 queue 的讀取操作
            readLock.lock()
            try {
                return queue.toList()
            } finally {
                readLock.unlock()
            }
        }
        if(debug) Log.d(TAG, "#$label reset")
        markCount = 0
        totalTime = 0
        totalInterval = 0  // 重置 totalInterval
        lastTimestamp = 0L
        pauseTimestamp = 0L
        return emptyList()
    }

    fun total(): Int{
        return when(state){
            STATE_START -> totalTime+((System.nanoTime() / 1000)-lastTimestamp).toInt()
            STATE_PAUSE -> totalTime+(pauseTimestamp-lastTimestamp).toInt()
            else -> 0
        }
    }

    // 新增安全的 queue 讀取方法
    fun getQueueSnapshot(): List<Mark> {
        readLock.lock()
        try {
            return queue.toList()
        } finally {
            readLock.unlock()
        }
    }

    // 新增安全的 totalInterval 讀取方法
    fun getTotalIntervalWithLock(): Int {
        readLock.lock()
        try {
            return totalInterval
        } finally {
            readLock.unlock()
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