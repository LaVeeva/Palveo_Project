package com.palveo.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.palveo.dao.CommentDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.Comment;

public class CommentDaoImpl implements CommentDao {

    private Comment mapResultSetToComment(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setCommentId(rs.getInt("c_comment_id"));
        comment.setAuthorUserId(rs.getInt("c_author_user_id"));
        comment.setTargetEventId(rs.getObject("c_target_event_id", Integer.class));
        comment.setTargetProfileUserId(rs.getObject("c_target_profile_user_id", Integer.class));
        comment.setParentCommentId(rs.getObject("c_parent_comment_id", Integer.class));
        comment.setContent(rs.getString("c_content"));
        comment.setEdited(rs.getBoolean("c_is_edited"));
        Timestamp createdAt = rs.getTimestamp("c_created_at");
        if (createdAt != null) comment.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("c_updated_at");
        if (updatedAt != null) comment.setUpdatedAt(updatedAt.toLocalDateTime());
        
        try {
            comment.setAuthorUsername(rs.getString("u_username"));
        } catch (SQLException e) {
        }
        return comment;
    }

    private void setNullableInt(PreparedStatement pstmt, int parameterIndex, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(parameterIndex, value);
        } else {
            pstmt.setNull(parameterIndex, Types.INTEGER);
        }
    }
    
    private String baseSelectWithAuthorSQL() {
        return "SELECT c.comment_id as c_comment_id, c.author_user_id as c_author_user_id, " +
               "c.target_event_id as c_target_event_id, c.target_profile_user_id as c_target_profile_user_id, " +
               "c.parent_comment_id as c_parent_comment_id, c.content as c_content, c.is_edited as c_is_edited, " +
               "c.created_at as c_created_at, c.updated_at as c_updated_at, " +
               "u.username as u_username " +
               "FROM comments c JOIN users u ON c.author_user_id = u.id ";
    }

    private List<Comment> findCommentsWithQuery(String fullSql, Object... params) throws SQLException {
        List<Comment> comments = new ArrayList<>();
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) { 
            System.err.println("CommentDaoImpl.findCommentsWithQuery: No DB Connection"); 
            return comments;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(fullSql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapResultSetToComment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("CommentDaoImpl.findCommentsWithQuery: SQLException with SQL ["+fullSql+"]: " + e.getMessage());
            throw e;
        }
        return comments;
    }
    
    @Override
    public int save(Comment comment) throws SQLException {
        return save(comment, null);
    }

    @Override
    public int save(Comment comment, Connection conn) throws SQLException {
        String sql = "INSERT INTO comments (author_user_id, target_event_id, target_profile_user_id, parent_comment_id, content, is_edited, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        
        if (dbConn == null ) { 
            System.err.println("CommentDaoImpl.save: No DB Connection");
            return -1; 
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, comment.getAuthorUserId());
            setNullableInt(pstmt, 2, comment.getTargetEventId());
            setNullableInt(pstmt, 3, comment.getTargetProfileUserId());
            setNullableInt(pstmt, 4, comment.getParentCommentId());
            pstmt.setString(5, comment.getContent());
            pstmt.setBoolean(6, comment.isEdited());
            LocalDateTime now = LocalDateTime.now();
            pstmt.setTimestamp(7, comment.getCreatedAt() != null ? Timestamp.valueOf(comment.getCreatedAt()) : Timestamp.valueOf(now));
            pstmt.setTimestamp(8, comment.getUpdatedAt() != null ? Timestamp.valueOf(comment.getUpdatedAt()) : Timestamp.valueOf(now));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) return -1;

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    comment.setCommentId(generatedKeys.getInt(1));
                    return comment.getCommentId();
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            System.err.println("CommentDaoImpl.save: SQLException: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public boolean updateContent(int commentId, String newContent) throws SQLException {
        return updateContent(commentId, newContent, null);
    }

    @Override
    public boolean updateContent(int commentId, String newContent, Connection conn) throws SQLException {
        String sql = "UPDATE comments SET content = ?, is_edited = TRUE, updated_at = CURRENT_TIMESTAMP WHERE comment_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();

        if (dbConn == null ) {
             System.err.println("CommentDaoImpl.updateContent: No DB Conn"); 
             return false;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, newContent);
            pstmt.setInt(2, commentId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("CommentDaoImpl.updateContent: SQLException: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public boolean delete(int commentId) throws SQLException {
        return delete(commentId, null);
    }

    @Override
    public boolean delete(int commentId, Connection conn) throws SQLException {
        String sql = "DELETE FROM comments WHERE comment_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();

        if (dbConn == null) { 
            System.err.println("CommentDaoImpl.delete: No DB Conn"); 
            return false; 
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, commentId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("CommentDaoImpl.delete: SQLException: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<Comment> findById(int commentId) throws SQLException {
        String sql = baseSelectWithAuthorSQL() + "WHERE c.comment_id = ?";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) { 
             System.err.println("CommentDaoImpl.findById: No DB Conn"); 
             return Optional.empty(); 
         }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, commentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToComment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("CommentDaoImpl.findById: SQLException: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<Comment> findByEventId(int eventId) throws SQLException {
        String sql = baseSelectWithAuthorSQL() + "WHERE c.target_event_id = ? AND c.parent_comment_id IS NULL ORDER BY c.created_at ASC";
        return findCommentsWithQuery(sql, eventId);
    }

    @Override
    public List<Comment> findByTargetProfileUserId(int userId) throws SQLException {
        String sql = baseSelectWithAuthorSQL() + "WHERE c.target_profile_user_id = ? AND c.parent_comment_id IS NULL ORDER BY c.created_at ASC";
        return findCommentsWithQuery(sql, userId);
    }

    @Override
    public List<Comment> findRepliesToComment(int parentCommentId) throws SQLException {
        String sql = baseSelectWithAuthorSQL() + "WHERE c.parent_comment_id = ? ORDER BY c.created_at ASC";
        return findCommentsWithQuery(sql, parentCommentId);
    }

    @Override
    public List<Comment> findByAuthorId(int authorUserId) throws SQLException {
        String sql = baseSelectWithAuthorSQL() + "WHERE c.author_user_id = ? ORDER BY c.created_at DESC";
        return findCommentsWithQuery(sql, authorUserId);
    }
}