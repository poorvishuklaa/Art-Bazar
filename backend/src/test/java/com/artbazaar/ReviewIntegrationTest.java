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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewIntegrationTest {

    private static final String SHIPPING = """
            {"name":"Asha Rao","phone":"9876543210","address":"1 Hill Rd","city":"Pune","state":"Maharashtra","pincode":"411001"}
            """;

    @Autowired MockMvc mvc;
    @Autowired ArtistProfileRepository artistProfiles;
    @Autowired CategoryRepository categories;

    private record Account(long id, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Account signUp(String type, String name) throws Exception {
        String email = type.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"Secret123\",\"accountType\":\"%s\"}"
                                .formatted(name, email, type)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Account(((Number) JsonPath.read(json, "$.user.id")).longValue(), JsonPath.read(json, "$.accessToken"));
    }

    private Account approvedArtist() throws Exception {
        Account a = signUp("ARTIST", "Review Artist");
        ArtistProfile p = artistProfiles.findByUserId(a.id()).orElseThrow();
        p.setApproved(true);
        artistProfiles.save(p);
        return a;
    }

    private long listArtwork(Account artist) throws Exception {
        byte[] jpeg = new byte[32];
        Arrays.fill(jpeg, (byte) 1);
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        String json = mvc.perform(multipart("/api/artist/artworks")
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", jpeg))
                        .param("title", "Reviewed " + UUID.randomUUID().toString().substring(0, 5))
                        .param("price", "500").param("stock", "10")
                        .param("categoryId", String.valueOf(categories.findBySlug("craft").orElseThrow().getId()))
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** Buys one of the artwork; pays unless told not to. Returns the order id. */
    private long buy(Account buyer, long artworkId, boolean pay) throws Exception {
        mvc.perform(post("/api/cart/items").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"artworkId\":%d,\"quantity\":1}".formatted(artworkId)));
        long orderId = ((Number) JsonPath.read(mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(SHIPPING))
                .andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        if (pay) {
            mvc.perform(post("/api/orders/{id}/pay", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"UPI\",\"upiId\":\"buyer@okhdfc\"}"))
                    .andExpect(status().isOk());
        }
        return orderId;
    }

    private ResultActions review(Account who, long artworkId, int rating, String comment) throws Exception {
        return mvc.perform(put("/api/artworks/{id}/reviews/me", artworkId).header(HttpHeaders.AUTHORIZATION, who.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":%d,\"comment\":\"%s\"}".formatted(rating, comment)));
    }

    private ResultActions mine(Account who, long artworkId) throws Exception {
        return mvc.perform(get("/api/artworks/{id}/reviews/me", artworkId).header(HttpHeaders.AUTHORIZATION, who.bearer()));
    }

    @Test
    void onlyPayingBuyersCanReviewAndAveragesAreShownEverywhere() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist);

        // Not bought: can't review
        Account browser = signUp("BUYER", "Just Looking");
        mine(browser, art).andExpect(jsonPath("$.canReview").value(false));
        review(browser, art, 5, "Lovely").andExpect(status().isForbidden());

        // Ordered but unpaid: still can't
        Account unpaid = signUp("BUYER", "Not Yet Paid");
        buy(unpaid, art, false);
        mine(unpaid, art).andExpect(jsonPath("$.canReview").value(false));

        // Paid: can review, name shown as "First L."
        Account asha = signUp("BUYER", "Asha Mehta Rao");
        buy(asha, art, true);
        mine(asha, art).andExpect(jsonPath("$.canReview").value(true)).andExpect(jsonPath("$.review").doesNotExist());
        review(asha, art, 5, "Even better in person")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewerName").value("Asha R."));

        Account ravi = signUp("BUYER", "Ravi");
        buy(ravi, art, true);
        review(ravi, art, 2, "Smaller than I expected").andExpect(jsonPath("$.reviewerName").value("Ravi"));

        mvc.perform(get("/api/artworks/{id}/reviews", art))
                .andExpect(jsonPath("$.average").value(3.5))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.distribution.5").value(1))
                .andExpect(jsonPath("$.distribution.2").value(1))
                .andExpect(jsonPath("$.distribution.1").value(0))
                .andExpect(jsonPath("$.reviews.content.length()").value(2));
        mvc.perform(get("/api/artworks/{id}", art))
                .andExpect(jsonPath("$.averageRating").value(3.5))
                .andExpect(jsonPath("$.reviewCount").value(2));
        mvc.perform(get("/api/artworks").param("artistId", String.valueOf(artist.id())))
                .andExpect(jsonPath("$.content[0].averageRating").value(3.5))
                .andExpect(jsonPath("$.content[0].reviewCount").value(2));
        mvc.perform(get("/api/artists/{id}", artist.id()))
                .andExpect(jsonPath("$.averageRating").value(3.5))
                .andExpect(jsonPath("$.reviewCount").value(2));

        // Updating keeps one review per buyer
        review(ravi, art, 4, "Grew on me").andExpect(status().isOk());
        mvc.perform(get("/api/artworks/{id}/reviews", art))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.average").value(4.5));
        mine(ravi, art).andExpect(jsonPath("$.review.comment").value("Grew on me"));

        // Deleting my own
        mvc.perform(delete("/api/artworks/{id}/reviews/me", art).header(HttpHeaders.AUTHORIZATION, ravi.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/artworks/{id}/reviews", art)).andExpect(jsonPath("$.count").value(1));

        // Admin moderation
        String admin = "Bearer " + JsonPath.read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test.local\",\"password\":\"AdminPass123\"}")).andReturn().getResponse().getContentAsString(), "$.accessToken");
        long reviewId = ((Number) JsonPath.read(mvc.perform(get("/api/artworks/{id}/reviews", art))
                .andReturn().getResponse().getContentAsString(), "$.reviews.content[0].id")).longValue();
        mvc.perform(delete("/api/admin/reviews/{id}", reviewId).header(HttpHeaders.AUTHORIZATION, asha.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/reviews/{id}", reviewId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/artworks/{id}", art))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.averageRating").doesNotExist());
    }

    @Test
    void cancelledPurchasesDontCount() throws Exception {
        long art = listArtwork(approvedArtist());
        Account buyer = signUp("BUYER", "Changed Mind");
        long orderId = buy(buyer, art, true);
        mvc.perform(post("/api/orders/{id}/cancel", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(status().isOk());
        mine(buyer, art).andExpect(jsonPath("$.canReview").value(false));
        review(buyer, art, 1, "Never received").andExpect(status().isForbidden());
    }

    @Test
    void validationAndAnonymousAccess() throws Exception {
        long art = listArtwork(approvedArtist());
        Account buyer = signUp("BUYER", "Valid Buyer");
        buy(buyer, art, true);
        review(buyer, art, 6, "Too many stars").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
        review(buyer, art, 0, "").andExpect(status().isBadRequest());

        mvc.perform(get("/api/artworks/{id}/reviews/me", art)).andExpect(jsonPath("$.canReview").value(false));
        mvc.perform(get("/api/artworks/{id}/reviews", art)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        mvc.perform(put("/api/artworks/{id}/reviews/me", art).contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/artworks/{id}/reviews", 987654)).andExpect(status().isNotFound());
    }
}
