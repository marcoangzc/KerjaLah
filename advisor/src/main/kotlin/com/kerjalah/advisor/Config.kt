package com.kerjalah.advisor

// Server configuration, all from environment variables.
//
// Nothing here has a default that could accidentally ship a secret: the three
// required values throw at startup if missing, so a misconfigured deploy fails
// loudly on boot instead of silently degrading at the first student's tap.
object Config {

    val supabaseUrl: String = required("SUPABASE_URL").trimEnd('/')

    // Used only to forward the caller's token to Supabase's auth endpoint.
    val supabaseAnonKey: String = required("SUPABASE_ANON_KEY")

    // Bypasses RLS entirely. Every use of this key in AdvisorRoutes is preceded
    // by a hand-written authorisation check, because Postgres will not make one.
    val serviceRoleKey: String = required("SUPABASE_SERVICE_ROLE_KEY")

    // Optional: without it the advisor degrades to "no advice", which is a
    // supported state - applying must never depend on the AI being reachable.
    val groqApiKey: String? = System.getenv("GROQ_API_KEY")?.takeIf { it.isNotBlank() }

    val groqModel: String = System.getenv("GROQ_MODEL")?.takeIf { it.isNotBlank() }
        ?: "qwen/qwen3.8-27b"

    // Hosts like Render/Railway/Fly inject PORT; 8080 is the local default.
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080

    private fun required(name: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: error("Missing required environment variable: $name")
}
