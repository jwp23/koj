# Graph Drawn With Compose Canvas

Decision: The intake graph is drawn by the app itself with Compose's `Canvas`, not with a
charting library. Each intake marker and check-in marker is a separate composable placed over
the canvas, so it has its own tap target and its own semantics node.

Rationale: The graph is two lines, a projection, and markers that respond to taps; Compose
already provides everything needed to draw it, and a charting library would be a new dependency
whose marker and tap model the design would have to fit. Markers that are real composables can
be found and tapped by Compose UI tests and by accessibility services, which shapes drawn inside
a canvas cannot.
