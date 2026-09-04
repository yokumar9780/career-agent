# Career Agent Service — Development Rules

These rules apply to ALL code changes in the backend service. Follow them in every session without exception.

## Architecture Rules

1. **Always follow the layered architecture: Controller → Service → Repository.** Never bypass the service layer. Controllers must NOT directly call repositories. Business logic lives in services, not controllers.

2. **Controllers** handle HTTP concerns only: request validation, response mapping, status codes. No business logic.

3. **Services** contain all business logic, transaction management, and orchestration. They call repositories and other services.

4. **Repositories** are Spring Data JPA interfaces only. No custom SQL in controllers or services — use repository methods or `@Query`.

## Lombok Rules

5. **Use `@RequiredArgsConstructor`** instead of writing explicit constructors. Declare dependencies as `private final` fields.

6. **Use `@Slf4j`** instead of manual `LoggerFactory.getLogger()` declarations.

7. **Use `@Getter`** on exception classes that have fields (e.g., `PasswordValidationException`).

8. **Use `@Builder`** on JPA entities with `@NoArgsConstructor` and `@AllArgsConstructor`.

9. **Do NOT use `@Data`** on JPA entities — it generates `equals`/`hashCode` based on all fields which causes Hibernate issues. Use `@Getter @Setter` instead.

10. **Exception:** Classes that need `@Value` annotation on constructor parameters (e.g., reading from `application.yml`) must keep explicit constructors. `@RequiredArgsConstructor` does not support `@Value` params.

## Comment Rules

11. **Add a one-line class comment** on every class, interface, and enum: `/** Handles candidate registration and login. */`

12. **Add a one-line method comment** on every public method: `/** Registers a new candidate and returns a JWT token. */`

13. **Do NOT include `@param`, `@return`, `@throws`** in comments. Keep it to a single descriptive sentence.

14. **Do NOT add comments on getters, setters, or trivial methods.** Only comment methods that do something meaningful.

## Code Style

15. **Use Java records** for all DTOs (request/response objects). No Lombok on DTOs — records are cleaner.

16. **Use enums** for fixed value sets (status, type, preference). Store as `@Enumerated(EnumType.STRING)` in JPA.

17. **Sanitize all text inputs** through `ValidationService.sanitizeText()` before persisting. No raw user input in the database.

18. **Use `@Transactional`** on service methods that modify multiple entities. Controllers should NOT have `@Transactional`.

19. **Extract candidateId from JWT** using `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`. Every protected endpoint must verify ownership.

## Testing Rules

20. **Property-based tests** use jqwik `@Property` annotation with `@Label` matching the design doc property.

21. **Unit tests** use JUnit 5 + Mockito + AssertJ. No Spring context needed for unit tests.

22. **Run `mvn compile` after every code change** to verify compilation before claiming completion.

23. **Run `mvn test` after implementation** to verify all existing tests still pass.

## Git Rules

24. **Provide a GitHub commit message** at the end of every session summarizing all changes. Format:
    ```
    type(scope): short description

    - bullet point per significant change
    - include files created/modified
    ```

25. **Never commit `.env` files.** Only `.env.example`.

## Package Structure

```
com.careeragent/
├── api/              # Controllers, DTOs, exceptions, GlobalExceptionHandler
├── service/          # Business logic services
├── domain/           # JPA entities, enums
├── repository/       # Spring Data JPA repositories
├── infrastructure/   # Security, config, observability, LLM
├── integration/      # External systems (storage, portal, email, browser, vector, okf)
├── agent/            # AI agents (profile, matching, job analysis, application)
├── workflow/         # Workflow engine
├── scheduler/        # Scheduled tasks
└── tool/             # Spring AI tools
```
