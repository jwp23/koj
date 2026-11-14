<!--
The title of this pull request becomes the commit subject on main, because pull
requests are squash-merged. It must be a single Conventional Commits line, e.g.
"feat: log a drink with one tap". CI checks this.
-->

## Summary

<!-- What changes, and why. One bullet per logical change. -->

-

## Decision Record

<!--
Did this change make a technical choice — a new dependency, a library, a pattern,
a convention? If so it needs an ADR (docs/adr/) or a decision doc (docs/decisions/).
Fill in one of these two lines and delete the other.
-->

- Recorded in: `docs/…`
- Not needed because:

## Tests

<!--
Which tests cover this, and were they written first? A bugfix needs a test that
reproduces the bug.
-->

-

## Checklist

- [ ] Tests were written before the implementation, and cover the new behaviour
- [ ] `./gradlew test` passes locally
- [ ] Changes to storage, screens, or notifications passed `./gradlew connectedAndroidTest`, or none were made
- [ ] Lock-screen behaviour was checked on a device and the result is recorded here, or none changed
- [ ] Documentation updated, or no user-facing behaviour changed
