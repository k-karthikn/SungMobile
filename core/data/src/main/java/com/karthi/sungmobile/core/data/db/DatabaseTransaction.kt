package com.karthi.sungmobile.core.data.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable

internal suspend fun <T> SungDatabase.transaction(block: () -> T): T =
    withContext(Dispatchers.IO) { runInTransaction(Callable { block() }) }
