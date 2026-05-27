# SmartCourseV3.2 Project Context

Last updated: 2026-05-28

## Current Runtime

- Backend is expected on port `9096`.
- Admin frontend is expected on port `9528`.
- Student frontend is expected on port `9530`.
- Teacher frontend is expected on port `9529`.
- Backend jar path used during recent work:
  `E:\SmartCourseV3.2\SmartCourse\SmartCourse-Server\target\online-edu-0.0.1-SNAPSHOT.jar`.

## Recent Fixes

### Student Course Category Display

- Fixed student category/search/teacher course pages filtering out valid
  courses when backend response did not include `semester`, `courseType`, or
  `majors`.
- Updated:
  - `SmartCourse-App/src/views/search_subject.vue`
  - `SmartCourse-App/src/views/search_keyword.vue`
  - `SmartCourse-App/src/views/search_teacher.vue`
- Verified the public required category shows the expected English/math demo
  courses.
- Verified the practice/innovation category shows the expected software
  engineering and graduation-project demo courses.

### Demo Course Categories

- Fixed demo course seed logic so courses are assigned to meaningful subjects
  instead of the first available subject.
- Disabled stale/empty legacy subject branches.
- Updated:
  - `SmartCourse-Server/src/main/java/xyz/refrain/onlineedu/config/DatabaseMigrationRunner.java`
  - `SmartCourse-Server/src/main/java/xyz/refrain/onlineedu/service/EduSubjectService.java`

### Graduation Warning Logic

- Fixed graduation completion calculation to cap credits per requirement
  bucket instead of letting surplus credits in one bucket hide gaps in another.
- Requirement buckets:
  - `PUBLIC_REQUIRED`: 4 credits
  - `MAJOR_REQUIRED`: 18 credits
  - `MAJOR_ELECTIVE`: 6 credits
  - `GENERAL_ELECTIVE`: 4 credits
- Updated recommendation path planning so the multi-course path checks conflicts
  among recommended courses, not only against already selected courses.
- Clarified diagnosis wording: single-course diagnosis checks the course against
  the current selected timetable; multi-course choice should use the generated
  path plan.
- Updated admin table labels for passed-credit completion and projected
  category gap.
- Updated:
  - `SmartCourse-Server/src/main/java/xyz/refrain/onlineedu/service/SmartAdvisorService.java`
  - `SmartCourse-Admin/src/views/smart/graduation.vue`

## Demo Student Risk Shape

Current intended demo data:

- `222023321102093` to `222023321102097`: low risk, all requirement buckets met.
- `222023321102098` to `222023321102102`: medium risk, professional elective
  gap of 4 credits.
- `222023321102103` to `222023321102108`: high risk, large required-credit gaps.

Recent API/browser verification showed admin overview counts:

- High risk: 6
- Medium risk: 5
- Low risk: 5

## Verification Already Run

- `mvn -DskipTests compile` in `SmartCourse-Server`: passed.
- `mvn -DskipTests package` in `SmartCourse-Server`: passed after stopping
  the old running backend jar.
- `npm run build` in `SmartCourse-App`: passed with existing asset-size/Sass
  warnings.
- `npm run build:prod` in `SmartCourse-Admin`: passed with existing asset-size
  warnings.

## GitHub Archive

- Git remote: `origin` -> `https://github.com/LE0-Lin/SWUSmartCourseManagement.git`.
- Default branch: `main`.
- Repository visibility observed on 2026-05-28: `PUBLIC`.
- The repository already tracks local database configuration. Be careful before
  pushing sensitive config changes to a public remote.

## Notes For Future Codex Threads

- `AGENTS.md` is a rule/instruction file, not an automatic memory log.
- Keep this file updated after important changes so parallel Codex threads can
  share the project state.
- If frontend and backend disagree, check whether the backend jar has been
  rebuilt and restarted. The running backend has previously used a packaged jar,
  not hot reload.
