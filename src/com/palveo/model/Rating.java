package com.palveo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Rating {

    public enum RatedEntityType {
        EVENT, USER
    }

    private int ratingId;
    private int raterUserId; 
    private RatedEntityType ratedEntityType;
    private int ratedEntityId; 
    private int score; 
    private String comment; 
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    
    private String raterUsername;

    public Rating() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Rating(int raterUserId, RatedEntityType ratedEntityType, int ratedEntityId, int score,
            String comment) {
        this();
        this.raterUserId = raterUserId;
        this.ratedEntityType = ratedEntityType;
        this.ratedEntityId = ratedEntityId;
        this.score = score;
        this.comment = comment;
    }

    public int getRatingId() {
        return ratingId;
    }

    public void setRatingId(int ratingId) {
        this.ratingId = ratingId;
    }

    public int getRaterUserId() {
        return raterUserId;
    }

    public void setRaterUserId(int raterUserId) {
        this.raterUserId = raterUserId;
    }

    public RatedEntityType getRatedEntityType() {
        return ratedEntityType;
    }

    public void setRatedEntityType(RatedEntityType ratedEntityType) {
        this.ratedEntityType = ratedEntityType;
    }

    public int getRatedEntityId() {
        return ratedEntityId;
    }

    public void setRatedEntityId(int ratedEntityId) {
        this.ratedEntityId = ratedEntityId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public String getRaterUsername() {
        return raterUsername;
    }

    public void setRaterUsername(String raterUsername) {
        this.raterUsername = raterUsername;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Rating rating = (Rating) o;
        if (ratingId > 0 && rating.ratingId > 0) {
            return ratingId == rating.ratingId;
        }
        return raterUserId == rating.raterUserId && ratedEntityId == rating.ratedEntityId
                && ratedEntityType == rating.ratedEntityType;
    }

    @Override
    public int hashCode() {
        if (ratingId > 0) {
            return Objects.hash(ratingId);
        }
        return Objects.hash(raterUserId, ratedEntityType, ratedEntityId);
    }

    @Override
    public String toString() {
        return "Rating{" + "ratingId=" + ratingId + ", raterUserId=" + raterUserId
                + (raterUsername != null ? " (" + raterUsername + ")" : "") + ", ratedEntityType="
                + ratedEntityType + ", ratedEntityId=" + ratedEntityId + ", score=" + score
                + ", comment='"
                + (comment != null && comment.length() > 20 ? comment.substring(0, 20) + "..."
                        : comment)
                + '\'' + ", createdAt=" + createdAt + '}';
    }
}
