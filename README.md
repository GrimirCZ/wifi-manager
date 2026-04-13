# WiFi Manager

WiFi Manager is a Kotlin/Spring Boot application for managing Wi-Fi access through an admin interface and a captive portal.

## Features

- Ticket-based Wi-Fi access management
- Admin UI for creating and ending access tickets
- Captive portal for user login and device authorization
- Allowed MAC address management
- Device tracking and authorization history
- OIDC login for admin access
- Router-agent integration for allowing and revoking network access

## Architecture

The application is built as a modular monolith. Modules are split by bounded context (`admin`, `captive`, `user`) with `shared` for cross-cutting concerns and each context follows Clean Architecture with separate domain, application, adapter, and integration layers.

## Modules

- `app/` contains the runnable shell Spring Boot application and local development configuration.
- `admin/` contains the staff-facing ticketing and management functionality. 
- `captive/` contains the captive portal and device authorization flow.
- `user/` contains user identity and directory support.
- `shared/` contains shared libraries, events, security, and UI assets.
- `e2e/` contains end-to-end test suite.
- `routeragent/` contains the standalone Go router agent.

## Requirements

You need JDK 21, Docker, npm and Go 1.25.

## Local development

Start local infrastructure:

```bash
docker compose -f app/compose.yml up -d
```

This starts PostgreSQL on `localhost:5432` and Keycloak on `http://localhost:8081`.

Run the application:

```bash
./gradlew run
```

On Windows:

```powershell
.\gradlew.bat run
```

The application runs on `http://localhost:8080`.

Keycloak imports the dev realm from `app/keycloak/import/wifimanager-realm.json`. Default admin credentials are `admin` / `admin`.

## Commands

Build and test:

```bash
./gradlew bootJar
./gradlew check
./gradlew :e2e:screenshotTest
```

Format:

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
```

CSS:

```bash
./gradlew buildCss
./gradlew watchCss # to reload in real-time in development
```

## Configuration

Main application configuration lives in `app/src/main/resources/application.yml`. Development overrides are in `app/src/main/resources/application-dev.yml`.

## Notes

Use the Gradle wrapper instead of a system Gradle installation. Do not commit secrets; keep OIDC and client credentials in local configuration or environment variables.
