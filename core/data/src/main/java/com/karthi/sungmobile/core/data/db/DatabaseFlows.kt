package com.karthi.sungmobile.core.data.db

import androidx.room.InvalidationTracker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/** Register before the initial emission so a change cannot fall between reading and observing. */
internal fun SungDatabase.changes(vararg tables: String): Flow<Unit> = callbackFlow {
    val observer = object : InvalidationTracker.Observer(tables) {
        override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
    }
    invalidationTracker.addObserver(observer)
    trySend(Unit)
    awaitClose { invalidationTracker.removeObserver(observer) }
}.conflate().flowOn(kotlinx.coroutines.Dispatchers.IO)
