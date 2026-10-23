package com.karthi.sungmobile.feature.library

import java.text.DateFormat
import java.util.Date

internal fun timestamp(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))
