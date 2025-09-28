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