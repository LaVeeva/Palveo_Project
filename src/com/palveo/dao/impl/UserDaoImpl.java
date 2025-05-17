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
import com.palveo.dao.UserDao;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.User;

public class UserDaoImpl implements UserDao {

    public UserDaoImpl() {}

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setBio(rs.getString("bio"));
        user.setProfileImagePath(rs.getString("profile_image_path"));
        user.setCity(rs.getString("city"));
        user.setDistrict(rs.getString("district"));
        user.setSecurityQuestion(rs.getString("security_question"));
        user.setSecurityAnswerHash(rs.getString("security_answer_hash"));
        user.setSecurityAnswerSalt(rs.getString("security_answer_salt"));
        user.setEulaAccepted(rs.getBoolean("eula_accepted"));
        user.setAgeVerified(rs.getBoolean("age_verified"));
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            user.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        }
        return user;
    }

    @Override
    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection dbConn = DatabaseConnection.getConnection(); 
        if (dbConn == null || !DatabaseConnection.isConnected()) {
             System.err.println("UserDaoImpl.findById: No database connection.");
             return Optional.empty();
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println( "UserDaoImpl.findById: SQLException for ID [" + id + "]: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) {
             System.err.println("UserDaoImpl.findByUsername: No database connection.");
             return Optional.empty();
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDaoImpl.findByUsername: SQLException for username [" + username + "]: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        Connection dbConn = DatabaseConnection.getConnection();
        if (dbConn == null || !DatabaseConnection.isConnected()) {
             System.err.println("UserDaoImpl.findByEmail: No database connection.");
             return Optional.empty();
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDaoImpl.findByEmail: SQLException for email [" + email + "]: " + e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        Connection dbConn = DatabaseConnection.getConnection();
         if (dbConn == null || !DatabaseConnection.isConnected()) {
             System.err.println("UserDaoImpl.findAll: No database connection.");
             return users;
        }
        try (Statement stmt = dbConn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("UserDaoImpl.findAll: SQLException: " + e.getMessage());
            throw e;
        }
        return users;
    }
    
    @Override
    public int save(User user) throws SQLException {
        return save(user, null);
    }

    @Override
    public int save(User user, Connection conn) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, salt, first_name, last_name, bio, profile_image_path, city, district, eula_accepted, age_verified, created_at, security_question, security_answer_hash, security_answer_salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null || (!manageConnection && !DatabaseConnection.isConnected() )) { 
             System.err.println("UserDaoImpl.save: No database connection.");
             return -1;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getSalt());
            pstmt.setString(5, user.getFirstName());
            pstmt.setString(6, user.getLastName());
            if (user.getBio() != null) pstmt.setString(7, user.getBio()); else pstmt.setNull(7, Types.VARCHAR);
            if (user.getProfileImagePath() != null) pstmt.setString(8, user.getProfileImagePath()); else pstmt.setNull(8, Types.VARCHAR);
            pstmt.setString(9, user.getCity());
            if (user.getDistrict() != null && !user.getDistrict().trim().isEmpty()) pstmt.setString(10, user.getDistrict()); else pstmt.setNull(10, Types.VARCHAR);
            pstmt.setBoolean(11, user.isEulaAccepted());
            pstmt.setBoolean(12, user.isAgeVerified());
            pstmt.setTimestamp(13, user.getCreatedAt() != null ? Timestamp.valueOf(user.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            pstmt.setString(14, user.getSecurityQuestion());
            pstmt.setString(15, user.getSecurityAnswerHash());
            if (user.getSecurityAnswerSalt() != null) pstmt.setString(16, user.getSecurityAnswerSalt()); else pstmt.setNull(16, Types.VARCHAR);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) return -1;
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                    return user.getId();
                } else { return -1; }
            }
        } catch (SQLException e) {
            System.err.println("UserDaoImpl.save: SQLException: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                 
             }
        }
    }
    
    @Override
    public boolean update(User user) throws SQLException {
        return update(user, null);
    }

    @Override
    public boolean update(User user, Connection conn) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ?, password_hash = ?, salt = ?, first_name = ?, last_name = ?, bio = ?, profile_image_path = ?, city = ?, district = ?, eula_accepted = ?, age_verified = ?, security_question = ?, security_answer_hash = ?, security_answer_salt = ? WHERE id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null || (!manageConnection && !DatabaseConnection.isConnected())) {
             System.err.println("UserDaoImpl.update: No database connection.");
             return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getSalt());
            pstmt.setString(5, user.getFirstName());
            pstmt.setString(6, user.getLastName());
            if (user.getBio() != null) pstmt.setString(7, user.getBio()); else pstmt.setNull(7, Types.VARCHAR);
            if (user.getProfileImagePath() != null) pstmt.setString(8, user.getProfileImagePath()); else pstmt.setNull(8, Types.VARCHAR);
            pstmt.setString(9, user.getCity());
            if (user.getDistrict() != null && !user.getDistrict().trim().isEmpty()) pstmt.setString(10, user.getDistrict()); else pstmt.setNull(10, Types.VARCHAR);
            pstmt.setBoolean(11, user.isEulaAccepted());
            pstmt.setBoolean(12, user.isAgeVerified());
            pstmt.setString(13, user.getSecurityQuestion());
            pstmt.setString(14, user.getSecurityAnswerHash());
            if (user.getSecurityAnswerSalt() != null) pstmt.setString(15, user.getSecurityAnswerSalt()); else pstmt.setNull(15, Types.VARCHAR);
            pstmt.setInt(16, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDaoImpl.update: SQLException: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                 
             }
        }
    }
    
    @Override
    public boolean deleteById(int id) throws SQLException {
        return deleteById(id, null);
    }

    @Override
    public boolean deleteById(int id, Connection conn) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);
         if (dbConn == null || (!manageConnection && !DatabaseConnection.isConnected())) {
             System.err.println("UserDaoImpl.deleteById: No database connection.");
             return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDaoImpl.deleteById: SQLException for ID [" + id + "]: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                 
             }
        }
    }

    @Override
    public boolean updatePassword(int userId, String newPasswordHash, String newSalt) throws SQLException {
        return updatePassword(userId, newPasswordHash, newSalt, null);
    }
    
    @Override
    public boolean updatePassword(int userId, String newPasswordHash, String newSalt, Connection conn) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        Connection dbConn = (conn != null) ? conn : DatabaseConnection.getConnection();
        boolean manageConnection = (conn == null);

        if (dbConn == null || (!manageConnection && !DatabaseConnection.isConnected())) {
             System.err.println("UserDaoImpl.updatePassword: No database connection.");
             return false;
        }
        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, newSalt);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDaoImpl.updatePassword: SQLException for user ID [" + userId + "]: " + e.getMessage());
            throw e;
        } finally {
             if (manageConnection && dbConn != null && !DatabaseConnection.isTransactionalConnection(dbConn)) {
                
             }
        }
    }
}