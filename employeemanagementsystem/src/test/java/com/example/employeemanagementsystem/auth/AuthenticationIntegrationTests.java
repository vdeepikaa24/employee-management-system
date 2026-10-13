package com.example.employeemanagementsystem.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-tests;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=test-database-password",
        "app.datasource.archive.url=jdbc:h2:mem:archive-auth-tests;DB_CLOSE_DELAY=-1",
        "app.datasource.archive.username=sa",
        "app.datasource.archive.password=test-database-password",
        "app.security.jwt.secret=test-only-jwt-secret-with-at-least-32-characters",
        "app.security.bootstrap-admin.username=admin-test@example.com",
        "app.security.bootstrap-admin.password=TestAdminPassword123!"
})
@ActiveProfiles("test")
class AuthenticationIntegrationTests {

    private static final String USER_PASSWORD = "TestUserPassword123!";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Pattern ACCESS_TOKEN_PATTERN =
            Pattern.compile("\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    @LocalServerPort
    private int port;

    @Test
    void registrationCreatesUserAndReturnsBearerToken() throws Exception {
        HttpResponse<String> response = post("/api/auth/register",
                credentialsJson(uniqueEmail(), USER_PASSWORD), null);

        assertTokenResponse(response, 201, "USER");
        assertTrue(response.body().contains("\"tokenType\":\"Bearer\""));
    }

    @Test
    void loginReturnsTokenForRegisteredUser() throws Exception {
        String email = uniqueEmail();
        assertEquals(201, post("/api/auth/register",
                credentialsJson(email, USER_PASSWORD), null).statusCode());

        HttpResponse<String> response = post("/api/auth/login",
                credentialsJson(email, USER_PASSWORD), null);

        assertTokenResponse(response, 200, "USER");
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        String email = uniqueEmail();
        assertEquals(201, post("/api/auth/register",
                credentialsJson(email, USER_PASSWORD), null).statusCode());

        HttpResponse<String> response = post("/api/auth/login",
                credentialsJson(email, "WrongPassword123!"), null);

        assertEquals(401, response.statusCode());
    }

    @Test
    void userCannotCreateDepartment() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        HttpResponse<String> response = post("/api/departments",
                "{\"name\":\"Test Department " + UUID.randomUUID() + "\"}", token);

        assertEquals(403, response.statusCode());
    }

    @Test
    void userCanReadEmployees() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        HttpResponse<String> response = get("/api/employees", token);

        assertEquals(200, response.statusCode());
    }

    @Test
    void employeeSearchAcceptsValidPaginationParameters() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        HttpResponse<String> response = get(
                "/api/employees/search?name=&page=0&size=10&sort=name,asc", token);

        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"size\":10"));
    }

    @Test
    void employeeSearchRejectsNegativePage() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        HttpResponse<String> response = get(
                "/api/employees/search?name=&page=-1&size=10", token);

        assertEquals(400, response.statusCode(), response.body());
    }

    @Test
    void employeeSearchRejectsPageSizeAboveLimit() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        HttpResponse<String> response = get(
                "/api/employees/search?name=&page=0&size=101", token);

        assertEquals(400, response.statusCode(), response.body());
    }

    @Test
    void employeeSearchRejectsZeroPageSize() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        HttpResponse<String> response = get(
                "/api/employees/search?name=&page=0&size=0", token);

        assertEquals(400, response.statusCode(), response.body());
    }

    @Test
    void localFrontendOriginCanMakeCorsPreflightRequests() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        uri("/api/employees/search?page=0&size=10"))
                .header("Origin", "http://localhost:5500")
                .header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), response.body());
        assertEquals("http://localhost:5500",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }

    @Test
    void adminCanCreateDepartment() throws Exception {
        String adminToken = loginAndGetToken(
                "admin-test@example.com", "TestAdminPassword123!");

        HttpResponse<String> response = post("/api/departments",
                "{\"name\":\"Admin Test Department " + UUID.randomUUID() + "\"}",
                adminToken);

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("Admin Test Department"));
    }

    private String registerAndGetToken(String email) throws Exception {
        HttpResponse<String> response = post("/api/auth/register",
                credentialsJson(email, USER_PASSWORD), null);
        assertTokenResponse(response, 201, "USER");
        return accessTokenFrom(response.body());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        HttpResponse<String> response = post("/api/auth/login",
                credentialsJson(email, password), null);
        assertTokenResponse(response, 200, "ADMIN");
        return accessTokenFrom(response.body());
    }

    private HttpResponse<String> post(String path, String jsonBody, String token)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private void assertTokenResponse(HttpResponse<String> response,
            int expectedStatus, String expectedRole) {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        assertTrue(ACCESS_TOKEN_PATTERN.matcher(response.body()).find(),
                "Response should include an access token");
        assertTrue(response.body().contains("\"role\":\"" + expectedRole + "\""),
                "Response should identify role " + expectedRole);
    }

    private String accessTokenFrom(String responseBody) {
        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new AssertionError("Response did not include an access token");
        }
        return matcher.group(1);
    }

    private String credentialsJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }
}
