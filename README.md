# terra-common-lib
## Publishing 
MC Terra components use JFrog Artifactory to publish libraries to a central Maven [repository](https://broadinstitute.jfrog.io/ui/packages).
The library version number is the `version` in [build.gradle](build.gradle). We use [github actions](/.github/workflows) to bumping version and publish to repository.

The publishing procedure is:
1. After PR is merged to develop branch: github action automatically bumped the minor `version` in [build.gradle](build.gradle) then publish to [lib-snapshot-local](https://broadinstitute.jfrog.io/ui/repos/tree/General/libs-snapshot-local)
2. After release is created(usually a manual step): github action automatically bumped the minor `version` in [build.gradle](build.gradle) then publish to [lib-snapshot-release](https://broadinstitute.jfrog.io/ui/repos/tree/General/libs-release-local).
3. To bump major version, we need manually update `version` in [build.gradle](build.gradle) value first then create the release.
 
