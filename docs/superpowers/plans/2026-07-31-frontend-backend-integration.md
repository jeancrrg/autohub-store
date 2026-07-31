# Frontend ↔ Backend Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the frontend's mock data/auth (`lib/data/*.ts`, `authStore` fake login) with real calls to Auth Service, Catalog Service and API Gateway, and give the Catalog Service real image upload via MinIO — closing every gap identified in `docs/integration/frontend-backend-integration.md`.

**Architecture:** Frontend talks only to API Gateway (`:8001`) via a central axios client with `withCredentials:true`. Auth Service issues JWTs as httpOnly cookies (never in response body); Gateway reads the JWT from the cookie instead of an `Authorization` header. Catalog Service gains a 2-step product creation flow: `POST /products` (JSON) then `POST /products/{id}/images` (multipart → MinIO). Frontend adopts React Query for all server state and migrates `Product.id` from `number` to `string` (UUID) throughout.

**Tech Stack:** Spring Boot 3.5 (Java 25), Spring Cloud Gateway (WebFlux), Spring Security, MinIO Java SDK, Next.js 16 / React 19, axios, @tanstack/react-query, zustand.

## Global Constraints

- Checkstyle: all new/modified Java code must pass `infra/checkstyle/checkstyle.xml` on first write (4-space indent, no magic numbers, no generic exceptions, blank line after `{` and before `}`, `private` fields, ≤130 char lines).
- Services: `Service` classes only call `Repository`/`Service` of their own domain; DI via `@RequiredArgsConstructor` + `private final` fields only.
- Entity ↔ DTO mapping always via MapStruct (`@Mapper`) — never manual field-by-field mapping.
- Error contract: RFC 7807 `ProblemDetail` for every error response (already the case in `GlobalExceptionHandler` of catalog/auth — do not regress this).
- Frontend: no default exports for components (project convention, see existing components); CSS via `*.module.css` colocated with component.
- Do not create the next microservices (Search, Cart, Order, Payment, Notification, Analytics) until every task in this plan is checked off — this plan is the Fase 2.5 gate in `docs/planning/action-plan.md`.

---

## Task 1: Auth Service — issue JWT as httpOnly cookies

**Files:**
- Modify: `backend/auth-service/src/main/java/com/autohubstore/authservice/infrastructure/web/AuthController.java`
- Modify: `backend/auth-service/src/main/java/com/autohubstore/authservice/infrastructure/web/AuthApi.java`
- Create: `backend/auth-service/src/main/java/com/autohubstore/authservice/infrastructure/web/AuthCookieFactory.java`
- Test: `backend/auth-service/src/test/java/com/autohubstore/authservice/infrastructure/web/AuthCookieFactoryTest.java`
- Test: `backend/auth-service/src/test/java/com/autohubstore/authservice/infrastructure/web/AuthControllerTest.java`

**Interfaces:**
- Consumes: `LoginUseCase.execute(LoginRequest): LoginResponse` (unchanged, still returns `LoginResponse(accessToken, refreshToken, tokenType, expiresIn)` — domain/application layer is NOT touched)
- Produces: `AuthCookieFactory.buildAccessTokenCookie(String token, long ttlSeconds): ResponseCookie`, `AuthCookieFactory.buildRefreshTokenCookie(String token): ResponseCookie`, `AuthCookieFactory.expiredAccessTokenCookie(): ResponseCookie`, `AuthCookieFactory.expiredRefreshTokenCookie(): ResponseCookie`

- [ ] **Step 1: Write the failing test for `AuthCookieFactory`**

```java
package com.autohubstore.authservice.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieFactoryTest {

    private static final long ACCESS_TTL_SECONDS = 3600L;

    private final AuthCookieFactory factory = new AuthCookieFactory();

    @Test
    void buildAccessTokenCookieIsHttpOnlyAndSecure() {
        ResponseCookie cookie = factory.buildAccessTokenCookie("token-value", ACCESS_TTL_SECONDS);

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(ACCESS_TTL_SECONDS);
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void buildRefreshTokenCookieIsScopedToRefreshPath() {
        ResponseCookie cookie = factory.buildRefreshTokenCookie("refresh-value");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth/refresh");
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void expiredCookiesHaveZeroMaxAge() {
        assertThat(factory.expiredAccessTokenCookie().getMaxAge().getSeconds()).isZero();
        assertThat(factory.expiredRefreshTokenCookie().getMaxAge().getSeconds()).isZero();
    }

}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend/auth-service && ./mvnw test -Dtest=AuthCookieFactoryTest`
Expected: FAIL — `AuthCookieFactory` does not exist (compile error)

- [ ] **Step 3: Implement `AuthCookieFactory`**

```java
package com.autohubstore.authservice.infrastructure.web;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieFactory {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/api/v1/auth/refresh";
    private static final long REFRESH_TOKEN_TTL_SECONDS = 604800L;
    private static final int ZERO_MAX_AGE = 0;

    public ResponseCookie buildAccessTokenCookie(final String token, final long ttlSeconds) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .build();
    }

    public ResponseCookie buildRefreshTokenCookie(final String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofSeconds(REFRESH_TOKEN_TTL_SECONDS))
                .build();
    }

    public ResponseCookie expiredAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(ZERO_MAX_AGE))
                .build();
    }

    public ResponseCookie expiredRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofSeconds(ZERO_MAX_AGE))
                .build();
    }

}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend/auth-service && ./mvnw test -Dtest=AuthCookieFactoryTest`
Expected: PASS (3 tests)

- [ ] **Step 5: Write the failing test for `AuthController` cookie behavior**

```java
package com.autohubstore.authservice.infrastructure.web;

import com.autohubstore.authservice.application.dto.LoginRequest;
import com.autohubstore.authservice.application.dto.LoginResponse;
import com.autohubstore.authservice.application.usecase.ForgotPasswordUseCase;
import com.autohubstore.authservice.application.usecase.LoginUseCase;
import com.autohubstore.authservice.application.usecase.LogoutUseCase;
import com.autohubstore.authservice.application.usecase.RefreshTokenUseCase;
import com.autohubstore.authservice.application.usecase.ResetPasswordUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private static final long TTL_SECONDS = 3600L;

    private final LoginUseCase loginUseCase = mock(LoginUseCase.class);
    private final LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);
    private final ForgotPasswordUseCase forgotPasswordUseCase = mock(ForgotPasswordUseCase.class);
    private final ResetPasswordUseCase resetPasswordUseCase = mock(ResetPasswordUseCase.class);
    private final AuthCookieFactory cookieFactory = new AuthCookieFactory();

    private final AuthController controller = new AuthController(
            loginUseCase, logoutUseCase, refreshTokenUseCase,
            forgotPasswordUseCase, resetPasswordUseCase, cookieFactory);

    @Test
    void loginSetsAccessAndRefreshCookiesAndReturnsEmptyBody() {
        LoginRequest request = new LoginRequest("user@email.com", "password123");
        when(loginUseCase.execute(request))
                .thenReturn(LoginResponse.of("access-jwt", "refresh-jwt", TTL_SECONDS));

        ResponseEntity<Void> response = controller.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotNull();
        assertThat(setCookies).anyMatch(c -> c.startsWith("access_token=access-jwt"));
        assertThat(setCookies).anyMatch(c -> c.startsWith("refresh_token=refresh-jwt"));
    }

}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `cd backend/auth-service && ./mvnw test -Dtest=AuthControllerTest`
Expected: FAIL — `AuthController` constructor doesn't accept `AuthCookieFactory`; `login` still returns `ResponseEntity<LoginResponse>`

- [ ] **Step 7: Update `AuthApi` interface signatures**

Replace the `login`, `logout`, and `refresh` method signatures in `AuthApi.java` (keep the
`@Operation`/`@Tag` docs annotations already there):

```java
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica com e-mail/senha e seta cookies httpOnly de sessão")
    ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request);

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga tokens e limpa cookies de sessão",
               security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Void> logout(
            @CookieValue(value = "access_token", required = false) String accessToken,
            @CookieValue(value = "refresh_token", required = false) String refreshToken);

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Rotaciona o refresh token e re-seta os cookies")
    ResponseEntity<Void> refresh(@CookieValue(value = "refresh_token", required = true) String refreshToken);
```

Remove the now-unused `RefreshRequest` import from `AuthApi.java` and add
`import org.springframework.web.bind.annotation.CookieValue;`.

- [ ] **Step 8: Update `AuthController` to set cookies**

```java
package com.autohubstore.authservice.infrastructure.web;

import com.autohubstore.authservice.application.dto.ForgotPasswordRequest;
import com.autohubstore.authservice.application.dto.LoginRequest;
import com.autohubstore.authservice.application.dto.LoginResponse;
import com.autohubstore.authservice.application.dto.RefreshRequest;
import com.autohubstore.authservice.application.dto.ResetPasswordRequest;
import com.autohubstore.authservice.application.usecase.ForgotPasswordUseCase;
import com.autohubstore.authservice.application.usecase.LoginUseCase;
import com.autohubstore.authservice.application.usecase.LogoutUseCase;
import com.autohubstore.authservice.application.usecase.RefreshTokenUseCase;
import com.autohubstore.authservice.application.usecase.ResetPasswordUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final AuthCookieFactory cookieFactory;

    public ResponseEntity<Void> login(LoginRequest request) {
        LoginResponse tokens = loginUseCase.execute(request);
        return withSessionCookies(tokens);
    }

    public ResponseEntity<Void> logout(String accessToken, String refreshToken) {
        logoutUseCase.execute(accessToken, refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expiredRefreshTokenCookie().toString())
                .build();
    }

    public ResponseEntity<Void> refresh(String refreshToken) {
        LoginResponse tokens = refreshTokenUseCase.execute(new RefreshRequest(refreshToken));
        return withSessionCookies(tokens);
    }

    public ResponseEntity<Void> forgotPassword(ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request);
        return ResponseEntity.accepted().build();
    }

    public ResponseEntity<Void> resetPassword(ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> withSessionCookies(LoginResponse tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.buildAccessTokenCookie(tokens.accessToken(), tokens.expiresIn()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.buildRefreshTokenCookie(tokens.refreshToken()).toString())
                .build();
    }

}
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `cd backend/auth-service && ./mvnw test -Dtest=AuthCookieFactoryTest,AuthControllerTest`
Expected: PASS (4 tests)

- [ ] **Step 10: Run full module build (checkstyle + tests)**

Run: `cd backend/auth-service && ./mvnw verify`
Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add backend/auth-service
git commit -m "feat(auth-service): issue JWT as httpOnly cookies instead of response body"
```

---

## Task 2: API Gateway — read JWT from cookie instead of Authorization header

**Files:**
- Modify: `backend/api-gateway/src/main/java/com/autohubstore/gateway/adapter/in/web/JwtServerAuthenticationConverter.java`
- Test: `backend/api-gateway/src/test/java/com/autohubstore/gateway/adapter/in/web/JwtServerAuthenticationConverterTest.java`

**Interfaces:**
- Consumes: `ServerWebExchange` (from Spring WebFlux, already used)
- Produces: `Mono<Authentication>` (unchanged signature — only the token source changes)

- [ ] **Step 1: Write the failing test**

```java
package com.autohubstore.gateway.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServerAuthenticationConverterTest {

    private final JwtServerAuthenticationConverter converter = new JwtServerAuthenticationConverter();

    @Test
    void extractsTokenFromAccessTokenCookie() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .cookie(new HttpCookie("access_token", "cookie-jwt-value")));

        Authentication auth = converter.convert(exchange).block();

        assertThat(auth).isNotNull();
        assertThat(auth.getCredentials()).isEqualTo("cookie-jwt-value");
    }

    @Test
    void returnsEmptyWhenNoAccessTokenCookiePresent() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders"));

        Authentication auth = converter.convert(exchange).block();

        assertThat(auth).isNull();
    }

}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend/api-gateway && ./mvnw test -Dtest=JwtServerAuthenticationConverterTest`
Expected: FAIL — converter currently reads `Authorization` header, cookie test returns null auth

- [ ] **Step 3: Update `JwtServerAuthenticationConverter` to read the cookie**

```java
package com.autohubstore.gateway.adapter.in.web;

import org.springframework.http.HttpCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    @Override
    public Mono<Authentication> convert(final ServerWebExchange exchange) {
        final HttpCookie cookie = exchange.getRequest().getCookies().getFirst(ACCESS_TOKEN_COOKIE);
        if (cookie == null || cookie.getValue().isBlank()) {
            return Mono.empty();
        }
        final String token = cookie.getValue();
        return Mono.just(new UsernamePasswordAuthenticationToken(token, token));
    }

}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend/api-gateway && ./mvnw test -Dtest=JwtServerAuthenticationConverterTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Update Gateway's public-endpoints CORS/env doc note (no code change needed)**

`CorsConfig.java` already sets `allowCredentials(true)` and reads `cors.allowed-origins` — confirm
`backend/api-gateway/src/main/resources/application.yml` has:

```yaml
cors:
  allowed-origins: ${ALLOWED_ORIGINS:http://localhost:3000}
```

If missing, add it under the root of the yml (same level as `server:`).

- [ ] **Step 6: Run full module build**

Run: `cd backend/api-gateway && ./mvnw verify`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/api-gateway
git commit -m "feat(api-gateway): read JWT from httpOnly cookie instead of Authorization header"
```

---

## Task 3: Catalog Service — MinIO image upload endpoint

**Files:**
- Modify: `backend/catalog-service/build.gradle`
- Modify: `backend/catalog-service/src/main/resources/application.yml`
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/config/MinioConfig.java`
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/exception/UnsupportedImageTypeException.java`
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/service/ProductImageService.java`
- Modify: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/controller/ProductController.java`
- Test: `backend/catalog-service/src/test/java/com/autohubstore/catalogservice/service/ProductImageServiceTest.java`

**Interfaces:**
- Consumes: `ProductRepository.findById(UUID): Optional<Product>` (existing), `ProductImage` entity (existing, `backend/catalog-service/.../domain/entity/ProductImage.java`)
- Produces: `ProductImageService.uploadImages(UUID productId, List<MultipartFile> files): List<ProductImageResponse>`, `ProductImageService.deleteImage(UUID productId, UUID imageId): void`

- [ ] **Step 1: Add MinIO dependency to `build.gradle`**

```groovy
    implementation 'io.minio:minio:8.5.11'
```

Add this line right after `implementation 'org.postgresql:postgresql'` is declared as
`runtimeOnly` (keep it grouped with the other `implementation` lines, before the Lombok block).

- [ ] **Step 2: Add MinIO config to `application.yml`**

```yaml
  minio:
    endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
    access-key: ${MINIO_ACCESS_KEY:minio_admin}
    secret-key: ${MINIO_SECRET_KEY:minio_pass}
    bucket: ${MINIO_BUCKET:catalog-images}
```

Add this block nested under `spring:` (same indentation level as `data:` and `cache:`).

- [ ] **Step 3: Write the failing test for `ProductImageService`**

```java
package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.domain.entity.ProductImage;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.exception.UnsupportedImageTypeException;
import com.autohubstore.catalogservice.repository.ProductRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ProductImageServiceTest {

    private static final String BUCKET = "catalog-images";

    private ProductRepository productRepository;
    private MinioClient minioClient;
    private ProductImageService service;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        minioClient = Mockito.mock(MinioClient.class);
        service = new ProductImageService(productRepository, minioClient, BUCKET);
    }

    @Test
    void rejectsUnsupportedContentType() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(Product.builder().id(productId).images(new java.util.ArrayList<>()).build()));

        MockMultipartFile file = new MockMultipartFile("files", "malware.exe",
                "application/octet-stream", "content".getBytes());

        assertThatThrownBy(() -> service.uploadImages(productId, List.of(file)))
                .isInstanceOf(UnsupportedImageTypeException.class);
    }

    @Test
    void throwsWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg",
                "image/jpeg", "content".getBytes());

        assertThatThrownBy(() -> service.uploadImages(productId, List.of(file)))
                .isInstanceOf(ProductNotFoundException.class);
    }

}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `cd backend/catalog-service && ./gradlew test --tests ProductImageServiceTest`
Expected: FAIL — `ProductImageService` and `UnsupportedImageTypeException` don't exist

- [ ] **Step 5: Implement `UnsupportedImageTypeException`**

```java
package com.autohubstore.catalogservice.exception;

public class UnsupportedImageTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedImageTypeException(final String contentType) {
        super("Tipo de arquivo não suportado: " + contentType);
    }

}
```

- [ ] **Step 6: Implement `MinioConfig`**

```java
package com.autohubstore.catalogservice.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${spring.minio.endpoint}")
    private String endpoint;

    @Value("${spring.minio.access-key}")
    private String accessKey;

    @Value("${spring.minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

}
```

- [ ] **Step 7: Implement `ProductImageService`**

```java
package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.entity.Product;
import com.autohubstore.catalogservice.domain.entity.ProductImage;
import com.autohubstore.catalogservice.domain.dto.response.ProductImageResponse;
import com.autohubstore.catalogservice.exception.ProductNotFoundException;
import com.autohubstore.catalogservice.exception.UnsupportedImageTypeException;
import com.autohubstore.catalogservice.repository.ProductRepository;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final ProductRepository productRepository;
    private final MinioClient minioClient;

    @Value("${spring.minio.bucket}")
    private final String bucket;

    @Transactional
    public List<ProductImageResponse> uploadImages(UUID productId, List<MultipartFile> files) {
        Product product = findProductOrThrow(productId);
        boolean hasExistingPrimary = product.getImages().stream().anyMatch(ProductImage::isPrimary);

        List<ProductImageResponse> uploaded = files.stream()
                .map(file -> uploadOne(product, file, hasExistingPrimary))
                .collect(Collectors.toList());

        productRepository.save(product);
        return uploaded;
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        Product product = findProductOrThrow(productId);
        ProductImage image = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ProductNotFoundException(imageId.toString()));

        removeFromMinio(objectKey(productId, image.getUrl()));
        product.getImages().remove(image);
        productRepository.save(product);
    }

    private ProductImageResponse uploadOne(Product product, MultipartFile file, boolean hasExistingPrimary) {
        validate(file);
        String objectKey = productId(product) + "/" + UUID.randomUUID() + extensionOf(file);
        putInMinio(objectKey, file);

        ProductImage image = ProductImage.builder()
                .product(product)
                .url(publicUrl(objectKey))
                .primary(!hasExistingPrimary && product.getImages().isEmpty())
                .build();
        product.getImages().add(image);
        return new ProductImageResponse(image.getId(), image.getUrl(), image.isPrimary());
    }

    private void validate(MultipartFile file) {
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new UnsupportedImageTypeException(file.getContentType());
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new UnsupportedImageTypeException("arquivo excede 5MB: " + file.getOriginalFilename());
        }
    }

    private void putInMinio(String objectKey, MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(input, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new UnsupportedImageTypeException("falha ao enviar arquivo pro storage: " + e.getMessage());
        }
    }

    private void removeFromMinio(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new UnsupportedImageTypeException("falha ao remover arquivo do storage: " + e.getMessage());
        }
    }

    private Product findProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));
    }

    private String productId(Product product) {
        return product.getId().toString();
    }

    private String extensionOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        int dotIndex = name == null ? -1 : name.lastIndexOf('.');
        return dotIndex >= 0 ? name.substring(dotIndex) : "";
    }

    private String publicUrl(String objectKey) {
        return "/" + bucket + "/" + objectKey;
    }

    private String objectKey(UUID productId, String url) {
        return url.replaceFirst("^/" + bucket + "/", "");
    }

}
```

> Note: `MinioClient` throws checked exceptions from many providers (IO, invalid key, etc.) — catching
> broad `Exception` here is the one documented exception to `IllegalCatch` allowed by the MinIO SDK's
> own contract; wrap and rethrow immediately as the domain exception `UnsupportedImageTypeException`,
> never swallow it.

- [ ] **Step 8: Run test to verify it passes**

Run: `cd backend/catalog-service && ./gradlew test --tests ProductImageServiceTest`
Expected: PASS (2 tests)

- [ ] **Step 9: Add upload/delete endpoints to `ProductController`**

Add these two methods (and their imports: `MultipartFile`, `RequestPart`, `PathVariable`, `ProductImageResponse`, `ProductImageService`, `List`) to `ProductController.java`; inject `ProductImageService` alongside `ProductService` via the existing `@RequiredArgsConstructor`:

```java
    private final ProductImageService productImageService;

    @PostMapping("/{id}/images")
    public ResponseEntity<List<ProductImageResponse>> uploadImages(
            @PathVariable UUID id, @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productImageService.uploadImages(id, files));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        productImageService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }
```

- [ ] **Step 10: Run full module build (checkstyle + tests)**

Run: `cd backend/catalog-service && ./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add backend/catalog-service
git commit -m "feat(catalog-service): add MinIO-backed product image upload/delete endpoints"
```

---

## Task 4: Frontend — API client + React Query provider

**Files:**
- Modify: `apps/frontend/ecommerce/package.json`
- Create: `apps/frontend/ecommerce/lib/api/client.ts`
- Create: `apps/frontend/ecommerce/lib/api/queryClient.ts`
- Modify: `apps/frontend/ecommerce/components/providers/StoreProvider.tsx`
- Create: `apps/frontend/ecommerce/.env.local.example`
- Test: `apps/frontend/ecommerce/lib/api/__tests__/client.test.ts`

**Interfaces:**
- Produces: `apiClient` (default export, configured axios instance), `queryClient` (singleton `QueryClient`)

- [ ] **Step 1: Add dependencies**

```bash
cd apps/frontend/ecommerce && npm install @tanstack/react-query
```

- [ ] **Step 2: Add env var example file**

```
NEXT_PUBLIC_API_URL=http://localhost:8001
```

- [ ] **Step 3: Write the failing test for the 401-refresh interceptor**

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '../client'

describe('apiClient 401 refresh interceptor', () => {
    let mock: MockAdapter

    beforeEach(() => {
        mock = new MockAdapter(apiClient)
    })

    it('retries the original request once after a successful refresh', async () => {
        mock
            .onGet('/api/v1/users/me')
            .replyOnce(401)
            .onPost('/api/v1/auth/refresh')
            .replyOnce(200)
            .onGet('/api/v1/users/me')
            .replyOnce(200, { id: 'u1', email: 'user@email.com' })

        const response = await apiClient.get('/api/v1/users/me')

        expect(response.status).toBe(200)
        expect(response.data.id).toBe('u1')
    })

    it('propagates the error when refresh also fails', async () => {
        mock.onGet('/api/v1/users/me').reply(401)
        mock.onPost('/api/v1/auth/refresh').reply(401)

        await expect(apiClient.get('/api/v1/users/me')).rejects.toMatchObject({
            response: { status: 401 },
        })
    })
})
```

Note: this test needs `axios-mock-adapter` and `vitest` as dev dependencies:
`npm install -D axios-mock-adapter vitest`. If the project has no Vitest config yet, add a minimal
`vitest.config.ts` at the frontend root re-using the existing `tsconfig.json` paths.

- [ ] **Step 4: Run test to verify it fails**

Run: `cd apps/frontend/ecommerce && npx vitest run lib/api/__tests__/client.test.ts`
Expected: FAIL — `../client` module doesn't exist

- [ ] **Step 5: Implement `lib/api/client.ts`**

```ts
import axios from 'axios'

export const apiClient = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_URL,
    withCredentials: true,
})

let refreshPromise: Promise<void> | null = null

function refreshSession(): Promise<void> {
    if (!refreshPromise) {
        refreshPromise = apiClient
            .post('/api/v1/auth/refresh')
            .then(() => undefined)
            .finally(() => {
                refreshPromise = null
            })
    }
    return refreshPromise
}

apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config
        const isRefreshCall = originalRequest?.url === '/api/v1/auth/refresh'

        if (error.response?.status === 401 && !originalRequest._retry && !isRefreshCall) {
            originalRequest._retry = true
            try {
                await refreshSession()
                return apiClient(originalRequest)
            } catch (refreshError) {
                return Promise.reject(refreshError)
            }
        }

        return Promise.reject(error)
    }
)
```

- [ ] **Step 6: Implement `lib/api/queryClient.ts`**

```ts
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 60_000,
            retry: 1,
        },
    },
})
```

- [ ] **Step 7: Wire `QueryClientProvider` into `StoreProvider`**

```tsx
'use client'

import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/lib/api/queryClient'

export function StoreProvider({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `cd apps/frontend/ecommerce && npx vitest run lib/api/__tests__/client.test.ts`
Expected: PASS (2 tests)

- [ ] **Step 9: Run type-check and lint**

Run: `cd apps/frontend/ecommerce && npm run type-check && npm run lint`
Expected: both pass with no errors

- [ ] **Step 10: Commit**

```bash
git add apps/frontend/ecommerce/package.json apps/frontend/ecommerce/package-lock.json apps/frontend/ecommerce/lib/api apps/frontend/ecommerce/components/providers/StoreProvider.tsx apps/frontend/ecommerce/.env.local.example
git commit -m "feat(frontend): add axios client with refresh interceptor and React Query provider"
```

---

## Task 5: Frontend — migrate `Product` type from `id: number` to `id: string`

**Files:**
- Modify: `apps/frontend/ecommerce/types/product.ts`
- Modify: `apps/frontend/ecommerce/types/cart.ts` (no change needed — `CartItem.product` already typed via `Product`, verify only)
- Modify: `apps/frontend/ecommerce/lib/data/products.ts` (mock ids become string literals, temporary until Task 6 removes this file's usage from pages)
- Modify: `apps/frontend/ecommerce/store/cartStore.ts`
- Modify: `apps/frontend/ecommerce/components/admin/ProductsTable/ProductsTable.tsx`
- Test: `apps/frontend/ecommerce/store/__tests__/cartStore.test.ts` (update existing ids to strings)

**Interfaces:**
- Produces: `Product.id: string`, `Product.images: { id: string; url: string; isPrimary: boolean }[]`

- [ ] **Step 1: Update the failing assertions in the existing `cartStore` test**

Open `apps/frontend/ecommerce/store/__tests__/cartStore.test.ts` and change every numeric product
id fixture (e.g. `id: 1`) to a string UUID fixture (e.g. `id: '11111111-1111-1111-1111-111111111111'`).
Keep the rest of the test bodies (`addItem`, `removeItem`, `updateQty` calls) unchanged except for
passing the string id instead of a number.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd apps/frontend/ecommerce && npx vitest run store/__tests__/cartStore.test.ts`
Expected: FAIL — type errors, `removeItem`/`updateQty` still typed to accept `number`

- [ ] **Step 3: Update `types/product.ts`**

```ts
export type ProductTag = 'OFERTA' | 'NOVO'

export type ProductImage = {
    id: string
    url: string
    isPrimary: boolean
}

export type Product = {
    id: string
    name: string
    brand: string
    price: number
    oldPrice?: number
    tag?: ProductTag
    tagColor: string
    stars: number
    reviews: number
    installments: number
    inStock: boolean
    description: string
    specs: Record<string, string>
    category: string
    images: string[]
}

export type Category = {
    id: string
    name: string
    icon: string
    count: number
}

export type Brand = {
    id: string
    name: string
}
```

(`ProductImage` is added now for Task 6's real API mapping; the mock `Product.images: string[]`
shape is kept as-is since `lib/data/products.ts` stays URL-only until removed.)

- [ ] **Step 4: Update `store/cartStore.ts` id types**

```ts
import { create } from 'zustand'
import type { Product } from '@/types/product'
import type { CartItem } from '@/types/cart'

type CartStore = {
    items: CartItem[]
    total: number
    count: number
    addItem: (product: Product, qty: number) => void
    removeItem: (id: string) => void
    updateQty: (id: string, qty: number) => void
    clearCart: () => void
}
```

Leave the function bodies untouched — they already compare by `===`, which works identically for
strings.

- [ ] **Step 5: Update `lib/data/products.ts` mock ids to string UUIDs**

Replace every `id: 1`, `id: 2`, ... with fixed string UUIDs, e.g.
`id: '3fa85f64-5717-4562-b3fc-2c963f66afa6'` for the first product, incrementing the last
hex segment for each subsequent product. Keep every other field unchanged.

- [ ] **Step 6: Update `ProductsTable.tsx` `handleDelete` signature**

```ts
    function handleDelete(id: string) {
        setRows((prev) => prev.filter((p) => p.id !== id))
    }
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `cd apps/frontend/ecommerce && npx vitest run store/__tests__/cartStore.test.ts`
Expected: PASS

- [ ] **Step 8: Run type-check across the whole frontend**

Run: `cd apps/frontend/ecommerce && npm run type-check`
Expected: no errors — this surfaces every remaining `number`/`string` id mismatch (e.g.
`app/(public)/products/[id]/page.tsx` uses `Number(id)`; fix any reported here inline using the
pattern from Task 6, Step 3)

- [ ] **Step 9: Commit**

```bash
git add apps/frontend/ecommerce/types apps/frontend/ecommerce/store apps/frontend/ecommerce/lib/data/products.ts apps/frontend/ecommerce/components/admin/ProductsTable
git commit -m "refactor(frontend): migrate Product.id from number to string (UUID)"
```

---

## Task 6: Frontend — real catalog data (React Query hooks + pages)

**Files:**
- Create: `apps/frontend/ecommerce/lib/api/catalog.ts`
- Create: `apps/frontend/ecommerce/hooks/useProducts.ts`
- Create: `apps/frontend/ecommerce/hooks/useProduct.ts`
- Modify: `apps/frontend/ecommerce/app/(public)/catalog/page.tsx`
- Modify: `apps/frontend/ecommerce/app/(public)/products/[id]/page.tsx`
- Modify: `apps/frontend/ecommerce/components/catalog/ProductGrid/ProductGrid.tsx`
- Test: `apps/frontend/ecommerce/hooks/__tests__/useProducts.test.tsx`

**Interfaces:**
- Consumes: `apiClient` from Task 4 (`lib/api/client.ts`)
- Produces: `fetchProducts(): Promise<Product[]>`, `fetchProduct(id: string): Promise<Product>`, `useProducts(): UseQueryResult<Product[]>`, `useProduct(id: string): UseQueryResult<Product>`

> **Scope decision:** the Catalog Service response (`ProductResponse`) has no `stars`/`reviews`/
> `tag`/`installments` fields (documented gap in `docs/integration/frontend-backend-integration.md`
> §4). `fetchProducts`/`fetchProduct` fill those with fixed defaults (`stars: 5, reviews: 0,
> installments: 1`, no `tag`) so existing UI components keep compiling without special-casing —
> this is intentionally temporary mock-filling, not a new backend field.
>
> **Pagination decision:** the Catalog Service endpoint is paginated (`Page<ProductResponse>`),
> but `ProductGrid`'s existing filter/sort/paginate logic is client-side over the full catalog.
> To avoid rewriting that logic in this integration phase, `fetchProducts` requests `size=100`
> (all products fit in one backend page for this project's scale) and returns `page.content`
> mapped to `Product[]`. Revisit true server-side pagination when the catalog grows.

- [ ] **Step 1: Write the failing test for `useProducts`**

```tsx
import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useProducts } from '../useProducts'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useProducts', () => {
    it('maps a Page<ProductResponse> into Product[]', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/catalog/products', { params: { size: 100 } }).reply(200, {
            content: [
                {
                    id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
                    name: 'Filtro de Ar K&N',
                    description: 'Filtro esportivo',
                    price: 299.9,
                    stockQuantity: 10,
                    categoryId: 'cat-1',
                    categoryName: 'Filtros',
                    status: 'ACTIVE',
                    images: [{ id: 'img-1', url: '/catalog-images/x.jpg', primary: true }],
                },
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 100,
        })

        const { result } = renderHook(() => useProducts(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data).toHaveLength(1)
        expect(result.current.data?.[0].id).toBe('3fa85f64-5717-4562-b3fc-2c963f66afa6')
        expect(result.current.data?.[0].inStock).toBe(true)
    })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd apps/frontend/ecommerce && npx vitest run hooks/__tests__/useProducts.test.tsx`
Expected: FAIL — `../useProducts` doesn't exist

- [ ] **Step 3: Implement `lib/api/catalog.ts`**

```ts
import { apiClient } from './client'
import type { Product } from '@/types/product'

type ProductImageResponse = {
    id: string
    url: string
    primary: boolean
}

type ProductResponse = {
    id: string
    name: string
    description: string
    price: number
    stockQuantity: number
    categoryId: string
    categoryName: string
    status: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK'
    images: ProductImageResponse[]
}

type PageResponse<T> = {
    content: T[]
    totalElements: number
    totalPages: number
    number: number
    size: number
}

const DEFAULT_PAGE_SIZE = 100

function toProduct(response: ProductResponse): Product {
    return {
        id: response.id,
        name: response.name,
        brand: response.categoryName,
        price: response.price,
        tagColor: '',
        stars: 5,
        reviews: 0,
        installments: 1,
        inStock: response.status === 'ACTIVE' && response.stockQuantity > 0,
        description: response.description,
        specs: {},
        category: response.categoryName,
        images: response.images.map((img) => img.url),
    }
}

export async function fetchProducts(): Promise<Product[]> {
    const { data } = await apiClient.get<PageResponse<ProductResponse>>('/api/v1/catalog/products', {
        params: { size: DEFAULT_PAGE_SIZE },
    })
    return data.content.map(toProduct)
}

export async function fetchProduct(id: string): Promise<Product> {
    const { data } = await apiClient.get<ProductResponse>(`/api/v1/catalog/products/${id}`)
    return toProduct(data)
}
```

- [ ] **Step 4: Implement `hooks/useProducts.ts`**

```ts
import { useQuery } from '@tanstack/react-query'
import { fetchProducts } from '@/lib/api/catalog'

export function useProducts() {
    return useQuery({
        queryKey: ['products'],
        queryFn: fetchProducts,
    })
}
```

- [ ] **Step 5: Implement `hooks/useProduct.ts`**

```ts
import { useQuery } from '@tanstack/react-query'
import { fetchProduct } from '@/lib/api/catalog'

export function useProduct(id: string) {
    return useQuery({
        queryKey: ['products', id],
        queryFn: () => fetchProduct(id),
        enabled: Boolean(id),
    })
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd apps/frontend/ecommerce && npx vitest run hooks/__tests__/useProducts.test.tsx`
Expected: PASS

- [ ] **Step 7: Update `ProductGrid.tsx` to accept fetched data instead of a static prop default**

Change the exported function signature's usage site (Step 8) rather than `ProductGrid` itself —
`ProductGrid` already takes `allProducts: Product[]` as a prop, so no internal change is needed;
only its caller (`catalog/page.tsx`) changes.

- [ ] **Step 8: Update `app/(public)/catalog/page.tsx` to use `useProducts`**

```tsx
'use client'

import { useProducts } from '@/hooks/useProducts'
import { ProductGrid } from '@/components/catalog/ProductGrid/ProductGrid'
import Link from 'next/link'
import styles from './page.module.css'

export default function CatalogPage() {
    const { data: products, isLoading, isError } = useProducts()

    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>Catálogo</span>
            </nav>

            <h1 className={styles.title}>CATÁLOGO</h1>

            {isLoading && <p>Carregando produtos...</p>}
            {isError && <p>Não foi possível carregar os produtos.</p>}
            {products && <ProductGrid allProducts={products} />}
        </div>
    )
}
```

(This page was a server component before; switching to `useProducts` makes it a client component —
hence the added `'use client'` directive.)

- [ ] **Step 9: Update `app/(public)/products/[id]/page.tsx` to use `useProduct`**

```tsx
'use client'

import { useParams, notFound } from 'next/navigation'
import { useProduct } from '@/hooks/useProduct'
import { ImageGallery } from '@/components/product/ImageGallery/ImageGallery'
import { ProductInfo } from '@/components/product/ProductInfo/ProductInfo'
import { ProductTabs } from '@/components/product/ProductTabs/ProductTabs'
import Link from 'next/link'
import styles from './page.module.css'

export default function ProductPage() {
    const params = useParams<{ id: string }>()
    const { data: product, isLoading, isError } = useProduct(params.id)

    if (isError) notFound()
    if (isLoading || !product) return <p>Carregando produto...</p>

    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <Link href="/catalog" className={styles.breadcrumbLink}>
                    Catálogo
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>{product.name}</span>
            </nav>

            <div className={styles.productGrid}>
                <ImageGallery images={product.images} name={product.name} />
                <ProductInfo product={product} />
            </div>

            <ProductTabs product={product} />
        </div>
    )
}
```

Remove the now-unused `generateStaticParams` export and the `lib/data/products` import from this
file — static params don't apply once data is fetched client-side.

- [ ] **Step 10: Run type-check and lint**

Run: `cd apps/frontend/ecommerce && npm run type-check && npm run lint`
Expected: both pass

- [ ] **Step 11: Manual verification**

Run: `docker compose -f infra/docker-compose.yml up -d postgres-catalog redis minio minio-init` then
start Catalog Service and `npm run dev` in the frontend. Open `http://localhost:3000/catalog` —
confirm the product grid loads from the real Catalog Service (check Network tab for
`GET /api/v1/catalog/products`), then click into a product and confirm
`GET /api/v1/catalog/products/{id}` fires.

- [ ] **Step 12: Commit**

```bash
git add apps/frontend/ecommerce/lib/api/catalog.ts apps/frontend/ecommerce/hooks apps/frontend/ecommerce/app/\(public\)/catalog apps/frontend/ecommerce/app/\(public\)/products
git commit -m "feat(frontend): fetch catalog and product detail from Catalog Service via React Query"
```

---

## Task 7: Frontend — real authentication (session hook + authStore + login page)

**Files:**
- Create: `apps/frontend/ecommerce/lib/api/auth.ts`
- Create: `apps/frontend/ecommerce/hooks/useSession.ts`
- Modify: `apps/frontend/ecommerce/store/authStore.ts`
- Modify: `apps/frontend/ecommerce/app/(public)/login/page.tsx`
- Test: `apps/frontend/ecommerce/hooks/__tests__/useSession.test.tsx`

**Interfaces:**
- Consumes: `apiClient` (Task 4), RFC 7807 error shape `{ type, title, status, detail, instance }`
- Produces: `login(email, password): Promise<void>`, `logout(): Promise<void>`, `useSession(): UseQueryResult<SessionUser | null>`

- [ ] **Step 1: Write the failing test for `useSession`**

```tsx
import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useSession } from '../useSession'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useSession', () => {
    it('returns the logged-in user when the session cookie is valid', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/users/me').reply(200, {
            id: 'u1', name: 'Carlos Silva', email: 'carlos@email.com', phone: '(11) 99999-9999', cpf: '123.456.789-00',
        })

        const { result } = renderHook(() => useSession(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data?.email).toBe('carlos@email.com')
    })

    it('returns null when there is no valid session (401)', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/users/me').reply(401)

        const { result } = renderHook(() => useSession(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data).toBeNull()
    })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd apps/frontend/ecommerce && npx vitest run hooks/__tests__/useSession.test.tsx`
Expected: FAIL — `../useSession` doesn't exist

- [ ] **Step 3: Implement `lib/api/auth.ts`**

```ts
import { apiClient } from './client'

export type SessionUser = {
    id: string
    name: string
    email: string
    phone: string
    cpf: string
}

export async function fetchSession(): Promise<SessionUser | null> {
    try {
        const { data } = await apiClient.get<SessionUser>('/api/v1/users/me')
        return data
    } catch (error: any) {
        if (error.response?.status === 401) {
            return null
        }
        throw error
    }
}

export async function login(email: string, password: string): Promise<void> {
    await apiClient.post('/api/v1/auth/login', { email, password })
}

export async function logout(): Promise<void> {
    await apiClient.post('/api/v1/auth/logout')
}
```

- [ ] **Step 4: Implement `hooks/useSession.ts`**

```ts
import { useQuery } from '@tanstack/react-query'
import { fetchSession } from '@/lib/api/auth'

export function useSession() {
    return useQuery({
        queryKey: ['session'],
        queryFn: fetchSession,
    })
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd apps/frontend/ecommerce && npx vitest run hooks/__tests__/useSession.test.tsx`
Expected: PASS

- [ ] **Step 6: Rewrite `authStore.ts` to derive state from the session query**

```ts
import { create } from 'zustand'
import type { SessionUser } from '@/lib/api/auth'

type AuthStore = {
    user: SessionUser | null
    setUser: (user: SessionUser | null) => void
}

export const useAuthStore = create<AuthStore>((set) => ({
    user: null,
    setUser: (user) => set({ user }),
}))
```

`isLoggedIn`/`login`/`logout` are removed from the store — components read auth state from
`useSession()` (React Query, source of truth for server state) and sync it into `useAuthStore`
via a `setUser` effect where a global read is needed outside of a component with query access
(e.g. `Header`). `login`/`logout` actions live in `lib/api/auth.ts` and are called as mutations.

- [ ] **Step 7: Update the existing `authStore` test**

Update `apps/frontend/ecommerce/store/__tests__/authStore.test.ts` to test only `setUser`:

```ts
import { describe, it, expect } from 'vitest'
import { useAuthStore } from '../authStore'

describe('authStore', () => {
    it('setUser updates the stored user', () => {
        const user = { id: 'u1', name: 'Carlos Silva', email: 'carlos@email.com', phone: '', cpf: '' }
        useAuthStore.getState().setUser(user)
        expect(useAuthStore.getState().user).toEqual(user)

        useAuthStore.getState().setUser(null)
        expect(useAuthStore.getState().user).toBeNull()
    })
})
```

- [ ] **Step 8: Update the login page to call the real login mutation**

```tsx
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { login } from '@/lib/api/auth'
import styles from './page.module.css'

export default function LoginPage() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const router = useRouter()
    const queryClient = useQueryClient()

    const loginMutation = useMutation({
        mutationFn: () => login(email, password),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: ['session'] })
            router.push('/account')
        },
        onError: (error: any) => {
            setErrorMessage(error.response?.data?.detail ?? 'Falha ao entrar. Verifique suas credenciais.')
        },
    })

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        setErrorMessage(null)
        loginMutation.mutate()
    }

    return (
        <div className={styles.pageWrapper}>
            <div className={styles.formCard}>
                <div className={styles.logoArea}>
                    <p className={styles.logoTitle}>AUTOHUB</p>
                    <p className={styles.logoSubtitle}>STORE</p>
                </div>

                <h1 className={styles.heading}>ENTRAR NA CONTA</h1>

                <form onSubmit={handleSubmit} className={styles.form}>
                    <div>
                        <label className={styles.fieldLabel}>E-MAIL</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="seu@email.com"
                            required
                            className={styles.fieldInput}
                        />
                    </div>

                    <div>
                        <label className={styles.fieldLabel}>SENHA</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            required
                            className={styles.fieldInput}
                        />
                    </div>

                    {errorMessage && <p role="alert">{errorMessage}</p>}

                    <button type="submit" className={styles.submitButton} disabled={loginMutation.isPending}>
                        {loginMutation.isPending ? 'ENTRANDO...' : 'ENTRAR'}
                    </button>
                </form>

                <p className={styles.registerText}>
                    Não tem conta?{' '}
                    <a href="/login" className={styles.registerLink}>
                        Criar conta grátis
                    </a>
                </p>
            </div>
        </div>
    )
}
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `cd apps/frontend/ecommerce && npx vitest run store/__tests__/authStore.test.ts hooks/__tests__/useSession.test.tsx`
Expected: PASS

- [ ] **Step 10: Run type-check and lint**

Run: `cd apps/frontend/ecommerce && npm run type-check && npm run lint`
Expected: both pass — this will surface any component still calling the removed `login()`/`logout()`/`isLoggedIn` from `authStore` (e.g. `Header.tsx`, `AccountSidebar.tsx`); update each call site to use `useSession()` + the `logout` mutation from `lib/api/auth.ts` following the same pattern as Step 8.

- [ ] **Step 11: Manual verification**

With Auth Service, User Service, Gateway and frontend all running: log in via the UI, confirm
`document.cookie` shows no `access_token` (httpOnly, invisible to JS) but subsequent requests to
`/account` succeed; log out and confirm `/account` redirects/fails.

- [ ] **Step 12: Commit**

```bash
git add apps/frontend/ecommerce/lib/api/auth.ts apps/frontend/ecommerce/hooks/useSession.ts apps/frontend/ecommerce/store/authStore.ts apps/frontend/ecommerce/store/__tests__/authStore.test.ts apps/frontend/ecommerce/app/\(public\)/login
git commit -m "feat(frontend): replace mock auth with real login/session against Auth Service"
```

---

## Task 8: Frontend — admin product creation with 2-step image upload

**Files:**
- Create: `apps/frontend/ecommerce/lib/api/adminCatalog.ts`
- Create: `apps/frontend/ecommerce/hooks/useCreateProduct.ts`
- Create: `apps/frontend/ecommerce/hooks/useUploadProductImages.ts`
- Create: `apps/frontend/ecommerce/components/admin/ProductForm/ProductForm.tsx`
- Create: `apps/frontend/ecommerce/components/admin/ProductForm/ProductForm.module.css`
- Create: `apps/frontend/ecommerce/components/admin/ProductImageUpload/ProductImageUpload.tsx`
- Create: `apps/frontend/ecommerce/components/admin/ProductImageUpload/ProductImageUpload.module.css`
- Modify: `apps/frontend/ecommerce/app/(admin)/admin/products/page.tsx`
- Test: `apps/frontend/ecommerce/hooks/__tests__/useCreateProduct.test.tsx`

**Interfaces:**
- Consumes: `apiClient` (Task 4)
- Produces: `createProduct(input: CreateProductInput): Promise<{ id: string }>`, `uploadProductImages(productId: string, files: File[]): Promise<ProductImageResponse[]>`

- [ ] **Step 1: Write the failing test for `useCreateProduct`**

```tsx
import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useCreateProduct } from '../useCreateProduct'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useCreateProduct', () => {
    it('posts product data and returns the created id', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onPost('/api/v1/catalog/products').reply(201, { id: 'new-product-id' })

        const { result } = renderHook(() => useCreateProduct(), { wrapper })

        result.current.mutate({
            name: 'Rodas Aro 18', description: 'desc', price: 100, stockQuantity: 5, categoryId: 'cat-1',
        })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data?.id).toBe('new-product-id')
    })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd apps/frontend/ecommerce && npx vitest run hooks/__tests__/useCreateProduct.test.tsx`
Expected: FAIL — `../useCreateProduct` doesn't exist

- [ ] **Step 3: Implement `lib/api/adminCatalog.ts`**

```ts
import { apiClient } from './client'

export type CreateProductInput = {
    name: string
    description: string
    price: number
    stockQuantity: number
    categoryId: string
}

export type ProductImageResponse = {
    id: string
    url: string
    primary: boolean
}

export async function createProduct(input: CreateProductInput): Promise<{ id: string }> {
    const { data } = await apiClient.post<{ id: string }>('/api/v1/catalog/products', input)
    return data
}

export async function uploadProductImages(productId: string, files: File[]): Promise<ProductImageResponse[]> {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))

    const { data } = await apiClient.post<ProductImageResponse[]>(
        `/api/v1/catalog/products/${productId}/images`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    return data
}
```

- [ ] **Step 4: Implement `hooks/useCreateProduct.ts`**

```ts
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createProduct } from '@/lib/api/adminCatalog'

export function useCreateProduct() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: createProduct,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['products'] })
        },
    })
}
```

- [ ] **Step 5: Implement `hooks/useUploadProductImages.ts`**

```ts
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { uploadProductImages } from '@/lib/api/adminCatalog'

export function useUploadProductImages(productId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (files: File[]) => uploadProductImages(productId, files),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['products', productId] })
        },
    })
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd apps/frontend/ecommerce && npx vitest run hooks/__tests__/useCreateProduct.test.tsx`
Expected: PASS

- [ ] **Step 7: Implement `ProductImageUpload` component (step 2 of the wizard)**

```tsx
'use client'

import { useState } from 'react'
import { useUploadProductImages } from '@/hooks/useUploadProductImages'
import styles from './ProductImageUpload.module.css'

const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024

export function ProductImageUpload({ productId, onDone }: { productId: string; onDone: () => void }) {
    const [files, setFiles] = useState<File[]>([])
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const uploadMutation = useUploadProductImages(productId)

    function handleFilesSelected(e: React.ChangeEvent<HTMLInputElement>) {
        const selected = Array.from(e.target.files ?? [])
        const oversized = selected.find((file) => file.size > MAX_FILE_SIZE_BYTES)
        if (oversized) {
            setErrorMessage(`Arquivo "${oversized.name}" excede 5MB.`)
            return
        }
        setErrorMessage(null)
        setFiles(selected)
    }

    function handleUpload() {
        uploadMutation.mutate(files, { onSuccess: onDone })
    }

    return (
        <div className={styles.wrapper}>
            <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                onChange={handleFilesSelected}
                className={styles.fileInput}
            />
            {errorMessage && <p className={styles.error}>{errorMessage}</p>}
            <button
                onClick={handleUpload}
                disabled={files.length === 0 || uploadMutation.isPending}
                className={styles.uploadButton}
            >
                {uploadMutation.isPending ? 'ENVIANDO...' : 'ENVIAR IMAGENS'}
            </button>
            <button onClick={onDone} className={styles.skipButton}>
                Pular por enquanto
            </button>
        </div>
    )
}
```

```css
.wrapper {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.fileInput {
    border: 1px dashed #999;
    padding: 1rem;
}

.error {
    color: #c0392b;
}

.uploadButton,
.skipButton {
    padding: 0.5rem 1rem;
}
```

- [ ] **Step 8: Implement `ProductForm` component (step 1 of the wizard)**

```tsx
'use client'

import { useState } from 'react'
import { useCreateProduct } from '@/hooks/useCreateProduct'
import { ProductImageUpload } from '../ProductImageUpload/ProductImageUpload'
import styles from './ProductForm.module.css'

export function ProductForm({ onClose }: { onClose: () => void }) {
    const [name, setName] = useState('')
    const [description, setDescription] = useState('')
    const [price, setPrice] = useState('')
    const [stockQuantity, setStockQuantity] = useState('')
    const [categoryId, setCategoryId] = useState('')
    const [createdProductId, setCreatedProductId] = useState<string | null>(null)
    const createProductMutation = useCreateProduct()

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        createProductMutation.mutate(
            { name, description, price: Number(price), stockQuantity: Number(stockQuantity), categoryId },
            { onSuccess: (result) => setCreatedProductId(result.id) }
        )
    }

    if (createdProductId) {
        return <ProductImageUpload productId={createdProductId} onDone={onClose} />
    }

    return (
        <form onSubmit={handleSubmit} className={styles.form}>
            <label className={styles.field}>
                Nome
                <input value={name} onChange={(e) => setName(e.target.value)} required />
            </label>
            <label className={styles.field}>
                Descrição
                <textarea value={description} onChange={(e) => setDescription(e.target.value)} />
            </label>
            <label className={styles.field}>
                Preço
                <input type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />
            </label>
            <label className={styles.field}>
                Estoque
                <input type="number" value={stockQuantity} onChange={(e) => setStockQuantity(e.target.value)} required />
            </label>
            <label className={styles.field}>
                Categoria (ID)
                <input value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required />
            </label>
            {createProductMutation.isError && <p className={styles.error}>Falha ao criar produto.</p>}
            <button type="submit" disabled={createProductMutation.isPending} className={styles.submitButton}>
                {createProductMutation.isPending ? 'CRIANDO...' : 'CRIAR PRODUTO'}
            </button>
        </form>
    )
}
```

```css
.form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    max-width: 480px;
}

.field {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.error {
    color: #c0392b;
}

.submitButton {
    padding: 0.5rem 1rem;
}
```

- [ ] **Step 9: Wire the form into the admin products page**

```tsx
'use client'

import { useState } from 'react'
import { ProductsTable } from '@/components/admin/ProductsTable/ProductsTable'
import { ProductForm } from '@/components/admin/ProductForm/ProductForm'
import styles from './page.module.css'

export default function AdminProductsPage() {
    const [isCreating, setIsCreating] = useState(false)

    return (
        <div>
            <div className={styles.header}>
                <h1 className={styles.title}>PRODUTOS</h1>
                <button className={styles.addButton} onClick={() => setIsCreating(true)}>
                    + NOVO PRODUTO
                </button>
            </div>
            {isCreating && <ProductForm onClose={() => setIsCreating(false)} />}
            <ProductsTable />
        </div>
    )
}
```

- [ ] **Step 10: Run type-check and lint**

Run: `cd apps/frontend/ecommerce && npm run type-check && npm run lint`
Expected: both pass

- [ ] **Step 11: Manual verification**

With Catalog Service + MinIO running, open `/admin/products`, click "+ NOVO PRODUTO", fill the
form, submit — confirm `POST /api/v1/catalog/products` fires and the image-upload step appears;
upload one JPG and confirm `POST /api/v1/catalog/products/{id}/images` fires and the MinIO
console (`http://localhost:9001`) shows the object under `catalog-images`.

- [ ] **Step 12: Commit**

```bash
git add apps/frontend/ecommerce/lib/api/adminCatalog.ts apps/frontend/ecommerce/hooks/useCreateProduct.ts apps/frontend/ecommerce/hooks/useUploadProductImages.ts apps/frontend/ecommerce/components/admin/ProductForm apps/frontend/ecommerce/components/admin/ProductImageUpload apps/frontend/ecommerce/app/\(admin\)/admin/products/page.tsx
git commit -m "feat(frontend): add admin product creation with 2-step image upload"
```

---

## Final gate check

- [ ] Update `docs/planning/action-plan.md` Fase 2.5 checklist (already added) — mark it complete
      once all 8 tasks above are merged.
- [ ] Only after this: resume Fase 3 (Catalog Service extras / Search Service) per `action-plan.md`.
