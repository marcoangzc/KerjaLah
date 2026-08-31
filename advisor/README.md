# KerjaLah AI Advisor (`:advisor`)

A small Kotlin/JVM [Ktor](https://ktor.io) server. It is the only place that
holds the Groq API key, and the only thing that writes the `ai_*` columns.

## Why it exists

The advisor used to run inside the Android app. That meant:

- the Groq key was compiled into the APK, where anyone can extract it;
- the AI verdict travelled in the client's `INSERT`, so a student could edit
  their own `ai_match_percent` before sending it.

Now the phone sends one field — `jobId` — and the server derives everything
else from the caller's verified JWT.

## API

### `POST /assess-application`

```
Authorization: Bearer <supabase access token>
Content-Type: application/json

{ "jobId": "<uuid>" }
```

The server then:

1. verifies the token with Supabase and extracts the user id;
2. loads that user's profile and rejects anyone who is not a `STUDENT`;
3. loads the job;
4. asks Groq to score the fit (best effort — see below);
5. inserts the `applications` row with the service role key.

| Status | Meaning |
|---|---|
| `200` | Applied. `aiAdviceAvailable` says whether Groq answered. |
| `400` | Missing or malformed `jobId`. |
| `401` | Missing, expired or invalid token. |
| `403` | No profile, or the caller is an employer. |
| `404` | No such job. |
| `409` | Already applied (unique `job_id, student_id`). |

**The AI never gates an application.** If Groq is down, slow, returns malformed
JSON, or `GROQ_API_KEY` is unset, the row is still inserted with the three
`ai_*` columns left null. Only an auth or database failure stops an application.

### `GET /health`

Returns `ok`. Point your host's health check at this.

## Configuration

All via environment variables. The first three are required and the server
refuses to boot without them.

| Variable | Required | Notes |
|---|---|---|
| `SUPABASE_URL` | yes | e.g. `https://abc.supabase.co` |
| `SUPABASE_ANON_KEY` | yes | Only used to forward the caller's token to the auth endpoint. |
| `SUPABASE_SERVICE_ROLE_KEY` | yes | **Bypasses RLS.** Never put this in the app or in `local.properties`. |
| `GROQ_API_KEY` | no | Absent means "no advice", which is a supported state. |
| `GROQ_MODEL` | no | Defaults to `qwen/qwen3.8-27b`. |
| `PORT` | no | Defaults to `8080`; most hosts inject this. |

## Run locally

```bash
./gradlew :advisor:run
```

The Android emulator reaches your host machine at `10.0.2.2`, which is the
default `ADVISOR_BASE_URL` in `local.properties.example`. On a physical device
use your machine's LAN IP.

## Deploy

Any host that runs a JVM container works — Render, Railway, Fly.io, Koyeb.

```bash
./gradlew :advisor:installDist   # builds advisor/build/install/advisor/
```

A `Dockerfile` is included. Whichever host you choose, set the environment
variables above as **secrets**, and point the app's `ADVISOR_BASE_URL` at the
resulting HTTPS URL.

> Use HTTPS in production. The request carries the user's Supabase access
> token, and Android blocks cleartext HTTP by default anyway.
