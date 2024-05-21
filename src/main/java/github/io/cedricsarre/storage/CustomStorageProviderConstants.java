package github.io.cedricsarre.storage;

public class CustomStorageProviderConstants {

    static final String CONFIG_KEY_PROVIDER_ID = "CustomUserProvider";
    static final String CONFIG_KEY_BASE_URL = "Backend URL";
    static final String CONFIG_KEY_DEFAULT_TIMEOUT_LABEL = "Default Timeout Value for user creation";

    /**
     * Attribut de l'utilisateur à récupérer depuis le backend et à stocker (peut permettre de faire le lien entre Keycloak et le backend en cas de problème sur ce compte)
     */
    static final String USER_ATTRIBUTE_ID = "user_id";
    static final String GET_USER_URL = "/user";

    static final String CONFIG_KEY_FORCE_PWD_RESET = "Force Reset Password at first authentication";

    private CustomStorageProviderConstants() {
        // private constructor to hide the public one
    }
}
