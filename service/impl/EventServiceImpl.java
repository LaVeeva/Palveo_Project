package com.palveo.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.palveo.dao.EventDao;
import com.palveo.dao.impl.EventDaoImpl;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.Event;
import com.palveo.model.User;
import com.palveo.service.EventService;
import com.palveo.service.FriendshipService;
import com.palveo.service.exception.EventCreationException;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.EventOperationException;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.util.ValidationUtils;

public class EventServiceImpl implements EventService {

    private EventDao eventDao;
    private FriendshipService friendshipService;

    public EventServiceImpl() {
        this.eventDao = new EventDaoImpl();
        this.friendshipService = new FriendshipServiceImpl();
    }

    public EventServiceImpl(EventDao eventDao, FriendshipService friendshipService) {
        this.eventDao = eventDao;
        this.friendshipService = friendshipService;
    }

    @Override
    public Event createEvent(Event eventDetails, User host)
            throws EventOperationException {
        if (host == null) {
            throw new EventOperationException("Host user required.");
        }
        eventDetails.setHostUserId(host.getId());

        if (ValidationUtils.isNullOrEmpty(eventDetails.getTitle())) {
            throw new EventOperationException("Event title required.");
        }
        if (eventDetails.getTitle().length() > 255) {
            throw new EventOperationException("Event title too long.");
        }
        if (eventDetails.getEventDateTime() == null
                || eventDetails.getEventDateTime().isBefore(LocalDateTime.now())) {
            throw new EventOperationException("Event date/time must be in the future.");
        }
        if (ValidationUtils.isNullOrEmpty(eventDetails.getLocationString())) {
            throw new EventOperationException("Event location required.");
        }
        if (eventDetails.getCategory() == null) {
            throw new EventOperationException("Event category required.");
        }
        if (eventDetails.getPrivacy() == null) {
             throw new EventOperationException("Event privacy setting required.");
        }

        LocalDateTime now = LocalDateTime.now();
        eventDetails.setCreatedAt(now);
        eventDetails.setUpdatedAt(now);
        
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();
            int eventId = eventDao.save(eventDetails, conn);
            if (eventId != -1) {
                eventDetails.setId(eventId);
                DatabaseConnection.commitTransaction();
                return eventDetails;
            } else {
                throw new EventCreationException("Failed to save event (DAO error).");
            }
        } catch (SQLException e) {
            try { if(conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("EventService: Error during rollback: " + re.getMessage());}
            System.err.println("DB Error creating event: " + e.getMessage());
            throw new EventCreationException("Database error during event creation.", e);
        } catch (EventOperationException e) { 
            try { if(conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("EventService: Error during rollback: " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("EventServiceImpl: Error ensuring transaction is rolled back in finally (createEvent): " + se.getMessage());}
            }
        }
    }

    @Override
    public Optional<Event> getEventById(int eventId) {
        try {
            return eventDao.findById(eventId);
        } catch (SQLException e) {
            System.err.println("DB Error getting event by ID " + eventId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Event> getEventsHostedBy(int userId) {
        try {
            return eventDao.findByHostId(userId);
        } catch (SQLException e) {
            System.err.println("DB Error getting events by host " + userId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Event> getUpcomingPublicEvents() {
        try {
            return eventDao.findUpcomingPublicEvents(LocalDateTime.now());
        } catch (SQLException e) {
            System.err.println("DB Error getting upcoming public events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Event> getUpcomingEventsForFeed(User currentUser)
            throws UserNotFoundException, EventOperationException {
        if (currentUser == null) {
            throw new UserNotFoundException("Current user cannot be null for feed generation.");
        }
        try {
            List<User> friends = friendshipService.listFriends(currentUser);
            List<Integer> friendIds =
                    friends.stream().map(User::getId).collect(Collectors.toList());
            return eventDao.findUpcomingEventsForFeed(currentUser.getId(), friendIds,
                    LocalDateTime.now());
        } catch (SQLException e) {
            System.err.println("DB Error getting upcoming events for feed for user "
                    + currentUser.getId() + ": " + e.getMessage());
            throw new EventOperationException("Database error retrieving events for feed.", e);
        } catch (com.palveo.service.exception.FriendshipOperationException e) {
            System.err.println("Friendship Error getting friends for feed for user "
                    + currentUser.getId() + ": " + e.getMessage());
            throw new EventOperationException("Error retrieving friend list for feed.", e);
        }
    }

    @Override
    public List<Event> searchPublicEvents(String searchTerm, String category,
            LocalDateTime dateFrom, LocalDateTime dateTo) {
        System.err.println(
                "Simplified searchPublicEvents: Only fetching all public events. Full search criteria not implemented in this version.");
        try {
            if (!ValidationUtils.isNullOrEmpty(category)) {
                System.err.println(
                        "Category filter in simplified searchPublicEvents would require new DAO method. Returning all public.");
                return eventDao.findAllPublicEvents();
            }
            List<Event> allPublic = eventDao.findAllPublicEvents();
            if (!ValidationUtils.isNullOrEmpty(searchTerm)) {
                String lowerSearch = searchTerm.toLowerCase();
                return allPublic.stream().filter(event -> event.getTitle().toLowerCase()
                        .contains(lowerSearch)
                        || (event.getDescription() != null
                                && event.getDescription().toLowerCase().contains(lowerSearch)))
                        .collect(Collectors.toList());
            }
            return allPublic;
        } catch (SQLException e) {
            System.err.println("DB Error searching public events: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Event updateEvent(int eventId, Event eventDetailsToUpdate, User currentUser)
            throws EventOperationException, EventNotFoundException {
        if (currentUser == null) {
            throw new EventOperationException("Current user required.");
        }
        if (eventDetailsToUpdate == null) {
            throw new EventOperationException("Event details required for update.");
        }

        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            Optional<Event> existingEventOpt = getEventById(eventId);
            if (!existingEventOpt.isPresent()) {
                throw new EventNotFoundException("Event not found (ID: " + eventId + ").");
            }
            Event existingEvent = existingEventOpt.get();

            if (existingEvent.getHostUserId() != currentUser.getId()) {
                throw new EventOperationException("Unauthorized to update this event.");
            }

            if (ValidationUtils.isNullOrEmpty(eventDetailsToUpdate.getTitle())) {
                throw new EventOperationException("Updated event title required.");
            }

            existingEvent.setTitle(eventDetailsToUpdate.getTitle());
            if (eventDetailsToUpdate.getDescription() != null) existingEvent.setDescription(eventDetailsToUpdate.getDescription());
            if (!ValidationUtils.isNullOrEmpty(eventDetailsToUpdate.getLocationString())) existingEvent.setLocationString(eventDetailsToUpdate.getLocationString());
            if (eventDetailsToUpdate.getEventDateTime() != null && eventDetailsToUpdate.getEventDateTime().isAfter(LocalDateTime.now())) {
                existingEvent.setEventDateTime(eventDetailsToUpdate.getEventDateTime());
            } else if (eventDetailsToUpdate.getEventDateTime() != null) {
                throw new EventOperationException("Updated event date/time must be in the future.");
            }
            if (eventDetailsToUpdate.getCategory() != null) existingEvent.setCategory(eventDetailsToUpdate.getCategory());
            if (eventDetailsToUpdate.getPrivacy() != null) existingEvent.setPrivacy(eventDetailsToUpdate.getPrivacy());
            if (eventDetailsToUpdate.getEventImagePath() != null) existingEvent.setEventImagePath(eventDetailsToUpdate.getEventImagePath());
            existingEvent.setUpdatedAt(LocalDateTime.now());

            boolean success = eventDao.update(existingEvent, conn);
            if (!success) {
                throw new EventOperationException("Failed to update event in DB (ID: " + eventId + ").");
            }
            DatabaseConnection.commitTransaction();
            return existingEvent;

        } catch (SQLException e) {
            try { if(conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("EventService: Error during rollback on updateEvent: " + re.getMessage());}
            System.err.println("DB Error updating event " + eventId + ": " + e.getMessage());
            throw new EventOperationException("Database error during event update.", e);
        } catch (EventOperationException e ) { 
             try { if(conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("EventService: Error during rollback on updateEvent: " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("EventServiceImpl: Error ensuring transaction is rolled back in finally (updateEvent): " + se.getMessage());}
            }
        }
    }

    @Override
    public void cancelEvent(int eventId, User currentUser)
            throws EventOperationException, EventNotFoundException {
        if (currentUser == null) {
            throw new EventOperationException("Current user required.");
        }
        
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();
            
            Optional<Event> existingEventOpt = getEventById(eventId);
            if (!existingEventOpt.isPresent()) {
                throw new EventNotFoundException("Event not found (ID: " + eventId + ").");
            }
            Event existingEvent = existingEventOpt.get();

            if (existingEvent.getHostUserId() != currentUser.getId()) {
                throw new EventOperationException("Unauthorized to cancel this event.");
            }

            boolean success = eventDao.delete(eventId, conn);
            if (!success) {
                throw new EventOperationException("Failed to cancel/delete event in DB (ID: " + eventId + ").");
            }
            DatabaseConnection.commitTransaction();
        } catch (SQLException e) {
            try { if(conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("EventService: Error during rollback on cancelEvent: " + re.getMessage());}
            System.err.println("DB Error cancelling event " + eventId + ": " + e.getMessage());
            throw new EventOperationException("Database error during event cancellation.", e);
        } catch (EventOperationException e){ 
            try { if(conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("EventService: Error during rollback on cancelEvent: " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("EventServiceImpl: Error ensuring transaction is rolled back in finally (cancelEvent): " + se.getMessage());}
            }
        }
    }
}