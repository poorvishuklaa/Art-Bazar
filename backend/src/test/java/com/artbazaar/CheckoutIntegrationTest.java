package com.artbazaar;

import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.CategoryRepository;
import com.artbazaar.service.OrderService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutIntegrationTest {

    private static final String SHIPPING = """
            {"name":"Asha Rao","phone":"9876543210","address":"12 MG Road","city":"Pune","state":"Maharashtra","pincode":"411001"}
            """;
    private static final String GOOD_CARD = """
            {"method":"CARD","cardNumber":"4242 4242 4242 4242","cardExpiry":"12/30","cardCvv":"123"}
            """;

    @Autowired MockMvc mvc;
    @Autowired ArtistProfileRepository artistProfiles;
    @Autowired CategoryRepository categories;
    @Autowired ArtworkRepository artworks;
    @Autowired OrderService orderService;
    @Autowired JdbcTemplate jdbc;

    // ---------- helpers ----------

    private record Account(long id, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

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
        ArtistProfile p = artistProfiles.findByUserId(a.id()).orElseThrow();
        p.setApproved(true);
        artistProfiles.save(p);
        return a;
    }

    private long listArtwork(Account artist, String price, int stock) throws Exception {
        byte[] jpeg = new byte[32];
        Arrays.fill(jpeg, (byte) 1);
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        String json = mvc.perform(multipart("/api/artist/artworks")
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", jpeg))
                        .param("title", "Piece " + UUID.randomUUID().toString().substring(0, 5))
                        .param("price", price).param("stock", String.valueOf(stock))
                        .param("categoryId", String.valueOf(categories.findBySlug("craft").orElseThrow().getId()))
                        .header(HttpHeaders.AUTHORIZATION, artist.bearer()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions addToCart(Account buyer, long artworkId, int qty) throws Exception {
        return mvc.perform(post("/api/cart/items").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"artworkId\":%d,\"quantity\":%d}".formatted(artworkId, qty)));
    }

    private ResultActions checkout(Account buyer) throws Exception {
        return mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(SHIPPING));
    }

    private long placeOrder(Account buyer) throws Exception {
        String json = checkout(buyer).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions pay(Account buyer, long orderId, String body) throws Exception {
        return mvc.perform(post("/api/orders/{id}/pay", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int stockOf(long artworkId) {
        return artworks.findById(artworkId).orElseThrow().getStock();
    }

    // ---------- cart ----------

    @Test
    void cartAddsMergesUpdatesAndRemoves() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "500", 5);
        Account buyer = signUp("BUYER");

        addToCart(buyer, art, 2).andExpect(status().isOk());
        addToCart(buyer, art, 1)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.subtotal").value(1500.0))
                .andExpect(jsonPath("$.readyForCheckout").value(true));

        addToCart(buyer, art, 3).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", startsWith("Only 5 of")));

        mvc.perform(put("/api/cart/items/{id}", art).header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(jsonPath("$.itemCount").value(1));

        mvc.perform(delete("/api/cart/items/{id}", art).header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.readyForCheckout").value(false));
    }

    @Test
    void cannotBuyOwnOrUnlistedArtworkAndAdminsCannotShop() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "500", 2);
        addToCart(artist, art, 1).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can't buy your own artwork"));

        mvc.perform(patch("/api/artist/artworks/{id}/status", art).header(HttpHeaders.AUTHORIZATION, artist.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIDDEN\"}")).andExpect(status().isOk());
        addToCart(signUp("BUYER"), art, 1).andExpect(status().isNotFound());

        String admin = JsonPath.read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.local\",\"password\":\"AdminPass123\"}"))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    void cartFlagsItemsThatBecameUnavailableAndCheckoutRefusesThem() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "800", 1);
        Account buyer = signUp("BUYER");
        addToCart(buyer, art, 1);

        mvc.perform(patch("/api/artist/artworks/{id}/status", art).header(HttpHeaders.AUTHORIZATION, artist.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIDDEN\"}"));

        mvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(jsonPath("$.items[0].issue").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.subtotal").value(0))
                .andExpect(jsonPath("$.readyForCheckout").value(false));
        checkout(buyer).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("no longer available")));
    }

    // ---------- checkout, payment, cancellation ----------

    @Test
    void fullPurchaseFlow() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "1250.50", 3);
        Account buyer = signUp("BUYER");

        checkout(buyer).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Your cart is empty"));

        addToCart(buyer, art, 2);
        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"phone\":\"12345\",\"address\":\"x\",\"city\":\"y\",\"state\":\"z\",\"pincode\":\"0123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.pincode").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        String placed = checkout(buyer).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(2501.0))
                .andExpect(jsonPath("$.items[0].unitPrice").value(1250.5))
                .andExpect(jsonPath("$.payBy").exists())
                .andReturn().getResponse().getContentAsString();
        long orderId = ((Number) JsonPath.read(placed, "$.id")).longValue();

        assertThat(stockOf(art)).isEqualTo(1); // reserved at checkout
        mvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(jsonPath("$.items.length()").value(0));

        // Invalid card shape is a 400 and changes nothing
        pay(buyer, orderId, "{\"method\":\"CARD\",\"cardNumber\":\"1234 5678 9012 3456\",\"cardExpiry\":\"12/30\",\"cardCvv\":\"123\"}")
                .andExpect(status().isBadRequest());

        // Test card is declined; the failure is recorded on the order
        pay(buyer, orderId, "{\"method\":\"CARD\",\"cardNumber\":\"4000 0000 0000 0002\",\"cardExpiry\":\"12/30\",\"cardCvv\":\"123\"}")
                .andExpect(status().isPaymentRequired());
        mvc.perform(get("/api/orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.status").value("PLACED"));

        // Retry with UPI succeeds
        pay(buyer, orderId, "{\"method\":\"UPI\",\"upiId\":\"asha@okbank\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.paymentReference", startsWith("SIMPAY-")))
                .andExpect(jsonPath("$.payBy").doesNotExist());
        pay(buyer, orderId, GOOD_CARD).andExpect(status().isConflict());

        mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(jsonPath("$.content[0].id").value(orderId))
                .andExpect(jsonPath("$.content[0].itemCount").value(2));

        // Cancel a paid, unshipped order: refunded and stock returned
        mvc.perform(post("/api/orders/{id}/cancel", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.items[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellable").value(false));
        assertThat(stockOf(art)).isEqualTo(3);
        mvc.perform(post("/api/orders/{id}/cancel", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(status().isConflict());
        pay(buyer, orderId, GOOD_CARD).andExpect(status().isConflict());
    }

    @Test
    void ordersArePrivate() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "300", 2);
        Account buyer = signUp("BUYER");
        addToCart(buyer, art, 1);
        long orderId = placeOrder(buyer);

        Account other = signUp("BUYER");
        mvc.perform(get("/api/orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isNotFound());
        pay(other, orderId, GOOD_CARD).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/{id}/cancel", orderId).header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void lastPieceCannotBeSoldTwiceUnderConcurrentCheckout() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "999", 1);
        List<Account> buyers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Account b = signUp("BUYER");
            addToCart(b, art, 1).andExpect(status().isOk());
            buyers.add(b);
        }

        ExecutorService pool = Executors.newFixedThreadPool(buyers.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (Account b : buyers) {
            Callable<Integer> task = () -> {
                start.await();
                return checkout(b).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) statuses.add(f.get());
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(buyers.size() - 1);
        assertThat(stockOf(art)).isZero();
    }

    @Test
    void unpaidOrdersExpireAndReleaseStock() throws Exception {
        Account artist = approvedArtist();
        long art = listArtwork(artist, "450", 2);
        Account buyer = signUp("BUYER");
        addToCart(buyer, art, 2);
        long orderId = placeOrder(buyer);
        assertThat(stockOf(art)).isZero();

        // Pretend the order was placed an hour ago
        jdbc.update("update orders set created_at = DATEADD('HOUR', -1, CURRENT_TIMESTAMP) where id = ?", orderId);
        assertThat(orderService.expireUnpaidOrders()).isGreaterThanOrEqualTo(1);

        mvc.perform(get("/api/orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, buyer.bearer()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        assertThat(stockOf(art)).isEqualTo(2);
    }
}
