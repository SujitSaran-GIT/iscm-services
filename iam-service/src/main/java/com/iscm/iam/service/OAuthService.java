package com.iscm.iam.service;

import com.iscm.iam.dto.AuthResponse;
import com.iscm.iam.model.OAuthAccount;
import com.iscm.iam.model.Organization;
import com.iscm.iam.model.Role;
import com.iscm.iam.model.User;
import com.iscm.iam.model.UserRole;
import com.iscm.iam.repository.OAuthAccountRepository;
import com.iscm.iam.repository.OrganizationRepository;
import com.iscm.iam.repository.RoleRepository;
import com.iscm.iam.repository.UserRepository;
import com.iscm.iam.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final UserSessionService sessionService;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${app.oauth.google.client-id}")
    private String googleClientId;

    @Value("${app.oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${app.oauth.microsoft.client-id}")
    private String microsoftClientId;

    @Value("${app.oauth.microsoft.client-secret}")
    private String microsoftClientSecret;

    @Value("${app.oauth.linkedin.client-id}")
    private String linkedinClientId;

    @Value("${app.oauth.linkedin.client-secret}")
    private String linkedinClientSecret;

    @Transactional
    public String getOAuthUrl(String provider, String redirectUri) {
        return switch (provider.toLowerCase()) {
            case "google" -> getGoogleOAuthUrl(redirectUri);
            case "microsoft" -> getMicrosoftOAuthUrl(redirectUri);
            case "linkedin" -> getLinkedInOAuthUrl(redirectUri);
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        };
    }

    @Transactional
    public AuthResponse handleOAuthCallback(String provider, String code, String redirectUri, String userAgent, String ipAddress) {
        OAuthUserInfo userInfo = switch (provider.toLowerCase()) {
            case "google" -> handleGoogleOAuthCallback(code, redirectUri);
            case "microsoft" -> handleMicrosoftOAuthCallback(code, redirectUri);
            case "linkedin" -> handleLinkedInOAuthCallback(code, redirectUri);
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        };

        return processOAuthUser(userInfo, provider, userAgent, ipAddress);
    }

    private AuthResponse processOAuthUser(OAuthUserInfo userInfo, String provider, String userAgent, String ipAddress) {
        // Check if user exists with this email
        Optional<User> existingUser = userRepository.findByEmail(userInfo.email());

        User user;
        boolean isNewUser = false;

        if (existingUser.isPresent()) {
            user = existingUser.get();

            // Check if OAuth account is linked
            Optional<OAuthAccount> existingOAuthAccount = oAuthAccountRepository
                    .findByUserIdAndProvider(user.getId(), provider);

            if (existingOAuthAccount.isPresent()) {
                // Update OAuth account info
                OAuthAccount oauthAccount = existingOAuthAccount.get();
                oauthAccount.setProviderId(userInfo.providerId());
                oauthAccount.setProviderUsername(userInfo.username());
                oauthAccount.setAccessToken(userInfo.accessToken());
                oauthAccount.setRefreshToken(userInfo.refreshToken());
                oauthAccount.setTokenExpiry(userInfo.tokenExpiry());
                oAuthAccountRepository.save(oauthAccount);
            } else {
                // Link OAuth account to existing user
                linkOAuthAccount(user, provider, userInfo);
            }
        } else {
            // Create new user
            user = createOAuthUser(userInfo, provider);
            isNewUser = true;
        }

        // Generate tokens
        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        String accessToken = jwtUtil.generateAccessToken(
            user.getId(), user.getEmail(), roles, user.getTenantId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Create session
        sessionService.createSession(user, refreshToken, userAgent, ipAddress);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .user(AuthResponse.UserDto.fromEntity(user))
                .isNewUser(isNewUser)
                .build();
    }

    private User createOAuthUser(OAuthUserInfo userInfo, String provider) {
        User user = new User();
        user.setEmail(userInfo.email());
        user.setFirstName(userInfo.firstName());
        user.setLastName(userInfo.lastName());
        user.setAuthProvider(provider.toUpperCase());
        user.setIsActive(true);
        user.setTenantId(null); // Can be set later if needed

        // Get default role
        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));

        User savedUser = userRepository.save(user);

        // Create UserRole relationship
        UserRole userRole = new UserRole();
        userRole.setUser(savedUser);
        userRole.setRole(defaultRole);
        userRole.setAssignedAt(LocalDateTime.now());

        // Link OAuth account
        linkOAuthAccount(savedUser, provider, userInfo);

        return savedUser;
    }

    private void linkOAuthAccount(User user, String provider, OAuthUserInfo userInfo) {
        OAuthAccount oauthAccount = new OAuthAccount();
        oauthAccount.setUser(user);
        oauthAccount.setProvider(OAuthAccount.OAuthProvider.valueOf(provider.toUpperCase()));
        oauthAccount.setProviderId(userInfo.providerId());
        oauthAccount.setProviderUsername(userInfo.username());
        oauthAccount.setEmail(userInfo.email());
        oauthAccount.setAccessToken(userInfo.accessToken());
        oauthAccount.setRefreshToken(userInfo.refreshToken());
        oauthAccount.setTokenExpiry(userInfo.tokenExpiry());
        oauthAccount.setScopes(userInfo.scopes());

        oAuthAccountRepository.save(oauthAccount);
    }

    @Transactional
    public void unlinkOAuthAccount(UUID userId, String provider) {
        OAuthAccount oauthAccount = oAuthAccountRepository
                .findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new IllegalArgumentException("OAuth account not found"));

        oAuthAccountRepository.delete(oauthAccount);
        log.info("Unlinked OAuth account for user: {}, provider: {}", userId, provider);
    }

    public List<OAuthAccount> getUserOAuthAccounts(UUID userId) {
        return oAuthAccountRepository.findByUserId(userId);
    }

    // Provider-specific implementations
    private String getGoogleOAuthUrl(String redirectUri) {
        return String.format(
            "https://accounts.google.com/o/oauth2/v2/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=email profile",
            googleClientId, redirectUri
        );
    }

    private OAuthUserInfo handleGoogleOAuthCallback(String code, String redirectUri) {
        // Exchange code for tokens
        String tokenUrl = "https://oauth2.googleapis.com/token";
        Map<String, String> params = Map.of(
            "client_id", googleClientId,
            "client_secret", googleClientSecret,
            "code", code,
            "redirect_uri", redirectUri,
            "grant_type", "authorization_code"
        );

        Map<String, Object> tokenResponse = restTemplate.postForObject(tokenUrl, params, Map.class);
        String accessToken = (String) tokenResponse.get("access_token");

        // Get user info
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
        Map<String, Object> userInfo = restTemplate.getForObject(
            userInfoUrl + "?access_token=" + accessToken, Map.class);

        return new OAuthUserInfo(
            (String) userInfo.get("id"),
            (String) userInfo.get("email"),
            (String) userInfo.get("given_name"),
            (String) userInfo.get("family_name"),
            (String) userInfo.get("name"),
            accessToken,
            (String) tokenResponse.get("refresh_token"),
            LocalDateTime.now().plusSeconds(((Number) tokenResponse.getOrDefault("expires_in", 3600)).longValue()),
            "email profile"
        );
    }

    private String getMicrosoftOAuthUrl(String redirectUri) {
        return String.format(
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=openid email profile",
            microsoftClientId, redirectUri
        );
    }

    private OAuthUserInfo handleMicrosoftOAuthCallback(String code, String redirectUri) {
        // Microsoft OAuth implementation
        // Similar to Google but with Microsoft endpoints
        return null; // Implementation would follow similar pattern
    }

    private String getLinkedInOAuthUrl(String redirectUri) {
        return String.format(
            "https://www.linkedin.com/oauth/v2/authorization?client_id=%s&redirect_uri=%s&response_type=code&scope=r_liteprofile r_emailaddress",
            linkedinClientId, redirectUri
        );
    }

    private OAuthUserInfo handleLinkedInOAuthCallback(String code, String redirectUri) {
        // LinkedIn OAuth implementation
        // Similar to Google but with LinkedIn endpoints
        return null; // Implementation would follow similar pattern
    }

    private record OAuthUserInfo(
        String providerId,
        String email,
        String firstName,
        String lastName,
        String username,
        String accessToken,
        String refreshToken,
        LocalDateTime tokenExpiry,
        String scopes
    ) {}
}