package com.artbazaar;

import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.CategoryRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ArtistProfileRepository artistProfiles;
    @Autowired CategoryRepository categories;

    private String admin;

    private record Account(long id, String email, String token, String refreshCookie) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private static byte[] jpeg() {
        byte[] b = new byte[32];
        Arrays.fill(b, (byte) 1);
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        return b;
    }

    @BeforeEach
    void signInAdmin() throws Exception {
        admin = "Bearer " + JsonPath.read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test.local\",\"password\":\"AdminPass123\"}"))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private Account signUp(String type, String name) throws Exception {
        String email = type.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"Secret123\",\"accountType\":\"%s\"}"
                                .formatted(name, email, type)))
                .andExpect(status().isCreated()).andReturn();
        String json = r.getResponse().getContentAsString();
        Matcher m = Pattern.compile("ab_refresh=([^;]*)").matcher(r.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        m.find();
        return new Account(((Number) JsonPath.read(json, "$.user.id")).longValue(), email,
                JsonPath.read(json, "$.accessToken"), m.group(1));
    }

    @Test
    void onlyAdminsGetIn() throws Exception {
        Account buyer = signUp("BUYER", "Curious Buyer");
        for (String path : new String[]{"/api/admin/stats", "/api/admin/users", "/api/admin/artworks",
                "/api/admin/categories", "/api/admin/orders"}) {
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, buyer.bearer())).andExpect(status().isForbidden());
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isOk());
        }
    }

    @Test
    void statsCoverThePlatform() throws Exception {
        signUp("ARTIST", "Pending Painter");
        mvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.buyers").isNumber())
                .andExpect(jsonPath("$.pendingArtists", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.listedArtworks", greaterThanOrEqualTo(18)))
                .andExpect(jsonPath("$.gmv").isNumber());
    }

    @Test
    void searchesUsersAndDisablingLocksTheAccountOutImmediately() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        Account buyer = signUp("BUYER", "Zara " + tag);

        mvc.perform(get("/api/admin/users").param("q", "zara " + tag).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(buyer.email()));
        mvc.perform(get("/api/admin/users").param("role", "ARTIST").param("size", "50").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.content[*].role", everyItem(is("ARTIST"))));
        mvc.perform(get("/api/admin/users").param("q", "%").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.totalElements").value(0)); // wildcards are literal

        mvc.perform(patch("/api/admin/users/{id}/status", buyer.id()).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));

        // existing access token, refresh cookie and password all stop working
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, buyer.bearer())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("ab_refresh", buyer.refreshCookie())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(buyer.email())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", startsWith("This account has been disabled")));

        mvc.perform(patch("/api/admin/users/{id}/status", buyer.id()).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(buyer.email())))
                .andExpect(status().isOk());
    }

    @Test
    void adminsCannotBeDisabled() throws Exception {
        long adminId = ((Number) JsonPath.read(mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, admin))
                .andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        mvc.perform(patch("/api/admin/users/{id}/status", adminId).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void takeDownAndRestoreAnArtwork() throws Exception {
        Account artist = signUp("ARTIST", "Moderated Artist");
        ArtistProfile p = artistProfiles.findByUserId(artist.id()).orElseThrow();
        p.setApproved(true);
        artistProfiles.save(p);
        long id = ((Number) JsonPath.read(mvc.perform(multipart("/api/artist/artworks")
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", jpeg()))
                        .param("title", "Questionable Piece").param("price", "100").param("stock", "1")
                        .param("categoryId", String.valueOf(categories.findBySlug("craft").orElseThrow().getId()))
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andReturn().getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(patch("/api/admin/artworks/{id}/status", id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REMOVED\"}"))
                .andExpect(jsonPath("$.status").value("REMOVED"));
        mvc.perform(get("/api/artworks/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/artworks").param("size", "50").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) id))));
        mvc.perform(get("/api/admin/artworks").param("status", "REMOVED").param("size", "50").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) id)));
        // the artist can no longer touch a removed artwork
        mvc.perform(get("/api/artist/artworks/{id}", id).header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/admin/artworks/{id}/status", id).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"));
        mvc.perform(get("/api/artworks/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void manageCategories() throws Exception {
        String name = "Pottery " + UUID.randomUUID().toString().substring(0, 4);
        String json = mvc.perform(multipart("/api/admin/categories")
                        .file(new MockMultipartFile("icon", "i.jpg", "image/jpeg", jpeg()))
                        .param("name", name).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug", startsWith("pottery-")))
                .andExpect(jsonPath("$.iconUrl", startsWith("/uploads/categories/")))
                .andExpect(jsonPath("$.artworkCount").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        mvc.perform(get("/api/categories")).andExpect(jsonPath("$[*].name", hasItem(name)));
        mvc.perform(multipart("/api/admin/categories").param("name", name.toUpperCase()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict());
        mvc.perform(multipart(HttpMethod.PUT, "/api/admin/categories/" + id).param("name", name + " & Clay")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.slug", startsWith("pottery-")))
                .andExpect(jsonPath("$.iconUrl", startsWith("/uploads/categories/")));

        long paintings = categories.findBySlug("paintings").orElseThrow().getId();
        mvc.perform(delete("/api/admin/categories/{id}", paintings).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/categories/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/categories")).andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
    }

    @Test
    void listsAllOrders() throws Exception {
        mvc.perform(get("/api/admin/orders").param("status", "PAID").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("PAID"))));
        mvc.perform(get("/api/admin/orders").param("status", "NOPE").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest());
    }
}
