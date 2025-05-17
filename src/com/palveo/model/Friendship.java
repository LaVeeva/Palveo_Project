package com.palveo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Friendship {

    public enum FriendshipStatus {
        PENDING, 
        ACCEPTED, 
        DECLINED, 
        BLOCKED 
    }

    private int friendshipId;
    private int userOneId; 
    private int userTwoId; 
    private FriendshipStatus status;
    private int actionUserId; 
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Friendship() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    
    public Friendship(int userAId, int userBId, FriendshipStatus status, int actionUserId) {
        this();
        if (userAId == userBId) {
            throw new IllegalArgumentException("User IDs in a friendship cannot be the same.");
        }
        
        if (userAId < userBId) {
            this.userOneId = userAId;
            this.userTwoId = userBId;
        } else {
            this.userOneId = userBId;
            this.userTwoId = userAId;
        }
        this.status = status;
        this.actionUserId = actionUserId;
    }

    public int getFriendshipId() {
        return friendshipId;
    }

    public void setFriendshipId(int friendshipId) {
        this.friendshipId = friendshipId;
    }

    public int getUserOneId() {
        return userOneId;
    }

    public void setUserOneId(int userOneId) {
        
        this.userOneId = userOneId;
    }

    public int getUserTwoId() {
        return userTwoId;
    }

    public void setUserTwoId(int userTwoId) {
        this.userTwoId = userTwoId;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }

    public int getActionUserId() {
        return actionUserId;
    }

    public void setActionUserId(int actionUserId) {
        this.actionUserId = actionUserId;
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

    
    public int getOtherUserId(int currentUserId) {
        if (currentUserId == userOneId) {
            return userTwoId;
        } else if (currentUserId == userTwoId) {
            return userOneId;
        } else {
            throw new IllegalArgumentException("Provided userId " + currentUserId
                    + " is not part of this friendship (" + userOneId + ", " + userTwoId + ")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Friendship that = (Friendship) o;
        if (friendshipId > 0 && that.friendshipId > 0) {
            return friendshipId == that.friendshipId;
        }
        
        
        int thisMinId = Math.min(userOneId, userTwoId);
        int thisMaxId = Math.max(userOneId, userTwoId);
        int thatMinId = Math.min(that.userOneId, that.userTwoId);
        int thatMaxId = Math.max(that.userOneId, that.userTwoId);
        return thisMinId == thatMinId && thisMaxId == thatMaxId;
    }

    @Override
    public int hashCode() {
        if (friendshipId > 0) {
            return Objects.hash(friendshipId);
        }
        
        return Objects.hash(Math.min(userOneId, userTwoId), Math.max(userOneId, userTwoId));
    }

    @Override
    public String toString() {
        return "Friendship{" + "friendshipId=" + friendshipId + ", userOneId=" + userOneId
                + ", userTwoId=" + userTwoId + ", status=" + status + ", actionUserId="
                + actionUserId + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}
