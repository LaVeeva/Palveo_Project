package com.palveo.service;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import com.palveo.model.Comment;
import com.palveo.model.User;
import com.palveo.service.exception.CommentNotFoundException;
import com.palveo.service.exception.CommentOperationException;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.UserNotFoundException;

public interface CommentService {
    Comment postCommentToEvent(int eventId, User author, String content)
            throws CommentOperationException, EventNotFoundException, UserNotFoundException;

    Comment postCommentToUserProfile(int targetProfileUserId, User author, String content)
            throws CommentOperationException, UserNotFoundException;
            
    Comment postCommentToUserProfile(int targetProfileUserId, User author, String content, Connection conn)
            throws CommentOperationException, UserNotFoundException;


    Comment replyToComment(int parentCommentId, User author, String content)
            throws CommentOperationException, CommentNotFoundException, UserNotFoundException;

    Optional<Comment> getCommentById(int commentId) throws CommentOperationException;

    List<Comment> getCommentsForEvent(int eventId)
            throws EventNotFoundException, CommentOperationException;

    List<Comment> getCommentsForUserProfile(int targetProfileUserId)
            throws UserNotFoundException, CommentOperationException;

    List<Comment> getRepliesForComment(int parentCommentId)
            throws CommentNotFoundException, CommentOperationException;

    List<Comment> getCommentsByAuthor(int authorUserId)
            throws UserNotFoundException, CommentOperationException;

    Comment updateComment(int commentId, String newContent, User currentUser)
            throws CommentOperationException, CommentNotFoundException, UserNotFoundException;

    void deleteComment(int commentId, User currentUser)
            throws CommentOperationException, CommentNotFoundException, UserNotFoundException, EventNotFoundException;
            
    void deleteComment(int commentId, User currentUser, Connection conn)
            throws CommentOperationException, CommentNotFoundException, UserNotFoundException, EventNotFoundException;
}
