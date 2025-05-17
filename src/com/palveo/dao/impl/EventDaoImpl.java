package com.palveo.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.palveo.dao.EventDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.Event;

public class EventDaoImpl implements EventDao {

    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("id"));
        event.setHostUserId(rs.getInt("host_user_id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        Timestamp eventTimestamp = rs.getTimestamp("event_date_time");
        if (eventTimestamp != null) {
            event.setEventDateTime(eventTimestamp.toLocalDateTime());
        }
        event.setLocationString(rs.getString("location_string"));
        if (rs.getObject("latitude") != null) {
            event.setLatitude(rs.getDouble("latitude"));
        }
        if (rs.getObject("longitude") != null) {
            event.setLongitude(rs.getDouble("longitude"));
        }
        event.setCategory(Event.EventCategory.fromString(rs.getString("category")));
        event.setPrivacy(Event.PrivacySetting.fromString(rs.getString("privacy")));
        event.setEventImagePath(rs.getString("event_image_path"));
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            event.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        }
        Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");
        if (updatedAtTimestamp != null) {
            event.setUpdatedAt(updatedAtTimestamp.toLocalDateTime());
        }
        return event;
    }

    @Override
    public int save(Event event) throws SQLException {
        return save(event, null);
    }
    
    @Override
    public int save(Event event, Connection conn) throws SQLException {
        String sql =
                "INSERT INTO events (host_user_id, title, description, event_date_time, location_string, latitude, longitude, category, privacy, event_image_path, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null ) {
            System.err.println("EventDaoImpl.save: No DB connection.");
            return -1;
        }

        try (PreparedStatement pstmt =
                dbConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, event.getHostUserId());
            pstmt.setString(2, event.getTitle());
            pstmt.setString(3, event.getDescription());
            pstmt.setTimestamp(4, Timestamp.valueOf(event.getEventDateTime()));
            pstmt.setString(5, event.getLocationString());

            if (event.getLatitude() != null)
                pstmt.setDouble(6, event.getLatitude());
            else
                pstmt.setNull(6, Types.DECIMAL);

            if (event.getLongitude() != null)
                pstmt.setDouble(7, event.getLongitude());
            else
                pstmt.setNull(7, Types.DECIMAL);

            pstmt.setString(8, event.getCategory().name());
            pstmt.setString(9, event.getPrivacy().name());
            pstmt.setString(10, event.getEventImagePath());

            LocalDateTime now = LocalDateTime.now();
            pstmt.setTimestamp(11,
                    event.getCreatedAt() != null ? Timestamp.valueOf(event.getCreatedAt())
                            : Timestamp.valueOf(now));
            pstmt.setTimestamp(12,
                    event.getUpdatedAt() != null ? Timestamp.valueOf(event.getUpdatedAt())
                            : Timestamp.valueOf(now));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    event.setId(generatedKeys.getInt(1));
                    return event.getId();
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            System.err.println("EventDaoImpl.save: SQLException for event [" + event.getTitle()
                    + "]: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
            }
        }
    }
    
    @Override
    public boolean update(Event event) throws SQLException {
        return update(event, null);
    }

    @Override
    public boolean update(Event event, Connection conn) throws SQLException {
        String sql =
                "UPDATE events SET host_user_id = ?, title = ?, description = ?, event_date_time = ?, "
                        + "location_string = ?, latitude = ?, longitude = ?, category = ?, privacy = ?, "
                        + "event_image_path = ?, updated_at = ? WHERE id = ?";
        
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) {
            System.err.println("EventDaoImpl.update: No DB connection.");
            return false;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, event.getHostUserId());
            pstmt.setString(2, event.getTitle());
            pstmt.setString(3, event.getDescription());
            pstmt.setTimestamp(4, Timestamp.valueOf(event.getEventDateTime()));
            pstmt.setString(5, event.getLocationString());
            if (event.getLatitude() != null)
                pstmt.setDouble(6, event.getLatitude());
            else
                pstmt.setNull(6, Types.DECIMAL);
            if (event.getLongitude() != null)
                pstmt.setDouble(7, event.getLongitude());
            else
                pstmt.setNull(7, Types.DECIMAL);
            pstmt.setString(8, event.getCategory().name());
            pstmt.setString(9, event.getPrivacy().name());
            pstmt.setString(10, event.getEventImagePath());
            pstmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(12, event.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("EventDaoImpl.update: SQLException for event ID [" + event.getId()
                    + "]: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
             }
        }
    }
    
    @Override
    public boolean delete(int eventId) throws SQLException {
        return delete(eventId, null);
    }

    @Override
    public boolean delete(int eventId, Connection conn) throws SQLException {
        String sql = "DELETE FROM events WHERE id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null) {
            System.err.println("EventDaoImpl.delete: No DB connection.");
            return false;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("EventDaoImpl.delete: SQLException for event ID [" + eventId + "]: "
                    + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
            }
        }
    }

    @Override
    public Optional<Event> findById(int eventId) throws SQLException {
        String sql = "SELECT * FROM events WHERE id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("EventDaoImpl.findById: No DB connection for event ID: " + eventId);
            return Optional.empty();
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("EventDaoImpl.findById: SQLException for event ID [" + eventId
                    + "]: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Event> findByHostId(int hostUserId) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE host_user_id = ? ORDER BY event_date_time DESC";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println(
                    "EventDaoImpl.findByHostId: No DB connection for host ID: " + hostUserId);
            return events;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, hostUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("EventDaoImpl.findByHostId: SQLException for host ID [" + hostUserId
                    + "]: " + e.getMessage());
            throw e;
        }
        return events;
    }

    @Override
    public List<Event> findAllPublicEvents() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE privacy = ? ORDER BY event_date_time DESC";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("EventDaoImpl.findAllPublicEvents: No DB connection.");
            return events;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, Event.PrivacySetting.PUBLIC.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("EventDaoImpl.findAllPublicEvents: SQLException: " + e.getMessage());
            throw e;
        }
        return events;
    }

    @Override
    public List<Event> findUpcomingPublicEvents(LocalDateTime afterDateTime) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql =
                "SELECT * FROM events WHERE privacy = ? AND event_date_time > ? ORDER BY event_date_time ASC";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("EventDaoImpl.findUpcomingPublicEvents: No DB connection.");
            return events;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, Event.PrivacySetting.PUBLIC.name());
            pstmt.setTimestamp(2, Timestamp.valueOf(afterDateTime));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(
                    "EventDaoImpl.findUpcomingPublicEvents: SQLException: " + e.getMessage());
            throw e;
        }
        return events;
    }

    @Override
    public List<Event> findUpcomingEventsForFeed(int currentUserId, List<Integer> friendIds,
            LocalDateTime afterDateTime) throws SQLException {
        List<Event> events = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT * FROM events WHERE event_date_time > ? AND (privacy = ?");

        if (friendIds != null && !friendIds.isEmpty()) {
            sqlBuilder.append(" OR (privacy = ? AND host_user_id IN (");
            sqlBuilder.append(
                    friendIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            sqlBuilder.append("))");
        }
        sqlBuilder.append(") ORDER BY event_date_time ASC");

        String sql = sqlBuilder.toString();
        Connection dbConn = DatabaseConnection.getConnection();

        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("EventDaoImpl.findUpcomingEventsForFeed: No DB connection.");
            return events;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            int paramIndex = 1;
            pstmt.setTimestamp(paramIndex++, Timestamp.valueOf(afterDateTime));
            pstmt.setString(paramIndex++, Event.PrivacySetting.PUBLIC.name());
            if (friendIds != null && !friendIds.isEmpty()) {
                pstmt.setString(paramIndex++, Event.PrivacySetting.PRIVATE.name());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(
                    "EventDaoImpl.findUpcomingEventsForFeed: SQLException: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return events;
    }
}