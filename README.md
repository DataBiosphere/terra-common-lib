# terra-common-lib
## Publishing 
MC Terra components use JFrog Artifactory to publish libraries to a central Maven [repository](https://broadinstitute.jfrog.io/ui/packages).
The library version number is the `version` in [build.gradle](build.gradle). We use [github actions](/.github/workflows) to bumping version and publish to repository.

The publishing procedure is:
1. After PR is merged to develop branch: github action automatically bumped the minor `version` in [build.gradle](build.gradle) then publish to [lib-snapshot-local](https://broadinstitute.jfrog.io/ui/repos/tree/General/libs-snapshot-local)
2. After release is created(usually a manual step): github action automatically bumped the minor `version` in [build.gradle](build.gradle) then publish to [lib-snapshot-release](https://broadinstitute.jfrog.io/ui/repos/tree/General/libs-release-local).
3. To bump major version, we need manually update `version` in [build.gradle](build.gradle) value first then create the release.
 
## Development 
### Dependencies
We use [Gradle's dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html)
to ensure that builds use the same transitive dependencies, so they're reproducible. This means that
adding or updating a dependency requires telling Gradle to save the change. If you're getting errors
that mention "dependency lock state" after changing a dep, you need to do this step.

```sh
./gradlew dependencies --write-locks
```

### Setup

### Database Configuration
Terra Common Lib includes functionality using [Stairway](https://github.com/DataBiosphere/stairway).
Some of the TCL unit tests therefore rely on a running SQL instance to run Stairway. There are two
recommended ways to set up a local Postgres Database for the unit tests.

#### Option A: Docker Postgres
##### Running the Postgres Container
To start a postgres container configured with the necessary databases:
```sh
./local-dev/run_postgres.sh start
```
To stop the container:
```sh
./local-dev/run_postgres.sh start
```
Note that the contents of the database is not saved between container runs.

##### Connecting to the Postgres Container
Use `psql` to connect to databases within the started database container, e.g. for database `tclstairway` users `tclstairwayuser` with password `tclstairwaypwd`:
```sh
PGPASSWORD=tclstairwaypwd psql postgresql://127.0.0.1:5432/tclstairway -U tclstairwayuser
```

#### Option B: Local Postgres 
##### Database Configuration
Set up a local Postgres instance. To set up TCL's required database for unit tests, run the following command, which will create the DB's and users:

```sh
psql -f local-dev/local-postgres-init.sql
```
