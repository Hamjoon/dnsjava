# Consistency checks

```json
{
  "nonincreasing": true,
  "index_zero_equals_denominators": true,
  "raw_records": 525,
  "expected_raw_records": 525,
  "raw_count_matches": true,
  "missing_records": [],
  "build_fail_indices": [
    23,
    44,
    45
  ],
  "measured_raw_records": 525,
  "derived_raw_records": 0,
  "expected_measured_raw_records": 525,
  "expected_derived_raw_records": 0,
  "skipped_indices": [
    3,
    14,
    21,
    24,
    33,
    40,
    41,
    43,
    46,
    60,
    82,
    88,
    94,
    97,
    100,
    109,
    114,
    120,
    121,
    122,
    176
  ]
}
```

133 unmapped baseline cases excluded from all Dev denominators (dev.unmapped: exclude); see dev/dev-unmapped-excluded.csv.

Coverage: identical executable line sets across all 3 groups at 145 indices. Required indices: 148. Missing measurements: 9 (enumerated in coverage-consistency.json).

## Three-group checks

| Check | Dev (scope) | KTester | Ouédraogo |
|---|---:|---:|---:|
| Fixed denominator | 353 | 225 | 75 |
| Final survivors | 119 | 206 | 74 |
| Survival records | 175 | 175 | 175 |
| Coverage records | 145 | 145 | 145 |
| Matching executable universes | 145 | 145 | 145 |
