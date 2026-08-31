-- KerjaLah migration 01 - security hardening + data model fixes.
--
-- Run ONCE in: Supabase Dashboard -> SQL Editor -> New query -> paste -> Run.
-- Assumes supabase_schema.sql has already been applied (that file stays as the
-- historical "v0" schema and is intentionally NOT edited).
--
-- Everything below is idempotent where Postgres allows it, so a partial run can
-- be re-run safely. Sections are ordered by dependency, not by priority.

begin;

-- ===========================================================================
-- 0. Helper: the caller's role, readable from inside RLS policies
-- ===========================================================================
-- security definer so the function can read public.profiles even though the
-- caller's own SELECT policy on profiles is now restrictive (section 2).
-- search_path is pinned so a malicious temp schema cannot shadow "profiles".
create or replace function public.user_role()
returns text
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select role from public.profiles where id = auth.uid()
$$;

revoke all on function public.user_role() from public;
grant execute on function public.user_role() to authenticated;

comment on function public.user_role() is
  'Role (STUDENT/EMPLOYER) of the current JWT user. Used by RLS policies.';

-- ===========================================================================
-- 1. profiles: drop the duplicated email column
-- ===========================================================================
-- auth.users.email is the source of truth; a second copy could drift and it
-- widened the blast radius of the old "everyone can read every profile" policy.
alter table public.profiles drop column if exists email;

-- ===========================================================================
-- 2. profiles: least-privilege SELECT
-- ===========================================================================
-- Before: any signed-in user could read EVERY profile row (name, university,
-- bio, email) - i.e. the whole user directory leaked to anyone who registered.
-- After: you read your own row, and an employer reads a student's row only
-- while that student has an application on one of the employer's jobs.
drop policy if exists "profiles are readable by signed-in users" on public.profiles;

create policy "read own profile"
  on public.profiles for select to authenticated
  using (id = auth.uid());

-- No recursion risk: this policy reads applications -> jobs, and neither of
-- those tables' policies read back into profiles.
create policy "employer reads applicants of own jobs"
  on public.profiles for select to authenticated
  using (
    exists (
      select 1
      from public.applications a
      join public.jobs j on j.id = a.job_id
      where a.student_id = public.profiles.id
        and j.employer_id = auth.uid()
    )
  );

-- ===========================================================================
-- 3. jobs: fair-wage + sane-hours enforced by the database
-- ===========================================================================
-- numeric(6,2) = up to RM 9999.99/hour with exact 2-decimal money semantics
-- (plain `numeric` allowed RM 8.7199999 to sneak past a >= 8.72 check).
alter table public.jobs
  alter column pay_per_hour type numeric(6, 2);

-- !! MINIMUM WAGE LIVES IN TWO PLACES !!
-- Keep this constraint in sync with FairWage.MIN_HOURLY_RM in
-- app/src/main/java/com/kerjalah/app/data/FairWage.kt.
-- Source: Malaysia Minimum Wages Order 2024 - RM1,700/month = RM8.72/hour.
-- The Kotlin constant is only a friendly UI hint; THIS is the real gate.
alter table public.jobs
  drop constraint if exists jobs_pay_per_hour_min_wage;
alter table public.jobs
  add constraint jobs_pay_per_hour_min_wage check (pay_per_hour >= 8.72);

alter table public.jobs
  drop constraint if exists jobs_hours_per_week_positive;
alter table public.jobs
  add constraint jobs_hours_per_week_positive check (hours_per_week > 0);

-- ===========================================================================
-- 4. jobs / applications: role-aware INSERT policies
-- ===========================================================================
-- Owning the row was not enough: a STUDENT account could post jobs and an
-- EMPLOYER account could apply to them.
drop policy if exists "employer inserts own jobs" on public.jobs;
create policy "employer inserts own jobs"
  on public.jobs for insert to authenticated
  with check (employer_id = auth.uid() and public.user_role() = 'EMPLOYER');

drop policy if exists "student applies as self" on public.applications;
create policy "student applies as self"
  on public.applications for insert to authenticated
  with check (student_id = auth.uid() and public.user_role() = 'STUDENT');

-- ===========================================================================
-- 5. applications.applied_at: bigint epoch millis -> timestamptz
-- ===========================================================================
-- The client used to send its own wall clock, so a skewed phone could file an
-- application "next year". Now the database stamps it.
alter table public.applications
  alter column applied_at type timestamptz
    using to_timestamp(applied_at / 1000.0),
  alter column applied_at set default now(),
  alter column applied_at set not null;

-- ===========================================================================
-- 6. applications: the AI columns are server-written, client-readable
-- ===========================================================================
-- Column-level privileges only bite once the table-level grant is gone:
-- Postgres does NOT subtract a column REVOKE from a table-level GRANT.
-- After this, an `authenticated` client can only ever insert these 3 columns;
-- ai_* and applied_at are writable exclusively by the :advisor Ktor server
-- (service_role, which bypasses RLS and column grants).
revoke insert on public.applications from authenticated;
grant insert (job_id, student_id, status) on public.applications to authenticated;

-- The advisor's verdict vocabulary. Deliberately NOT the same words as
-- applications.status: the AI describes fit, it does not make decisions -
-- "ACCEPTED" in an advisory column invited exactly that confusion.
-- Translate any legacy rows FIRST, or the CHECK below refuses to validate.
update public.applications
   set ai_suggested_status = case ai_suggested_status
     when 'ACCEPTED' then 'STRONG_MATCH'
     when 'REJECTED' then 'WEAK_MATCH'
     else null
   end
 where ai_suggested_status is not null
   and ai_suggested_status not in ('STRONG_MATCH', 'POSSIBLE_MATCH', 'WEAK_MATCH');

alter table public.applications
  drop constraint if exists applications_ai_suggested_status_check;
alter table public.applications
  add constraint applications_ai_suggested_status_check check (
    ai_suggested_status is null
    or ai_suggested_status in ('STRONG_MATCH', 'POSSIBLE_MATCH', 'WEAK_MATCH')
  );

alter table public.applications
  drop constraint if exists applications_ai_match_percent_range;
alter table public.applications
  add constraint applications_ai_match_percent_range check (
    ai_match_percent is null or ai_match_percent between 0 and 100
  );

-- ===========================================================================
-- 7. applications: ai_* and applied_at are immutable after insert
-- ===========================================================================
-- The employer has UPDATE on these rows so they can set status. Without this
-- guard that same policy let them rewrite the AI's verdict or backdate the
-- application. Applies to EVERY role including service_role - the Edge
-- Function only ever INSERTs.
create or replace function public.applications_guard_immutable_columns()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if new.applied_at is distinct from old.applied_at then
    raise exception 'applications.applied_at is immutable';
  end if;
  if new.ai_match_percent is distinct from old.ai_match_percent
     or new.ai_suggested_status is distinct from old.ai_suggested_status
     or new.ai_reason is distinct from old.ai_reason then
    raise exception 'applications.ai_* columns are immutable';
  end if;
  return new;
end;
$$;

drop trigger if exists applications_guard_immutable_columns on public.applications;
create trigger applications_guard_immutable_columns
  before update on public.applications
  for each row execute function public.applications_guard_immutable_columns();

-- ===========================================================================
-- 8. Indexes for the three hot foreign-key lookups
-- ===========================================================================
-- Postgres indexes primary keys and unique constraints automatically but NOT
-- foreign keys. Every screen in the app filters on exactly these columns.
create index if not exists applications_student_id_idx on public.applications (student_id);
create index if not exists applications_job_id_idx on public.applications (job_id);
create index if not exists jobs_employer_id_idx on public.jobs (employer_id);

-- ===========================================================================
-- 9. Realtime: full row on DELETE
-- ===========================================================================
-- Default replica identity sends only the primary key on DELETE, so a client
-- cannot tell whose application vanished without a full refetch.
alter table public.applications replica identity full;

-- ===========================================================================
-- 10. Auto-create the profile row on sign-up
-- ===========================================================================
-- Registration used to be two round trips (signUp, then INSERT profile) with
-- no transaction around them: a dropped connection left an auth user with no
-- profile, and that account could never log in again.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  insert into public.profiles (id, role, name)
  values (
    new.id,
    -- Never trust client metadata into a CHECK constraint: anything that is
    -- not exactly 'EMPLOYER' becomes a student.
    case
      when new.raw_user_meta_data ->> 'role' = 'EMPLOYER' then 'EMPLOYER'
      else 'STUDENT'
    end,
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'name'), ''), 'New user')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

commit;
