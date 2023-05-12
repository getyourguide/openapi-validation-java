# Release process

1. Update the `version` in `build.gradle`
2. Update `CHANGELOG.md`
   - Add the new version with the date
   - Add the new version at the bottom and link it to the tag
   - Update the `unreleased` link at the bottom
3. Tag the commit on main with above changes with the version (e.g. `v1.0.0`)
4. Push the tag
   - This will trigger the following in CI
     - Push new version to maven central (sonatype)
     - Create a GitHub release
