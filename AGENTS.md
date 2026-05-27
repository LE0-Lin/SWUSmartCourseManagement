# SmartCourseV3.2 Codex Instructions

This file is the project-level instruction file for Codex-style agents.
It is not an automatic memory database. Agents should read it at the start
of work and follow the rules below.

## Cross-Thread Handoff

- Read `PROJECT_CONTEXT.md` before making non-trivial changes.
- After meaningful code, data, config, or runtime changes, update
  `PROJECT_CONTEXT.md` with:
  - what changed,
  - files touched,
  - commands/tests run,
  - current server/runtime state,
  - unresolved issues or next steps.
- Keep `PROJECT_CONTEXT.md` concise. Do not paste long logs.
- Do not overwrite useful history from other Codex conversations. Append or
  update the relevant section carefully.

## Workspace Rules

- Main workspace: `E:\SmartCourseV3.2`.
- Main project folder: `E:\SmartCourseV3.2\SmartCourse`.
- Student frontend: `SmartCourse-App`, normally on port `9530`.
- Admin frontend: `SmartCourse-Admin`, normally on port `9528`.
- Teacher frontend: `SmartCourse-Teacher`, normally on port `9529`.
- Backend: `SmartCourse-Server`, normally on port `9096`.
- Database: MySQL database `online_edu`.
- Do not assume generated demo data is correct; verify with the running API
  or database when debugging course categories, graduation warnings, schedules,
  grades, or recommendations.

## Verification Expectations

- For backend changes, run at least `mvn -DskipTests compile` in
  `SmartCourse-Server`.
- For admin frontend changes, use `npm run build:prod`.
- For student frontend changes, use `npm run build`.
- When fixing a visible UI problem, verify through the browser or the relevant
  API endpoint, not just by reading code.
