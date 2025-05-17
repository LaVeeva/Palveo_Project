package com.palveo.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.palveo.dao.CommentDao;
import com.palveo.dao.EventDao;
import com.palveo.dao.UserDao;
import com.palveo.dao.impl.CommentDaoImpl;
import com.palveo.dao.impl.EventDaoImpl;
import com.palveo.dao.impl.UserDaoImpl;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.Comment;
import com.palveo.model.Event;
import com.palveo.model.Participant;
import com.palveo.model.User;
import com.palveo.service.CommentService;
import com.palveo.service.ParticipantService;
import com.palveo.service.exception.CommentNotFoundException;
import com.palveo.service.exception.CommentOperationException;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.util.ValidationUtils;

public class CommentServiceImpl implements CommentService {

    private CommentDao commentDao;
    private UserDao userDao;
    private EventDao eventDao;
    private ParticipantService participantService;

    public CommentServiceImpl() {
        this.commentDao = new CommentDaoImpl();
        this.userDao = new UserDaoImpl();
        this.eventDao = new EventDaoImpl();
        this.participantService = new ParticipantServiceImpl();
    }

    public CommentServiceImpl(CommentDao commentDao, UserDao userDao, EventDao eventDao, ParticipantService participantService) {
        this.commentDao = commentDao;
        this.userDao = userDao;
        this.eventDao = eventDao;
        this.participantService = participantService;
    }

    private void validateCommentContent(String content) throws CommentOperationException {
        if (ValidationUtils.isNullOrEmpty(content)) {
            throw new CommentOperationException("Comment content cannot be empty.");
        }
        if (content.length() > 2000) {
            throw new CommentOperationException("Comment content is too long (max 2000 characters).");
        }
    }

    private User getUserOrThrow(int userId, String contextErrorMessage) throws UserNotFoundException, CommentOperationException {
        try {
            return userDao.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId + " (" + contextErrorMessage + ")"));
        } catch (SQLException e) {
            throw new CommentOperationException("Database error fetching user " + userId + " (" + contextErrorMessage + ")", e);
        }
    }
    
    private Event getEventOrThrow(int eventId, String contextErrorMessage) throws EventNotFoundException, CommentOperationException {
        try {
            return eventDao.findById(eventId).orElseThrow(() -> new EventNotFoundException("Event not found with ID: " + eventId + " (" + contextErrorMessage + ")"));
        } catch (SQLException e) {
            throw new CommentOperationException("Database error fetching event " + eventId + " (" + contextErrorMessage + ")", e);
        }
    }

    private Comment getCommentOrThrow(int commentId, String contextErrorMessage) throws CommentNotFoundException, CommentOperationException {
         try {
            return commentDao.findById(commentId).orElseThrow(() -> new CommentNotFoundException("Comment not found with ID: " + commentId + " (" + contextErrorMessage + ")"));
        } catch (SQLException e) {
            throw new CommentOperationException("Database error fetching comment " + commentId + " (" + contextErrorMessage + ")", e);
        }
    }

    @Override
    public Comment postCommentToEvent(int eventId, User author, String content)
            throws CommentOperationException, EventNotFoundException, UserNotFoundException {
        if (author == null) {
            throw new UserNotFoundException("Author user cannot be null when posting a comment.");
        }
        validateCommentContent(content);
        Event event = getEventOrThrow(eventId, "posting comment to event");

        if (event.getPrivacy() == Event.PrivacySetting.PRIVATE) {
            boolean isHost = author.getId() == event.getHostUserId();
            boolean isParticipant = false;
            Optional<Participant> rsvpOpt = participantService.getUserRsvpForEvent(eventId, author.getId());
            if (rsvpOpt.isPresent()) {
                Participant.RsvpStatus status = rsvpOpt.get().getStatus();
                isParticipant = (status == Participant.RsvpStatus.JOINED || status == Participant.RsvpStatus.ATTENDED);
            }
            if (!isHost && !isParticipant) {
                throw new CommentOperationException("Only the host or confirmed participants can comment on private events.");
            }
        }

        Comment comment = new Comment(author.getId(), content);
        comment.setTargetEventId(eventId);
        
        Connection localConn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            localConn = DatabaseConnection.getTransactionalConnection();
            int commentId = commentDao.save(comment, localConn);
            if (commentId > 0) {
                comment.setCommentId(commentId);
                comment.setAuthorUsername(author.getUsername());
                if (transactionOwner) {
                    DatabaseConnection.commitTransaction();
                }
                return comment;
            } else {
                throw new CommentOperationException("Failed to save comment to event (DAO error).");
            }
        } catch (SQLException e) {
            if (transactionOwner && localConn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (postCommentToEvent SQL): " + re.getMessage());}
            }
            throw new CommentOperationException("Database error posting comment to event.", e);
        } catch (CommentOperationException e) {
            if (transactionOwner && localConn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (postCommentToEvent Op): " + re.getMessage());}
            }
            throw e;
        } finally {
            if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && localConn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("CommentServiceImpl: Error ensuring transaction is rolled back in finally (postCommentToEvent): " + se.getMessage());}
            }
        }
    }

    @Override
    public Comment postCommentToUserProfile(int targetProfileUserId, User author, String content)
            throws CommentOperationException, UserNotFoundException {
        return postCommentToUserProfile(targetProfileUserId, author, content, null);
    }
    
    @Override
    public Comment postCommentToUserProfile(int targetProfileUserId, User author, String content, Connection existingConn)
            throws CommentOperationException, UserNotFoundException {
        if (author == null) {
            throw new UserNotFoundException("Author user cannot be null when posting a profile comment.");
        }
        validateCommentContent(content);
        getUserOrThrow(targetProfileUserId, "posting comment to user profile");

        Comment comment = new Comment(author.getId(), content);
        comment.setTargetProfileUserId(targetProfileUserId);
        
        Connection localConn = null;
        boolean transactionOwner = false;
        try {
            if (existingConn != null) {
                localConn = existingConn;
            } else {
                if (DatabaseConnection.getTransactionalConnection() == null) {
                    DatabaseConnection.beginTransaction();
                    transactionOwner = true;
                }
                localConn = DatabaseConnection.getTransactionalConnection();
            }

            int commentId = commentDao.save(comment, localConn);
            if (commentId > 0) {
                comment.setCommentId(commentId);
                comment.setAuthorUsername(author.getUsername());
                if (transactionOwner) {
                    DatabaseConnection.commitTransaction();
                }
                return comment;
            } else {
                throw new CommentOperationException("Failed to save comment to user profile (DAO error).");
            }
        } catch (SQLException e) {
            if (transactionOwner && localConn != null && existingConn == null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (postCommentToUserProfile SQL): " + re.getMessage());}
            }
            throw new CommentOperationException("Database error posting comment to user profile.", e);
        } catch (CommentOperationException e) {
            if (transactionOwner && localConn != null && existingConn == null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (postCommentToUserProfile Op): " + re.getMessage());}
            }
            throw e;
        } finally {
            if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && localConn == DatabaseConnection.getTransactionalConnection() && existingConn == null){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("CommentServiceImpl: Error ensuring transaction is rolled back in finally (postCommentToUserProfile): " + se.getMessage());}
            }
        }
    }
    
    @Override
    public Comment replyToComment(int parentCommentId, User author, String content)
            throws CommentOperationException, CommentNotFoundException, UserNotFoundException {
        if (author == null) {
             throw new UserNotFoundException("Author user cannot be null when replying to a comment.");
        }
        validateCommentContent(content);
        Comment parentComment = getCommentOrThrow(parentCommentId, "replying to comment");

        Comment reply = new Comment(author.getId(), content);
        reply.setParentCommentId(parentCommentId);
        reply.setTargetEventId(parentComment.getTargetEventId()); 
        reply.setTargetProfileUserId(parentComment.getTargetProfileUserId());
        
        Connection localConn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            localConn = DatabaseConnection.getTransactionalConnection();
            int replyId = commentDao.save(reply, localConn);
            if (replyId > 0) {
                reply.setCommentId(replyId);
                reply.setAuthorUsername(author.getUsername());
                if (transactionOwner) {
                    DatabaseConnection.commitTransaction();
                }
                return reply;
            } else {
                throw new CommentOperationException("Failed to save reply (DAO error).");
            }
        } catch (SQLException e) {
            if (transactionOwner && localConn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (replyToComment SQL): " + re.getMessage());}
            }
            throw new CommentOperationException("Database error posting reply.", e);
        } catch (CommentOperationException e) {
            if (transactionOwner && localConn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (replyToComment Op): " + re.getMessage());}
            }
            throw e;
        } finally {
            if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && localConn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("CommentServiceImpl: Error ensuring transaction is rolled back in finally (replyToComment): " + se.getMessage());}
            }
        }
    }

    @Override
    public Optional<Comment> getCommentById(int commentId) throws CommentOperationException {
        try {
            return commentDao.findById(commentId);
        } catch (SQLException e) {
            System.err.println("CommentServiceImpl.getCommentById: SQLException for ID " + commentId + ": " + e.getMessage());
            throw new CommentOperationException("Database error fetching comment by ID: " + commentId, e);
        }
    }

    @Override
    public List<Comment> getCommentsForEvent(int eventId) throws EventNotFoundException, CommentOperationException {
        getEventOrThrow(eventId, "fetching comments for event"); 
        try {
            return commentDao.findByEventId(eventId);
        } catch (SQLException e) {
            throw new CommentOperationException("Database error fetching comments for event " + eventId, e);
        }
    }

    @Override
    public List<Comment> getCommentsForUserProfile(int targetProfileUserId) throws UserNotFoundException, CommentOperationException {
         getUserOrThrow(targetProfileUserId, "fetching comments for user profile");
        try {
            return commentDao.findByTargetProfileUserId(targetProfileUserId);
        } catch (SQLException e) {
            throw new CommentOperationException("Database error fetching comments for user profile " + targetProfileUserId, e);
        }
    }
    
    @Override
    public List<Comment> getRepliesForComment(int parentCommentId) throws CommentNotFoundException, CommentOperationException {
        getCommentOrThrow(parentCommentId, "fetching replies for comment");
        try {
            return commentDao.findRepliesToComment(parentCommentId);
        } catch (SQLException e) {
            throw new CommentOperationException("Database error fetching replies for comment " + parentCommentId, e);
        }
    }

    @Override
    public List<Comment> getCommentsByAuthor(int authorUserId) throws UserNotFoundException, CommentOperationException {
        getUserOrThrow(authorUserId, "fetching comments by author");
        try {
            return commentDao.findByAuthorId(authorUserId);
        } catch (SQLException e) {
            throw new CommentOperationException("Database error fetching comments by author " + authorUserId, e);
        }
    }

    @Override
    public Comment updateComment(int commentId, String newContent, User currentUser)
            throws CommentOperationException, CommentNotFoundException, UserNotFoundException {
        if (currentUser == null) {
            throw new UserNotFoundException("Current user cannot be null when updating a comment.");
        }
        validateCommentContent(newContent);
        Comment commentToUpdate = getCommentOrThrow(commentId, "updating comment");

        if (commentToUpdate.getAuthorUserId() != currentUser.getId()) {
            throw new CommentOperationException("Unauthorized: You can only update your own comments.");
        }

        commentToUpdate.setContent(newContent);
        commentToUpdate.setEdited(true);
        commentToUpdate.setUpdatedAt(LocalDateTime.now());

        Connection localConn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            localConn = DatabaseConnection.getTransactionalConnection();
            boolean updated = commentDao.updateContent(commentToUpdate.getCommentId(), commentToUpdate.getContent(), localConn);
            if (updated) {
                commentToUpdate.setAuthorUsername(currentUser.getUsername());
                if (transactionOwner) {
                    DatabaseConnection.commitTransaction();
                }
                return commentToUpdate;
            } else {
                throw new CommentOperationException("Failed to update comment in database.");
            }
        } catch (SQLException e) {
            if (transactionOwner && localConn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (updateComment SQL): " + re.getMessage());}
            }
            throw new CommentOperationException("Database error updating comment.", e);
        } catch (CommentOperationException e) {
            if (transactionOwner && localConn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (updateComment Op): " + re.getMessage());}
            }
            throw e;
        } finally {
            if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && localConn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("CommentServiceImpl: Error ensuring transaction is rolled back in finally (updateComment): " + se.getMessage());}
            }
        }
    }

    @Override
    public void deleteComment(int commentId, User currentUser)
            throws CommentOperationException, CommentNotFoundException, UserNotFoundException, EventNotFoundException {
        deleteComment(commentId, currentUser, null);
    }
    
    @Override
    public void deleteComment(int commentId, User currentUser, Connection existingConn)
            throws CommentOperationException, CommentNotFoundException, UserNotFoundException, EventNotFoundException {
        if (currentUser == null) {
            throw new UserNotFoundException("Current user cannot be null when deleting a comment.");
        }
        Comment commentToDelete = getCommentOrThrow(commentId, "deleting comment");

        boolean canDelete = false;
        if (commentToDelete.getAuthorUserId() == currentUser.getId()) {
            canDelete = true;
        } else {
            Integer eventId = commentToDelete.getTargetEventId();
            Integer profileUserId = commentToDelete.getTargetProfileUserId();

            if (eventId != null) {
                Event event = getEventOrThrow(eventId, "verifying event host for comment deletion");
                if (event.getHostUserId() == currentUser.getId()) {
                    canDelete = true;
                }
            } else if (profileUserId != null) {
                if (profileUserId.equals(currentUser.getId())) {
                    canDelete = true;
                }
            }
        }

        if (!canDelete) {
            throw new CommentOperationException("Unauthorized: You do not have permission to delete this comment.");
        }
        
        Connection localConn = null;
        boolean transactionOwner = false;
        try {
            if (existingConn != null) {
                localConn = existingConn;
            } else {
                if (DatabaseConnection.getTransactionalConnection() == null) {
                    DatabaseConnection.beginTransaction();
                    transactionOwner = true;
                }
                localConn = DatabaseConnection.getTransactionalConnection();
            }
            
            boolean deleted = commentDao.delete(commentId, localConn);
            if (!deleted) {
                throw new CommentOperationException("Failed to delete comment from database.");
            }
            if (transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
        } catch (SQLException e) {
            if (transactionOwner && localConn != null && existingConn == null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (deleteComment SQL): " + re.getMessage());}
            }
            throw new CommentOperationException("Database error deleting comment.", e);
        } catch (CommentOperationException e) {
            if (transactionOwner && localConn != null && existingConn == null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("CommentService: Error during rollback (deleteComment Op): " + re.getMessage());}
            }
            throw e;
        } finally {
            if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && localConn == DatabaseConnection.getTransactionalConnection() && existingConn == null){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("CommentServiceImpl: Error ensuring transaction is rolled back in finally (deleteComment): " + se.getMessage());}
            }
        }
    }
}