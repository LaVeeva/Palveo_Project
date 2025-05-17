package com.palveo.service;

import java.util.List;
import java.util.Optional;
import com.palveo.model.Rating;
import com.palveo.model.User;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.RatingOperationException;
import com.palveo.service.exception.UserNotFoundException;

public interface RatingService {
    Rating submitRatingForEvent(int eventId, User rater, int score, String comment)
            throws RatingOperationException, EventNotFoundException, UserNotFoundException;

    Rating submitRatingForUser(int ratedUserId, User rater, int score, String comment)
            throws RatingOperationException, UserNotFoundException;

    Optional<Rating> getRatingById(int ratingId) throws RatingOperationException;

    List<Rating> getRatingsForEvent(int eventId)
            throws RatingOperationException, EventNotFoundException;

    List<Rating> getRatingsForUser(int ratedUserId)
            throws RatingOperationException, UserNotFoundException;

    List<Rating> getRatingsGivenByUser(int raterUserId)
            throws RatingOperationException, UserNotFoundException;

    Rating updateRatingComment(int ratingId, String newComment, User rater)
            throws RatingOperationException, UserNotFoundException;

    void deleteRating(int ratingId, User currentUser)
            throws RatingOperationException, UserNotFoundException;

    double calculateAverageRatingForEvent(int eventId)
            throws RatingOperationException, EventNotFoundException;

    double calculateAverageRatingForUser(int ratedUserId)
            throws RatingOperationException, UserNotFoundException;
}
