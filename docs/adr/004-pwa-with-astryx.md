# ADR-004: Frontend as a PWA built with Astryx

- **Date**: 2026-11-18
- **Status**: Proposed
- **Deciders**: Kauan Di Nubila
- **Tags**: frontend, mobile

## Context and Problem Statement

Lyra must work on the phone (logging food while travelling) and on the desktop (editing the diet plan). The chosen design system to evaluate is [Astryx](https://github.com/meta/astryx), Meta's open source design system: React 19, MIT licence, 150+ components, currently in beta (0.7.x). Its native package (`@astryxdesign/native`) is experimental, private and explicitly not supported for production, so Astryx runs only on the web today.

## Decision Drivers

1. One codebase for phone and desktop.
2. Actually evaluate Astryx, which only runs on the web.
3. Barcode scanning with the phone camera.
4. Usable with a weak connection.

## Considered Options

1. **Progressive Web App** with React, Vite and Astryx.
2. **The same PWA wrapped with Capacitor** for the app stores.
3. **React Native** app, without Astryx.

## Decision Outcome

Chosen option: **Progressive Web App with React, Vite and Astryx**.

- Vite instead of Next.js: Lyra is an app behind login, with no need for server rendering or SEO.
- Installable through a web app manifest; a service worker caches the app shell and today's diary for offline reading.
- Barcode: the browser `BarcodeDetector` API where available (Chrome on Android) and a JavaScript library fallback for Safari on iOS, which does not support it.

Capacitor stays as the upgrade path if publishing to the stores becomes a goal; it wraps the same build without a rewrite. React Native is rejected because it would not use Astryx and would mean a second frontend to maintain.

### Consequences

- Good: one frontend, installable on the phone from day one.
- Good: the backend does not care; any future native client uses the same HTTP API.
- Bad: Astryx is in beta, so minor updates can break components; versions are pinned and upgraded deliberately.
- Bad: Astryx charts are only on the canary tag; progress charts may need another library until they are stable.
- Bad: PWAs on iOS have limits (storage eviction, push notifications only when installed).
