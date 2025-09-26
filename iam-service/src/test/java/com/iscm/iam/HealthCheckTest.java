package com.iscm.iam;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthCheckTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/iam";
    }

    @Test
    void testHealthEndpoint() {
        given()
        .when()
            .get("/actuator/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
            .body("components.db.status", equalTo("UP"))
            .body("components.diskSpace.status", equalTo("UP"));
    }

    @Test
    void testInfoEndpoint() {
        given()
        .when()
            .get("/actuator/info")
        .then()
            .statusCode(200);
    }

    @Test
    void testMetricsEndpoint() {
        given()
        .when()
            .get("/actuator/metrics")
        .then()
            .statusCode(200);
    }
}