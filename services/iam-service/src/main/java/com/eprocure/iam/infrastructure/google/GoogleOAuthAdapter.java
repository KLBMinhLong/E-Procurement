package com.eprocure.iam.infrastructure.google;

import com.eprocure.iam.application.port.out.GoogleOAuthPort;
import com.eprocure.iam.application.port.out.GoogleOAuthProfile;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuthAdapter implements GoogleOAuthPort {
    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthAdapter.class);
    private final RestClient restClient;
    private final String authorizationUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;

    public GoogleOAuthAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${eprocure.google-oauth.authorization-url:https://accounts.google.com/o/oauth2/v2/auth}") String authorizationUrl,
            @Value("${eprocure.google-oauth.token-url:https://oauth2.googleapis.com/token}") String tokenUrl,
            @Value("${eprocure.google-oauth.user-info-url:https://www.googleapis.com/oauth2/v3/userinfo}") String userInfoUrl,
            @Value("${eprocure.google-oauth.client-id:}") String clientId,
            @Value("${eprocure.google-oauth.client-secret:}") String clientSecret,
            @Value("${eprocure.google-oauth.redirect-uri:http://localhost:8081/api/v1/auth/oauth/google/callback}") String redirectUri,
            @Value("${eprocure.google-oauth.scope:openid email profile}") String scope) {
        this.restClient = restClientBuilder.build();
        this.authorizationUrl = authorizationUrl;
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    @Override
    public String authorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(authorizationUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "select_account")
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public Optional<GoogleOAuthProfile> fetchProfile(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            return Optional.empty();
        }
        try {
            GoogleTokenResponse token = exchangeCode(authorizationCode);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                return Optional.empty();
            }
            GoogleUserInfoResponse userInfo = fetchUserInfo(token.accessToken());
            if (userInfo == null || userInfo.sub() == null || userInfo.email() == null) {
                return Optional.empty();
            }
            return Optional.of(new GoogleOAuthProfile(
                    userInfo.sub(),
                    userInfo.email().toLowerCase(Locale.ROOT),
                    userInfo.emailVerified(),
                    userInfo.name(),
                    userInfo.picture()));
        } catch (RestClientResponseException exception) {
            log.warn("[SECURITY] Google OAuth verification failed | status={}", exception.getStatusCode().value());
            return Optional.empty();
        } catch (RestClientException exception) {
            log.warn("[SECURITY] Google OAuth verification failed | provider_unavailable=true");
            return Optional.empty();
        } catch (IllegalArgumentException exception) {
            log.warn("[SECURITY] Google OAuth verification failed | invalid_response=true");
            return Optional.empty();
        }
    }

    private GoogleTokenResponse exchangeCode(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        return restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    private GoogleUserInfoResponse fetchUserInfo(String accessToken) {
        return restClient.get()
                .uri(userInfoUrl)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(GoogleUserInfoResponse.class);
    }

    private record GoogleTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record GoogleUserInfoResponse(
            String sub,
            String email,
            @JsonProperty("email_verified") boolean emailVerified,
            String name,
            String picture) {
    }
}
