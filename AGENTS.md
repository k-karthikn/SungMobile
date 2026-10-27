# SungMobile code organization

- Keep each file focused on one responsibility. Do not place a whole feature's UI, data format, persistence, and tests in one file.
- Put Android screens and controls in `app/src/main/java/com/karthi/sungmobile/feature/<feature>/`.
- Put reusable data formats, validation, repositories, and database access in `core/data` under separate, descriptive files.
- Keep app wiring in `app/AppContainer.kt` and `app/SungRoot.kt` small; feature behavior belongs to its feature package.
- Use clear names, small functions, and explicit data models. Preserve existing architecture and avoid duplicate logic.
- Add focused tests for data migration, restoration, and bug fixes when behavior could lose user data.
