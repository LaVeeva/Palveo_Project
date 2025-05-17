package com.palveo.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import com.palveo.dao.EventTagDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.db.exception.DuplicateKeyException;
import com.palveo.model.Event;
import com.palveo.model.Tag;

public class EventTagDaoImpl implements EventTagDao {

    private Event mapResultSetToBasicEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("e_id"));
        event.setHostUserId(rs.getInt("e_host_user_id"));
        event.setTitle(rs.getString("e_title"));
        Timestamp eventTimestamp = rs.getTimestamp("e_event_date_time");
        if (eventTimestamp != null) {
            event.setEventDateTime(eventTimestamp.toLocalDateTime());
        }
        event.setLocationString(rs.getString("e_location_string"));
        event.setCategory(Event.EventCategory.fromString(rs.getString("e_category")));
        event.setPrivacy(Event.PrivacySetting.fromString(rs.getString("e_privacy")));
        
        return event;
    }

    private Tag mapResultSetToBasicTag(ResultSet rs) throws SQLException {
        return new Tag(rs.getInt("t_tag_id"), rs.getString("t_tag_name"));
    }

    @Override
    public boolean addTagToEvent(int eventId, int tagId, int addedByUserId) throws SQLException {
        return addTagToEvent(eventId, tagId, addedByUserId, null);
    }

    @Override
    public boolean addTagToEvent(int eventId, int tagId, int addedByUserId, Connection conn) throws SQLException {
        String sql = "INSERT INTO event_tags (event_id, tag_id, added_by_user_id) VALUES (?, ?, ?)";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null ) {
             System.err.println("EventTagDaoImpl.addTagToEvent: No DB Connection");
             return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, tagId);
            pstmt.setInt(3, addedByUserId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate entry"))) {
                throw new DuplicateKeyException("Tag (ID: " + tagId + ") is already associated with event (ID: " + eventId + ") by user (ID: " + addedByUserId + ").", e.getSQLState(), e.getErrorCode(), e);
            }
            System.err.println("EventTagDaoImpl.addTagToEvent: SQLException for event " + eventId + ", tag " + tagId + ", user " + addedByUserId + ": " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
            }
        }
    }
    
    @Override
    public boolean removeTagFromEvent(int eventId, int tagId) throws SQLException {
        return removeTagFromEvent(eventId, tagId, null);
    }

    @Override
    public boolean removeTagFromEvent(int eventId, int tagId, Connection conn) throws SQLException {
        String sql = "DELETE FROM event_tags WHERE event_id = ? AND tag_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) { 
            System.err.println("EventTagDaoImpl.removeTagFromEvent: No DB Conn"); 
            return false; 
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, tagId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("EventTagDaoImpl.removeTagFromEvent: SQLException for event " + eventId + ", tag " + tagId + ": " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
            }
        }
    }
    
    @Override
    public boolean removeAllTagsFromEvent(int eventId) throws SQLException {
        return removeAllTagsFromEvent(eventId, null);
    }

    @Override
    public boolean removeAllTagsFromEvent(int eventId, Connection conn) throws SQLException {
        String sql = "DELETE FROM event_tags WHERE event_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null) {
            System.err.println("EventTagDaoImpl.removeAllTagsFromEvent: No DB Connection");
            return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.executeUpdate(); 
            return true; 
        } catch (SQLException e) {
            System.err.println("EventTagDaoImpl.removeAllTagsFromEvent: SQLException for event " + eventId + ": " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
            }
        }
    }

    @Override
    public List<Tag> findTagsByEventId(int eventId) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT t.tag_id as t_tag_id, t.tag_name as t_tag_name FROM tags t " +
                     "JOIN event_tags et ON t.tag_id = et.tag_id " +
                     "WHERE et.event_id = ? ORDER BY t.tag_name ASC";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) {
             System.err.println("EventTagDaoImpl.findTagsByEventId: No DB Conn for event " + eventId); 
             return tags;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapResultSetToBasicTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("EventTagDaoImpl.findTagsByEventId: SQLException for event " + eventId + ": " + e.getMessage());
            throw e;
        }
        return tags;
    }

    @Override
    public List<Event> findEventsByTagId(int tagId) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.id as e_id, e.host_user_id as e_host_user_id, e.title as e_title, " +
                     "e.event_date_time as e_event_date_time, e.location_string as e_location_string, " +
                     "e.category as e_category, e.privacy as e_privacy " +
                     "FROM events e JOIN event_tags et ON e.id = et.event_id " +
                     "WHERE et.tag_id = ? ORDER BY e.event_date_time DESC";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("EventTagDaoImpl.findEventsByTagId: No DB Conn for tag " + tagId); 
            return events;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, tagId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToBasicEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("EventTagDaoImpl.findEventsByTagId: SQLException for tag " + tagId + ": " + e.getMessage());
            throw e;
        }
        return events;
    }

    @Override
    public boolean isTagAssociatedWithEvent(int eventId, int tagId) throws SQLException {
        String sql = "SELECT 1 FROM event_tags WHERE event_id = ? AND tag_id = ? LIMIT 1";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("EventTagDaoImpl.isTagAssociatedWithEvent: No DB Conn for event " + eventId + ", tag " + tagId); 
            return false; 
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, tagId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); 
            }
        } catch (SQLException e) {
            System.err.println("EventTagDaoImpl.isTagAssociatedWithEvent: SQLException for event " + eventId + ", tag " + tagId + ": " + e.getMessage());
            throw e;
        }
    }
}