# Agent Work Is Tracked in Beads, Not GitHub Issues

Decision: Agents track the work they do or find in beads and never open a GitHub issue for it,
including bugs found while working, follow-ups, and review findings deferred out of a pull
request. GitHub issues stay the place where people outside the work report bugs and propose
features; a bead that answers one names its number, and so does the pull request.

Rationale: Two trackers for the same work left some deferred bugs visible only on GitHub and
invisible to `bd ready`, and AGENTS.md's task-tracking rule says agents use beads for all of it.
One place per kind of work keeps the agents' queue complete without crowding the public issue list
with their own notes.
