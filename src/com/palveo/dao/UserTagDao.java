package com.palveo.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Tag;
import com.palveo.model.User;
import com.palveo.model.UserTag;

public interface UserTagDao {

    int addTagToUser(UserTag userTag) throws SQLException;
    int addTagToUser(UserTag userTag, Connection conn) throws SQLException;

    boolean removeTagFromUser(int userId, int tagId, Integer taggedByUserId) throws SQLException;
    boolean removeTagFromUser(int userId, int tagId, Integer taggedByUserId, Connection conn) throws SQLException;
    
    boolean removeSpecificUserTagLink(int userTagId) throws SQLException;
    boolean removeSpecificUserTagLink(int userTagId, Connection conn) throws SQLException;
    
    boolean removeAllTagsFromUser(int userId) throws SQLException;
    boolean removeAllTagsFromUser(int userId, Connection conn) throws SQLException;

    boolean removeAllTagsAppliedByUser(int taggedByUserId) throws SQLException;
    boolean removeAllTagsAppliedByUser(int taggedByUserId, Connection conn) throws SQLException;


    Optional<UserTag> findSpecificUserTagInstance(int userId, int tagId, Integer taggedByUserId) throws SQLException;
    List<UserTag> findUserTagDetailsByUserId(int userId) throws SQLException;
    List<Tag> findDistinctTagsByUserId(int userId) throws SQLException;
    List<UserTag> findUserTagDetailsByTagId(int tagId) throws SQLException;
    List<User> findUsersByTagId(int tagId) throws SQLException;
    List<UserTag> findTagsAppliedByUser(int taggedByUserId) throws SQLException;

    boolean isUserTaggedWith(int userId, int tagId) throws SQLException; 
}