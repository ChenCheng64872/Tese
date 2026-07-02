# Android/Mobile Cryptography Benchmark Analysis Report

## Scope

This report summarizes the benchmark analysis performed in the notebook for the mobile cryptography experiment. The analysis uses the raw round-level CSV files in `unplugged_run/d1` and `unplugged_run/d2` as the mobile datasets, and the CSV files in the `time` folder as the PC/computer baseline.

The raw mobile files contain the expected benchmark schema:

- `size_bytes`
- `round_index`
- `enc_ns`
- `dec_ns`
- `enc_mem_bytes`
- `dec_mem_bytes`
- `energy_mWh`
- `method`

The notebook converts time to milliseconds, energy to Joules, and size to KB/MB, then computes aggregated summaries and rankings.

## Data Quality Summary

The raw mobile benchmark dataset contains 900 rows in total after combining the raw round-level files from both devices.

Main quality checks:

- Missing values: none detected.
- Duplicated rows: none detected.
- Invalid numeric values: none detected.
- Expected rounds per size/algorithm/device combination: 5.
- IQR-based outliers: 300 rows flagged across the dataset.

The outlier flag is kept for interpretation and boxplot analysis; the original observations are not removed.

## Key Derived Metrics

The notebook computes the following derived metrics:

- `enc_ms` = `enc_ns / 1,000,000`
- `dec_ms` = `dec_ns / 1,000,000`
- `total_time_ms` = `enc_ms + dec_ms`
- `energy_J` = `energy_mWh * 3.6`
- `size_kb` = `size_bytes / 1024`
- `size_mb` = `size_bytes / 1024^2`
- `avg_mem_bytes` = average of encryption and decryption memory usage
- `total_mem_bytes` = `enc_mem_bytes + dec_mem_bytes`

## Main Mobile Results

### Fastest Algorithm per Device

| Device | Best algorithm | Mean total time (ms) | Mean energy (J) |
|---|---:|---:|---:|
| D1 | AES-GCM | 6.3307 | 0.000430 |
| D2 | AES-GCM | 6.2992 | 0.000486 |

AES-GCM is the fastest algorithm on both devices, and the result is consistent across the two mobile platforms.

### Lowest-Energy Algorithm per Device

| Device | Best algorithm | Mean total time (ms) | Mean energy (J) |
|---|---:|---:|---:|
| D1 | ElGamal | 38.5726 | 0.000174 |
| D2 | ElGamal | 38.4249 | 0.000212 |

ElGamal minimizes energy on both devices, but it does so at a much higher runtime cost than AES-GCM.

### Best Algorithm by Input Size

| Input size (bytes) | Best algorithm | Mean total time (ms) | Mean energy (J) |
|---:|---:|---:|---:|
| 1024 | ChaCha20 | 2.8039 | 0.000185 |
| 2048 | ChaCha20 | 3.0398 | 0.000114 |
| 4096 | AES-GCM | 3.1630 | 0.000123 |
| 8192 | AES-GCM | 4.0590 | 0.000175 |
| 16384 | AES-GCM | 4.5586 | 0.000231 |
| 32768 | AES-GCM | 5.2747 | 0.000319 |
| 65536 | AES-GCM | 6.5562 | 0.000514 |
| 131072 | AES-GCM | 10.0832 | 0.000871 |
| 262144 | AES-GCM | 17.2153 | 0.001586 |

ChaCha20 is the best at the smallest sizes, but AES-GCM becomes the dominant choice for medium and large inputs.

## Trade-Off Ranking

The notebook computes a normalized score using:

- `time_norm` = min-max normalized `total_time_ms`
- `energy_norm` = min-max normalized `energy_J`
- `score = alpha * time_norm + (1 - alpha) * energy_norm`

with `alpha = 0.5` by default.

### Composite Ranking

| Rank | Algorithm | Mean score | Median score | Score std | Mean total time (ms) | Mean energy (J) |
|---:|---|---:|---:|---:|---:|---:|
| 1 | AES-GCM | 0.0207 | 0.0080 | 0.0272 | 6.3150 | 0.000458 |
| 2 | ElGamal | 0.0242 | 0.0176 | 0.0138 | 38.4988 | 0.000193 |
| 3 | RSA-Hybrid | 0.0249 | 0.0183 | 0.0137 | 37.9185 | 0.000213 |
| 4 | ASCON | 0.0275 | 0.0094 | 0.0358 | 12.8408 | 0.000521 |
| 5 | Xoodyak | 0.0341 | 0.0102 | 0.0481 | 16.2028 | 0.000613 |
| 6 | ChaCha20 | 0.0386 | 0.0103 | 0.0570 | 9.6171 | 0.000768 |
| 7 | GIFT-COFB | 0.0613 | 0.0229 | 0.0780 | 52.1101 | 0.000766 |
| 8 | AES | 0.0774 | 0.0275 | 0.1048 | 12.1958 | 0.001488 |
| 9 | Grain-128AEAD | 0.1234 | 0.0356 | 0.1720 | 90.6167 | 0.001563 |
| 10 | Elephant | 0.2293 | 0.0731 | 0.3109 | 225.9220 | 0.002201 |

### Sensitivity to `alpha`

| alpha | Best algorithm |
|---:|---|
| 0.25 | ElGamal |
| 0.50 | AES-GCM |
| 0.75 | AES-GCM |

AES-GCM is the best overall choice for the default balanced weighting and remains the best when time is weighted more heavily.

## PC / Computer Baseline Comparison

The `time` folder corresponds to the PC/computer benchmark run. It is not the same as the mobile benchmark data, so it is treated as a separate baseline and compared only for algorithms that appear in both datasets.

### PC vs Mobile Summary

The comparison is plotted with log scales, so the PC results remain visible even where the absolute values differ strongly from the mobile runs.

- AES-GCM is the fastest algorithm on both platforms.
- The mobile runtime penalty is smallest for AES, at about 1.84× slower than PC.
- The mobile runtime penalty is largest for ElGamal, at about 16.43× slower than PC.
- RSA-Hybrid is also much slower on mobile, at about 12.56× slower than PC.
- ChaCha20 stays relatively close to the PC baseline, with a mobile slowdown of about 3.18×.
- Energy values are much smaller on mobile because the notebook converts the benchmark energy readings into Joules and the devices were measured under different execution conditions.

### PC-only Ranking

| Rank | Algorithm | Mean time (ms) | Mean energy (J) |
|---:|---|---:|---:|
| 1 | AES-GCM | 8.3932 | 0.0563 |
| 2 | ChaCha20 | 16.1282 | 0.0922 |
| 3 | AES | 18.3687 | 0.1005 |
| 4 | RSA-Hybrid | 18.5917 | 0.1063 |
| 5 | ElGamal | 18.9334 | 0.1063 |

The PC-only summary makes the computer baseline explicit and shows that AES-GCM remains the best all-round choice even on the desktop run, while the other algorithms cluster at higher runtime and energy values.

## Variability and Stability

The notebook also generates round-level boxplots for total time.

Main takeaway:

- AES-GCM and ChaCha20 show comparatively tighter distributions.
- Larger and more complex algorithms exhibit wider spread and more variability across rounds.
- The IQR-based outlier flags support the visual interpretation that some algorithms are more stable than others under repeated execution.

## Figures Generated

The following plots were created and saved as PNG files:

- `plots/time_trends_by_algorithm_device.png`
- `plots/energy_memory_trends.png`
- `plots/algorithm_comparison_by_device.png`
- `plots/device_comparison_by_algorithm.png`
- `plots/round_level_variability_total_time.png`
- `plots/pc_vs_mobile_time.png`
- `plots/pc_vs_mobile_energy.png`
- `plots/pc_vs_mobile_ratios.png`
- `plots/pc_algorithm_summary.png`
- `plots/device_d1_summary.png`
- `plots/device_d2_summary.png`

## Conclusions

1. AES-GCM is the best overall algorithm for the mobile benchmark when balancing time and energy with `alpha = 0.5`.
2. ElGamal is the lowest-energy algorithm on both devices, but it is far slower and therefore not the best balanced choice.
3. ChaCha20 is best for the smallest input sizes, while AES-GCM dominates most medium and large input sizes.
4. The PC/computer baseline is faster than the mobile devices for the shared algorithms, and the log-scale PC comparison plots make that difference visible in the figures.
5. The per-device figures make it easy to include a separate summary page for each device in the final report.

## Exported Artifacts

The notebook exports the following files under `benchmark_analysis_outputs`:

- `tables/aggregated_results.csv`
- `tables/summary_rankings.csv`
- `tables/tradeoff_ranking.csv`
- `tables/pc_algorithm_ranking.csv`
- `tables/pc_vs_mobile_comparison.csv`
- all PNG plots listed above

## Final Note

If you need this report adapted into a more formal thesis style, the same content can be rewritten into a short academic discussion section with references to the saved figures and tables.