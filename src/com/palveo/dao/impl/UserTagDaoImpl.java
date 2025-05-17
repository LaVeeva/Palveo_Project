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
import com.palveo.dao.UserTagDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.db.exception.DuplicateKeyException;
import com.palveo.model.Tag;
import com.palveo.model.User;
import com.palveo.model.UserTag;

public class UserTagDaoImpl implements UserTagDao {

    private Tag mapResultSetToBasicTag(ResultSet rs) throws SQLException {
        return new Tag(rs.getInt("t_tag_id"), rs.getString("t_tag_name"));
    }

    private User mapResultSetToBasicUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("u_id")); 
        user.setUsername(rs.getString("u_username"));
        return user;
    }
    
    private UserTag mapResultSetToUserTag(ResultSet rs) throws SQLException {
        UserTag userTag = new UserTag();
        userTag.setUserTagId(rs.getInt("ut_user_tag_id"));
        userTag.setUserId(rs.getInt("ut_user_id"));
        userTag.setTagId(rs.getInt("ut_tag_id"));
        userTag.setTaggedByUserId(rs.getObject("ut_tagged_by_user_id", Integer.class));
        Timestamp taggedAtTimestamp = rs.getTimestamp("ut_tagged_at");
        if(taggedAtTimestamp != null) {
            userTag.setTaggedAt(taggedAtTimestamp.toLocalDateTime());
        }
        try { userTag.setTagName(rs.getString("t_tag_name")); }
        catch (SQLException e) {}
        try { userTag.setTaggedByUsername(rs.getString("tagger_username")); }
        catch (SQLException e) {}
        return userTag;
    }

    private void setNullableInt(PreparedStatement pstmt, int parameterIndex, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(parameterIndex, value);
        } else {
            pstmt.setNull(parameterIndex, Types.INTEGER);
        }
    }
    
    @Override
    public int addTagToUser(UserTag userTag) throws SQLException {
        return addTagToUser(userTag, null);
    }

    @Override
    public int addTagToUser(UserTag userTag, Connection conn) throws SQLException {
        String sql = "INSERT INTO user_tags (user_id, tag_id, tagged_by_user_id, tagged_at) VALUES (?, ?, ?, ?)";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null) { 
            System.err.println("UserTagDaoImpl.addTagToUser: No DB Connection"); 
            return -1; 
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userTag.getUserId());
            pstmt.setInt(2, userTag.getTagId());
            setNullableInt(pstmt, 3, userTag.getTaggedByUserId());
            pstmt.setTimestamp(4, userTag.getTaggedAt() != null ? Timestamp.valueOf(userTag.getTaggedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) return -1;

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userTag.setUserTagId(generatedKeys.getInt(1)); 
                    return userTag.getUserTagId();
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
             if (e.getErrorCode() == 1062 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("uq_user_tag_instance"))) {
                throw new DuplicateKeyException("Duplicate user tag instance.", e.getSQLState(), e.getErrorCode(), e);
            }
            System.err.println("UserTagDaoImpl.addTagToUser: SQLException: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
               
            }
        }
    }
    
    @Override
    public boolean removeTagFromUser(int userId, int tagId, Integer taggedByUserId) throws SQLException {
        return removeTagFromUser(userId, tagId, taggedByUserId, null);
    }

    @Override
    public boolean removeTagFromUser(int userId, int tagId, Integer taggedByUserId, Connection conn) throws SQLException {
        String sql;
        if (taggedByUserId != null) {
            sql = "DELETE FROM user_tags WHERE user_id = ? AND tag_id = ? AND tagged_by_user_id = ?";
        } else {
            sql = "DELETE FROM user_tags WHERE user_id = ? AND tag_id = ? AND tagged_by_user_id IS NULL";
        }
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) { 
            System.err.println("UserTagDaoImpl.removeTagFromUser: No DB Connection"); 
            return false; 
        }
        
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, tagId);
            if (taggedByUserId != null) {
                pstmt.setInt(3, taggedByUserId);
            }
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.removeTagFromUser: SQLException: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
            }
        }
    }
    
    @Override
    public boolean removeSpecificUserTagLink(int userTagId) throws SQLException {
        return removeSpecificUserTagLink(userTagId, null);
    }

    @Override
    public boolean removeSpecificUserTagLink(int userTagId, Connection conn) throws SQLException {
        String sql = "DELETE FROM user_tags WHERE user_tag_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null) {
             System.err.println("UserTagDaoImpl.removeSpecificUserTagLink: No DB Connection"); 
             return false;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userTagId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.removeSpecificUserTagLink: SQLException: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
               
            }
        }
    }
    
    @Override
    public boolean removeAllTagsFromUser(int userId) throws SQLException {
        return removeAllTagsFromUser(userId, null);
    }

    @Override
    public boolean removeAllTagsFromUser(int userId, Connection conn) throws SQLException {
        String sql = "DELETE FROM user_tags WHERE user_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null) { 
            System.err.println("UserTagDaoImpl.removeAllTagsFromUser: No DB Conn");
             return false;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate(); 
            return true;
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.removeAllTagsFromUser: SQLException: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
            }
        }
    }
    
    @Override
    public boolean removeAllTagsAppliedByUser(int taggedByUserId) throws SQLException {
        return removeAllTagsAppliedByUser(taggedByUserId, null);
    }

    @Override
    public boolean removeAllTagsAppliedByUser(int taggedByUserId, Connection conn) throws SQLException {
        String sql = "DELETE FROM user_tags WHERE tagged_by_user_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
        
        if (dbConn == null) {
             System.err.println("UserTagDaoImpl.removeAllTagsAppliedByUser: No DB Connection");
            return false;
        }

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, taggedByUserId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.removeAllTagsAppliedByUser: SQLException: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
               
            }
        }
    }
    
    @Override
    public Optional<UserTag> findSpecificUserTagInstance(int userId, int tagId, Integer taggedByUserId) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT ut.user_tag_id as ut_user_tag_id, ut.user_id as ut_user_id, ut.tag_id as ut_tag_id, " +
            "ut.tagged_by_user_id as ut_tagged_by_user_id, ut.tagged_at as ut_tagged_at, t.tag_name as t_tag_name " +
            "FROM user_tags ut JOIN tags t ON ut.tag_id = t.tag_id WHERE ut.user_id = ? AND ut.tag_id = ? "
        );
        if (taggedByUserId != null) {
            sqlBuilder.append("AND ut.tagged_by_user_id = ?");
        } else {
            sqlBuilder.append("AND ut.tagged_by_user_id IS NULL");
        }
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("UserTagDaoImpl.findSpecificUserTagInstance: No DB Conn"); 
            return Optional.empty(); 
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sqlBuilder.toString())) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, tagId);
            if (taggedByUserId != null) {
                pstmt.setInt(3, taggedByUserId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUserTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.findSpecificUserTagInstance: SQLException: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<UserTag> findUserTagDetailsByUserId(int userId) throws SQLException {
        List<UserTag> userTags = new ArrayList<>();
        String sql = "SELECT ut.user_tag_id as ut_user_tag_id, ut.user_id as ut_user_id, ut.tag_id as ut_tag_id, " +
                     "ut.tagged_by_user_id as ut_tagged_by_user_id, ut.tagged_at as ut_tagged_at, " +
                     "t.tag_name as t_tag_name, u_tagger.username as tagger_username " +
                     "FROM user_tags ut " +
                     "JOIN tags t ON ut.tag_id = t.tag_id " +
                     "LEFT JOIN users u_tagger ON ut.tagged_by_user_id = u_tagger.id " +
                     "WHERE ut.user_id = ? ORDER BY t.tag_name ASC";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) { 
             System.err.println("UserTagDaoImpl.findUserTagDetailsByUserId: No DB Conn");
            return userTags;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    userTags.add(mapResultSetToUserTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.findUserTagDetailsByUserId: SQLException: " + e.getMessage());
            throw e;
        }
        return userTags;
    }

    @Override
    public List<Tag> findDistinctTagsByUserId(int userId) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT DISTINCT t.tag_id as t_tag_id, t.tag_name as t_tag_name FROM tags t " +
                     "JOIN user_tags ut ON t.tag_id = ut.tag_id " +
                     "WHERE ut.user_id = ? ORDER BY t.tag_name ASC";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) {
             System.err.println("UserTagDaoImpl.findDistinctTagsByUserId: No DB Conn");
             return tags;
         }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapResultSetToBasicTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.findDistinctTagsByUserId: SQLException: " + e.getMessage());
            throw e;
        }
        return tags;
    }

    @Override
    public List<UserTag> findUserTagDetailsByTagId(int tagId) throws SQLException {
        List<UserTag> userTags = new ArrayList<>();
         String sql = "SELECT ut.user_tag_id as ut_user_tag_id, ut.user_id as ut_user_id, ut.tag_id as ut_tag_id, " + 
                      "ut.tagged_by_user_id as ut_tagged_by_user_id, ut.tagged_at as ut_tagged_at, " +
                      "t.tag_name as t_tag_name, u_tagger.username as tagger_username " +
                      "FROM user_tags ut " +
                      "JOIN tags t ON ut.tag_id = t.tag_id " +
                      "LEFT JOIN users u_tagger ON ut.tagged_by_user_id = u_tagger.id " +
                      "WHERE ut.tag_id = ? ORDER BY ut.user_id ASC"; 
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) { 
             System.err.println("UserTagDaoImpl.findUserTagDetailsByTagId: No DB Conn");
            return userTags;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, tagId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    userTags.add(mapResultSetToUserTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.findUserTagDetailsByTagId: SQLException: " + e.getMessage());
            throw e;
        }
        return userTags;
    }
    
    @Override
    public List<User> findUsersByTagId(int tagId) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT DISTINCT u.id as u_id, u.username as u_username, u.first_name as u_first_name, u.last_name as u_last_name " + 
                     "FROM users u JOIN user_tags ut ON u.id = ut.user_id " +
                     "WHERE ut.tag_id = ? ORDER BY u.username ASC";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) { 
            System.err.println("UserTagDaoImpl.findUsersByTagId: No DB Conn");
             return users;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, tagId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToBasicUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.findUsersByTagId: SQLException: " + e.getMessage());
            throw e;
        }
        return users;
    }

    @Override
    public List<UserTag> findTagsAppliedByUser(int taggedByUserId) throws SQLException {
        List<UserTag> userTags = new ArrayList<>();
        String sql = "SELECT ut.user_tag_id as ut_user_tag_id, ut.user_id as ut_user_id, ut.tag_id as ut_tag_id, " +
                     "ut.tagged_by_user_id as ut_tagged_by_user_id, ut.tagged_at as ut_tagged_at, " +
                     "t.tag_name as t_tag_name " +
                     "FROM user_tags ut " +
                     "JOIN tags t ON ut.tag_id = t.tag_id " +
                     "WHERE ut.tagged_by_user_id = ? ORDER BY ut.tagged_at DESC";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
            System.err.println("UserTagDaoImpl.findTagsAppliedByUser: No DB Conn"); 
             return userTags;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, taggedByUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    userTags.add(mapResultSetToUserTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.findTagsAppliedByUser: SQLException: " + e.getMessage());
            throw e;
        }
        return userTags;
    }

    @Override
    public boolean isUserTaggedWith(int userId, int tagId) throws SQLException {
        String sql = "SELECT 1 FROM user_tags WHERE user_id = ? AND tag_id = ? LIMIT 1";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) { 
             System.err.println("UserTagDaoImpl.isUserTaggedWith: No DB Conn");
             return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, tagId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("UserTagDaoImpl.isUserTaggedWith: SQLException: " + e.getMessage());
            throw e;
        }
    }
}