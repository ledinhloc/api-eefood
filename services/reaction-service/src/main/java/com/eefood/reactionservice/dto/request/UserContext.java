package com.eefood.reactionservice.dto.request;

import com.eefood.reactionservice.dto.response.UserResponse;

import java.util.List;

public record UserContext(UserResponse user,
                          List<Long> newFollowings,
                          List<Long> oldFollowings) {
    public static UserContext guest() {
        return new UserContext(null, List.of(), List.of());
    }
}
