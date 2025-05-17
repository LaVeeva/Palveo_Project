package com.palveo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Comment {
    private int commentId;
    private int authorUserId;
    private Integer targetEventId;
    private Integer targetProfileUserId;
    private Integer parentCommentId;
    private String content;
    private boolean isEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorUsername;

    public Comment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isEdited = false;
    }

    public Comment(int authorUserId, String content) {
        this();
        this.authorUserId = authorUserId;
        this.content = content;
    }

    public int getCommentId() { return commentId; }
    public void setCommentId(int commentId) { this.commentId = commentId; }

    public int getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(int authorUserId) { this.authorUserId = authorUserId; }

    public Integer getTargetEventId() { return targetEventId; }
    public void setTargetEventId(Integer targetEventId) { this.targetEventId = targetEventId; }

    public Integer getTargetProfileUserId() { return targetProfileUserId; }
    public void setTargetProfileUserId(Integer targetProfileUserId) { this.targetProfileUserId = targetProfileUserId; }

    public Integer getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Integer parentCommentId) { this.parentCommentId = parentCommentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return commentId == comment.commentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commentId);
    }

    @Override
    public String toString() {
        return "Comment{id=" + commentId +
               ", author=" + authorUserId + (authorUsername != null ? " (" + authorUsername + ")" : "") +
               (targetEventId != null ? ", event=" + targetEventId : "") +
               (targetProfileUserId != null ? ", profile=" + targetProfileUserId : "") +
               (parentCommentId != null ? ", parent=" + parentCommentId : "") +
               ", edited=" + isEdited +
               ", content='" + (content != null && content.length() > 20 ? content.substring(0, 20) + "..." : content) + "'}";
    }
}