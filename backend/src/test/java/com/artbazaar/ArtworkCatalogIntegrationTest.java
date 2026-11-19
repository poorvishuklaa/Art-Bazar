package com.artbazaar;

import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.CategoryRepository;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
class ArtworkCatalogIntegrationTest {

    /** Smallest byte sequence our storage accepts as a JPEG (magic number + padding). */
    private static final byte[] JPEG = jpegBytes();

    @Autowired
    MockMvc mvc;
    @Autowired
    ArtistProfileRepository artistProfiles;
    @Autowired
    CategoryRepository categories;

    // ---------- helpers ----------

    private static byte[] jpegBytes() {
        byte[] b = new byte[64];
        Arrays.fill(b, (byte) 0x20);
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        return b;
    }

    private record Account(long id, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Account signUp(String accountType) throws Exception {
        String email = accountType.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test %s\",\"email\":\"%s\",\"password\":\"Secret123\",\"accountType\":\"%s\",\"city\":\"Pune\"}"
                                .formatted(accountType, email, accountType)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(((Number) JsonPath.read(json, "$.user.id")).longValue(), JsonPath.read(json, "$.accessToken"));
    }

    private Account approvedArtist() throws Exception {
        Account artist = signUp("ARTIST");
        setApproved(artist, true);
        return artist;
    }

    private void setApproved(Account artist, boolean approved) {
        ArtistProfile p = artistProfiles.findByUserId(artist.id()).orElseThrow();
        p.setApproved(approved);
        artistProfiles.save(p);
    }

    private long categoryId(String slug) {
        return categories.findBySlug(slug).orElseThrow().getId();
    }

    private MockMultipartHttpServletRequestBuilder artworkForm(HttpMethod method, String url, String title,
                                                                String price, String category, String status) {
        MockMultipartHttpServletRequestBuilder b = multipart(method, url);
        b.param("title", title)
                .param("description", "Handmade test piece")
                .param("price", price)
                .param("stock", "3")
                .param("categoryId", String.valueOf(categoryId(category)));
        if (status != null) b.param("status", status);
        return b;
    }

    private long createArtwork(Account artist, String title, String price, String category) throws Exception {
        String json = mvc.perform(artworkForm(HttpMethod.POST, "/api/artist/artworks", title, price, category, null)
                        .file(new MockMultipartFile("image", "art.jpg", "image/jpeg", JPEG))
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private List<Integer> publicIds(String query) throws Exception {
        String json = mvc.perform(get("/api/artworks?size=48&" + query)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.content[*].id");
    }

    // ---------- public catalog ----------

    @Test
    void demoCatalogIsSeededAndPaginated() throws Exception {
        mvc.perform(get("/api/artworks").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(18)))
                .andExpect(jsonPath("$.content[0].artist.name").exists())
                .andExpect(jsonPath("$.content[0].category.slug").exists())
                .andExpect(jsonPath("$.content[0].imageUrl", startsWith("/")));
    }

    @Test
    void filtersByCategoryPriceAndText() throws Exception {
        mvc.perform(get("/api/artworks").param("category", "dolls").param("size", "48"))
                .andExpect(jsonPath("$.content[*].category.slug", everyItem(is("dolls"))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));

        mvc.perform(get("/api/artworks").param("minPrice", "1000").param("maxPrice", "3000").param("size", "48"))
                .andExpect(jsonPath("$.content[*].price", everyItem(greaterThanOrEqualTo(1000.0))))
                .andExpect(jsonPath("$.content[*].price", everyItem(lessThanOrEqualTo(3000.0))));

        mvc.perform(get("/api/artworks").param("q", "PEACOCK"))
                .andExpect(jsonPath("$.content[0].title").value("Peacock in Bloom"));

        // LIKE wildcards in user input are treated literally
        mvc.perform(get("/api/artworks").param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void sortsByPrice() throws Exception {
        String json = mvc.perform(get("/api/artworks").param("sort", "price_asc").param("size", "48"))
                .andReturn().getResponse().getContentAsString();
        List<Double> prices = JsonPath.read(json, "$.content[*].price");
        assertThat(prices).isSortedAccordingTo(Double::compare);
    }

    @Test
    void invalidPriceRangeIs400() throws Exception {
        mvc.perform(get("/api/artworks").param("minPrice", "500").param("maxPrice", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void artworkDetailAndArtistProfile() throws Exception {
        String list = mvc.perform(get("/api/artworks").param("q", "Krishna")).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(list, "$.content[0].id");

        String detail = mvc.perform(get("/api/artworks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.artist.city").value("Jaipur"))
                .andReturn().getResponse().getContentAsString();
        int artistId = JsonPath.read(detail, "$.artist.id");

        mvc.perform(get("/api/artists/{id}", artistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Meera Joshi"))
                .andExpect(jsonPath("$.artworkCount", greaterThanOrEqualTo(6)));

        mvc.perform(get("/api/artworks/{id}", 987654)).andExpect(status().isNotFound());
    }

    // ---------- artist management ----------

    @Test
    void approvedArtistCanListEditHideAndDelete() throws Exception {
        Account artist = approvedArtist();
        String title = "Madhubani Fish " + UUID.randomUUID().toString().substring(0, 6);
        long id = createArtwork(artist, title, "1999.50", "paintings");

        assertThat(publicIds("q=" + title.split(" ")[2])).contains((int) id);
        mvc.perform(get("/api/artworks/{id}", id))
                .andExpect(jsonPath("$.imageUrl", startsWith("/uploads/artworks/")))
                .andExpect(jsonPath("$.price").value(1999.5));

        // Edit without a new image, and hide it
        mvc.perform(artworkForm(HttpMethod.PUT, "/api/artist/artworks/" + id, title + " (framed)", "2499", "paintings", "HIDDEN")
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title + " (framed)"))
                .andExpect(jsonPath("$.status").value("HIDDEN"));
        mvc.perform(get("/api/artworks/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/artist/artworks").header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) id)));

        // Quick toggle back to listed
        mvc.perform(patch("/api/artist/artworks/{id}/status", id).header(HttpHeaders.AUTHORIZATION, artist.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.description").value("Handmade test piece"));
        mvc.perform(get("/api/artworks/{id}", id)).andExpect(status().isOk());
        mvc.perform(patch("/api/artist/artworks/{id}/status", id).header(HttpHeaders.AUTHORIZATION, artist.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REMOVED\"}"))
                .andExpect(status().isBadRequest());

        // Delete: gone from the artist's list as well
        mvc.perform(delete("/api/artist/artworks/{id}", id).header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/artist/artworks").header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) id))));
        mvc.perform(get("/api/artist/artworks/{id}", id).header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unapprovedArtistCannotListArtworks() throws Exception {
        Account artist = signUp("ARTIST");
        mvc.perform(artworkForm(HttpMethod.POST, "/api/artist/artworks", "Too soon", "500", "craft", null)
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", JPEG))
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isForbidden());
        // ...but can open their (empty) dashboard list
        mvc.perform(get("/api/artist/artworks").header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void buyersAndAnonymousUsersCannotManageArtworks() throws Exception {
        Account buyer = signUp("BUYER");
        mvc.perform(get("/api/artist/artworks").header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/artist/artworks")).andExpect(status().isUnauthorized());
    }

    @Test
    void artistCannotTouchAnotherArtistsArtwork() throws Exception {
        Account owner = approvedArtist();
        Account other = approvedArtist();
        long id = createArtwork(owner, "Owner's piece", "800", "craft");

        mvc.perform(artworkForm(HttpMethod.PUT, "/api/artist/artworks/" + id, "Stolen", "1", "craft", null)
                        .header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/artist/artworks/{id}", id).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/artworks/{id}", id)).andExpect(jsonPath("$.title").value("Owner's piece"));
    }

    @Test
    void revokingApprovalHidesTheArtistsArtworks() throws Exception {
        Account artist = approvedArtist();
        long id = createArtwork(artist, "Soon hidden", "650", "craft");
        mvc.perform(get("/api/artworks/{id}", id)).andExpect(status().isOk());

        setApproved(artist, false);
        mvc.perform(get("/api/artworks/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/artists/{id}", artist.id())).andExpect(status().isNotFound());
        assertThat(publicIds("category=craft")).doesNotContain((int) id);
    }

    @Test
    void validatesFormAndImage() throws Exception {
        Account artist = approvedArtist();

        mvc.perform(multipart("/api/artist/artworks")
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", JPEG))
                        .param("title", "x").param("price", "0").param("stock", "-1")
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.stock").exists())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());

        // A text file renamed to .jpg is rejected by its content, not its name
        mvc.perform(artworkForm(HttpMethod.POST, "/api/artist/artworks", "Fake image", "500", "craft", null)
                        .file(new MockMultipartFile("image", "evil.jpg", "image/jpeg", "<script>alert(1)</script>".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only JPEG, PNG or WebP images are allowed"));

        mvc.perform(artworkForm(HttpMethod.POST, "/api/artist/artworks", "No image", "500", "craft", null)
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isBadRequest());

        mvc.perform(artworkForm(HttpMethod.POST, "/api/artist/artworks", "Bad status", "500", "craft", "REMOVED")
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", JPEG))
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isBadRequest());
    }
}
