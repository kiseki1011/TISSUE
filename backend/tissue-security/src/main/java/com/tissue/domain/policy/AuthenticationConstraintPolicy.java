package com.tissue.domain.policy;

public interface AuthenticationConstraintPolicy {

    String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,30}$";
    String PASSWORD_PATTERN_MESSAGE =
            "Password must be 8-30 characters long and include at least one letter and one number.";

    int PASSWORD_MIN_LENGTH = 8;
    int PASSWORD_MAX_LENGTH = 30;
}
