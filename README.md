# terra-common-lib
## Publishing 
MC Terra components use JFrog Artifactory to publish libraries to a central Maven [repository](https://broadinstitute.jfrog.io/ui/packages).
The library version number is the `version` in [build.gradle](build.gradle). We use [github actions](/.github/workflows) to bumping version and publish to repository.

The publishing procedure is:
1. Automatically after PR is merged to develop branch: bump version number and publish to [lib-snapshot-local](https://broadinstitute.jfrog.io/ui/repos/tree/General/libs-snapshot-local)
2. Manually create a release: bump version number and publish to [lib-snapshot-release](https://broadinstitute.jfrog.io/ui/repos/tree/General/libs-release-local).
To bump major version, we need manually update version value first before create the release. 