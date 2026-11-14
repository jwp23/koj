# Git Hook Distribution

Decision: The hooks are committed in `.githooks/` and enabled with `scripts/setup-hooks.sh`,
which sets `core.hooksPath` to that directory. Each hook is a single `exec` line that delegates
to a script in `scripts/`. Maintainers running the beads issue tracker keep `core.hooksPath` on
`.beads/hooks/`, whose hooks keep their beads section and delegate to the same scripts. No hook
framework is adopted.

Rationale: `core.hooksPath` is not shared by a clone, so without committed hooks the checks would
run only on machines configured by hand. A framework such as `pre-commit` wants to own the hooks
directory, which beads already owns on maintainer machines, and would put a Python dependency in
front of building a Kotlin application. Keeping the check bodies in `scripts/` gives both hook
directories one definition, so they cannot drift apart.
