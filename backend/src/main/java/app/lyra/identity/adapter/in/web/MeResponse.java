package app.lyra.identity.adapter.in.web;

import app.lyra.identity.domain.User;

import java.util.UUID;

record MeResponse(UUID id, String email, String name) {

    static MeResponse from(User user) {
        return new MeResponse(user.getId().value(), user.getEmail(), user.getName());
    }
}
