package com.kerjalah.app.data.data

import com.kerjalah.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

// [A] One shared Supabase client for the whole data layer.
// URL + anon key come from local.properties via BuildConfig,
// so no secret ever lives in source code / git.
// The anon key is safe on devices BECAUSE Row Level Security
// decides what each logged-in user can actually touch.
object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)      // login / register / session
            install(Postgrest) // database tables
            install(Realtime)  // live updates (status changes push instantly)
        }
    }
}
