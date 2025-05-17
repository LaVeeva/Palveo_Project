package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Participant;

public interface ParticipantDao {
    int save(Participant participant) throws SQLException;
    int save(Participant participant, Connection conn) throws SQLException;

    boolean updateStatus(int eventId, int userId, Participant.RsvpStatus newStatus) throws SQLException;
    boolean updateStatus(int eventId, int userId, Participant.RsvpStatus newStatus, Connection conn) throws SQLException;
    
    boolean delete(int eventId, int userId) throws SQLException;
    boolean delete(int eventId, int userId, Connection conn) throws SQLException;

    Optional<Participant> findByEventIdAndUserId(int eventId, int userId) throws SQLException;
    List<Participant> findByEventId(int eventId) throws SQLException;
    List<Participant> findByUserId(int userId) throws SQLException;
}