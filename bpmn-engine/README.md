# BPMN 2.0 Process Engine

A **from-scratch, production-ready BPMN 2.0 process engine** built with Java 21 and Spring Boot 3.  
It parses standard BPMN 2.0 XML, executes process models using a token-based state machine, and persists all runtime state to a relational database (H2 for development, PostgreSQL for production).

---

## Table of Contents

1. [Features](#features)
2. [Quick Start](#quick-start)
3. [Project Structure](#project-structure)
4. [Architecture](#architecture)
5. [BPMN 2.0 Elements Supported](#bpmn-20-elements-supported)
6. [Database Schema](#database-schema)
7. [REST API Reference](#rest-api-reference)
8. [Process Definition & Versioning API](#process-definition--versioning-api)
9. [Identity API — Users, Roles & Groups](#identity-api)
10. [Expression Language](#expression-language)
11. [Timer Support](#timer-support)
12. [Implementing a JavaDelegate](#implementing-a-javadelegate)
13. [Writing Custom Element Handlers](#writing-custom-element-handlers)
14. [Configuration Reference](#configuration-reference)
15. [Running with PostgreSQL](#running-with-postgresql)
16. [Testing](#testing)
17. [Extending the Engine](#extending-the-engine)

---

## Features

| Capability | Detail |
|---|---|
| **Full BPMN 2.0 XML parsing** | All standard elements from the OMG BPMN 2.0 spec |
| **Token-based execution** | Proper parallel branching, joining, and wait states |
| **All event types** | Start, End, Intermediate Catch/Throw, Boundary — with all 8 event definitions |
| **All gateway types** | Exclusive, Inclusive, Parallel, Event-Based, Complex |
| **All task types** | User, Service, Script, BusinessRule, Manual, Receive, Send, CallActivity, SubProcess |
| **Loop & multi-instance** | Standard loop and multi-instance (sequential & parallel) |
| **Expression language** | JUEL (`${amount > 100}`, `#{myBean.method()}`) on conditions and assignments |
| **Timer support** | ISO 8601 date, duration (`PT1H`), repeating cycle (`R3/PT30M`), and cron via Quartz |
| **DB persistence** | 15-table relational schema — definitions, instances, executions, variables, tasks, jobs, subscriptions |
| **Full audit history** | Process-instance and activity-instance history tables |
| **Process versioning** | Each deployment creates a new immutable version; active version switchable at runtime |
| **Suspend / activate** | Suspend a process key or a specific version to block new instance creation |
| **Identity system** | Built-in users, roles, groups and membership with BCrypt passwords |
| **REST API** | Deploy, version, start, manage instances, tasks, messages, signals, and identity |
| **Flyway migrations** | 3-script versioned schema (V1 core, V2 identity, V3 versioning) |

---

## Quick Start

### Prerequisites

| Requirement | Version |
|---|---|
| Java | 21+ |
| Maven | 3.8+ |

### Build & run (H2 in-memory)

```bash
cd bpmn-engine
mvn spring-boot:run
```

The engine starts on **http://localhost:8080**.  
The H2 console is available at **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:bpmndb`).

### Run tests

```bash
mvn test
```

---

## Project Structure

```
bpmn-engine/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/bpmnengine/
    │   │   ├── BpmnEngineApplication.java          ← Spring Boot entry point
    │   │   ├── api/
    │   │   │   ├── ProcessController.java           ← Legacy deployment & instance REST API
    │   │   │   ├── ProcessDefinitionController.java ← Versioning REST API (/api/definitions)
    │   │   │   ├── TaskController.java              ← Task REST API
    │   │   │   ├── ApiExceptionHandler.java         ← Global error handler
    │   │   │   └── dto/
    │   │   │       ├── DeployProcessRequest.java    ← Rich deploy request (deployedBy, deployMessage)
    │   │   │       ├── DeployProcessResponse.java
    │   │   │       ├── ProcessDefinitionDto.java    ← Full definition view (incl. bpmnXml)
    │   │   │       └── ProcessVersionSummaryDto.java← Version history response
    │   │   ├── engine/
    │   │   │   ├── ProcessEngine.java               ← Public engine interface (all operations)
    │   │   │   ├── ProcessEngineImpl.java           ← Core execution engine
    │   │   │   ├── ExecutionContext.java            ← Runtime token / variable scope
    │   │   │   ├── ExecutionOutcome.java            ← Handler return value
    │   │   │   ├── ProcessInstanceResult.java       ← API result DTO
    │   │   │   ├── HistoryService.java              ← Audit history recording
    │   │   │   ├── VariableMapper.java              ← Java ↔ DB variable type mapping
    │   │   │   ├── EngineException.java
    │   │   │   ├── delegate/
    │   │   │   │   └── JavaDelegate.java            ← Service task delegate interface
    │   │   │   ├── expression/
    │   │   │   │   ├── ExpressionEvaluator.java     ← Evaluator interface
    │   │   │   │   ├── ExpressionEvaluationException.java
    │   │   │   │   └── JuelExpressionEvaluator.java ← JUEL implementation
    │   │   │   ├── handler/                         ← 14 handlers — one per BPMN element type
    │   │   │   │   ├── FlowElementHandler.java
    │   │   │   │   ├── FlowElementHandlerRegistry.java
    │   │   │   │   ├── StartEventHandler.java
    │   │   │   │   ├── EndEventHandler.java
    │   │   │   │   ├── IntermediateCatchEventHandler.java
    │   │   │   │   ├── IntermediateThrowEventHandler.java
    │   │   │   │   ├── UserTaskHandler.java         ← Resolves candidateGroups via IdentityService
    │   │   │   │   ├── ServiceTaskHandler.java
    │   │   │   │   ├── ScriptTaskHandler.java
    │   │   │   │   ├── ExclusiveGatewayHandler.java
    │   │   │   │   ├── ParallelGatewayHandler.java
    │   │   │   │   ├── InclusiveGatewayHandler.java
    │   │   │   │   ├── EventBasedGatewayHandler.java
    │   │   │   │   ├── CallActivityHandler.java
    │   │   │   │   ├── SubProcessHandler.java
    │   │   │   │   └── ReceiveTaskHandler.java
    │   │   │   └── timer/
    │   │   │       ├── TimerScheduler.java          ← Quartz integration
    │   │   │       └── TimerJob.java                ← Quartz job implementation
    │   │   ├── model/                               ← 43 BPMN domain model classes
    │   │   ├── identity/
    │   │   │   ├── api/                             ← UserController, RoleController, GroupController
    │   │   │   ├── dto/                             ← UserDto, RoleDto, GroupDto, CreateUserRequest, …
    │   │   │   ├── entity/                          ← UserEntity, RoleEntity, GroupEntity, MembershipEntity
    │   │   │   ├── repository/                      ← 4 Spring Data repositories
    │   │   │   └── service/                         ← UserService, RoleService, GroupService, IdentityService
    │   │   ├── parser/
    │   │   │   ├── BpmnXmlParser.java               ← Full BPMN 2.0 XML parser
    │   │   │   └── BpmnParseException.java
    │   │   └── persistence/
    │   │       ├── entity/                          ← 10 JPA entities
    │   │       └── repository/                      ← 9 Spring Data repositories
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/
    │       │   ├── V1__init_bpmn_engine_schema.sql  ← Core 15-table schema
    │       │   ├── V2__identity_schema.sql          ← Identity tables + 13 seed users
    │       │   └── V3__process_version_support.sql  ← Adds active/deployedBy/deployMessage columns
    │       └── samples/
    │           └── employee-onboarding.bpmn20.xml   ← Sample process
    └── test/
        ├── java/com/bpmnengine/
        │   ├── parser/BpmnXmlParserTest.java                          ← 8 tests
        │   ├── engine/ExclusiveGatewayHandlerTest.java                ← 2 tests
        │   ├── engine/VariableMapperTest.java                         ← 5 tests
        │   ├── engine/expression/JuelExpressionEvaluatorTest.java     ← 9 tests
        │   ├── engine/ProcessVersioningIntegrationTest.java           ← 25 tests
        │   └── identity/service/IdentityServiceIntegrationTest.java   ← 22 tests
        └── resources/application.yml
```

---

## Architecture

### Execution Model

The engine uses a **token-based execution model** (also called "process token" or "execution pointer"):

```
 ┌─────────────────────────────────────────────────────────────────┐
 │  ProcessEngineImpl.executeElement(ctx)                          │
 │                                                                 │
 │   while (ctx.isActive() && !ctx.isEnded()) {                   │
 │     FlowElement current = ctx.getCurrentFlowElement()          │
 │     FlowElementHandler handler = registry.getHandler(current)  │
 │     ExecutionOutcome outcome = handler.execute(ctx, current)   │
 │                                                                 │
 │     if (outcome.isWait())  → pause, save state to DB           │
 │     if (outcome.isEnd())   → complete the process              │
 │     else                   → takeSequenceFlow(nextFlow)        │
 │   }                                                             │
 └─────────────────────────────────────────────────────────────────┘
```

- **`ExecutionContext`** — carries the current position, all variables (scoped up the parent chain), and metadata about the running instance.
- **`FlowElementHandler`** — stateless Spring `@Component`. The registry picks the right handler for the current element type.
- **`ExecutionOutcome`** — what the handler returns: `wait()`, `end()`, or `continueOn(List<SequenceFlow>)`.
- **Parallel branches** — when a handler returns multiple outgoing flows, the engine forks child `ExecutionEntity` records and creates sibling `ExecutionContext` objects.

### Component Diagram

```
REST Client
    │
    ├── ProcessController       (POST /api/deployments, /api/processes/*)
    ├── ProcessDefinitionController  (GET|POST|DELETE /api/definitions/*)
    ├── TaskController          (GET|POST /api/tasks/*)
    └── identity/api/*Controller     (/api/identity/users|roles|groups)
              │
              ▼
        ProcessEngine (interface)
              │
              ▼
        ProcessEngineImpl ────────────────────────────────────────────┐
              │                                                        │
              ├── BpmnXmlParser          parse BPMN XML → BpmnModel   │
              ├── FlowElementHandlerRegistry                           │
              │     └── [14 handlers]    execute each element type    │
              ├── JuelExpressionEvaluator evaluate ${...} conditions  │
              ├── TimerScheduler          schedule Quartz timer jobs  │
              ├── HistoryService          write audit records         │
              ├── IdentityService         resolve candidate groups    │
              └── JPA Repositories ───────────────────────────────────┘
                        │
                        ▼
                H2 / PostgreSQL  (via 3 Flyway migrations)
```

---

## BPMN 2.0 Elements Supported

### Events

| Element | Types / Definitions Supported |
|---|---|
| `<startEvent>` | None, Timer, Message, Signal, Conditional, Error, Escalation |
| `<endEvent>` | None, Terminate, Error, Escalation, Message, Signal, Compensate |
| `<intermediateCatchEvent>` | Timer, Message, Signal, Conditional, Compensate |
| `<intermediateThrowEvent>` | None, Signal, Message, Escalation, Compensate |
| `<boundaryEvent>` | Timer, Message, Signal, Error, Escalation, Conditional, Compensate (interrupting & non-interrupting) |

### Tasks

| Element | Execution |
|---|---|
| `<userTask>` | Creates a task record; waits; resumes on `completeTask()` |
| `<serviceTask>` | Calls a `JavaDelegate`, Spring EL expression, or delegate expression |
| `<scriptTask>` | Executes JavaScript / Groovy via JSR-223 `ScriptEngine` |
| `<businessRuleTask>` | Hook for a DMN engine (logs warning if none configured) |
| `<manualTask>` | Pass-through (informational; no engine action) |
| `<receiveTask>` | Waits for a message; resumes on `correlateMessage()` |
| `<sendTask>` | Pass-through (expression/delegate execution like serviceTask) |
| `<callActivity>` | Starts a child process instance; waits if child does not complete synchronously |
| `<subProcess>` | Enters embedded scope with its own start event |
| `<transaction>` | Treated as subProcess |
| `<adHocSubProcess>` | Treated as subProcess |

### Gateways

| Element | Behaviour |
|---|---|
| `<exclusiveGateway>` | Evaluates conditions in order; takes first `true` path or default flow |
| `<inclusiveGateway>` | Evaluates all conditions; takes every `true` path or default flow |
| `<parallelGateway>` | Fork: activates all outgoing flows. Join: waits for all incoming tokens |
| `<eventBasedGateway>` | Registers subscriptions for all following events; takes first to fire |
| `<complexGateway>` | Passes through (custom `activationCondition` evaluable; plug in your own logic) |

### Loop Characteristics

| Feature | Details |
|---|---|
| `<multiInstanceLoopCharacteristics>` | Sequential and parallel; `collection`, `elementVariable`, `completionCondition` |
| `<standardLoopCharacteristics>` | `loopCondition`, `testBefore`, `loopMaximum` |

### Data & Artifacts

`<dataObject>`, `<dataObjectReference>`, `<dataStoreReference>`, `<dataInput>`, `<dataOutput>`, `<ioSpecification>`, `<dataInputAssociation>`, `<dataOutputAssociation>`, `<laneSet>`, `<lane>`, `<textAnnotation>`, `<association>`

### Global Definitions

`<message>`, `<signal>`, `<error>`, `<escalation>`, `<itemDefinition>`

---

## Database Schema

The schema is managed by **Flyway** (`V1__init_bpmn_engine_schema.sql`).

### Core Tables

| Table | Purpose |
|---|---|
| `process_definition` | Deployed BPMN process definitions (versioned by key) |
| `deployment` | Deployment bundles |
| `process_instance` | Running/completed process instances |
| `execution` | Individual tokens (one per active path of execution) |
| `process_variable` | Typed process variables (String, Boolean, Integer, Long, Double, Bytes, JSON, Null) |
| `task` | User tasks (CREATED → ASSIGNED → COMPLETED) |
| `job` | Async jobs and timer jobs |
| `event_subscription` | Waiting message/signal/conditional subscriptions |
| `incident` | Error incidents |

### History Tables

| Table | Purpose |
|---|---|
| `hist_process_instance` | Immutable record of every process instance lifecycle |
| `hist_activity_instance` | Per-activity start/end times and durations |
| `hist_variable_instance` | Variable value snapshots |

### Variable Types

| Type | Storage column |
|---|---|
| `STRING` | `text_value` |
| `BOOLEAN` | `text_value` (`"true"` / `"false"`) |
| `INTEGER` | `long_value` |
| `LONG` | `long_value` |
| `DOUBLE` | `double_value` |
| `BYTES` | `bytes_value` |
| `JSON` | `text_value` (Jackson-serialized) |
| `NULL` | — |

---

## REST API Reference

Base URL: `http://localhost:8080/api`

### Deployment

#### Deploy a process

```http
POST /api/deployments
Content-Type: application/json

{
  "resourceName": "my-process.bpmn20.xml",
  "bpmnXml": "<?xml version=\"1.0\"?>..."
}
```

**Response `200`:**
```json
{ "processDefinitionId": "a1b2c3d4-..." }
```

---

### Process Instances

#### Start a process instance

```http
POST /api/processes/{processKey}/start
Content-Type: application/json

{
  "businessKey": "ORDER-001",
  "variables": {
    "amount": 1500,
    "customerId": "cust-42"
  }
}
```

**Response `200`:**
```json
{
  "id": "pi-uuid",
  "businessKey": "ORDER-001",
  "processDefinitionKey": "orderProcess",
  "state": "RUNNING",
  "startTime": "2024-06-01T10:00:00Z"
}
```

#### Get process instance

```http
GET /api/processes/{instanceId}
```

#### Suspend / Activate

```http
POST /api/processes/{instanceId}/suspend
POST /api/processes/{instanceId}/activate
```

#### Terminate

```http
DELETE /api/processes/{instanceId}?reason=Cancelled+by+admin
```

---

### Tasks

#### List open tasks

```http
GET /api/tasks
```

#### Get a specific task

```http
GET /api/tasks/{taskId}
```

#### Complete a task

```http
POST /api/tasks/{taskId}/complete
Content-Type: application/json

{
  "approved": true,
  "comment": "Looks good"
}
```

#### Claim a task

```http
POST /api/tasks/{taskId}/claim?userId=john.doe
```

#### Tasks by assignee

```http
GET /api/tasks/assignee/{userId}
```

#### Tasks for a process instance

```http
GET /api/processes/{instanceId}/tasks
```

---

### Messages & Signals

#### Correlate a message

```http
POST /api/messages/{messageName}/correlate
Content-Type: application/json

{
  "businessKey": "ORDER-001",
  "variables": { "paymentReceived": true }
}
```

#### Broadcast a signal

```http
POST /api/signals/{signalName}/broadcast
Content-Type: application/json

{ "cancelReason": "Out of stock" }
```

---

### Error Responses

All errors follow a consistent shape:

```json
{
  "timestamp": "2024-06-01T10:05:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Process not found: unknownProcess"
}
```

| HTTP Status | Trigger |
|---|---|
| `400` | Invalid / unparseable BPMN XML |
| `422` | Engine error (no start event, no handler, etc.) |
| `500` | Unexpected internal error |

---

---

## Process Definition & Versioning API

Every deployment creates a new immutable **version** of a process.
Each process key has exactly one **active** version — the version that `startProcessByKey` will use.
All other versions remain available for history, comparison, or explicit start.

Base URL: `http://localhost:8080/api/definitions`

---

### Deploy a process (rich endpoint)

```http
POST /api/definitions/deploy
Content-Type: application/json

{
  "resourceName": "order-process-v3.bpmn20.xml",
  "bpmnXml": "<?xml version=\"1.0\"?>...",
  "deployedBy": "alice",
  "deployMessage": "Added email notification step"
}
```

**Response `200`:**
```json
{
  "id": "def-uuid",
  "key": "orderProcess",
  "version": 3,
  "name": "Order Process",
  "active": true,
  "suspended": false,
  "resourceName": "order-process-v3.bpmn20.xml",
  "deployedBy": "alice",
  "deployMessage": "Added email notification step",
  "createdAt": "2024-06-01T10:00:00Z",
  "updatedAt": "2024-06-01T10:00:00Z"
}
```

> The legacy `POST /api/deployments` endpoint still works and returns `{ "processDefinitionId": "..." }`.

---

### List active definitions

```http
GET /api/definitions
```

Returns one entry per process key — the currently active version.

```http
GET /api/definitions?q=order
```

Filters by name or key (case-insensitive substring match).

---

### Get active version

```http
GET /api/definitions/{key}
```

Returns the active version **including** the full `bpmnXml` field.

---

### Get definition by ID

```http
GET /api/definitions/by-id/{definitionId}
```

---

### Version history

```http
GET /api/definitions/{key}/versions
```

**Response `200`:**
```json
{
  "key": "orderProcess",
  "name": "Order Process",
  "latestVersion": 3,
  "activeVersion": 3,
  "suspended": false,
  "totalVersions": 3,
  "totalRunningInstances": 2,
  "versions": [
    { "version": 3, "active": true, "deployedBy": "alice", ... },
    { "version": 2, "active": false, "deployedBy": "bob", ... },
    { "version": 1, "active": false, "deployedBy": null, ... }
  ]
}
```

---

### Get a specific version

```http
GET /api/definitions/{key}/versions/{version}
```

Includes the full `bpmnXml` field.

---

### Download raw BPMN XML

```http
GET /api/definitions/{key}/versions/{version}/bpmn
```

Returns `Content-Type: application/xml` with a `Content-Disposition: attachment` header.

---

### Set the active version

```http
POST /api/definitions/{key}/versions/{version}/activate
```

Makes version `{version}` the active version for key `{key}`.
All other versions for that key become inactive.

---

### Suspend & activate

| Endpoint | Effect |
|---|---|
| `POST /api/definitions/{key}/suspend` | Suspend **all** versions — prevents new instance creation |
| `POST /api/definitions/{key}/activate` | Re-activate **all** versions |
| `POST /api/definitions/{key}/versions/{version}/suspend` | Suspend a single version only |
| `POST /api/definitions/{key}/versions/{version}/resume` | Re-activate a single version only |

A `startProcessByKey` call on a suspended definition throws an `EngineException` (HTTP 422).

---

### Start a specific version

```http
POST /api/definitions/{key}/versions/{version}/start
Content-Type: application/json

{
  "businessKey": "ORDER-001",
  "variables": { "amount": 1500 }
}
```

Returns the same `ProcessInstanceResult` as `POST /api/processes/{key}/start`.

---

### Delete a version

```http
DELETE /api/definitions/{key}/versions/{version}
```

- Returns `204 No Content` on success.
- Fails with `422` if any running instances exist for this version.
- If the deleted version was the active one, the next newest version is automatically promoted to active.

---

### Delete all versions

```http
DELETE /api/definitions/{key}
```

- Fails with `422` if **any** version of this key has running instances.

---

### Version lifecycle summary

```
Deploy v1  →  v1 active
Deploy v2  →  v2 active, v1 inactive
setActive(v1)  →  v1 active, v2 inactive
Delete v2  →  v1 is the only version (still active)
suspendAll  →  v1 suspended (no new instances)
activateAll →  v1 active again
```

---

---

## Identity API

The engine ships with a built-in identity system: **users**, **roles**, and **groups** (with membership).  
Roles can be assigned directly to users or to groups — group members automatically inherit their group's roles.  
Task candidate groups are resolved to actual usernames at runtime via the identity store.

Base URL prefix: `/api/identity`

---

### Seed Data

The following data is pre-loaded by Flyway migration `V2__identity_schema.sql`:

#### Roles (9 built-in)

| Name | Description |
|---|---|
| `ADMIN` | Full administrative access |
| `PROCESS_MANAGER` | Deploy and manage process definitions |
| `OPERATOR` | Start processes and manage instances |
| `HR_MANAGER` | Approve HR-related tasks |
| `HR_STAFF` | Handle HR tasks |
| `IT_STAFF` | Handle IT setup and onboarding |
| `FINANCE` | Handle finance tasks |
| `AUDITOR` | Read-only access to history |
| `DEVELOPER` | Deploy processes and view engine internals |

#### Groups (7 built-in)

| Name | Type | Roles |
|---|---|---|
| `administrators` | SECURITY | ADMIN, PROCESS_MANAGER, OPERATOR |
| `hr_team` | ORGANISATIONAL | HR_MANAGER, HR_STAFF |
| `it_team` | ORGANISATIONAL | IT_STAFF, OPERATOR |
| `finance_team` | ORGANISATIONAL | FINANCE |
| `operations` | ORGANISATIONAL | OPERATOR |
| `audit_team` | SECURITY | AUDITOR |
| `developers` | ORGANISATIONAL | DEVELOPER, PROCESS_MANAGER |

#### Users (13 seeded)

| Username | Email | Groups |
|---|---|---|
| `alice` | alice@example.com | administrators |
| `bob` | bob@example.com | hr_team |
| `carol` | carol@example.com | hr_team |
| `dave` | dave@example.com | hr_team |
| `eve` | eve@example.com | it_team |
| `frank` | frank@example.com | it_team |
| `grace` | grace@example.com | finance_team |
| `henry` | henry@example.com | finance_team |
| `ivy` | ivy@example.com | operations |
| `jack` | jack@example.com | audit_team |
| `kate` | kate@example.com | developers, operations |
| `liam` | liam@example.com | developers |
| `mia` | mia@example.com | operations, hr_team |

> **Note:** Seed passwords are BCrypt hashes. Use `POST /api/identity/users/verify-password` to verify credentials or `PUT /api/identity/users/{id}` with `newPassword` to change them.

---

### Users API

#### Create a user

```http
POST /api/identity/users
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "displayName": "John Doe",
  "roleNames": ["OPERATOR"],
  "groupNames": ["hr_team"]
}
```

**Response `201 Created`:**
```json
{
  "id": "uuid",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "displayName": "John Doe",
  "enabled": true,
  "locked": false,
  "roles": ["OPERATOR"],
  "groups": ["hr_team"],
  "effectiveRoles": ["OPERATOR", "HR_MANAGER", "HR_STAFF"],
  "createdAt": "2024-06-01T10:00:00Z"
}
```

#### List users (paged)

```http
GET /api/identity/users?page=0&size=20&sort=username
```

#### Get user by ID

```http
GET /api/identity/users/{id}
```

#### Get user by username

```http
GET /api/identity/users/by-username/{username}
```

#### Update user

```http
PUT /api/identity/users/{id}
Content-Type: application/json

{
  "displayName": "John D.",
  "enabled": true,
  "newPassword": "NewPassword99!"
}
```

#### Delete user

```http
DELETE /api/identity/users/{id}
```

#### Assign / revoke role

```http
POST   /api/identity/users/{id}/roles/{roleName}
DELETE /api/identity/users/{id}/roles/{roleName}
```

#### Get effective roles (direct + inherited)

```http
GET /api/identity/users/{id}/roles
```

**Response:**
```json
["ADMIN", "PROCESS_MANAGER", "OPERATOR"]
```

#### Join / leave a group

```http
POST /api/identity/users/{id}/memberships
Content-Type: application/json
{ "groupName": "hr_team", "addedBy": "alice" }

DELETE /api/identity/users/{id}/memberships/{groupName}
```

#### List memberships for a user

```http
GET /api/identity/users/{id}/memberships
```

#### Find users by group

```http
GET /api/identity/users/by-group/{groupName}
```

#### Find users by effective role

```http
GET /api/identity/users/by-role/{roleName}
```

#### Verify password

```http
POST /api/identity/users/verify-password
Content-Type: application/json
{ "username": "alice", "password": "MyPassword" }
```

**Response:**
```json
{ "valid": true }
```

---

### Roles API

#### Create a role

```http
POST /api/identity/roles
Content-Type: application/json
{ "name": "BILLING_MANAGER", "description": "Manages billing tasks" }
```

#### List all roles

```http
GET /api/identity/roles
```

#### Get role by name

```http
GET /api/identity/roles/by-name/{name}
```

#### Update / delete role

```http
PUT    /api/identity/roles/{id}
DELETE /api/identity/roles/{id}
```

---

### Groups API

#### Create a group

```http
POST /api/identity/groups
Content-Type: application/json

{
  "name": "security_team",
  "description": "Security operations team",
  "type": "SECURITY",
  "roleNames": ["OPERATOR", "AUDITOR"]
}
```

#### List all groups (optionally filter by type)

```http
GET /api/identity/groups
GET /api/identity/groups?type=ORGANISATIONAL
```

#### Get group by name

```http
GET /api/identity/groups/by-name/{name}
```

#### Assign / remove role from group

```http
POST   /api/identity/groups/{id}/roles/{roleName}
DELETE /api/identity/groups/{id}/roles/{roleName}
```

#### List group members

```http
GET /api/identity/groups/{id}/members
```

**Response:**
```json
[
  {
    "id": "mem-uuid",
    "userId": "usr-uuid",
    "username": "bob",
    "displayName": "Bob Manager",
    "groupId": "grp-uuid",
    "groupName": "hr_team",
    "joinedAt": "2024-01-01T00:00:00Z",
    "addedBy": null
  }
]
```

---

### Identity & Task Resolution

When a `<userTask>` with `candidateGroups` is executed, the engine automatically resolves group names to actual usernames using the identity store:

```xml
<userTask id="reviewTask"
          activiti:candidateGroups="hr_team,finance_team"/>
```

The `task.candidate_users` column is populated with all enabled, non-locked members of both groups at the time the task is created. This means:
- Task queries by assignee/candidate work against real usernames
- Changes to group membership after task creation do not affect existing tasks

---


## Expression Language

The engine uses **JUEL** (Java Unified Expression Language) for:

- Sequence flow `<conditionExpression>` evaluation
- Service task `activiti:expression` attributes
- Delegate expressions (`activiti:delegateExpression`)

### Syntax

| Form | Example |
|---|---|
| Immediate evaluation | `${amount > 1000}` |
| Deferred evaluation | `#{myBean.approved}` |
| Without delimiters | `amount > 1000` (auto-wrapped) |

### Variable access

All process variables are automatically bound into the EL context:

```xml
<conditionExpression>${score >= 80 && category == 'premium'}</conditionExpression>
```

### Service task expression

```xml
<serviceTask id="calc"
             activiti:expression="${pricingService.calculate(execution, amount)}"
             activiti:resultVariable="finalPrice"/>
```

The result is stored in the `finalPrice` process variable.

---

## Timer Support

The engine uses **Quartz Scheduler** for timer events. Three timer formats are supported:

### ISO 8601 Date (fire once at a specific instant)

```xml
<timerEventDefinition>
  <timeDate>2024-12-31T23:59:59Z</timeDate>
</timerEventDefinition>
```

### ISO 8601 Duration (fire once after a delay)

```xml
<timerEventDefinition>
  <timeDuration>PT30M</timeDuration>   <!-- 30 minutes -->
</timerEventDefinition>
```

Common duration patterns:

| Expression | Meaning |
|---|---|
| `PT1H` | 1 hour |
| `P2D` | 2 days |
| `PT30M` | 30 minutes |
| `P1Y2M3DT4H5M6S` | 1 year, 2 months, 3 days, 4 hours, 5 minutes, 6 seconds |

### ISO 8601 Repeating Interval (repeating cycle)

```xml
<timerEventDefinition>
  <timeCycle>R3/PT1H</timeCycle>   <!-- 3 times, every hour -->
</timerEventDefinition>
```

| Pattern | Meaning |
|---|---|
| `R3/PT1H` | Repeat 3 times every hour |
| `R/PT5M` | Repeat indefinitely every 5 minutes |
| `R0/PT1D` | Repeat indefinitely every day |

### Cron expression

```xml
<timerEventDefinition>
  <timeCycle>0 0 9 * * ?</timeCycle>   <!-- Every day at 09:00 -->
</timerEventDefinition>
```

---

## Implementing a JavaDelegate

Create a Spring `@Component` (or plain class) that implements [`JavaDelegate`](src/main/java/com/bpmnengine/engine/delegate/JavaDelegate.java):

```java
package com.example;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class SendInvoiceDelegate implements JavaDelegate {

    @Override
    public void execute(ExecutionContext execution) {
        String customerId = (String) execution.getVariable("customerId");
        Double amount     = (Double) execution.getVariable("amount");

        // ... call your service ...

        execution.setVariable("invoiceId", "INV-12345");
    }
}
```

Reference it from your BPMN:

```xml
<serviceTask id="sendInvoice"
             name="Send Invoice"
             activiti:class="com.example.SendInvoiceDelegate"/>
```

### Variable access in ExecutionContext

| Method | Description |
|---|---|
| `getVariable(name)` | Read a variable (walks up the scope chain) |
| `setVariable(name, value)` | Write a variable (propagates to scope root) |
| `setVariableLocal(name, value)` | Write to this execution's local scope only |
| `getAllVariables()` | Returns all visible variables as a `Map<String, Object>` |

---

## Writing Custom Element Handlers

Implement [`FlowElementHandler`](src/main/java/com/bpmnengine/engine/handler/FlowElementHandler.java) and register it as a Spring `@Component`:

```java
@Component
public class MyCustomTaskHandler implements FlowElementHandler {

    @Override
    public boolean supports(FlowElement element) {
        // Match by element type or subclass
        return element instanceof ServiceTask st
               && "myCustomType".equals(st.getTopic());
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        // ... perform work ...

        // Continue on outgoing flow(s)
        List<SequenceFlow> flows = ctx.getProcess().getOutgoingFlows(element.getId());
        return flows.isEmpty()
               ? ExecutionOutcome.endProcess()
               : ExecutionOutcome.continueOn(flows.get(0));
    }
}
```

`ExecutionOutcome` factory methods:

| Method | Meaning |
|---|---|
| `ExecutionOutcome.waitState()` | Pause — engine saves state, resumes later |
| `ExecutionOutcome.endProcess()` | This execution is done |
| `ExecutionOutcome.continueOn(flow)` | Follow a single sequence flow |
| `ExecutionOutcome.continueOn(flows)` | Follow multiple flows (creates parallel branches) |

---

## Configuration Reference

`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:bpmndb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: validate        # Flyway manages schema — do not change
    show-sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  quartz:
    job-store-type: jdbc        # Use 'memory' for tests
    jdbc:
      initialize-schema: never  # Flyway creates Quartz tables

bpmn:
  engine:
    async-executor-enabled: true
    history-level: FULL         # NONE | ACTIVITY | AUDIT | FULL
    default-timezone: UTC
```

---

## Running with PostgreSQL

1. Start a PostgreSQL instance (Docker example):

```bash
docker run -d \
  --name bpmn-postgres \
  -e POSTGRES_DB=bpmndb \
  -e POSTGRES_USER=bpmn \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:16
```

2. Override the datasource in `application.yml` or via environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bpmndb
    driver-class-name: org.postgresql.Driver
    username: bpmn
    password: secret
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

3. Run the application — Flyway will automatically create all tables on first start.

---

## Testing

### Run all tests

```bash
mvn test
```

### Test coverage

**71 tests — 0 failures**

| Test class | Tests | What is covered |
|---|---|---|
| `BpmnXmlParserTest` | 8 | All element types, all gateway types, all event definitions, boundary events, multi-instance, sub-processes |
| `ExclusiveGatewayHandlerTest` | 2 | Condition evaluation, default flow fallback |
| `JuelExpressionEvaluatorTest` | 9 | Arithmetic, variable comparison, boolean, string, blank expressions |
| `VariableMapperTest` | 5 | All variable types (String, Boolean, Long, Double, Null) round-trip |
| `ProcessVersioningIntegrationTest` | 25 | Deploy versioning, list/search, active version, version history, set-active, delete version, delete all, suspend/activate (all & single version), start by key/version/id |
| `IdentityServiceIntegrationTest` | 22 | Create/update/delete users, role assignment, group membership, effective roles, candidate group resolution, password verify |

### Writing integration tests

```java
@SpringBootTest
@Transactional
class OrderProcessIntegrationTest {

    @Autowired ProcessEngine engine;

    @Test
    void completeOrderProcess() {
        // 1. Deploy
        String xml = new ClassPathResource("test-processes/order.bpmn20.xml")
                         .getContentAsString(StandardCharsets.UTF_8);
        engine.deployProcess("order.bpmn20.xml", xml);

        // 2. Start
        ProcessInstanceResult result = engine.startProcessByKey(
            "orderProcess",
            Map.of("amount", 500, "customerId", "cust-1")
        );
        assertThat(result.getState()).isEqualTo("RUNNING");

        // 3. Complete user task
        List<TaskEntity> tasks = taskRepo.findByProcessInstanceId(result.getId());
        engine.completeTask(tasks.get(0).getId(), Map.of("approved", true));

        // 4. Assert completed
        ProcessInstanceResult updated = engine.getProcessInstance(result.getId());
        assertThat(updated.getState()).isEqualTo("COMPLETED");
    }
}
```

---

## Extending the Engine

### Adding a DMN Engine (BusinessRuleTask)

Inject your DMN evaluator into `ServiceTaskHandler.executeBusinessRule()`:

```java
private void executeBusinessRule(ExecutionContext ctx, BusinessRuleTask brt) {
    String decisionRef = brt.getDecisionRef();
    Map<String, Object> result = dmnEngine.evaluate(decisionRef, ctx.getAllVariables());
    if (brt.getResultVariable() != null) {
        ctx.setVariable(brt.getResultVariable(), result);
    }
}
```

### Custom variable serialisation

Extend `VariableMapper` with your own type mappings by adding cases to `mapToEntity()` / `mapFromEntity()`.

### Custom history levels

Override `HistoryService` methods to record additional data or skip recording based on your `bpmn.engine.history-level` configuration property.

### External worker tasks (topic-based)

Mark a service task with `activiti:topic="my-worker"` and use `ServiceTask.getTopic()` to create an external worker poll-and-complete pattern:

```xml
<serviceTask id="processPayment"
             activiti:type="external"
             activiti:topic="payment-service"/>
```

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Persistence | Spring Data JPA + Hibernate 6 |
| DB (dev) | H2 (in-memory) |
| DB (prod) | PostgreSQL |
| Schema migration | Flyway (3 versioned scripts) |
| Expression language | JUEL (Java Unified EL) |
| Timer / scheduling | Quartz Scheduler |
| Password hashing | BCrypt (strength 10) |
| Build tool | Maven |
| Code generation | Lombok |
| Test framework | JUnit 5 + AssertJ + Spring Boot Test |

---

## License

This project is released under the **MIT License**.
