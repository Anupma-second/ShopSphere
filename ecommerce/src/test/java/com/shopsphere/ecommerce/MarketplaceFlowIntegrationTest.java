package com.shopsphere.ecommerce;

import com.shopsphere.ecommerce.entity.Category;
import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.repository.CategoryRepository;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.UserRepository;
import com.shopsphere.ecommerce.security.JwtService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check of the seller / admin features against an in-memory
 * H2 database (your real MySQL is never touched).
 *
 * Run just this test:  ./mvnw test -Dtest=MarketplaceFlowIntegrationTest
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:shopsphere;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "jwt.secret=test-secret-test-secret-test-secret-test-secret",
                "razorpay.key.id=rzp_test_dummy",
                "razorpay.key.secret=dummy",
                "spring.task.scheduling.enabled=false"
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MarketplaceFlowIntegrationTest {

    @Autowired Environment env;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    private String adminToken, sellerToken, otherSellerToken, customerToken;
    private Long categoryId, productId, orderId, customerId;

    @BeforeAll
    void setUp() {
        adminToken = tokenFor(createUser("admin@test.local", Role.ADMIN));
        sellerToken = tokenFor(createUser("seller@test.local", Role.SELLER));
        otherSellerToken = tokenFor(createUser("seller2@test.local", Role.SELLER));
        User customer = createUser("customer@test.local", Role.CUSTOMER);
        customerId = customer.getId();
        customerToken = tokenFor(customer);
        categoryId = categoryRepository.save(new Category("Shirts", "Tops")).getId();
    }

    @Test @org.junit.jupiter.api.Order(1)
    void sellerCreatesProductAndOwnsIt() throws Exception {
        JsonNode p = call("POST", "/api/products", sellerToken,
                "{\"name\":\"Blue Shirt\",\"description\":\"cotton\",\"price\":499,"
                        + "\"stock\":10,\"category\":{\"id\":" + categoryId + "}}", 200);

        productId = p.get("id").asLong();
        assertThat(p.get("sellerName").asString()).isEqualTo("seller@test.local");
        assertThat(p.has("seller")).isFalse();   // no User object (password hash) in JSON
    }

    @Test @org.junit.jupiter.api.Order(2)
    void otherSellerCannotEditIt() throws Exception {
        call("PUT", "/api/products/" + productId, otherSellerToken,
                "{\"name\":\"Hacked\",\"price\":1,\"stock\":1,\"category\":{\"id\":" + categoryId + "}}",
                403);
        call("PATCH", "/api/seller/products/" + productId + "/stock?stock=0", otherSellerToken, null, 403);
        call("DELETE", "/api/products/" + productId, otherSellerToken, null, 403);
    }

    @Test @org.junit.jupiter.api.Order(3)
    void customerCannotUseSellerOrAdminApis() throws Exception {
        call("GET", "/api/seller/stats", customerToken, null, 403);
        call("GET", "/api/admin/users", customerToken, null, 403);
        call("GET", "/api/admin/stats", sellerToken, null, 403);
    }

    @Test @org.junit.jupiter.api.Order(4)
    void publicSearchFiltersSortsAndPages() throws Exception {
        JsonNode page = call("GET",
                "/api/products/search?q=shirt&categoryId=" + categoryId
                        + "&minPrice=100&maxPrice=999&inStock=true&sort=price,desc&size=5",
                null, null, 200);
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("id").asLong()).isEqualTo(productId);

        call("GET", "/api/products/search?sort=seller", null, null, 400);
    }

    @Test @org.junit.jupiter.api.Order(5)
    void sellerSeesAndShipsOrderForTheirProduct() throws Exception {
        JsonNode address = call("POST", "/api/addresses", customerToken,
                "{\"fullName\":\"C Ust\",\"phone\":\"9876543210\",\"addressLine\":\"1 Main St\","
                        + "\"city\":\"Pune\",\"state\":\"MH\",\"postalCode\":\"411001\",\"country\":\"India\"}",
                200, 201);
        call("POST", "/api/cart-items/add", customerToken,
                "{\"productId\":" + productId + ",\"quantity\":2}", 200);
        JsonNode order = call("POST", "/api/orders/checkout", customerToken,
                "{\"addressId\":" + address.get("id").asLong() + "}", 200, 201);
        orderId = order.get("id").asLong();

        // simulate a verified payment
        Order o = orderRepository.findById(orderId).orElseThrow();
        o.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(o);

        JsonNode orders = call("GET", "/api/seller/orders", sellerToken, null, 200);
        assertThat(orders.size()).isEqualTo(1);
        assertThat(orders.get(0).get("sellerSubtotal").asDouble()).isEqualTo(998.0);
        assertThat(orders.get(0).get("canUpdateStatus").asBoolean()).isTrue();

        // the other seller can't see or touch it
        assertThat(call("GET", "/api/seller/orders", otherSellerToken, null, 200).size()).isZero();
        call("PUT", "/api/seller/orders/" + orderId + "/status?status=SHIPPED", otherSellerToken, null, 404);

        // sellers can't cancel; they can ship
        call("PUT", "/api/seller/orders/" + orderId + "/status?status=CANCELLED", sellerToken, null, 400);
        JsonNode shipped = call("PUT", "/api/seller/orders/" + orderId
                + "/status?status=SHIPPED&carrier=Delhivery&trackingNumber=DL123", sellerToken, null, 200);
        assertThat(shipped.get("status").asString()).isEqualTo("SHIPPED");
        assertThat(shipped.get("trackingNumber").asString()).isEqualTo("DL123");
    }

    @Test @org.junit.jupiter.api.Order(6)
    void sellerStats() throws Exception {
        JsonNode stats = call("GET", "/api/seller/stats", sellerToken, null, 200);
        assertThat(stats.get("totalProducts").asLong()).isEqualTo(1);
        assertThat(stats.get("unitsSold").asLong()).isEqualTo(2);
        assertThat(stats.get("revenue").asDouble()).isEqualTo(998.0);
        assertThat(stats.get("topProducts").get(0).get("productId").asLong()).isEqualTo(productId);
    }

    @Test @org.junit.jupiter.api.Order(7)
    void adminManagesUsersAndSeesStats() throws Exception {
        JsonNode sellers = call("GET", "/api/admin/users?role=SELLER", adminToken, null, 200);
        assertThat(sellers.get("totalElements").asLong()).isEqualTo(2);

        JsonNode found = call("GET", "/api/admin/users?q=CUSTOMER@", adminToken, null, 200);
        assertThat(found.get("totalElements").asLong()).isEqualTo(1);

        JsonNode promoted = call("PUT", "/api/admin/users/" + customerId + "/role?role=SELLER",
                adminToken, null, 200);
        assertThat(promoted.get("role").asString()).isEqualTo("SELLER");

        call("PUT", "/api/admin/users/" + customerId + "/enabled?enabled=false", adminToken, null, 200);
        call("GET", "/api/orders", customerToken, null, 401);   // blocked immediately

        JsonNode stats = call("GET", "/api/admin/stats", adminToken, null, 200);
        assertThat(stats.get("totalRevenue").asDouble()).isEqualTo(998.0);
        assertThat(stats.get("revenueLast30Days").asDouble()).isEqualTo(998.0);
        assertThat(stats.get("dailyRevenueLast30Days").size()).isEqualTo(30);
        assertThat(stats.get("ordersByStatus").get("SHIPPED").asLong()).isEqualTo(1);
    }

    @Test @org.junit.jupiter.api.Order(8)
    void adminCanEditAnyProduct() throws Exception {
        JsonNode p = call("PATCH", "/api/seller/products/" + productId + "/stock?stock=3",
                sellerToken, null, 200);
        assertThat(p.get("stock").asInt()).isEqualTo(3);

        JsonNode edited = call("PUT", "/api/products/" + productId, adminToken,
                "{\"name\":\"Blue Shirt v2\",\"price\":549,\"stock\":3,\"category\":{\"id\":" + categoryId + "}}",
                200);
        assertThat(edited.get("name").asString()).isEqualTo("Blue Shirt v2");
        assertThat(edited.get("sellerName").asString()).isEqualTo("seller@test.local"); // owner kept
    }

    @Test @org.junit.jupiter.api.Order(9)
    void productsShowImagesAndRatings() throws Exception {
        String image = "{\"imageUrl\":\"%s\",\"product\":{\"id\":" + productId + "}}";

        call("POST", "/api/product-images", sellerToken, image.formatted("javascript:alert(1)"), 400);
        call("POST", "/api/product-images", otherSellerToken, image.formatted("https://img.test/x.jpg"), 403);
        call("POST", "/api/product-images", sellerToken, image.formatted("https://img.test/shirt.jpg"), 200);

        call("POST", "/api/reviews", otherSellerToken,
                "{\"productId\":" + productId + ",\"rating\":4,\"comment\":\"Nice\"}", 200, 201);
        call("POST", "/api/reviews", adminToken,
                "{\"productId\":" + productId + ",\"rating\":5}", 200, 201);

        JsonNode p = call("GET", "/api/products/" + productId, null, null, 200);
        assertThat(p.get("images").get(0).get("imageUrl").asString()).isEqualTo("https://img.test/shirt.jpg");
        assertThat(p.get("averageRating").asDouble()).isEqualTo(4.5);
        assertThat(p.get("reviewCount").asInt()).isEqualTo(2);

        // the same fields come back from the paged search
        JsonNode page = call("GET", "/api/products/search?q=blue", null, null, 200);
        assertThat(page.get("content").get(0).get("images").size()).isEqualTo(1);
    }


    @Test @org.junit.jupiter.api.Order(10)
    void myAccountCartAndWishlist() throws Exception {
        // profile
        assertThat(call("GET", "/api/users/me", otherSellerToken, null, 200)
                .get("email").asString()).isEqualTo("seller2@test.local");
        assertThat(call("PUT", "/api/users/me", otherSellerToken, "{\"name\":\"Seller Two\"}", 200)
                .get("name").asString()).isEqualTo("Seller Two");
        call("PUT", "/api/users/me", otherSellerToken, "{\"name\":\"  \"}", 400);

        // password: wrong current -> 400, right -> 204, then the new one logs in
        call("PUT", "/api/users/me/password", otherSellerToken,
                "{\"currentPassword\":\"wrong-pass\",\"newPassword\":\"newpass123\"}", 400);
        call("PUT", "/api/users/me/password", otherSellerToken,
                "{\"currentPassword\":\"password123\",\"newPassword\":\"newpass123\"}", 204);
        call("POST", "/api/auth/login", null,
                "{\"email\":\"seller2@test.local\",\"password\":\"newpass123\"}", 200);
        call("POST", "/api/auth/login", null,
                "{\"email\":\"seller2@test.local\",\"password\":\"password123\"}", 401);

        // cart + wishlist items carry the photo and stock
        JsonNode cartLine = call("POST", "/api/cart-items/add", otherSellerToken,
                "{\"productId\":" + productId + ",\"quantity\":1}", 200);
        assertThat(cartLine.get("imageUrl").asString()).isEqualTo("https://img.test/shirt.jpg");
        assertThat(cartLine.get("stock").asInt()).isEqualTo(3);

        JsonNode wish = call("POST", "/api/wishlist-items", otherSellerToken,
                "{\"productId\":" + productId + "}", 201);
        assertThat(wish.get("imageUrl").asString()).isEqualTo("https://img.test/shirt.jpg");
        call("POST", "/api/wishlist-items", otherSellerToken, "{\"productId\":" + productId + "}", 409);
    }

    @Test @org.junit.jupiter.api.Order(11)
    void couponsAtCheckout() throws Exception {
        // admin creates coupons (codes are stored upper-case)
        call("POST", "/api/coupons", adminToken,
                "{\"code\":\"save10\",\"discountType\":\"PERCENT\",\"discountPercentage\":10,\"maxDiscount\":50}", 200);
        call("POST", "/api/coupons", adminToken,
                "{\"code\":\"FLAT200\",\"discountType\":\"FIXED\",\"flatAmount\":200,\"minOrderAmount\":1500}", 200);
        call("POST", "/api/coupons", adminToken,
                "{\"code\":\"OLD\",\"discountPercentage\":20,\"expiresAt\":\"2020-01-01T00:00:00\"}", 200);
        JsonNode once = call("POST", "/api/coupons", adminToken,
                "{\"code\":\"ONCE\",\"discountPercentage\":5,\"usageLimit\":1,\"oncePerCustomer\":true}", 200);
        call("POST", "/api/coupons", adminToken, "{\"code\":\"BAD\",\"discountPercentage\":95}", 400);
        call("POST", "/api/coupons", adminToken, "{\"code\":\"SAVE10\",\"discountPercentage\":5}", 409);
        call("GET", "/api/coupons", sellerToken, null, 403);

        // a fresh shopper with 2 x ₹549 in the cart = ₹1098
        String buyer = tokenFor(createUser("buyer@test.local", Role.CUSTOMER));
        JsonNode address = call("POST", "/api/addresses", buyer,
                "{\"fullName\":\"B Uyer\",\"phone\":\"9876543210\",\"addressLine\":\"2 Main St\","
                        + "\"city\":\"Pune\",\"state\":\"MH\",\"postalCode\":\"411001\",\"country\":\"India\"}",
                200, 201);
        call("POST", "/api/cart-items/add", buyer, "{\"productId\":" + productId + ",\"quantity\":2}", 200);

        // preview: 10% of 1098 = 109.80, capped at 50
        JsonNode quote = call("POST", "/api/orders/apply-coupon", buyer, "{\"code\":\" save10 \"}", 200);
        assertThat(quote.get("discount").asDouble()).isEqualTo(50.0);
        assertThat(quote.get("total").asDouble()).isEqualTo(1048.0);

        assertThat(call("POST", "/api/orders/apply-coupon", buyer, "{\"code\":\"FLAT200\"}", 400)
                .get("error").asString()).contains("more to use this coupon");
        assertThat(call("POST", "/api/orders/apply-coupon", buyer, "{\"code\":\"OLD\"}", 400)
                .get("error").asString()).contains("expired");
        call("POST", "/api/orders/apply-coupon", buyer, "{\"code\":\"NOPE\"}", 400);

        // checkout with ONCE: 5% of 1098 = 54.90 off
        JsonNode order = call("POST", "/api/orders/checkout", buyer,
                "{\"addressId\":" + address.get("id").asLong() + ",\"couponCode\":\"once\"}", 200, 201);
        assertThat(order.get("totalAmount").asDouble()).isEqualTo(1043.1);
        assertThat(order.get("discountAmount").asDouble()).isEqualTo(54.9);
        assertThat(order.get("subtotalAmount").asDouble()).isEqualTo(1098.0);
        assertThat(order.get("couponCode").asString()).isEqualTo("ONCE");

        // ONCE is now used up
        call("POST", "/api/cart-items/add", buyer, "{\"productId\":" + productId + ",\"quantity\":1}", 200);
        call("POST", "/api/orders/apply-coupon", buyer, "{\"code\":\"ONCE\"}", 400);

        // cancelling gives the use back
        call("PUT", "/api/orders/" + order.get("id").asLong() + "/cancel", buyer, null, 200);
        JsonNode coupon = call("GET", "/api/coupons/" + once.get("id").asLong(), adminToken, null, 200);
        assertThat(coupon.get("usedCount").asInt()).isZero();
        call("POST", "/api/orders/apply-coupon", buyer, "{\"code\":\"ONCE\"}", 200);
    }

    // ---------- helpers ----------

    private User createUser(String email, Role role) {
        User u = new User(email, email, passwordEncoder.encode("password123"), role);
        u.setEmailVerified(true);
        return userRepository.save(u);
    }

    private String tokenFor(User u) {
        return jwtService.generateToken(u.getEmail());
    }

    private JsonNode call(String method, String path, String token, String body,
                          int... expectedStatus) throws Exception {

        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + env.getProperty("local.server.port") + path))
                .header("Content-Type", "application/json")
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));

        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());

        assertThat(expectedStatus)
                .as("%s %s -> %d %s", method, path, res.statusCode(), res.body())
                .contains(res.statusCode());

        return res.body().isBlank() ? json.createObjectNode() : json.readTree(res.body());
    }
}
