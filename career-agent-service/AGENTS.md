# Career Agent Service â€” Development Rules

These rules apply to ALL code changes in the backend service. Follow them in every session without exception.

## Architecture Rules

1. **Always follow the layered architecture: Controller â†’ Service â†’ Repository.** Never bypass the service layer. Controllers must NOT directly call repositories. Business logic lives in services, not controllers.

2. **Controllers** handle HTTP concerns only: request validation, response mapping, status codes. No business logic.

3. **Services** contain all business logic, transaction management, and orchestration. They call repositories and other services.

4. **Repositories** are Spring Data JPA interfaces only. No custom SQL in controllers or services â€” use repository methods or `@Query`.

## Lombok Rules

5. **Use `@RequiredArgsConstructor`** instead of writing explicit constructors. Declare dependencies as `private final` fields.

6. **Use `@Slf4j`** instead of manual `LoggerFactory.getLogger()` declarations.

7. **Use `@Getter`** on exception classes that have fields (e.g., `PasswordValidationException`).

8. **Use `@Builder`** on JPA entities with `@NoArgsConstructor` and `@AllArgsConstructor`.

9. **Do NOT use `@Data`** on JPA entities â€” it generates `equals`/`hashCode` based on all fields which causes Hibernate issues. Use `@Getter @Setter` instead.

10. **Exception:** Classes that need `@Value` annotation on constructor parameters (e.g., reading from `application.yml`) must keep explicit constructors. `@RequiredArgsConstructor` does not support `@Value` params.

## Comment Rules

11. **Add a one-line class comment** on every class, interface, and enum: `/** Handles candidate registration and login. */`

12. **Add a one-line method comment** on every public method: `/** Registers a new candidate and returns a JWT token. */`

13. **Do NOT include `@param`, `@return`, `@throws`** in comments. Keep it to a single descriptive sentence.

14. **Do NOT add comments on getters, setters, or trivial methods.** Only comment methods that do something meaningful.

## Code Style

15. **Use Java records** for all DTOs (request/response objects). No Lombok on DTOs â€” records are cleaner.

16. **Use enums** for fixed value sets (status, type, preference). Store as `@Enumerated(EnumType.STRING)` in JPA.

17. **Sanitize all text inputs** through `ValidationService.sanitizeText()` before persisting. No raw user input in the database.

18. **Use `@Transactional`** on service methods that modify multiple entities. Controllers should NOT have `@Transactional`.

19. **Extract candidateId from JWT** using `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`. Every protected endpoint must verify ownership.

## Java Modernization Rules

20. **Use lambda expressions** and functional programming style (streams, Optional, method references) wherever they improve readability. Prefer `list.stream().map(...).toList()` over manual loops, `Optional.map()` over null checks, and `forEach` over indexed iteration.

21. **Use the latest Java APIs** available in Java 25: records, sealed classes, pattern matching (`instanceof` with binding), switch expressions, text blocks, `List.of()`, `Map.of()`, `Stream.toList()`, virtual threads where applicable. Avoid deprecated or legacy APIs (`Vector`, `Hashtable`, `Date`/`Calendar` — use `java.time` instead).

22. **Use the latest Spring Boot 4.1 and Spring Framework 7 features**: constructor injection via `@RequiredArgsConstructor`, `@HttpExchange` for declarative HTTP clients, `RestClient` over `RestTemplate`, Spring AI 2.0.1 ChatClient builder pattern, and `@ConfigurationProperties` with records where possible.

## Testing Rules

23. **Property-based tests** use jqwik `@Property` annotation with `@Label` matching the design doc property.

24. **Unit tests** use JUnit 5 + Mockito + AssertJ. No Spring context needed for unit tests.

25. **Run `mvn compile` after every code change** to verify compilation before claiming completion.

26. **Run `mvn test` after implementation** to verify all existing tests still pass.

## Git Rules

27. **Provide a GitHub commit message** at the end of every session summarizing all changes. Format:
    ```
    type(scope): short description

    - bullet point per significant change
    - include files created/modified
    ```

28. **Never commit `.env` files.** Only `.env.example`.

## Package Structure

```
com.careeragent/
â”œâ”€â”€ api/              # Controllers, DTOs, exceptions, GlobalExceptionHandler
â”œâ”€â”€ service/          # Business logic services
â”œâ”€â”€ domain/           # JPA entities, enums
â”œâ”€â”€ repository/       # Spring Data JPA repositories
â”œâ”€â”€ infrastructure/   # Security, config, observability, LLM
â”œâ”€â”€ integration/      # External systems (storage, portal, email, browser, vector, okf)
â”œâ”€â”€ agent/            # AI agents (profile, matching, job analysis, application)
â”œâ”€â”€ workflow/         # Workflow engine
â”œâ”€â”€ scheduler/        # Scheduled tasks
â””â”€â”€ tool/             # Spring AI tools
```
