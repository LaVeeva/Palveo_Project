package com.palveo.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.palveo.dao.FriendshipDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.db.exception.DuplicateKeyException;
import com.palveo.model.Friendship;

public class FriendshipDaoImpl implements FriendshipDao {

    private Friendship mapResultSetToFriendship(ResultSet rs) throws SQLException {
        Friendship friendship = new Friendship();
        friendship.setFriendshipId(rs.getInt("friendship_id"));
        friendship.setUserOneId(rs.getInt("user_one_id"));
        friendship.setUserTwoId(rs.getInt("user_two_id"));
        friendship.setStatus(Friendship.FriendshipStatus.valueOf(rs.getString("status")));
        friendship.setActionUserId(rs.getInt("action_user_id"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null)
            friendship.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null)
            friendship.setUpdatedAt(updatedAt.toLocalDateTime());
        return friendship;
    }

    private void setOrderedUserIds(PreparedStatement pstmt, int paramIndex1, int paramIndex2,
            int userAId, int userBId) throws SQLException {
        if (userAId < userBId) {
            pstmt.setInt(paramIndex1, userAId);
            pstmt.setInt(paramIndex2, userBId);
        } else {
            pstmt.setInt(paramIndex1, userBId);
            pstmt.setInt(paramIndex2, userAId);
        }
    }
    
    @Override
    public int saveNewRequest(Friendship friendship) throws SQLException {
        return saveNewRequest(friendship, null);
    }

    @Override
    public int saveNewRequest(Friendship friendship, Connection conn) throws SQLException {
        String sql =
                "INSERT INTO friendships (user_one_id, user_two_id, status, action_user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null ) {
            System.err.println("FriendshipDaoImpl.saveNewRequest: No DB Conn");
            return -1;
        }

        try (PreparedStatement pstmt =
                dbConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, friendship.getUserOneId());
            pstmt.setInt(2, friendship.getUserTwoId());
            pstmt.setString(3, friendship.getStatus().toString());
            pstmt.setInt(4, friendship.getActionUserId());
            pstmt.setTimestamp(5, Timestamp.valueOf(friendship.getCreatedAt()));
            pstmt.setTimestamp(6, Timestamp.valueOf(friendship.getUpdatedAt()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0)
                return -1;

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    friendship.setFriendshipId(generatedKeys.getInt(1));
                    return friendship.getFriendshipId();
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("uq_friendship_pair"))) {
                throw new DuplicateKeyException("Duplicate friendship pair for user_one_id="
                        + friendship.getUserOneId() + ", user_two_id=" + friendship.getUserTwoId(), e.getSQLState(), e.getErrorCode(), e);
            }
            System.err.println("FriendshipDaoImpl.saveNewRequest: SQLException: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
            }
        }
    }
    
    @Override
    public boolean updateStatus(int userAId, int userBId, Friendship.FriendshipStatus newStatus,
            int actionUserId) throws SQLException {
        return updateStatus(userAId, userBId, newStatus, actionUserId, null);
    }

    @Override
    public boolean updateStatus(int userAId, int userBId, Friendship.FriendshipStatus newStatus,
            int actionUserId, Connection conn) throws SQLException {
        String sql =
                "UPDATE friendships SET status = ?, action_user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE user_one_id = ? AND user_two_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) {
            System.err.println("FriendshipDaoImpl.updateStatus: No DB Conn");
            return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.toString());
            pstmt.setInt(2, actionUserId);
            setOrderedUserIds(pstmt, 3, 4, userAId, userBId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("FriendshipDaoImpl.updateStatus: SQLException: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
               
            }
        }
    }
    
    @Override
    public boolean deleteFriendship(int userAId, int userBId) throws SQLException {
        return deleteFriendship(userAId, userBId, null);
    }

    @Override
    public boolean deleteFriendship(int userAId, int userBId, Connection conn) throws SQLException {
        String sql = "DELETE FROM friendships WHERE user_one_id = ? AND user_two_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) {
            System.err.println("FriendshipDaoImpl.deleteFriendship: No DB Conn");
            return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            setOrderedUserIds(pstmt, 1, 2, userAId, userBId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("FriendshipDaoImpl.deleteFriendship: SQLException: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
            }
        }
    }

    @Override
    public Optional<Friendship> findFriendship(int userAId, int userBId) throws SQLException {
        String sql = "SELECT * FROM friendships WHERE user_one_id = ? AND user_two_id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("FriendshipDaoImpl.findFriendship: No DB Conn");
            return Optional.empty();
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            setOrderedUserIds(pstmt, 1, 2, userAId, userBId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToFriendship(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("FriendshipDaoImpl.findFriendship: SQLException: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Friendship> findFriendshipsByUserId(int userId) throws SQLException {
        List<Friendship> friendships = new ArrayList<>();
        String sql = "SELECT * FROM friendships WHERE user_one_id = ? OR user_two_id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("FriendshipDaoImpl.findFriendshipsByUserId: No DB Conn");
            return friendships;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    friendships.add(mapResultSetToFriendship(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("FriendshipDaoImpl.findFriendshipsByUserId: SQLException: " + e.getMessage());
            throw e;
        }
        return friendships;
    }

    @Override
    public List<Friendship> findFriendshipsByUserIdAndStatus(int userId,
            Friendship.FriendshipStatus status) throws SQLException {
        List<Friendship> friendships = new ArrayList<>();
        String sql = "SELECT * FROM friendships WHERE (user_one_id = ? OR user_two_id = ?) AND status = ?";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("FriendshipDaoImpl.findFriendshipsByUserIdAndStatus: No DB Conn");
            return friendships;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, status.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    friendships.add(mapResultSetToFriendship(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("FriendshipDaoImpl.findFriendshipsByUserIdAndStatus: SQLException: " + e.getMessage());
            throw e;
        }
        return friendships;
    }
}