package com.madlauncher

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppsReloadBus {
    private val _tick = MutableSharedFlow<Unit>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val tick = _tick.asSharedFlow()

    fun request() {
        _tick.tryEmit(Unit)
    }
}
