package com.ss.shopai.config;

import com.ss.shopai.entity.Category;
import com.ss.shopai.entity.Product;
import com.ss.shopai.entity.ProductImage;
import com.ss.shopai.entity.User;
import com.ss.shopai.enums.Role;
import com.ss.shopai.repository.CategoryRepository;
import com.ss.shopai.repository.ProductImageRepository;
import com.ss.shopai.repository.ProductRepository;
import com.ss.shopai.repository.UserRepository;
import com.ss.shopai.service.CartService;
import com.ss.shopai.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bootstraps the database with:
 *  1) A default admin account (from application.properties)
 *  2) A small set of demo categories, products, and images for local development/testing
 *
 * Runs on every startup but is fully idempotent — it checks for existing data before inserting,
 * so re-running the app (even against the persisted H2 file) never creates duplicates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartService cartService;
    private final WishlistService wishlistService;

    @Value("${app.admin.default-email}")
    private String adminEmail;

    @Value("${app.admin.default-password}")
    private String adminPassword;

    @Value("${app.admin.default-name}")
    private String adminName;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedCategoriesAndProducts();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists ({}), skipping admin seed", adminEmail);
            return;
        }

        User admin = User.builder()
                .fullName(adminName)
                .email(adminEmail.toLowerCase())
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        User saved = userRepository.save(admin);
        cartService.getOrCreateCart(saved.getId());
        wishlistService.getOrCreateWishlist(saved.getId());

        log.info("=================================================");
        log.info(" ShopAI Admin account created");
        log.info(" Email:    {}", adminEmail);
        log.info(" Password: {}", adminPassword);
        log.info(" (Change this password after first login)");
        log.info("=================================================");
    }

    private void seedCategoriesAndProducts() {
        if (categoryRepository.count() > 0) {
            log.info("Categories already exist, skipping catalog seed");
            return;
        }

        Category electronics = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Phones, laptops, audio gear, and other electronic devices")
                .imageUrl("https://images.unsplash.com/photo-1498049794561-7780e7231661")
                .active(true)
                .build());

        Category fashion = categoryRepository.save(Category.builder()
                .name("Fashion")
                .description("Clothing, footwear, and accessories for everyone")
                .imageUrl("https://images.unsplash.com/photo-1445205170230-053b83016050")
                .active(true)
                .build());

        Category homeAndKitchen = categoryRepository.save(Category.builder()
                .name("Home & Kitchen")
                .description("Appliances, cookware, and home essentials")
                .imageUrl("https://images.unsplash.com/photo-1583847268964-b28dc8f51f92")
                .active(true)
                .build());

        Category books = categoryRepository.save(Category.builder()
                .name("Books")
                .description("Fiction, non-fiction, academic, and children's books")
                .imageUrl("https://images.unsplash.com/photo-1512820790803-83ca734da794")
                .active(true)
                .build());

        Category sports = categoryRepository.save(Category.builder()
                .name("Sports & Fitness")
                .description("Gym equipment, sportswear, and outdoor gear")
                .imageUrl("https://images.unsplash.com/photo-1517836357463-d25dfeac3438")
                .active(true)
                .build());

        seedProduct(electronics, "Aether X12 Smartphone", "ELEC-PHN-001", "NovaTech",
                "6.5-inch AMOLED display, 128GB storage, triple camera setup, all-day battery life.",
                new BigDecimal("24999.00"), new BigDecimal("10.00"), 45,
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9",
                List.of(
                        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9",
                        "https://images.unsplash.com/photo-1592286927505-1def25115558"
                ));

        seedProduct(electronics, "SoundWave Pro Wireless Earbuds", "ELEC-AUD-002", "SoundWave",
                "Active noise cancellation, 30-hour battery with case, IPX5 water resistance.",
                new BigDecimal("3499.00"), new BigDecimal("15.00"), 120,
                "https://images.unsplash.com/photo-1590658268037-6bf12165a8df",
                List.of("https://images.unsplash.com/photo-1590658268037-6bf12165a8df"));

        seedProduct(electronics, "CoreBook 14 Laptop", "ELEC-LAP-003", "CoreTech",
                "14-inch FHD display, 16GB RAM, 512GB SSD, all-day battery for work and study.",
                new BigDecimal("54999.00"), new BigDecimal("8.00"), 25,
                "https://images.unsplash.com/photo-1496181133206-80ce9b88a853",
                List.of("https://images.unsplash.com/photo-1496181133206-80ce9b88a853"));

        seedProduct(fashion, "Classic Fit Cotton Shirt", "FASH-SHT-001", "Urban Thread",
                "100% breathable cotton, tailored fit, available in multiple colors.",
                new BigDecimal("899.00"), new BigDecimal("20.00"), 200,
                "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf",
                List.of("https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf"));

        seedProduct(fashion, "Everyday Running Sneakers", "FASH-SHO-002", "Stride",
                "Lightweight cushioned sole, breathable mesh upper, ideal for daily runs.",
                new BigDecimal("2799.00"), new BigDecimal("12.00"), 80,
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff",
                List.of("https://images.unsplash.com/photo-1542291026-7eec264c27ff"));

        seedProduct(homeAndKitchen, "Stainless Steel Cookware Set (5-Piece)", "HOME-CKW-001", "ChefLine",
                "Induction-compatible, even heat distribution, dishwasher safe.",
                new BigDecimal("4299.00"), new BigDecimal("18.00"), 40,
                "https://images.unsplash.com/photo-1584990347449-a8ea8fdaee85",
                List.of("https://images.unsplash.com/photo-1584990347449-a8ea8fdaee85"));

        seedProduct(homeAndKitchen, "Digital Air Fryer 4.5L", "HOME-APP-002", "KitchenPlus",
                "Rapid air circulation technology, 8 preset programs, easy-clean basket.",
                new BigDecimal("5499.00"), new BigDecimal("10.00"), 55,
                "https://images.unsplash.com/photo-1626200419199-391ae4be7a41",
                List.of("https://images.unsplash.com/photo-1626200419199-391ae4be7a41"));

        seedProduct(books, "The Silent Orchard", "BOOK-FIC-001", "Penfield Press",
                "A gripping literary fiction novel about family, memory, and belonging.",
                new BigDecimal("399.00"), new BigDecimal("5.00"), 150,
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f",
                List.of("https://images.unsplash.com/photo-1544947950-fa07a98d237f"));

        seedProduct(sports, "Pro Grip Yoga Mat", "SPRT-FIT-001", "FlexCore",
                "6mm extra-cushion mat with non-slip surface, includes carrying strap.",
                new BigDecimal("1299.00"), new BigDecimal("0.00"), 90,
                "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b",
                List.of("https://images.unsplash.com/photo-1544367567-0f2fcb009e0b"));

        seedProduct(sports, "Adjustable Dumbbell Set (2x10kg)", "SPRT-FIT-002", "IronCore",
                "Space-saving adjustable dumbbells, rubber-coated grip, ideal for home gyms.",
                new BigDecimal("3999.00"), new BigDecimal("7.00"), 30,
                "https://images.unsplash.com/photo-1517344884509-a0c97ec11bcc",
                List.of("https://images.unsplash.com/photo-1517344884509-a0c97ec11bcc"));

        log.info("=================================================");
        log.info(" ShopAI demo catalog seeded: {} categories, {} products",
                categoryRepository.count(), productRepository.count());
        log.info("=================================================");
    }

    private void seedProduct(Category category, String name, String sku, String brand, String description,
                              BigDecimal price, BigDecimal discountPercentage, int stock,
                              String thumbnailUrl, List<String> imageUrls) {

        Product product = Product.builder()
                .name(name)
                .sku(sku)
                .brand(brand)
                .description(description)
                .price(price)
                .discountPercentage(discountPercentage)
                .stockQuantity(stock)
                .thumbnailUrl(thumbnailUrl)
                .category(category)
                .active(true)
                .averageRating(0.0)
                .totalReviews(0)
                .totalSold(0)
                .build();

        Product saved = productRepository.save(product);

        int order = 0;
        for (String url : imageUrls) {
            productImageRepository.save(ProductImage.builder()
                    .product(saved)
                    .imageUrl(url)
                    .displayOrder(order++)
                    .build());
        }
    }
}