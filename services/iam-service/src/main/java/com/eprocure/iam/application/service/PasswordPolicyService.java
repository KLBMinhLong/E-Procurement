package com.eprocure.iam.application.service;

import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.model.User;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {
    private final PasswordHashService passwordHashService;

    public PasswordPolicyService(PasswordHashService passwordHashService) {
        this.passwordHashService = passwordHashService;
    }

    public void validate(User user, String newPassword, String confirmPassword, List<String> recentPasswordHashes) {
        if (!newPassword.equals(confirmPassword) || !isStrong(newPassword) || containsIdentity(user, newPassword)) {
            throw new BusinessException(ErrorCode.IAM_008);
        }
        if (recentPasswordHashes.stream()
                .anyMatch(passwordHash -> passwordHashService.matches(newPassword, user.getId(), passwordHash))) {
            throw new BusinessException(ErrorCode.IAM_008);
        }
    }

    private boolean isStrong(String password) {
        return password.length() >= 8
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(value -> !Character.isLetterOrDigit(value));
    }

    private boolean containsIdentity(User user, String password) {
        String normalizedPassword = password.toLowerCase(Locale.ROOT);
        String username = user.getUsername().toLowerCase(Locale.ROOT);
        String emailLocal = user.getEmail().split("@", 2)[0].toLowerCase(Locale.ROOT);
        return normalizedPassword.contains(username) || normalizedPassword.contains(emailLocal);
    }
}
