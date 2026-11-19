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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArtistSalesIntegrationTest {

    private static final String SHIPPING = """
            {"name":"Neha Singh","phone":"9812345670","address":"4 Lake View","city":"Bhopal","state":"Madhya Pradesh","pincode":"462001"}
            """;

    @Autowired MockMvc mvc;
    @Autowired ArtistProfileRepository artistProfiles;
    @Autowired CategoryRepository categories;

    private record Account(long id, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    // ---------- helpers ----------

    private Account signUp(String type) throws Exception {
        String email = type.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"T %s\",\"email\":\"%s\",\"password\":\"Secret123\",\"accountType\":\"%s\"}"
                                .formatted(type, email, type)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Account(((Number) JsonPath.read(json, "$.user.id")).longValue(), JsonPath.read(json, "$.accessToken"));
    }

    private Account approvedArtist() throws Exception {
        Account a = signUp("ARTIST");
        setApproved(a, true);
        return a;
    }

    private void setApproved(Account a, boolean approved) {
        ArtistProfile p = artistProfiles.findByUserId(a.id()).orElseThrow();
        p.setApproved(approved);
        artistProfiles.save(p);
    }

    private long listArtwork(Account artist, String title, String price, int stock) throws Exception {
        byte[] jpeg = new byte[32];
        Arrays.fill(jpeg, (byte) 1);
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        String json = mvc.perform(multipart("/api/artist/artworks")
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", jpeg))
                        .param("title", title).param("price", price).param("stock", String.valueOf(stock))
                        .param("categoryId", String.valueOf(categories.findBySlug("craft").orElseThrow().getId()))
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private void addToCart(Account buyer, long artworkId, int qty) throws Exception {
        mvc.perform(post("/api/cart/items").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"artworkId\":%d,\"quantity\":%d}".formatted(artworkId, qty)))
                .andExpect(status().isOk());
    }

    private long order(Account buyer, boolean pay) throws Exception {
        String json = mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(SHIPPING))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        if (pay) {
            mvc.perform(post("/api/orders/{id}/pay", id).header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"UPI\",\"upiId\":\"neha@okaxis\"}"))
                    .andExpect(status().isOk());
        }
        return id;
    }

    private ResultActions fulfil(Account artist, long orderId, String status) throws Exception {
        return mvc.perform(patch("/api/artist/orders/{id}/fulfilment", orderId)
                .header(HttpHeaders.AUTHORIZATION, artist.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private ResultActions buyerOrder(Account buyer, long orderId) throws Exception {
        return mvc.perform(get("/api/orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer()));
    }

    // ---------- tests ----------

    @Test
    void eachArtistFulfilsTheirOwnPartAndTheOrderStatusFollows() throws Exception {
        Account meera = approvedArtist();
        Account kutch = approvedArtist();
        long painting = listArtwork(meera, "Lotus Pond", "2000", 3);
        long print = listArtwork(meera, "Lotus Print", "500", 5);
        long mirror = listArtwork(kutch, "Mud Mirror", "1200", 2);

        Account buyer = signUp("BUYER");
        addToCart(buyer, painting, 2);
        addToCart(buyer, print, 1);
        addToCart(buyer, mirror, 1);
        long orderId = order(buyer, true);

        // Meera sees only her two lines, the address to ship to, and her share of the money
        mvc.perform(get("/api/artist/orders").header(HttpHeaders.AUTHORIZATION, meera.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value(orderId))
                .andExpect(jsonPath("$.content[0].items.length()").value(2))
                .andExpect(jsonPath("$.content[0].myTotal").value(4500.0))
                .andExpect(jsonPath("$.content[0].myStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].shipping.pincode").value("462001"))
                .andExpect(jsonPath("$.content[0].items[*].title", not(hasItem("Mud Mirror"))));
        mvc.perform(get("/api/artist/orders").header(HttpHeaders.AUTHORIZATION, kutch.bearer()))
                .andExpect(jsonPath("$.content[0].items.length()").value(1))
                .andExpect(jsonPath("$.content[0].myTotal").value(1200.0));

        // Can't skip straight to delivered
        fulfil(meera, orderId, "DELIVERED").andExpect(status().isConflict());

        fulfil(meera, orderId, "SHIPPED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.orderStatus").value("PARTIALLY_SHIPPED"));
        buyerOrder(buyer, orderId)
                .andExpect(jsonPath("$.status").value("PARTIALLY_SHIPPED"))
                .andExpect(jsonPath("$.cancellable").value(false));
        mvc.perform(post("/api/orders/{id}/cancel", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(status().isConflict());

        // Shipped orders leave Meera's "to ship" list but stay under "all"
        mvc.perform(get("/api/artist/orders").header(HttpHeaders.AUTHORIZATION, meera.bearer()))
                .andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/artist/orders").param("filter", "all").header(HttpHeaders.AUTHORIZATION, meera.bearer()))
                .andExpect(jsonPath("$.content[0].myStatus").value("SHIPPED"));

        fulfil(meera, orderId, "SHIPPED").andExpect(status().isConflict()); // already shipped
        fulfil(kutch, orderId, "SHIPPED").andExpect(jsonPath("$.orderStatus").value("SHIPPED"));
        fulfil(meera, orderId, "DELIVERED").andExpect(jsonPath("$.orderStatus").value("SHIPPED"));
        fulfil(kutch, orderId, "DELIVERED").andExpect(jsonPath("$.orderStatus").value("DELIVERED"));

        buyerOrder(buyer, orderId)
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.items[*].status", not(hasItem("PENDING"))));
        fulfil(kutch, orderId, "SHIPPED").andExpect(status().isConflict()); // no going back
    }

    @Test
    void unpaidAndForeignOrdersAreOffLimits() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "Clay Bowl", "700", 4);
        Account buyer = signUp("BUYER");
        addToCart(buyer, art, 1);
        long unpaid = order(buyer, false);

        mvc.perform(get("/api/artist/orders").param("filter", "all").header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(jsonPath("$.content.length()").value(0));
        fulfil(artist, unpaid, "SHIPPED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This order hasn't been paid yet"));

        Account stranger = approvedArtist();
        fulfil(stranger, unpaid, "SHIPPED").andExpect(status().isNotFound());
        fulfil(artist, unpaid, "PENDING").andExpect(status().isBadRequest());

        mvc.perform(get("/api/artist/orders").header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/artist/sales/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void artistWhoseApprovalWasRevokedCanStillShipWhatTheySold() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "Tile Coaster", "300", 2);
        Account buyer = signUp("BUYER");
        addToCart(buyer, art, 1);
        long orderId = order(buyer, true);

        setApproved(artist, false);
        fulfil(artist, orderId, "SHIPPED").andExpect(status().isOk());
    }

    @Test
    void salesSummaryCountsOnlyPaidUncancelledSales() throws Exception {
        Account artist = approvedArtist();
        long big = listArtwork(artist, "Big Canvas", "3000", 5);
        long small = listArtwork(artist, "Small Canvas", "400", 10);

        Account a = signUp("BUYER");
        addToCart(a, big, 1);
        addToCart(a, small, 3);
        order(a, true);                     // 3000 + 1200, paid

        Account b = signUp("BUYER");
        addToCart(b, small, 2);
        long refunded = order(b, true);     // 800, then cancelled -> refunded, must not count
        mvc.perform(post("/api/orders/{id}/cancel", refunded).header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(status().isOk());

        Account c = signUp("BUYER");
        addToCart(c, big, 1);
        order(c, false);                    // unpaid, must not count

        String json = mvc.perform(get("/api/artist/sales/summary").header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(4200.0))
                .andExpect(jsonPath("$.itemsSold").value(4))
                .andExpect(jsonPath("$.revenueLast30Days").value(4200.0))
                .andExpect(jsonPath("$.ordersToShip").value(1))
                .andExpect(jsonPath("$.activeListings").value(2))
                .andExpect(jsonPath("$.monthly.length()").value(6))
                .andExpect(jsonPath("$.topArtworks[0].title").value("Big Canvas"))
                .andExpect(jsonPath("$.topArtworks[1].quantity").value(3))
                .andReturn().getResponse().getContentAsString();

        List<Number> monthly = JsonPath.read(json, "$.monthly[*].revenue");
        assertThat(monthly.getLast().doubleValue()).isEqualTo(4200.0);                // this month
        assertThat(monthly.subList(0, 5)).allMatch(v -> v.doubleValue() == 0.0);     // empty months still present

        // The refunded order is still listed for the artist's records
        mvc.perform(get("/api/artist/orders").param("filter", "all").header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(jsonPath("$.content[*].paymentStatus", hasItem("REFUNDED")));
    }
}
