package com.palveo.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.palveo.dao.EventDao;
import com.palveo.dao.ParticipantDao;
import com.palveo.dao.UserDao;
import com.palveo.dao.impl.EventDaoImpl;
import com.palveo.dao.impl.ParticipantDaoImpl;
import com.palveo.dao.impl.UserDaoImpl;
import com.palveo.db.DatabaseConnection;
import com.palveo.db.exception.DuplicateKeyException;
import com.palveo.model.Event;
import com.palveo.model.Friendship;
import com.palveo.model.Participant;
import com.palveo.model.User;
import com.palveo.service.FriendshipService;
import com.palveo.service.ParticipantService;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.EventOperationException;
import com.palveo.service.exception.UserNotFoundException;

public class ParticipantServiceImpl implements ParticipantService {

    private ParticipantDao participantDao;
    private EventDao eventDao;
    private UserDao userDao;
    private FriendshipService friendshipService;

    public ParticipantServiceImpl() {
        this.participantDao = new ParticipantDaoImpl();
        this.eventDao = new EventDaoImpl();
        this.userDao = new UserDaoImpl();
        this.friendshipService = new FriendshipServiceImpl();
    }

    public ParticipantServiceImpl(ParticipantDao participantDao, EventDao eventDao, UserDao userDao,
            FriendshipService friendshipService) {
        this.participantDao = participantDao;
        this.eventDao = eventDao;
        this.userDao = userDao;
        this.friendshipService = friendshipService;
    }

    private Event getEventOrThrow(int eventId)
            throws EventNotFoundException, EventOperationException {
        try {
            return eventDao.findById(eventId).orElseThrow(
                    () -> new EventNotFoundException("Event not found with ID: " + eventId));
        } catch (SQLException e) {
            System.err.println("DB Error fetching event " + eventId + ": " + e.getMessage());
            throw new EventOperationException(
                    "Database error while fetching event details for ID: " + eventId, e);
        }
    }

    @Override
    public Participant joinEvent(int eventId, User user)
            throws EventOperationException, EventNotFoundException {
        if (user == null)
            throw new EventOperationException("User cannot be null.");
        Event event = getEventOrThrow(eventId);

        if (event.getEventDateTime().isBefore(LocalDateTime.now())) {
            throw new EventOperationException("Cannot join a past event.");
        }

        if (event.getPrivacy() == Event.PrivacySetting.PRIVATE) {
            if (event.getHostUserId() != user.getId()) {
                boolean isFriend = false;
                try {
                    User hostUser = userDao.findById(event.getHostUserId())
                            .orElseThrow(() -> new EventOperationException(
                                    "Host user not found for private event check."));
                    isFriend = friendshipService.getFriendshipStatus(user,
                            hostUser) == Friendship.FriendshipStatus.ACCEPTED;
                } catch (UserNotFoundException
                        | com.palveo.service.exception.FriendshipOperationException e) {
                    throw new EventOperationException(
                            "Could not verify friendship status for private event.", e);
                } catch (SQLException eSqlex) {
                    throw new EventOperationException(
                            "Database error verifying host for private event.", eSqlex);
                }

                if (!isFriend) {
                    throw new EventOperationException(
                            "This is a private event and you are not friends with the host.");
                }
            }
        }
        
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();
            Optional<Participant> existingRsvpOpt =
                    participantDao.findByEventIdAndUserId(eventId, user.getId());
            if (existingRsvpOpt.isPresent()) {
                Participant existingRsvp = existingRsvpOpt.get();
                if (existingRsvp.getStatus() == Participant.RsvpStatus.JOINED
                        || existingRsvp.getStatus() == Participant.RsvpStatus.ATTENDED) {
                    DatabaseConnection.commitTransaction();
                    return existingRsvp;
                } else {
                    Participant updatedRsvp = updateRsvpStatusInternal(existingRsvp, Participant.RsvpStatus.JOINED, user.getId(), conn);
                    DatabaseConnection.commitTransaction();
                    return updatedRsvp;
                }
            } else {
                Participant newParticipant =
                        new Participant(eventId, user.getId(), Participant.RsvpStatus.JOINED);
                int newId = participantDao.save(newParticipant, conn);
                newParticipant.setId(newId);
                DatabaseConnection.commitTransaction();
                return newParticipant;
            }
        } catch (DuplicateKeyException dke) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (joinEvent DupKey): " + re.getMessage());}
            throw new EventOperationException("You are already part of this event or have an existing RSVP.", dke);
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (joinEvent SQL): " + re.getMessage());}
            System.err.println("DB Error joining event: " + e.getMessage());
            throw new EventOperationException("Database error while joining event.", e);
        } catch (EventOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (joinEvent Op): " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("ParticipantServiceImpl: Error ensuring transaction is rolled back in finally (joinEvent): " + se.getMessage());}
            }
        }
    }

    @Override
    public void leaveEvent(int eventId, User user)
            throws EventOperationException, EventNotFoundException {
        if (user == null)
            throw new EventOperationException("User cannot be null.");
        Event event = getEventOrThrow(eventId);

        if (event.getEventDateTime().isBefore(LocalDateTime.now())) {
            throw new EventOperationException("Cannot leave an event that has already passed.");
        }
        
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();
            boolean deleted = participantDao.delete(eventId, user.getId(), conn);
            if (!deleted) {
                Optional<Participant> rsvp =
                        participantDao.findByEventIdAndUserId(eventId, user.getId());
                if (rsvp.isPresent() && rsvp.get().getStatus() == Participant.RsvpStatus.DECLINED) {
                    System.out.println("User " + user.getUsername()
                            + " already declined/not part of event " + eventId);
                    DatabaseConnection.commitTransaction();
                    return;
                }
                throw new EventOperationException(
                        "You were not actively RSVP'd as JOINED/ATTENDED to this event or the removal failed.");
            }
            DatabaseConnection.commitTransaction();
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (leaveEvent SQL): " + re.getMessage());}
            System.err.println("DB Error leaving event: " + e.getMessage());
            throw new EventOperationException("Database error while leaving event.", e);
        } catch (EventOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (leaveEvent Op): " + re.getMessage());}
            throw e;
        } finally {
             if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("ParticipantServiceImpl: Error ensuring transaction is rolled back in finally (leaveEvent): " + se.getMessage());}
            }
        }
    }

    @Override
    public Participant updateRsvpStatus(int eventId, User user, Participant.RsvpStatus newStatus)
            throws EventOperationException, EventNotFoundException {
        if (user == null)
            throw new EventOperationException("User cannot be null.");
        if (newStatus == null)
            throw new EventOperationException("New RSVP status cannot be null.");
        Event event = getEventOrThrow(eventId);

        if (event.getEventDateTime().isBefore(LocalDateTime.now())) {
            if (newStatus == Participant.RsvpStatus.ATTENDED
                    && event.getHostUserId() == user.getId()) { 
                throw new EventOperationException(
                        "Host should use 'markAttendance' to set ATTENDED status for past events for others, or adjust own attendance directly if intended (less common scenario).");
            } else if (newStatus != Participant.RsvpStatus.ATTENDED) { 
                throw new EventOperationException("Cannot change RSVP for a past event, unless marking attendance by host.");
            }
        }
        
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();
            Optional<Participant> existingRsvpOpt =
                    participantDao.findByEventIdAndUserId(eventId, user.getId());
            if (existingRsvpOpt.isPresent()) {
                Participant existingRsvp = existingRsvpOpt.get();
                if (newStatus == Participant.RsvpStatus.ATTENDED
                        && event.getHostUserId() != user.getId()) {
                    throw new EventOperationException(
                            "Attendance status can only be marked by the event host.");
                }
                Participant updatedRsvp = updateRsvpStatusInternal(existingRsvp, newStatus, user.getId(), conn);
                DatabaseConnection.commitTransaction();
                return updatedRsvp;
            } else {
                if (newStatus == Participant.RsvpStatus.JOINED) {
                    DatabaseConnection.commitTransaction(); 
                    return joinEvent(eventId, user); 
                }
                if (newStatus == Participant.RsvpStatus.DECLINED) {
                    Participant declinedParticipant =
                            new Participant(eventId, user.getId(), Participant.RsvpStatus.DECLINED);
                    int newId = participantDao.save(declinedParticipant, conn);
                    if (newId > 0) {
                        declinedParticipant.setId(newId);
                        DatabaseConnection.commitTransaction();
                        return declinedParticipant;
                    } else {
                         throw new EventOperationException(
                                "Failed to record 'DECLINED' status for non-participant. DAO Error.");
                    }
                }
                throw new EventOperationException("No existing RSVP found to update status to "
                        + newStatus
                        + ". User may need to join the event first (if public/friend's private).");
            }
        } catch (DuplicateKeyException dke) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (updateRsvpStatus DupKey): " + re.getMessage());}
            throw new EventOperationException("Failed to save new RSVP status due to a conflict (likely already exists).", dke);
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (updateRsvpStatus SQL): " + re.getMessage());}
            System.err.println("DB Error updating RSVP status: " + e.getMessage());
            throw new EventOperationException("Database error while updating RSVP status.", e);
        } catch (EventOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (updateRsvpStatus Op/Event): " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("ParticipantServiceImpl: Error ensuring transaction is rolled back in finally (updateRsvpStatus): " + se.getMessage());}
            }
        }
    }

    private Participant updateRsvpStatusInternal(Participant rsvp, Participant.RsvpStatus newStatus,
            int actionUserId, Connection conn) throws SQLException, EventOperationException {
        if (rsvp.getStatus() == newStatus) {
            return rsvp;
        }
        boolean updated =
                participantDao.updateStatus(rsvp.getEventId(), rsvp.getUserId(), newStatus, conn);
        if (updated) {
            rsvp.setStatus(newStatus);
            rsvp.setRsvpTimestamp(LocalDateTime.now());
            return rsvp;
        } else {
            throw new EventOperationException(
                    "Failed to update RSVP status in the database for user " + rsvp.getUserId()
                            + " and event " + rsvp.getEventId());
        }
    }

    @Override
    public Optional<Participant> getUserRsvpForEvent(int eventId, int userId) {
        try {
            return participantDao.findByEventIdAndUserId(eventId, userId);
        } catch (SQLException e) {
            System.err.println("DB Error getting user RSVP for event " + eventId + ", user "
                    + userId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<User> getConfirmedUsersForEvent(int eventId)
            throws EventNotFoundException, EventOperationException {
        getEventOrThrow(eventId);
        List<User> confirmedUsers = new ArrayList<>();
        try {
            List<Participant> participants = participantDao.findByEventId(eventId);
            for (Participant p : participants) {
                if (p.getStatus() == Participant.RsvpStatus.JOINED
                        || p.getStatus() == Participant.RsvpStatus.ATTENDED) {
                    userDao.findById(p.getUserId()).ifPresent(confirmedUsers::add);
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error getting confirmed users for event " + eventId + ": "
                    + e.getMessage());
            throw new EventOperationException(
                    "Database error retrieving confirmed users for event.", e);
        }
        return confirmedUsers;
    }

    @Override
    public List<Event> getEventsUserIsAssociatedWith(int userId) throws EventOperationException {
        List<Event> associatedEvents = new ArrayList<>();
        try {
            List<Participant> rsvps = participantDao.findByUserId(userId);
            for (Participant p : rsvps) {
                try {
                    Event event = getEventOrThrow(p.getEventId());
                    associatedEvents.add(event);
                } catch (EventNotFoundException e) {
                    System.err.println("Event (ID: " + p.getEventId() + ") associated with user "
                            + userId + " was not found. Skipping.");
                }
            }
        } catch (SQLException e) {
            System.err.println("DB Error getting events user " + userId + " is associated with: "
                    + e.getMessage());
            throw new EventOperationException("Database error retrieving events for user.", e);
        }
        return associatedEvents;
    }

    @Override
    public Participant markAttendance(int eventId, int userIdToMark, User actionUser)
            throws EventOperationException, EventNotFoundException {
        if (actionUser == null) {
            throw new EventOperationException("Action user (host) cannot be null.");
        }
        Event event = getEventOrThrow(eventId);

        if (event.getHostUserId() != actionUser.getId()) {
            throw new EventOperationException(
                    "Unauthorized: Only the event host can mark attendance.");
        }

        if (event.getEventDateTime().isAfter(LocalDateTime.now().plusHours(1))) {
            throw new EventOperationException(
                    "Cannot mark attendance for an event that has not significantly passed or started yet.");
        }
        
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();
            Optional<Participant> rsvpOpt =
                    participantDao.findByEventIdAndUserId(eventId, userIdToMark);
            Participant rsvpToUpdate;

            if (rsvpOpt.isPresent()) {
                rsvpToUpdate = rsvpOpt.get();
                if (rsvpToUpdate.getStatus() == Participant.RsvpStatus.ATTENDED) {
                    DatabaseConnection.commitTransaction();
                    return rsvpToUpdate;
                }
                Participant updatedRsvp = updateRsvpStatusInternal(rsvpToUpdate, Participant.RsvpStatus.ATTENDED, actionUser.getId(), conn);
                DatabaseConnection.commitTransaction();
                return updatedRsvp;
            } else {
                System.out.println("User " + userIdToMark + " has no prior RSVP for event "
                        + eventId + ". Host is adding and marking as attended.");
                Participant newAttendance =
                        new Participant(eventId, userIdToMark, Participant.RsvpStatus.ATTENDED);
                int newId = participantDao.save(newAttendance, conn);
                 if (newId > 0) {
                    newAttendance.setId(newId);
                    DatabaseConnection.commitTransaction();
                    return newAttendance;
                } else {
                    throw new EventOperationException("Failed to record new attendance for user "
                            + userIdToMark + ". DAO Error.");
                }
            }
        } catch (DuplicateKeyException dke){
             try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (markAttendance DupKey): " + re.getMessage());}
            throw new EventOperationException("Failed to record attendance due to an existing conflicting record.", dke);
        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (markAttendance SQL): " + re.getMessage());}
            System.err.println("DB Error marking attendance for user " + userIdToMark + " at event "
                    + eventId + ": " + e.getMessage());
            throw new EventOperationException("Database error while marking attendance.", e);
        } catch (EventOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("ParticipantService: Error during rollback (markAttendance Op): " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("ParticipantServiceImpl: Error ensuring transaction is rolled back in finally (markAttendance): " + se.getMessage());}
            }
        }
    }
}