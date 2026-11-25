# System tests

This independent Maven project contains only JUnit 5 test code. It uses
`RestTemplate` to make real HTTP calls to separately running customer and inventory
APIs. It does not start either API or use MockMvc, mocked HTTP responses, service
classes, or repositories. Dependencies import the shared `demo-bom`; all
dependencies have test scope.

## Run

Use JDK 21 or newer. From `pom-bom`, install the shared platform first:

```sh
mvn -f platform/pom.xml install
```

Start each API in its own terminal, also from `pom-bom`:

Configure `AGIFY_API_KEY` for the customer API. Customer create/update call Agify;
`AGIFY_BASE_URL` can instead point the running customer API to a deterministic
local Agify test server. The system tests continue to call the API over real HTTP.

```sh
mvn -f customer-api/pom.xml spring-boot:run
```

```sh
mvn -f inventory-api/pom.xml spring-boot:run
```

Once both APIs are ready, run the system tests:

```sh
mvn -f system-test/pom.xml test
```

The default base URLs are `http://localhost:8081` for customer and
`http://localhost:8082` for inventory. Override them to test other running instances:

```sh
mvn -f system-test/pom.xml test \
  -Dcustomer.base-url=http://localhost:9081 \
  -Dinventory.base-url=http://localhost:9082
```

Run a positive workflow with `-Dtest=CustomerApiSystemTest` or
`-Dtest=InventoryApiSystemTest`. Negative cases are in
`CustomerApiNegativeSystemTest` and `InventoryApiNegativeSystemTest`; select either
class with `-Dtest` or run both with `-Dtest='*NegativeSystemTest'`.
Unavailable APIs cause test failures, not skips.
Connection and read timeouts are five and ten seconds respectively.

The positive customer lifecycle waits **one second before update** to space out
its two Agify lookups. Set the delay in `TestConfig.CUSTOMER_UPDATE_DELAY`, currently
`Duration.ofSeconds(1)`. The wait is logged inside the `updateCustomer` step; the
grouped test retains its six helper calls. This setting lives in test code and
requires no Maven parameter.

This delay can help with short-term throttling, but an exhausted quota or per-IP
request allowance still requires waiting for the limit to reset or configuring an
account with available quota.
Agify documents both causes of HTTP 429 in its
[API reference](https://agify.io/documentation/api/reference#responses-and-errors).

## Troubleshooting customer HTTP 502

A customer create/update returns 502 when its Agify lookup fails. The customer
server now logs the upstream status (if available), exception type, and root-cause
type. A TLS failure also logs a hint about the JVM certificate trust store.
API keys and exception URLs are not included in these diagnostic messages.

A validation warning for blank names or an invalid email belongs to a negative
test that expects HTTP 400. It does not explain a 502 from a valid create request.
The invalid-update test also creates a valid customer first, so that setup step
still requires working Agify access.

On this Mac, the Java 25 client failed with `PKIX path building failed` because
the HTTPS connection presented a certificate issued by Cisco Umbrella that the
JDK's default trust store did not trust. The same client succeeded using the
macOS `KeychainStore`, which already contains the required trusted certificate.
The customer API POM now activates its `macos-keychain` Maven profile automatically
on macOS. Stop and restart the customer API normally from `pom-bom`:

```sh
mvn -f customer-api/pom.xml spring-boot:run
```

Or run `mvn spring-boot:run` inside `customer-api`. No extra JVM arguments are needed.
The profile passes `javax.net.ssl.trustStore=NONE` and
`javax.net.ssl.trustStoreType=KeychainStore` to Spring Boot's forked application
JVM, where the Agify call runs. It applies to Maven startup on macOS; a packaged
JAR started with `java -jar` requires its own JVM configuration.
Certificate-chain and hostname validation remain enabled. `NONE` selects a
non-file-backed trust store; it does not disable trust checks. The macOS provider
is documented in the [JDK provider reference](https://docs.oracle.com/en/java/javase/25/security/oracle-providers.html).
For other environments, configure a dedicated Java trust store containing the
organization-approved CA certificate. If a keychain does not already trust the
issuer, obtain the appropriate CA through your organization's normal process.

An upstream 401 indicates an invalid API key; 429 indicates a request limit or
throttling. Configure `AGIFY_API_KEY` in the terminal or run configuration that
starts the customer server when required. Merely creating a token file does not
configure Spring. See the [Agify API reference](https://agify.io/documentation/api/reference).

For the full suite, inventory must also be running on port 8082. If running only
customer, select the customer cases:

```sh
mvn -f system-test/pom.xml test -Dtest='CustomerApi*SystemTest'
```

## Coverage

Both APIs are checked for create, fetch, list, update, delete, `Location` headers,
missing-record responses, invalid creation, and invalid updates that leave existing
data unchanged. Each API has one positive lifecycle test that executes
**create → list → enquiry → update → enquiry → delete** against the same record.
The second enquiry verifies the persisted update through the public API, and the
test checks that the deleted record can no longer be fetched or listed.
Missing-record and validation scenarios run independently in separate negative
test classes. Test data uses unique email addresses and SKUs, and cleanup deletes
only records created by the current test, including after assertion failures.

Customer create/update requests contain only `firstName`, `lastName`, and `email`.
Age is generated by Agify, appears in responses, and may be null when no estimate is available.
The customer workflow checks the generated field and includes its returned value
in subsequent whole-JSON comparisons, verifying that list/enquiry return the saved
age. It does not call Agify directly or hardcode live predictions. Build-time
customer component/client/service tests verify the precise lookup values and
that get/list do not invoke the downstream client. The client and component tests
use WireMock for Agify's HTTP endpoint.

## Reports and logs

After `mvn test`, open `target/test-reports/index.html` for the latest run's
statuses, durations, and individual test-log links. Refresh during a run to see
progress. Each invocation, including parameterized and negative cases, gets its
own log; the grouped lifecycle's steps share that lifecycle test's log file.

Archived reports and logs are under `target/test-reports/runs/<run-id>/` until
`mvn clean`. Reports include failures and cleanup errors even when Maven fails.
Surefire also saves per-class console output as
`target/surefire-reports/*-output.txt` and standard JUnit XML alongside it.


The report also explains the coverage boundary: this project has no application
code, and HTTP calls execute customer/inventory code in separate server JVMs.
JaCoCo line, method, and branch coverage from the API build-time tests is available
in each API's `target/test-reports/index.html` after `mvn test` there. To measure
server coverage exercised by this system suite, start the API servers with JaCoCo
agents and collect their execution data. The system-test runner alone cannot
measure that remote code.
