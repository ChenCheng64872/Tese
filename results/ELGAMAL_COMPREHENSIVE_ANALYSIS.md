# ElGamal Comprehensive Analysis (Validated)

Date: 2026-07-02
Data source: run_results/analysis_tables (canonical)

## 1. Why this file was corrected

The previous version mixed measured benchmark results with speculative scenario numbers and inconsistent unit statements.
This corrected version keeps only values supported by the current CSV outputs.

## 2. Core benchmark facts (mobile aggregate)

From tradeoff_ranking.csv:
- ElGamal mean total time: 38.4988 ms
- ElGamal mean energy: 0.000192984 J
- ElGamal composite score (alpha=0.5): 0.024198
- ElGamal rank: 2nd overall (out of 10)

Interpretation:
- ElGamal is not fast.
- ElGamal is very energy-efficient.
- Under balanced time+energy scoring, ElGamal remains highly competitive.

## 3. Device-level position

From summary_rankings.csv:
- Lowest-energy on D1: ElGamal (best_energy_J_mean = 0.000173953 J, total_time_ms_mean = 38.5726 ms)
- Lowest-energy on D2: ElGamal (best_energy_J_mean = 0.000212015 J, total_time_ms_mean = 38.4249 ms)

Interpretation:
- The energy advantage is consistent across both tested devices.
- Latency remains high on both devices.

## 4. ElGamal vs AES-GCM (direct validated comparison)

From tradeoff_ranking.csv:
- ElGamal time: 38.4988 ms
- AES-GCM time: 6.3150 ms
- ElGamal energy: 0.000192984 J
- AES-GCM energy: 0.000457949 J

Computed from canonical table:
- Time ratio (ElGamal / AES-GCM): 6.096x slower
- Added latency: +32.184 ms per operation
- Energy efficiency gain (AES-GCM / ElGamal): 2.373x better for ElGamal

Interpretation:
- Choosing ElGamal over AES-GCM means a major latency increase in exchange for strong energy reduction.

## 5. ElGamal vs ChaCha20

From tradeoff_ranking.csv:
- ElGamal vs ChaCha20 time ratio: 4.003x slower
- ElGamal vs ChaCha20 energy efficiency gain: 3.980x better for ElGamal

Interpretation:
- ChaCha20 is much faster.
- ElGamal is much more energy-efficient.

## 6. ElGamal vs RSA-Hybrid

From tradeoff_ranking.csv:
- ElGamal vs RSA-Hybrid time ratio: 1.015x slower (very close)
- ElGamal vs RSA-Hybrid energy efficiency gain: 1.103x better for ElGamal

Interpretation:
- ElGamal and RSA-Hybrid are in a similar latency class.
- ElGamal has a modest but consistent energy advantage.

## 7. Practical decision guidance (data-aligned)

Use ElGamal when:
- Energy budget is a primary constraint.
- Additional ~32 ms per operation versus AES-GCM is acceptable.
- Workload is delay-tolerant.

Avoid ElGamal when:
- Low latency is the main requirement.
- Real-time interaction quality depends on minimal cryptographic delay.

Default balanced option:
- AES-GCM remains the best overall default for general deployment.

## 8. What was removed from the old version

Removed intentionally because not directly validated by current benchmark tables:
- Speculative battery-hour/day/month claims derived from arbitrary duty cycles.
- Utility-cost estimates (kWh/year and USD) not tied to measured workload model.
- Any statements mixing ms-level benchmark timing with second-level latency assumptions.

## 9. References (current workspace files)

- run_results/analysis_tables/tradeoff_ranking.csv
- run_results/analysis_tables/summary_rankings.csv
- FINAL_BENCHMARK_ANALYSIS_REPORT_ENGLISH.md

## 10. Final conclusion

ElGamal is a specialized energy-efficient choice, not a general-speed winner.

Validated evidence in this workspace shows:
- strong energy advantage,
- large latency penalty versus AES-GCM,
- and a clear use case in energy-prioritized, delay-tolerant contexts.