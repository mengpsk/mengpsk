package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
