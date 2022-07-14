package com.hardycheng.androidtoolshub

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hardycheng.androidtoolshub.databinding.ActivityMainBinding
import com.hardycheng.androidtoolshub.tool.StopWatch
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var stopWatch: StopWatch = StopWatch.create()
    var timerTask: TimerTask? = null
    lateinit var adapter: MarkAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = MarkAdapter()
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.start.setOnClickListener {
            stopWatch.start()
        }
        binding.stop.setOnClickListener {
            stopWatch.reset()
        }
        binding.pause.setOnClickListener { stopWatch.pause() }
        binding.mark.setOnClickListener {
            adapter.addMark(stopWatch.mark())
        }
        stopWatch.stateObserver = StopWatch.StateObserver { oldState, newState ->
            if(oldState == StopWatch.STATE_STOP && newState == StopWatch.STATE_START){
                adapter.reset()
            }
        }
        stopWatch.bindToLifecycle(this)
    }

    override fun onStart() {
        super.onStart()
        timerTask = Timer().schedule(delay = 0, period = 50) {
            runOnUiThread {
                binding.timer.text = FormatUtil.toTimerString(stopWatch.total())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        timerTask?.cancel()
    }
}