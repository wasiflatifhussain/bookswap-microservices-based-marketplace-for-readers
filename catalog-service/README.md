```
catalog-service/
└── src/
└── main/
├── java/com/bookswap/catalog/
│    ├── CatalogServiceApplication.java   # main entry
│
│    ├── config/                          # service-level configs
│    │     ├── SecurityConfig.java
│    │     ├── WebConfig.java
│    │     └── OpenApiConfig.java
│
│    ├── controller/                      # REST controllers
│    │     └── BookController.java
│
│    ├── dto/                             # request/response DTOs
│    │     ├── request/
│    │     │     ├── CreateBookRequest.java
│    │     │     └── UpdateBookRequest.java
│    │     └── response/
│    │           └── BookResponse.java
│
│    ├── domain/                          # models & enums
│    │     ├── Book.java
│    │     ├── BookStatus.java
│    │     └── Condition.java
│
│    ├── repository/                      # Spring Data JPA repositories
│    │     └── BookRepository.java
│
│    ├── service/                         # business logic
│    │     ├── BookService.java
│    │     └── BookServiceImpl.java
│
│    ├── external/                     # external service clients
│    │     ├── ValuationClient.java
│    │     └── MediaClient.java
│
│    ├── events/                          # outbox + kafka publisher
│    │     ├── BookEvent.java
│    │     ├── OutboxEntity.java
│    │     └── EventPublisher.java
│
│    ├── security/                        # auth checks, JWT utils
│    │     └── AuthUtil.java
│
│    └── util/                            # helper classes (mappers, constants)
│          └── BookMapper.java
│
└── resources/
├── application.yml
├── bootstrap.yml                    # for Config Server
└── db/migration/
└── V1__init.sql                # Flyway migration
```

## Authentication

All endpoints (except health/docs) require a valid access token.

- Each request must have `Authorization: Bearer <token>`.
- The token is introspected with Keycloak using the backend client.
- Only active tokens are accepted; others get `401/403 Unauthorized`.

See `security/KeycloakIntrospectionFilter.java` for details.