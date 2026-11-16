# Performance tests

The NFT workflow runs basic landlord journeys, saved-registration resume/deletion, completed-property
and organisation updates, gas/electrical certificates, organisation registration/team operations,
joint invitations/acceptance, agent access/property updates, operator operations and property
deregistration. Ten families run concurrently; preparation and sequential checks also exercise the
restored journeys. Landlord registration is a proxy, not paid property-registration capacity.

## Merge and release order

The changes are split for focused review: seeder/data generation (#2259), Gatling journeys/assertions
(#2260), fixtures/workflow/recovery (#2261), and infrastructure (communitiesuk/prsdb-infra#395).
Merge #2259, then #2260, then #2261; update/retarget each child onto main and rerun checks after its
parent merges. Infra can merge independently, but the approved NFT task-definition-state plan,
apply/configuration of the managed worker revision and webapp release are required before execution.
Merging alone does not deploy or establish load acceptance.

## Prerequisites

Use the seeder and supported Gatling changes together with this orchestration. The NFT environment needs
the seeded database dump, its pinned S3 version and checksum, an exact active VPC restore task revision,
the restore worker's S3/KMS access and the workflow runner's ECS, SSM and restore permissions.
The runner also needs `ecs:DescribeServices` and `ecs:UpdateService` on the NFT async-worker
service, plus the scoped task-list and task-describe permissions used to verify its shutdown.
Configure `NFT_RESTORE_TASK_DEFINITION` as an NFT environment variable or pass the exact revision to the
workflow. The restore worker task definition must contain the pinned dump inputs and verification settings.
Certificate metadata in a database dump does not contain file bodies: the matching synthetic objects
must be available in the app's storage for property and certificate checks.

For local development use Docker, Node 22+, the JDK required by the build and the normal `.env`.
Start the local dependencies and the official One Login simulator:

```bash
docker compose -f docker-compose.local.yml up -d --wait
docker run --rm --name prsdb-performance-simulator \
  -p 127.0.0.1:13000:3000 -e INTERACTIVE_MODE=true \
  ghcr.io/govuk-one-login/simulator@sha256:0d5e62c1db1c400c4881be2270b3f08aeb55c72ca3d9eb9a6e5196becef6f5e5
```

In another terminal start the local app and simulator bootstrap:

```bash
node --env-file=.env scripts/performance-tests/start-local.mjs
```

The bootstrap uses `SERVER_PORT` and `ONE_LOGIN_SIMULATOR_PORT` if set, and creates disposable signing
keys. It does not reset the database. Only on a disposable local database, stop the app and use
`FLYWAY_CLEAN_DISABLED=false ./gradlew flywayClean --console=plain` before starting it if a fresh schema
is needed. The bootstrap loads the local performance address fixture.

Run the basic journeys with a fresh registration subject prefix for each run:

```bash
./gradlew gatlingRun --non-interactive \
  --simulation uk.gov.communities.prsdb.webapp.performance.BasicJourneysSimulation \
  -Dgatling.target=local -Dgatling.basic.mode=baseline \
  -Dgatling.baseUrl=http://localhost:8080 \
  -Dgatling.simulatorUrl=http://127.0.0.1:13000 \
  -Dgatling.basic.landlordSubject=urn:fdc:gov.uk:2022:UVWXY \
  -Dgatling.basic.registrationSubject=urn:fdc:gov.uk:2026:PERF-REGISTRATION-001
```

For locally restored seeded property fixtures, export the matching SQL through a read-only database
connection, then prepare synthetic file bodies in an existing isolated storage root outside the
repository and its ancestors:

```bash
node scripts/performance-tests/local-seed-files.mjs property-fixtures.json /absolute/path/isolated-storage-root
node scripts/performance-tests/local-registration-certificate.mjs synthetic-certificate.pdf
```

Use the isolated directory as the app working directory, with an absolute jar path and a `src` link
to the checkout. The fixture helper refuses unsafe paths, symlinks, conflicting objects and non-seed
keys. The generated certificate is synthetic and not valid for any real compliance purpose.

## NFT workflow and recovery

Dispatch **Run Performance Tests** from `nft`. It checks for changes, compiles Gatling, stops actual
writers, restores the pinned dump in the VPC, migrates and verifies it, starts the app and prepares
the simulator. It exports bounded, distinct saved-registration owners, sole-property owners and
organisation administrators through a read-only tunnel. It prepares invitations through the application
and re-exports matching tokens before their consumers run. Basic/property/profile/certificate checks
precede the ramp; additional restored sequential checks follow it. The ramp uses exclusive actors,
properties and organisations, two-second pauses, a five-minute ramp and a fifteen-minute hold.
It requires zero request failures and exact journey completions. Insufficient or overlapping fixture
pools fail closed; randomized seed eligibility is not assumed to guarantee capacity.

The workflow records non-secret ownership and recovery metadata for the exact run, workers, services
and simulator state. It records the async service's original task definition and desired count before
stopping it, verifies its recorded tasks have stopped, and keeps it stopped through the QA reset.
Cleanup independently attempts worker and readiness recovery, stops actual
writers before the QA reset and fails closed if required recovery state is missing. It then resets QA,
restarts the app and verifies the required QA properties and links. A separate recovery job restores
the recorded async desired count after a successful reset even when app recovery or QA verification
fails. If reset fails or ownership state is unavailable, the async worker remains stopped for manual
reconciliation to avoid writing to a potentially incomplete database. Preserve recovery artifacts and
investigate a failed or missing-state cleanup before retrying; do not fabricate state. The manual
**Reset Database - NFT** workflow can instead verify the existing QA baseline with `verify-only`.
No unattended restoration of modified environment settings is claimed.

Large datasets can require additional app startup time; ensure the environment's startup readiness
configuration is appropriate before an approved run. Run
`./gradlew performanceTest --console=plain` for the Gatling contracts.

## Limitations and evidence

The concurrent mix retains the landlord-registration proxy at 26/minute and saved, completed-property
and organisation-profile updates at 4/minute each. Six additional families run at provisional 1/minute:
gas/electrical compliance, organisation registration/team, joint acceptance, agent access/update,
operator and deregistration. Organisation registration/team share one arrival. This is 44 arrivals/minute
at hold and 767 starts overall; the added rates are assumptions, not measured production traffic.
The 26/minute planning forecast concerns paid property registrations, which the proxy does not prove.

Previously successful **local real-application Gatling runs** recorded:

| Journey | Successful requests |
|---|---:|
| Organisation registration / team invitation / acceptance / access change | 572 / 187 / 253 / 99 |
| Joint invitation operations / acceptance | 264 / 156 |
| Agent preparation / access and updates | 360 / 264 |
| Operator operations | 300 |
| Property deregistration | 156 |

These are historical sequential results, not expanded concurrent or NFT acceptance. Fresh fixture/HTTP
contracts validate restored code; a subsequent approved NFT run must validate the combined workload.
Joint rejection and existing-password agent variants are not claimed: preserved simulations support
acceptance and unclaimed invitations. Council execution, paid property registration, the failing public
EPC readiness path and EPC-dependent scenarios remain excluded. Gas/electrical checks remain included.

NFT run 43063425738 passed its then-supported correctness checks and QA recovery, but two of 31,257
ramp requests failed at simulator OAuth authorization because WAF `SQLi_QUERYARGUMENTS` matched nonce/state.
It did not run the restored extended families. Earlier expanded NFT attempts stopped in preparation;
skipped journeys are not demonstrated failures. Public EPC readiness failure does not establish an
internal loopback EPC failure; the payment roundtrip was skipped rather than proven failing.
No agreed latency SLO, successful combined NFT acceptance or paid-registration capacity is claimed.
