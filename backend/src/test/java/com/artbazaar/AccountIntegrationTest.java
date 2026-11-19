package com.artbazaar;

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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountIntegrationTest {

    @Autowired MockMvc mvc;

    private record Account(String email, String token, String refresh) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private static String refreshCookie(MvcResult r) {
        Matcher m = Pattern.compile("ab_refresh=([^;]*)").matcher(r.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        return m.find() ? m.group(1) : null;
    }

    private Account signUp(String type) throws Exception {
        String email = type.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Old Name\",\"email\":\"%s\",\"password\":\"Secret123\",\"accountType\":\"%s\",\"city\":\"Agra\",\"bio\":\"Marble inlay\"}"
                                .formatted(email, type)))
                .andExpect(status().isCreated()).andReturn();
        return new Account(email, JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken"), refreshCookie(r));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private ResultActions changePassword(Account a, String current, String next) throws Exception {
        return mvc.perform(post("/api/account/password").header(HttpHeaders.AUTHORIZATION, a.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next)));
    }

    @Test
    void artistUpdatesProfile() throws Exception {
        Account artist = signUp("ARTIST");
        mvc.perform(get("/api/account").header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(jsonPath("$.city").value("Agra"))
                .andExpect(jsonPath("$.bio").value("Marble inlay"))
                .andExpect(jsonPath("$.artistApproved").value(false));

        mvc.perform(put("/api/account").header(HttpHeaders.AUTHORIZATION, artist.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\",\"bio\":\"Pietra dura since 2010\",\"city\":\"Jaipur\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.city").value("Jaipur"));
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void buyerProfileHasNoArtistFields() throws Exception {
        Account buyer = signUp("BUYER");
        mvc.perform(put("/api/account").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Buyer B\",\"bio\":\"ignored\"}"))
                .andExpect(jsonPath("$.name").value("Buyer B"))
                .andExpect(jsonPath("$.bio").doesNotExist());
        mvc.perform(put("/api/account").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
    }

    @Test
    void changingPasswordSignsOutOtherSessions() throws Exception {
        Account me = signUp("BUYER");

        changePassword(me, "WrongOld1", "BrandNew123").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your current password is incorrect"));
        changePassword(me, "Secret123", "Secret123").andExpect(status().isBadRequest());
        changePassword(me, "Secret123", "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());

        MvcResult ok = changePassword(me, "Secret123", "BrandNew123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        String newRefresh = refreshCookie(ok);

        // the old session's refresh token is dead, the new one works
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", me.refresh()))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("ab_refresh", newRefresh))).andExpect(status().isOk());

        login(me.email(), "Secret123").andExpect(status().isUnauthorized());
        login(me.email(), "BrandNew123").andExpect(status().isOk());
    }

    @Test
    void repeatedFailedLoginsAreThrottled() throws Exception {
        Account me = signUp("BUYER");
        for (int i = 0; i < 5; i++) {
            login(me.email(), "Wrong" + i + "pass").andExpect(status().isUnauthorized());
        }
        // even the right password is refused while throttled
        login(me.email(), "Secret123")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message", containsString("Too many failed sign-in attempts")));

        // other accounts are unaffected
        Account other = signUp("BUYER");
        login(other.email(), "Secret123").andExpect(status().isOk());
    }

    @Test
    void successfulLoginResetsTheCounter() throws Exception {
        Account me = signUp("BUYER");
        for (int i = 0; i < 4; i++) login(me.email(), "Nope" + i + "nope");
        login(me.email(), "Secret123").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) login(me.email(), "Nope" + i + "nope").andExpect(status().isUnauthorized());
        login(me.email(), "Secret123").andExpect(status().isOk());
    }
}
