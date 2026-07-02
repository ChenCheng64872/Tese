# 📁 CSV Files Organization

## Folder Structure

All benchmark CSV files have been reorganized into a clean structure under `run_results/`:

```
run_results/
├── device1/              (10 files - D1 Unplugged Benchmark Runs)
├── device2/              (10 files - D2 Unplugged Benchmark Runs)
├── pc_benchmark/         (10 files - PC Benchmark Runs)
├── analysis_tables/      (5 files - Aggregated Analysis Results)
└── README_ORGANIZATION.md (This file)
```

---

## Folder Descriptions

### 📱 `device1/` - Device 1 Benchmark Data
- **Source**: `unplugged_run/d1/`
- **Count**: 10 CSV files
- **Content**: Cryptographic algorithm benchmarks from Device 1 (unplugged runs)
- **File naming**: `{algorithm}_bench_2p10_2p18.csv`
- **Algorithms**:
  - aes_bench_2p10_2p18.csv
  - aesgcm_bench_2p10_2p18.csv
  - ascon_bench_2p10_2p18.csv
  - chacha20_bench_2p10_2p18.csv
  - elephant_bench_2p10_2p18.csv
  - elgamal_bench_2p10_2p18.csv
  - giftcofb_bench_2p10_2p18.csv
  - grain128aead_bench_2p10_2p18.csv
  - rsa_hybrid_bench_2p10_2p18.csv
  - xoodyak_bench_2p10_2p18.csv

### 📱 `device2/` - Device 2 Benchmark Data
- **Source**: `unplugged_run/d2/`
- **Count**: 10 CSV files
- **Content**: Cryptographic algorithm benchmarks from Device 2 (unplugged runs)
- **File naming**: `{algorithm}_bench_2p10_2p18.csv`
- **Algorithms**: (Same as device1)

### 💻 `pc_benchmark/` - PC Benchmark Data
- **Source**: `time/`
- **Count**: 10 CSV files
- **Content**: Cryptographic algorithm benchmarks from PC baseline
- **File naming**: `{algorithm}_bench_2p10_2p20.csv` and `{algorithm}_bench_2p10_2p20_per_exec.csv`
- **Algorithms**:
  - aes_bench_2p10_2p20.csv / aes_bench_2p10_2p20_per_exec.csv
  - aesgcm_bench_2p10_2p20.csv / aesgcm_bench_2p10_2p20_per_exec.csv
  - chacha20_bench_2p10_2p20.csv / chacha20_bench_2p10_2p20_per_exec.csv
  - elgamal_bench_2p10_2p20.csv / elgamal_bench_2p10_2p20_per_exec.csv
  - rsa_hybrid_bench_2p10_2p20.csv / rsa_hybrid_bench_2p10_2p20_per_exec.csv

### 📊 `analysis_tables/` - Aggregated Analysis Results
- **Source**: `benchmark_analysis_outputs/tables/`
- **Count**: 5 CSV files
- **Content**: Pre-computed aggregated results and comparisons
- **Files**:
  - `aggregated_results.csv` - Combined results from all benchmarks
  - `pc_algorithm_ranking.csv` - PC benchmark algorithm rankings
  - `pc_vs_mobile_comparison.csv` - Comparison between PC and mobile results
  - `summary_rankings.csv` - Summary rankings across all dimensions
  - `tradeoff_ranking.csv` - Trade-off analysis rankings

---

## File Statistics

| Folder | File Count | Data Type |
|--------|-----------|-----------|
| device1 | 10 | Mobile Device 1 Raw Data |
| device2 | 10 | Mobile Device 2 Raw Data |
| pc_benchmark | 10 | PC Baseline Raw Data |
| analysis_tables | 5 | Computed Analysis Results |
| **TOTAL** | **35** | — |

---

## Naming Convention

### Device Benchmark Files
- **Pattern**: `{algorithm}_bench_2p{param1}_2p{param2}.csv`
- **Example**: `aes_bench_2p10_2p18.csv`
- **Meaning**: 
  - `2p10` = 2^10 = 1024 bytes
  - `2p18` = 2^18 = 262,144 bytes (device runs)
  - `2p20` = 2^20 = 1,048,576 bytes (PC runs)

### Per-Execution Files (PC only)
- **Pattern**: `{algorithm}_bench_2p{param1}_2p{param2}_per_exec.csv`
- **Meaning**: Per-execution statistics (more granular)

---

## Legacy Locations

Legacy folders now exist only as empty placeholders:
- `unplugged_run/d1/`
- `unplugged_run/d2/`
- `time/`
- `benchmark_analysis_outputs/tables/`

**Note**: CSV files were consolidated to `run_results/` to remove repeated copies.

---

## Usage

### For Data Analysis
Use files from their respective device folders:
```python
import pandas as pd

# Load Device 1 results
df_d1 = pd.read_csv('run_results/device1/aes_bench_2p10_2p18.csv')
df_d2 = pd.read_csv('run_results/device2/aes_bench_2p10_2p18.csv')
df_pc = pd.read_csv('run_results/pc_benchmark/aes_bench_2p10_2p20.csv')
```

### For Comparisons
Use analysis tables:
```python
# Load comparison results
comparison = pd.read_csv('run_results/analysis_tables/pc_vs_mobile_comparison.csv')
rankings = pd.read_csv('run_results/analysis_tables/summary_rankings.csv')
```

---

## Backup & Recovery

If you need to rebuild legacy locations, copy from `run_results/`:
- `run_results/device1/*.csv` → `unplugged_run/d1/`
- `run_results/device2/*.csv` → `unplugged_run/d2/`
- `run_results/pc_benchmark/*.csv` → `time/`
- `run_results/analysis_tables/*.csv` → `benchmark_analysis_outputs/tables/`

---

**Organization Date**: July 2, 2026
**Status**: ✅ Complete
**Total Files Organized**: 35 CSV files
