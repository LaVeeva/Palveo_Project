package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Friendship;

public interface FriendshipDao {
    int saveNewRequest(Friendship friendship) throws SQLException;
    int saveNewRequest(Friendship friendship, Connection conn) throws SQLException;

    boolean updateStatus(int userOneId, int userTwoId, Friendship.FriendshipStatus newStatus,
            int actionUserId) throws SQLException;
    boolean updateStatus(int userOneId, int userTwoId, Friendship.FriendshipStatus newStatus,
            int actionUserId, Connection conn) throws SQLException;

    Optional<Friendship> findFriendship(int userAId, int userBId) throws SQLException;

    List<Friendship> findFriendshipsByUserId(int userId) throws SQLException;

    List<Friendship> findFriendshipsByUserIdAndStatus(int userId,
            Friendship.FriendshipStatus status) throws SQLException;

    boolean deleteFriendship(int userOneId, int userTwoId) throws SQLException;
    boolean deleteFriendship(int userOneId, int userTwoId, Connection conn) throws SQLException;
}