package com.palveo.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.palveo.dao.EventDao;
import com.palveo.dao.RatingDao;
import com.palveo.dao.UserDao;
import com.palveo.dao.impl.EventDaoImpl;
import com.palveo.dao.impl.RatingDaoImpl;
import com.palveo.dao.impl.UserDaoImpl;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.Event;
import com.palveo.model.Rating;
import com.palveo.model.User;
import com.palveo.service.CommentService;
import com.palveo.service.RatingService;
import com.palveo.service.exception.CommentOperationException;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.RatingOperationException;
import com.palveo.service.exception.UserNotFoundException;

public class RatingServiceImpl implements RatingService {

    private RatingDao ratingDao;
    private UserDao userDao;
    private EventDao eventDao;
    private CommentService commentService;

    public RatingServiceImpl() {
        this.ratingDao = new RatingDaoImpl();
        this.userDao = new UserDaoImpl();
        this.eventDao = new EventDaoImpl();
        this.commentService = new CommentServiceImpl();
    }

    public RatingServiceImpl(RatingDao ratingDao, UserDao userDao, EventDao eventDao,
            CommentService commentService) {
        this.ratingDao = ratingDao;
        this.userDao = userDao;
        this.eventDao = eventDao;
        this.commentService = commentService;
    }

    private void validateRatingInput(User rater, int score, String comment)
            throws RatingOperationException {
        if (rater == null) {
            throw new RatingOperationException("Rater user cannot be null.");
        }
        if (score < 1 || score > 5) {
            throw new RatingOperationException("Score must be between 1 and 5.");
        }
        if (comment != null && comment.length() > 1000) {
            throw new RatingOperationException("Rating comment is too long (max 1000 characters).");
        }
    }

    private Rating getRatingOrThrow(int ratingId, String context) throws RatingOperationException {
        try {
            return ratingDao.findById(ratingId).orElseThrow(() -> new RatingOperationException(
                    "Rating not found with ID: " + ratingId + " (" + context + ")"));
        } catch (SQLException e) {
            throw new RatingOperationException(
                    "Database error fetching rating " + ratingId + " (" + context + ")", e);
        }
    }

    @Override
    public Rating submitRatingForEvent(int eventId, User rater, int score, String comment)
            throws RatingOperationException, EventNotFoundException, UserNotFoundException {
        validateRatingInput(rater, score, comment);

        Event eventToRate;
        try {
            eventToRate = eventDao.findById(eventId).orElseThrow(() -> new EventNotFoundException(
                    "Event not found with ID: " + eventId + " to rate."));
        } catch (SQLException e) {
            throw new RatingOperationException("Database error checking event " + eventId, e);
        }

        if (eventToRate.getHostUserId() == rater.getId()) {
            throw new RatingOperationException("Users cannot rate their own events.");
        }
        
        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            Optional<Rating> existingRating = ratingDao.findByRaterAndEntity(rater.getId(),
                    Rating.RatedEntityType.EVENT.toString(), eventId);
            if (existingRating.isPresent()) {
                throw new RatingOperationException("You have already rated this event.");
            }

            Rating rating =
                    new Rating(rater.getId(), Rating.RatedEntityType.EVENT, eventId, score, comment);
            int ratingId = ratingDao.save(rating, conn);

            if (ratingId <= 0) {
                throw new RatingOperationException(
                        "Failed to save rating for event (DAO error). DAO returned: " + ratingId);
            }

            rating.setRatingId(ratingId);
            rating.setRaterUsername(rater.getUsername());
            if (transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
            return rating;
        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("RatingService: Error during rollback (submitRatingForEvent SQL): " + re.getMessage());}
            }
            if (e.getErrorCode() == 1062 || (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("uq_rating_instance"))) {
                throw new RatingOperationException(
                        "You have already rated this event (caught as SQLException).", e);
            }
            throw new RatingOperationException("Database error submitting rating for event.", e);
        } catch (RatingOperationException e){
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("RatingService: Error during rollback (submitRatingForEvent Op): " + re.getMessage());}
            }
            throw e;
        } finally {
             if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("RatingServiceImpl: Error ensuring transaction is rolled back in finally (submitRatingForEvent): " + se.getMessage());}
            }
        }
    }

    @Override
    public Rating submitRatingForUser(int ratedUserId, User rater, int score, String comment)
            throws RatingOperationException, UserNotFoundException {
        validateRatingInput(rater, score, comment);

        User userToRate;
        try {
            userToRate = userDao.findById(ratedUserId).orElseThrow(() -> new UserNotFoundException(
                    "User to be rated not found with ID: " + ratedUserId));
        } catch (SQLException e) {
            throw new RatingOperationException("Database error checking user " + ratedUserId, e);
        }

        if (userToRate.getId() == rater.getId()) {
            throw new RatingOperationException("Users cannot rate their own profiles.");
        }

        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            Optional<Rating> existingRating = ratingDao.findByRaterAndEntity(rater.getId(),
                    Rating.RatedEntityType.USER.toString(), ratedUserId);
            if (existingRating.isPresent()) {
                throw new RatingOperationException("You have already rated this user.");
            }

            Rating rating =
                    new Rating(rater.getId(), Rating.RatedEntityType.USER, ratedUserId, score, comment);
            int ratingId = ratingDao.save(rating, conn);
            
            if (ratingId <= 0) {
                throw new RatingOperationException(
                        "Failed to save rating for user (DAO error). DAO returned: " + ratingId);
            }

            rating.setRatingId(ratingId);
            rating.setRaterUsername(rater.getUsername());

            if (comment != null && !comment.trim().isEmpty()) {
                try {
                    commentService.postCommentToUserProfile(ratedUserId, rater,
                            "[Rating " + score + "/5] " + comment, conn);
                } catch (CommentOperationException coe) {
                    System.err.println("RatingServiceImpl: Rating for user " + ratedUserId
                            + " saved, but failed to post rating comment as profile comment: "
                            + coe.getMessage());
                }
            }
            if (transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
            return rating;

        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("RatingService: Error during rollback (submitRatingForUser SQL): " + re.getMessage());}
            }
            if (e.getErrorCode() == 1062 || (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("uq_rating_instance"))) {
                throw new RatingOperationException(
                        "You have already rated this user (caught as SQLException).", e);
            }
            throw new RatingOperationException("Database error submitting rating for user.", e);
        } catch (RatingOperationException | UserNotFoundException e){
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("RatingService: Error during rollback (submitRatingForUser Op/User): " + re.getMessage());}
            }
            throw e;
        } finally {
            if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("RatingServiceImpl: Error ensuring transaction is rolled back in finally (submitRatingForUser): " + se.getMessage());}
            }
        }
    }

    @Override
    public Optional<Rating> getRatingById(int ratingId) throws RatingOperationException {
        try {
            return ratingDao.findById(ratingId);
        } catch (SQLException e) {
            throw new RatingOperationException("Database error fetching rating by ID " + ratingId,
                    e);
        }
    }

    @Override
    public List<Rating> getRatingsForEvent(int eventId)
            throws RatingOperationException, EventNotFoundException {
        try {
            if (!eventDao.findById(eventId).isPresent()) {
                throw new EventNotFoundException(
                        "Event not found with ID: " + eventId + " when fetching ratings.");
            }
            return ratingDao.findByRatedEntity(Rating.RatedEntityType.EVENT.toString(), eventId);
        } catch (SQLException e) {
            throw new RatingOperationException(
                    "Database error fetching ratings for event " + eventId, e);
        }
    }

    @Override
    public List<Rating> getRatingsForUser(int ratedUserId)
            throws RatingOperationException, UserNotFoundException {
        try {
            if (!userDao.findById(ratedUserId).isPresent()) {
                throw new UserNotFoundException(
                        "User not found with ID: " + ratedUserId + " when fetching ratings.");
            }
            return ratingDao.findByRatedEntity(Rating.RatedEntityType.USER.toString(), ratedUserId);
        } catch (SQLException e) {
            throw new RatingOperationException(
                    "Database error fetching ratings for user " + ratedUserId, e);
        }
    }

    @Override
    public List<Rating> getRatingsGivenByUser(int raterUserId)
            throws RatingOperationException, UserNotFoundException {
        try {
            if (!userDao.findById(raterUserId).isPresent()) {
                throw new UserNotFoundException("Rater user not found with ID: " + raterUserId
                        + " when fetching given ratings.");
            }
            return ratingDao.findByRaterUserId(raterUserId);
        } catch (SQLException e) {
            throw new RatingOperationException(
                    "Database error fetching ratings given by user " + raterUserId, e);
        }
    }

    @Override
    public Rating updateRatingComment(int ratingId, String newComment, User rater)
            throws RatingOperationException, UserNotFoundException {
        if (rater == null)
            throw new RatingOperationException("User cannot be null for updating rating comment.");
        if (newComment != null && newComment.length() > 1000) {
            throw new RatingOperationException("Rating comment is too long (max 1000 characters).");
        }

        Rating existingRating = getRatingOrThrow(ratingId, "updating comment");

        if (existingRating.getRaterUserId() != rater.getId()) {
            throw new RatingOperationException(
                    "Unauthorized: You can only update comments on your own ratings.");
        }

        String oldRatingComment = existingRating.getComment();
        boolean profileCommentUpdateNeeded = false;
        if (existingRating.getRatedEntityType() == Rating.RatedEntityType.USER
                && oldRatingComment != null && !oldRatingComment.trim().isEmpty()) {
            profileCommentUpdateNeeded = true;
        }
        
        Connection conn = null;
        boolean transactionOwner = false;
        try {
             if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            boolean updated = ratingDao.updateComment(ratingId, newComment, conn);
            if (updated) {
                existingRating.setComment(newComment);
                existingRating.setUpdatedAt(LocalDateTime.now());
                existingRating.setRaterUsername(rater.getUsername());

                if (profileCommentUpdateNeeded) {
                    List<com.palveo.model.Comment> profileComments = commentService
                            .getCommentsForUserProfile(existingRating.getRatedEntityId());
                    Optional<com.palveo.model.Comment> oldProfileCommentFromRating =
                            profileComments.stream()
                                    .filter(pc -> pc.getAuthorUserId() == rater.getId()
                                            && pc.getContent() != null
                                            && pc.getContent()
                                                    .startsWith("[Rating "
                                                            + existingRating.getScore() + "/5] "
                                                            + oldRatingComment))
                                    .findFirst();

                    if (oldProfileCommentFromRating.isPresent()) {
                        commentService.deleteComment(
                                oldProfileCommentFromRating.get().getCommentId(), rater, conn);
                    }

                    if (newComment != null && !newComment.trim().isEmpty()) {
                        commentService.postCommentToUserProfile(
                                existingRating.getRatedEntityId(), rater,
                                "[Rating " + existingRating.getScore() + "/5] " + newComment, conn);
                    }
                }
                if(transactionOwner) {
                    DatabaseConnection.commitTransaction();
                }
                return existingRating;
            } else {
                throw new RatingOperationException("Failed to update rating comment in database.");
            }
        } catch (SQLException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("RatingService: Error during rollback (updateRatingComment SQL): " + re.getMessage());}
            }
            throw new RatingOperationException("Database error updating rating comment.", e);
        } catch (UserNotFoundException | CommentOperationException | RatingOperationException | EventNotFoundException e) {
             if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("RatingService: Error during rollback (updateRatingComment Op/User/Comment): " + re.getMessage());}
            }
            System.err.println(
                    "RatingServiceImpl: Failed to update/delete corresponding profile comment during rating comment update: "
                            + e.getMessage());
             if (e instanceof RatingOperationException) throw (RatingOperationException)e;
             if (e instanceof UserNotFoundException) throw (UserNotFoundException)e;
             throw new RatingOperationException("Error handling associated profile comment during rating update.", e);
        } finally {
            if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("RatingServiceImpl: Error ensuring transaction is rolled back in finally (updateRatingComment): " + se.getMessage());}
            }
        }
    }

    @Override
    public void deleteRating(int ratingId, User currentUser) throws RatingOperationException, UserNotFoundException {
        if (currentUser == null)
            throw new RatingOperationException("Current user cannot be null for deleting rating.");

        Rating ratingToDelete = getRatingOrThrow(ratingId, "deleting rating");

        boolean canDelete = false;
        if (ratingToDelete.getRaterUserId() == currentUser.getId()) {
            canDelete = true;
        }
        
        if (!canDelete) {
            throw new RatingOperationException(
                    "Unauthorized: You do not have permission to delete this rating.");
        }

        boolean profileCommentDeleteNeeded = false;
        String ratingCommentContentToDelete = ratingToDelete.getComment();
        int ratingScoreToDelete = ratingToDelete.getScore();
        int ratedEntityTypeId = ratingToDelete.getRatedEntityId();

        if (ratingToDelete.getRatedEntityType() == Rating.RatedEntityType.USER && ratingCommentContentToDelete != null && !ratingCommentContentToDelete.trim().isEmpty()) {
            profileCommentDeleteNeeded = true;
        }
        
        Connection conn = null;
        boolean transactionOwner = false;
        try {
            if (DatabaseConnection.getTransactionalConnection() == null) {
                DatabaseConnection.beginTransaction();
                transactionOwner = true;
            }
            conn = DatabaseConnection.getTransactionalConnection();

            boolean deleted = ratingDao.delete(ratingId, conn);
            if (!deleted) {
                throw new RatingOperationException(
                        "Failed to delete rating from database (ID: " + ratingId + ").");
            }

            if (profileCommentDeleteNeeded) {
                try {
                    List<com.palveo.model.Comment> profileComments =
                        commentService.getCommentsForUserProfile(ratedEntityTypeId);
                    Optional<com.palveo.model.Comment> profileCommentFromRating = profileComments
                        .stream().filter(pc -> pc.getAuthorUserId() == currentUser.getId() &&
                            pc.getContent() != null
                                    && pc.getContent().startsWith("[Rating " + ratingScoreToDelete
                                            + "/5] " + ratingCommentContentToDelete))
                            .findFirst();

                    if (profileCommentFromRating.isPresent()) {
                        commentService.deleteComment(profileCommentFromRating.get().getCommentId(),
                                currentUser, conn); 
                    }
                } catch (UserNotFoundException | CommentOperationException | EventNotFoundException ce) {
                    System.err.println(
                            "RatingServiceImpl: Rating deleted, but failed to delete corresponding profile comment: "
                                    + ce.getMessage());
                }
            }
            if(transactionOwner) {
                DatabaseConnection.commitTransaction();
            }
        } catch (SQLException e) {
             if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("RatingService: Error during rollback (deleteRating SQL): " + re.getMessage());}
            }
            throw new RatingOperationException("Database error deleting rating.", e);
        } catch (RatingOperationException e) {
            if (transactionOwner && conn != null) {
                try { DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("RatingService: Error during rollback (deleteRating Op/User): " + re.getMessage());}
            }
            throw e;
        } finally {
            if(transactionOwner && DatabaseConnection.getTransactionalConnection() != null && conn == DatabaseConnection.getTransactionalConnection()){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("RatingServiceImpl: Error ensuring transaction is rolled back in finally (deleteRating): " + se.getMessage());}
            }
        }
    }

    @Override
    public double calculateAverageRatingForEvent(int eventId)
            throws RatingOperationException, EventNotFoundException {
        List<Rating> ratings = getRatingsForEvent(eventId);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Rating rating : ratings) {
            sum += rating.getScore();
        }
        return sum / ratings.size();
    }

    @Override
    public double calculateAverageRatingForUser(int ratedUserId)
            throws RatingOperationException, UserNotFoundException {
        List<Rating> ratings = getRatingsForUser(ratedUserId);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Rating rating : ratings) {
            sum += rating.getScore();
        }
        return sum / ratings.size();
    }
}