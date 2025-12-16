# Event Storming (Transcription)

This document is a written transcription of `architecture/img.png`. It describes the individual elements (actors, commands, events, policies, external systems) and the end-to-end flows. It is intended as a baseline for further implementation.

## Bounded Contexts / Systems

- **Admin**: staff-facing system for creating and managing tickets and their authorized devices.
- **Captive**: captive portal / authorization system that validates access codes, tracks authorized devices and kicked devices.
- **Router agent**: external system responsible for enforcing network access (allow/revoke) for a client device.

## Actors / Triggers

- **Staff**: human operator performing actions in the Admin system.
- **Ticket expirer (Cron)**: scheduled trigger that ends expired tickets.
- **Captive device user**: client device user authenticating through Captive.

## Domain Concepts

- **Ticket (Admin)**: time-bound authorization ticket (access code, validity window, author, cancellation/ending).
- **Authorization Token (Captive)**: captive-side authorization state for a ticket (access code, authorized devices, kicked MACs).
- **Device**: client device identified primarily by MAC address (optional name/hostname).

## Commands (Intent)

### Admin

- **Create ticket** → emits **Ticket created**
- **Kick client** → emits **Client kicked**
- **End ticket** → emits **Ticket ended**
- **Expire ticket** (Cron) → emits **Ticket ended**
- **Add authorized device** (reaction to Captive) → emits **Authorized device added**

### Captive

- **Create code based authorization token** (reaction to Admin) → emits **Code based authorization token created**
- **Authorize device with code** → emits **Device authorized**
- **Revoke client access** (reaction to Admin) → emits **Client access revoked**
- **Remove authorization token** (reaction to Admin) → emits **Authorization token removed**

## Events (Facts)

- **Ticket created**
- **Code based authorization token created**
- **Device authorized**
- **Authorized device added**
- **Client kicked**
- **Client access revoked**
- **Ticket ended**
- **Authorization token removed**

## Policies (Reactions / Automation)

- **When ticket created, create a code based authorization**
  - Input: `Ticket created`
  - Command: `Create code based authorization token` (Captive)
- **When device authenticated, allow access**
  - Input: `Device authorized`
  - Action: Router agent `Allow client access`
- **On device authorized, update ticket**
  - Input: `Device authorized`
  - Command: `Add authorized device` (Admin)
- **On client kicked, revoke access**
  - Input: `Client kicked`
  - Command: `Revoke client access` (Captive)
  - Action: Router agent `Revoke client access`
- **When ticket ended, disable authorized devices and revoke access**
  - Input: `Ticket ended`
  - Command: `Remove authorization token` (Captive)
  - Action: Router agent `Revoke client access` (for affected devices)

## End-to-End Flows

### Flow A — Create ticket → Captive token created

1. Staff issues **Create ticket** in **Admin**.
2. Admin emits **Ticket created**.
3. Policy triggers **Captive: Create code based authorization token**.
4. Captive emits **Code based authorization token created**.

### Flow B — Authorize device → Router allows → Admin updates ticket

1. Captive device user executes **Authorize device with code** in **Captive**.
2. Captive emits **Device authorized**.
3. Policy calls **Router agent: Allow client access**.
4. Policy triggers **Admin: Add authorized device**.
5. Admin emits **Authorized device added**.

### Flow C — Kick client → Captive revokes → Router revokes

1. Staff executes **Kick client** in **Admin**.
2. Admin emits **Client kicked**.
3. Policy triggers **Captive: Revoke client access**.
4. Captive emits **Client access revoked**.
5. Policy calls **Router agent: Revoke client access**.

### Flow D — End/Expire ticket → Captive removes token → Router revokes

1. Staff executes **End ticket** in **Admin** or Cron triggers **Expire ticket**.
2. Admin emits **Ticket ended**.
3. Policy triggers **Captive: Remove authorization token**.
4. Captive emits **Authorization token removed**.
5. Policy calls **Router agent: Revoke client access** (for affected devices).

