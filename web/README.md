# Asapp Web

This directory is reserved for an authenticated WebRTC viewer and signaling server.

No remote camera streaming is enabled in the current Android build.

Security requirements for future implementation:
- Device owner explicitly starts each streaming session.
- Persistent visible indicator and immediate stop control.
- Authentication and short-lived viewer tokens.
- TLS and no public unauthenticated video endpoints.
- Respect Android camera and foreground-service restrictions.
