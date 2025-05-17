package com.palveo.service;

import java.util.List;
import java.util.Optional;
import com.palveo.model.Event;
import com.palveo.model.Participant;
import com.palveo.model.User;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.EventOperationException;

public interface ParticipantService {

    Participant joinEvent(int eventId, User user) throws EventOperationException, EventNotFoundException;

    void leaveEvent(int eventId, User user) throws EventOperationException, EventNotFoundException;

    Participant updateRsvpStatus(int eventId, User user, Participant.RsvpStatus newStatus) throws EventOperationException, EventNotFoundException;

    Optional<Participant> getUserRsvpForEvent(int eventId, int userId);

    List<User> getConfirmedUsersForEvent(int eventId) throws EventNotFoundException, EventOperationException;

    List<Event> getEventsUserIsAssociatedWith(int userId) throws EventOperationException;

    Participant markAttendance(int eventId, int userIdToMark, User actionUser) throws EventOperationException, EventNotFoundException;
    
}