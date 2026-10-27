package com.karthi.sungmobile.core.data.db;

import androidx.room.*;
import java.util.List;

@Dao
public interface HistoryDao {
    @Query("SELECT * FROM history_policy WHERE id = 1") HistoryPolicyEntity policy();
    @Upsert void setPolicy(HistoryPolicyEntity policy);
    @Query("SELECT * FROM listening_events WHERE id = :id") ListeningEntity event(String id);
    @Upsert void saveEvent(ListeningEntity event);
    @Query("SELECT * FROM listening_events WHERE qualified = 1 AND trackKey LIKE 'youtube:%' ORDER BY lastPlayedAt, id") List<ListeningEntity> qualifiedForBackup();
    @Insert(onConflict = OnConflictStrategy.IGNORE) void importEvents(List<ListeningEntity> events);
    @Query("DELETE FROM listening_events") void clearListening();
    @Query("SELECT * FROM search_history ORDER BY submittedAt DESC, id DESC") List<SearchEntity> searches();
    @Upsert void saveSearch(SearchEntity search);
    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY submittedAt DESC, id DESC LIMIT 50)") void trimSearches();
    @Query("DELETE FROM search_history") void clearSearches();
    @Query("DELETE FROM search_history WHERE id = :id") void removeSearch(String id);
}
