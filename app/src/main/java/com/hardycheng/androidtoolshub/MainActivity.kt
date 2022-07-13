package com.hardycheng.androidtoolshub

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hardycheng.androidtoolshub.databinding.ActivityMainBinding
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var stopWatch: StopWatch = StopWatch.create()
    lateinit var timer: Timer
    lateinit var adapter: MarkAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        timer = Timer()
        adapter = MarkAdapter()
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.start.setOnClickListener { stopWatch.start() }
        binding.stop.setOnClickListener {
            stopWatch.reset()
            adapter.reset()
        }
        binding.pause.setOnClickListener { stopWatch.pause() }
        binding.mark.setOnClickListener {
            adapter.addMark(stopWatch.mark())
        }
    }

    override fun onStart() {
        super.onStart()
        timer.scheduleAtFixedRate(delay = 0, period = 50) {
            if(stopWatch.state == StopWatch.STATE_START){
                runOnUiThread {
                    binding.timer.text = FormatUtil.toTimerString(stopWatch.total())
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        timer.cancel()
    }
}