# Does regenerating LLM tests help, and when? Three regeneration policies on ta4j, json-schema-validator and dnsjava at commit granularity

## Question

A test set generated at a starting commit *t* breaks as the code under test moves on. If the generated tests are regenerated along the way, when should that happen and how much is gained? Three regeneration policies are compared against no regeneration on three Java projects, with two measurements taken at every commit after *t*: how many of the tests still pass (survival), and how many of the lines changed at that commit the surviving tests execute (changed-line coverage).

## Setup

**Projects.** Three Java projects, each from a starting commit *t* to the head of `master` in late October 2026, first-parent commits only.

- **ta4j-core.** *t* = release 0.16 (`897a114`, 2024-05-17); 530 commits to 2026-10-22. Scope classes: `NumericIndicator`, `Position`, `BarSeriesManager`, `CashFlow`, `ATRIndicator` (84 methods).
- **json-schema-validator.** *t* = release 1.1.0 (`0c14a7a`, 2023-12-15); 229 commits to 2026-10-23. Scope classes: `JsonSchema`, `BaseJsonValidator`, `AnyOfValidator`, `OneOfValidator`, `JsonSchemaFactory` (64 methods).
- **dnsjava.** *t* = the `master` commit of the day release 3.5.3 was made (`4c51bf2`, 2023-11-11); 178 commits to 2026-10-29, of which 154 build (21 do not touch the module, 3 do not build upstream). Scope classes: `Message`, `Zone`, `TSIG` (54 methods).

**Groups at *t*.** Every count is the number of test methods that passed three runs.

| Project | Dev(scope) | KTester | Ouédraogo |
|---|---:|---:|---:|
| ta4j | 67 (test classes of the scope classes) | excluded (14 passing methods) | 98 |
| json-schema-validator | 153 (methods that execute a scope class) | 131 | 41 |
| dnsjava | 353 (methods that execute a scope class) | 225 | 75 |

- **Dev(scope)**: the project's own tests that exercise the scope classes. They are not regenerated under any policy and serve as a reference line.
- **KTester**: Li et al., ICSE 2026, the released tool with its default of three repair rounds (the json-schema-validator set at *t* was generated with five).
- **Ouédraogo**: Ouédraogo et al., EMSE 2026, the five prompt templates with three adjustments: the signatures of the project types the class refers to are added to the prompt, the model is told to use only the API shown and no mocks, and missing imports are inserted after generation.

**Policies.** Each LLM group is run under four policies; a regeneration generates tests for the scope classes at that commit with the same generator settings as at *t*.

- *No regeneration*: the set generated at *t*.
- *Reset at each release*: at two releases in the range (ta4j 0.22.0 and 0.23.0; json-schema-validator 1.5.0 and 1.5.9; dnsjava v3.6.0 and v3.6.5-1) the group is discarded and generated again.
- *Reset when 50% broken*: the same, at the first commit where at least half of the group has broken (taken from the no-regeneration curve, once per group).
- *Fill when 50% broken*: at that commit the surviving tests are kept and new tests are generated for the focal methods (Ouédraogo: the classes) of the broken ones, up to the number broken.

**Measurement.**

- A method survives at commit *c* if it passed at every evaluated commit up to *c*; a method that fails once is not run again.
- A regeneration opens a new segment: the methods that pass three runs at the regeneration commit are the segment denominator, survival inside a segment only decreases, and the regeneration commit has two rows (losses of the old set, then the new set in bold).
- Changed-line coverage is, for each commit that changes executable lines of the scope classes, the share of those lines executed by the group's surviving methods at that commit. Survival and coverage come from the same run.

## Results

Table 1 summarises survival at the end of the range; Tables 2 to 9 give the per-commit series, one pair per policy. In the per-commit tables a survival row is shown when a group lost three or more methods, and a coverage row when the commit changed ten or more scope lines or a group lost methods; the remaining commits are aggregated. The 50% policies did not trigger on dnsjava, so its rows under those two policies repeat the no-regeneration rows.

**Table 1. Surviving methods at the end of the range** (alive / denominator of the last segment; number of regenerations in brackets).


| Project | Group | No regeneration | Reset at each release | Reset when 50% broken | Fill when 50% broken |
|---|---|---|---|---|---|
| ta4j | Dev(scope) (ref., not regenerated) | 0/67 (0.0%) | 0/67 (0.0%) | 0/67 (0.0%) | 0/67 (0.0%) |
|  | KTester | (excluded) | (excluded) | (excluded) | (excluded) |
|  | Ouédraogo | 0/98 (0.0%) | 26/26 (100.0%) [2 regen] | 14/14 (100.0%) [1 regen] | 14/14 (100.0%) [1 regen] |
| json-schema-validator | Dev(scope) (ref., not regenerated) | 0/153 (0.0%) | 0/153 (0.0%) | 0/153 (0.0%) | 0/153 (0.0%) |
|  | KTester | 0/131 (0.0%) | 0/123 (0.0%) [2 regen] | 0/71 (0.0%) [1 regen] | 0/58 (0.0%) [1 regen] |
|  | Ouédraogo | 0/41 (0.0%) | 0/10 (0.0%) [2 regen] | 0/29 (0.0%) [1 regen] | 0/35 (0.0%) [1 regen] |
| dnsjava | Dev(scope) (ref., not regenerated) | 119/353 (33.7%) | 119/353 (33.7%) | 119/353 (33.7%) | 119/353 (33.7%) |
|  | KTester | 206/225 (91.6%) | 120/120 (100.0%) [2 regen] | 206/225 (91.6%) | 206/225 (91.6%) |
|  | Ouédraogo | 74/75 (98.7%) | 79/79 (100.0%) [2 regen] | 74/75 (98.7%) | 74/75 (98.7%) |

**Table 2. Survival, no regeneration** (surviving methods / segment denominator; bold rows open a new segment).

| # | Commit | Date | Change | Dev(scope) (ref.) | KTester | Ouédraogo |
|---|---|---|---|---|---|---|
| **ta4j** (530 commits) | | | |  |  |  |
| 0 | 897a114 | 2024-05-17 | t = 897a114 | 67/67 (100.0%) | (excluded) | 98/98 (100.0%) |
| 10 | 7e296de | 2024-07-06 | Merge pull request #1109 from nimo23/add--bigDe… | 67/67 (100.0%) | (excluded) | 82/98 (83.7%) |
| 21 | 632b307 | 2024-09-30 | Merge pull request #1147 from sgflt/introduce-n… | 0/67 (0.0%) | (excluded) | 0/98 (0.0%) |
| 530 | 7adc45b | 2026-10-22 | (end of range) | 0/67 (0.0%) | (excluded) | 0/98 (0.0%) |
| **json-schema-validator** (229 commits) | | | |  |  |  |
| 0 | 0c14a7a | 2023-12-15 | t = 0c14a7a | 153/153 (100.0%) | 131/131 (100.0%) | 41/41 (100.0%) |
| 2 | 48ca8c3 | 2024-01-18 | Refactor validation message generation (#910) | 146/153 (95.4%) | 131/131 (100.0%) | 40/41 (97.6%) |
| 4 | 5a94df7 | 2024-01-19 | Refactor of paths (#915) | 125/153 (81.7%) | 82/131 (62.6%) | 13/41 (31.7%) |
| 10 | 7f1ec11 | 2024-01-26 | Support Draft 2020-12 and refactor schema retri… | 72/153 (47.1%) | 51/131 (38.9%) | 0/41 (0.0%) |
| 18 | 85d642b | 2024-02-04 | Fix issues with hierarchy output report (#947) | 68/153 (44.4%) | 49/131 (37.4%) | 0/41 (0.0%) |
| 55 | ea64777 | 2024-04-22 | upgrade jackson to 2.17.0 | 59/153 (38.6%) | 48/131 (36.6%) | 0/41 (0.0%) |
| 148 | db71780 | 2025-09-20 | Add benchmark for performance regression testin… | 46/153 (30.1%) | 48/131 (36.6%) | 0/41 (0.0%) |
| 150 | 02a6e06 | 2025-09-27 | Refactor set to list in execution context (#119… | 7/153 (4.6%) | 30/131 (22.9%) | 0/41 (0.0%) |
| 151 | e880dd0 | 2025-10-08 | Main refactor for 2.x (#1198) | 0/153 (0.0%) | 0/131 (0.0%) | 0/41 (0.0%) |
| 229 | 1b57102 | 2026-10-23 | (end of range) | 0/153 (0.0%) | 0/131 (0.0%) | 0/41 (0.0%) |
| **dnsjava** (178 commits) | | | |  |  |  |
| 0 | 4c51bf2 | 2023-11-11 | t = 4c51bf2 | 353/353 (100.0%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 1 | 5da2770 | 2023-11-11 | Pluggable I/O for SimpleResolver | 331/353 (93.8%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 5 | 85ff1c9 | 2024-02-18 | Pass the executor from the initial to all subse… | 127/353 (36.0%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 15 | 2073a0c | 2024-06-02 | CVE-2024-25638: Message normalization | 122/353 (34.6%) | 212/225 (94.2%) | 74/75 (98.7%) |
| 56 | c32a9b9 | 2024-12-15 | Add support for new TSIG algorithms from RFC 89… | 120/353 (34.0%) | 209/225 (92.9%) | 74/75 (98.7%) |
| 146 | a943d80 | 2026-10-25 | Message: validate section in addRecord | 120/353 (34.0%) | 206/225 (91.6%) | 74/75 (98.7%) |
| 178 | 87409c5 | 2026-10-29 | (end of range) | 119/353 (33.7%) | 206/225 (91.6%) | 74/75 (98.7%) |

**Table 3. Changed-line coverage, no regeneration** (covered: changed scope lines executed by the group's surviving methods; broken: methods of the group lost at that commit).

| # | Commit | Change | Changed lines | Dev(scope) covered | KTester covered | Ouédraogo covered | Dev(scope) broken | KTester broken | Ouédraogo broken |
|---|---|---|---|---|---|---|---|---|---|
| **ta4j** | | |  |  |  |  |  |  |
| 21 | 632b307 | Merge pull request #1147 from sgflt/int… | 11 | 0 (0.0%) | (excluded) | 0 (0.0%) | 67 | (excluded) | 82 |
| 69 | 759c392 | More on drawdown (#1321) | 19 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 159 | 1c822d8 | Add option to CashFlow for realized equ… | 35 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 162 | 9e0f63a | Sharpe ratio (#1406) | 54 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 186 | b112d34 | Live Trading Record, Equity Curve Contr… | 82 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 361 | 3970525 | Merge branch 'master' into feature/deli… | 60 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 364 | c636055 | Merge branch 'master' into feature/deli… | 43 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 419 | 30f9f95 | Merge pull request #1510 from ta4j/feat… | 71 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 420 | 0534f32 | Merge pull request #1555 from ta4j/feat… | 77 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 492 | 6447d77 | Merge pull request #1613 from ta4j/refa… | 148 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 493 | a4771c9 | Merge pull request #1618 from ta4j/bugf… | 103 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 530 | 7adc45b | Merge pull request #1593 from ta4j/feat… | 13 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| | other 13 | 1-9 changed lines each | 47 | 1 (2.1%) | (excluded) | 1 (2.1%) | 0 | (excluded) | 0 |
| **json-schema-validator** | | |  |  |  |  |  |  |
| 2 | 48ca8c3 | Refactor validation message generation … | 43 | 26 (60.5%) | 7 (16.3%) | 1 (2.3%) | 7 | 0 | 1 |
| 4 | 5a94df7 | Refactor of paths (#915) | 178 | 161 (90.4%) | 98 (55.1%) | 30 (16.9%) | 21 | 49 | 27 |
| 5 | 9b73d10 | Support schema resource (#922) | 139 | 125 (89.9%) | 63 (45.3%) | 65 (46.8%) | 2 | 1 | 1 |
| 10 | 7f1ec11 | Support Draft 2020-12 and refactor sche… | 279 | 182 (65.2%) | 110 (39.4%) | 0 (0.0%) | 48 | 30 | 12 |
| 13 | 9c95c06 | Add annotation support refactor keyword… | 180 | 130 (72.2%) | 64 (35.6%) | 0 (0.0%) | 0 | 0 | 0 |
| 15 | 2795d79 | Fix for required annotations for evalua… | 10 | 8 (80.0%) | 8 (80.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 18 | 85d642b | Fix issues with hierarchy output report… | 22 | 20 (90.9%) | 11 (50.0%) | 0 (0.0%) | 4 | 2 | 0 |
| 30 | 1f60740 | Reduce memory usage and improve perform… | 33 | 22 (66.7%) | 12 (36.4%) | 0 (0.0%) | 0 | 0 | 0 |
| 32 | 2879ca3 | Fix validation messages (#969) | 9 | 8 (88.9%) | 0 (0.0%) | 0 (0.0%) | 1 | 0 | 0 |
| 33 | bd085ed | Fixes for discriminator (#971) | 34 | 8 (23.5%) | 1 (2.9%) | 0 (0.0%) | 0 | 0 | 0 |
| 34 | 53a3402 | Support GraalVM and refactor (#972) | 23 | 13 (56.5%) | 13 (56.5%) | 0 (0.0%) | 0 | 0 | 0 |
| 38 | 95911ba | Support custom vocabularies and unknown… | 45 | 33 (73.3%) | 27 (60.0%) | 0 (0.0%) | 2 | 1 | 0 |
| 40 | 0f983b0 | Refactor walk (#986) | 13 | 10 (76.9%) | 3 (23.1%) | 0 (0.0%) | 1 | 0 | 0 |
| 41 | 6f44455 | Explicitly handle if the discriminator … | 25 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 63 | 9777fd3 | Validate allOf, oneOf and anyOf contain… | 14 | 2 (14.3%) | 1 (7.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 65 | 880329b | Allow tracking of json node location in… | 27 | 16 (59.3%) | 12 (44.4%) | 0 (0.0%) | 0 | 0 | 0 |
| 72 | 028710e | Optimize logging by creating a debug fl… | 12 | 3 (25.0%) | 2 (16.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 76 | 4e5a808 | Refactor to make more fields final (#10… | 29 | 20 (69.0%) | 20 (69.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 78 | 9113b4f | Deprecate validateAndCollect in favor o… | 23 | 4 (17.4%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 82 | 4f3345b | Add builder for `SchemaValidatorsConfig… | 37 | 18 (48.6%) | 13 (35.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 99 | a9e8a49 | Optimize hasAdjacentKeywordInEvaluation… | 14 | 12 (85.7%) | 12 (85.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 117 | 7c4944b | Clean up code (#1130) | 10 | 5 (50.0%) | 4 (40.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 149 | 19b2207 | Refactor and remove validation message … | 76 | 43 (56.6%) | 43 (56.6%) | 0 (0.0%) | 0 | 0 | 0 |
| 150 | 02a6e06 | Refactor set to list in execution conte… | 70 | 11 (15.7%) | 5 (7.1%) | 0 (0.0%) | 39 | 18 | 0 |
| | other 14 | 1-9 changed lines each | 54 | 35 (64.8%) | 24 (44.4%) | 0 (0.0%) | 0 | 0 | 0 |
| **dnsjava** | | |  |  |  |  |  |  |
| 11 | e6302ef | Remove mix of how SetResponse is constr… | 10 | 5 (50.0%) | 0 (0.0%) | 7 (70.0%) | 0 | 0 | 0 |
| 15 | 2073a0c | CVE-2024-25638: Message normalization | 198 | 110 (55.6%) | 17 (8.6%) | 17 (8.6%) | 5 | 13 | 1 |
| 30 | 2f9b45b | Replace synchronization in Zone with lo… | 193 | 52 (26.9%) | 54 (28.0%) | 132 (68.4%) | 0 | 0 | 0 |
| 56 | c32a9b9 | Add support for new TSIG algorithms fro… | 48 | 27 (56.2%) | 42 (87.5%) | 0 (0.0%) | 0 | 3 | 0 |
| 77 | 60e75e5 | Fix section size in header after messag… | 21 | 19 (90.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 110 | 06a0599 | TSIG: use original message ID in signat… | 23 | 1 (4.3%) | 6 (26.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 123 | dfcba5d | Include Other Data in BADTIME response … | 10 | 0 (0.0%) | 5 (50.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 146 | a943d80 | Message: validate section in addRecord | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 3 | 0 |
| 164 | 52399e3 | Don't synthesize wildcard below an exis… | 13 | 12 (92.3%) | 0 (0.0%) | 7 (53.8%) | 0 | 0 | 0 |
| 165 | dc1527f | zone: return NODATA for ENT and wildcar… | 33 | 19 (57.6%) | 14 (42.4%) | 27 (81.8%) | 1 | 0 | 0 |
| | other 18 | 1-8 changed lines each | 51 | 31 (60.8%) | 23 (45.1%) | 12 (23.5%) | 0 | 0 | 0 |

**Table 4. Survival, reset at each release** (surviving methods / segment denominator; bold rows open a new segment).

| # | Commit | Date | Change | Dev(scope) (ref.) | KTester | Ouédraogo |
|---|---|---|---|---|---|---|
| **ta4j** (530 commits) | | | |  |  |  |
| 0 | 897a114 | 2024-05-17 | t = 897a114 | 67/67 (100.0%) | (excluded) | 98/98 (100.0%) |
| 10 | 7e296de | 2024-07-06 | Merge pull request #1109 from nimo23/add--bigDe… | 67/67 (100.0%) | (excluded) | 82/98 (83.7%) |
| 21 | 632b307 | 2024-09-30 | Merge pull request #1147 from sgflt/introduce-n… | 0/67 (0.0%) | (excluded) | 0/98 (0.0%) |
| 155 | 0af7c71 | 2026-01-18 | **Derive the NumFactory from the firs… - regenerate** | 0/67 (0.0%) | (excluded) | **7/7 new** |
| 426 | e869f8b | 2026-07-13 | **Merge pull request #1565 from ta4j/… - regenerate** | 0/67 (0.0%) | (excluded) | **26/26 new** |
| 530 | 7adc45b | 2026-10-22 | (end of range) | 0/67 (0.0%) | (excluded) | 26/26 (100.0%) |
| **json-schema-validator** (229 commits) | | | |  |  |  |
| 0 | 0c14a7a | 2023-12-15 | t = 0c14a7a | 153/153 (100.0%) | 131/131 (100.0%) | 41/41 (100.0%) |
| 2 | 48ca8c3 | 2024-01-18 | Refactor validation message generation (#910) | 146/153 (95.4%) | 131/131 (100.0%) | 40/41 (97.6%) |
| 4 | 5a94df7 | 2024-01-19 | Refactor of paths (#915) | 125/153 (81.7%) | 82/131 (62.6%) | 13/41 (31.7%) |
| 10 | 7f1ec11 | 2024-01-26 | Support Draft 2020-12 and refactor schema retri… | 72/153 (47.1%) | 51/131 (38.9%) | 0/41 (0.0%) |
| 18 | 85d642b | 2024-02-04 | Fix issues with hierarchy output report (#947) | 68/153 (44.4%) | 49/131 (37.4%) | 0/41 (0.0%) |
| 55 | ea64777 | 2024-04-22 | upgrade jackson to 2.17.0 | 59/153 (38.6%) | 48/131 (36.6%) | 0/41 (0.0%) |
| 98 | 11f9af0 | 2024-07-05 | **upgrade to 1.5.0 and update changel… - regenerate** | 59/153 (38.6%) | **269/269 new** | **0/0 new** |
| 146 | 270df62 | 2025-09-13 | **upgrade to 1.5.9 and update changel… - regenerate** | 59/153 (38.6%) | **123/123 new** | **10/10 new** |
| 148 | db71780 | 2025-09-20 | Add benchmark for performance regression testin… | 46/153 (30.1%) | 123/123 (100.0%) | 10/10 (100.0%) |
| 149 | 19b2207 | 2025-09-23 | Refactor and remove validation message handler … | 46/153 (30.1%) | 95/123 (77.2%) | 10/10 (100.0%) |
| 150 | 02a6e06 | 2025-09-27 | Refactor set to list in execution context (#119… | 7/153 (4.6%) | 78/123 (63.4%) | 10/10 (100.0%) |
| 151 | e880dd0 | 2025-10-08 | Main refactor for 2.x (#1198) | 0/153 (0.0%) | 0/123 (0.0%) | 0/10 (0.0%) |
| 229 | 1b57102 | 2026-10-23 | (end of range) | 0/153 (0.0%) | 0/123 (0.0%) | 0/10 (0.0%) |
| **dnsjava** (178 commits) | | | |  |  |  |
| 0 | 4c51bf2 | 2023-11-11 | t = 4c51bf2 | 353/353 (100.0%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 1 | 5da2770 | 2023-11-11 | Pluggable I/O for SimpleResolver | 331/353 (93.8%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 5 | 85ff1c9 | 2024-02-18 | Pass the executor from the initial to all subse… | 127/353 (36.0%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 15 | 2073a0c | 2024-06-02 | CVE-2024-25638: Message normalization | 122/353 (34.6%) | 212/225 (94.2%) | 74/75 (98.7%) |
| 34 | fd1d7c9 | 2024-07-21 | **Release v3.6.0 - regenerate** | 120/353 (34.0%) | **397/397 new** | **90/90 new** |
| 47 | 6c519f2 | 2024-09-12 | Fix compiling on Java 8 | 120/353 (34.0%) | 393/397 (99.0%) | 90/90 (100.0%) |
| 102 | ca25720 | 2026-05-23 | **Release v3.6.5 - regenerate** | 120/353 (34.0%) | **120/120 new** | **79/79 new** |
| 178 | 87409c5 | 2026-10-29 | (end of range) | 119/353 (33.7%) | 120/120 (100.0%) | 79/79 (100.0%) |

**Table 5. Changed-line coverage, reset at each release** (covered: changed scope lines executed by the group's surviving methods; broken: methods of the group lost at that commit).

| # | Commit | Change | Changed lines | Dev(scope) covered | KTester covered | Ouédraogo covered | Dev(scope) broken | KTester broken | Ouédraogo broken |
|---|---|---|---|---|---|---|---|---|---|
| **ta4j** | | |  |  |  |  |  |  |
| 21 | 632b307 | Merge pull request #1147 from sgflt/int… | 11 | 0 (0.0%) | (excluded) | 0 (0.0%) | 67 | (excluded) | 82 |
| 69 | 759c392 | More on drawdown (#1321) | 19 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 159 | 1c822d8 | Add option to CashFlow for realized equ… | 35 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 162 | 9e0f63a | Sharpe ratio (#1406) | 54 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 186 | b112d34 | Live Trading Record, Equity Curve Contr… | 82 | 0 (0.0%) | (excluded) | 2 (2.4%) | 0 | (excluded) | 0 |
| 361 | 3970525 | Merge branch 'master' into feature/deli… | 60 | 0 (0.0%) | (excluded) | 16 (26.7%) | 0 | (excluded) | 0 |
| 364 | c636055 | Merge branch 'master' into feature/deli… | 43 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 419 | 30f9f95 | Merge pull request #1510 from ta4j/feat… | 71 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 420 | 0534f32 | Merge pull request #1555 from ta4j/feat… | 77 | 0 (0.0%) | (excluded) | 12 (15.6%) | 0 | (excluded) | 0 |
| 492 | 6447d77 | Merge pull request #1613 from ta4j/refa… | 148 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 493 | a4771c9 | Merge pull request #1618 from ta4j/bugf… | 103 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 530 | 7adc45b | Merge pull request #1593 from ta4j/feat… | 13 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| | other 13 | 1-9 changed lines each | 47 | 1 (2.1%) | (excluded) | 1 (2.1%) | 0 | (excluded) | 0 |
| **json-schema-validator** | | |  |  |  |  |  |  |
| 2 | 48ca8c3 | Refactor validation message generation … | 43 | 26 (60.5%) | 7 (16.3%) | 1 (2.3%) | 7 | 0 | 1 |
| 4 | 5a94df7 | Refactor of paths (#915) | 178 | 161 (90.4%) | 98 (55.1%) | 30 (16.9%) | 21 | 49 | 27 |
| 5 | 9b73d10 | Support schema resource (#922) | 139 | 125 (89.9%) | 63 (45.3%) | 65 (46.8%) | 2 | 1 | 1 |
| 10 | 7f1ec11 | Support Draft 2020-12 and refactor sche… | 279 | 182 (65.2%) | 110 (39.4%) | 0 (0.0%) | 48 | 30 | 12 |
| 13 | 9c95c06 | Add annotation support refactor keyword… | 180 | 130 (72.2%) | 64 (35.6%) | 0 (0.0%) | 0 | 0 | 0 |
| 15 | 2795d79 | Fix for required annotations for evalua… | 10 | 8 (80.0%) | 8 (80.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 18 | 85d642b | Fix issues with hierarchy output report… | 22 | 20 (90.9%) | 11 (50.0%) | 0 (0.0%) | 4 | 2 | 0 |
| 30 | 1f60740 | Reduce memory usage and improve perform… | 33 | 22 (66.7%) | 12 (36.4%) | 0 (0.0%) | 0 | 0 | 0 |
| 32 | 2879ca3 | Fix validation messages (#969) | 9 | 8 (88.9%) | 0 (0.0%) | 0 (0.0%) | 1 | 0 | 0 |
| 33 | bd085ed | Fixes for discriminator (#971) | 34 | 8 (23.5%) | 1 (2.9%) | 0 (0.0%) | 0 | 0 | 0 |
| 34 | 53a3402 | Support GraalVM and refactor (#972) | 23 | 13 (56.5%) | 13 (56.5%) | 0 (0.0%) | 0 | 0 | 0 |
| 38 | 95911ba | Support custom vocabularies and unknown… | 45 | 33 (73.3%) | 27 (60.0%) | 0 (0.0%) | 2 | 1 | 0 |
| 40 | 0f983b0 | Refactor walk (#986) | 13 | 10 (76.9%) | 3 (23.1%) | 0 (0.0%) | 1 | 0 | 0 |
| 41 | 6f44455 | Explicitly handle if the discriminator … | 25 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 63 | 9777fd3 | Validate allOf, oneOf and anyOf contain… | 14 | 2 (14.3%) | 1 (7.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 65 | 880329b | Allow tracking of json node location in… | 27 | 16 (59.3%) | 12 (44.4%) | 0 (0.0%) | 0 | 0 | 0 |
| 72 | 028710e | Optimize logging by creating a debug fl… | 12 | 3 (25.0%) | 2 (16.7%) | 0 (0.0%) | 0 | 0 | 0 |
| 76 | 4e5a808 | Refactor to make more fields final (#10… | 29 | 20 (69.0%) | 20 (69.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 78 | 9113b4f | Deprecate validateAndCollect in favor o… | 23 | 4 (17.4%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 82 | 4f3345b | Add builder for `SchemaValidatorsConfig… | 37 | 18 (48.6%) | 13 (35.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 99 | a9e8a49 | Optimize hasAdjacentKeywordInEvaluation… | 14 | 12 (85.7%) | 13 (92.9%) | 0 (0.0%) | 0 | 0 | 0 |
| 117 | 7c4944b | Clean up code (#1130) | 10 | 5 (50.0%) | 6 (60.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 149 | 19b2207 | Refactor and remove validation message … | 76 | 43 (56.6%) | 49 (64.5%) | 24 (31.6%) | 0 | 28 | 0 |
| 150 | 02a6e06 | Refactor set to list in execution conte… | 70 | 11 (15.7%) | 0 (0.0%) | 0 (0.0%) | 39 | 17 | 0 |
| | other 14 | 1-9 changed lines each | 54 | 35 (64.8%) | 24 (44.4%) | 0 (0.0%) | 0 | 0 | 0 |
| **dnsjava** | | |  |  |  |  |  |  |
| 11 | e6302ef | Remove mix of how SetResponse is constr… | 10 | 5 (50.0%) | 0 (0.0%) | 7 (70.0%) | 0 | 0 | 0 |
| 15 | 2073a0c | CVE-2024-25638: Message normalization | 198 | 110 (55.6%) | 17 (8.6%) | 17 (8.6%) | 5 | 13 | 1 |
| 30 | 2f9b45b | Replace synchronization in Zone with lo… | 193 | 52 (26.9%) | 54 (28.0%) | 132 (68.4%) | 0 | 0 | 0 |
| 47 | 6c519f2 | Fix compiling on Java 8 | 1 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 4 | 0 |
| 56 | c32a9b9 | Add support for new TSIG algorithms fro… | 48 | 27 (56.2%) | 43 (89.6%) | 27 (56.2%) | 0 | 1 | 0 |
| 77 | 60e75e5 | Fix section size in header after messag… | 21 | 19 (90.5%) | 11 (52.4%) | 14 (66.7%) | 0 | 0 | 0 |
| 110 | 06a0599 | TSIG: use original message ID in signat… | 23 | 1 (4.3%) | 2 (8.7%) | 1 (4.3%) | 0 | 0 | 0 |
| 123 | dfcba5d | Include Other Data in BADTIME response … | 10 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 164 | 52399e3 | Don't synthesize wildcard below an exis… | 13 | 12 (92.3%) | 0 (0.0%) | 7 (53.8%) | 0 | 0 | 0 |
| 165 | dc1527f | zone: return NODATA for ENT and wildcar… | 33 | 19 (57.6%) | 22 (66.7%) | 27 (81.8%) | 1 | 0 | 0 |
| | other 18 | 1-8 changed lines each | 51 | 32 (62.7%) | 21 (41.2%) | 25 (49.0%) | 0 | 0 | 0 |

**Table 6. Survival, reset when 50% broken** (surviving methods / segment denominator; bold rows open a new segment).

| # | Commit | Date | Change | Dev(scope) (ref.) | KTester | Ouédraogo |
|---|---|---|---|---|---|---|
| **ta4j** (530 commits) | | | |  |  |  |
| 0 | 897a114 | 2024-05-17 | t = 897a114 | 67/67 (100.0%) | (excluded) | 98/98 (100.0%) |
| 10 | 7e296de | 2024-07-06 | Merge pull request #1109 from nimo23/add--bigDe… | 67/67 (100.0%) | (excluded) | 82/98 (83.7%) |
| 21 | 632b307 | 2024-09-30 | Merge pull request #1147 from sgflt/introduce-n… | 0/67 (0.0%) | (excluded) | 0/98 (0.0%) |
| 21 | 632b307 | 2024-09-30 | **Merge pull request #1147 from sgflt… - regenerate** | 0/67 (0.0%) | (excluded) | **14/14 new** |
| 530 | 7adc45b | 2026-10-22 | (end of range) | 0/67 (0.0%) | (excluded) | 14/14 (100.0%) |
| **json-schema-validator** (229 commits) | | | |  |  |  |
| 0 | 0c14a7a | 2023-12-15 | t = 0c14a7a | 153/153 (100.0%) | 131/131 (100.0%) | 41/41 (100.0%) |
| 2 | 48ca8c3 | 2024-01-18 | Refactor validation message generation (#910) | 146/153 (95.4%) | 131/131 (100.0%) | 40/41 (97.6%) |
| 4 | 5a94df7 | 2024-01-19 | Refactor of paths (#915) | 125/153 (81.7%) | 82/131 (62.6%) | 13/41 (31.7%) |
| 4 | 5a94df7 | 2024-01-19 | **Refactor of paths (#915) - regenerate** | 125/153 (81.7%) | 82/131 (62.6%) | **29/29 new** |
| 10 | 7f1ec11 | 2024-01-26 | Support Draft 2020-12 and refactor schema retri… | 72/153 (47.1%) | 51/131 (38.9%) | 0/29 (0.0%) |
| 10 | 7f1ec11 | 2024-01-26 | **Support Draft 2020-12 and refactor … - regenerate** | 72/153 (47.1%) | **71/71 new** | 0/29 (0.0%) |
| 11 | b76c9f4 | 2024-01-26 | fixes #933 update javadoc and a test case (#934) | 72/153 (47.1%) | 0/71 (0.0%) | 0/29 (0.0%) |
| 18 | 85d642b | 2024-02-04 | Fix issues with hierarchy output report (#947) | 68/153 (44.4%) | 0/71 (0.0%) | 0/29 (0.0%) |
| 55 | ea64777 | 2024-04-22 | upgrade jackson to 2.17.0 | 59/153 (38.6%) | 0/71 (0.0%) | 0/29 (0.0%) |
| 148 | db71780 | 2025-09-20 | Add benchmark for performance regression testin… | 46/153 (30.1%) | 0/71 (0.0%) | 0/29 (0.0%) |
| 150 | 02a6e06 | 2025-09-27 | Refactor set to list in execution context (#119… | 7/153 (4.6%) | 0/71 (0.0%) | 0/29 (0.0%) |
| 151 | e880dd0 | 2025-10-08 | Main refactor for 2.x (#1198) | 0/153 (0.0%) | 0/71 (0.0%) | 0/29 (0.0%) |
| 229 | 1b57102 | 2026-10-23 | (end of range) | 0/153 (0.0%) | 0/71 (0.0%) | 0/29 (0.0%) |
| **dnsjava** (178 commits) | | | |  |  |  |
| 0 | 4c51bf2 | 2023-11-11 | t = 4c51bf2 | 353/353 (100.0%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 1 | 5da2770 | 2023-11-11 | Pluggable I/O for SimpleResolver | 331/353 (93.8%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 5 | 85ff1c9 | 2024-02-18 | Pass the executor from the initial to all subse… | 127/353 (36.0%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 15 | 2073a0c | 2024-06-02 | CVE-2024-25638: Message normalization | 122/353 (34.6%) | 212/225 (94.2%) | 74/75 (98.7%) |
| 56 | c32a9b9 | 2024-12-15 | Add support for new TSIG algorithms from RFC 89… | 120/353 (34.0%) | 209/225 (92.9%) | 74/75 (98.7%) |
| 146 | a943d80 | 2026-10-25 | Message: validate section in addRecord | 120/353 (34.0%) | 206/225 (91.6%) | 74/75 (98.7%) |
| 178 | 87409c5 | 2026-10-29 | (end of range) | 119/353 (33.7%) | 206/225 (91.6%) | 74/75 (98.7%) |

**Table 7. Changed-line coverage, reset when 50% broken** (covered: changed scope lines executed by the group's surviving methods; broken: methods of the group lost at that commit).

| # | Commit | Change | Changed lines | Dev(scope) covered | KTester covered | Ouédraogo covered | Dev(scope) broken | KTester broken | Ouédraogo broken |
|---|---|---|---|---|---|---|---|---|---|
| **ta4j** | | |  |  |  |  |  |  |
| 21 | 632b307 | **Merge pull request #1147 from sgf… - regenerate** | 11 | 0 (0.0%) | (excluded) | n/a | 67 | (excluded) | 82 |
| 69 | 759c392 | More on drawdown (#1321) | 19 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 159 | 1c822d8 | Add option to CashFlow for realized equ… | 35 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 162 | 9e0f63a | Sharpe ratio (#1406) | 54 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 186 | b112d34 | Live Trading Record, Equity Curve Contr… | 82 | 0 (0.0%) | (excluded) | 2 (2.4%) | 0 | (excluded) | 0 |
| 361 | 3970525 | Merge branch 'master' into feature/deli… | 60 | 0 (0.0%) | (excluded) | 16 (26.7%) | 0 | (excluded) | 0 |
| 364 | c636055 | Merge branch 'master' into feature/deli… | 43 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 419 | 30f9f95 | Merge pull request #1510 from ta4j/feat… | 71 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 420 | 0534f32 | Merge pull request #1555 from ta4j/feat… | 77 | 0 (0.0%) | (excluded) | 20 (26.0%) | 0 | (excluded) | 0 |
| 492 | 6447d77 | Merge pull request #1613 from ta4j/refa… | 148 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 493 | a4771c9 | Merge pull request #1618 from ta4j/bugf… | 103 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 530 | 7adc45b | Merge pull request #1593 from ta4j/feat… | 13 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| | other 13 | 1-9 changed lines each | 47 | 1 (2.1%) | (excluded) | 2 (4.3%) | 0 | (excluded) | 0 |
| | | coverage unavailable at 1 commits: 21 | |  |  |  |  |  |  |
| **json-schema-validator** | | |  |  |  |  |  |  |
| 2 | 48ca8c3 | Refactor validation message generation … | 43 | 26 (60.5%) | 7 (16.3%) | 1 (2.3%) | 7 | 0 | 1 |
| 4 | 5a94df7 | **Refactor of paths (#915) - regenerate** | 178 | 161 (90.4%) | 98 (55.1%) | n/a | 21 | 49 | 27 |
| 5 | 9b73d10 | Support schema resource (#922) | 139 | 125 (89.9%) | 63 (45.3%) | 38 (27.3%) | 2 | 1 | 2 |
| 10 | 7f1ec11 | **Support Draft 2020-12 and refacto… - regenerate** | 279 | 182 (65.2%) | n/a | 0 (0.0%) | 48 | 30 | 27 |
| 13 | 9c95c06 | Add annotation support refactor keyword… | 180 | 130 (72.2%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 15 | 2795d79 | Fix for required annotations for evalua… | 10 | 8 (80.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 18 | 85d642b | Fix issues with hierarchy output report… | 22 | 20 (90.9%) | 0 (0.0%) | 0 (0.0%) | 4 | 0 | 0 |
| 30 | 1f60740 | Reduce memory usage and improve perform… | 33 | 22 (66.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 32 | 2879ca3 | Fix validation messages (#969) | 9 | 8 (88.9%) | 0 (0.0%) | 0 (0.0%) | 1 | 0 | 0 |
| 33 | bd085ed | Fixes for discriminator (#971) | 34 | 8 (23.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 34 | 53a3402 | Support GraalVM and refactor (#972) | 23 | 13 (56.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 38 | 95911ba | Support custom vocabularies and unknown… | 45 | 33 (73.3%) | 0 (0.0%) | 0 (0.0%) | 2 | 0 | 0 |
| 40 | 0f983b0 | Refactor walk (#986) | 13 | 10 (76.9%) | 0 (0.0%) | 0 (0.0%) | 1 | 0 | 0 |
| 41 | 6f44455 | Explicitly handle if the discriminator … | 25 | 0 (0.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 63 | 9777fd3 | Validate allOf, oneOf and anyOf contain… | 14 | 2 (14.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 65 | 880329b | Allow tracking of json node location in… | 27 | 16 (59.3%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 72 | 028710e | Optimize logging by creating a debug fl… | 12 | 3 (25.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 76 | 4e5a808 | Refactor to make more fields final (#10… | 29 | 20 (69.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 78 | 9113b4f | Deprecate validateAndCollect in favor o… | 23 | 4 (17.4%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 82 | 4f3345b | Add builder for `SchemaValidatorsConfig… | 37 | 18 (48.6%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 99 | a9e8a49 | Optimize hasAdjacentKeywordInEvaluation… | 14 | 12 (85.7%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 117 | 7c4944b | Clean up code (#1130) | 10 | 5 (50.0%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 149 | 19b2207 | Refactor and remove validation message … | 76 | 43 (56.6%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 150 | 02a6e06 | Refactor set to list in execution conte… | 70 | 11 (15.7%) | 0 (0.0%) | 0 (0.0%) | 39 | 0 | 0 |
| | other 14 | 1-9 changed lines each | 54 | 35 (64.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| | | coverage unavailable at 2 commits: 4, 10 | |  |  |  |  |  |  |
| **dnsjava** | | |  |  |  |  |  |  |
| 11 | e6302ef | Remove mix of how SetResponse is constr… | 10 | 5 (50.0%) | 0 (0.0%) | 7 (70.0%) | 0 | 0 | 0 |
| 15 | 2073a0c | CVE-2024-25638: Message normalization | 198 | 110 (55.6%) | 17 (8.6%) | 17 (8.6%) | 5 | 13 | 1 |
| 30 | 2f9b45b | Replace synchronization in Zone with lo… | 193 | 52 (26.9%) | 54 (28.0%) | 132 (68.4%) | 0 | 0 | 0 |
| 56 | c32a9b9 | Add support for new TSIG algorithms fro… | 48 | 27 (56.2%) | 42 (87.5%) | 0 (0.0%) | 0 | 3 | 0 |
| 77 | 60e75e5 | Fix section size in header after messag… | 21 | 19 (90.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 110 | 06a0599 | TSIG: use original message ID in signat… | 23 | 1 (4.3%) | 6 (26.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 123 | dfcba5d | Include Other Data in BADTIME response … | 10 | 0 (0.0%) | 5 (50.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 146 | a943d80 | Message: validate section in addRecord | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 3 | 0 |
| 164 | 52399e3 | Don't synthesize wildcard below an exis… | 13 | 12 (92.3%) | 0 (0.0%) | 7 (53.8%) | 0 | 0 | 0 |
| 165 | dc1527f | zone: return NODATA for ENT and wildcar… | 33 | 19 (57.6%) | 14 (42.4%) | 27 (81.8%) | 1 | 0 | 0 |
| | other 18 | 1-8 changed lines each | 51 | 31 (60.8%) | 23 (45.1%) | 12 (23.5%) | 0 | 0 | 0 |

**Table 8. Survival, fill when 50% broken** (surviving methods / segment denominator; bold rows open a new segment).

| # | Commit | Date | Change | Dev(scope) (ref.) | KTester | Ouédraogo |
|---|---|---|---|---|---|---|
| **ta4j** (530 commits) | | | |  |  |  |
| 0 | 897a114 | 2024-05-17 | t = 897a114 | 67/67 (100.0%) | (excluded) | 98/98 (100.0%) |
| 10 | 7e296de | 2024-07-06 | Merge pull request #1109 from nimo23/add--bigDe… | 67/67 (100.0%) | (excluded) | 82/98 (83.7%) |
| 21 | 632b307 | 2024-09-30 | Merge pull request #1147 from sgflt/introduce-n… | 0/67 (0.0%) | (excluded) | 0/98 (0.0%) |
| 21 | 632b307 | 2024-09-30 | **Merge pull request #1147 from sgflt… - regenerate** | 0/67 (0.0%) | (excluded) | **14/14 new (carried 0 + new 14)** |
| 530 | 7adc45b | 2026-10-22 | (end of range) | 0/67 (0.0%) | (excluded) | 14/14 (100.0%) |
| **json-schema-validator** (229 commits) | | | |  |  |  |
| 0 | 0c14a7a | 2023-12-15 | t = 0c14a7a | 153/153 (100.0%) | 131/131 (100.0%) | 41/41 (100.0%) |
| 2 | 48ca8c3 | 2024-01-18 | Refactor validation message generation (#910) | 146/153 (95.4%) | 131/131 (100.0%) | 40/41 (97.6%) |
| 4 | 5a94df7 | 2024-01-19 | Refactor of paths (#915) | 125/153 (81.7%) | 82/131 (62.6%) | 13/41 (31.7%) |
| 4 | 5a94df7 | 2024-01-19 | **Refactor of paths (#915) - regenerate** | 125/153 (81.7%) | 82/131 (62.6%) | **35/35 new (carried 13 + new 22)** |
| 5 | 9b73d10 | 2024-01-19 | Support schema resource (#922) | 123/153 (80.4%) | 81/131 (61.8%) | 32/35 (91.4%) |
| 10 | 7f1ec11 | 2024-01-26 | Support Draft 2020-12 and refactor schema retri… | 72/153 (47.1%) | 51/131 (38.9%) | 0/35 (0.0%) |
| 10 | 7f1ec11 | 2024-01-26 | **Support Draft 2020-12 and refactor … - regenerate** | 72/153 (47.1%) | **58/58 new (carried 51 + new 7)** | 0/35 (0.0%) |
| 11 | b76c9f4 | 2024-01-26 | fixes #933 update javadoc and a test case (#934) | 72/153 (47.1%) | 51/58 (87.9%) | 0/35 (0.0%) |
| 18 | 85d642b | 2024-02-04 | Fix issues with hierarchy output report (#947) | 68/153 (44.4%) | 49/58 (84.5%) | 0/35 (0.0%) |
| 55 | ea64777 | 2024-04-22 | upgrade jackson to 2.17.0 | 59/153 (38.6%) | 48/58 (82.8%) | 0/35 (0.0%) |
| 148 | db71780 | 2025-09-20 | Add benchmark for performance regression testin… | 46/153 (30.1%) | 48/58 (82.8%) | 0/35 (0.0%) |
| 150 | 02a6e06 | 2025-09-27 | Refactor set to list in execution context (#119… | 7/153 (4.6%) | 30/58 (51.7%) | 0/35 (0.0%) |
| 151 | e880dd0 | 2025-10-08 | Main refactor for 2.x (#1198) | 0/153 (0.0%) | 0/58 (0.0%) | 0/35 (0.0%) |
| 229 | 1b57102 | 2026-10-23 | (end of range) | 0/153 (0.0%) | 0/58 (0.0%) | 0/35 (0.0%) |
| **dnsjava** (178 commits) | | | |  |  |  |
| 0 | 4c51bf2 | 2023-11-11 | t = 4c51bf2 | 353/353 (100.0%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 1 | 5da2770 | 2023-11-11 | Pluggable I/O for SimpleResolver | 331/353 (93.8%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 5 | 85ff1c9 | 2024-02-18 | Pass the executor from the initial to all subse… | 127/353 (36.0%) | 225/225 (100.0%) | 75/75 (100.0%) |
| 15 | 2073a0c | 2024-06-02 | CVE-2024-25638: Message normalization | 122/353 (34.6%) | 212/225 (94.2%) | 74/75 (98.7%) |
| 56 | c32a9b9 | 2024-12-15 | Add support for new TSIG algorithms from RFC 89… | 120/353 (34.0%) | 209/225 (92.9%) | 74/75 (98.7%) |
| 146 | a943d80 | 2026-10-25 | Message: validate section in addRecord | 120/353 (34.0%) | 206/225 (91.6%) | 74/75 (98.7%) |
| 178 | 87409c5 | 2026-10-29 | (end of range) | 119/353 (33.7%) | 206/225 (91.6%) | 74/75 (98.7%) |

**Table 9. Changed-line coverage, fill when 50% broken** (covered: changed scope lines executed by the group's surviving methods; broken: methods of the group lost at that commit).

| # | Commit | Change | Changed lines | Dev(scope) covered | KTester covered | Ouédraogo covered | Dev(scope) broken | KTester broken | Ouédraogo broken |
|---|---|---|---|---|---|---|---|---|---|
| **ta4j** | | |  |  |  |  |  |  |
| 21 | 632b307 | **Merge pull request #1147 from sgf… - regenerate** | 11 | 0 (0.0%) | (excluded) | n/a | 67 | (excluded) | 82 |
| 69 | 759c392 | More on drawdown (#1321) | 19 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 159 | 1c822d8 | Add option to CashFlow for realized equ… | 35 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 162 | 9e0f63a | Sharpe ratio (#1406) | 54 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 186 | b112d34 | Live Trading Record, Equity Curve Contr… | 82 | 0 (0.0%) | (excluded) | 2 (2.4%) | 0 | (excluded) | 0 |
| 361 | 3970525 | Merge branch 'master' into feature/deli… | 60 | 0 (0.0%) | (excluded) | 16 (26.7%) | 0 | (excluded) | 0 |
| 364 | c636055 | Merge branch 'master' into feature/deli… | 43 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 419 | 30f9f95 | Merge pull request #1510 from ta4j/feat… | 71 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 420 | 0534f32 | Merge pull request #1555 from ta4j/feat… | 77 | 0 (0.0%) | (excluded) | 20 (26.0%) | 0 | (excluded) | 0 |
| 492 | 6447d77 | Merge pull request #1613 from ta4j/refa… | 148 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 493 | a4771c9 | Merge pull request #1618 from ta4j/bugf… | 103 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| 530 | 7adc45b | Merge pull request #1593 from ta4j/feat… | 13 | 0 (0.0%) | (excluded) | 0 (0.0%) | 0 | (excluded) | 0 |
| | other 13 | 1-9 changed lines each | 47 | 1 (2.1%) | (excluded) | 2 (4.3%) | 0 | (excluded) | 0 |
| | | coverage unavailable at 1 commits: 21 | |  |  |  |  |  |  |
| **json-schema-validator** | | |  |  |  |  |  |  |
| 2 | 48ca8c3 | Refactor validation message generation … | 43 | 26 (60.5%) | 7 (16.3%) | 1 (2.3%) | 7 | 0 | 1 |
| 4 | 5a94df7 | **Refactor of paths (#915) - regenerate** | 178 | 161 (90.4%) | 98 (55.1%) | n/a | 21 | 49 | 27 |
| 5 | 9b73d10 | Support schema resource (#922) | 139 | 125 (89.9%) | 63 (45.3%) | n/a | 2 | 1 | 3 |
| 10 | 7f1ec11 | **Support Draft 2020-12 and refacto… - regenerate** | 279 | 182 (65.2%) | n/a | 0 (0.0%) | 48 | 30 | 32 |
| 13 | 9c95c06 | Add annotation support refactor keyword… | 180 | 130 (72.2%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 15 | 2795d79 | Fix for required annotations for evalua… | 10 | 8 (80.0%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 18 | 85d642b | Fix issues with hierarchy output report… | 22 | 20 (90.9%) | n/a | 0 (0.0%) | 4 | 2 | 0 |
| 30 | 1f60740 | Reduce memory usage and improve perform… | 33 | 22 (66.7%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 32 | 2879ca3 | Fix validation messages (#969) | 9 | 8 (88.9%) | n/a | 0 (0.0%) | 1 | 0 | 0 |
| 33 | bd085ed | Fixes for discriminator (#971) | 34 | 8 (23.5%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 34 | 53a3402 | Support GraalVM and refactor (#972) | 23 | 13 (56.5%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 38 | 95911ba | Support custom vocabularies and unknown… | 45 | 33 (73.3%) | n/a | 0 (0.0%) | 2 | 1 | 0 |
| 40 | 0f983b0 | Refactor walk (#986) | 13 | 10 (76.9%) | n/a | 0 (0.0%) | 1 | 0 | 0 |
| 41 | 6f44455 | Explicitly handle if the discriminator … | 25 | 0 (0.0%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 63 | 9777fd3 | Validate allOf, oneOf and anyOf contain… | 14 | 2 (14.3%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 65 | 880329b | Allow tracking of json node location in… | 27 | 16 (59.3%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 72 | 028710e | Optimize logging by creating a debug fl… | 12 | 3 (25.0%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 76 | 4e5a808 | Refactor to make more fields final (#10… | 29 | 20 (69.0%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 78 | 9113b4f | Deprecate validateAndCollect in favor o… | 23 | 4 (17.4%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 82 | 4f3345b | Add builder for `SchemaValidatorsConfig… | 37 | 18 (48.6%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 99 | a9e8a49 | Optimize hasAdjacentKeywordInEvaluation… | 14 | 12 (85.7%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 117 | 7c4944b | Clean up code (#1130) | 10 | 5 (50.0%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 149 | 19b2207 | Refactor and remove validation message … | 76 | 43 (56.6%) | n/a | 0 (0.0%) | 0 | 0 | 0 |
| 150 | 02a6e06 | Refactor set to list in execution conte… | 70 | 11 (15.7%) | n/a | 0 (0.0%) | 39 | 18 | 0 |
| | other 14 | 1-9 changed lines each | 54 | 35 (64.8%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| | | coverage unavailable at 37 commits: 4, 5, 9, 10, 13, 15, 18, 21, 29, 30, 32, 33… | |  |  |  |  |  |  |
| **dnsjava** | | |  |  |  |  |  |  |
| 11 | e6302ef | Remove mix of how SetResponse is constr… | 10 | 5 (50.0%) | 0 (0.0%) | 7 (70.0%) | 0 | 0 | 0 |
| 15 | 2073a0c | CVE-2024-25638: Message normalization | 198 | 110 (55.6%) | 17 (8.6%) | 17 (8.6%) | 5 | 13 | 1 |
| 30 | 2f9b45b | Replace synchronization in Zone with lo… | 193 | 52 (26.9%) | 54 (28.0%) | 132 (68.4%) | 0 | 0 | 0 |
| 56 | c32a9b9 | Add support for new TSIG algorithms fro… | 48 | 27 (56.2%) | 42 (87.5%) | 0 (0.0%) | 0 | 3 | 0 |
| 77 | 60e75e5 | Fix section size in header after messag… | 21 | 19 (90.5%) | 0 (0.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 110 | 06a0599 | TSIG: use original message ID in signat… | 23 | 1 (4.3%) | 6 (26.1%) | 0 (0.0%) | 0 | 0 | 0 |
| 123 | dfcba5d | Include Other Data in BADTIME response … | 10 | 0 (0.0%) | 5 (50.0%) | 0 (0.0%) | 0 | 0 | 0 |
| 146 | a943d80 | Message: validate section in addRecord | 1 | 1 (100.0%) | 1 (100.0%) | 1 (100.0%) | 0 | 3 | 0 |
| 164 | 52399e3 | Don't synthesize wildcard below an exis… | 13 | 12 (92.3%) | 0 (0.0%) | 7 (53.8%) | 0 | 0 | 0 |
| 165 | dc1527f | zone: return NODATA for ENT and wildcar… | 33 | 19 (57.6%) | 14 (42.4%) | 27 (81.8%) | 1 | 0 | 0 |
| | other 18 | 1-8 changed lines each | 51 | 31 (60.8%) | 23 (45.1%) | 12 (23.5%) | 0 | 0 | 0 |

## Observations

1. **A release reset restores a set that then follows the same curve as the frozen one; what it adds is the commits between the release and the next breaking change.**
   - ta4j: the methods regenerated at 0.22.0 (7) and 0.23.0 (26) survive to the end of the range; the only scope changes they execute are at commits 186, 361 and 420 (Table 5).
   - json-schema-validator: the 269 KTester methods regenerated at 1.5.0 survive to 1.5.9; the 123 regenerated at 1.5.9 are all lost at the 2.x rewrite five commits later. The large changes to the scope classes (commits 2 to 41) come before the first release and are reached by neither reset.
   - dnsjava: the frozen sets rarely break, and the reset sets execute the same changes as the frozen ones at most commits (Tables 3 and 5).
2. **The 50% trigger fires at the first large refactoring, and there a fill is almost a reset.**
   - ta4j: the trigger is commit 21, where every Ouédraogo method breaks; the fill carries nothing and equals the reset (14 new methods).
   - json-schema-validator: the Ouédraogo trigger is commit 4 (fill: 13 carried + 22 new) and the KTester trigger is commit 10 (fill: 51 carried + 7 new); both regenerated sets are lost by commit 10 (Ouédraogo) and at the 2.x rewrite (KTester).
   - dnsjava: no group loses half of its methods, so the two policies never act.
3. **The number of passing methods a regeneration produces differs by up to ten times between commits of the same project.**
   - KTester: 71, 269 and 123 at json-schema-validator commits 10, 98 and 146; 397 and 120 at dnsjava commits 34 and 102.
   - Ouédraogo: 29, 0 and 10 at json-schema-validator commits 4, 98 and 146 (at 1.5.0 none of its 24 files compiled); 14, 7 and 26 at ta4j commits 21, 155 and 426.
   - The size of the restored set depends on this yield at the regeneration commit more than on the policy.
4. **On dnsjava the developer reference line falls at one commit.** Dev(scope) loses 204 of 353 methods at commit 5, all of them in the `dnssec` test package (189 assertion failures, 15 runtime errors); the two LLM groups lose nothing there. The LLM groups on this project are unit tests of `Message`, `Zone` and `TSIG`, whose public API changes little over the range.

## Limitations

- Each regeneration is one generation run; the yields in observation 3 are single draws. KTester used three repair rounds for every regeneration and for dnsjava at *t*; the json-schema-validator set at *t* used five.
- The 50% policies trigger once per group, and the trigger commit is read from the no-regeneration curve; a second trigger inside a regenerated segment is not modelled.
- ta4j has no KTester group: the generator produced 14 passing methods at *t*, and its environment cannot build the project after commit 21 (Java 21 and later), so it could not regenerate there.
- The second release reset is a patch release on json-schema-validator (1.5.9; every scope class is renamed at 2.0.0) and on dnsjava (v3.6.5-1; v3.7.0 is six commits before the end of the range). dnsjava's *t* is the `master` commit of the release day rather than the tag, and 133 of its developer test cases that could not be mapped to a method are excluded from every developer count.
- Dev(scope) on dnsjava is selected by coverage and consists largely of DNSSEC validation tests; its curve is a reference, not a comparison group.
- A lost method is classified as compile failure, assertion failure or runtime error from the recorded message; flaky reruns and the distinction between assertion failures and other exceptions are not applied.

## Repository

Branches `experiment/2026-10-week5-<project>-regen-policies` in `garibong-labs/ta4j`, `Hamjoon/json-schema-validator` and `garibong-labs/dnsjava`, directory `experiments/regen-policies/`. The no-regeneration runs the policies are evaluated against: ta4j and dnsjava `experiments/commit-survival/`, json-schema-validator `experiments/commit-survival-run2/`.

- Plan and pools: `results/policy/plan.md`, `results/policy/pools/<group>/<index>/` (generated files, screening funnel, `pool.json`).
- Per policy, `results/policy/<policy>/`: `survival.csv`, `segments.json`, `first-failures.csv`, `coverage-commits.csv`, `coverage-missing.csv`.
- Cross-policy: `results/policy/summary.csv`, `consistency.md`; per-test run records under `results/policy/runs/`.

