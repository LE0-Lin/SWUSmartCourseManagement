# SmartCourseV3.2 Project Context

Last updated: 2026-05-31

## Current Runtime

- Backend is expected on port `9096`.
- Admin frontend is expected on port `9528`.
- Student frontend is expected on port `9530`.
- Teacher frontend is expected on port `9529`.
- Backend jar path used during recent work:
  `E:\SmartCourseV3.2\SmartCourse\SmartCourse-Server\target\online-edu-0.0.1-SNAPSHOT.jar`.
- 2026-05-31 runtime: backend jar rebuilt and restarted successfully; port
  `9096` is currently listened on by PID `19684`.

## Recent Fixes

### Student Course Category Display

- Fixed student category/search/teacher course pages filtering out valid
  courses when backend response did not include `semester`, `courseType`, or
  `majors`.
- Updated:
  - `SmartCourse-App/src/views/search_subject.vue`
  - `SmartCourse-App/src/views/search_keyword.vue`
  - `SmartCourse-App/src/views/search_teacher.vue`
- Verified the six curriculum categories display their matching courses.
- The curriculum semester is exposed separately as `curriculumSemester`, so
  it is displayed without being confused with the legacy current-term filter.

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
  - `PUBLIC_REQUIRED`: 37 credits
  - `GENERAL_ELECTIVE`: 8 credits
  - `DISCIPLINE_REQUIRED`: 33.5 credits
  - `MAJOR_REQUIRED`: 32.5 credits
  - `MAJOR_ELECTIVE`: 17 credits
  - `PRACTICE_REQUIRED`: 32 credits
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

### Student Homepage Course Carousel

- Replaced the empty legacy banner feed on the student homepage with a
  personalized course-cover carousel.
- The backend now lists courses that the current student can still select:
  published and enabled courses only, excluding already selected courses and
  courses that conflict with the current timetable.
- The frontend shuffles that candidate pool, shows up to five course covers,
  rotates automatically, exposes clickable indicator dots, and links each
  cover to its course detail page.
- Updated:
  - `SmartCourse-App/src/api/content.js`
  - `SmartCourse-App/src/components/home/carousel.vue`
  - `SmartCourse-Server/src/main/java/xyz/refrain/onlineedu/controller/app/ContentController.java`
  - `SmartCourse-Server/src/main/java/xyz/refrain/onlineedu/service/EduCourseService.java`
- Verified with student `222023321102093`: 17 selectable carousel candidates
  remain after the curriculum data refresh.
- Browser verification confirmed five randomized course links and five
  clickable indicators.

### 2022 Curriculum Demo Data

- Replaced generic demo courses with the 2022 training plan for
  `计算机科学与技术（中外合作办学）`.
- Seeded 79 enabled records:
  - 75 named courses from the PDF.
  - 4 honest general-elective selection groups because the PDF defines the
    categories and required credits but does not list specific course names.
- Enabled category totals:
  - `通识教育必修`: 21 courses.
  - `通识教育选修`: 4 selection groups.
  - `学科基础`: 9 courses.
  - `专业发展必修`: 10 courses.
  - `专业发展选修`: 25 courses.
  - `综合实践`: 10 courses.
- Added course fields: `courseCode`, `credit`, `courseType`, `majorName`,
  `curriculumSemester`, and `assessmentMethod`.
- Disabled stale samples including `画图`, `大学英语综合训练`, and
  `毕业设计智能选题实践`.
- Added 15 generated curriculum cover assets under
  `uploads/demo/curriculum/`; all 79 courses use a matching generated or
  existing technical cover and no course uses the default placeholder.
- Corrected backend local image storage from the V4 path to
  `E:/SmartCourseV3.2/SmartCourse/uploads/`.

### Unique Curriculum Course Covers

- Replaced theme-shared cover URLs with one deterministic unique cover URL for
  each of the 79 curriculum records.
- `CurriculumCourseCatalog.uniqueCover(...)` maps `courseCode|title` to a stable
  image filename under `uploads/demo/curriculum/unique/`.
- Added `scripts/generate_curriculum_covers.py` to generate 480x270 course
  covers, `manifest.json`, and a contact-sheet preview from the curriculum
  catalog.
- Added distinct generated source tiles for civics, military education,
  physical education, academic language, career development, and
  entrepreneurship under `uploads/demo/curriculum/source/`.
- 2026-05-31 file audit: 79 JPG files, 79 distinct SHA-256 hashes, and 79
  manifest rows.
- 2026-05-31 live API audit: 79 enabled courses, 79 distinct cover URLs, zero
  broken image responses, and zero non-unique legacy cover paths.
- 2026-05-31 browser audit: the first 12 rendered homepage course cards used 12
  distinct image URLs; the carousel still exposed five clickable indicators.

### Unified Demonstration Portal

- Added `SmartCourse-Portal/`, a standalone static portal served on port `9527`.
- The portal presents one Southwest University-branded entry page for the three
  existing applications:
  - Student service: `http://localhost:9530`
  - Teacher workbench: `http://localhost:9529`
  - Academic administration: `http://localhost:9528`
- Added three locally stored official Southwest University campus photos and
  the official logo under `SmartCourse-Portal/assets/images/`. Sources are
  recorded in `SmartCourse-Portal/PHOTO_SOURCES.md` so the demonstration does
  not depend on external network access.
- Added local Lucide icons and endpoint status checks. The campus backgrounds
  rotate automatically and expose three clickable indicator controls.
- Updated both `start_smartcourse.bat` and `一键启动系统.bat` to launch the
  portal together with the existing backend and frontends.
- 2026-05-31 runtime state: portal is listening on port `9527` with PID `43664`.
- 2026-05-31 verification:
  - `node --check SmartCourse-Portal/app.js` and
    `node --check SmartCourse-Portal/server.cjs`: passed.
  - `git diff --check`: passed.
  - Browser narrow-width screenshot: no horizontal overflow or text overlap.
  - Desktop `1440x1000` Edge screenshot:
    `E:/SmartCourseV3.2/outputs/portal-desktop.png`.
  - Browser interaction audit: automatic background rotation and manual
    indicator selection passed; all three role endpoints reported online.

### Portal Return Links And Simplified Covers

- Added `http://localhost:9527` return links to all three login pages.
- Added persistent post-login portal access:
  - Student app: icon-and-text entry in the shared top header.
  - Teacher and admin apps: home icon with `返回统一门户` tooltip in the shared
    Navbar.
- Portal links use same-tab navigation to keep the demonstration workspace
  tidy.
- Simplified all 79 generated curriculum covers: preserve the top category
  color band and category label, but remove repeated course title, course code,
  semester, assessment, and bottom dark overlay from the image.
- Reused existing source images and stable cover URLs. No AI regeneration,
  database migration, or backend restart was needed.
- 2026-06-01 verification:
  - Cover regeneration: 79 JPG files, 79 distinct SHA-256 hashes, zero
    duplicates, and 79 manifest rows.
  - Builds passed: student `npm run build`; teacher and admin
    `npm run build:prod`.
  - Browser audit passed for all three login return links and all three
    post-login return links.
  - Student homepage rendered 12 visible cards with 12 distinct cover URLs.
  - Screenshot: `E:/SmartCourseV3.2/outputs/student-home-clean-covers.png`.

### Bottom-Aligned Cover Category Labels

- Moved the retained curriculum cover category band and label from the top edge
  to the bottom edge so it does not obscure faces or other primary subjects.
- Reused the same stable cover URLs and source images; no database change or
  backend restart was required.
- 2026-06-01 verification: regenerated 79 JPG files with 79 distinct hashes and
  zero duplicates; inspected the contact sheet; and browser-verified 12 visible
  homepage cards with 12 distinct cover URLs.
- Screenshot:
  `E:/SmartCourseV3.2/outputs/student-home-bottom-category-labels.png`.

## Demo Student Risk Shape

The 16 seeded student accounts now keep the stable `5 / 5 / 6` low, medium,
and high-risk distribution while exposing distinct advisor scenarios:

- Low risk includes fully complete students and `222023321102094`, whose
  current completion is `95%` but projected completion is `100%` because
  `8` general-elective credits are selected and awaiting grades.
- Medium risk includes single elective gaps from `0.5` to `4` credits and
  `222023321102101`, which combines a `2`-credit general-elective gap with a
  `3.5`-credit professional-elective gap.
- High risk includes a broad `134`-credit gap, an empty `160`-credit
  transcript, one missing required course, the same required course failed,
  an elective-only `8`-credit threshold case, and a `17`-credit practice gap.

2026-06-01 live API verification showed admin overview counts:

- High risk: 6
- Medium risk: 5
- Low risk: 5

### Advisor Path And Teacher Demo Refinements

- `SmartAdvisorService` now ranks gap-matching courses ahead of popularity and
  keeps one disabled conflict card visible whenever a real conflict exists.
- Seeded two controlled professional-elective schedule overlaps:
  - `智能系统与行业大数据应用` conflicts with the selected
    `计算机科学导论`.
  - `机器视觉与行业大数据应用` conflicts with
    `数字经济与区块链技术`, so the generated path chooses only one.
- Verified `222023321102105` now receives the exact missing required course,
  `数据库原理及应用`, in its path.
- Teacher course dropdowns now filter disabled legacy samples.
- Teacher login defaults to the active `13800138002 / 123456` account.
- Rewrote `TEST_ACCOUNTS.txt` as the current demo handbook with every account,
  expected scenario, advisor-path semantics, and an eight-step demo order.
- 2026-06-01 runtime state: packaged backend jar is listening on port `9096`
  with PID `53524`.
- 2026-06-01 verification:
  - Backend `mvn -DskipTests compile` and `mvn -DskipTests package`: passed.
  - Teacher frontend `npm run build:prod`: passed with existing asset-size
    warnings.
  - Live API assertions: passed for 16 students, `5 / 5 / 6` risk counts,
    gap-specific path ranking, visible conflict cards, and both `OK` and
    `TIME_CONFLICT` diagnoses.
  - Headless Edge browser audit: `222023321102098` rendered two path steps,
    12 recommendation cards, and one disabled red conflict card without layout
    overlap.
  - Screenshot:
    `E:/SmartCourseV3.2/outputs/advisor-medium-conflict-path.png`.

## Verification Already Run

- `mvn -DskipTests compile` in `SmartCourse-Server`: passed.
- `mvn -DskipTests package` in `SmartCourse-Server`: passed after stopping
  the old running backend jar.
- `npm run build` in `SmartCourse-App`: passed with existing asset-size/Sass
  warnings.
- `npm run build:prod` in `SmartCourse-Admin`: passed with existing asset-size
  warnings.
- 2026-05-31 carousel change: reran `mvn -DskipTests compile`,
  `mvn -DskipTests package`, and `npm run build`; all passed.
- 2026-05-31 curriculum refresh: reran `mvn -DskipTests compile`,
  `mvn -DskipTests package`, and `npm run build`; all passed.
- 2026-05-31 API audit: 79 enabled curriculum records, 23 cover assets, no
  broken cover URLs, no missing curriculum fields, no default placeholders,
  and stale sample searches returned zero results.
- 2026-05-31 unique-cover refresh: reran `mvn -DskipTests compile`,
  `mvn -DskipTests package`, and `git diff --check`; all passed. Backend jar is
  running on port `9096` with PID `51836`.
- 2026-05-31 browser verification: six categories, five carousel indicators,
  general-elective category navigation, psychology cover detail, and academic
  English cover detail passed. Browser screenshot capture timed out in the
  extension, so verification used DOM state and direct asset requests.

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

## 2026-06-01 Final System QA And Archive Prep

- Fixed student category navigation by keying the root router view to the
  current path. Clicking a homepage category now renders the matching course
  list instead of reusing the homepage component.
- Restored login sessions after ordinary browser refreshes in the student,
  teacher, and admin apps. Teacher and admin recovery now also clear expired
  tokens when the backend session is no longer valid.
- Replaced the advisor path's greedy course selection with a conflict-aware
  search over plans of up to four courses. It minimizes remaining gaps,
  excess credits, and course count in that order while excluding timetable
  conflicts inside the proposed plan.
- Verified `222023321102098` now receives the exact `3.5`-credit path:
  `人工智能` (`3`) plus `学术文化与科研能力 II` (`0.5`).
- Aligned the teacher dashboard and course list around enabled published
  courses. The `teacher_cs` dashboard and active list both show `30` courses;
  disabled historical courses remain visible only in the admin-side totals.
- Removed the tracked local database password from Spring configuration,
  `db-init.js`, and the JDBC smoke test. Startup scripts now inherit
  `MYSQL_PWD` instead of placing its value in backend or MySQL command lines.
- Final build and static verification passed:
  - backend `mvn -DskipTests package`
  - student `npm run build`
  - teacher and admin `npm run build:prod`
  - portal `node --check app.js` and `node --check server.cjs`
  - `git diff --check`
- Final live API assertions passed:
  - `79` enabled curriculum courses across `6` categories with counts
    `21 / 4 / 9 / 10 / 25 / 10`
  - `79` distinct cover URLs, `79` distinct SHA-256 hashes, and no perceptual
    near-duplicate pairs at the checked threshold
  - `16` students with stable `5 / 5 / 6` low, medium, and high-risk counts
  - advisor exact-path and visible-conflict behavior
  - teacher dashboard and enabled list both at `30`
- Final browser regression passed for the portal, all three login pages, all
  three post-login portal returns, student carousel and category refresh,
  student search and course detail, advisor what-if plan, teacher active list
  refresh, and admin risk dashboard refresh.
- Final screenshots:
  - `E:/SmartCourseV3.2/outputs/final-qa-portal.png`
  - `E:/SmartCourseV3.2/outputs/final-qa-student-home.png`
  - `E:/SmartCourseV3.2/outputs/final-qa-student-advisor.png`
  - `E:/SmartCourseV3.2/outputs/final-qa-teacher-courses.png`
  - `E:/SmartCourseV3.2/outputs/final-qa-admin-risk.png`
- Runtime state after QA:
  - portal `9527`, PID `43664`
  - backend `9096`, PID `30132`
  - admin `9528`, PID `36084`
  - teacher `9529`, PID `29584`
  - student `9530`, PID `21248`
- Remaining security note: the public Git history previously contained a local
  database password. Rotate that local password once and consider rewriting
  repository history if the old value must be fully removed from GitHub.

## 2026-06-03 One-Click Startup Portal Opening

- Fixed the one-click startup browser behavior. The three Vue frontends no
  longer auto-open their own pages because `devServer.open` is now `false` in
  the student, teacher, and admin `vue.config.js` files.
- Updated both startup batches (`start_smartcourse.bat` and the Chinese
  one-click startup batch) to open only `http://localhost:9527` after the
  unified portal server starts.
- Root cause: the batch file launched the services, then Vue CLI opened the
  three frontend pages automatically while the portal had no explicit browser
  open step.
- Verification: `node --check` passed for all three `vue.config.js` files,
  student `npm run build` passed, teacher/admin `npm run build:prod` passed,
  and `git diff --check` passed. Build output still contains only the existing
  asset-size/Sass deprecation warnings.
