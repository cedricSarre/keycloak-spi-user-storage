package github.io.cedricsarre.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.utils.StringUtil;

import java.util.List;

import static github.io.cedricsarre.storage.CustomStorageProviderConstants.CONFIG_KEY_FORCE_PWD_RESET;

public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {

    @Override
    public String getId() {
        return CustomStorageProviderConstants.CONFIG_KEY_PROVIDER_ID;
    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        boolean resetPwd = model.get(CONFIG_KEY_FORCE_PWD_RESET, true);
        return new CustomUserStorageProvider(session, model, new CustomUserService(session), resetPwd);
    }

    @Override
    public String getHelpText() {
        return "Backend User Provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property(
                        CustomStorageProviderConstants.CONFIG_KEY_BASE_URL,
                        "Backend URL",
                        "The backend URL. Ex: http://localhost:8181",
                        ProviderConfigProperty.STRING_TYPE,
                        "http://localhost:8181",
                        null)
                .property(
                        CONFIG_KEY_FORCE_PWD_RESET,
                        "Reset Password (Keep enabled!)",
                        "Force the user to reset his password at its first authentication",
                        ProviderConfigProperty.BOOLEAN_TYPE,
                        true,
                        null)
                .build();
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) {
        if (StringUtil.isBlank(config.get(CustomStorageProviderConstants.CONFIG_KEY_BASE_URL))) {
            throw new ComponentValidationException("Configuration not properly set, please verify.");
        }
    }
}
