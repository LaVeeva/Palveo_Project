package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Comment;

public interface CommentDao {
    int save(Comment comment, Connection conn) throws SQLException;

    boolean updateContent(int commentId, String newContent, Connection conn) throws SQLException;

    boolean delete(int commentId, Connection conn) throws SQLException;
    
    Optional<Comment> findById(int commentId) throws SQLException;

    List<Comment> findByEventId(int eventId) throws SQLException;

    List<Comment> findByTargetProfileUserId(int userId) throws SQLException;

    List<Comment> findRepliesToComment(int parentCommentId) throws SQLException;
    
    List<Comment> findByAuthorId(int authorUserId) throws SQLException;

    int save(Comment comment) throws SQLException;
    boolean updateContent(int commentId, String newContent) throws SQLException;
    boolean delete(int commentId) throws SQLException;
}