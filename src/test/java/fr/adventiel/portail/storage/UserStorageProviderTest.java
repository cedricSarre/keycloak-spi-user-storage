package fr.adventiel.portail.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Slf4j
@Testcontainers
class UserStorageProviderTest {

    static final String REALM = "backend";
    static Network network = Network.newNetwork();
    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.4")
            .withRealmImportFile("/realm-export.json")
            .withEnv("KEYCLOAK_USER", "admin")
            .withEnv("KEYCLOAK_PASSWORD", "admin")
            .withProviderClassesFrom("target/classes")
            .withReuse(false)
            .withNetwork(network);

    @Container
    private static final GenericContainer<?> apiMock = new GenericContainer<>("muonsoft/openapi-mock:latest")
            .withExposedPorts(8080)
            .withCopyFileToContainer(MountableFile.forHostPath("./src/test/resources/backendApi.yaml"), "/tmp/spec.yaml")
            .withEnv(new HashMap<>() {{
                put("OPENAPI_MOCK_SPECIFICATION_URL", "/tmp/spec.yaml");
                put("OPENAPI_MOCK_USE_EXAMPLES", "if_present");
            }})
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withReuse(false)
            .withNetwork(network)
            .withNetworkAliases("api");

    @ParameterizedTest
    @ValueSource(strings = {KeycloakContainer.MASTER_REALM, REALM})
    void testRealms(String realm) {
        String accountServiceUrl = given().when()
                .get(keycloak.getAuthServerUrl() + "/realms/" + realm)
                .then()
                .statusCode(200)
                .body("realm", equalTo(realm))
                .extract()
                .path("account-service");

        given().when().get(accountServiceUrl).then().statusCode(200);
    }

    @Test
    void testLoginAsUserAndCheckAccessToken() throws IOException {
        String accessTokenString = requestToken()
                .then()
                .statusCode(200)
                .extract()
                .path("access_token");

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };

        byte[] tokenPayload = Base64.getDecoder().decode(accessTokenString.split("\\.")[1]);
        Map<String, Object> payload = mapper.readValue(tokenPayload, typeRef);

        assertThat(payload.get("preferred_username"), is("john.doe@example.com"));
        assertThat(payload.get("email"), is("john.doe@example.com"));
        assertThat(payload.get("email_verified"), is(true));
    }

    private Response requestToken() {
        String tokenEndpoint = given().when()
                .get(keycloak.getAuthServerUrl() + "/realms/" + REALM + "/.well-known/openid-configuration")
                .then()
                .statusCode(200)
                .extract()
                .path("token_endpoint");

        return given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", "john.doe@example.com")
                .formParam("password", "password")
                .formParam("grant_type", "password")
                .formParam("client_id", KeycloakContainer.ADMIN_CLI_CLIENT)
                .formParam("scope", "openid")
                .when()
                .post(tokenEndpoint);
    }

}
