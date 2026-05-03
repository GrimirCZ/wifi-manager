# WiFi Manager

WiFi Manager is a Kotlin/Spring Boot application for managing Wi-Fi access through an admin interface and a captive
portal.

## Requirements


- [Docker + Docker Compose 29](https://docs.docker.com/desktop/setup/install/windows-install/)
- [JDK 21](https://jdk.java.net/java-se-ri/21)
- [NPM 11](https://nodejs.org/en/download)
- [Go 1.25](https://go.dev/dl/)

## Local development/preview

Start local infrastructure:

```bash
docker compose -f app/compose.yml up -d
```

Install dependencies:

```
npm install
```

Run the application:

- Linux:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
- Windows:
```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

Local services will be running on:

| Service        | Port | Links                         |
|----------------|------|-------------------------------|
| Administration | 8080 | http://localhost:8080/admin   |
| Captive portal | 8080 | http://localhost:8080/captive |

Local test users:

| Username | Password | Role                                                      |
|----------|----------|-----------------------------------------------------------|
| user     | user     | STAFF - can create and manage own tickets and own devices |
| admin    | admin    | ADMIN - can administer the whole system                   |

If you open just http://localhost:8080 you will be redirected to Captive portal because of dev mode subnet settings.

## Features

- Ticket-based Wi-Fi access management
- Admin UI for creating and ending access tickets
- Captive portal for user login and device authorization
- Allowed MAC address management
- Device tracking and authorization history
- OIDC login for admin access
- Router-agent integration for allowing and revoking network access

## Architecture

The application is built as a modular monolith. Modules are split by bounded context (`admin`, `captive`, `user`) with
`shared` for cross-cutting concerns and each context follows Clean Architecture with separate domain, application,
adapter, and integration layers.

## Modules

- `app/` contains the runnable shell Spring Boot application and local development configuration.
- `admin/` contains the staff-facing ticketing and management functionality.
- `captive/` contains the captive portal and device authorization flow.
- `user/` contains user identity and directory support.
- `shared/` contains shared libraries, events, security, and UI assets.
- `e2e/` contains end-to-end test suite.
- `routeragent/` contains the standalone Go router agent.

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

Main application configuration lives in `app/src/main/resources/application.yml`. Development overrides are in
`app/src/main/resources/application-dev.yml`.

Keycloak imports the dev realm from `app/keycloak/import/wifimanager-realm.json`.

Local docker compose starts PostgreSQL on `localhost:5432` and Keycloak on `http://localhost:8081`.

## Notes

Use the Gradle wrapper instead of a system Gradle installation. 
