package com.example.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    val allSessions: Flow<List<Session>> = sessionDao.getAllSessions()

    suspend fun getSessionById(id: Long): Session? {
        return sessionDao.getSessionById(id)
    }

    suspend fun insertSession(session: Session): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session)
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.deleteSession(session)
    }

    suspend fun deleteSessionById(id: Long) {
        sessionDao.deleteSessionById(id)
    }
}
