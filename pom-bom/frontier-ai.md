# Engineering Lead — Frontier AI Program
## Tony's candidate pre-interview response

## My current focus

In my current role, I am establishing a **human-in-the-loop harness engineering practice**. I use AI agents to perform repeatable engineering tasks, but engineers retain responsibility for requirements, architecture, validation and release decisions. This control is essential because even leading LLMs can generate plausible but incorrect code, tests and conclusions.

I am also leading changes to coding standards and conventions, moving from traditional human-oriented principles toward **AI-first engineering principles and patterns**. These include KISS, an AI-first interpretation of SOLID, and coding styles that are explicit, consistent and easy for both engineers and coding agents to understand and modify safely.

The same approach can accelerate dependency management. I am developing reusable AI skills and specialised sub-agents to:

- Upgrade Java and Spring Framework versions.
- Fix dependency vulnerabilities.
- Migrate existing POMs to new parent POMs based on a shared BOM.
- Write system and contract tests to improve regression safety—not just unit-test coverage.

## Scenario 1: Dependency management

### Discover and classify

I would create an automated inventory covering Java and Spring Boot versions, parent POMs, direct and transitive dependencies, private artifacts, vulnerabilities, test maturity, release processes and ownership. I would then classify APIs into cohorts based on technical version, business risk and migration readiness.

### Introduce the BOM

I would avoid a big-bang rollout. I would start with a small number of representative pilot APIs, establish a tested parent POM and BOM, and use the results to improve the migration process before expanding it.

The BOM would define approved Spring Boot, third-party and internal-library versions. Teams could initially retain documented exceptions, but each exception would require an owner, reason and review date.

### Manage internal dependencies

Internal libraries would have named owners, supported Java and Spring ranges, release notes, compatibility tests and deprecation dates. Production builds would use immutable private Maven artifacts rather than snapshots. Significant library releases would be validated against representative APIs before wider adoption.

### Lead team adoption

Because each API team currently manages dependencies independently, this is as much an adoption challenge as a technical one. I would communicate the benefits, migration sequence and team responsibilities clearly through engineering forums, demonstrations, documentation and direct engagement with API teams.

I am doing this type of communication and internal marketing in my current role. The objective is to demonstrate that the standard reduces repetitive work and risk, rather than presenting it as a centrally imposed control.

### Reusable assets

I would provide:

- A version-inventory and classification tool.
- The shared BOM and parent POM.
- A POM-migration agent.
- Compatibility matrices and migration playbooks.
- CI templates and policy checks.
- An exception and deprecation register.

## Scenario 2: Safe updates

### Upgrade order

I would use the following sequence for every pilot API:

1. Generate meaningful system and contract tests.
2. Run them to establish a behavioural baseline.
3. Upgrade Java and Spring by migrating to the new parent POM and BOM.
4. Run the vulnerability-fix agent.
5. Re-run the system and contract tests to verify that behaviour remains correct.

This sequence is important because some vulnerabilities can only be resolved after moving to a newer Java or Spring version. Across the estate, I would prioritise urgent vulnerabilities and critical or externally exposed APIs, while selecting early pilots with engaged owners and enough testability to validate the process.

### Required validation

Each proposed change would run:

- Maven dependency resolution and compilation.
- Unit, system and contract tests.
- Vulnerability and dependency-policy scans.
- Checks for unexpected transitive changes.
- Integration and deployment smoke tests where appropriate.
- Human review of AI-generated changes and test evidence.

### Automatic merging

I would allow automatic merging only for low-risk patch updates when the API team has opted in, all tests pass, the version remains within an approved compatibility range, and no behaviour or contract has changed. Java, Spring, major-version or behaviour-changing upgrades would require human approval.

### Failures and exceptions

Failures would be classified as regressions, incompatibilities, flaky tests, environment problems, private-repository access failures or incorrect agent output. AI-generated diagnoses would be treated as recommendations, not accepted automatically.

Exceptions would require a documented reason, risk owner, compensating control and expiry date. Repeated failures would be used to improve the shared agent, pipeline or migration guidance.

### Reusable assets

I would create reusable agent skills, prompts, pipeline templates, private-repository configuration, PR templates, validation scripts and failure-triage playbooks. These assets would support teams while preserving their ownership of application behaviour and releases.

## Scenario 3: AI-first testing

### Discover business behaviour

I would begin with the production incident and reconstruct the inputs, external interactions, state changes and expected outcome. I would combine approved logs, code, API specifications and discussions with business and engineering owners to identify the highest-risk behavioural gaps.

### Tests to create first

The first system test would reproduce the production issue and fail against the original implementation. I would then add tests for adjacent risks such as validation boundaries, error handling, retries, timeouts, idempotency and external-service failures. Contract tests would protect assumptions between independently released services.

### AI-agent responsibilities

A testing agent could:

- Produce a behaviour and integration map.
- Identify missing scenarios and boundary cases.
- Generate JUnit system and contract tests.
- Create synthetic, non-sensitive test data.
- Run tests and explain failures.
- Link each generated test to a requirement, risk or incident.

### Tests and environments

I am driving the decommissioning of Cucumber and Karate in favour of simpler JUnit-based system tests. Coding agents reduce the cost of writing test code, so I prioritise direct, readable tests over abstraction layers that can make behaviour harder to trace and debug.

Mocks would be limited to genuine external boundaries. Contract tests would validate service assumptions, while selected integration tests would run in controlled environments to detect issues that mocks cannot reveal.

### Useful-test criteria

A generated test is useful only if it verifies a meaningful business outcome, detects a plausible defect, exercises a relevant boundary and remains deterministic and readable. Increased coverage alone would not demonstrate improved regression safety.

### Quality gates

Every AI-generated test or implementation change would require automated execution and human review. Reviewers would confirm the correctness of the scenarios, assertions, data and mocks. This human-in-the-loop harness ensures that AI improves delivery speed without becoming the final authority on correctness.
