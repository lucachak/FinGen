# Project Plan: OpenRouter Fix

## Goal
Resolve the HTTP 429 Too Many Requests error arising from rate-limits on OpenRouter's LLaMA 3.3 free tier endpoint.

## Phase 1: Context & Socratic Gate
- Reviewed the `application.properties` which currently uses another key.
- Identified the `/app/ai` controller dependency on `OpenRouterService.java`.
- **Blocked**: Waiting for User to answer Socratic Gate questions on property renaming and fallback models.

## Phase 2: Design & Strategy
- Target File: `application.properties`
  - Update `openrouter.api.key` (or `OPENAI_API_KEY` based on gate).
- Target File: `OpenRouterService.java`
  - Ensure correct JSON error parsing or apply fallback configurations.
- Feature Target: `OpenRouterServiceTest.java` (using `/tdd-workflow`)
  - Simulate OpenRouter network conditions to guarantee test coverage.

## Phase 3: Implementation Detail
- Red-Green-Refactor phase applied to `OpenRouterServiceTest.java`.
- Write the logic for gracefully degrading or retrying in `OpenRouterService.java`.
- Configure the key.

## Phase 4: Verification Checklist
- [ ] Tests passed for rate-limited scenario.
- [ ] `./mvnw clean spring-boot:run` starts app correctly.
- [ ] AI feature on `/app/ai` yields an accurate AI response without 429 errors.
