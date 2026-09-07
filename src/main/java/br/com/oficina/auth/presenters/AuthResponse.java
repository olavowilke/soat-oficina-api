package br.com.oficina.auth.presenters;

public record AuthResponse(String token, String username, String role, long expiresInMs) {}
