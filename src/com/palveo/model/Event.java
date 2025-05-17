package com.palveo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Event {

    private int id;
    private int hostUserId;
    private String title;
    private String description;
    private LocalDateTime eventDateTime;
    private String locationString;
    private Double latitude;
    private Double longitude;
    private EventCategory category;
    private PrivacySetting privacy;
    private String eventImagePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum PrivacySetting {
        PUBLIC("Public"),
        PRIVATE("Private");

        private final String displayName;

        PrivacySetting(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public static PrivacySetting fromString(String text) {
            for (PrivacySetting b : PrivacySetting.values()) {
                if (b.displayName.equalsIgnoreCase(text) || b.name().equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return PUBLIC; 
        }
    }

    public enum EventCategory {
        SOCIAL("Social"),
        SPORTS("Sports"),
        MUSIC("Music"),
        FOOD("Food & Drink"),
        GAMING("Gaming"),
        STUDY("Study/Workshop"),
        ARTS_CULTURE("Arts & Culture"),
        OUTDOORS("Outdoors"),
        TECH("Technology"),
        OTHER("Other");

        private final String displayName;

        EventCategory(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public static EventCategory fromString(String text) {
            for (EventCategory ec : EventCategory.values()) {
                if (ec.displayName.equalsIgnoreCase(text) || ec.name().equalsIgnoreCase(text)) {
                    return ec;
                }
            }
            return OTHER; 
        }
    }


    public Event() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.privacy = PrivacySetting.PUBLIC;
        this.category = EventCategory.OTHER;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(int hostUserId) {
        this.hostUserId = hostUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(LocalDateTime eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public String getLocationString() {
        return locationString;
    }

    public void setLocationString(String locationString) {
        this.locationString = locationString;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }
    
    public void setCategory(String categoryString) {
        this.category = EventCategory.fromString(categoryString);
    }


    public PrivacySetting getPrivacy() {
        return privacy;
    }

    public void setPrivacy(PrivacySetting privacy) {
        this.privacy = privacy;
    }
    
    public void setPrivacy(String privacyString) {
         this.privacy = PrivacySetting.fromString(privacyString);
    }


    public String getEventImagePath() {
        return eventImagePath;
    }

    public void setEventImagePath(String eventImagePath) {
        this.eventImagePath = eventImagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Event event = (Event) o;
        return id == event.id && hostUserId == event.hostUserId
                && Objects.equals(title, event.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hostUserId, title);
    }

    @Override
    public String toString() {
        return "Event{" + "id=" + id + ", hostUserId=" + hostUserId + ", title='" + title + '\''
                + ", eventDateTime=" + eventDateTime + '}';
    }
}