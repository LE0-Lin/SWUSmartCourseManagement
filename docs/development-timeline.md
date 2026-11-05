# Development timeline

This page organizes the repository's existing development records for software-engineering study. Dates below are shown in Asia/Shanghai time. Follow the commit links to inspect the actual changes and authorship; a commit does not by itself establish that a feature was comprehensively verified at that date.

| Date | Change | Engineering lesson |
| --- | --- | --- |
| 2026-04-07 | [Import the education platform](https://github.com/LE0-Lin/SWUSmartCourseManagement/commit/276905b612ed61f5be6e7e5f7c527b3d701a8dd2) | Record upstream provenance before adding project-specific functionality. |
| 2026-04-07 | [Add grade and schedule management](https://github.com/LE0-Lin/SWUSmartCourseManagement/commit/0477b8d3c4ffd9577e211d4843db720be903116a) | Extend the domain model and connect it to role-specific workflows. |
| 2026-04-15 | [Fix week-aware timetable flow](https://github.com/LE0-Lin/SWUSmartCourseManagement/commit/34938b8c408977bd4cd44aadbbc0636319c47089) | A timetable needs teaching-week intervals as well as weekdays and sections. |
| 2026-05-06 | [Add the graduation advisor](https://github.com/LE0-Lin/SWUSmartCourseManagement/commit/f4fc16367093835baec2aee6cde4295ea3427ed3) | Express curriculum gaps as explicit rules and observable explanations. |
| 2026-05-07 | [Add weighted usual/exam grading](https://github.com/LE0-Lin/SWUSmartCourseManagement/commit/f831a522b7e177f1faa2fa0b008e12e8634e50cf) | Preserve an understandable relationship between raw inputs and reported grades. |
| 2026-06-01 | [Archive the integrated demonstration](https://github.com/LE0-Lin/SWUSmartCourseManagement/commit/f08c9971187d06b172bfee7cbbd239ee164bee56) | Bring role interfaces, curriculum data, and conflict-aware path search into one demonstrable system. |
| 2026-06-03 | [Make upload paths portable](https://github.com/LE0-Lin/SWUSmartCourseManagement/commit/f909fc1c0ea6c37e56e76cf41dd439ce341596f6) | Deployment should not depend on the original machine's directory layout. |
| 2026-06-18 | [Merge the archived system](https://github.com/LE0-Lin/SWUSmartCourseManagement/commit/2fe21403659d2ae3b58bce64c2a2233950551f93) | Preserve a reviewable integration point. |
| 2026-11-05 | Full-catalog bounded planning, evidence metadata, automated verification, and fixture preview | Separate presentation from decision logic, test the actual SQL adapter, and report the limits of a computed result. |

## Study the current regression

1. **Problem:** the previous dashboard passed only twelve display cards into its planner. A less popular, exact-fit course could be invisible to the search.
2. **Design:** batch the catalog's timetables and pass all eligible candidates to a pure search module. Keep display truncation downstream.
3. **Implementation:** define the objective and a deterministic work budget; expose completed search, incomplete data, and budget exhaustion separately.
4. **Verification:** reproduce the thirteenth-course counterexample with actual JDBC queries, compare small catalogs against exhaustive subset enumeration, and render the actual Vue component using the generated response.
5. **Remaining questions:** validate a broader set of real curricula, modernize dependencies, and add MySQL/authentication/enrollment tests before expanding deployment claims.
