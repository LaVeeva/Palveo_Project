package com.palveo.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.palveo.dao.RatingDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.db.exception.DuplicateKeyException;
import com.palveo.model.Rating;

public class RatingDaoImpl implements RatingDao {

    private Rating mapResultSetToRating(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setRatingId(rs.getInt("r_rating_id"));
        rating.setRaterUserId(rs.getInt("r_rater_user_id"));
        rating.setRatedEntityType(
                Rating.RatedEntityType.valueOf(rs.getString("r_rated_entity_type")));
        rating.setRatedEntityId(rs.getInt("r_rated_entity_id"));
        rating.setScore(rs.getInt("r_score"));
        rating.setComment(rs.getString("r_comment"));
        Timestamp createdAt = rs.getTimestamp("r_created_at");
        if (createdAt != null) rating.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("r_updated_at");
        if (updatedAt != null) rating.setUpdatedAt(updatedAt.toLocalDateTime());
        try {
            rating.setRaterUsername(rs.getString("u_username"));
        } catch (SQLException e) {
        }
        return rating;
    }

    private String baseSelectWithRaterSQL() {
        return "SELECT r.rating_id as r_rating_id, r.rater_user_id as r_rater_user_id, "
                + "r.rated_entity_type as r_rated_entity_type, r.rated_entity_id as r_rated_entity_id, "
                + "r.score as r_score, r.comment as r_comment, r.created_at as r_created_at, r.updated_at as r_updated_at, "
                + "u.username as u_username "
                + "FROM ratings r JOIN users u ON r.rater_user_id = u.id ";
    }

    @Override
    public int save(Rating rating) throws SQLException {
        return save(rating, null);
    }
    
    @Override
    public int save(Rating rating, Connection conn) throws SQLException {
        String sql =
                "INSERT INTO ratings (rater_user_id, rated_entity_type, rated_entity_id, score, comment, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null ) { 
            System.err.println("RatingDaoImpl.save: No DB Conn"); 
            return -1;
        }

        try (PreparedStatement pstmt =
                dbConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, rating.getRaterUserId());
            pstmt.setString(2, rating.getRatedEntityType().toString());
            pstmt.setInt(3, rating.getRatedEntityId());
            pstmt.setInt(4, rating.getScore());
            if (rating.getComment() != null) {
                pstmt.setString(5, rating.getComment());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            pstmt.setTimestamp(6, Timestamp.valueOf(rating.getCreatedAt()));
            pstmt.setTimestamp(7, Timestamp.valueOf(rating.getUpdatedAt()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0)
                return -1;

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    rating.setRatingId(generatedKeys.getInt(1));
                    return rating.getRatingId();
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("uq_rating_instance"))) {
                throw new DuplicateKeyException("Duplicate rating instance for rater_user_id="
                        + rating.getRaterUserId() + ", type=" + rating.getRatedEntityType()
                        + ", entity_id=" + rating.getRatedEntityId(), e.getSQLState(), e.getErrorCode(), e);
            }
            System.err.println("RatingDaoImpl.save: SQLException: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
            }
        }
    }

    @Override
    public Optional<Rating> findById(int ratingId) throws SQLException {
        String sql = baseSelectWithRaterSQL() + "WHERE r.rating_id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("RatingDaoImpl.findById: No DB Conn");
            return Optional.empty();
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, ratingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRating(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("RatingDaoImpl.findById: SQLException for ID " + ratingId + ": "
                    + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Rating> findByRatedEntity(String entityType, int entityId) throws SQLException {
        List<Rating> ratings = new ArrayList<>();
        String sql = baseSelectWithRaterSQL()
                + "WHERE r.rated_entity_type = ? AND r.rated_entity_id = ? ORDER BY r.created_at DESC";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("RatingDaoImpl.findByRatedEntity: No DB Conn");
            return ratings;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, entityType);
            pstmt.setInt(2, entityId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ratings.add(mapResultSetToRating(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("RatingDaoImpl.findByRatedEntity: SQLException for type="
                    + entityType + ", id=" + entityId + ": " + e.getMessage());
            throw e;
        }
        return ratings;
    }

    @Override
    public List<Rating> findByRaterUserId(int raterUserId) throws SQLException {
        List<Rating> ratings = new ArrayList<>();
        String sql = baseSelectWithRaterSQL() + "WHERE r.rater_user_id = ? ORDER BY r.created_at DESC";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("RatingDaoImpl.findByRaterUserId: No DB Conn");
            return ratings;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, raterUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ratings.add(mapResultSetToRating(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("RatingDaoImpl.findByRaterUserId: SQLException for rater_id="
                    + raterUserId + ": " + e.getMessage());
            throw e;
        }
        return ratings;
    }

    @Override
    public Optional<Rating> findByRaterAndEntity(int raterUserId, String entityType, int entityId)
            throws SQLException {
        String sql = baseSelectWithRaterSQL()
                + "WHERE r.rater_user_id = ? AND r.rated_entity_type = ? AND r.rated_entity_id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("RatingDaoImpl.findByRaterAndEntity: No DB Conn");
            return Optional.empty();
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, raterUserId);
            pstmt.setString(2, entityType);
            pstmt.setInt(3, entityId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRating(rs));
                }
            }
        } catch (SQLException e) {
            System.err
                    .println("RatingDaoImpl.findByRaterAndEntity: SQLException: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public boolean updateComment(int ratingId, String newComment) throws SQLException {
        return updateComment(ratingId, newComment, null);
    }
    
    @Override
    public boolean updateComment(int ratingId, String newComment, Connection conn) throws SQLException {
        String sql = "UPDATE ratings SET comment = ?, updated_at = CURRENT_TIMESTAMP WHERE rating_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null) {
            System.err.println("RatingDaoImpl.updateComment: No DB Conn");
            return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            if (newComment != null) {
                pstmt.setString(1, newComment);
            } else {
                pstmt.setNull(1, Types.VARCHAR);
            }
            pstmt.setInt(2, ratingId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("RatingDaoImpl.updateComment: SQLException for rating_id=" + ratingId
                    + ": " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
            }
        }
    }

    @Override
    public boolean delete(int ratingId) throws SQLException {
        return delete(ratingId, null);
    }

    @Override
    public boolean delete(int ratingId, Connection conn) throws SQLException {
        String sql = "DELETE FROM ratings WHERE rating_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) {
            System.err.println("RatingDaoImpl.delete: No DB Conn");
            return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, ratingId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("RatingDaoImpl.delete: SQLException for rating_id=" + ratingId + ": "
                    + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
            }
        }
    }
}