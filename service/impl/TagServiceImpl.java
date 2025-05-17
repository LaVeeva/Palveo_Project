package com.palveo.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.palveo.dao.EventDao;
import com.palveo.dao.EventTagDao;
import com.palveo.dao.TagDao;
import com.palveo.dao.UserDao;
import com.palveo.dao.UserTagDao;
import com.palveo.dao.impl.EventDaoImpl;
import com.palveo.dao.impl.EventTagDaoImpl;
import com.palveo.dao.impl.TagDaoImpl;
import com.palveo.dao.impl.UserDaoImpl;
import com.palveo.dao.impl.UserTagDaoImpl;
import com.palveo.db.DatabaseConnection;
import com.palveo.db.exception.DuplicateKeyException;
import com.palveo.model.Event;
import com.palveo.model.Tag;
import com.palveo.model.User;
import com.palveo.model.UserTag;
import com.palveo.service.TagService;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.TagNotFoundException;
import com.palveo.service.exception.TagOperationException;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.util.ValidationUtils;

public class TagServiceImpl implements TagService {

    private TagDao tagDao;
    private EventTagDao eventTagDao;
    private UserTagDao userTagDao;
    private EventDao eventDao;
    private UserDao userDao;

    public TagServiceImpl() {
        this.tagDao = new TagDaoImpl();
        this.eventTagDao = new EventTagDaoImpl();
        this.userTagDao = new UserTagDaoImpl();
        this.eventDao = new EventDaoImpl();
        this.userDao = new UserDaoImpl();
    }

    public TagServiceImpl(TagDao tagDao, EventTagDao eventTagDao, UserTagDao userTagDao,
            EventDao eventDao, UserDao userDao) {
        this.tagDao = tagDao;
        this.eventTagDao = eventTagDao;
        this.userTagDao = userTagDao;
        this.eventDao = eventDao;
        this.userDao = userDao;
    }

    private void validateTagName(String tagName) throws TagOperationException {
        if (ValidationUtils.isNullOrEmpty(tagName)) {
            throw new TagOperationException("Tag name cannot be empty.");
        }
        if (tagName.length() > 50) {
            throw new TagOperationException("Tag name is too long (max 50 characters).");
        }
        if (!tagName.matches("^[\\p{L}\\p{N}#+-._ ]+$")) {
            throw new TagOperationException(
                    "Tag name contains invalid characters. Allowed: letters, numbers, #, +, -, ., _, space");
        }
    }
    
    @Override
    public Tag createOrGetTag(String tagName) throws TagOperationException {
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);
        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();
            Tag tag = tagDao.findOrCreateTag(processedTagName, conn)
                    .orElseThrow(() -> new TagOperationException(
                            "Failed to create or find tag (internal DAO issue): " + processedTagName));
            if (transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
            return tag;
        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (createOrGetTag): Error during rollback: " + re.getMessage()); }
            }
            throw new TagOperationException("Database error in createOrGetTag for: " + processedTagName, e);
        } finally {
            if (transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()) {
                 try { DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("TagService (createOrGetTag): Error ensuring transaction rollback in finally: " + se.getMessage());}
            }
        }
    }

    @Override
    public Tag createOrGetTag(String tagName, Connection conn) throws TagOperationException, SQLException {
        if (conn == null) {
            throw new SQLException("Provided connection cannot be null for createOrGetTag operation.");
        }
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);
        return tagDao.findOrCreateTag(processedTagName, conn)
                .orElseThrow(() -> new TagOperationException(
                        "Failed to create or find tag (DAO issue with provided connection): " + processedTagName));
    }

    @Override
    public void applyTagToEvent(int eventId, String tagName, User currentUser)
            throws TagOperationException, EventNotFoundException, UserNotFoundException {
        if (currentUser == null) {
            throw new UserNotFoundException("Current user cannot be null to apply a tag.");
        }
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);
        
        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            eventDao.findById(eventId).orElseThrow(
                    () -> new EventNotFoundException("Event not found with ID: " + eventId));

            Tag tag = this.createOrGetTag(processedTagName, conn);
            
            try {
                eventTagDao.addTagToEvent(eventId, tag.getTagId(), currentUser.getId(), conn);
            } catch (DuplicateKeyException dke) {
            }
            
            if (transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (applyTagToEvent): Error during rollback: " + re.getMessage()); }
            }
            throw new TagOperationException(
                    "Database error applying tag '" + processedTagName + "' to event " + eventId, e);
        } catch (TagOperationException | EventNotFoundException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (applyTagToEvent): Error during rollback on specific exception: " + re.getMessage()); }
            }
            throw e;
        } finally {
            if (transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()) {
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("TagServiceImpl: Error ensuring transaction is rolled back in finally (applyTagToEvent): " + se.getMessage());}
            }
        }
    }

    @Override
    public void removeTagFromEvent(int eventId, String tagName, User currentUser)
            throws TagOperationException, EventNotFoundException, TagNotFoundException, UserNotFoundException {
        if (currentUser == null) {
            throw new UserNotFoundException("Current user cannot be null to remove a tag.");
        }
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);

        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            Event event = eventDao.findById(eventId).orElseThrow(
                    () -> new EventNotFoundException("Event not found with ID: " + eventId));

            if (event.getHostUserId() != currentUser.getId()) {
                throw new TagOperationException("Only the event host can remove tags from the event.");
            }

            Tag tag = tagDao.findByName(processedTagName).orElseThrow(
                    () -> new TagNotFoundException("Tag '" + processedTagName + "' not found."));

            eventTagDao.removeTagFromEvent(eventId, tag.getTagId(), conn);
            
            if (transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (removeTagFromEvent): Error during rollback: " + re.getMessage()); }
            }
            throw new TagOperationException(
                    "Database error removing tag '" + processedTagName + "' from event " + eventId, e);
        } catch (TagOperationException | EventNotFoundException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (removeTagFromEvent): Error during rollback on specific exception: " + re.getMessage()); }
            }
            throw e;
        } finally {
             if (transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()) {
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("TagServiceImpl: Error ensuring transaction is rolled back in finally (removeTagFromEvent): " + se.getMessage());}
            }
        }
    }

    @Override
    public List<Tag> getTagsForEvent(int eventId) throws EventNotFoundException, TagOperationException {
        try {
            eventDao.findById(eventId).orElseThrow(
                    () -> new EventNotFoundException("Event not found with ID: " + eventId));
            return eventTagDao.findTagsByEventId(eventId);
        } catch (SQLException e) {
            throw new TagOperationException("Database error fetching tags for event " + eventId, e);
        }
    }

    @Override
    public List<Event> findEventsByTag(String tagName)
            throws TagNotFoundException, TagOperationException {
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);
        try {
            Tag tag = tagDao.findByName(processedTagName).orElseThrow(
                    () -> new TagNotFoundException("Tag '" + processedTagName + "' not found."));
            return eventTagDao.findEventsByTagId(tag.getTagId());
        } catch (SQLException e) {
            throw new TagOperationException(
                    "Database error finding events by tag '" + processedTagName + "'", e);
        }
    }

    @Override
    public List<Event> findEventsByTags(List<String> tagNames, boolean matchAll)
            throws TagOperationException {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> tagIds = new ArrayList<>();
        try {
            for (String tagName : tagNames) {
                String processedTagName = tagName.trim().toLowerCase();
                validateTagName(processedTagName);
                Tag tag = tagDao.findByName(processedTagName)
                        .orElseThrow(() -> new TagNotFoundException(
                                "One or more tags not found: " + processedTagName));
                tagIds.add(tag.getTagId());
            }

            if (tagIds.isEmpty())
                return new ArrayList<>();

            Set<Integer> matchingEventIds = new HashSet<>();
            if (!matchAll) {
                for (Integer tagId : tagIds) {
                    List<Event> eventsForTag = eventTagDao.findEventsByTagId(tagId);
                    eventsForTag.forEach(event -> matchingEventIds.add(event.getId()));
                }
            } else {
                if (tagIds.size() == 1) {
                    List<Event> eventsForTag = eventTagDao.findEventsByTagId(tagIds.get(0));
                    eventsForTag.forEach(event -> matchingEventIds.add(event.getId()));
                } else {
                    List<Event> initialEvents = eventTagDao.findEventsByTagId(tagIds.get(0));
                    for (Event event : initialEvents) {
                        boolean currentEventMatchesAllOtherTags = true;
                        for (int i = 1; i < tagIds.size(); i++) {
                            if (!eventTagDao.isTagAssociatedWithEvent(event.getId(),
                                    tagIds.get(i))) {
                                currentEventMatchesAllOtherTags = false;
                                break;
                            }
                        }
                        if (currentEventMatchesAllOtherTags) {
                            matchingEventIds.add(event.getId());
                        }
                    }
                }
            }

            List<Event> resultEvents = new ArrayList<>();
            for (Integer eventId : matchingEventIds) {
                eventDao.findById(eventId).ifPresent(resultEvents::add);
            }
            return resultEvents;

        } catch (SQLException e) {
            throw new TagOperationException("Database error finding events by multiple tags.", e);
        } catch (TagNotFoundException e) {
            throw new TagOperationException("One or more specified tags not found.", e);
        }
    }

    @Override
    public void applyTagToUser(int targetUserId, String tagName, User appliedByUser)
            throws TagOperationException, UserNotFoundException, TagNotFoundException {
        if (appliedByUser == null)
            throw new UserNotFoundException("User applying the tag (applier) cannot be null.");
        
        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            userDao.findById(targetUserId).orElseThrow(() -> new UserNotFoundException(
                    "Target user not found with ID: " + targetUserId));
            Tag tag = this.createOrGetTag(tagName, conn);

            UserTag userTag = new UserTag(targetUserId, tag.getTagId(), appliedByUser.getId());
            userTagDao.addTagToUser(userTag, conn);

            if (transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
        } catch (DuplicateKeyException dke) {
             if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (applyTagToUser): Error during rollback for DupKey: " + re.getMessage()); }
            }
            throw new TagOperationException("This tag has already been applied to the user by you.", dke);
        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (applyTagToUser): Error during rollback: " + re.getMessage()); }
            }
            throw new TagOperationException(
                    "Database error applying tag '" + tagName + "' to user " + targetUserId, e);
        } catch (TagOperationException | UserNotFoundException e) {
             if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (applyTagToUser): Error during rollback for specific exc: " + re.getMessage()); }
            }
            throw e;
        } finally {
            if (transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()) {
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("TagServiceImpl: Error ensuring transaction is rolled back in finally (applyTagToUser): " + se.getMessage());}
            }
        }
    }

    @Override
    public void removeTagFromUserByTagger(int targetUserId, String tagName, User tagger)
            throws TagOperationException, UserNotFoundException, TagNotFoundException {
        if (tagger == null)
            throw new UserNotFoundException(
                    "User who applied the tag (tagger) cannot be null for removal.");
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);

        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            userDao.findById(targetUserId).orElseThrow(() -> new UserNotFoundException(
                    "Target user not found with ID: " + targetUserId));

            Tag tag = tagDao.findByName(processedTagName).orElseThrow(
                    () -> new TagNotFoundException("Tag '" + processedTagName + "' not found."));

            UserTag userTagInstance = userTagDao
                    .findSpecificUserTagInstance(targetUserId, tag.getTagId(), tagger.getId())
                    .orElseThrow(() -> new TagOperationException("Tag '" + processedTagName
                            + "' was not applied by user " + tagger.getUsername() + " to user "
                            + targetUserId + ", or it does not exist."));

            userTagDao.removeSpecificUserTagLink(userTagInstance.getUserTagId(), conn);
            
            if (transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (removeTagFromUserByTagger): Error during rollback: " + re.getMessage()); }
            }
            throw new TagOperationException("Database error removing tag '" + processedTagName
                    + "' from user " + targetUserId + " by tagger " + tagger.getUsername(), e);
        } catch (TagOperationException | UserNotFoundException e) {
             if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (removeTagFromUserByTagger): Error during rollback for specific exc: " + re.getMessage()); }
            }
            throw e;
        } finally {
            if (transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()) {
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("TagServiceImpl: Error ensuring transaction is rolled back in finally (removeTagFromUserByTagger): " + se.getMessage());}
            }
        }
    }

    @Override
    public void removeTagFromUser(int targetUserId, String tagName, User removedByUser)
            throws TagOperationException, UserNotFoundException, TagNotFoundException {
        if (removedByUser == null)
            throw new UserNotFoundException("User removing the tag cannot be null.");
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);
        
        Connection conn = null;
        boolean transactionOwner = false;
        try {
             if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            User targetUser =
                    userDao.findById(targetUserId).orElseThrow(() -> new UserNotFoundException(
                            "Target user not found with ID: " + targetUserId));
            Tag tag = tagDao.findByName(processedTagName).orElseThrow(
                    () -> new TagNotFoundException("Tag '" + processedTagName + "' not found."));

            List<UserTag> userTags = userTagDao.findUserTagDetailsByUserId(targetUserId).stream()
                    .filter(ut -> ut.getTagId() == tag.getTagId()).collect(Collectors.toList());

            if (userTags.isEmpty()) {
                if(transactionOwner) DatabaseConnection.commitTransaction();
                return;
            }

            boolean removedAny = false;
            for (UserTag ut : userTags) {
                boolean canRemove = false;
                if (targetUser.getId() == removedByUser.getId()) {
                    canRemove = true;
                } else if (ut.getTaggedByUserId() != null
                        && ut.getTaggedByUserId().equals(removedByUser.getId())) {
                    canRemove = true;
                }

                if (canRemove) {
                    if (userTagDao.removeSpecificUserTagLink(ut.getUserTagId(), conn)) {
                        removedAny = true;
                    } else {
                        System.err.println("Failed to remove specific user_tag_id: "
                                + ut.getUserTagId() + " for tag " + processedTagName + " by user "
                                + removedByUser.getUsername());
                    }
                }
            }
            if (!removedAny && userTagDao.isUserTaggedWith(targetUserId, tag.getTagId())) {
                throw new TagOperationException(
                        "Unauthorized to remove tag '" + processedTagName + "' from user "
                                + targetUserId + ", or tag instance not found for this remover.");
            }
            if(transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
        } catch (SQLException e) {
             if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (removeTagFromUser): Error during rollback: " + re.getMessage()); }
            }
            throw new TagOperationException("Database error removing tag '" + processedTagName
                    + "' from user " + targetUserId, e);
        } catch (TagOperationException | UserNotFoundException e) {
             if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (removeTagFromUser): Error during rollback for specific exc: " + re.getMessage()); }
            }
            throw e;
        } finally {
            if (transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()) {
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("TagServiceImpl: Error ensuring transaction is rolled back in finally (removeTagFromUser): " + se.getMessage());}
            }
        }
    }

    @Override
    public List<Tag> getTagsForUser(int userId)
            throws UserNotFoundException, TagOperationException {
        try {
            userDao.findById(userId).orElseThrow(
                    () -> new UserNotFoundException("User not found with ID: " + userId));
            return userTagDao.findDistinctTagsByUserId(userId);
        } catch (SQLException e) {
            throw new TagOperationException("Database error fetching tags for user " + userId, e);
        }
    }

    @Override
    public List<User> findUsersByTag(String tagName)
            throws TagNotFoundException, TagOperationException {
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);
        try {
            Tag tag = tagDao.findByName(processedTagName).orElseThrow(
                    () -> new TagNotFoundException("Tag '" + processedTagName + "' not found."));
            return userTagDao.findUsersByTagId(tag.getTagId());
        } catch (SQLException e) {
            throw new TagOperationException(
                    "Database error finding users by tag '" + processedTagName + "'", e);
        }
    }

    @Override
    public List<User> findUsersByTags(List<String> tagNames, boolean matchAll)
            throws TagOperationException {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> tagIds = new ArrayList<>();
        try {
            for (String tagName : tagNames) {
                String processedTagName = tagName.trim().toLowerCase();
                validateTagName(processedTagName);
                Tag tag = tagDao.findByName(processedTagName)
                        .orElseThrow(() -> new TagNotFoundException(
                                "One or more tags not found: " + processedTagName));
                tagIds.add(tag.getTagId());
            }
            if (tagIds.isEmpty())
                return new ArrayList<>();

            Set<Integer> matchingUserIds = new HashSet<>();
            if (!matchAll) {
                for (Integer tagId : tagIds) {
                    List<User> usersForTag = userTagDao.findUsersByTagId(tagId);
                    usersForTag.forEach(user -> matchingUserIds.add(user.getId()));
                }
            } else {
                if (tagIds.size() == 1) {
                    List<User> usersForTag = userTagDao.findUsersByTagId(tagIds.get(0));
                    usersForTag.forEach(user -> matchingUserIds.add(user.getId()));
                } else {
                    List<User> initialUsers = userTagDao.findUsersByTagId(tagIds.get(0));
                    for (User user : initialUsers) {
                        boolean currentEventMatchesAllOtherTags = true;
                        for (int i = 1; i < tagIds.size(); i++) {
                            if (!userTagDao.isUserTaggedWith(user.getId(), tagIds.get(i))) {
                                currentEventMatchesAllOtherTags = false;
                                break;
                            }
                        }
                        if (currentEventMatchesAllOtherTags) {
                            matchingUserIds.add(user.getId());
                        }
                    }
                }
            }

            List<User> resultUsers = new ArrayList<>();
            for (Integer userId : matchingUserIds) {
                userDao.findById(userId).ifPresent(resultUsers::add);
            }
            return resultUsers;

        } catch (SQLException e) {
            throw new TagOperationException("Database error finding users by multiple tags.", e);
        } catch (TagNotFoundException e) {
            throw new TagOperationException("One or more specified tags not found.", e);
        }
    }

    @Override
    public List<Tag> searchTags(String query) throws TagOperationException {
        if (ValidationUtils.isNullOrEmpty(query) || query.trim().length() < 1) {
            return new ArrayList<>();
        }
        String processedQuery = query.trim().toLowerCase();
        try {
            return tagDao.searchTagsByName(processedQuery);
        } catch (SQLException e) {
            throw new TagOperationException("Database error searching tags with query: " + query,
                    e);
        }
    }

    @Override
    public void deleteTagIfUnused(String tagName)
            throws TagOperationException, TagNotFoundException {
        String processedTagName = tagName.trim().toLowerCase();
        validateTagName(processedTagName);
        
        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            Tag tag = tagDao.findByName(processedTagName).orElseThrow(
                    () -> new TagNotFoundException("Tag '" + processedTagName + "' not found."));

            List<Event> eventsWithTag = eventTagDao.findEventsByTagId(tag.getTagId());
            if (!eventsWithTag.isEmpty()) {
                throw new TagOperationException("Tag '" + processedTagName
                        + "' cannot be deleted, it is still associated with " + eventsWithTag.size()
                        + " event(s).");
            }

            List<User> usersWithTag = userTagDao.findUsersByTagId(tag.getTagId());
            if (!usersWithTag.isEmpty()) {
                throw new TagOperationException("Tag '" + processedTagName
                        + "' cannot be deleted, it is still associated with " + usersWithTag.size()
                        + " user(s).");
            }

            if (!tagDao.delete(tag.getTagId(), conn)) {
                 if (tagDao.findById(tag.getTagId()).isPresent()) {
                    throw new TagOperationException("Failed to delete unused tag '"
                            + processedTagName + "' from database. Tag still exists.");
                 }
            }
            if(transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (deleteTagIfUnused): Error during rollback: " + re.getMessage()); }
            }
            throw new TagOperationException(
                    "Database error deleting tag '" + processedTagName + "'", e);
        } catch (TagOperationException e) {
             if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagService (deleteTagIfUnused): Error during rollback for specific exc: " + re.getMessage()); }
            }
            throw e;
        } finally {
            if (transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()) {
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("TagServiceImpl: Error ensuring transaction is rolled back in finally (deleteTagIfUnused): " + se.getMessage());}
            }
        }
    }
}