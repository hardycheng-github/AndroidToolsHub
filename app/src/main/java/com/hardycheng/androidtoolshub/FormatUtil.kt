package com.hardycheng.androidtoolshub

class FormatUtil {
    companion object {
        fun toTimerString(interval: Int):String{
            val ms = interval % 1000
            val sec = interval / 1000 % 60
            val min = interval / 60000 % 60
            val hr = interval / 3600000
            return String.format("%02d:%02d:%02d.%02d", hr, min, sec, ms/10)
        }
    }
}