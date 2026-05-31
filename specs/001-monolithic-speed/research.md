# Research: Version 1 Monolithic Speed Calculator

## Research Goals

Resolve data and calculation assumptions before implementation reaches real assignment files.

## Known Inputs

```text
/opt/sitm-mio/lines-241-ActiveGT.csv
/opt/sitm-mio/datagrams-MiniPilot.csv
/opt/sitm-mio/datagrams4Pilot.csv
/opt/sitm-mio/Diccionario_De_Datos-OkGTM.pdf
```

The first implementation target is `datagrams-MiniPilot.csv`.

## Data Questions To Answer

### Active Route File

- What is the delimiter?
- What is the encoding?
- Is there a header row?
- Which column identifies the route?
- Are route IDs numeric, alphanumeric, or padded?
- Does the file already contain only active routes, or is there an active-status column?

### Datagram File

- What is the delimiter?
- What is the encoding?
- Is there a header row?
- Which column identifies route ID?
- Which column identifies bus/vehicle ID?
- Which column contains timestamp?
- Which columns contain latitude and longitude?
- Are coordinates decimal degrees?
- Are rows already sorted?
- Can the same bus appear on multiple routes?

### Timestamp

- What timestamp format is used?
- Does the timestamp include timezone or offset?
- If no timezone is present, which timezone should be assumed?
- Should month grouping use local Cali time?
- How should duplicate timestamps for the same route and bus be handled?

## Calculation Research

### Distance

Use the Haversine formula for Version 1 because the datagram positions are latitude/longitude coordinates.

Initial Earth radius:

```text
6371.0088 km
```

This is the mean Earth radius recommended for general geodesic calculations.

### Segment Validity

Initial thresholds:

```text
max_gap_minutes = 30
max_speed_kmh = 120
```

Rationale:

- Very long gaps are unlikely to represent continuous movement.
- Very high speeds are likely GPS noise or invalid records for urban buses.
- Thresholds are configurable because assignment data may require adjustment.

### Average Definitions

Version 1 will output both:

- Weighted average speed: `total_distance_km / total_time_hours`.
- Arithmetic average segment speed: `sum(segment_speed_kmh) / valid_segment_count`.

Weighted average is the primary value because it is based on total observed movement over total observed time.

### Month Assignment

Initial decision:

- Assign each segment to the year-month of the later datagram timestamp.

Reason:

- The segment becomes known only when the later point is processed.
- This avoids assigning a segment to a month before the bus reached the later point.

Open risk:

- Segments crossing month boundaries may be slightly biased unless split at the boundary. Version 1 will not split segments unless the assignment requires it.

## Architecture Research

Version 1 remains monolithic:

- In-memory processing is acceptable for `datagrams-MiniPilot.csv`.
- For `datagrams4Pilot.csv`, first try the same deterministic in-memory sort and measure runtime/memory behavior.
- If memory becomes a problem, prefer a single-process external-sort or chunking approach in Version 1, but do not add concurrency or distributed processing.

## Testing Research

Use synthetic data for repeatable tests:

- Two points roughly one kilometer apart with one known time delta.
- One inactive route that must be filtered.
- One bus with duplicate or reversed timestamps.
- One segment above the speed threshold.
- Two buses on the same route/month to verify observed bus count.
- Two months to verify grouping.

Known-distance tests should use tolerances because Haversine results depend on Earth radius.

## Pending Data Dictionary Review

Before implementing real CSV readers, inspect `Diccionario_De_Datos-OkGTM.pdf` and update:

- Confirmed column names.
- Confirmed units.
- Confirmed timestamp format.
- Confirmed coordinate format.
- Confirmed route and bus identifiers.
- Any official validity constraints.
