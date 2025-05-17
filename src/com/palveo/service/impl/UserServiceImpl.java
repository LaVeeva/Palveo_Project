package com.palveo.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.palveo.config.AppConfig;
import com.palveo.dao.UserDao;
import com.palveo.dao.impl.UserDaoImpl;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.User;
import com.palveo.service.UserService;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.service.exception.UserOperationException;
import com.palveo.util.PasswordUtils;
import com.palveo.util.ValidationUtils;

public class UserServiceImpl implements UserService {

    private UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
        initializeStorageDirectories();
    }

    public UserServiceImpl() {
        this.userDao = new UserDaoImpl();
        initializeStorageDirectories();
    }
    
    private void initializeStorageDirectories() {
        try {
            Path avatarsDir = Paths.get(AppConfig.getUserAvatarsDir());
            if (!Files.exists(avatarsDir)) {
                Files.createDirectories(avatarsDir);
            }
        } catch (IOException e) {
            System.err.println("UserServiceImpl: Could not create user avatars directory: " + e.getMessage());
        }
    }

    @Override
    public Optional<User> getUserById(int userId) {
        try {
            return userDao.findById(userId);
        } catch (SQLException e) {
            System.err.println(
                    "UserService: SQL error fetching user by ID " + userId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        try {
            return userDao.findByUsername(username);
        } catch (SQLException e) {
            System.err.println("UserService: SQL error fetching user by username " + username + ": "
                    + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public User updateUserProfile(User userWithUpdates, User currentUser)
            throws UserNotFoundException, UserOperationException {
        if (userWithUpdates == null || currentUser == null) {
            throw new UserOperationException("User objects cannot be null for profile update.");
        }
        if (userWithUpdates.getId() != currentUser.getId()) {
            throw new UserOperationException(
                    "Unauthorized: Users can only update their own profiles.");
        }

        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            User existingUser = userDao.findById(currentUser.getId()) 
                    .orElseThrow(() -> new UserNotFoundException(
                            "Current user not found in database with ID: " + currentUser.getId()));

            if (userWithUpdates.getFirstName() != null) {
                if (!ValidationUtils.isValidNameFormat(userWithUpdates.getFirstName())) {
                    throw new UserOperationException("Invalid first name format.");
                }
                existingUser.setFirstName(userWithUpdates.getFirstName());
            }
            if (userWithUpdates.getLastName() != null) {
                if (!ValidationUtils.isValidNameFormat(userWithUpdates.getLastName())) {
                    throw new UserOperationException("Invalid last name format.");
                }
                existingUser.setLastName(userWithUpdates.getLastName());
            }
            if (userWithUpdates.getBio() != null) {
                if (userWithUpdates.getBio().length() > 5000) {
                    throw new UserOperationException("Bio is too long (max 5000 characters).");
                }
                existingUser.setBio(userWithUpdates.getBio());
            }
            if (userWithUpdates.getProfileImagePath() != null) {
                existingUser.setProfileImagePath(userWithUpdates.getProfileImagePath());
            }
            if (userWithUpdates.getCity() != null) {
                if (ValidationUtils.isNullOrEmpty(userWithUpdates.getCity())) {
                    throw new UserOperationException(
                            "City cannot be empty if provided for update.");
                }
                existingUser.setCity(userWithUpdates.getCity());
            }
            if (userWithUpdates.getDistrict() != null) {
                existingUser.setDistrict(userWithUpdates.getDistrict());
            }
            if (userWithUpdates.getSecurityQuestion() != null) {
                if (ValidationUtils.isNullOrEmpty(userWithUpdates.getSecurityQuestion())) {
                    throw new UserOperationException(
                            "Security question cannot be empty if provided for update.");
                }
                existingUser.setSecurityQuestion(userWithUpdates.getSecurityQuestion());
            }
            if (userWithUpdates.getSecurityAnswerHash() != null) {
                 existingUser.setSecurityAnswerHash(userWithUpdates.getSecurityAnswerHash());
            }
            if (userWithUpdates.getSecurityAnswerSalt() != null) {
                 existingUser.setSecurityAnswerSalt(userWithUpdates.getSecurityAnswerSalt());
            }

            boolean success = userDao.update(existingUser, conn);
            if (!success) {
                throw new UserOperationException("Failed to update user profile in the database.");
            }
            DatabaseConnection.commitTransaction();
            return existingUser;

        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("UserService: Error during rollback on profile update: " + re.getMessage());}
            throw new UserOperationException("Database error updating user profile.", e);
        } catch (UserNotFoundException | UserOperationException e ) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("UserService: Error during rollback on profile update: " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("UserServiceImpl: Error ensuring transaction is rolled back in finally: " + se.getMessage());}
            }
        }
    }

    @Override
    public boolean changeUserPassword(User user, String oldPassword, String newPassword)
            throws UserNotFoundException, UserOperationException {
        if (user == null) {
            throw new UserOperationException("User cannot be null for password change.");
        }
        if (ValidationUtils.isNullOrEmpty(oldPassword)
                || ValidationUtils.isNullOrEmpty(newPassword)) {
            throw new UserOperationException("Old and new passwords cannot be empty.");
        }
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            User fullUserDetails = userDao.findById(user.getId()).orElseThrow( 
                    () -> new UserNotFoundException("User not found with ID: " + user.getId()));

            if (!PasswordUtils.verifyPassword(oldPassword, fullUserDetails.getPasswordHash(),
                    fullUserDetails.getSalt())) {
                throw new UserOperationException("Incorrect old password.");
            }

            if (!ValidationUtils.isPasswordStrongEnough(newPassword)) {
                throw new UserOperationException(
                        "New password is not strong enough. Must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character.");
            }

            if (oldPassword.equals(newPassword)) {
                throw new UserOperationException(
                        "New password cannot be the same as the old password.");
            }

            String newSalt = PasswordUtils.generateSalt();
            String newHashedPassword = PasswordUtils.hashPassword(newPassword, newSalt);

            boolean success = userDao.updatePassword(fullUserDetails.getId(), newHashedPassword, newSalt, conn);
            if(success) {
                DatabaseConnection.commitTransaction();
                return true;
            } else {
                throw new UserOperationException("Failed to update password in database.");
            }

        } catch (SQLException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("UserService: Error during rollback on password change: " + re.getMessage());}
            throw new UserOperationException("Database error changing user password.", e);
        } catch (UserNotFoundException | UserOperationException e) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("UserService: Error during rollback on password change: " + re.getMessage());}
            throw e;
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {System.err.println("UserServiceImpl: Error ensuring transaction is rolled back in finally (password change): " + se.getMessage());}
            }
        }
    }

    @Override
    public List<User> searchUsers(String searchTerm) throws UserOperationException {
        if (ValidationUtils.isNullOrEmpty(searchTerm) || searchTerm.trim().length() < 2) {
            return new ArrayList<>();
        }
        String term = searchTerm.trim().toLowerCase();
        try {
            List<User> allUsers = userDao.findAll();
            return allUsers.stream()
                    .filter(user -> (user.getUsername().toLowerCase().contains(term))
                            || (user.getFirstName() != null
                                    && user.getFirstName().toLowerCase().contains(term))
                            || (user.getLastName() != null
                                    && user.getLastName().toLowerCase().contains(term)))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new UserOperationException("Database error searching users.", e);
        }
    }
}