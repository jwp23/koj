# Mutation Testing Deferred

Decision: The project does not run mutation testing yet. No mutation tool is installed, and no
hook or CI job depends on one.

Rationale: pitest is the established JVM mutation tool, but its own documentation says Kotlin is
supported only through the proprietary Arcmutate plugin and that without it most generated
mutants are junk. The Gradle plugin for pitest claims only initial Gradle 9 support. The one
Kotlin-native alternative found, mutflow, is a single-author compiler plugin that requires
annotating tests. None of these is worth the supply-chain exposure before there is logic to
mutate.

Checked 2026-11-14. Revisit once the intake model and the reminder rules exist: they are pure
Kotlin on the JVM, which is the case mutation tools handle best. Until then the intake model's
hand-calculated expected values and one test per reminder rule carry the weight.
