package com.taskmanagement.api.infra.web.dto;

public record PatchTaskRequest(String title, Boolean completed) {}
