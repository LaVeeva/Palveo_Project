package com.palveo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Event;
import com.palveo.model.User;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.EventOperationException;
import com.palveo.service.exception.UserNotFoundException;

public interface EventService {

    Event createEvent(Event eventDetails, User host) throws EventOperationException;

    Optional<Event> getEventById(int eventId);

    List<Event> getEventsHostedBy(int userId);

    List<Event> getUpcomingPublicEvents();

    List<Event> getUpcomingEventsForFeed(User currentUser)
            throws UserNotFoundException, EventOperationException;

    List<Event> searchPublicEvents(String searchTerm, String category, LocalDateTime dateFrom,
            LocalDateTime dateTo);

    Event updateEvent(int eventId, Event eventDetails, User currentUser)
            throws EventOperationException, EventNotFoundException;

    void cancelEvent(int eventId, User currentUser)
            throws EventOperationException, EventNotFoundException;
}
