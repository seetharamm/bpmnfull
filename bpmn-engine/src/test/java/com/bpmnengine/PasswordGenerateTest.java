package com.bpmnengine;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Generates correct BCrypt hashes for seed users.
 * All users share the same password: Alice@123
 */
public class PasswordGenerateTest {

    @Test
    void generateHash() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder(10);
        // All seed users share password "Alice@123" per the SQL comment convention
        String hash = enc.encode("Alice@123");
        System.out.println("BCrypt hash for 'Alice@123': " + hash);
        System.out.println("Verified: " + enc.matches("Alice@123", hash));
    }
}
