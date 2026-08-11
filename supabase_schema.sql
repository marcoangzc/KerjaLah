-- KerjaLah database schema for Supabase
-- Run this ONCE in: Supabase Dashboard -> SQL Editor -> New query -> paste -> Run
-- Tables mirror the app's data classes; RLS keeps each role in its lane.

-- ========== 1. Tables ==========

-- Module 1: user profiles (auth itself lives in auth.users, managed by Supabase)
create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  role text not null check (role in ('STUDENT', 'EMPLOYER')),
  name text not null,
  email text not null,
  organization text not null default '',
  bio text not null default ''
);

-- Module 2: job postings
create table public.jobs (
  id uuid primary key default gen_random_uuid(),
  employer_id uuid not null references public.profiles (id) on delete cascade,
  title text not null,
  company_name text not null,
  location text not null,
  pay_per_hour numeric not null,
  hours_per_week int not null,
  description text not null
);

-- Module 3: applications (User applies Job -> Application)
create table public.applications (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs (id) on delete cascade,
  student_id uuid not null references public.profiles (id) on delete cascade,
  status text not null default 'PENDING' check (status in ('PENDING', 'ACCEPTED', 'REJECTED')),
  applied_at bigint not null, -- epoch millis, matches the Kotlin model
  ai_match_percent int,       -- filled by Gemini later (suggestion only)
  ai_suggested_status text,
  ai_reason text,
  unique (job_id, student_id) -- one application per student per job
);

-- ========== 2. Row Level Security ==========

alter table public.profiles enable row level security;
alter table public.jobs enable row level security;
alter table public.applications enable row level security;

-- profiles: any logged-in user can read profiles (employers need to see
-- applicant names); you can only create/update/delete YOUR OWN profile.
create policy "profiles are readable by signed-in users"
  on public.profiles for select to authenticated using (true);
create policy "insert own profile"
  on public.profiles for insert to authenticated with check (id = auth.uid());
create policy "update own profile"
  on public.profiles for update to authenticated using (id = auth.uid());
create policy "delete own profile"
  on public.profiles for delete to authenticated using (id = auth.uid());

-- jobs: everyone signed in can browse; only the owning employer can write.
create policy "jobs are readable by signed-in users"
  on public.jobs for select to authenticated using (true);
create policy "employer inserts own jobs"
  on public.jobs for insert to authenticated with check (employer_id = auth.uid());
create policy "employer updates own jobs"
  on public.jobs for update to authenticated using (employer_id = auth.uid());
create policy "employer deletes own jobs"
  on public.jobs for delete to authenticated using (employer_id = auth.uid());

-- applications: student sees/creates/withdraws own; employer sees/updates
-- only applications for jobs they own.
create policy "student reads own applications, employer reads own jobs' applications"
  on public.applications for select to authenticated using (
    student_id = auth.uid()
    or exists (
      select 1 from public.jobs j
      where j.id = job_id and j.employer_id = auth.uid()
    )
  );
create policy "student applies as self"
  on public.applications for insert to authenticated with check (student_id = auth.uid());
create policy "employer updates status of own jobs' applications"
  on public.applications for update to authenticated using (
    exists (
      select 1 from public.jobs j
      where j.id = job_id and j.employer_id = auth.uid()
    )
  );
create policy "student withdraws own pending application"
  on public.applications for delete to authenticated using (
    student_id = auth.uid() and status = 'PENDING'
  );

-- ========== 3. Realtime ==========
-- Let the app subscribe to live changes (status updates push to students).

alter publication supabase_realtime add table public.jobs;
alter publication supabase_realtime add table public.applications;
