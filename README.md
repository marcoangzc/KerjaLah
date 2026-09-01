# 🇲 KerjaLah

> *Decent part-time work for Malaysian students — fairly paid, AI-assisted, real-time.*

**KerjaLah** is a native Android app built with **Kotlin + Jetpack Compose** that connects
students with part-time jobs. Employers can only publish jobs that pass a mandatory
**Fair-Wage Check** (Malaysia Minimum Wages Order 2024, aligned with **UN SDG 8**).
Students apply with one tap, and an **AI Advisor** instantly scores how well each
applicant fits the job — while the final decision always stays with the human employer.

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=android&logoColor=white)
![Supabase](https://img.shields.io/badge/Backend-Supabase-3ECF8E?logo=supabase&logoColor=white)
![AI](https://img.shields.io/badge/AI-Groq%20%C3%97%20Qwen%203.8 26b-F55036)
![minSdk](https://img.shields.io/badge/minSdk-24-brightgreen)

📦 Latest APK: [Releases](../../releases)
📚 New to the codebase? Start with **[ONBOARDING.md](ONBOARDING.md)** — a zero-basis guide (中文) covering Kotlin, Jetpack Compose, UDF, SQL/Supabase and AI prompting, tailored to this repo.

---

## ✨ Highlights

| | Feature | Description |
|---|---------|-------------|
| ⚖️ | **Fair-Wage Check** | Every posting must pay **≥ RM 8.72 / hour** (RM 1,700 / month, Minimum Wages Order 2024). Underpaid jobs never go live. |
| 🤖 | **AI Advisor** | When a student applies, the AI reads the job + applicant profile and returns a **match %**, a **suggested status** and a **one-line reason**. It only *suggests* — it never decides. |
| ⚡ | **Realtime Everything** | Powered by Supabase Realtime: the employer taps *Accept* on their phone and the student's screen recolors itself instantly. |
| 🎭 | **One App, Two Roles** | Full **Student** and **Employer** experiences from a single codebase. |

---

## 🔄 How It Works

```
 Student taps Apply ──▶ AI Advisor (Groq) scores the match (~12 s budget)
                          │
                          ▼
              Supabase INSERT (status = PENDING, advice rides along:
              ai_match_percent / ai_suggested_status / ai_reason)
                          │
                          ▼
 Employer opens the applicant → sees match % + AI suggestion
                          │
                          ▼
 Employer Accepts / Rejects ──▶ Supabase UPDATE
                          │
                          ▼
 Realtime push ──▶ Student's application list updates instantly
```

*The advisor never gates the application: any failure or time-out simply inserts
the row without advice. RLS lets students INSERT but not UPDATE, so the advice
must ride along with the insert itself, and a trigger then freezes the `ai_*`
columns so nobody can rewrite the verdict afterwards. The apply flow is also
`NonCancellable` — leaving the screen while the advisor runs can never lose the
application.*

---

## 🛠️ Tech Stack

| Layer        | Technology |
|--------------|------------|
| Language     | Kotlin (100%) |
| UI           | Jetpack Compose (Material 3) |
| Architecture | Unidirectional Data Flow — `ViewModel` + `StateFlow` |
| Backend      | Supabase (Auth · PostgREST · Realtime) |
| Networking   | Ktor client (CIO engine) |
| Serialization| kotlinx.serialization |
| AI           | Groq API (`Qwen 3.8`), OpenAI-compatible REST |
| Navigation   | Navigation Compose (all routes centralized in `Routes.kt`) |

---

## 📦 Modules

1. **Module 1 — Users & Auth** · Splash, login, register, role selection (Student / Employer), editable profiles.
2. **Module 2 — Jobs** · Employers post & edit jobs (guarded by the Fair-Wage Check); students browse job details.
3. **Module 3 — Applications** · One-tap apply / withdraw, accept / reject, realtime status sync and the AI Advisor.

---

## 📁 Project Structure

```
app/src/main/java/com/kerjalah/app/
├── data/                # Models, repositories, Supabase client
│   ├── SupabaseClientProvider.kt
│   ├── FairWage.kt      # SDG 8 minimum-wage rule (UI hint; DB has the real check)
│   └── *Repository.kt   # StateFlow caches + Realtime subscriptions
├── navigation/          # Routes.kt + NavGraph (the only navigator)
├── ui/
│   ├── user/            # Auth & profile screens
│   ├── job/             # Student: browse jobs
│   ├── application/     # Student: my applications
│   ├── employer/        # Employer: postings, applicants, AI card
│   └── theme/           # Material 3 theme
└── MainActivity.kt
supabase_schema.sql          # Run once in Supabase SQL Editor
supabase_migration_01.sql    # Then this one
supabase_migration_02.sql    # Then this one
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Ladybug or newer) with JDK 11+
- A [Supabase](https://supabase.com) project
- A free [Groq](https://console.groq.com/keys) API key

### 1 · Clone
```bash
git clone https://github.com/marcoangzc/KerjaLah.git
cd KerjaLah
```

### 2 · Set up the database
Supabase Dashboard → **SQL Editor** → run these three, in order:
1. `supabase_schema.sql` — creates the `profiles`, `jobs` and `applications` tables.
2. `supabase_migration_01.sql` — RLS hardening, constraints, indexes and the sign-up trigger.
3. `supabase_migration_02.sql` — re-grants the client the three `ai_*` columns.

### 3 · Configure the app
Copy `local.properties.example` to `local.properties` and fill it in.
⚠️ This file is git-ignored — **never commit your keys**.

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
GROQ_API_KEY=gsk_your-groq-key
```

### 4 · Run
Open the project in Android Studio → **Sync Gradle** → press **Run ▶** on an emulator or device (minSdk 24).

---

## 🧪 Try the AI Advisor

1. Register a **STUDENT** account and apply to a job.
2. Log in as the **EMPLOYER** who owns that job → *My Postings* → *Applicants*.
3. Open the applicant — you'll see the match %, suggested status and reason.
4. Or verify directly in Supabase: `applications` table → `ai_match_percent`, `ai_suggested_status`, `ai_reason`.

---

## 🔐 Security & Principles

- **Row-Level Security** — you can read only your own profile; an employer additionally
  reads a student's profile only while that student has an application on one of their
  jobs. Enforced by Postgres, so bypassing the app gains nothing.
- **Server-generated timestamps** — `applied_at` defaults to `now()` and is immutable,
  so a device with a skewed clock cannot backdate an application.
- **Known limitation** — `GROQ_API_KEY` is compiled into the APK and is therefore
  extractable, so the AI match score is a hint for the employer, not evidence.
- **Human-in-the-loop** — the AI advises; only the employer can accept or reject.
- **SDG 8** — decent work & economic growth, enforced at RM 8.72 / hour.

---

## 🗺️ Roadmap

- [ ] Resume / PDF parsing for richer AI matching
- [ ] Push notifications on status changes
- [ ] In-app chat between student & employer

---

Built with ❤️ in Malaysia · *KerjaLah!* 💪
