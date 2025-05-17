package com.palveo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Participant {
    private int id;
    private int eventId;
    private int userId;
    private RsvpStatus status;
    private LocalDateTime rsvpTimestamp;

    public enum RsvpStatus {
        JOINED, ATTENDED, DECLINED, INVITED
    }

    public Participant() {
        this.rsvpTimestamp = LocalDateTime.now();
    }

    public Participant(int eventId, int userId, RsvpStatus status) {
        this.eventId = eventId;
        this.userId = userId;
        this.status = status;
        this.rsvpTimestamp = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public RsvpStatus getStatus() { return status; }
    public void setStatus(RsvpStatus status) { this.status = status; }
    public LocalDateTime getRsvpTimestamp() { return rsvpTimestamp; }
    public void setRsvpTimestamp(LocalDateTime rsvpTimestamp) { this.rsvpTimestamp = rsvpTimestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return eventId == that.eventId && userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, userId);
    }

    @Override
    public String toString() {
        return "Participant{id=" + id + ", eventId=" + eventId + ", userId=" + userId + ", status=" + status + ", rsvpTimestamp=" + rsvpTimestamp + '}';
    }
}