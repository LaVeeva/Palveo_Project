package com.palveo.service;

import java.util.List;
import java.util.Optional;
import com.palveo.model.User;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.service.exception.UserOperationException;

public interface UserService {

    Optional<User> getUserById(int userId);

    Optional<User> getUserByUsername(String username);

    User updateUserProfile(User userWithUpdates, User currentUser)
            throws UserNotFoundException, UserOperationException;

    boolean changeUserPassword(User user, String oldPassword, String newPassword)
            throws UserNotFoundException, UserOperationException;

    List<User> searchUsers(String searchTerm) throws UserOperationException;
}
