package com.tissue.feature.workflow.application.dto;

public record StateMigrationMapping(Long fromStateId, NodeIdentifier toStateIdentifier) {}
