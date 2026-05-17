# Style Guide

## Kotlin (General)

- Indentation: 4 spaces and no tabs.
- Prefer immutable `val` and pure functions. Keep mutation localized.
- Favor explicit, descriptive names (`findActiveTickets`, not `getTickets`).
- Use Kotlin nullability intentionally. Avoid `!!`. Use `requireNotNull`/`error` with clear messages.
- Keep constructors small. Prefer dependency injection via constructor parameters.

## Architecture & Layering

- `admin/*` and `captive/*` follow a ports/usecases pattern:
  - Web/UI (`*/web`): controllers, request DTOs, template composition.
  - Application (`*/application`): usecases, commands/queries, ports.
  - Core (`*/core`): domain aggregates/value objects, domain exceptions.
  - Persistence (`*/persistence`): JPA entities + adapters implementing ports.
- Controllers should orchestrate HTTP concerns only (params, redirects, HTMX partials). Business rules live in usecases.

## Spring MVC + Thymeleaf

- Template paths:
  - Pages: `admin/web/src/main/resources/templates/admin/*.html`
  - Fragments: `admin/web/src/main/resources/templates/admin/fragments/*.html`
- Prefer fragments for HTMX swaps (`th:fragment="…"`) and return fragment views from controllers.
- Use message bundles for user-facing strings:
  - `admin/web/src/main/resources/messages.properties`
  - `admin/web/src/main/resources/messages_cs.properties`

## HTMX Conventions

- Prefer server-driven updates: return a fragment and swap it into a stable container element.
- Detect HTMX requests via `io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest` (avoid manual header parsing).
- Use `hx-*` attributes (not `data-hx-*`) in Thymeleaf templates. Prefer `th:hx-get`/`th:hx-post` (or `th:attr` for dynamic attrs).

## Tailwind/CSS

- Source CSS lives in `*/web/src/main/frontend/*.css` and shared utilities in `shared/ui/tailwind/shared.css`.
- Implement reusable patterns with Tailwind utilities and `@apply` (in Tailwind source files).
- Generated outputs (e.g. `*/web/src/main/resources/static/assets/*/*.css`) are build artifacts. Update them by running `./gradlew buildCss` (or `./gradlew watchCss` during development).

## Formatting & Linting

- Use Spotless + ktlint:
  - `./gradlew spotlessApply` before opening a PR.
  - `./gradlew spotlessCheck` to verify.
