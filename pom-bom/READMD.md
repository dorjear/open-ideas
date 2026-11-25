# POM vs BOM demo

Four independent Maven projects:

| Repo | Artifact(s) | Role |
|---|---|---|
| `platform/` | `com.demo.platform:demo-bom`, `com.demo.platform:demo-parent` | Shared platform (versions + build config) |
| `customer-api/` | `com.demo:customer-api` | Customer CRUD API (port 8081) |
| `inventory-api/` | `com.demo:inventory-api` | Inventory item CRUD API (port 8082) |
| `system-test/` | `com.demo:system-test` | RestTemplate system tests against both running APIs |

```
spring-boot-dependencies (Spring's BOM)
        ▲ import
   demo-bom            ← versions only (<dependencyManagement>)
        ▲ import
   demo-parent         ← build config + common deps (<parent>)
        ▲ inherit              ▲ inherit
  customer-api            inventory-api
```

## Parent POM vs BOM

| | Parent POM (`demo-parent`) | BOM (`demo-bom`) |
|---|---|---|
| How it's used | `<parent>` (inheritance, only **one** allowed) | `<scope>import</scope>` in `<dependencyManagement>` (import **many**) |
| Shares | Properties, plugins/pluginManagement, `<dependencies>`, build settings | Only dependency **versions** |
| Adds jars to classpath? | Yes, anything in its `<dependencies>` | No, it only pins versions |
| Example here | Java 21, `-parameters`, Spring Boot repackage, web/validation/actuator/openapi/test for every API | Spring Boot 3.5.16 + springdoc 2.8.16 |

What to look at:
- [`demo-bom/pom.xml`](platform/demo-bom/pom.xml): imports Spring Boot's BOM and pins springdoc (which Spring Boot doesn't manage).
- [`demo-parent/pom.xml`](platform/demo-parent/pom.xml): imports `demo-bom`, adds the common dependencies, and sets up the plugins.
- [`customer-api/pom.xml`](customer-api/pom.xml): no versions anywhere. `spring-boot-starter-data-jpa` and `h2` get their versions from the BOM, and the web/test deps come from the parent.
- [`system-test/pom.xml`](system-test/pom.xml): imports `demo-bom` directly, with test-scoped dependencies and its own build plugins.

Upgrading Spring Boot for every service is one change: bump `demo-bom` (and the plugin version in `demo-parent`), release, then bump the parent version in each API.

> BOMs can only manage dependencies, not plugins. That's why `spring-boot.version` is also set in `demo-parent`, for the `spring-boot-maven-plugin`.

### Using the BOM without the parent
A service that already has a different parent can still get the same versions:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.demo.platform</groupId>
      <artifactId>demo-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## Build & run

```powershell
# 1. Publish the platform to ~/.m2 (in real life: deploy to Artifactory)
cd platform;       mvn install

# 2. Build/test each API independently
cd ../customer-api;  mvn verify
cd ../inventory-api; mvn verify

# 3. Run
cd ../customer-api;  mvn spring-boot:run   # http://localhost:8081/swagger-ui.html
cd ../inventory-api; mvn spring-boot:run   # http://localhost:8082/swagger-ui.html
```

See where each version comes from:

```powershell
cd customer-api
mvn help:effective-pom      # parent + BOM merged
mvn dependency:tree         # resolved versions
```

## Controller tests

Both APIs include three levels of controller tests:

| Test class suffix | Spring context | Repository | What it checks |
|---|---|---|---|
| `ControllerTest` | None (plain Mockito unit test) | Customer service / inventory repository mocked | Controller methods called directly |
| `ControllerMvcTest` | MVC slice via `@WebMvcTest` | Customer service / inventory repository mocked with `@MockitoBean` | HTTP routing, JSON, validation, status codes, and Location headers through MockMvc |
| `ComponentTest` / `Controller*IntegrationTest` | Full application via `@SpringBootTest` and `@AutoConfigureMockMvc` | Real Spring Data JPA repository | CRUD requests through MockMvc, persisted changes in H2, missing records, and validation |

`@MockitoBean` registers the mocked collaborator in Spring's application context.
The MVC tests use the real controller. The component/integration tests also use the
real repository, with a dedicated in-memory H2 database and `create-drop` schema
management. `CustomerApiComponentTest` uses the real service and Agify client,
with WireMock serving the downstream HTTP responses. `AgifyClientTest` also uses
WireMock to verify HTTP requests, responses, errors, and a real read timeout.
The separate negative integration class mocks the client for its failure cases.
Each component/integration test clears its database before
running and reads persisted data back through the repository. MockMvc dispatches
requests within the Spring context without opening an HTTP port.

The customer lifecycle is named `CustomerApiComponentTest` to describe its
in-process application boundary. Its negative cases are in
`CustomerControllerNegativeIntegrationTest`.

Run `mvn test` from either API directory to run all tests, or select a level:

```sh
mvn -Dtest='*ControllerMvcTest' test
mvn -Dtest='*ComponentTest,*Controller*IntegrationTest' test
```

## System tests over real HTTP

The independent [`system-test`](system-test/README.md) project uses JUnit 5 and
`RestTemplate` to call both running APIs over HTTP. It covers CRUD, validation,
missing records, and cleanup of its own test data. Start both APIs first, then run
from `pom-bom`:

```sh
mvn -f system-test/pom.xml test
```

Override the base URLs when the APIs run elsewhere:

```sh
mvn -f system-test/pom.xml test \
  -Dcustomer.base-url=http://localhost:9081 \
  -Dinventory.base-url=http://localhost:9082
```

## Test logging

All test suites log the test name, final outcome, and elapsed time using SLF4J.
Each test and lifecycle step starts with a banner such as
`=====createCustomer=====` before its request and response details.
Final outcomes are recorded after assertions and cleanup, so cleanup failures are
reported as failures. Parameterized cases include their invocation details.

MockMvc and real HTTP tests log the method, URL, request headers, request body,
response status, and response body, including expected 400/404 responses.
System-test responses are buffered so logging does not consume the body needed by
assertions. Authorization and cookie request headers are redacted.

Direct controller unit tests log the logical endpoint, inputs, and returned
status/body. Repository, validation, and context tests log their operations and
results. Fields that do not exist without HTTP are labelled `N/A`.

The logging/reporting helpers live under each project's
`src/test/java/com/demo/testsupport`.

## Test reports and log files

Running `mvn test` in `customer-api`, `inventory-api`, or `system-test` automatically
creates an offline HTML report and a separate log file for every test invocation.
No additional reporting command is needed.

| Output inside each project | Contents |
|---|---|
| `target/test-reports/index.html` | Latest run: status totals, test names, durations, and links to individual logs |
| `target/test-reports/runs/<run-id>/index.html` | Archived report for that run |
| `target/test-reports/runs/<run-id>/logs/*.log` | One log per test case, including steps, request/response details, final outcome, and failure stack traces |
| `target/surefire-reports/*-output.txt` | Complete console output per test class, including Spring startup and background-thread messages |
| `target/surefire-reports/TEST-*.xml` | Standard Maven/JUnit results for CI tools |

Refresh the HTML page while tests are running to see newly started and completed
cases. Reports are updated from the test callbacks, so a failed test still gets a
report entry and log even when Maven exits with a failure. Final results include
setup/cleanup failures, aborted tests, and skipped tests.

Each parameterized invocation has its own report entry and log file. A grouped
lifecycle is one test case, with all six step banners in that case's log.
Per-case files capture logs associated with the test invocation; class-level
console files retain startup/background output outside that scope. Maven's console
still shows the test summary, while detailed test output is saved to the files.

Run history stays under `target` until `mvn clean` removes it. To choose another
report directory, use `mvn test -Dtest.reports.directory=/path/to/reports`.

## Code coverage (JaCoCo)

After a successful `mvn test` in `customer-api` or `inventory-api`, the same
`target/test-reports/index.html` includes **line, method, and branch coverage**
percentages and covered/total counts. Open its detailed JaCoCo link to explore
packages, classes, methods, and highlighted source lines. The standard JaCoCo
HTML, XML, and CSV outputs are also available under `target/site/jacoco/`.
No `verify` or `site` command is required.

Coverage combines all tests executed in that API's test JVM: unit, MVC slice,
repository, HTTP-client, and component tests. Only application classes from
`target/classes` are reported; test code and dependency libraries do not count.
A metric with no applicable code (for example, no branches) is shown as `N/A`.
Running a selected test with `-Dtest=...` measures only that selected run.
Execution data is replaced on each run rather than accumulated across runs.

The report publisher copies JaCoCo's output into
`target/test-reports/runs/<run-id>/coverage/`, so archived test reports retain
that run's totals and source details. This also works with a custom
`-Dtest.reports.directory=...`. Coverage appears after the test process exits;
refresh the report after Maven finishes. `mvn clean` removes the default history.

If tests fail, Maven preserves its failure status and stops before the coverage
report goals. Test results and logs remain available. To publish coverage from
that failed run without rerunning the tests, run this in the same API directory:

```bash
mvn jacoco:report exec:java@publish-test-coverage
```

`system-test` calls separately running servers and has no application classes.
Its report therefore explains that server coverage is **not collected**. The API
build reports measure their own build-time tests. Measuring coverage specifically
from the system tests requires starting each API JVM with a JaCoCo agent and
collecting its execution data; attaching an agent only to the HTTP test runner
would not measure the servers.

## Customer age enrichment

The customer controller delegates to `CustomerService`, which owns persistence and
calls `AgifyClient` on every successful create/update using the request's first
name. The returned age is stored in the customer table. Get/list return that stored
age without calling Agify, and deletion does not call Agify.

`CustomerRequest` contains only `firstName`, `lastName`, and `email` for create/update.
`age` is returned in customer responses and is populated from Agify. A successful Agify response
with `age: null` is stored as unknown. If Agify fails, times out, or returns an
invalid response, create/update return HTTP 502 and leave the database unchanged.
Updates refresh age even when the first name stays the same. Existing rows without
an age remain null until updated.

Configure `AGIFY_API_KEY` with your Agify key before starting the customer API.
`AGIFY_BASE_URL` defaults to `https://api.agify.io` and can point to a local test
server. The equivalent Spring properties are `agify.api-key` and
`agify.base-url`. Connect/read timeouts are five/ten seconds. Authentication and
nullable-age behavior follow the [Agify API reference](https://agify.io/documentation/api/reference).

If customer system tests return HTTP 502, check the customer server's Agify
failure diagnostic. Corporate HTTPS inspection can require a CA certificate
trusted by the JVM even when a browser or `curl` works. See
[system-test troubleshooting](system-test/README.md#troubleshooting-customer-http-502)
for details of the automatic `macos-keychain` Maven profile. On macOS,
`mvn spring-boot:run` in `customer-api` uses the keychain without extra JVM arguments. Invalid-customer validation
warnings correspond to expected HTTP 400 cases, rather than downstream failures.

Customer response example (the age below is illustrative):

```json
{"id":1,"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","age":42}
```

Build-time tests do not call the live Agify service. `AgifyClientTest` and
`CustomerApiComponentTest` use WireMock on dynamically assigned local ports.
The component lifecycle verifies persisted ages, the three-field request contract, exactly one
lookup on create/update, and no additional lookups on reads/deletes.
`CustomerServiceTest` uses Mockito to verify service logic and failure behavior.
WireMock's test-only dependency version is managed in `demo-bom`; run
`mvn -f platform/pom.xml install` after changing the shared BOM. The standalone
WireMock artifact keeps its bundled dependencies separate from Spring Boot's.

## API

| Method | Customer API | Inventory API |
|---|---|---|
| GET | `/api/customers` | `/api/items` |
| GET | `/api/customers/{id}` | `/api/items/{id}` |
| POST | `/api/customers` | `/api/items` |
| PUT | `/api/customers/{id}` | `/api/items/{id}` |
| DELETE | `/api/customers/{id}` | `/api/items/{id}` |

```powershell
curl -X POST localhost:8081/api/customers -H "Content-Type: application/json" `
  -d '{"firstName":"Jane","lastName":"Doe","email":"jane@example.com"}'
curl -X POST localhost:8082/api/items -H "Content-Type: application/json" `
  -d '{"sku":"SKU-001","name":"Widget","quantity":10,"price":9.99}'
```
