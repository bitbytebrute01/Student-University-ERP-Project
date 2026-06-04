# PROJECT AUDIT REPORT

## 1. Broken Features / Issues
- **Theme Switching:** While `ThemeManager` is implemented, UI components are not consistently updating across all open JFrames/JDialogs when toggled.
- **Course-Assignment Link:** Faculty assignment creation workflow is currently loose; it needs stricter validation to ensure an assignment is always linked to a specific course.
- **Admin Attendance:** Currently missing a direct navigation link and dashboard for Admin-level attendance analytics.

## 2. Incomplete Workflows
- **Project Uploads:** Files are saved, but the UI feedback for successful/failed uploads needs to be more robust.
- **Course CRUD:** Faculty course management is partially functional but needs validation against orphan assignments.
- **Dark Mode:** Needs a global trigger to update all active windows instantly.

## 3. Database Relationships
- **Foreign Keys:** Need to verify `assignments` -> `courses` and `submissions` -> `assignments` referential integrity in practice, not just schema definition.
- **Persistence:** Ensure `user_preferences` is truly persisting across sessions.

## 4. Missing CRUD / UI
- **Admin Attendance Dashboard:** Missing.
- **Student Dashboard:** Needs more real data widgets (Charts for attendance, course progress).
- **Course Management:** Admin needs a better view of all courses.

## Plan of Action
1. Fix Theme persistence and global UI updates.
2. Implement Admin Attendance Dashboard.
3. Hardcode/Seed better academic data (Step 5 of user instructions).
4. Refactor `MainFrame` to ensure UI scaling.
