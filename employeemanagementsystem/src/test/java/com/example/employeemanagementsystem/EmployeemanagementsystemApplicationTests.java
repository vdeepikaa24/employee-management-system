package com.example.employeemanagementsystem;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.security.jwt.secret=ci-test-secret-with-at-least-32-characters",
        "app.security.bootstrap-admin.username=ci-admin@example.com",
        "app.security.bootstrap-admin.password=CiAdminPassword123!"
})
@ActiveProfiles("test")
class EmployeemanagementsystemApplicationTests {

    @Test
    void contextLoads() {
    }
}
