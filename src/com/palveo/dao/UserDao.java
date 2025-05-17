package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.palveo.model.User;

public interface UserDao {

    Optional<User> findById(int id) throws SQLException;

    Optional<User> findByUsername(String username) throws SQLException;

    Optional<User> findByEmail(String email) throws SQLException;

    List<User> findAll() throws SQLException;

    int save(User user) throws SQLException;
    int save(User user, Connection conn) throws SQLException;

    boolean update(User user) throws SQLException;
    boolean update(User user, Connection conn) throws SQLException;

    boolean deleteById(int id) throws SQLException;
    boolean deleteById(int id, Connection conn) throws SQLException;

    boolean updatePassword(int userId, String newPasswordHash, String newSalt) throws SQLException;
    boolean updatePassword(int userId, String newPasswordHash, String newSalt, Connection conn) throws SQLException;

}