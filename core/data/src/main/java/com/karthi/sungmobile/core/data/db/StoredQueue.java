package com.karthi.sungmobile.core.data.db;

import java.util.List;

public final class StoredQueue {
    public final QueueHeaderEntity header;
    public final List<QueueEntryEntity> entries;
    public StoredQueue(QueueHeaderEntity header, List<QueueEntryEntity> entries) {
        this.header = header;
        this.entries = entries;
    }
}
