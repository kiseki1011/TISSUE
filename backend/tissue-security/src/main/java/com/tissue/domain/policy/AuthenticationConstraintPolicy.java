package com.tissue.domain.policy;

public interface AuthenticationConstraintPolicy {

    String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,30}$";

    int PASSWORD_MIN_LENGTH = 8;
    int PASSWORD_MAX_LENGTH = 30;
}
