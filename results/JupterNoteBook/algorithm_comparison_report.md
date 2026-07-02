# Algorithm Comparison Report

## Scope

This report compares the cryptographic algorithms in the benchmark using the mobile datasets from `unplugged_run/d1` and `unplugged_run/d2`.

The comparison is focused on three criteria:

1. Average total time
2. Average energy consumption
3. Composite trade-off score

The analysis uses the already processed notebook outputs, where lower values are better for all three criteria.

## 1. Comparison by Average Time

| Rank | Algorithm | Mean time (ms) | Median (ms) |
|---:|---|---:|---:|
| 1 | AES-GCM | 6.3150 | 4.5796 |
| 2 | ChaCha20 | 9.6171 | 5.1231 |
| 3 | AES | 12.1958 | 5.2467 |
| 4 | ASCON | 12.8408 | 8.6229 |
| 5 | Xoodyak | 16.2028 | 8.8625 |
| 6 | RSA-Hybrid | 37.9185 | 31.1956 |
| 7 | ElGamal | 38.4988 | 31.3004 |
| 8 | GIFT-COFB | 52.1101 | 25.1930 |
| 9 | Grain-128AEAD | 90.6167 | 33.2195 |
| 10 | Elephant | 225.9220 | 80.0766 |

AES-GCM is the fastest algorithm on average. ChaCha20 is the second-best in speed and remains clearly faster than most of the remaining algorithms.

## 2. Comparison by Average Energy

| Rank | Algorithm | Mean energy (J) | Median (J) |
|---:|---|---:|---:|
| 1 | ElGamal | 0.000193 | 0.000031 |
| 2 | RSA-Hybrid | 0.000213 | 0.000031 |
| 3 | AES-GCM | 0.000458 | 0.000031 |
| 4 | ASCON | 0.000521 | 0.000031 |
| 5 | Xoodyak | 0.000613 | 0.000031 |
| 6 | GIFT-COFB | 0.000766 | 0.000029 |
| 7 | ChaCha20 | 0.000768 | 0.000031 |
| 8 | AES | 0.001488 | 0.000031 |
| 9 | Grain-128AEAD | 0.001563 | 0.000031 |
| 10 | Elephant | 0.002201 | 0.000037 |

ElGamal has the lowest mean energy, followed closely by RSA-Hybrid. However, these methods are much slower than AES-GCM, so they are not the best balanced choices.

## 3. Composite Trade-Off Score

The notebook computes a normalized score using:

- `time_norm` = min-max normalized total time
- `energy_norm` = min-max normalized energy
- `score = 0.5 * time_norm + 0.5 * energy_norm`

Lower scores are better.

| Rank | Algorithm | Mean score | Mean time (ms) | Mean energy (J) |
|---:|---|---:|---:|---:|
| 1 | AES-GCM | 0.0207 | 6.3150 | 0.000458 |
| 2 | ElGamal | 0.0242 | 38.4988 | 0.000193 |
| 3 | RSA-Hybrid | 0.0249 | 37.9185 | 0.000213 |
| 4 | ASCON | 0.0275 | 12.8408 | 0.000521 |
| 5 | Xoodyak | 0.0341 | 16.2028 | 0.000613 |
| 6 | ChaCha20 | 0.0386 | 9.6171 | 0.000768 |
| 7 | GIFT-COFB | 0.0613 | 52.1101 | 0.000766 |
| 8 | AES | 0.0774 | 12.1958 | 0.001488 |
| 9 | Grain-128AEAD | 0.1234 | 90.6167 | 0.001563 |
| 10 | Elephant | 0.2293 | 225.9220 | 0.002201 |

AES-GCM is the best overall algorithm under the balanced trade-off score. It combines the best runtime with a competitive energy profile, which makes it the strongest general-purpose choice in this benchmark.

## Main Conclusion

If the goal is to choose one algorithm from the benchmark, AES-GCM is the best overall option.

- It is the fastest algorithm on average.
- It is competitive in energy consumption, even if it is not the absolute lowest.
- It also ranks first in the combined score that balances time and energy equally.

If the goal is pure energy minimization, ElGamal is the best. If the goal is pure speed, AES-GCM is the best. For a practical mobile benchmark recommendation, AES-GCM is the most balanced choice.

## Figures Used in the Notebook

The notebook already includes plots that support this comparison:

- `time_trends_by_algorithm_device.png`
- `energy_memory_trends.png`
- `algorithm_comparison_by_device.png`
- `device_comparison_by_algorithm.png`
- `round_level_variability_total_time.png`

## Exported Tables Used Here

- `summary_rankings.csv`
- `tradeoff_ranking.csv`
