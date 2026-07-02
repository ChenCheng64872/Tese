# Final Integrated Benchmark Analysis Report

Date: 2026-07-02

## Abstract

This final report consolidates the complete mobile and PC cryptographic benchmark analysis into a single interpretation-driven academic document. The study evaluates algorithms across execution time, energy consumption, memory behavior, throughput, and cross-platform transferability. Results indicate that AES-GCM is the strongest overall default for production deployment due to superior latency and balanced energy performance. ElGamal is the energy-efficiency specialist, achieving the lowest energy footprint with substantial latency cost. ChaCha20 demonstrates high runtime consistency and superior behavior for very small input sizes. The evidence supports workload-specific algorithm selection rather than a one-size-fits-all strategy.

## 1. Introduction

Cryptographic algorithm selection on mobile systems requires balancing competing objectives: low latency, low energy consumption, predictable performance, and practical scalability across input sizes. This report integrates all completed analyses into one final interpretation.

Research objective:
- Determine which algorithm is most suitable for general mobile deployment.
- Identify specialized options for energy-first and consistency-first scenarios.
- Quantify PC-to-mobile transfer effects.

## 2. Data and Methodology

### 2.1 Data Sources

Canonical datasets were consolidated under run_results:
- Mobile Device 1 raw benchmark files: run_results/device1
- Mobile Device 2 raw benchmark files: run_results/device2
- PC benchmark baseline files: run_results/pc_benchmark
- Aggregated analysis outputs: run_results/analysis_tables

Main tables used:
- [results/run_results/analysis_tables/summary_rankings.csv](results/run_results/analysis_tables/summary_rankings.csv)
- [results/run_results/analysis_tables/tradeoff_ranking.csv](results/run_results/analysis_tables/tradeoff_ranking.csv)
- [results/run_results/analysis_tables/pc_vs_mobile_comparison.csv](results/run_results/analysis_tables/pc_vs_mobile_comparison.csv)
- [results/run_results/analysis_tables/pc_algorithm_ranking.csv](results/run_results/analysis_tables/pc_algorithm_ranking.csv)
- [results/run_results/analysis_tables/aggregated_results.csv](results/run_results/analysis_tables/aggregated_results.csv)

### 2.2 Core Metrics

- Total execution time (ms)
- Energy consumption (J)
- Memory usage (bytes)
- Throughput (KB/ms)
- Composite trade-off score (balanced weighting)

### 2.3 Stability Metric

Coefficient of variation (CV) was used to normalize variability:

$$
CV(\%) = \frac{\sigma}{\mu} \times 100
$$

Interpretation scale:
- CV < 10%: excellent stability
- 10% <= CV < 20%: good stability
- 20% <= CV < 30%: acceptable variability
- CV >= 30%: high variability

## 3. Results

### 3.1 Mobile Speed Leaders by Device

From summary rankings:
- D1 fastest: AES-GCM at 6.3307 ms mean total time
- D2 fastest: AES-GCM at 6.2992 ms mean total time

Interpretation:
- AES-GCM exhibits robust speed leadership across both devices, suggesting strong implementation portability and low device sensitivity in this benchmark context.

### 3.2 Mobile Lowest-Energy Leaders by Device

From summary rankings:
- D1 lowest energy: ElGamal at 0.00017395 J (with 38.5726 ms)
- D2 lowest energy: ElGamal at 0.00021201 J (with 38.4249 ms)

Interpretation:
- ElGamal provides the best energy efficiency but at large latency cost. This confirms a strict speed-energy trade-off rather than a globally superior profile.

### 3.3 Best Algorithm by Input Size

From summary rankings:
- 1024 bytes: ChaCha20 best time (2.8039 ms)
- 2048 bytes: ChaCha20 best time (3.0398 ms)
- 4096 bytes and above: AES-GCM dominates

Interpretation:
- ChaCha20 is advantageous for very small messages.
- AES-GCM scales more favorably as payload size grows and becomes the practical default for mixed real-world traffic.

### 3.4 Composite Trade-off Ranking (Balanced)

From tradeoff_ranking:
1. AES-GCM (score_mean 0.02070)
2. ElGamal (0.02420)
3. RSA-Hybrid (0.02492)
4. ASCON
5. Xoodyak
6. ChaCha20
7. GIFT-COFB
8. AES
9. Grain-128AEAD
10. Elephant

Key paired means for top algorithms:
- AES-GCM: 6.31497 ms, 0.00045795 J
- ElGamal: 38.49876 ms, 0.00019298 J
- RSA-Hybrid: 37.91853 ms, 0.00021286 J

Interpretation:
- AES-GCM wins globally because it combines very low latency with acceptable energy behavior.
- ElGamal ranks second because extreme energy efficiency compensates for poor speed in a balanced score.
- Elephant and Grain-128AEAD are weak choices for latency-sensitive deployment due to high total time.

### 3.5 PC vs Mobile Transfer

From pc_vs_mobile_comparison:
- ElGamal time ratio (mobile/pc): 16.43x
- RSA-Hybrid: 12.56x
- ChaCha20: 3.18x
- AES-GCM: 2.98x
- AES: 1.84x

From pc_algorithm_ranking (PC-only):
1. AES-GCM
2. ChaCha20
3. AES
4. RSA-Hybrid
5. ElGamal

Interpretation:
- Mobile slowdown is highly algorithm-dependent.
- ElGamal and RSA-Hybrid degrade most on mobile relative to PC.
- AES and AES-GCM transfer more consistently across platforms.
- AES-GCM remains first on PC and mobile, reinforcing cross-platform reliability.

## 4. Figure-Based Interpretation

This section links final conclusions to generated figures.

Primary performance figures:
- [results/benchmark_analysis_outputs/plots/time_trends_by_algorithm_device.png](results/benchmark_analysis_outputs/plots/time_trends_by_algorithm_device.png)
- [results/benchmark_analysis_outputs/plots/energy_memory_trends.png](results/benchmark_analysis_outputs/plots/energy_memory_trends.png)
- [results/benchmark_analysis_outputs/plots/algorithm_comparison_by_device.png](results/benchmark_analysis_outputs/plots/algorithm_comparison_by_device.png)
- [results/benchmark_analysis_outputs/plots/device_comparison_by_algorithm.png](results/benchmark_analysis_outputs/plots/device_comparison_by_algorithm.png)

Variability and platform figures:
- [results/benchmark_analysis_outputs/plots/round_level_variability_total_time.png](results/benchmark_analysis_outputs/plots/round_level_variability_total_time.png)
- [results/benchmark_analysis_outputs/plots/pc_vs_mobile_time.png](results/benchmark_analysis_outputs/plots/pc_vs_mobile_time.png)
- [results/benchmark_analysis_outputs/plots/pc_vs_mobile_energy.png](results/benchmark_analysis_outputs/plots/pc_vs_mobile_energy.png)
- [results/benchmark_analysis_outputs/plots/pc_vs_mobile_ratios.png](results/benchmark_analysis_outputs/plots/pc_vs_mobile_ratios.png)
- [results/benchmark_analysis_outputs/plots/pc_algorithm_summary.png](results/benchmark_analysis_outputs/plots/pc_algorithm_summary.png)
- [results/benchmark_analysis_outputs/plots/device_d1_summary.png](results/benchmark_analysis_outputs/plots/device_d1_summary.png)
- [results/benchmark_analysis_outputs/plots/device_d2_summary.png](results/benchmark_analysis_outputs/plots/device_d2_summary.png)

Interpretation synthesis:
- Time trend and comparison figures confirm AES-GCM as the fastest stable option at medium and large sizes.
- Variability figure supports the need to evaluate consistency, not only mean speed.
- PC/mobile ratio figures confirm that architectural transition penalties differ strongly by algorithm class.

## 5. Discussion

### 5.1 Why AES-GCM Wins Overall

AES-GCM combines:
- Lowest mean mobile latency across devices
- Good cross-platform persistence of ranking
- Better practical scalability at larger payloads

This produces the strongest operational balance for general-purpose deployment.

### 5.2 ElGamal Trade-off in Practical Terms

Relative to AES-GCM:
- Approximately 6x slower
- Approximately 2.4x more energy efficient

Operational meaning:
- ElGamal is suitable when energy budget is the dominant constraint and latency budget is loose.
- It is unsuitable for interactive or real-time flows.

### 5.3 ChaCha20 Positioning

ChaCha20 provides:
- Strong small-message speed
- High runtime consistency

It is a strong alternative for consistency-focused or low-size workloads, but it does not beat AES-GCM in overall balanced ranking.

### 5.4 Other Algorithms

- RSA-Hybrid: Similar latency class to ElGamal but weaker energy profile than ElGamal.
- ASCON and Xoodyak: Mid-tier behavior, scenario-dependent utility.
- GIFT-COFB, Grain-128AEAD, Elephant: High-latency profiles, weak for mainstream mobile low-latency requirements.

## 6. Threats to Validity

- Benchmark outcomes are sensitive to implementation details and test environment.
- Energy readings are affected by thermal state and background system activity.
- Composite rankings depend on weighting assumptions.
- Security-policy constraints and compliance requirements may override purely performance-based decisions.

## 7. Final Recommendations

Default recommendation:
- Use AES-GCM for most production scenarios.

Energy-first recommendation:
- Use ElGamal only for battery-constrained, delay-tolerant workloads.

Consistency/small-payload recommendation:
- Use ChaCha20 when deterministic runtime and small-input behavior are primary.

Decision matrix:
- Real-time interactive services: AES-GCM
- Battery-critical asynchronous services: ElGamal
- Consistency-first edge workloads: ChaCha20

## 8. Conclusion

The complete integrated evidence supports three conclusions:
1. AES-GCM is the strongest general-purpose algorithm in this benchmark.
2. ElGamal is the strongest energy-specialized algorithm but not a latency-general solution.
3. ChaCha20 is a robust consistency-focused alternative, especially for small payloads.

Therefore, algorithm selection should be policy-driven by workload intent: speed balance, energy priority, or consistency priority.

## Appendix A: Referenced Files

Final report:
- [results/FINAL_BENCHMARK_ANALYSIS_REPORT_ENGLISH.md](results/FINAL_BENCHMARK_ANALYSIS_REPORT_ENGLISH.md)

Core tables:
- [results/run_results/analysis_tables/summary_rankings.csv](results/run_results/analysis_tables/summary_rankings.csv)
- [results/run_results/analysis_tables/tradeoff_ranking.csv](results/run_results/analysis_tables/tradeoff_ranking.csv)
- [results/run_results/analysis_tables/pc_vs_mobile_comparison.csv](results/run_results/analysis_tables/pc_vs_mobile_comparison.csv)
- [results/run_results/analysis_tables/pc_algorithm_ranking.csv](results/run_results/analysis_tables/pc_algorithm_ranking.csv)
- [results/run_results/analysis_tables/aggregated_results.csv](results/run_results/analysis_tables/aggregated_results.csv)

Figures:
- [results/benchmark_analysis_outputs/plots/time_trends_by_algorithm_device.png](results/benchmark_analysis_outputs/plots/time_trends_by_algorithm_device.png)
- [results/benchmark_analysis_outputs/plots/energy_memory_trends.png](results/benchmark_analysis_outputs/plots/energy_memory_trends.png)
- [results/benchmark_analysis_outputs/plots/algorithm_comparison_by_device.png](results/benchmark_analysis_outputs/plots/algorithm_comparison_by_device.png)
- [results/benchmark_analysis_outputs/plots/device_comparison_by_algorithm.png](results/benchmark_analysis_outputs/plots/device_comparison_by_algorithm.png)
- [results/benchmark_analysis_outputs/plots/round_level_variability_total_time.png](results/benchmark_analysis_outputs/plots/round_level_variability_total_time.png)
- [results/benchmark_analysis_outputs/plots/pc_vs_mobile_time.png](results/benchmark_analysis_outputs/plots/pc_vs_mobile_time.png)
- [results/benchmark_analysis_outputs/plots/pc_vs_mobile_energy.png](results/benchmark_analysis_outputs/plots/pc_vs_mobile_energy.png)
- [results/benchmark_analysis_outputs/plots/pc_vs_mobile_ratios.png](results/benchmark_analysis_outputs/plots/pc_vs_mobile_ratios.png)
- [results/benchmark_analysis_outputs/plots/pc_algorithm_summary.png](results/benchmark_analysis_outputs/plots/pc_algorithm_summary.png)
- [results/benchmark_analysis_outputs/plots/device_d1_summary.png](results/benchmark_analysis_outputs/plots/device_d1_summary.png)
- [results/benchmark_analysis_outputs/plots/device_d2_summary.png](results/benchmark_analysis_outputs/plots/device_d2_summary.png)