package com.team8.damo.fixture;

import com.team8.damo.entity.User;

public class UserFixture {

    public static User create() {
        return new User(1L, "test@example.com", 12345678L);
    }

    public static User create(Long id) {
        return new User(id, "test" + id + "@example.com", 12345678L + id);
    }

    public static User create(Long id, String email) {
        return new User(id, email, 12345678L);
    }
}
