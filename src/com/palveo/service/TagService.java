package com.palveo.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import com.palveo.model.Event;
import com.palveo.model.Tag;
import com.palveo.model.User;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.TagNotFoundException;
import com.palveo.service.exception.TagOperationException;
import com.palveo.service.exception.UserNotFoundException;

public interface TagService {

    Tag createOrGetTag(String tagName) throws TagOperationException;
    
    Tag createOrGetTag(String tagName, Connection conn) throws TagOperationException, SQLException;

    void applyTagToEvent(int eventId, String tagName, User currentUser)
            throws TagOperationException, EventNotFoundException, UserNotFoundException;

    void removeTagFromEvent(int eventId, String tagName, User currentUser)
            throws TagOperationException, EventNotFoundException, TagNotFoundException, UserNotFoundException;

    List<Tag> getTagsForEvent(int eventId) throws EventNotFoundException, TagOperationException;

    List<Event> findEventsByTag(String tagName) throws TagNotFoundException, TagOperationException;

    List<Event> findEventsByTags(List<String> tagNames, boolean matchAll)
            throws TagOperationException;

    void applyTagToUser(int targetUserId, String tagName, User appliedByUser)
            throws TagOperationException, UserNotFoundException, TagNotFoundException;

    void removeTagFromUser(int targetUserId, String tagName, User removedByUser)
            throws TagOperationException, UserNotFoundException, TagNotFoundException;

    void removeTagFromUserByTagger(int targetUserId, String tagName, User tagger)
            throws TagOperationException, UserNotFoundException, TagNotFoundException;

    List<Tag> getTagsForUser(int userId) throws UserNotFoundException, TagOperationException;

    List<User> findUsersByTag(String tagName) throws TagNotFoundException, TagOperationException;

    List<User> findUsersByTags(List<String> tagNames, boolean matchAll)
            throws TagOperationException;

    List<Tag> searchTags(String query) throws TagOperationException;

    void deleteTagIfUnused(String tagName) throws TagOperationException, TagNotFoundException;

}