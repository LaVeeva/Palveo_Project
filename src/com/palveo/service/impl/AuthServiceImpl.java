package com.palveo.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import com.palveo.dao.UserDao;
import com.palveo.dao.impl.UserDaoImpl;
import com.palveo.db.DatabaseConnection;
import com.palveo.model.User;
import com.palveo.service.AuthService;
import com.palveo.service.exception.RegistrationException;
import com.palveo.util.PasswordUtils;
import com.palveo.util.ValidationUtils;

public class AuthServiceImpl implements AuthService {

    private UserDao userDao;

    public AuthServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    public AuthServiceImpl() {
        this.userDao = new UserDaoImpl();
    }

    @Override
    public User registerUser(User userToRegister, String rawPassword, String securityQuestion,
            String rawSecurityAnswer) throws RegistrationException {

        if (ValidationUtils.isNullOrEmpty(userToRegister.getUsername())) {
            throw new RegistrationException("Username cannot be empty.");
        }
        if (!ValidationUtils.isValidUsernameFormat(userToRegister.getUsername())) {
            throw new RegistrationException(
                    "Invalid username format. Username must be 3-20 characters, alphanumeric, and can include underscores or hyphens.");
        }
        if (userToRegister.getEmail() == null
                || !ValidationUtils.isValidEmail(userToRegister.getEmail())) {
            throw new RegistrationException("Invalid email format or email is empty.");
        }
        if (ValidationUtils.isNullOrEmpty(rawPassword)) {
            throw new RegistrationException("Password cannot be empty.");
        }
        if (!ValidationUtils.isPasswordStrongEnough(rawPassword)) {
            throw new RegistrationException(
                    "Password is not strong enough. Must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character.");
        }
        if (ValidationUtils.isNullOrEmpty(userToRegister.getFirstName())) {
            throw new RegistrationException("First name cannot be empty.");
        }
        if (!ValidationUtils.isValidNameFormat(userToRegister.getFirstName())) {
            throw new RegistrationException(
                    "Invalid first name format. Please use letters, spaces, hyphens, or apostrophes.");
        }
        if (ValidationUtils.isNullOrEmpty(userToRegister.getLastName())) {
            throw new RegistrationException("Last name cannot be empty.");
        }
        if (!ValidationUtils.isValidNameFormat(userToRegister.getLastName())) {
            throw new RegistrationException(
                    "Invalid last name format. Please use letters, spaces, hyphens, or apostrophes.");
        }
        if (ValidationUtils.isNullOrEmpty(userToRegister.getCity())) {
            throw new RegistrationException("City cannot be empty.");
        }
        if (!userToRegister.isEulaAccepted()) {
            throw new RegistrationException("EULA must be accepted to register.");
        }
        if (!userToRegister.isAgeVerified()) {
            throw new RegistrationException("Age verification is required (must be 18 or older).");
        }
        if (ValidationUtils.isNullOrEmpty(securityQuestion)) {
            throw new RegistrationException("Security question cannot be empty.");
        }
        if (ValidationUtils.isNullOrEmpty(rawSecurityAnswer)) {
            throw new RegistrationException("Security answer cannot be empty.");
        }

        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            if (userDao.findByUsername(userToRegister.getUsername()).isPresent()) {
                throw new RegistrationException(
                        "Username '" + userToRegister.getUsername() + "' is already taken.");
            }
            if (userDao.findByEmail(userToRegister.getEmail()).isPresent()) {
                throw new RegistrationException(
                        "Email '" + userToRegister.getEmail() + "' is already registered.");
            }
            
            String passwordSalt = PasswordUtils.generateSalt();
            String hashedPassword = PasswordUtils.hashPassword(rawPassword, passwordSalt);
            userToRegister.setSalt(passwordSalt);
            userToRegister.setPasswordHash(hashedPassword);

            String securityAnswerSalt = PasswordUtils.generateSalt();
            String hashedSecurityAnswer = PasswordUtils.hashPassword(rawSecurityAnswer, securityAnswerSalt);
            userToRegister.setSecurityQuestion(securityQuestion);
            userToRegister.setSecurityAnswerHash(hashedSecurityAnswer);
            userToRegister.setSecurityAnswerSalt(securityAnswerSalt);
            
            int newUserId = userDao.save(userToRegister, conn); 
            if (newUserId != -1) {
                userToRegister.setId(newUserId);
                DatabaseConnection.commitTransaction();
                return userToRegister;
            } else {
                throw new RegistrationException(
                        "Failed to save user to the database. No ID was returned.");
            }
        } catch (SQLException e) {
            System.err.println("AuthService: SQL error during user save/registration check: " + e.getMessage());
             try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("AuthService: Error during rollback: " + re.getMessage());}
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate entry")) {
                throw new RegistrationException("Username or email became unavailable. Please try a different one.", e);
            }
            throw new RegistrationException("An error occurred while saving user information. Please try again later.", e);
        } catch (RegistrationException re) {
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException rbe) { System.err.println("AuthService: Error during rollback after RegistrationException: " + rbe.getMessage());}
            throw re; 
        } finally {
            if(DatabaseConnection.getTransactionalConnection() != null){
                try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {se.printStackTrace();} 
            }
        }
    }


    @Override
    public Optional<User> loginUser(String usernameOrEmail, String rawPassword) {
        if (ValidationUtils.isNullOrEmpty(usernameOrEmail)
                || ValidationUtils.isNullOrEmpty(rawPassword)) {
            return Optional.empty();
        }
        Optional<User> userOpt;
        try {
            userOpt = userDao.findByUsername(usernameOrEmail);
            if (!userOpt.isPresent()) {
                userOpt = userDao.findByEmail(usernameOrEmail);
            }
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (PasswordUtils.verifyPassword(rawPassword, user.getPasswordHash(),
                        user.getSalt())) {
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("AuthService: SQL error during login: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getSecurityQuestionForUser(String username) {
        try {
            Optional<User> userOpt = userDao.findByUsername(username);
            if (userOpt.isPresent()) {
                return Optional.ofNullable(userOpt.get().getSecurityQuestion());
            }
        } catch (SQLException e) {
            System.err.println("AuthService: SQL error fetching security question for " + username + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean verifySecurityAnswer(String username, String rawSecurityAnswer) {
        if (ValidationUtils.isNullOrEmpty(username)
                || ValidationUtils.isNullOrEmpty(rawSecurityAnswer)) {
            return false;
        }
        try {
            Optional<User> userOpt = userDao.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getSecurityAnswerHash() == null || user.getSecurityAnswerSalt() == null) {
                    System.err.println("AuthService: User " + username + " has no security answer hash or security answer salt set for verification.");
                    return false;
                }
                return PasswordUtils.verifyPassword(rawSecurityAnswer, user.getSecurityAnswerHash(),
                        user.getSecurityAnswerSalt());
            }
        } catch (SQLException e) {
            System.err.println("AuthService: SQL error verifying security answer for " + username
                    + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean resetPassword(String username, String newRawPassword)
            throws RegistrationException {
        if (ValidationUtils.isNullOrEmpty(username)) {
            throw new RegistrationException("Username cannot be empty for password reset.");
        }
        if (!ValidationUtils.isPasswordStrongEnough(newRawPassword)) {
            throw new RegistrationException(
                    "New password is not strong enough. Must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character.");
        }
        Connection conn = null;
        try {
            DatabaseConnection.beginTransaction();
            conn = DatabaseConnection.getTransactionalConnection();

            Optional<User> userOpt = userDao.findByUsername(username); 
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String newPasswordSalt = PasswordUtils.generateSalt();
                String newHashedPassword = PasswordUtils.hashPassword(newRawPassword, newPasswordSalt);
                boolean success = userDao.updatePassword(user.getId(), newHashedPassword, newPasswordSalt, conn);
                if (!success) {
                    System.err.println("AuthService: Failed to update password in DB for user " + username);
                    throw new RegistrationException("Could not update password in the database.");
                }
                DatabaseConnection.commitTransaction();
                return true;
            } else {
                throw new RegistrationException("User '" + username + "' not found for password reset.");
            }
        } catch (SQLException e) {
            System.err.println("AuthService: SQL error resetting password for " + username + ": " + e.getMessage());
            try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException re) { System.err.println("AuthService: Error during rollback on SQL reset error: " + re.getMessage());}
            throw new RegistrationException("An error occurred while resetting the password. Please try again later.", e);
        } catch (RegistrationException re) {
             try { if (conn != null) DatabaseConnection.rollbackTransaction(); } catch (SQLException rbe) { System.err.println("AuthService: Error during rollback after RegistrationException: " + rbe.getMessage());}
            throw re;
        } finally {
             if(DatabaseConnection.getTransactionalConnection() != null){
                 try{ DatabaseConnection.rollbackTransaction(); } catch (SQLException se) {se.printStackTrace();}
             }
        }
    }
}