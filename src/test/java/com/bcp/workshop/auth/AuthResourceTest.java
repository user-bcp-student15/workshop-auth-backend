package com.bcp.workshop.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("expiresIn", is(3600))
                .body("token", notNullValue());

        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
        try {
            SignedJWT signed = SignedJWT.parse(token);
            assertEquals(JWSAlgorithm.HS256, signed.getHeader().getAlgorithm());
            assertTrue(signed.verify(new MACVerifier("workshop-test-secret-32-bytes-minimum-value".getBytes(StandardCharsets.UTF_8))));
            assertEquals("student", signed.getJWTClaimsSet().getSubject());
            assertEquals("STUDENT", signed.getJWTClaimsSet().getStringClaim("role"));
            Instant issuedAt = signed.getJWTClaimsSet().getIssueTime().toInstant();
            Instant expiresAt = signed.getJWTClaimsSet().getExpirationTime().toInstant();
            assertTrue(expiresAt.isAfter(Instant.now()));
            assertEquals(3600, expiresAt.getEpochSecond() - issuedAt.getEpochSecond());
        } catch (Exception error) {
            throw new AssertionError("Login must issue a valid signed token", error);
        }
    }

    @Test
    void treatsSqlSyntaxInCredentialsAsLiteralValues() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student' -- \",\"password\":\"wrong\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void rejectsNullLoginRequest() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsBlankUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"  \",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsBlankPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"  \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsTokenSecretBelowMinimumLength() {
        LegacyTokenUtil tokenUtil = new LegacyTokenUtil();
        tokenUtil.secret = "too-short";
        assertThrows(IllegalStateException.class, () -> tokenUtil.create("student", "STUDENT"));
    }

    @Test
    void returnsServerErrorWhenTokenIssuanceFails() {
        AuthResource resource = new AuthResource();
        resource.database = new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return new UserRecord(1, username, "Workshop Student", "STUDENT");
            }
        };
        LegacyTokenUtil tokenUtil = new LegacyTokenUtil();
        tokenUtil.secret = "too-short";
        resource.tokenUtil = tokenUtil;
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";

        Response response = resource.login(request);
        assertEquals(500, response.getStatus());
        response.close();
    }
}
