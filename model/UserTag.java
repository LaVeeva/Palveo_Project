package com.palveo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserTag {
    private int userTagId;
    private int userId;
    private int tagId;
    private Integer taggedByUserId;
    private LocalDateTime taggedAt;

    private String tagName;
    private String taggedByUsername;

    public UserTag() {
        this.taggedAt = LocalDateTime.now();
    }

    public UserTag(int userId, int tagId, Integer taggedByUserId) {
        this();
        this.userId = userId;
        this.tagId = tagId;
        this.taggedByUserId = taggedByUserId;
    }

    
    public int getUserTagId() { return userTagId; }
    public void setUserTagId(int userTagId) { this.userTagId = userTagId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getTagId() { return tagId; }
    public void setTagId(int tagId) { this.tagId = tagId; }

    public Integer getTaggedByUserId() { return taggedByUserId; }
    public void setTaggedByUserId(Integer taggedByUserId) { this.taggedByUserId = taggedByUserId; }

    public LocalDateTime getTaggedAt() { return taggedAt; }
    public void setTaggedAt(LocalDateTime taggedAt) { this.taggedAt = taggedAt; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public String getTaggedByUsername() { return taggedByUsername; }
    public void setTaggedByUsername(String taggedByUsername) { this.taggedByUsername = taggedByUsername; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTag userTag = (UserTag) o;
        if (userTagId > 0 && userTag.userTagId > 0) {
            return userTagId == userTag.userTagId;
        }
        return userId == userTag.userId &&
               tagId == userTag.tagId &&
               Objects.equals(taggedByUserId, userTag.taggedByUserId);
    }

    @Override
    public int hashCode() {
        if (userTagId > 0) {
            return Objects.hash(userTagId);
        }
        return Objects.hash(userId, tagId, taggedByUserId);
    }

    @Override
    public String toString() {
        return "UserTag{userTagId=" + userTagId + ", userId=" + userId + ", tagId=" + tagId +
               (tagName != null ? " ('" + tagName + "')" : "") +
               (taggedByUserId != null ? ", taggedByUserId=" + taggedByUserId : "") +
               (taggedByUsername != null ? " (" + taggedByUsername + ")" : "") +
               ", taggedAt=" + taggedAt + "}";
    }
}