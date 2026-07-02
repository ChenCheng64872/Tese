# 📊 COMPLETE GUIDE: NEW ADVANCED ANALYSIS PLOTS

## 13 New Plots for Advanced Results Analysis

---

## SERIES 1: STATISTICAL VISUALIZATION PLOTS (5 plots)

### Plot 1️⃣: **coefficient_of_variation_analysis.png**
**What it shows**: Coefficient of Variation (%) for Time and Energy

**How to read**:
- Left bars: CV for Total Time
- Right bars: CV for Energy
- **Green/Low**: Better (more consistent)
- **Red/High**: Worse (less consistent)

**Key Insight**: ChaCha20 has the best time stability (9.8% CV)

**Interpretation**: Lower CV means more predictable and consistent performance - critical for real-time applications.

---

### Plot 2️⃣: **time_distribution_histograms.png**
**What it shows**: Frequency distribution of execution times

**Components**:
- Bars: Frequency of observations
- Red line: Mean
- Orange lines: ±1 Standard Deviation

**What to look for**:
- ✓ Symmetric distribution = Stable
- ⚠ Right-skewed = Occasional slowdowns
- ⚠ Multimodal = Inconsistent behavior

**Ideal**: Bell curve centered at mean with most values within ±1σ

---

### Plot 3️⃣: **time_distribution_violin_plots.png**
**What it shows**: Complete distribution shape by algorithm and device

**Interpretation**:
- Wide sections = Higher data density
- Narrow sections = Sparse data
- D1 vs D2 comparison = Device-specific behavior

**Ideal**: Narrow, symmetric violin = Predictable performance

**Why it matters**: Shows multimodal behavior and device-specific variations not visible in boxplots alone.

---

### Plot 4️⃣: **energy_time_tradeoff_scatter.png**
**What it shows**: Fundamental Energy vs Time trade-off space

**Reading**:
- X-axis (log): Time (ms) - left = fast
- Y-axis (log): Energy (J) - bottom = efficient
- Colors: Different algorithms

**Critical Regions**:
- 🟢 **Lower-left corner**: IDEAL (fast + efficient)
  - Example: AES-GCM
- 🔴 **Upper-right corner**: WORST (slow + inefficient)
  - Example: Elephant

---

### Plot 5️⃣: **cv_heatmap_by_algorithm_device.png**
**What it shows**: Matrix of CV (%) for Algorithm × Device combinations

**Colors**:
- 🟢 Green (low CV): Best - more consistent
- 🔴 Red (high CV): Worst - less consistent

**Quick Use**: Identify which algorithm is most stable on each device

**Ideal**: All cells green = Highly portable and consistent

---

## SERIES 2: ADVANCED ANALYTICAL VISUALIZATIONS (8 NEW plots) ⭐

### Plot 6️⃣: **algorithm_ranking_multi_metric.png** ⭐ NEW
**What it shows**: 4 independent algorithm rankings

**4 Panels**:
1. **Top-left**: Speed Ranking (Time)
   - Leftmost = Fastest
2. **Top-right**: Energy Efficiency Ranking (Energy)
   - Leftmost = Least energy
3. **Bottom-left**: Stability Ranking (Time CV %)
   - Leftmost = Most consistent
4. **Bottom-right**: Throughput Ranking (KB/ms)
   - Leftmost = Highest throughput

**Key Finding**: No single algorithm excels in ALL dimensions
- AES-GCM: Excellent speed + stability
- ElGamal: Excellent energy but poor speed
- ChaCha20: Best stability

---

### Plot 7️⃣: **correlation_metrics_heatmap.png** ⭐ NEW
**What it shows**: Correlations between all performance metrics

**Color scheme**:
- 🔴 Red (+1 correlation): Metrics move together
- ⚪ White (0 correlation): Independent
- 🔵 Blue (-1 correlation): Trade-off between metrics

**Examples**:
- Time vs Energy: Strong positive (both high/low together)
- Time vs Memory: Positive correlation (input size affects both)
- Throughput vs Time: Negative correlation (inverse relationship)

**Use case**: Understand hidden relationships between performance dimensions

---

### Plot 8️⃣: **memory_usage_analysis.png** ⭐ NEW
**What it shows**: Memory consumption by algorithm

**Two panels**:
1. **Left**: Bar chart of average memory per algorithm
   - Height = Memory consumption
   - Log scale (important!)
2. **Right**: Scatter - Memory vs Time
   - Color = Input size (brighter = larger)

**Insight**: 
- ✓ Horizontal line = Constant memory (good!)
- ⚠ Diagonal line = Memory scales with input size

---

### Plot 9️⃣: **scaling_behavior_analysis.png** ⭐ NEW
**What it shows**: Performance scaling with input size (log-log plots)

**Two panels**:
1. **Left**: Time vs Input Size
2. **Right**: Energy vs Input Size

**Slope Interpretation** (log-log):
- Slope ≈ 1: **Linear** (IDEAL) - Doubling input ≈ doubles time
- Slope ≈ 1.5: **Superlinear** - Performance degrades faster
- Slope ≈ 2: **Quadratic** (POOR) - Very sensitive to input size

**Example**: AES-GCM has slope ~1 (scales linearly, excellent for large inputs)

---

### Plot 1️⃣0️⃣: **kde_distribution_comparison.png** ⭐ NEW
**What it shows**: Smooth probability density curves

**Two panels**:
1. **Left**: KDE by Device (D1 vs D2)
2. **Right**: KDE for Top 5 fastest algorithms

**Reading**:
- Tall, narrow curve = Concentrated distribution (consistent)
- Short, wide curve = Spread distribution (variable)
- Multiple peaks = Bimodal behavior (inconsistent)

---

### Plot 1️⃣1️⃣: **performance_matrix_heatmap.png** ⭐ NEW
**What it shows**: Algorithm × Input Size performance matrix

**Two panels**:
1. **Left**: Time (yellow-orange-red scale)
2. **Right**: Energy (pink-red-black scale)

**Interpretation**:
- Each cell = Algorithm + Input Size combination
- Color = Performance value
- 🟢 Green/Yellow = Good
- 🔴 Red = Poor

**Patterns**:
- **Horizontal line**: Algorithm insensitive to input size
- **Diagonal**: Clear scaling effects

---

### Plot 1️⃣2️⃣: **speed_vs_stability_analysis.png** ⭐ NEW
**What it shows**: Speed vs Consistency trade-off (3D visualization!)

**3 Dimensions**:
- **X-axis (log)**: Speed - left = fast
- **Y-axis**: Stability (CV %) - low = consistent
- **Bubble size**: Energy consumption (bigger = more energy)
- **Color**: Throughput (green = high, red = low)

**Green zone**: CV < 15% (ideal for latency-critical applications)

**Ideal algorithm**: Lower-left corner, small bubble, green color

---

### Plot 1️⃣3️⃣: **device_performance_comparison.png** ⭐ NEW
**What it shows**: Performance index comparison between D1 and D2

**Components**:
- Paired bars: Algorithm performance on each device
- Height = Performance index (time + energy combined)
- **Similar bars**: Good portability
- **Different bar heights**: Device-specific behavior

**Ideal**: All algorithms with similar bar heights on both devices

---

### Plot 1️⃣4️⃣: **percentile_distribution_analysis.png** ⭐ NEW
**What it shows**: Tail latency analysis - worst-case scenarios

**4 panels** (Top 4 fastest algorithms):
- Each panel shows percentile curve (0-100%)
- Line shows how time increases at extreme percentiles

**Key Metrics**:
- **Mean**: Average value
- **Median (P50)**: Central value
- **P95**: 95% of executions faster than this
- **P99**: 99% of executions faster than this (worst ~1%)

**Ideal**: Flat curve (no degradation in tail latency)

---

### Plot 1️⃣5️⃣: **pareto_frontier_time_energy.png** ⭐ NEW
**What it shows**: Pareto-optimal algorithms in the time-energy space

**Red circle**: Pareto frontier (non-dominated algorithms)

**Concept**: Algorithm on frontier cannot be improved in both dimensions without worsening another

**Interpretation**:
- **On frontier**: Optimal choice depending on priorities
- **Inside frontier**: Dominated by at least one frontier algorithm

---

## SERIES 3: ELGAMAL DEEP DIVE ANALYSIS (3 NEW plots) ⭐⭐⭐

### Plot 1️⃣6️⃣: **elgamal_detailed_comparison.png** ⭐⭐⭐ FOCUS
**What it shows**: ElGamal vs AES-GCM head-to-head comparison

**4 Panels**:
1. **Top-left**: Time scaling (with error bars)
   - Shows ElGamal is around 6.10× slower than AES-GCM on aggregate
2. **Top-right**: Energy scaling (with error bars)
   - Shows ElGamal uses around 2.37× less energy than AES-GCM on aggregate
3. **Bottom-left**: Time distribution by device
   - Box plots reveal variability
4. **Bottom-right**: Trade-off ratio
   - Normalized time vs energy comparison

**Key Finding**: ElGamal trades massive time penalty for energy savings

**Use case**: ElGamal offers strong energy savings at significant latency cost; use where energy is prioritized over latency.

---

### Plot 1️⃣7️⃣: **elgamal_positioning_analysis.png** ⭐⭐⭐ FOCUS
**What it shows**: ElGamal's strategic position in the algorithm space

**Two panels**:
1. **Left**: Time-Energy trade-off with Pareto frontier
   - ElGamal highlighted in RED (unique position)
   - Green circle = Pareto frontier
   - Annotation zones for "IDEAL" and "POOR" regions
   
2. **Right**: Ranking matrix (Speed rank vs Energy rank)
   - ElGamal is #1 in energy but LAST in speed
   - AES-GCM is balanced performer
   - Visual representation of specialization

**Critical Insight**: ElGamal is the ONLY algorithm in upper-left region (low energy, high time)

**Specialization**: Perfect for energy-critical applications, not for speed-critical ones

---

### Plot 1️⃣8️⃣: **elgamal_battery_simulation.png** ⭐⭐⭐ FOCUS
**What it shows**: Practical battery life implications

**Simulation**: How many encryption operations possible per battery charge

**Parameters**:
- Multiple battery capacities (1000-10000 mWh)
- 1 KB payload per operation
- Comparison: ElGamal vs AES-GCM

**Key Metrics**:
- ElGamal enables approximately 2.37× more operations than AES-GCM for the same energy budget
- Keep this as a relative efficiency result unless a specific workload duty cycle is defined

**Annotation**: Shows exact efficiency gain percentage

**When to use**:
- ✓ IoT devices with small batteries
- ✓ Wearables with strict power budget
- ✓ Background encryption in low-power mode
- ❌ Real-time applications needing fast response

---

## 📈 QUICK REFERENCE: Which Plot to Use?

| Question | Use This Plot |
|----------|---|
| Which algorithm is most consistent? | CV Heatmap or CV Analysis |
| How does performance scale with input? | Scaling Behavior (log-log) |
| What's the energy-time trade-off? | Energy-Time Scatter |
| Which algorithm is best on each device? | Device Performance Comparison |
| What does the distribution look like? | Histograms or Violin Plots |
| Which input sizes are problematic? | Performance Matrix |
| What's the worst-case scenario? | Percentile Distribution |
| What's the best overall choice? | Algorithm Ranking Multi-Metric |
| Are metrics related? | Correlation Heatmap |
| How much memory does each use? | Memory Usage Analysis |
| **What about ElGamal specifically?** | **ElGamal Analysis (3 plots)** |

---

## 🎯 ANALYSIS WORKFLOW RECOMMENDATION

### Phase 1: Quick Overview (5 mins)
1. View `algorithm_ranking_multi_metric.png` - Understand all 4 dimensions
2. View `energy_time_tradeoff_scatter.png` - See trade-off space

### Phase 2: Deep Dive (15 mins)
3. View `coefficient_of_variation_analysis.png` - Stability analysis
4. View `scaling_behavior_analysis.png` - How it scales
5. View `performance_matrix_heatmap.png` - Algorithm × Input matrix

### Phase 3: Technical Analysis (20 mins)
6. View `percentile_distribution_analysis.png` - Tail latency
7. View `correlation_metrics_heatmap.png` - Metric relationships
8. View `memory_usage_analysis.png` - Memory footprint

### Phase 4: Deployment Decisions
9. View `device_performance_comparison.png` - Cross-device behavior
10. View `speed_vs_stability_analysis.png` - Best balance

### Phase 5: ElGamal Evaluation (IF RELEVANT)
11. View `elgamal_detailed_comparison.png` - Head-to-head vs AES-GCM
12. View `elgamal_positioning_analysis.png` - Unique position
13. View `elgamal_battery_simulation.png` - Practical implications

---

## 🔍 ELGAMAL SPECIAL ANALYSIS

### Why ElGamal Analysis Matters

**The Paradox**: ElGamal is worst in speed but BEST in energy

- **Time Performance**: 6.10× SLOWER than AES-GCM (aggregate)
- **Energy Performance**: 2.37× MORE EFFICIENT than AES-GCM (aggregate)

### Key ElGamal Metrics

```
Execution Time:  38.5 ms average
Energy Usage:    0.000193 J average (LOWEST)
Time CV:         8.3% (very stable!)
Energy CV:       52.1% (variable)
Ranking:         #1 Energy, #10 Speed, #2 Energy Stability
```

### When to Use ElGamal

✓ **Good Use Cases**:
- IoT devices with extremely limited power budget
- Wearable devices running on coin-cell batteries
- Background encryption in low-power mode
- Sensor networks where energy >> latency concerns
- Data centers with specific energy constraints

❌ **Bad Use Cases**:
- Real-time applications (voice, video)
- Interactive user-facing services
- Applications requiring <100ms response
- Memory-constrained embedded systems

### ElGamal vs AES-GCM Trade-off

| Metric | ElGamal | AES-GCM | Winner |
|--------|---------|---------|--------|
| Speed | 38.5 ms | 6.3 ms | AES-GCM (6.10× faster) |
| Energy | 0.000193 J | 0.000458 J | ElGamal (2.37× better) |
| Stability | 8.3% CV | 12.4% CV | ElGamal (more stable) |
| Throughput | Low | High | AES-GCM |
| Relative Operations per Fixed Energy Budget | 2.37× baseline | 1× baseline | ElGamal |

---

## 📊 TOTAL VISUALIZATION SUITE

```
Total Plots Generated: 18+ Advanced Visualizations

Breakdown:
  ✓ 5 Statistical plots (distributions, CV, etc.)
  ✓ 8 Advanced analytical plots (correlations, scaling, etc.)
  ✓ 3 ElGamal deep-dive plots (comparison, positioning, battery)
  ✓ 2+ Original performance comparison plots
  
Total Time Investment: ~30-50 minutes to view and interpret all plots
```

---

## 🚀 ADVANCED OPTIONS (Not Yet Implemented)

Potential future enhancements:

- [ ] Interactive dashboard with Plotly
- [ ] 3D scatter: Time vs Energy vs Memory
- [ ] Regression model predicting time from input size
- [ ] Dynamic ranking: Best algorithm per device
- [ ] Sensitivity analysis for alpha parameter
- [ ] Outlier timeline: When do slowdowns occur?
- [ ] Cross-platform deep comparison (PC vs Mobile)
- [ ] ML-based algorithm recommendation engine

---

**Complete Analysis Suite Ready for Publication and Presentation! 🎓**

*All plots include academic-quality formatting, legends, annotations, and interpretations.*
