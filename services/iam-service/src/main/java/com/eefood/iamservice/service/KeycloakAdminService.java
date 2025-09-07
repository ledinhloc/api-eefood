package com.eefood.iamservice.service;

import com.eefood.iamservice.dto.request.Credential;
import com.eefood.iamservice.dto.request.TokenExchangeParam;
import com.eefood.iamservice.dto.request.UserCreationParam;
import com.eefood.iamservice.dto.response.KeycloakTokenResponse;
import com.eefood.iamservice.dto.response.TokenExchangeResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.enums.SuccessMessage;
import com.eefood.iamservice.repository.httpclient.KeycloakClient;
import com.eefood.iamservice.utils.ExceptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {
    private final KeycloakClient keycloakClient;

    @Value("${idp.realm}")
    private String realm;

    @Value("${idp.client-id}")
    private String clientId;

    @Value("${idp.client-secret}")
    private String clientSecret;

    //logout
    public void logout(String refreshToken) {
        TokenExchangeParam param = TokenExchangeParam.builder()
            .client_id(clientId)
            .client_secret(clientSecret)
            .refresh_token(refreshToken)
            .build();
        keycloakClient.logout(realm, param);
    }

    // refresh token
    public TokenExchangeResponse refreshToken(String refreshToken) {
        TokenExchangeParam param = TokenExchangeParam.builder()
            .grant_type("refresh_token")
            .client_id(clientId)
            .client_secret(clientSecret)
            .refresh_token(refreshToken)
            .build();
        return keycloakClient.exchangeToken(realm, param);
    }

    //login
    public TokenExchangeResponse login(String email, String password) {
        TokenExchangeParam param = TokenExchangeParam.builder()
            .grant_type("password")
            .client_id(clientId)
            .client_secret(clientSecret)
            .username(email)
            .password(password)
            .build();
        return keycloakClient.exchangeToken(realm, param);
    }

    // Hàm lấy exchange token
    public String getAdminAccessToken() {
        var token = keycloakClient.exchangeToken(
                realm,
                TokenExchangeParam.builder()
                        .grant_type("client_credentials")
                        .client_id(clientId)
                        .client_secret(clientSecret)
                        .scope("openid")
                        .build());
        if (token == null || token.getAccessToken() == null) {
            throw new RuntimeException("Cannot obtain admin token from Keycloak");
        }
        return token.getAccessToken();
    }

    // Tạo user keycloak
    public ResponseEntity<?> createUserInKeycloak(UserCreationParam param) {
        String token = "Bearer " + getAdminAccessToken();
        return keycloakClient.createUser(token, realm, param);
    }

    // Hàm kích hoạt người dùng
    public boolean enableUserInKeycloak(String userId) {
        String token = "Bearer " + getAdminAccessToken();
        Map<String, Object> userRep = keycloakClient.getUserById(token, realm, userId);
        if (userRep == null) {
            log.warn("Keycloak: user representation null for id={}", userId);
            return false;
        }

        userRep.put("enabled", true);
        userRep.put("emailVerified", true);

        ResponseEntity<Void> updateResp = keycloakClient.updateUser(token, realm, userId, userRep);
        return updateResp != null && updateResp.getStatusCode().is2xxSuccessful();
    }

    // Hàm reset mật khẩu
    public void resetPassword(String userId, String newPassword) {
        String token = "Bearer " + getAdminAccessToken();
        Credential payload = Credential.builder()
                .type("password")
                .temporary(false)
                .value(newPassword)
                .build();

        ResponseEntity<Void> resp = keycloakClient.resetPassword(token, realm, userId, payload);

        if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to reset password for userId={} status={}", userId,
                    resp != null ? resp.getStatusCode() : "null response");
            throw ExceptionUtil.badRequest(ErrorMessage.FAIL_RESET_PASSWORD);
        }

        log.info(SuccessMessage.PASSWORD_RESET_SUCCESS.getMessage()+" for userId={}", userId);
    }

    // Tìm user id theo email
    public Optional<String> findUserIdByEmail(String email) {
        String token = "Bearer " + getAdminAccessToken();
        List<Map<String, Object>> list = keycloakClient.findUsersByEmail(token, realm, email);
        if (list == null || list.isEmpty()) return Optional.empty();
        Object id = list.get(0).get("id");
        return Optional.ofNullable(id != null ? id.toString() : null);
    }


    // Cập nhật user trong Keycloak
    public void updateUserInKeycloak(String userId, Map<String, Object> fields) {
        String token = "Bearer " + getAdminAccessToken();
        ResponseEntity<Void> updateResp = keycloakClient.updateUser(token, realm, userId, fields);
        if (updateResp == null || !updateResp.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to update user={} in Keycloak", userId);
            throw ExceptionUtil.badRequest(ErrorMessage.FAIL_UPDATE_USER);
        }
        log.info("Updated user={} successfully in Keycloak", userId);
    }

    //Vô hiệu hóa user trong keycloak
    public void disableUserInKeycloak(String userId) {
        String token = "Bearer " + getAdminAccessToken();
        Map<String, Object> userRep = keycloakClient.getUserById(token, realm, userId);

        if(userRep == null) {
            log.warn("Keycloak: user representation null for id={}", userId);
            return;
        }

        userRep.put("enabled", false);
        ResponseEntity<Void> updateResp = keycloakClient.updateUser(token, realm, userId, userRep);

        if(updateResp == null || !updateResp.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to update user={} in Keycloak", userId);
            throw ExceptionUtil.badRequest(ErrorMessage.FAIL_DELETE_USER);
        }
        log.info("Disabled user={} successfully in Keycloak", userId);
    }

    // Gán 1 realm role (roleName) cho user (userId)
    public void assignRealmRoleToUser(String userId, String roleName) {
        String token = "Bearer " + getAdminAccessToken();

        // Lấy role representation
        Map<String, Object> roleRep = keycloakClient.getRoleByName(token, realm, roleName);
        if (roleRep == null) {
            log.error("Keycloak: role not found name={}", roleName);
            throw ExceptionUtil.badRequest(ErrorMessage.FAIL_UPDATE_ROLE);
        }

        List<Map<String, Object>> roles = new ArrayList<>();
        roles.add(roleRep);

        ResponseEntity<Void> resp = keycloakClient.addRealmRoleMappingsToUser(token, realm, userId, roles);
        if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to add realm role={} to user={} status={}", roleName, userId,
                    resp != null ? resp.getStatusCode() : "null response");
            throw ExceptionUtil.badRequest(ErrorMessage.FAIL_UPDATE_ROLE);
        }

        log.info("Assigned realm role={} to userId={}", roleName, userId);
    }

    // Bỏ 1 realm role (roleName) khỏi user (userId)
    public void removeRealmRoleFromUser(String userId, String roleName) {
        String token = "Bearer " + getAdminAccessToken();

        // Lấy role representation
        Map<String, Object> roleRep = keycloakClient.getRoleByName(token, realm, roleName);
        if (roleRep == null) {
            log.warn("Keycloak: role not found name={}", roleName);
            // Nếu role không tồn tại trên KC, coi như đã "bỏ" — không ném exception cứng
            return;
        }

        List<Map<String, Object>> roles = new ArrayList<>();
        roles.add(roleRep);

        ResponseEntity<Void> resp = keycloakClient.deleteRealmRoleMappingsFromUser(token, realm, userId, roles);
        if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to remove realm role={} from user={} status={}", roleName, userId,
                    resp != null ? resp.getStatusCode() : "null response");
            throw ExceptionUtil.badRequest(ErrorMessage.FAIL_UPDATE_ROLE);
        }

        log.info("Removed realm role={} from userId={}", roleName, userId);
    }

}
