package com.palveo.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.palveo.dao.ParticipantDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.db.exception.DuplicateKeyException;
import com.palveo.model.Participant;

public class ParticipantDaoImpl implements ParticipantDao {

    private Participant mapResultSetToParticipant(ResultSet rs) throws SQLException {
        Participant participant = new Participant();
        participant.setId(rs.getInt("id"));
        participant.setEventId(rs.getInt("event_id"));
        participant.setUserId(rs.getInt("user_id"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                participant.setStatus(Participant.RsvpStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid RSVP status in DB: " + statusStr);
            }
        }
        Timestamp rsvpTimestamp = rs.getTimestamp("rsvp_timestamp");
        if (rsvpTimestamp != null) {
            participant.setRsvpTimestamp(rsvpTimestamp.toLocalDateTime());
        }
        return participant;
    }
    
    @Override
    public int save(Participant participant) throws SQLException {
        return save(participant, null);
    }

    @Override
    public int save(Participant participant, Connection conn) throws SQLException {
        String sql = "INSERT INTO participants (event_id, user_id, status, rsvp_timestamp) VALUES (?, ?, ?, ?)";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null ) {
             System.err.println("ParticipantDaoImpl.save: No DB Connection"); 
             return -1; 
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, participant.getEventId());
            pstmt.setInt(2, participant.getUserId());
            pstmt.setString(3, participant.getStatus().toString());
            pstmt.setTimestamp(4, Timestamp.valueOf(participant.getRsvpTimestamp()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) return -1;

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    participant.setId(generatedKeys.getInt(1));
                    return participant.getId();
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate entry"))) {
                 throw new DuplicateKeyException("Duplicate entry for event_id=" + participant.getEventId() + ", user_id=" + participant.getUserId(), e.getSQLState(), e.getErrorCode(), e);
            }
            System.err.println("ParticipantDaoImpl.save: SQLException: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn) ) {
                
            }
        }
    }
    
    @Override
    public boolean updateStatus(int eventId, int userId, Participant.RsvpStatus newStatus) throws SQLException {
        return updateStatus(eventId, userId, newStatus, null);
    }

    @Override
    public boolean updateStatus(int eventId, int userId, Participant.RsvpStatus newStatus, Connection conn) throws SQLException {
        String sql = "UPDATE participants SET status = ?, rsvp_timestamp = CURRENT_TIMESTAMP WHERE event_id = ? AND user_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) {
             System.err.println("ParticipantDaoImpl.updateStatus: No DB Connection"); 
             return false;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.toString());
            pstmt.setInt(2, eventId);
            pstmt.setInt(3, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("ParticipantDaoImpl.updateStatus: SQLException: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
            }
        }
    }
    
    @Override
    public boolean delete(int eventId, int userId) throws SQLException {
        return delete(eventId, userId, null);
    }

    @Override
    public boolean delete(int eventId, int userId, Connection conn) throws SQLException {
        String sql = "DELETE FROM participants WHERE event_id = ? AND user_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) {
            System.err.println("ParticipantDaoImpl.delete: No DB Connection");
            return false;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("ParticipantDaoImpl.delete: SQLException: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
            }
        }
    }

    @Override
    public Optional<Participant> findByEventIdAndUserId(int eventId, int userId) throws SQLException {
        String sql = "SELECT * FROM participants WHERE event_id = ? AND user_id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) { 
            System.err.println("ParticipantDaoImpl.findByEventIdAndUserId: No DB Connection"); 
            return Optional.empty(); 
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToParticipant(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ParticipantDaoImpl.findByEventIdAndUserId: SQLException: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Participant> findByEventId(int eventId) throws SQLException {
        List<Participant> participants = new ArrayList<>();
        String sql = "SELECT * FROM participants WHERE event_id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) {
             System.err.println("ParticipantDaoImpl.findByEventId: No DB Connection");
             return participants;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participants.add(mapResultSetToParticipant(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ParticipantDaoImpl.findByEventId: SQLException: " + e.getMessage());
            throw e;
        }
        return participants;
    }

    @Override
    public List<Participant> findByUserId(int userId) throws SQLException {
        List<Participant> participants = new ArrayList<>();
        String sql = "SELECT * FROM participants WHERE user_id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
             System.err.println("ParticipantDaoImpl.findByUserId: No DB Connection"); 
             return participants;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participants.add(mapResultSetToParticipant(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ParticipantDaoImpl.findByUserId: SQLException: " + e.getMessage());
            throw e;
        }
        return participants;
    }
}