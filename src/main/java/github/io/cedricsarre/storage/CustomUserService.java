package github.io.cedricsarre.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

import static github.io.cedricsarre.storage.CustomStorageProviderConstants.*;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static java.lang.Integer.parseInt;
import static org.keycloak.broker.provider.util.SimpleHttp.doPost;

public class CustomUserService {

    private static final Logger LOG = Logger.getLogger(CustomUserService.class);

    KeycloakSession session;

    public CustomUserService(KeycloakSession session) {
        this.session = session;
    }

    JsonNode getUserByUserName(String username, String password, ComponentModel model) {
        try {
            SimpleHttp simpleHttp = doPost(model.get(CONFIG_KEY_BASE_URL) + GET_USER_URL, this.session);

            simpleHttp.socketTimeOutMillis(parseInt(model.get(CONFIG_KEY_DEFAULT_TIMEOUT_LABEL)));
            simpleHttp.connectionRequestTimeoutMillis(parseInt(model.get(CONFIG_KEY_DEFAULT_TIMEOUT_LABEL)));
            simpleHttp.connectTimeoutMillis(parseInt(model.get(CONFIG_KEY_DEFAULT_TIMEOUT_LABEL)));

            return simpleHttp.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .json(createRequestBody(username, password))
                    .asJson();
        } catch (IOException e) {
            LOG.warn("Error fetching user " + username + " from external service " + CustomStorageProviderConstants.CONFIG_KEY_PROVIDER_ID + " : " + e.getMessage(), e);
        }
        return null;
    }

    private ObjectNode createRequestBody(String username, String password) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("username", username);
        rootNode.put("userPassword", password);
        return rootNode;
    }
}
