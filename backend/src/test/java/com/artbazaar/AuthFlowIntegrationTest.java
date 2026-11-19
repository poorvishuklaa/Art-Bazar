package com.artbazaar;

import com.artbazaar.entity.RefreshToken;
import com.artbazaar.repository.RefreshTokenRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final Pattern REFRESH_COOKIE = Pattern.compile("ab_refresh=([^;]*)");

    @Autowired
    MockMvc mvc;

    @Autowired
    RefreshTokenRepository refreshTokens;

    /** Moves a revoked token's revocation time into the past, beyond the concurrent-refresh grace window. */
    private void ageRevocation(String rawToken) {
        RefreshToken t = refreshTokens.findByTokenHash(sha256(rawToken)).orElseThrow();
        t.setRevokedAt(Instant.now().minus(Duration.ofMinutes(5)));
        refreshTokens.save(t);
    }

    private static String sha256(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- helpers ----------

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private MvcResult register(String name, String email, String accountType) throws Exception {
        String body = """
                {"name":"%s","email":"%s","password":"Secret123","accountType":"%s","city":"Indore"}
                """.formatted(name, email, accountType);
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String accessToken(MvcResult r) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String refreshCookie(MvcResult r) {
        Matcher m = REFRESH_COOKIE.matcher(r.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- tests ----------

    @Test
    void registerBuyerReturnsTokenAndSecureCookie() throws Exception {
        String email = uniqueEmail("buyer");
        MvcResult r = register("Asha", email.toUpperCase(), "BUYER");

        String json = r.getResponse().getContentAsString();
        assertThat((String) JsonPath.read(json, "$.user.email")).isEqualTo(email); // normalised to lower case
        assertThat((String) JsonPath.read(json, "$.user.role")).isEqualTo("BUYER");
        assertThat(json).doesNotContain("artistApproved").doesNotContain("password");

        String setCookie = r.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api/auth");

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken(r))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail("dup");
        register("First", email, "BUYER");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second\",\"email\":\"%s\",\"password\":\"Secret123\",\"accountType\":\"BUYER\"}"
                                .formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.accountType").exists());
    }

    @Test
    void cannotSelfRegisterAsAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eve\",\"email\":\"%s\",\"password\":\"Secret123\",\"accountType\":\"ADMIN\"}"
                                .formatted(uniqueEmail("eve"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() throws Exception {
        String email = uniqueEmail("login");
        register("Ravi", email, "BUYER");

        for (String[] creds : new String[][]{{email, "WrongPass1"}, {uniqueEmail("ghost"), "Secret123"}}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(creds[0], creds[1])))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
        login(email, "Secret123");
    }

    @Test
    void tamperedOrMissingTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer("not.a.jwt")))
                .andExpect(status().isUnauthorized());

        String token = accessToken(register("Tamper", uniqueEmail("tamper"), "BUYER"));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndReuseRevokesAllSessions() throws Exception {
        String email = uniqueEmail("rotate");
        MvcResult reg = register("Meera", email, "BUYER");
        String first = refreshCookie(reg);

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        String second = refreshCookie(refreshed);
        assertThat(second).isNotEqualTo(first);

        // Replaying the old token later looks like theft: it fails and kills the newer session too.
        ageRevocation(first);
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", first)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", second)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshWithinGraceDoesNotKillTheSession() throws Exception {
        String first = refreshCookie(register("Tabs", uniqueEmail("tabs"), "BUYER"));
        String second = refreshCookie(mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", first)))
                .andExpect(status().isOk()).andReturn());

        // A second tab raced with the same cookie: rejected with a retryable code, and the new session survives.
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", first)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_RACE"));
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", second)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshWithoutCookieIs401() throws Exception {
        mvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        String cookie = refreshCookie(register("Out", uniqueEmail("logout"), "BUYER"));

        mvc.perform(post("/api/auth/logout").cookie(new Cookie("ab_refresh", cookie)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", cookie)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void artistNeedsAdminApproval() throws Exception {
        String artistEmail = uniqueEmail("artist");
        MvcResult reg = register("Kavya", artistEmail, "ARTIST");
        String artistToken = accessToken(reg);
        long artistId = ((Number) JsonPath.read(reg.getResponse().getContentAsString(), "$.user.id")).longValue();

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(artistToken)))
                .andExpect(jsonPath("$.role").value("ARTIST"))
                .andExpect(jsonPath("$.artistApproved").value(false));

        // Artists and buyers cannot use admin endpoints.
        mvc.perform(get("/api/admin/artists").header(HttpHeaders.AUTHORIZATION, bearer(artistToken)))
                .andExpect(status().isForbidden());

        String adminToken = accessToken(login("admin@test.local", "AdminPass123"));
        mvc.perform(get("/api/admin/artists").param("approved", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(artistEmail)));

        mvc.perform(patch("/api/admin/artists/{id}/approval", artistId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));

        // Takes effect on the artist's existing token, no re-login needed.
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(artistToken)))
                .andExpect(jsonPath("$.artistApproved").value(true));

        mvc.perform(get("/api/admin/artists").param("approved", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$[*].email", not(hasItem(artistEmail))));
    }

    @Test
    void approvingUnknownArtistIs404() throws Exception {
        String adminToken = accessToken(login("admin@test.local", "AdminPass123"));
        mvc.perform(patch("/api/admin/artists/{id}/approval", 999_999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isNotFound());
    }
}
