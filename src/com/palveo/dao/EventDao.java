package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Event;

public interface EventDao {
    int save(Event event) throws SQLException;
    int save(Event event, Connection conn) throws SQLException;

    boolean update(Event event) throws SQLException;
    boolean update(Event event, Connection conn) throws SQLException;

    boolean delete(int eventId) throws SQLException;
    boolean delete(int eventId, Connection conn) throws SQLException;

    Optional<Event> findById(int eventId) throws SQLException;

    List<Event> findByHostId(int hostUserId) throws SQLException;

    List<Event> findAllPublicEvents() throws SQLException;

    List<Event> findUpcomingPublicEvents(LocalDateTime afterDateTime) throws SQLException;

    List<Event> findUpcomingEventsForFeed(int currentUserId, List<Integer> friendIds,
            LocalDateTime afterDateTime) throws SQLException;
}