# Private Rental Sector Database - Web App

This is the web app code for the Private Rental Sector Database (PRSDB).

## Development

The prsdb-webapp is a Spring Boot application written in Kotlin. The following should allow you to get started
developing functionality for prsdb-webapp.

### Dependencies

For the easiest local development experience, use Intellij, and have a docker daemon running on your machine. The repo
includes a `local` launch configuration in the `.run` folder which will use docker compose to setup the required local
dependencies before starting the application.

A running docker daemon is also required to run the integration tests, which make use
of [testcontainers](https://testcontainers.com/).

### Running multiple worktrees in parallel

The local build can be run from multiple git worktrees simultaneously. Each worktree
needs unique ports to avoid conflicts. Three environment variables in `.env` control this:

| Variable        | Default | Purpose                   |
|-----------------|---------|---------------------------|
| `SERVER_PORT`   | `8080`  | Spring Boot server port   |
| `POSTGRES_PORT` | `5433`  | Host-side PostgreSQL port |
| `REDIS_PORT`    | `6379`  | Host-side Redis port      |

When creating a worktree with the `scripts/git-worktrees/new-worktree` script, unique
ports are assigned automatically. To adjust ports manually, edit `.env` in the worktree
root.

#### Playwright CLI for browser testing

Agents can use the [Playwright CLI](https://github.com/microsoft/playwright-cli) to
interact with the application in a browser. Each worktree can use a separate session
(`-s=<name>`) to allow parallel browser testing. To install:

```shell
npm install -g @playwright/cli@latest
playwright-cli install --skills
```

The application requires Java 21 - Gradle should automatically install this for you the first time you run the
application locally.

We are using Ktlint for linting, via the [ktlint-gradle plugin](https://github.com/jlleitschuh/ktlint-gradle) and the
[ktlint Intellij plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) which can be installed from within Intellij.

To ensure that your code meets the linting and formatting rules install these pre-commit hooks by running the
`addKtlintCheckGitPreCommitHook` and `addKtlintFormatGitPreCommitHook` tasks from Gradle tab in Intellij.

To prevent you from accidentally committing secrets we are also using a precommit hook called `detect-secrets`. To install the pre-commit
hook first ensure you have Python 3 and pip installed, then run the appropriate script from the `scripts` folder:

**PowerShell (Windows):**

```powershell
.\scripts\install-detect-secrets.ps1
```

**Bash (Linux/macOS):**

```bash
./scripts/install-detect-secrets.sh
```

There are also some local secrets that will need to be set up if you need to test integrations with other services when
running the project locally. Ask the team lead where these can be found.

When running your build against the integration environment of Gov.UK One Login you will be prompted for credentials to
access the integration environment, ask your team lead where these can be found.

#### Troubleshooting `detect-secrets` installation issues

If you run into issues such as the one shown below, you can try the following:

- check if the python scripts folder is in your PATH variable - if not add it and restart
- if you installed Python a different way, try installing it directly from [python.org](https://www.python.org/downloads/)
- try running the `install-detect-secrets` script as an administrator

![detect-secrets-error.png](readMeAssets/detect-secrets-error.png)

### Troubleshooting gradle issues on apple silicon macs

If you run into an issue with an apple silicon mac where gradle fails to build the app as it can't find npm (typically when using nvm),
you have to start a gradle daemon on the command line first using

```bash
./gradlew --stop
./gradlew assemble
```

This then gives you a window of time you can run the app via the intelliJ gradle runner. If it breaks again, you may need to repeat this.
This is due to the gradle runner in intelliJ not finding variables on the PATH specifically on apple silicon macs.

### Testing

The project uses a combination of unit tests and integration tests. The integration tests use a testcontainer to run a
postgres database. This takes extra time to spin up, and spins up a clean container per test, and so should only be used
for integration tests that need to interact with a real database - for other tests the relevant repository beans should
be mocked instead.

You can run the unit tests by running the `verification\test` task from the Gradle tab in Intellij.

#### Running scheduled tasks locally

You can run scheduled tasks locally using a new Run Configuration with the correct profiles added.
You will need to include the following profiles:

- `web-server-deactivated`
- `scheduled-task`
- `local`
- The relevant task specific profile e.g. `incomplete-property-reminder-scheduled-task` to run
  `IncompletePropertiesReminderTaskApplicationRunner`

If you need to use notify, also add the `use-notify` profile.

#### Seeding NFT (load test) data locally

`NftDataSeeder` generates a realistic set of landlords, organisations, properties and property registrations for
performance/load testing. It runs as a one-off task (not a web server) via `NftDataSeedingTaskApplicationRunner`,
which calls `exitProcess()` once seeding completes — this is expected behaviour, not a crash.

**Run configuration:** use (or copy) the `local-nft-seeder` Run Configuration in IntelliJ
(`.run/local-nft-seeder.run.xml`). It activates the profiles `local`, `web-server-deactivated`,
`nft-data-seeder`, and automatically runs `local_resources_up` (starts local Postgres/Redis via Docker) and
`flywayClean-local-except-address` (resets the schema, preserving the address/local council reference data) before
each run.

To run from the CLI instead:

```bash
./gradlew flywayClean flywayMigrate   # reset the schema first
SPRING_PROFILES_ACTIVE=local,web-server-deactivated,nft-data-seeder ./gradlew bootRun
```

**Configuration** (`nft-seed.*` in `application.yml`, overridable via env vars, with smaller defaults for `local` in
`application-local.yml`):

| Property                    | Env var                        | `local` default | Purpose                                                                                                   |
|------------------------------|---------------------------------|------------------|-------------------------------------------------------------------------------------------------------------|
| `nft-seed.system-operators`   | `NFT_SEED_SYSTEM_OPERATORS`     | 5                | Number of system operator users to create.                                                                  |
| `nft-seed.local-council-users`| `NFT_SEED_LOCAL_COUNCIL_USERS`  | 10               | Number of local council users to create.                                                                    |
| `nft-seed.landlords`          | `NFT_SEED_LANDLORDS`            | 30               | Total number of landlords to create (individual + organisation, ~95%/5% split).                             |
| `nft-seed.properties`         | `NFT_SEED_PROPERTIES`           | 45               | Total number of properties/property registrations to create.                                               |
| `nft-seed.batch-size`         | `NFT_SEED_BATCH_SIZE`           | 10               | Batch size for bulk inserts.                                                                                 |
| `nft-seed.random-seed`        | `NFT_SEED_RANDOM_SEED`          | 239              | Seed for the random generator, so a given configuration produces deterministic output.                      |
| `nft-seed.reference-date`     | `NFT_SEED_REFERENCE_DATE`       | (blank = now)    | Set to an ISO date (e.g. `2026-01-01`) to make generated dates (registration dates etc.) fully reproducible. |
| `nft-seed.generated-addresses`| `NFT_SEED_GENERATED_ADDRESSES`  | 500              | Number of fictional addresses to generate before seeding. `0` reuses the real NGD address data already in the database (this is what a real NFT/deployed run does — only set a non-zero value for local testing without full NGD address data loaded). |

To test at a larger, more realistic scale locally (e.g. to catch batching/performance issues that don't show up at
the small `local` defaults), override the scale-related env vars before running, e.g.:

```bash
export NFT_SEED_LANDLORDS=500
export NFT_SEED_PROPERTIES=800
export NFT_SEED_GENERATED_ADDRESSES=2000
```

A regression test suite covering these checks (`NftDataSeederTests`, under `src/test/.../services/`) runs the
seeder directly against a testcontainer database at a small scale, so it doesn't need Docker profile activation and
doesn't hit the `exitProcess()` behaviour described above.

### Scripts

Utility scripts are in the `scripts/` directory.

| Script                                         | Purpose                                                                                                                                                                                                                                                                            |
|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `generate_passcodes.js`                        | Bulk-generate landlord passcodes. Paste into the browser console on `/system-operator/generate-passcode` while logged in as a system operator. Prompts for a count, generates passcodes sequentially, and downloads the results as a CSV. Requires the `require-passcode` profile. |
| `generate_update_local_councils_migrations.js` | Generate SQL migrations for updating local council data from CSV.                                                                                                                                                                                                                  |
| `install-detect-secrets.ps1` / `.sh`           | Install the detect-secrets pre-commit hook.                                                                                                                                                                                                                                        |

### Code structure

#### Backend

Controllers can be found in the `controllers` package, entities and repositories can be found in the `database`
package, configuration classes in the `config` package and so on.

App configuration can be found in the `application.yml` and `application-local.yml` files for deployed and local
configuration respectively. For deployed environments configuration that differs between environments should be managed
through referencing environment variables in `application.yml`.

Database migrations can be found in `src/main/resources/db.migrations`. See section below on database migrations for
more information on these.

When developing locally, third party APIs should be stubbed by pointing the requests back at http://localhost:8080
through your local configuration in `application-local.yml`, and adding an equivalent endpoint to the one you're calling
to the `local.api` package. Those controllers should be annotated by `@Profile("local")` to ensure that they are not
included in any deployed builds.

#### Frontend

The project uses the Thymeleaf templating engine, combined with
the [Gov.UK design system](https://design-system.service.gov.uk/). The top-level templates can be found in
`src/main/resources/templates`, while fragments are stored in the `fragments` subfolder. When a new reusable component
is required, use relevant html from the design system to create a fragment.

Static assets should be added to the `src/main/resources/assets` folder. These will be copied into
the `src/main/resources/static/assets` folder at build time. Assets should not be added to the `static/assets` folder
directly as this is excluded from source control.

Custom css can now be added using [sass](https://sass-lang.com/) which is compiled to css by rollup when the project is
run.
New styles can be added to new or existing files in `src/main/resources/css` - if you make a new file, make sure it is
added
to `custom.scss` (this is what will get compiled). This lets directly use the govuk colours / spacing mixins.
So far we just included minimal govuk scss as this is all we need - see
[here](https://frontend.design-system.service.gov.uk/import-css/#import-specific-parts-using-sass) for adding more if
required.

### Database migrations

The project uses Flyway to manage migrations. To add a migration, create a new SQL file
in `src/main/resources/db.migrations` with a name of the form `V<version number>__<migration name>.sql`. The version
number is in semvar format with underscores separating the major/minor/fix elements. This determines the order in which
Flyway runs the migrations.

Database migrations <will be/ are> run at deployment time for all non-local deployments. This is done using
the `flywayMigrate` Gradle task. When developing locally using the `local` profile the migrations will run at
application start up. If you are using the `local` launch profile in IntelliJ, this will also run the `flywayClean` task
before running the migrations. After the migrations have run Spring Boot will then run the SQL in `data-local.sql` to
populate the database with seed data.

### Updating Local Council Data

The project uses migrations to populate the `local_council` table with data from
`src/main/resources/data/local_councils/local_councils.csv`.

If the CSV file is updated, create a copy of it and call it `local_councils_V<version number>.csv`,
where `version number` is one more than the latest version in`src/main/resources/db/migrations/data/local_councils`.

Then run the utility script to generate the sql for some new migrations by:

- `cd`ing into the `/scripts` folder
- running `node generate_update_local_councils_migrations.js`
- this will output some draft migrations in `/scripts/output/`

#### Migration process for updating Local Council Data

- create a migration using `/scripts/output/draft_upsert_local_councils_migration.sql` to upsert and new/changed local councils
- run the sql statement in `/scripts/output/select_all_local_councils_to_be_deleted.sql` on your local copy of the database to
  get a list of the local councils that will be removed by the delete migration
- write a custom migration to handle any changes that will need to be made before the delete migration can be run
    - check for local council users/admins that will need to be deleted/assigned to another local council
    - check for addresses that will now belong to a different local council
- create a migration using `/scripts/output/draft_delete_local_councils_migration.sql` to delete any removed local councils

### Mock One Login Oauth2

For development, we've mocked elements of the governments one login system (that the web app will be using in
deployment).
When you start the app using the `local` run configuration, this will be available, when you attempt to login. It will
automatically log you in as a user that has every role - and therefore can access all pages.

If you are adding new roles please add the user with the `userId` set in `MockOneLoginHelper` to that new role/table.

If you need to be able to login as a user that has specific roles then you can change the `userId` in
`MockOneLoginHelper` to the id from the `one_login_user` table of a user that has the permissions you want.

#### Disabling the mock One Login Oauth2

If you need to disable the mock to run the app with One login's integration system, edit the run configuration so that
it uses the `local-auth` profile instead of the `local-no-auth` profile.

### One Login accounts

When you run the app with the one login mock disabled and try to view pages, you will be prompted to sign in or create a
One Login account.

To view most pages, your account will need to have been added to the relevant database (e.g. LandlordUser,
LocalCouncilUser) for you to be able to see the page. It checks the database on login (you can step through
`getRolesforSubjectId` in `UserRolesService` to test it), so you will need to log in again to see the change in
permissions (if logging out is not yet implemented, try running in an incognito tab so you are prompted to log in
again).

For local dev, you can add your account by modifying the `data-local.sql` file. Insert an entry into the
`one_login_user` database with a subject_identifier matching your real one login id (see below).
Then you can add entries to any other user database that you need access to (e.g. landlord, local_council_user
with is_manager set to true to see local council admin pages).

#### Finding your One Login id

One way to find your id is to check the `subjectId` in `getRolesForSubjectId` in the `UserRolesService` while you are
logging in.

* Run the app in debug mode, add a break point in `getRolesForSubjectId`
* If you are already logged in it won't hit the breakpoint. Load the app in an incognito tab so that you are prompted
  to log in again.
* When you hit the debug point, your one login id should be available in `subjectId`
  (it should look like `urn:fdc:gov.uk:2022:string-of-characters`)

If anyone knows a better way to do this please add it here!

### Testing Org Landlords

If you need to instead log in as an org landlord to see their dashboard & other views locally, run the `local-org-landlord`
run config or enable the `local-org-landlord` profile. You may need to log out & log in again after enabling this.

### Connecting to AWS

When the service runs in AWS it has the profile of the ECS service it is running on.
This allows it to connect to e.g. S3, the database and other AWS services.
To connect to the deployed database while running locally you need to set up a port forwarding session using SSM due to
networking rules.
To connect to S3 you need to provide your local service with a profile with which to connect.
You can do that using `aws-vault`, as follows.
To set up `aws-vault` follow the instructions in the `prsdb-infra` repository.

#### Setting up `aws-vault` as a profile server

Run

```shell
aws-vault exec <profile> --server
```

This starts a session with aws-vault acting as a credential server.
You can add `-- bash` or `-- powershell` to enter the server using your shell of choice.

Then run

```shell
env | grep AWS_CONTAINER
```

This will return two lines giving you the `AWS_CONTAINER_CREDENTIALS_FULL_URI` and the
`AWS_CONTAINER_AUTHORIZATION_TOKEN` for your server.
Copy both of these lines into your `.env` file and add the line

```
AWS_REGION=eu-west-2
```

Then run the service as usual, it will pick up the profile provided by the `aws-vault exec` command.

When you have finished running the service, run `exit` in the server terminal to close the server.

#### Connecting to AWS S3 locally

By default, when the service is run locally, it uses the `LocalFileUploader` instead of the `AwsS3FileUploader`.
You can manually switch by manipulating the profiles and attributes on those classes.
Currently, there isn't a profile which connects to AWS with an otherwise local build.

## Releasing

### Release flows

There are 3 release pathways we manage:

- `main` -> `test` (Releases to test)
- `main` -> `nft` (Releases to nft)
- `test` -> `production` (Releases to prod) (has extra protections, see below)

We release to integration by merging to `main`. There is no special process for this, just merge when the PR is approved.

### Testing a branch in integration

Use this process when a change needs to be tested in the deployed integration environment and cannot be adequately
tested locally. Integration is a shared environment, so before temporarily deploying a branch, check that nobody else
is using it and tell the team that the environment will be overwritten.

If the branch contains database migrations, agree how to restore or reset the integration database before using this
process. Deploying the branch applies its migrations to the shared database, and redeploying `main` does not reverse
them.

1. Temporarily add your branch to the `push.branches` list in
   [`.github/workflows/build-and-deploy-integration.yml`](.github/workflows/build-and-deploy-integration.yml).
   Make this change locally, but do not commit or push it yet:

   ```yaml
   branches:
     - main
     - <your-branch-name>
   ```

2. Open [Build and Deploy - Integration](https://github.com/communitiesuk/prsdb-webapp/actions/workflows/build-and-deploy-integration.yml)
   in GitHub Actions and record the latest successful run from `main`. Commit the temporary workflow change to the
   branch you want to test and push that branch. Wait for the branch's `Build and Deploy - Integration` run to finish,
   then complete the required testing.
3. After testing, or if the branch deployment fails or testing is abandoned, remove the temporary workflow change from
   the branch and push the cleanup commit. Confirm that the feature PR no longer includes the workflow trigger change.
4. Restore integration:
    - If the branch deployment applied database migrations, follow the agreed recovery or reset plan to restore both the
      database and the application to compatible versions from `main`. A `main` deployment alone does not reverse the
      migrations, and a database reset alone does not replace the feature-branch application image.
    - For a branch without migrations, integration is already restored if a newer successful deployment from `main`
      completed after the branch deployment. Any testing after that deployment did not exercise the branch.
    - If neither applies, open the recorded `main` run, select **Re-run jobs**, then **Re-run all jobs**.
5. Confirm that integration has been restored successfully. If restoration fails, investigate, retry, or seek help.

We also manage **feature releases** — config-only releases that change feature-flag values for a single environment
without shipping any other code. These are built differently to the code releases above; see
[Feature releases](#feature-releases).

The following steps of this guide will refer to the `main` -> `test` workflow, though the steps are the same for other flows.

### Cadence

At least once a sprint we aim to release changes into the Test environment. This process happens automatically when
changes are merged to the `test` branch. Merges into `test`, `nft` and `production` must use normal (not squash) merges
to keep a common git history. PRs into `main` still use the merge queue and squash merges.

### Release infra before webapp

Before releasing the webapp, go to the [prsd-infra](https://github.com/communitiesuk/prsdb-infra) repo and release main to test (follow the
same PR process below)

### PR process

The normal process is simply to raise a PR merging `main` into `test`, name the PR "Release main to test #n" for the nth release to `test`.
For the PR description add a list of all the commits that will be included and their ticket numbers.
In most cases this will be all that is required as all features on integration will have been QA'd, demoed, and be ready for review.
Use the same release number between the webapp repo and infra repo.

Normal code releases do not need a separate release branch unless the PR has [merge conflicts](#merge-conflicts).

Go and find the release tracking Jira ticket:

- Open this [filter](https://mhclgdigital.atlassian.net/issues/?filter=23406).
- The tickets are sorted by created date so the release ticket you're looking for should be near the top.
- If you can't find a release Jira ticket for the type of release you want to do (test/nft or prod), make one by cloning PDJB-1300

Read the release ticket carefully! The template contains steps you should make sure to complete before, during and after the release.

- Fill in the initial release details if needed (release number, type, date)
- Add your PR to the list in ticket
- Make sure you're happy with all the ticket steps before returning to the PR.

Note: You will probably see the message "This branch is out-of-date with the base branch" on your PR. This does not need to be resolved and
can be ignored.

In the rare case that there are changes on `main` that we do not want to release to `test`:

- Identify the last commit on `main` before the code that you do not want to release was added
- Create a new branch off of that commit, e.g. `release/main-to-test-11` for the 11th release to `test`
- Identify any later commits that you _do_ want to release to `test` and cherry-pick them onto the new branch
- Merge the new branch into `test`
- Merge `test` back into `main` **using a normal merge - not a squash commit** - you will need to ask an admin on the
  repo to temporarily allow normal merges into `main` to do this

#### Merge conflicts

If a release PR has merge conflicts, resolve them on a separate branch rather than on the source branch:

- Create a branch from `test`, e.g. `release/main-to-test-52`
- Merge `main` into the branch and resolve the conflicts
- Raise a replacement PR from the branch into `test`, keeping the release title and notes. Update the link on the
  release ticket and close the original PR
- Merge the PR using a **normal merge, not squash**

There is no need to merge the resolution back into `main`, because the normal merge into `test` keeps the history
needed for the next release.

#### Hotfixes

It should be very rare that a hotfix will need to be made directly to `test` (vs. being made on `main` and then
releasing to `test` in the normal way). However, if this is needed:

- Create a new branch from `test` e.g. `hotfix/prsd-<ticket number>-<description>`
- Make the changes on the hotfix branch
- Merge the hotfix branch into `test`
- Merge `test` back into `main` **using a normal merge - not a squash commit** - you will need to ask an admin on the
  repo to temporarily allow normal merges into `main` to do this

### Feature releases

A _feature release_ is a config-only release that changes feature-flag values for a single environment, without
shipping any code that has accumulated on `main` since the last code release. It is still managed by PRs, but is built
differently to a code release.

Feature-flag values are set per environment in the webapp's `application-<profile>.yml` files, which live on the same
branches that deploy to each environment. A normal `main` -> `test` merge would therefore release all of main's
unreleased code as well, so a feature release instead branches from the **target environment branch** and cherry-picks
only the flag change.

Unlike code releases, feature releases apply to `prsdb-webapp` only — feature flags are not configured in `prsdb-infra`,
so no infra release is required.

| Environment | Flag file to edit                           | Deploy branch |
|-------------|---------------------------------------------|---------------|
| test        | `src/main/resources/application-test.yml`   | `test`        |
| nft         | `src/main/resources/application-nft.yml`    | `nft`         |
| production  | `src/main/resources/application.yml` (base) | `production`  |

Every flag is present in each environment override file, so the `application.yml` base values only affect production.

Feature flags are usually created without being assigned to a release group until it is clear which release they belong
to. If a feature should go live as part of a release group (see [Feature flags](docs/FeatureFlagsReadMe.md)) that does
not exist yet, create that release and associate the flag as part of the `main` change in the first step below.

To make a feature release (the example uses `test`; other environments follow the same steps):

- **Make the change on `main` first.** Raise a normal PR that edits _only_ the target environment's flag file, as a
  standalone config change, and merge it through the usual queue and squash process. The change is inert for
  test/nft/production until it is released — it only takes effect immediately on integration (which deploys `main`).
  Use a separate PR per environment so each config change can be cherry-picked separately.
- Create a branch from the **target environment branch** (not `main`), e.g. `release/feature-test-3` for the 3rd feature
  release to `test`.
- Cherry-pick _only_ the flag commit from `main` onto the new branch. Because the branch is based on `test`, the diff
  contains only the flag change and none of main's unreleased code.
- Raise a PR merging the branch into `test`, named `Feature release to test #n` for the nth feature release to `test`
  (to find `n`, check GitHub for the most recent `Feature release to test` PR and increment its number). In the
  description, state which flag(s)/release(s) change and link the `main` PR.
- Merge with a normal merge (not a squash commit), as with other merges into environment branches. No merge back into
  `main` is needed — the change already originated there.

Feature releases can cause conflicts in a later code release, for example if old flags have since been removed.
If this happens, follow the steps under [Merge conflicts](#merge-conflicts).

Each environment is released independently; there is no required ordering, so a flag can be feature-released straight to
production if it were neceesary (subject to the prod approval checks in [Feature flag releases](#feature-flag-releases) below).

### Releasing to Prod

There are extra considerations to take when releasing from `test` to `production`.
We need to ensure that any new behaviour on prod is auditably approved before continuing.

#### Code releases

This is the standard release where we release new code to production.

In the release PR, check the status of all tickets that will be released.

if there is any ticket that'll be released that is not 'Done' and is not behind a feature flag, **stop** and check in with your tech lead.

Before merging, take a note of the last merged PR to `production`. You may need this later if you need to rollback.

#### Feature flag releases

These are [feature releases](#feature-releases) to production — we release configuration only (a feature-flag change) and
no other code. Follow the feature-release process above to build the PR, then apply the extra production checks below.

The feature flag should be labelled with an epic ticket number.
Look through the tickets in the feature flag's epic and ensure they are all approved by the product team. This will be denoted as 'Done' as
the Jira ticket status.

If there is any ticket that'll be released that is not 'Done', **stop** and check in with your tech lead.

#### Rollback procedure

Our preference where possible is to rollback a faulty production release by reverting the merge PR. This will deploy the last version of the
code to prod.

In some cases however this alone will not correctly rollback the release, such as if the release contains a database migration.
In this case, we should revert the merge PR with a commit included to:

- Keep the migrations that are on still present in the deployed code, otherwise the revert PR will remove them which can cause issues.
- Add a new migration to undo the previous migrations impact using SQL statements.

## Licence

Unless stated otherwise, the codebase is released under [the MIT License][mit].
This covers both the codebase and any sample code in the documentation.

Copyright © 2025 Crown Copyright (Ministry of Housing, Communities & Local Government)

[mit]: LICENCE
