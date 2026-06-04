# Next-Gen Smart AI University Ecosystem

An advanced, feature-rich University ERP transformed into a production-grade ecosystem. It features an AI-powered Student Identity System, a complete LMS module, Placement analytics, and an interactive AI Academic Copilot.

## Key Features

### 🎓 Student Digital Identity System
- **Comprehensive Profiles:** Skills, Projects, Certifications, and Internships.
- **Digital ID Card:** QR-based identity verification.
- **Resume Generator:** Automated professional resume creation.

### 📚 Assignment Management System (LMS)
- **Faculty Portal:** Create assignments, set deadlines, and attach resources.
- **Student Portal:** Track assignments, upload submissions, and view graded feedback.
- **Gradebook:** Centralized tracking of assignment marks.

### 🤖 AI Academic Copilot
- **Placement Readiness:** AI-calculated scores based on academics and skills.
- **Career Mentor:** Personalized career and skill recommendations.
- **AI Chatbot:** Heuristic-based assistant for quick academic queries.

### 🛠 Technical Upgrades
- **Maven Integration:** Standardized build and dependency management.
- **SQLite Support:** Foundation for persistent relational data.
- **Modern UI:** Tabbed dashboards and interactive analytics.

---

## Quick Start (Maven)

Run these commands from the project root:

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
chmod +x run.sh
./run.sh
```

Or manually:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.university.main.UniversityERP"
```

Default admin login:

```text
Username: admin
Password: admin123
```

Seeded student login:

```text
Username: alice
Password: pass
```

To stop the program:

```text
Press Ctrl+C
```

There is no proper Exit option yet, so Ctrl+C is currently required.

---

## Command Center

Use these collapsible command blocks as a copy-paste runbook.

<details>
<summary>Fresh compile and run on macOS/Linux</summary>

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
rm -rf out
mkdir -p out
javac -d out $(find university -name "*.java")
java -cp out com.university.main.UniversityERP
```

</details>

<details>
<summary>Fresh compile and run on Windows PowerShell</summary>

```powershell
cd "C:\path\to\Student-University-ERP-Project"
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force out
Get-ChildItem -Recurse -Filter *.java university | ForEach-Object { $_.FullName } > sources.txt
javac -d out @sources.txt
java -cp out com.university.main.UniversityERP
```

</details>

<details>
<summary>Run RMI client after the ERP is running</summary>

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
java -cp out com.university.rmi.client.StudentClient
```

</details>

<details>
<summary>Create and run an executable JAR</summary>

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
rm -rf out university-erp.jar
mkdir -p out
javac -d out $(find university -name "*.java")
jar --create --file university-erp.jar --main-class=com.university.main.UniversityERP -C out .
java -jar university-erp.jar
```

</details>

<details>
<summary>Reset demo data</summary>

macOS/Linux:

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
rm -f *.dat
```

Windows PowerShell:

```powershell
Remove-Item *.dat -ErrorAction SilentlyContinue
```

</details>

<details>
<summary>IDE run configuration</summary>

Use these values in IntelliJ IDEA, Eclipse, VS Code, or NetBeans:

```text
Main class: com.university.main.UniversityERP
Classpath/module output: out
Working directory: /Users/adityajoshi/Desktop/Student-University-ERP-Project
```

If the IDE asks for a source root, use the project folder that contains `university/`.

</details>

---

## Project Snapshot

```text
university/
  ai/                 AI-style recommendation and risk heuristics
  attendance/         Attendance records and attendance percentage updates
  authentication/     Login, users, sessions, roles
  core/               Base person model
  courses/            Course creation, faculty assignment, enrollments
  examination/        Marks upload, GPA calculation, result generation
  exceptions/         Custom checked exceptions
  faculty/            Faculty records
  fees/               Fee payment and receipt logic
  hostel/             Hostel room allocation
  interfaces/         Shared contracts
  library/            Book and issue record handling
  main/               Main console application entry point
  models/             Student, Course, Book, Faculty, Admin, Room, Company
  placement/          Company registration and eligibility checks
  reports/            Student report generation
  rmi/                Java RMI server, client, and shared service interface
  students/           Student CRUD/search/sort logic
  threads/            Background attendance, fee, and notification threads
  utils/              File serialization helper
```

Main class:

```text
com.university.main.UniversityERP
```

Optional RMI client class:

```text
com.university.rmi.client.StudentClient
```

---

## Architecture Map

```mermaid
flowchart TD
    A[Console UI: UniversityERP] --> B[AuthenticationManager]
    A --> C[StudentManager]
    A --> D[CourseManager]
    A --> E[AttendanceManager]
    A --> F[ExamManager]
    A --> G[FeeManager]
    A --> H[LibraryManager]
    A --> I[HostelManager]
    A --> J[PlacementManager]
    A --> K[AIRecommendationEngine]
    A --> L[UniversityServer RMI]

    L --> M[StudentClient]

    B --> N[(users.dat)]
    C --> O[(students.dat)]
    D --> P[(courses.dat)]
    D --> Q[(enrollments.dat)]
    E --> R[(attendance.dat)]
    F --> S[(marks.dat)]
    H --> T[(books.dat)]
    I --> U[(hostel.dat)]
    J --> V[(companies.dat)]
```

Runtime idea:

```text
User
  -> Terminal Menu
    -> Managers
      -> Serialized .dat files

Optional RMI Client
  -> RMI Server on localhost:1099
    -> Managers
      -> Serialized .dat files
```

---

## Requirements

You need the JDK, not only the JRE.

Recommended:

```text
Java 17 or newer
```

Tested successfully with:

```text
javac 21.0.11
OpenJDK 21.0.11
```

Check your Java setup:

```bash
javac -version
java -version
```

If `javac` is missing, install a JDK.

---

## Build Commands

### macOS / Linux

Clean old compiled files:

```bash
rm -rf out
```

Compile:

```bash
mkdir -p out
javac -d out $(find university -name "*.java")
```

Run:

```bash
java -cp out com.university.main.UniversityERP
```

### Windows PowerShell

Clean old compiled files:

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
```

Compile:

```powershell
New-Item -ItemType Directory -Force out
Get-ChildItem -Recurse -Filter *.java university | ForEach-Object { $_.FullName } > sources.txt
javac -d out @sources.txt
```

Run:

```powershell
java -cp out com.university.main.UniversityERP
```

---

## Interactive Execution Flow

When the app starts, you should see output similar to:

```text
Starting AI-Powered Distributed University ERP...
[AttendanceThread] Checking for low attendance...
[FeeProcessingThread] Processing pending fees...
University RMI Server is running...

--- LOGIN ---
Username:
```

Login as admin:

```text
Username: admin
Password: admin123
```

Admin menu:

```text
--- ADMIN DASHBOARD ---
1. Add Student
2. View All Students
3. AI: Detect At-Risk Students
4. Logout
Choice:
```

Try this simple demo path:

```text
admin
admin123
2
4
Ctrl+C
```

Expected result for option `2`:

```text
Student Profile:
ID: S101
Name: Alice
Email: alice@univ.edu
Department: CS
Semester: 1
CGPA: 8.5
Attendance: 85.0%
```

---

## UI Integration Status

### Current UI

The current user interface is a terminal-based menu system inside:

```text
university/main/UniversityERP.java
```

It supports:

| Role | Current menu features |
| --- | --- |
| Admin | Add student, view students, detect at-risk students, logout |
| Faculty | Logout only |
| Student | Placeholder profile view, logout |

### Suggested UI Integration

The backend managers are already separated enough that a better UI can be added later. Recommended UI options:

| UI Type | Best for | Notes |
| --- | --- | --- |
| Java Swing | Simple desktop GUI | No external server needed |
| JavaFX | Modern Java desktop GUI | Cleaner UI, but needs JavaFX setup |
| Web UI + REST API | Best long-term choice | Requires adding an HTTP layer such as Spring Boot |
| Improved Console UI | Fastest improvement | Add full menus for all existing managers |

Recommended future structure:

```text
UI Layer
  -> Admin Dashboard
  -> Faculty Dashboard
  -> Student Dashboard

Service Layer
  -> StudentManager
  -> CourseManager
  -> AttendanceManager
  -> ExamManager
  -> FeeManager
  -> LibraryManager
  -> HostelManager
  -> PlacementManager

Storage Layer
  -> Serialized .dat files, or later a database
```

The easiest next UI upgrade is to expand the existing console menus before introducing Swing, JavaFX, or a web app.

### Console UI Wireframe

Current UI:

```text
+--------------------------------------------------+
| AI-Powered Distributed University ERP            |
+--------------------------------------------------+
| LOGIN                                            |
| Username:                                        |
| Password:                                        |
+--------------------------------------------------+
        |
        v
+----------------------+  +----------------------+  +----------------------+
| Admin Dashboard      |  | Faculty Dashboard    |  | Student Dashboard    |
| 1. Add Student       |  | 1. Logout            |  | 1. View Profile      |
| 2. View Students     |  |                      |  | 2. Logout            |
| 3. AI Risk Detection |  | Lacking features     |  | Profile incomplete   |
| 4. Logout            |  |                      |  |                      |
+----------------------+  +----------------------+  +----------------------+
```

Recommended future UI:

```text
+----------------------+  +----------------------+  +----------------------+
| Admin Dashboard      |  | Faculty Dashboard    |  | Student Dashboard    |
| Students             |  | Attendance           |  | Profile              |
| Faculty              |  | Marks Upload         |  | Attendance           |
| Courses              |  | Course Students      |  | Results              |
| Library              |  | Reports              |  | Fees                 |
| Hostel               |  | Notifications        |  | Library              |
| Placements           |  |                      |  | Placements           |
| Reports              |  |                      |  |                      |
+----------------------+  +----------------------+  +----------------------+
```

---

## RMI Execution

The main ERP automatically starts an RMI server on port `1099`.

To test the RMI client, use two terminals.

Terminal 1:

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
java -cp out com.university.main.UniversityERP
```

Terminal 2:

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
java -cp out com.university.rmi.client.StudentClient
```

Expected RMI client output:

```text
Connected to University Server via RMI.
```

If port `1099` is already busy on macOS/Linux:

```bash
lsof -i :1099
```

Then stop the process using that port, or change the RMI port in:

```text
university/rmi/server/UniversityServer.java
```

---

## Data Files

The app stores data using Java object serialization in `.dat` files in the directory where you run the app.

Common files:

```text
users.dat
students.dat
courses.dat
enrollments.dat
attendance.dat
marks.dat
faculty.dat
books.dat
issues.dat
hostel.dat
companies.dat
```

Reset all local app data:

```bash
rm -f *.dat
```

Then rerun:

```bash
java -cp out com.university.main.UniversityERP
```

This recreates the default admin user and seeded demo data.

Important: because the data files are created relative to the current terminal directory, running the app from a different folder can create a separate set of `.dat` files.

---

## Available Modules

| Module | Status | Description |
| --- | --- | --- |
| Authentication | Partially exposed | Admin and student login works through serialized users |
| Session Management | Exposed | Tracks active user session |
| Student Management | Partially exposed | Add student and view students are in admin menu |
| Course Management | Backend only | Add courses, enroll/drop students, assign faculty |
| Faculty Management | Backend only | Add, remove, search, sort faculty |
| Attendance | Backend plus thread | Mark attendance and update attendance percentage |
| Examination | Backend only | Upload marks and generate result text |
| Fees | Backend plus thread | Process fee payments and generate receipts |
| Library | Backend only | Add, borrow, return, and search books |
| Hostel | Backend only | Add rooms, allocate/vacate rooms, print occupancy |
| Placement | Backend plus RMI usage | Register companies and check eligible students |
| Reports | Backend only | Generate overall student report |
| AI Engine | Partially exposed | Detect at-risk students from attendance and CGPA |
| RMI Server | Auto-started | Exposes student, attendance, library, and placement checks |
| RMI Client | Demo only | Connects to the RMI server |
| Background Threads | Auto-started | Attendance check, fee reminder, notification queue |

---

## Major Improvements & Fixed Areas

The project has been significantly upgraded from a basic prototype to a functional ecosystem:

1. **Full Graphical UI Integrated:**
   The project now features a comprehensive Swing-based Dashboard for Admins, Faculty, and Students, replacing the legacy console-only interface.

2. **Backend Modules Connected:**
   All modules including Course, Faculty, Library, LMS, and Placements are now fully reachable via the graphical sidebar navigation.

3. **Complete Faculty & Student Dashboards:**
   Faculty can now manage assignments and upload marks. Students have a full Digital Identity profile, LMS access, and an AI Assistant.

4. **Modern Build System:**
   Introduced **Maven** for professional dependency management and streamlined build/execution.

5. **AI Academic Copilot:**
   Enhanced heuristic engine with Placement Readiness scoring, Career Mentoring, and an interactive Chatbot.

6. **LMS Module:**
   Implemented a brand-new Assignment Management System with bidirectional file-based submission tracking.

7. **Database Foundation:**
   Initialized Maven dependencies for **SQLite** to support the transition from serialization to a relational database.

---

## Remaining Areas for Enhancement
1. **Full Database Migration:** Complete the shift from `.dat` files to SQLite tables.
2. **Advanced Security:** Implement BCrypt password hashing and JWT for the upcoming REST API layer.
3. **Real Machine Learning:** Transition the AI module from heuristics to a trained ML model (e.g., using Deeplearning4j or Python bridge).


---

## Troubleshooting

### `javac: command not found`

Install a JDK, then verify:

```bash
javac -version
```

### `Could not find or load main class`

Make sure you compiled with `-d out` and used the full package name:

```bash
javac -d out $(find university -name "*.java")
java -cp out com.university.main.UniversityERP
```

### Login fails

Reset the serialized data files:

```bash
rm -f *.dat
java -cp out com.university.main.UniversityERP
```

Then use:

```text
admin / admin123
```

### RMI server fails

Port `1099` may already be in use.

```bash
lsof -i :1099
```

Stop the conflicting process or change the port in the RMI server and client.

### Data disappeared

Make sure you are running from the same project root each time:

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
```

The `.dat` files are relative to the current directory.

---

## Stabilization Status

The primary workflow uses the Swing UI backed by SQLite. Demo login verification is available with:

```bash
mvn exec:java -Dexec.mainClass=com.university.main.UniversityERP -Dexec.args="--verify-login"
```
11. Make RMI host and port configurable.
12. Add proper input validation and duplicate-record checks.

---

## One-Command Local Demo

Use this when you just want to compile and launch quickly:

```bash
cd /Users/adityajoshi/Desktop/Student-University-ERP-Project
rm -rf out
mkdir -p out
javac -d out $(find university -name "*.java")
java -cp out com.university.main.UniversityERP
```

Then login:

```text
admin
admin123
```

---

## Submission Notes

If this is being submitted as a university project, mention clearly that:

```text
The project demonstrates Java OOP, packages, inheritance, interfaces, exceptions,
collections, file handling, multithreading, Java RMI, and a basic terminal UI.
```

Also mention clearly that:

```text
The current version is a prototype/demo and not production-ready because it lacks
a complete UI, secure authentication, database storage, tests, and complete menu
integration for all modules.
```
