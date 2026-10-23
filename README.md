# Task Tracker API

A Java 17 / Spring Boot REST service for projects, users and tasks. Uses Maven,
Spring Data JPA, Hibernate, Bean Validation and an in-memory H2 database.

## Prerequisites and quick start

- JDK 17 or newer (`java -version`)
- Maven 3.6.3 or newer (`mvn -version`)
- Port 8080 available
- Internet access for the first Maven build

Extract the ZIP and open a terminal **inside `task-tracker-api`**, beside `pom.xml`.

```sh
mvn clean verify
mvn spring-boot:run
```

Alternatively, after building:

```sh
java -jar target/task-tracker-api-1.0.0.jar
```

Check startup:

```sh
curl http://localhost:8080/api/info
```

Expected default response: `{"name":"Task Tracker API","version":"1.0.0"}`.
The H2 database is recreated on application startup; data is not persistent.
Stop the server with Ctrl+C. No external database or credentials are needed.

On Windows, use **Git Bash** for the curl examples below. In PowerShell, use
`curl.exe` and adjust JSON quoting for your shell.

## API endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST / GET | `/api/projects` | Create / list projects |
| GET / PUT / DELETE | `/api/projects/{id}` | Read / replace / delete a project |
| POST / GET | `/api/users` | Create / list users |
| GET / PUT / DELETE | `/api/users/{id}` | Read / replace / delete a user |
| POST / GET | `/api/tasks` | Create / list tasks |
| GET / PUT / DELETE | `/api/tasks/{id}` | Read / replace / delete a task |
| GET | `/api/projects/{id}/tasks` | List tasks for an existing project |
| GET | `/api/info` | Configured application name and version |

Successful creation returns **201** and a `Location` header. Reads and updates
return **200**. Deletion returns **204** with no body.

### Runnable example workflow

These examples assume a fresh database: the first project, user and task each
have ID 1. For an existing database, substitute the IDs returned by POST.

```sh
# 1. Create a project
curl -i -X POST http://localhost:8080/api/projects \
  -H 'Content-Type: application/json' \
  -d '{"name":"Media Platform","description":"Task management service"}'

# 2. Create a user
curl -i -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Geethika","email":"geethika@example.com","role":"DEVELOPER"}'

# 3. Create a task; status defaults to TODO and priority to configured MEDIUM
curl -i -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Implement task API","projectId":1,"assigneeId":1,"dueDate":"2030-01-15"}'

# 4. Read individual resources
curl http://localhost:8080/api/projects/1
curl http://localhost:8080/api/users/1
curl http://localhost:8080/api/tasks/1

# 5. Lists, combined filters, pagination and sorting (zero-based page index)
curl 'http://localhost:8080/api/projects?page=0&size=10&sort=name,asc'
curl 'http://localhost:8080/api/users?sort=email,asc'
curl 'http://localhost:8080/api/tasks?status=TODO&projectId=1&page=0&size=10&sort=dueDate,asc'
curl http://localhost:8080/api/projects/1/tasks

# 6. Replace a project and user
curl -X PUT http://localhost:8080/api/projects/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Media Platform v2","description":"Updated project"}'
curl -X PUT http://localhost:8080/api/users/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Geethika","email":"geethika@example.com","role":"MANAGER"}'

# 7. Replace a task, mark DONE and unassign it
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Implement task API","status":"DONE","priority":"HIGH","projectId":1,"assigneeId":null,"dueDate":"2030-01-15"}'

# 8. Delete in dependency order
curl -i -X DELETE http://localhost:8080/api/tasks/1
curl -i -X DELETE http://localhost:8080/api/users/1
curl -i -X DELETE http://localhost:8080/api/projects/1
```

### Pagination and sorting

All list endpoints return a consistent envelope:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

`page` must be nonnegative. `size` is 1–100, defaulting to the configured page
size. `sort` accepts one `field[,asc|desc]`, default `id,asc`. ID is added as a
secondary ordering when another field is selected to keep pagination stable.

| Resource | Allowed sort fields |
| --- | --- |
| Project | id, name, createdAt |
| User | id, name, email, role |
| Task (including project tasks) | id, title, status, priority, dueDate |

Enum sorting is alphabetical by stored enum name; database default null ordering
applies to nullable fields. Task filters `status` and `projectId` can be combined.
A nonexistent `projectId` filter returns 404, consistent with the nested endpoint.

## Validation, relationships and assumptions

- Project name is required, unique, trimmed and at most 150 characters.
  Uniqueness is case-sensitive. Description is optional, up to 2000 characters.
- User name and a valid email are required; name is at most 150 characters and
  email at most 254. Emails are stored in lowercase for case-insensitive uniqueness.
  Role is an optional string up to 50 characters, such as DEVELOPER, QA or MANAGER.
- Task title is required, trimmed and up to 200 characters. A task must reference
  an existing project. An assignee is optional but must exist when supplied.
- Status values: `TODO`, `IN_PROGRESS`, `DONE`. Priority: `LOW`, `MEDIUM`, `HIGH`.
  Enum values are case-sensitive. Dates use `YYYY-MM-DD`; past dates are allowed
  because the brief does not prohibit them.
- Project creation time is server local `LocalDateTime`, set by `@PrePersist`;
  it cannot be changed through request DTOs.
- **PUT is full replacement**, not PATCH. Required fields must be supplied.
  Missing/null optional values are cleared. Missing/null task status becomes
  TODO and priority uses `task.default-priority`, on both creation and update.
  To change only status, send the task's other values along with the new status.
- Deleting a referenced project or user returns 409. Delete project tasks or
  unassign user tasks first. Tasks have no cascading delete into projects/users.
- The scenario mentions project membership, but defines no user–project
  membership table, field or endpoint. This implementation follows the explicit
  data model: users may be assigned to tasks across projects. Membership rules
  would need an additional relationship and requirements clarification.
- No authentication or authorization was requested. User roles are descriptive.
- Unknown request JSON properties are rejected to catch client typos.
- DTOs are returned rather than entities, preventing recursive JSON relationships
  and accidental exposure of lazy persistence objects.

### Error responses

400: invalid DTO, JSON, enum/date, query value or pagination/sorting.
404: requested entity or referenced project/user does not exist.
409: unique name/email conflict, or deletion of a referenced project/user.

```json
{
  "timestamp": "2026-10-23T09:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/tasks",
  "fieldErrors": {"projectId": "must not be null"}
}
```

Database unique and foreign-key constraints back up the service checks, including
races between concurrent requests. Database integrity failures are mapped to 409
without exposing SQL or database internals.

## External configuration

Values live in `src/main/resources/application.properties` and bind to validated
`@ConfigurationProperties` records injected through constructors.

| Property | Supplied default | Environment override |
| --- | --- | --- |
| app.name | Task Tracker API | APP_NAME |
| app.version | 1.0.0 | APP_VERSION |
| task.default-priority | MEDIUM | TASK_DEFAULT_PRIORITY |
| pagination.default-page-size | 20 | PAGINATION_DEFAULT_PAGE_SIZE |

Example (Git Bash / Linux / macOS):

```sh
TASK_DEFAULT_PRIORITY=HIGH PAGINATION_DEFAULT_PAGE_SIZE=5 mvn spring-boot:run
```

You can also use Spring Boot arguments:

```sh
java -jar target/task-tracker-api-1.0.0.jar --task.default-priority=HIGH --pagination.default-page-size=5 --server.port=8081
```

Invalid configuration (for example, an unknown priority or page size over 100)
fails at startup. The H2 console and Open Session in View are disabled.

## Structure and design

```text
src/main/java/com/geethika/tasktracker/
  config/       Validated external properties
  domain/       JPA entities and enums
  dto/          Request, response and pagination records
  repository/   Spring Data JPA queries
  service/      Business rules and transaction boundaries
  web/          Controllers and pagination validation
  exception/    Structured error responses and global handler
src/main/resources/application.properties
src/test/java/com/geethika/tasktracker/
openapi.yaml
Dockerfile
```

Controllers handle HTTP/validation; services enforce business rules; repositories
perform persistence. Constructor injection makes dependencies explicit and easy
to mock. Write services are transactional. Mapping lazy references into DTOs takes
place within service transactions; filtered task lists use an entity graph to
fetch related entities without a per-task association query.

## Tests

```sh
mvn clean verify
```

- JUnit 5 / Mockito service tests: CRUD, missing entities, duplicates, normalization,
  default and explicit priority, assignee validation, assignment clearing,
  combined filters and deletion constraints.
- Pagination tests: configured default, stable sort and rejected inputs.
- `@SpringBootTest` / MockMvc integration tests: real H2 CRUD workflow, foreign
  keys, validation/error statuses, paging, combined filters, full replacement,
  and externally overridden app/default-priority/page-size values.
- Tests use transactions to roll back database changes between integration cases.

Maven reports are written to `target/surefire-reports/` locally. Generated build
artifacts are excluded from the submission.

## OpenAPI specification

`openapi.yaml` in the project root is the OpenAPI **3.0.3** contract, including all
17 method/route combinations, request/response models, query parameters, error
responses, and creation headers. Open it in an OpenAPI-compatible viewer/editor.
Swagger UI is an optional bonus and is not bundled; the YAML is the API contract.

## Docker (optional)

```sh
docker build -t task-tracker-api .
docker run --rm -p 8080:8080 task-tracker-api
```

The multi-stage build runs tests and packages the application; the runtime uses
JRE 17 and a non-root numeric user. Docker Compose is unnecessary for a single
service with embedded H2. Running Docker requires a local Docker installation.

## Submission

The email asks for a ZIP; the PDF also asks for a public GitHub repository. Supply
both. The ZIP includes source, tests, this README, Dockerfile and `openapi.yaml`;
it excludes `target/`, `build/`, IDE folders and compiled files.

To publish from the extracted project, create an empty public GitHub repository
without a generated README, then run (replace YOUR_USERNAME first):

```sh
git init
git add .
git commit -m "Implement Task Tracker API with tests and OpenAPI contract"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/task-tracker-api.git
git push -u origin main
```

Email subject: `[Take-Home] Task Tracker API - Geethika Seemala`.
Review the code, run the workflow locally, and be ready to explain the choices
above before submitting.
