package com.hardycheng.androidtoolshub

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

class CameraLifecycleHandler: LifecycleOwner{

    class CameraLifecycle: Lifecycle() {

        private var currentState: State = State.INITIALIZED

        override fun addObserver(observer: LifecycleObserver) {
            TODO("Not yet implemented")
        }

        override fun removeObserver(observer: LifecycleObserver) {
            TODO("Not yet implemented")
        }

        override fun getCurrentState(): State {
            return currentState
        }

    }

    val lifecycle: CameraLifecycle = CameraLifecycle()

    override fun getLifecycle(): Lifecycle {
        return lifecycle
    }
}