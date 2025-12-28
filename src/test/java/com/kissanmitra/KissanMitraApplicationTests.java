package com.kissanmitra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application context loading test.
 *
 * <p>Note: This test requires MongoDB and Redis to be running.
 * If these services are not available, the test will fail.
 * 
 * <p>For CI/CD, use Testcontainers or skip this test if services aren't available.
 * 
 * <p>Uses MOCK web environment to allow security configuration to load without requiring
 * a real HTTP server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class KissanMitraApplicationTests {

	@Test
	void contextLoads() {
		// Test passes if Spring context loads successfully
		// This validates that all beans can be created and dependencies are satisfied
	}

}
