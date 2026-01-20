# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0.1 application for "Damo" service with Kakao OAuth login, JWT authentication, and MySQL/Redis persistence.

## Build & Development Commands

```bash
# Build project
./gradlew build

# Run application (local profile)
./gradlew bootRun

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests "com.team8.damo.ClassName.methodName"

# Clean build
./gradlew clean build
```

## Environment Variables

Required environment variables (defined in `application.yaml`):

```bash
# JWT Configuration
JWT_SECRET=<base64-encoded-secret>

# Kakao OAuth
KAKAO_ADMIN=<kakao-admin-key>
KAKAO_CLIENT_ID=<kakao-rest-api-key>
KAKAO_CLIENT_SECRET=<kakao-client-secret>
KAKAO_REDIRECT_URI=<redirect-uri>
```

## Architecture

### Security & Authentication Flow

1. **JWT-based Stateless Authentication**
   - Access token: 15 minutes (stored in `access_token` cookie)
   - Refresh token: 14 days (stored in `refresh_token` cookie)
   - Filter chain: `JwtExceptionFilter` → `JwtAuthenticationFilter` → `UsernamePasswordAuthenticationFilter`

2. **Kakao OAuth Login Flow**
   - `KakaoUtil.getAccessToken(code)`: Exchange authorization code for Kakao access token
   - `KakaoUtil.getUserInfo(token)`: Fetch user info from Kakao API
   - `AuthService.oauthLogin(code)`: Handle login logic and create JWT tokens
   - Controller adds tokens to HTTP-only cookies

3. **Public Endpoints** (no authentication required):
   - `/api/v1/auth/oauth` - OAuth login
   - `/login` - Login page
   - `/swagger-ui/**`, `/v3/api-docs/**`, `/api-test` - API documentation
   - `/ws-stomp`, `/sub/**`, `/pub/**` - WebSocket endpoints

### Entity Design

- **ID Generation**: Uses Snowflake algorithm (`Snowflake.nextId()`) for distributed unique IDs
- **Auditing**: `BaseTimeEntity` provides `created_at` and `updated_at` via JPA Auditing
- **User Entity**:
  - Supports Kakao OAuth (`provider_id`, `email`)
  - Onboarding flow: BASIC → CHARACTERISTIC → DONE
  - Soft delete pattern (`is_withdraw`, `withdraw_at`)
  - Push notification management (`fcm_token`, `is_push_notification_allowed`)

### Layer Responsibilities

- **Controller**: HTTP request/response handling, cookie management
- **Service**: Business logic, transaction management
- **Repository**: Data access via JPA
- **Util**: Reusable utilities (Snowflake ID generation, Kakao API integration, cookie utilities)

**Important**: Service layer should NOT depend on HTTP-specific classes (`HttpServletRequest`, `HttpServletResponse`, `Cookie`). Controllers handle all HTTP concerns.

## Database

### Profiles

- `local`: MySQL on localhost:3306, Redis on localhost:6379
- `test`: MySQL on localhost:3306 (damo_test database), Redis on localhost:6380

### JPA Configuration

- `ddl-auto: create` - Recreates schema on startup (both local and test)
- P6Spy enabled for SQL logging (local only)
- Swagger UI available at `/api-test`

## Key Technologies

- Spring Boot 4.0.1 (Java 21)
- Spring Security + JWT (jjwt 0.11.5)
- Spring Data JPA + MySQL
- Spring Data Redis
- Lombok
- Swagger/OpenAPI (springdoc)
- P6Spy (SQL logging)

## Common Patterns

### Request DTO Pattern

Follow a two-layer DTO pattern for request handling:

1. **Controller Request DTO** (`controller/request/`)
   - Contains validation annotations (`@NotNull`, `@Email`, etc.)
   - Implements `toServiceRequest()` method for conversion
   - Handles HTTP-layer concerns

2. **Service Request DTO** (`service/request/`)
   - Contains only pure values (no validation annotations)
   - Used by service layer for business logic
   - Decouples service from web layer

**Example**:
```java
// controller/request/LoginRequest.java
@Getter
public class LoginRequest {
    @NotBlank
    private String code;

    public LoginServiceRequest toServiceRequest() {
        return new LoginServiceRequest(code);
    }
}

// service/request/LoginServiceRequest.java
public record LoginServiceRequest(String code) {}

// Controller usage
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    LoginServiceRequest serviceRequest = request.toServiceRequest();
    authService.login(serviceRequest);
    return ResponseEntity.ok().build();
}
```

### Cookie Management

Use `CookieUtil` component (injected, not static) to create HTTP-only cookies:
```java
cookieUtil.addCookie(response, "access", accessToken);
cookieUtil.addCookie(response, "refresh", refreshToken);
```

### Error Handling

- Custom exceptions extend `CustomException` with `ErrorCode`
- `GlobalExceptionHandler` handles exceptions globally
- `ResponseInterceptor` wraps responses in `BaseResponse`

### JPA Entity Best Practices

- Protected no-args constructor
- Use `@Getter` (not `@Data`)
- Explicit column lengths for String fields
- `EnumType.STRING` for enums with explicit length
- Implement `equals()`/`hashCode()` based on ID
- Extend `BaseTimeEntity` for automatic timestamp management
