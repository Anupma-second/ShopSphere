package com.shopsphere.ecommerce;

import com.shopsphere.ecommerce.entity.Category;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.repository.CategoryRepository;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Swagger docs, created_at / updated_at and login rate limiting (H2 only).
 *
 * Run just this test:  ./mvnw test -Dtest=PolishFeaturesIntegrationTest
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:polish;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "jwt.secret=test-secret-test-secret-test-secret-test-secret",
                "razorpay.key.id=rzp_test_dummy",
                "razorpay.key.secret=dummy",
                "spring.task.scheduling.enabled=false",
                "app.login.max-failures-per-account=3"
        })
class PolishFeaturesIntegrationTest {

    @Autowired Environment env;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void swaggerDocsArePublic() throws Exception {
        HttpResponse<String> docs = get("/v3/api-docs");
        assertThat(docs.statusCode()).isEqualTo(200);
        assertThat(docs.body()).contains("ShopSphere API", "/api/auth/login", "bearerAuth");

        HttpResponse<String> ui = get("/swagger-ui/index.html");
        assertThat(ui.statusCode()).isEqualTo(200);
    }

    @Test
    void timestampsAreFilledAutomatically() throws Exception {
        Category saved = categoryRepository.save(new Category("Shoes", "Footwear"));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());

        Thread.sleep(20);
        saved.setDescription("All footwear");
        Category updated = categoryRepository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    void loginIsBlockedAfterTooManyFailures() throws Exception {
        createUser("victim@test.local", "RightPass123");

        // a success in between must not be blocked and resets the counter
        assertThat(login("victim@test.local", "wrong").statusCode()).isEqualTo(401);
        assertThat(login("victim@test.local", "RightPass123").statusCode()).isEqualTo(200);

        for (int i = 0; i < 3; i++) {
            assertThat(login("victim@test.local", "wrong").statusCode()).isEqualTo(401);
        }

        // 4th try is refused even with the RIGHT password
        HttpResponse<String> blocked = login("VICTIM@test.local", "RightPass123");
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(blocked.body()).contains("Too many failed login attempts");
        assertThat(blocked.headers().firstValue("Retry-After")).isPresent();

        // other accounts are not affected
        createUser("other@test.local", "OtherPass123");
        assertThat(login("other@test.local", "OtherPass123").statusCode()).isEqualTo(200);
    }

    // ---------------------------------------------------------------------

    private void createUser(String email, String password) {
        User user = new User();
        user.setName("Test");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + env.getProperty("local.server.port") + path);
    }
}
