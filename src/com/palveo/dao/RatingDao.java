package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Rating;

public interface RatingDao {
    int save(Rating rating) throws SQLException;
    int save(Rating rating, Connection conn) throws SQLException;

    Optional<Rating> findById(int ratingId) throws SQLException;

    List<Rating> findByRatedEntity(String entityType, int entityId) throws SQLException;

    List<Rating> findByRaterUserId(int raterUserId) throws SQLException;

    Optional<Rating> findByRaterAndEntity(int raterUserId, String entityType, int entityId)
            throws SQLException;

    boolean updateComment(int ratingId, String newComment) throws SQLException;
    boolean updateComment(int ratingId, String newComment, Connection conn) throws SQLException;

    boolean delete(int ratingId) throws SQLException;
    boolean delete(int ratingId, Connection conn) throws SQLException;
}