# Contributing to koj

## Reporting Bugs and Proposing Features

Open a GitHub issue.

## Working With an AI Coding Agent

Agent-driven work in this repository requires [beads](https://github.com/gastownhall/beads)
(`bd`). The agent instructions in [AGENTS.md](AGENTS.md) direct agents to track their tasks and
persistent memory in beads, and the committed Claude Code session hook runs `bd prime` at the
start of every session. Without `bd` installed, that hook fails.

Contributing by hand does not require beads.
