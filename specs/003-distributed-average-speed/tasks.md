# Tasks: Version 3 Distributed Average Speed

Keep tasks small enough for focused commits.

## Spec Baseline

- [x] Create Version 3 spec folder.
- [x] Define selected distributed pattern.
- [x] Define Version 3 implementation plan.
- [x] Define Version 3 task checklist.
- [x] Define Version 3 research notes.
- [ ] Review Version 3 scope with the team.
- [ ] Update README with Version 3 branch and scope after implementation starts.

## Architecture Design

- [ ] Draw deployment diagram for Version 3.
- [ ] Document Master-Worker responsibilities.
- [ ] Document Master-Worker task flow.
- [ ] Document code boundaries without presenting them as extra architecture patterns.
- [ ] Define local multi-process deployment.
- [ ] Define university-server deployment.
- [ ] Define failure handling for missing or failed worker outputs.

## Mergeable Domain Model

- [x] Add partial aggregate model for route/month totals.
- [x] Include total segment speed for mergeable average segment speed.
- [x] Verify weighted average can be recomputed after merge.
- [x] Verify observed bus count remains correct under selected partition key.
- [x] Add tests for merging two partial results for the same route/month.
- [x] Add tests for deterministic final ordering after merge.

## Partitioning

- [x] Add partition work item model.
- [x] Add partition manifest model.
- [x] Add partition writer.
- [x] Add partition-only CLI mode for option B deployment.
- [x] Partition by `routeId + busId` unless team chooses route-only first.
- [x] Ensure partitioning streams input rows instead of loading all datagrams into memory.
- [x] Add tests for active route filtering during partitioning.
- [x] Add tests for invalid row accounting during partitioning.
- [x] Add tests that the same route/bus pair always maps to the same partition.

## Worker

- [x] Add worker processor service.
- [x] Add worker CLI mode or entry point.
- [x] Worker reads one partition.
- [x] Worker sorts by route, bus, timestamp.
- [x] Worker calculates valid segments.
- [x] Worker writes partial result CSV.
- [x] Worker reports task metrics.
- [x] Add worker tests with tiny partition files.
- [x] Add scan-worker mode for multi-PC raw datagram scanning.
- [x] Add tests for scan-worker partition filtering.

## Master

- [x] Add distributed master service.
- [x] Add master CLI mode.
- [x] Master creates run directory.
- [x] Master writes task manifest.
- [x] Master launches or coordinates workers.
- [x] Master waits for expected partial outputs.
- [x] Master detects failed or missing outputs.
- [x] Master merges partial results.
- [x] Master writes final deterministic CSV.
- [x] Master prints distributed run summary.
- [x] Add merge mode for partial CSVs produced on remote PCs.
- [x] Add tests for remote partial merge.

## Experiment

- [ ] Run Version 2 MiniPilot baseline with 1 thread.
- [ ] Run Version 2 MiniPilot baseline with multiple thread counts.
- [ ] Run Version 3 MiniPilot with 1 worker.
- [x] Run Version 3 MiniPilot with multiple workers.
- [ ] Compare Version 2 and Version 3 MiniPilot output files.
- [x] Run Version 3 on datagrams4Pilot if environment allows.
- [x] Record memory behavior and runtime.
- [x] Determine when distribution is worth it.

## Documentation

- [x] Update `docs/experiment-results.md` with Version 2 and Version 3 measurements.
- [ ] Add Version 3 architecture drivers using QAW scenarios.
- [ ] Add Version 3 deployment documentation.
- [ ] Add Master-Worker pattern mapping documentation.
- [ ] Add CLI usage examples.
- [ ] Add limitations and assumptions.
- [x] Add multi-PC scan-worker runbook.
- [x] Add Version 3 MiniPilot experiment analysis.
- [x] Add Version 3 full pilot experiment analysis.

## Verification

- [x] `./gradlew clean build` succeeds.
- [x] `./gradlew check` succeeds.
- [x] CLI help works.
- [x] Version 2 tests still pass.
- [x] Version 3 partition tests pass.
- [x] Version 3 worker tests pass.
- [x] Version 3 merger tests pass.
- [x] Version 3 scan-worker tests pass.
- [ ] MiniPilot output equivalence with Version 2 is documented.

## Guardrails

- [ ] Do not change speed calculation semantics without updating specs.
- [ ] Keep Master-Worker as the only Version 3 distribution pattern explanation.
- [ ] Do not commit raw university datasets.
- [ ] Do not require a database.
- [ ] Do not require Spring Boot.
- [ ] Keep domain services independent from distribution infrastructure.
