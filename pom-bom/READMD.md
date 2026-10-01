# POM vs BOM demo

Three independent repos:

| Repo | Artifact(s) | Role |
|---|---|---|
| `platform/` | `com.demo.platform:demo-bom`, `com.demo.platform:demo-parent` | Shared platform (versions + build config) |
| `customer-api/` | `com.demo:customer-api` | Customer CRUD API (port 8081) |
| `inventory-api/` | `com.demo:inventory-api` | Inventory item CRUD API (port 8082) |

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
