# OpenID Connect (OIDC) single sign-on

Airsonic-Pulse can authenticate web users against an external OpenID Connect provider (Keycloak,
Authentik, Entra ID, Google, ...) as an additional login method next to the login form and LDAP.

OIDC is only used for the **web interface**. The Subsonic REST API, the `/ext` endpoints and Sonos keep
using API keys, tokens and credentials. Logging out only ends the local session (no single logout).

## Quick start

1. Register a client for Airsonic-Pulse at your provider (authorization code flow, PKCE is used).
   Register this redirect URI (it is also shown on *Settings > Advanced*):

   ```
   https://<your host>/<context path>/login/oauth2/code/airsonic
   ```

2. Configure Airsonic-Pulse, either on *Settings > Advanced* or with environment variables:

   ```
   OidcEnabled=true
   OidcIssuerUri=https://id.example.com/realms/main
   OidcClientId=airsonic
   OidcClientSecret=<secret>
   ```

3. **Restart** Airsonic-Pulse. A "Sign in with SSO" button appears on the login page.

OIDC settings are read once at startup, like the LDAP settings; changes need a restart. If the provider
cannot be reached at startup, an error is logged and OIDC stays disabled for that run; form and LDAP
login keep working.

## Settings

Every setting is read like all other Airsonic settings: environment variables and Java options win over
`airsonic.properties`, which is what the *Advanced Settings* page writes. A setting can be given under
the name below or in upper case (`OidcIssuerUri` or `OIDCISSUERURI`). The client secret is stored
obfuscated (hex encoded, not encrypted), is never shown in the UI again and a blank field keeps the
current value.

| setting | default | description |
| --- | --- | --- |
| `OidcEnabled` | `false` | Turns OIDC login on. |
| `OidcIssuerUri` | | Issuer URL of the provider. The configuration is discovered from `<issuer>/.well-known/openid-configuration`. Must match the `iss` claim exactly. |
| `OidcClientId` | | Client id registered at the provider. |
| `OidcClientSecret` | | Client secret. Leave empty for a public client (PKCE only). |
| `OidcScopes` | `openid profile email` | Requested scopes (space separated). `openid` is always added. Add `groups` or similar if your provider only emits group claims for a scope. |
| `OidcUsernameClaim` | `preferred_username` | Claim used as the Airsonic username for automatically created users. |
| `OidcButtonLabel` | `SSO` | Label of the login button ("Sign in with ..."). |
| `OidcAutoCreateUsers` | `false` | Create unknown users on first sign-in. |
| `OidcLinkByEmail` | `false` | Link unknown identities to the existing user with the same email. |
| `OidcRequiredClaim` | | Access restriction: claim that must contain `OidcRequiredClaimValue`. Empty disables the restriction. |
| `OidcRequiredClaimValue` | | Value the required claim must contain. If empty, the claim only has to be present and non-empty. |
| `OidcGroupsClaim` | | Claim holding the user's groups, used for the admin group mapping. |
| `OidcAdminGroup` | | Members of this group get the admin role. |

## How a user is matched

After the provider authenticated the user and the access restriction (below) passed, the Airsonic user
is resolved in this order:

1. **Existing link.** An OIDC identity (issuer + subject) is stored as a credential of type *OpenID
   Connect* on the Credentials page. If one exists, that user signs in. Expired links are rejected.
2. **Link by email** (`OidcLinkByEmail`). If the token contains an email with `email_verified=true`
   that belongs to exactly one existing user, the identity is linked to that user. Unverified emails are
   never used.
3. **Auto-create** (`OidcAutoCreateUsers`). A new user is created from `OidcUsernameClaim` with the
   stream and settings roles. A username that already exists but is not linked is never taken over.
   Automatically created users cannot use a password (`password authentication` is off) and use API
   keys for Subsonic clients. They can set a password later on the credentials page if needed.
4. Otherwise the sign-in is rejected and the login page shows its generic error.

An administrator can **pre-link** an identity on *Settings > Users*: enter the `sub` claim of the user at
the configured provider. Links are visible to the user on *Settings > Credentials*, where they can be
deleted. Deleting a link revokes single sign-on for that identity. OIDC links are never usable as
passwords and are never re-encoded.

## Access restriction

To allow only members of a group, set the required claim and value, for example:

```
OidcRequiredClaim=groups
OidcRequiredClaimValue=airsonic-users
```

* The claim name may be a dot path for nested claims, e.g. `realm_access.roles` (Keycloak).
* The claim may be a list (any element equals the value), a single string (equals) or a space separated
  string (any token equals). Comparison is exact and case sensitive; `admin` does not match
  `administrators`.
* Claims come from the ID token merged with the UserInfo response.
* The check runs **first and on every sign-in**, also for linked and pre-linked users, so removing a user
  from the group at the provider blocks the next sign-in. Nothing is created for rejected users.
* It fails closed: a missing or empty claim, or a claim of an unsupported type (number, object), is
  rejected. Rejections are logged as a warning naming the subject and claim, never the token.
* Remember to request the scope that makes your provider emit the claim (`OidcScopes`).

The admin group mapping (`OidcGroupsClaim` / `OidcAdminGroup`) is independent from this restriction: it
only grants the admin role (when the user signs in), it never removes roles.

## Security notes

* Only enable *Link by email* if the provider owns and verifies email addresses, otherwise anybody who
  can register an account with a victim's address at the provider could take over the matching account.
* Be careful with *Auto-create*: without an access restriction every user the provider lets sign in to
  this client becomes an Airsonic user.
* Use HTTPS and, behind a reverse proxy, make sure forwarded headers are honoured so that the redirect
  URI matches what is registered at the provider.
* The client secret is only obfuscated at rest. Prefer supplying it through an environment variable or a
  secret store in container deployments.
