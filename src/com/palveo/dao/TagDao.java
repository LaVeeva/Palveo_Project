package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Tag;

public interface TagDao {
    Optional<Tag> findOrCreateTag(String tagName, Connection conn) throws SQLException;
    Optional<Tag> findOrCreateTag(String tagName) throws SQLException;
    
    Optional<Tag> findById(int tagId) throws SQLException;
    Optional<Tag> findByName(String tagName) throws SQLException;
    List<Tag> findAll() throws SQLException;
    List<Tag> searchTagsByName(String query) throws SQLException;
    
    boolean delete(int tagId, Connection conn) throws SQLException;
    boolean delete(int tagId) throws SQLException;
}