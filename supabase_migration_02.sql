-- KerjaLah migration 02 - let the client write the AI advisor columns again.
--
-- Run in: Supabase Dashboard -> SQL Editor, AFTER supabase_migration_01.sql.
--
-- WHY THIS EXISTS (read before assuming it is a mistake):
--
-- Migration 01 moved the AI advisor to a server and locked the ai_* columns
-- down accordingly:
--     revoke insert on public.applications from authenticated;
--     grant  insert (job_id, student_id, status) ... to authenticated;
--
-- That server has since been removed: it could only ever be reached from the
-- developer's own machine, so anyone else installing the APK got no AI advice
-- at all. The advisor now runs inside the app again, which means the app must
-- be able to write the three columns it produces.
--
-- THE TRADE-OFF, stated plainly:
-- A student can now put any value they like in ai_match_percent by editing the
-- request their own phone sends. The AI score is therefore a HINT for the
-- employer, not evidence. Everything else migration 01 established still holds
-- (see the bottom of this file).

begin;

grant insert (ai_match_percent, ai_suggested_status, ai_reason)
  on public.applications to authenticated;

commit;

-- ===========================================================================
-- What is still protected (all of this survives untouched)
-- ===========================================================================
--
-- 1. applied_at is STILL server-generated and un-writable by the client.
--    It was never re-granted, so `default now()` remains the only source.
--
-- 2. The ai_* columns and applied_at are STILL immutable after insert, via
--    the applications_guard_immutable_columns trigger. An employer cannot
--    rewrite a verdict, and nobody can backdate an application.
--
-- 3. The CHECK constraints still hold: ai_match_percent must be 0-100 and
--    ai_suggested_status must be one of STRONG_MATCH / POSSIBLE_MATCH /
--    WEAK_MATCH. Garbage values are rejected, only plausible ones get through.
--
-- 4. Applications can still only be inserted by a STUDENT, as themselves
--    (student_id = auth.uid() and public.user_role() = 'STUDENT').
--
-- 5. The profiles read policies are untouched - the actual privacy fix, and
--    the one that mattered most, is unaffected by any of this.
