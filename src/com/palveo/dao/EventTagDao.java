package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import com.palveo.model.Event;
import com.palveo.model.Tag;

public interface EventTagDao {
    boolean addTagToEvent(int eventId, int tagId, int addedByUserId, Connection conn) throws SQLException;

    boolean removeTagFromEvent(int eventId, int tagId, Connection conn) throws SQLException;

    boolean removeAllTagsFromEvent(int eventId, Connection conn) throws SQLException;

    List<Tag> findTagsByEventId(int eventId) throws SQLException;

    List<Event> findEventsByTagId(int tagId) throws SQLException;

    boolean isTagAssociatedWithEvent(int eventId, int tagId) throws SQLException;
    
    boolean addTagToEvent(int eventId, int tagId, int addedByUserId) throws SQLException;
    boolean removeTagFromEvent(int eventId, int tagId) throws SQLException;
    boolean removeAllTagsFromEvent(int eventId) throws SQLException;
}
