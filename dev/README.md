# Local development environment

1. Start the services:

   ```
   docker compose -f dev/docker-compose.yml up -d
   ```

2. Start the **Airsonic (dev)** run configuration in IntelliJ IDEA (`.run/Airsonic (dev).run.xml`).
   It stores everything Airsonic writes (config, logs, playlists, ...) in `work/airsonic-home/`, which is
   git-ignored, and uses the PostgreSQL database below. Airsonic listens on <http://localhost:4040>.

| Service | URL / address | Credentials |
| --- | --- | --- |
| Airsonic DB (PostgreSQL 17) | `jdbc:postgresql://localhost:5432/airsonic?stringtype=unspecified` | `airsonic` / `airsonic` |
| Keycloak | <http://localhost:8180> (admin console) | `admin` / `admin` |
| Keycloak DB (internal only) | `keycloak-db:5432` | `keycloak` / `keycloak` |

## OIDC test setup

The imported Keycloak realm `airsonic` contains the confidential client `airsonic` (secret
`airsonic-dev-secret`, PKCE S256, redirect URI `http://localhost:4040/login/oauth2/code/airsonic`) with a
mapper that adds the user's groups to the `groups` claim of the ID token, access token and UserInfo. The
run configuration enables OIDC with auto-created users and maps `airsonic-admins` to the admin role.

| User | Password | Email verified | Groups |
| --- | --- | --- | --- |
| `alice` | `alice` | yes | `airsonic-admins`, `airsonic-users` |
| `bob` | `bob` | yes | `airsonic-users` |
| `carol` | `carol` | no | none |

To try the access restriction, add `-DOidcRequiredClaim=groups -DOidcRequiredClaimValue=airsonic-users`
to the VM options: `carol` is then rejected. See [docs/configuration/oidc.md](../docs/configuration/oidc.md).

## Reset

```
docker compose -f dev/docker-compose.yml down -v   # drops both databases and Keycloak state
rm -rf work/airsonic-home                          # drops Airsonic's config (do this together with the DB reset)
```

Changes to `dev/keycloak/airsonic-realm.json` are only imported into a fresh Keycloak database (`down -v`).
All credentials here are for local development only.
