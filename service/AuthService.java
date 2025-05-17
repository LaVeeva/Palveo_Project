package com.palveo.service;

import java.util.Optional;
import com.palveo.model.User;
import com.palveo.service.exception.RegistrationException;

public interface AuthService {

    User registerUser(User userToRegister, String rawPassword, String securityQuestion,
            String rawSecurityAnswer) throws RegistrationException;

    Optional<User> loginUser(String usernameOrEmail, String rawPassword);

    Optional<String> getSecurityQuestionForUser(String username);

    boolean verifySecurityAnswer(String username, String rawSecurityAnswer);

    boolean resetPassword(String username, String newRawPassword) throws RegistrationException;

}
