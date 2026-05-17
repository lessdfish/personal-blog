package com.blogcommon.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestUserContextTest {

    @AfterEach
    void tearDown() {
        RequestUserContext.clear();
    }

    @Test
    void shouldSetAndGetUserContext() {
        RequestUserContext.setUserId(123L);
        RequestUserContext.setRole("MODERATOR");
        RequestUserContext.setUsername("alice");

        assertEquals(123L, RequestUserContext.getUserId());
        assertEquals("MODERATOR", RequestUserContext.getRole());
        assertEquals("alice", RequestUserContext.getUsername());
    }

    @Test
    void clearShouldRemoveStoredValues() {
        RequestUserContext.setUserId(456L);
        RequestUserContext.setRole("USER");
        RequestUserContext.setUsername("bob");

        RequestUserContext.clear();

        assertNull(RequestUserContext.getUserId());
        assertNull(RequestUserContext.getRole());
        assertNull(RequestUserContext.getUsername());
    }
}
