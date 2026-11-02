# Does regenerating LLM tests help, and when? Three regeneration policies on ta4j, json-schema-validator and dnsjava at commit granularity

## Key findings

1. **How many tests survive to the last commit depends less on the regeneration policy than on whether a large change comes after the last regeneration.** On json-schema-validator the large changes to the scope classes (commits #2 to #41) come before the first release reset (#98), and the second release reset (#146) comes five commits before the 2.0.0 refactoring (#151). On ta4j every set regenerated after the large change at #21 survives to the end.
2. **Where both were alive, the developer tests covered at least as many changed lines as the LLM tests on json-schema-validator, and neither side was consistently ahead on dnsjava.** On json-schema-validator Dev(scope) covered more lines than KTester at 18 of the 24 listed commits and the same number at 6, never fewer; the largest gaps are at the refactoring commits #4 (161 vs 98 of 178 lines) and #10 (182 vs 110 of 279). On dnsjava Dev(scope) covered more than KTester at 5 listed commits and fewer at 4; which group covered a change depended on which scope class it touched (#15, Message: 110 vs 17 of 198; #30, Zone: 52 vs 132 for Ouédraogo of 193).
3. **Survival and changed-line coverage have to be read separately.** A regenerated set can stay fully alive while covering none of the later changes: on ta4j the set regenerated at #426 is 26/26 at the end of the range, but at #492 and #493, which change 251 executable lines of `CashFlow` and `BarSeriesManager`, it covers 0. On json-schema-validator the set regenerated at #98 is also fully alive and covers 13 of the 14 lines changed at #99.

## Question

A test set generated at a starting commit *t* breaks as the code under test moves on. If the generated tests are regenerated along the way, when should that happen and how much is gained? Three regeneration policies are compared against no regeneration on three Java projects, with two measurements taken at every commit after *t*: how many of the tests still pass (survival), and how many of the lines changed at that commit the surviving tests execute (changed-line coverage).

## Setup

**Projects.** Three Java projects, each from a starting commit *t* to the head of `master` in late October 2026, first-parent commits only. Commit #*n* is the *n*-th first-parent commit after *t* (*t* = #0).

| Project | *t* | Range | Scope classes |
|---|---|---|---|
| ta4j-core | release 0.16 (`897a114`, 2024-05-17) | 530 commits to 2026-10-22 | `NumericIndicator`, `Position`, `BarSeriesManager`, `CashFlow`, `ATRIndicator` (84 methods) |
| json-schema-validator | release 1.1.0 (`0c14a7a`, 2023-12-15) | 229 commits to 2026-10-23 | `JsonSchema`, `BaseJsonValidator`, `AnyOfValidator`, `OneOfValidator`, `JsonSchemaFactory` (64 methods) |
| dnsjava | the `master` commit of the day release 3.5.3 was made (`4c51bf2`, 2023-11-11) | 178 commits to 2026-10-29, of which 154 build | `Message`, `Zone`, `TSIG` (54 methods) |

**Groups.** Every count is the number of test methods that passed three runs at *t*.

| Project | Dev(scope) | KTester | Ouédraogo |
|---|---:|---:|---:|
| ta4j | 67 (test classes of the scope classes) | excluded (14 passing methods) | 98 |
| json-schema-validator | 153 (methods that execute a scope class) | 131 | 41 |
| dnsjava | 353 (methods that execute a scope class) | 225 | 75 |

- **Dev(scope)**: the project's own tests that exercise the scope classes. They are not regenerated under any policy and serve as a reference line.
- **KTester**: Li et al., ICSE 2026, the released tool with its default of three repair rounds (the json-schema-validator set at *t* was generated with five).
- **Ouédraogo**: Ouédraogo et al., EMSE 2026, the five prompt templates with three adjustments: the signatures of the project types the class refers to are added to the prompt, the model is told to use only the API shown and no mocks, and missing imports are inserted after generation.

**Regeneration policies.** Each LLM group is run under four policies; a regeneration generates tests for the scope classes at that commit with the same generator settings as at *t*.

| Policy | When | What |
|---|---|---|
| No regeneration | never | the set generated at *t* |
| Reset at each release | two releases per project: ta4j 0.22.0 (#155) and 0.23.0 (#426); json-schema-validator 1.5.0 (#98) and 1.5.9 (#146); dnsjava v3.6.0 (#34) and v3.6.5-1 (#102) | the group is discarded and generated again |
| Reset when 50% broken | the first commit where at least half of the group has broken, read from the no-regeneration curve, once per group: ta4j #21; json-schema-validator #4 (Ouédraogo) and #10 (KTester); dnsjava never | the same |
| Fill when 50% broken | the same commit | the surviving tests are kept; new tests are added for the focal methods (Ouédraogo: the classes) of the broken ones, at most as many as broke per focal method |

**Measurement.**

- A method survives at commit *c* if it passed at every evaluated commit up to *c*; a method that fails once is not run again.
- A regeneration starts a new segment: the methods that pass three runs at the regeneration commit are the segment denominator, and survival inside a segment only decreases.
- Changed-line coverage is, for each commit that changes executable lines of the scope classes, the share of those lines executed by the group's surviving methods at that commit.

## Results

Table 1 gives survival at the last commit; Table 2 gives how the sets at *t* were selected. The per-commit series (survival and changed-line coverage under each policy, one sheet per project) are in the shared spreadsheet: https://docs.google.com/spreadsheets/d/1O7A6wZ0PJqCfllvPSUACv47pht6msxIKShUiZjeef6M/edit. There, a survival row is shown when a group lost three or more methods, and a coverage row when the commit changed ten or more scope lines or a group lost methods; the remaining commits are aggregated. The 50% policies did not trigger on dnsjava, so its sheets under those two policies repeat the no-regeneration series.

**Table 1. Surviving methods at the last commit of the range** (alive / denominator of the last segment; number of regenerations in brackets).


| Project | Group | No regeneration | Reset at each release | Reset when 50% broken | Fill when 50% broken |
|---|---|---|---|---|---|
| ta4j | Dev(scope) (ref., not regenerated) | 0/67 (0.0%) | 0/67 (0.0%) | 0/67 (0.0%) | 0/67 (0.0%) |
|  | KTester | (excluded) | (excluded) | (excluded) | (excluded) |
|  | Ouédraogo | 0/98 (0.0%) | 26/26 (100.0%) [2 regen] | 14/14 (100.0%) [1 regen] | 14/14 (100.0%) [1 regen] |
| json-schema-validator | Dev(scope) (ref., not regenerated) | 0/153 (0.0%) | 0/153 (0.0%) | 0/153 (0.0%) | 0/153 (0.0%) |
|  | KTester | 0/131 (0.0%) | 0/123 (0.0%) [2 regen] | 16/71 (22.5%) [1 regen] | 0/58 (0.0%) [1 regen] |
|  | Ouédraogo | 0/41 (0.0%) | 0/10 (0.0%) [2 regen] | 0/29 (0.0%) [1 regen] | 0/35 (0.0%) [1 regen] |
| dnsjava | Dev(scope) (ref., not regenerated) | 119/353 (33.7%) | 119/353 (33.7%) | 119/353 (33.7%) | 119/353 (33.7%) |
|  | KTester | 206/225 (91.6%) | 120/120 (100.0%) [2 regen] | 206/225 (91.6%) | 206/225 (91.6%) |
|  | Ouédraogo | 74/75 (98.7%) | 79/79 (100.0%) [2 regen] | 74/75 (98.7%) | 74/75 (98.7%) |

**Table 2. Selection of the LLM sets at *t*** (generated methods → methods passing three runs → after removing duplicates; the last column is the no-regeneration denominator). Ouédraogo was generated twice on ta4j and dnsjava because the first run gave fewer passing methods than Dev(scope); the rows sum both runs.

| Project | Group | Generation runs | Generated methods | Passed 3 runs | Duplicates removed | Set at *t* |
|---|---|---:|---:|---:|---:|---:|
| ta4j | KTester | 1 | 507 | 14 | 0 | 14 (excluded) |
| ta4j | Ouédraogo | 2 | 767 | 98 | 0 | 98 |
| json-schema-validator | KTester | 1 | 523 | 131 | 0 | 131 |
| json-schema-validator | Ouédraogo | 1 | 313 | 41 | 0 | 41 |
| dnsjava | KTester | 1 | 716 | 225 | 0 | 225 |
| dnsjava | Ouédraogo | 2 | 566 | 77 | 2 | 75 |

## Observations

1. **The number of passing methods a regeneration produces differs by up to ten times between commits of the same project.** KTester: 71, 269 and 123 at json-schema-validator #10, #98 and #146; 397 and 120 at dnsjava #34 and #102. Ouédraogo: 29, 0 and 10 at json-schema-validator #4, #98 and #146 (at #98 none of its 24 files compiled); 14, 7 and 26 at ta4j #21, #155 and #426. The size of the restored set depends on this yield more than on the policy.
2. **At the 50% trigger a fill is almost a reset.** On ta4j every Ouédraogo method breaks at #21, so the fill carries nothing and equals the reset (14 new methods). On json-schema-validator the Ouédraogo fill at #4 carries 13 and adds 22; the KTester fill at #10 carries 51 and adds 7, because only one of the focal methods of the broken tests (`JsonSchema.hasTypeValidator`) received passing tests from the generation at #10. Both sets are lost by #10 (Ouédraogo) and at #151 (KTester).
3. **On dnsjava the developer reference line falls at one commit.** Dev(scope) loses 204 of 353 methods at #5, all in the `dnssec` test package; the two LLM groups lose nothing there. The LLM groups on this project are unit tests of `Message`, `Zone` and `TSIG`, whose public API changes little over the range.

## Limitations

- ta4j has no KTester group: the generator produced 14 passing methods at *t*, and its environment cannot build the project after #21 (Java 21 and later), so it could not regenerate there.

## Repository

Branches `experiment/2026-10-week5-<project>-regen-policies` in `Hamjoon/ta4j`, `Hamjoon/json-schema-validator` and `Hamjoon/dnsjava`, directory `experiments/regen-policies/`. The no-regeneration runs the policies are evaluated against: ta4j and dnsjava `experiments/commit-survival/`, json-schema-validator `experiments/commit-survival-run2/`. Per-commit tables: https://docs.google.com/spreadsheets/d/1O7A6wZ0PJqCfllvPSUACv47pht6msxIKShUiZjeef6M/edit.

- Plan and pools: `results/policy/plan.md`, `results/policy/pools/<group>/<index>/` (generated files, `funnel.json`, `pool.json`).
- Per policy, `results/policy/<policy>/`: `survival.csv`, `segments.json`, `first-failures.csv`, `coverage-commits.csv`, `coverage-missing.csv`.
- Cross-policy: `results/policy/summary.csv`, `consistency.md`; per-test run records under `results/policy/runs/`.

