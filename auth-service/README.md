# auth-service — Identity & JWT Issuance

**Port 8081** · H2 (dev) / Postgres `authdb` (prod)

Owns users and mints JWTs. Every other service only *verifies* tokens; this is
the single place that *creates* them.

## Showcases

- **JWT issuance** — [`JwtService`](src/main/java/com/shopflow/auth/security/JwtService.java): claims, expiry, HS256 (and why prod wants RS256)
- **Spring Security 6** — [`SecurityConfig`](src/main/java/com/shopflow/auth/security/SecurityConfig.java): SecurityFilterChain lambda DSL, stateless sessions, why CSRF is off, BCrypt
- **Request validation** — [`RegisterRequest`](src/main/java/com/shopflow/auth/web/dto/RegisterRequest.java) + [`GlobalExceptionHandler`](src/main/java/com/shopflow/auth/web/GlobalExceptionHandler.java) turning violations into clean 400s
- **JPA / Spring Data JPA** — [`AppUser`](src/main/java/com/shopflow/auth/domain/AppUser.java) entity, derived queries in [`UserRepository`](src/main/java/com/shopflow/auth/repo/UserRepository.java)
- **Spring profiles** — [`application.yml`](src/main/resources/application.yml): dev=H2 / prod=Postgres in one file via `---` documents

## Endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/register` | 201 + JWT |
| POST | `/api/v1/auth/login` | 200 + JWT |

## Poke it

```bash
curl -s -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"dev@shopflow.io","password":"secret123","fullName":"Dev User"}'

# validation demo: bad email + short password -> 400 with field map
curl -s -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"nope","password":"x","fullName":""}'
```

Decode the returned token at [jwt.io](https://jwt.io) to see the claims.
