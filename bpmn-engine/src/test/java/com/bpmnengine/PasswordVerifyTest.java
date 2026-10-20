package com.bpmnengine;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Quick utility test — verifies what plaintext matches the seed password hash.
 * Run with: mvn test -Dtest=PasswordVerifyTest -pl bpmn-engine
 */
public class PasswordVerifyTest {

    private static final String HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Test
    void checkCandidates() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder(10);

        // Candidates mentioned in the SQL comment + variations
        String[] candidates = {
            "Alice@123", "alice@123", "ALICE@123",
            "password", "Password1", "test", "Test@123",
            "admin", "Admin@123", "Admin1234",
            "changeme", "secret",
        };

        System.out.println("Testing hash: " + HASH);
        boolean found = false;
        for (String pwd : candidates) {
            boolean match = enc.matches(pwd, HASH);
            System.out.printf("  %-20s → %s%n", pwd, match ? "✓ MATCH" : "✗");
            if (match) found = true;
        }
        if (!found) {
            System.out.println("  No candidate matched — hash was pre-generated with an unknown password.");
        }
        // Test always passes — this is diagnostic only
    }
}
