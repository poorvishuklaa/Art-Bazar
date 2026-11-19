package com.artbazaar.config;

import com.artbazaar.entity.ArtistProfile;
import com.artbazaar.entity.Artwork;
import com.artbazaar.entity.Category;
import com.artbazaar.entity.Role;
import com.artbazaar.entity.User;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.CategoryRepository;
import com.artbazaar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fills an empty catalog with three demo artists and their artworks. Images ship inside the
 * jar (static/images/seed), so they survive redeploys without a persistent volume.
 * Demo artists get a random password nobody knows: they exist to be browsed, not to sign in.
 * Disable with SEED_DEMO_DATA=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private record DemoArtist(String key, String name, String city, String bio) {
    }

    private record DemoArtwork(String artist, String title, String category, int price, int stock, int image,
                               String description) {
    }

    private static final List<DemoArtist> ARTISTS = List.of(
            new DemoArtist("meera", "Meera Joshi", "Jaipur",
                    "Self-taught painter mixing Indian devotional themes with bold modern colour."),
            new DemoArtist("kutch", "Kutch Craft Collective", "Bhuj",
                    "A small group of women artisans making Lippan art, decor and handmade gifts."),
            new DemoArtist("little", "Little Hands Studio", "Indore",
                    "Handmade dolls, toys, jewellery and mini canvases, each one made to order."));

    private static final List<DemoArtwork> ARTWORKS = List.of(
            new DemoArtwork("meera", "Peacock in Bloom", "paintings", 1499, 1, 1,
                    "A peacock in vivid blues and greens surrounded by red blossoms. Acrylic on canvas, ready to hang."),
            new DemoArtwork("meera", "Girl in the Garden", "paintings", 2499, 1, 2,
                    "A quiet watercolour scene of a girl resting under a tree. Soft colours for a calm corner."),
            new DemoArtwork("meera", "Woman of Colours", "paintings", 4999, 1, 3,
                    "A striking portrait painted with layered acrylics and gold detailing."),
            new DemoArtwork("meera", "Blossom", "paintings", 2499, 1, 4,
                    "A dreamy portrait wrapped in orange blossoms, painted on stretched canvas."),
            new DemoArtwork("meera", "Krishna with Flute", "paintings", 7999, 1, 5,
                    "Devotional painting of Krishna with fine detailing in the jewellery and crown."),
            new DemoArtwork("meera", "Vishnu", "paintings", 4999, 1, 6,
                    "Devotional artwork in deep blues and fiery orange, painted in a contemporary style."),
            new DemoArtwork("little", "Lavender Field Mini Canvas", "canvas-diy", 599, 5, 12,
                    "Small hand-painted canvas of a lavender field. Fits a desk, shelf or bedside table."),
            new DemoArtwork("little", "Sunset Mini Canvas", "canvas-diy", 599, 4, 13,
                    "Hand-painted sunset over the hills in pinks and purples, with a mini easel."),
            new DemoArtwork("little", "Lovebirds Canvas", "canvas-diy", 499, 6, 17,
                    "Two birds on a blossoming branch, painted on a small canvas board. A lovely gift."),
            new DemoArtwork("little", "Handmade Rag Doll", "dolls", 699, 3, 19,
                    "Soft rag doll stitched from cotton fabric, with hand-embroidered details."),
            new DemoArtwork("little", "Sunny Crochet Doll", "dolls", 899, 2, 20,
                    "A cheerful crocheted doll in yellow and mint, made with soft, child-safe yarn."),
            new DemoArtwork("little", "Vintage Lace Doll", "dolls", 799, 2, 22,
                    "A collector's doll in a hand-sewn lace and floral dress."),
            new DemoArtwork("kutch", "Seashell Mirror", "home-decor", 1299, 2, 23,
                    "Round mirror framed with hand-placed seashells and pearls."),
            new DemoArtwork("kutch", "Decorated Keepsake Box", "home-decor", 899, 3, 24,
                    "A keepsake box covered in hand-painted flowers, for jewellery or letters."),
            new DemoArtwork("kutch", "Blossom Tree Switchboard Art", "home-decor", 499, 8, 27,
                    "Hand-painted switchboard surround with a pink blossom tree and birds."),
            new DemoArtwork("little", "Wooden Bowling Set", "toys", 999, 4, 31,
                    "Hand-turned wooden pins and balls finished with natural, child-safe oils."),
            new DemoArtwork("kutch", "Starfish Silver Earrings", "jewellery", 449, 10, 33,
                    "Lightweight handmade earrings with starfish charms."),
            new DemoArtwork("kutch", "Pearl Flower Bracelet", "jewellery", 549, 7, 34,
                    "Hand-strung bracelet of pearls and tiny gold flowers."));

    private final AppProperties props;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final CategoryRepository categoryRepository;
    private final ArtworkRepository artworkRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (props.seed() == null || !props.seed().demoData() || artworkRepository.count() > 0) {
            return;
        }
        Map<String, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getSlug, Function.identity()));
        Map<String, User> artists = ARTISTS.stream()
                .collect(Collectors.toMap(DemoArtist::key, this::createArtist));

        for (DemoArtwork d : ARTWORKS) {
            Artwork a = new Artwork();
            a.setArtist(artists.get(d.artist()));
            a.setCategory(categories.get(d.category()));
            a.setTitle(d.title());
            a.setDescription(d.description());
            a.setPrice(BigDecimal.valueOf(d.price()));
            a.setStock(d.stock());
            a.setImageUrl("/images/seed/" + d.image() + ".jpg");
            artworkRepository.save(a);
        }
        log.info("Seeded {} demo artists and {} artworks", ARTISTS.size(), ARTWORKS.size());
    }

    private User createArtist(DemoArtist d) {
        String email = d.key() + "@demo.artbazaar.invalid";
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setName(d.name());
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            u.setRole(Role.ARTIST);
            return userRepository.save(u);
        });
        if (artistProfileRepository.findByUserId(user.getId()).isEmpty()) {
            ArtistProfile p = new ArtistProfile();
            p.setUser(user);
            p.setCity(d.city());
            p.setBio(d.bio());
            p.setApproved(true);
            p.setApprovedAt(Instant.now());
            artistProfileRepository.save(p);
        }
        return user;
    }
}
