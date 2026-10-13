package com.karthi.sungmobile.core.data.history

import com.karthi.sungmobile.core.data.db.HistoryPolicyEntity
import com.karthi.sungmobile.core.model.library.HistoryPolicy

internal fun HistoryPolicyEntity.toPolicy() = HistoryPolicy(privateMode, listening, searches, listeningEpoch, searchEpoch)
