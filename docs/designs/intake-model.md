# Intake Model

The intake model computes how much caffeine and L-theanine is in the body at a given time from
the logged drinks. It is pure Kotlin with no Android dependency.

## Calculation

- Each substance is computed separately, with its own absorption time and half-life.
- A **fast** drink is one dose at its start time. The in-body amount rises as the dose is
  absorbed, peaks, and then halves every half-life.
- A **sipped** drink is split into equal small doses spread evenly from its start to its finish.
  Each small dose follows the same rise and fall, and the results are summed.
- A sipped drink still in progress is computed as if it will finish at its default duration.
- The total at any time is the sum over all drinks, including drinks from earlier days whose
  caffeine has not cleared.

## Parameters

Half-lives and absorption times are settings. Their defaults come from published
pharmacokinetic ranges; each default is cited where it is defined in the code. Caffeine
half-life varies widely between people, so the user is expected to adjust it.

## Ratios

- **Taken ratio** — L-theanine dosed divided by caffeine dosed, per drink and per day.
- **In-body ratio** — L-theanine in the body divided by caffeine in the body at one moment.
  L-theanine clears much faster than caffeine, so this falls through the day.

## Limits

- The result is an estimate from population averages, not a measurement.
- The model treats equal amounts from different sources identically. Differences between
  sources appear in check-in ratings, not in the graph.
- Sources with no caffeine and no L-theanine contribute nothing to the calculation. They exist
  as events only.
