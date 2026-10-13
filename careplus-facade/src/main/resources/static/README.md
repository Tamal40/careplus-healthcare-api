# CarePlus UI with JWT Login

Open `http://localhost:8080/` after starting the Facade.

## Login flow
1. Enter username and password.
2. UI sends `POST /api/auth/login` with JSON `{ "username": "...", "password": "..." }`.
3. The response must contain a signed JWT in `token`, `accessToken`, `jwt`, or `access_token`.
4. The UI stores it in `sessionStorage`.
5. Every subsequent API request includes `Authorization: Bearer <JWT>`.
6. Logout clears the token. A 401/403 response clears the token and returns the UI to login.

## Backend requirement
The starter project does not currently include an authentication controller/security configuration. The UI alone does not provide authentication. Implement `/api/auth/login` to validate credentials and issue a signed JWT, and configure Spring Security to validate the token on protected Facade routes. Do not trust arbitrary tokens.

The UI follows the CarePlus routes for patients, doctors, and appointments. The starter's business API controllers are placeholders; implement the APIs and DTOs before expecting all operations to work. Adjust `app.js` if the DTO fields differ. Use dummy data only; this is not a production clinical system.

API routes are defined in constants.js and loaded before app.js in index.html.
