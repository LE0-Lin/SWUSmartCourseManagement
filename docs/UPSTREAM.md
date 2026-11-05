# Upstream sources and attribution

SmartCourse is an academic extension of an existing education platform. It is not a claim that the complete backend and three frontend applications were written from scratch.

## Inherited platform

The existing component READMEs identify these source projects:

- Backend: [snwjas/OnlineEdu](https://github.com/snwjas/OnlineEdu).
- Administrator frontend: [snwjas/OnlineEdu-Admin](https://github.com/snwjas/OnlineEdu-Admin).
- Teacher frontend: [snwjas/OnlineEdu-Teacher](https://github.com/snwjas/OnlineEdu-Teacher).
- Student frontend: [snwjas/OnlineEdu-App](https://github.com/snwjas/OnlineEdu-App).

The administrator frontend also retains its Vue admin template notice. Original copyright and license files remain in their component directories. Do not remove those notices when modifying inherited code.

## SmartCourse extensions

The repository's own commit history records the addition and refinement of:

- Week-aware course schedules and course-selection conflict checks.
- Weighted usual/exam grades and student-number login integration.
- Curriculum categories, per-category credit completion, and graduation-risk scenarios.
- Conflict-aware what-if course-path planning and a unified role portal.
- Full-catalog, bounded planning with search/data-quality metadata, regression fixtures, automated verification, and the current repository documentation.

The [development timeline](development-timeline.md) links representative commits. These extensions reuse inherited authentication, CRUD, media, and frontend infrastructure. A feature being present in this repository does not establish authorship of the underlying platform.

## Images and third-party assets

University photographs and logos have their source URLs recorded in [SmartCourse-Portal/PHOTO_SOURCES.md](../SmartCourse-Portal/PHOTO_SOURCES.md). They are not covered by this repository's MIT code license. The current advisor screenshots are captures of this repository's component using synthetic integration-test fixtures.
