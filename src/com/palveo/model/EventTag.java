package com.palveo.model;

import java.util.Objects;

public class EventTag {

    private int eventId;
    private int tagId;
    private int addedByUserId;

    public EventTag() {}

    public EventTag(int eventId, int tagId, int addedByUserId) {
        this.eventId = eventId;
        this.tagId = tagId;
        this.addedByUserId = addedByUserId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public int getAddedByUserId() {
        return addedByUserId;
    }

    public void setAddedByUserId(int addedByUserId) {
        this.addedByUserId = addedByUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventTag eventTag = (EventTag) o;
        return eventId == eventTag.eventId &&
               tagId == eventTag.tagId &&
               addedByUserId == eventTag.addedByUserId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, tagId, addedByUserId);
    }

    @Override
    public String toString() {
        return "EventTag{" +
               "eventId=" + eventId +
               ", tagId=" + tagId +
               ", addedByUserId=" + addedByUserId +
               '}';
    }
}