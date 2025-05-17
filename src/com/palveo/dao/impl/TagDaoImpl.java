package com.palveo.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.palveo.dao.TagDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.Tag;

public class TagDaoImpl implements TagDao {

    private Tag mapResultSetToTag(ResultSet rs) throws SQLException {
        Tag tag = new Tag();
        tag.setTagId(rs.getInt("tag_id"));
        tag.setTagName(rs.getString("tag_name"));
        return tag;
    }
    
    @Override
    public Optional<Tag> findOrCreateTag(String tagName) throws SQLException {
        Connection conn = null;
        boolean transactionStartedByThisMethod = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionStartedByThisMethod = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();
            Optional<Tag> result = findOrCreateTag(tagName, conn);
            if (transactionStartedByThisMethod) {
                DatabaseConnection.commitTransaction();
            }
            return result;
        } catch (SQLException | RuntimeException e) {
            if (transactionStartedByThisMethod && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("TagDaoImpl.findOrCreateTag (convenience): Error during rollback: " + re.getMessage()); }
            }
            if (e instanceof SQLException) throw (SQLException)e;
            else throw new SQLException("Runtime error in findOrCreateTag convenience method", e);
        } finally {
            if (transactionStartedByThisMethod && DatabaseConnection.getTransactionalConnection() != null) {
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("TagDaoImpl.findOrCreateTag (convenience): Error ensuring transaction is rolled back in finally: " + se.getMessage());}
            }
        }
    }
    
    @Override
    public Optional<Tag> findOrCreateTag(String tagName, Connection conn) throws SQLException {
        if (conn == null) {
            throw new SQLException("Connection cannot be null for findOrCreateTag with explicit connection.");
        }
        if (tagName == null || tagName.trim().isEmpty()) {
            return Optional.empty();
        }
        String trimmedTagName = tagName.trim().toLowerCase();

        Optional<Tag> existingTag = findByNameInternal(trimmedTagName, conn); 
        if (existingTag.isPresent()) {
            return existingTag;
        }

        String sqlInsert = "INSERT INTO tags (tag_name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, trimmedTagName);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return Optional.of(new Tag(generatedKeys.getInt(1), trimmedTagName));
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate entry"))) {
                return findByNameInternal(trimmedTagName, conn); 
            }
            System.err.println("TagDaoImpl.findOrCreateTag: SQLException for tag [" + trimmedTagName + "]: " + e.getMessage());
            throw e;
        }
        System.err.println("TagDaoImpl.findOrCreateTag: Failed to create or find tag [" + trimmedTagName + "] after insert attempt.");
        return Optional.empty();
    }

    private Optional<Tag> findByNameInternal(String tagName, Connection conn) throws SQLException {
        String sql = "SELECT * FROM tags WHERE tag_name = ?";
        String searchTagName = tagName.trim().toLowerCase();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, searchTagName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTag(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Tag> findById(int tagId) throws SQLException {
        String sql = "SELECT * FROM tags WHERE tag_id = ?";
        try (Connection dbConn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, tagId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("TagDaoImpl.findById: SQLException for ID [" + tagId + "]: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public Optional<Tag> findByName(String tagName) throws SQLException {
        try (Connection dbConn = DatabaseConnection.getConnection()) {
            return findByNameInternal(tagName, dbConn);
        }
    }

    @Override
    public List<Tag> findAll() throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags ORDER BY tag_name ASC";
        try (Connection dbConn = DatabaseConnection.getConnection();
             Statement stmt = dbConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tags.add(mapResultSetToTag(rs));
            }
        } catch (SQLException e) {
            System.err.println("TagDaoImpl.findAll: SQLException: " + e.getMessage());
            throw e;
        }
        return tags;
    }
    
    @Override
    public List<Tag> searchTagsByName(String query) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags WHERE tag_name LIKE ? ORDER BY tag_name ASC";
        try(Connection dbConn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + query.toLowerCase() + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapResultSetToTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("TagDaoImpl.searchTagsByName: SQLException: " + e.getMessage());
            throw e;
        }
        return tags;
    }
    
    @Override
    public boolean delete(int tagId) throws SQLException {
        return delete(tagId, null);
    }

    @Override
    public boolean delete(int tagId, Connection conn) throws SQLException {
        String sql = "DELETE FROM tags WHERE tag_id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null) {
             System.err.println("TagDaoImpl.delete: No DB Conn");
             return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, tagId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("TagDaoImpl.delete: SQLException for ID [" + tagId + "]: " + e.getMessage());
            throw e;
        } finally {
            if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {}
        }
    }
}