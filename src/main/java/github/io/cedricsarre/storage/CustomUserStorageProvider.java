package github.io.cedricsarre.storage;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import static github.io.cedricsarre.storage.CustomStorageProviderConstants.USER_ATTRIBUTE_ID;

public class CustomUserStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator {
    private final KeycloakSession session;
    private final ComponentModel model;
    private final CustomUserService userService;
    private final boolean resetPwd;
    
    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model, CustomUserService userService, boolean resetPwd) {
        this.session = session;
        this.model = model;
        this.userService = userService;
        this.resetPwd = resetPwd;
    }

    @Override
    public void close() {
        //
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(realm, username);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return createAdapter(realm, username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return getUserByUsername(realm, email);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        // On vérifie si l'utilisateur existe déjà dans le stockage local de Keycloak
        UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, user.getUsername());

        // Si l'utilisateur existe mais qu'il ne contient pas l'attribut attendu, on doit appelé le backend
        if (localUser != null && localUser.getAttributes().get(USER_ATTRIBUTE_ID) == null) {

            // On appelle le backend pour récupérer l'utilisateur
            JsonNode retrievedUser = userService.getUserByUserName(user.getUsername(), credentialInput.getChallengeResponse(), model);

            // Si l'utilisateur a bien été récupéré, on lui ajoute l'attribut
            if (retrievedUser != null && !retrievedUser.isNull() && !retrievedUser.isEmpty()) {
                user.setSingleAttribute(USER_ATTRIBUTE_ID, retrievedUser.get("UserId").textValue());

                // Une fois l'attribut récupéré, on doit supprimer le lien entre notre UserStorageProvider et Keycloak, ce qui permettra à Keycloak de vérifier dans sa base de données lors de l'authentification au lieu de demander au Backend.
                user.setFederationLink(null);

                if (resetPwd) {
                    // On force la réinitialisation du mot de passe lors de la première authentification de l'utilisateur
                    user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                }
                return true;
            }
            // Si l'utilisateur n'existe pas ou n'a pas pu être retrouvé dans le backend, on doit en supprimer toutes traces dans Keycloak
            UserStoragePrivateUtil.userLocalStorage(session).removeUser(realm, user);
        }
        return false;
    }

    protected UserModel createAdapter(RealmModel realm, String username) {
        // On récupère l'utilisateur depuis le stockage local de Keycloak
        UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, username);

        // Si l'utilisateur n'existe pas dans le stockage local de Keycloak, on doit l'initialiser
        if (localUser == null) {
            localUser = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, username);
            localUser.setEmail(username);
            localUser.setFederationLink(model.getId());
            localUser.setEmailVerified(true);
            localUser.setEnabled(true);
        }

        // On crée un nouvel Adapter (basé sur la classe AbstractUserAdapter)
        return new UserModelDelegate(localUser) {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public SubjectCredentialManager credentialManager() {
                return new UserCredentialManager(session, realm, this) {
                };
            }
        };
    }
}
