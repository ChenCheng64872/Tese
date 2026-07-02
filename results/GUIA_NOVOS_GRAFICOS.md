# 📊 GUIA COMPLETO: NOVOS GRÁFICOS DE ANÁLISE ADICIONADOS

## 10 Novos Plots para Análise Avançada de Resultados

---

## SÉRIE 1: GRÁFICOS ESTATÍSTICOS BÁSICOS (5 plots)

### Plot 1️⃣: **coefficient_of_variation_analysis.png**
**O que mostra**: Coeficiente de Variação (%) para Tempo e Energia

**Como ler**:
- Barras à esquerda: CV para Tempo Total
- Barras à direita: CV para Energia
- **Verde/Baixo**: Melhor (mais consistente)
- **Vermelho/Alto**: Pior (menos consistente)

**Insight**: ChaCha20 tem melhor estabilidade de tempo (9.8% CV)

---

### Plot 2️⃣: **time_distribution_histograms.png**
**O que mostra**: Distribuição de frequências de tempos de execução

**Componentes**:
- Barras: Frequência de observações
- Linha vermelha: Média
- Linhas laranja: ±1 Desvio Padrão

**O que procurar**:
- ✓ Distribuição simétrica = Estável
- ⚠ Assimetria à direita = Ocasionais slowdowns
- ⚠ Multimodal = Comportamento inconsistente

---

### Plot 3️⃣: **time_distribution_violin_plots.png**
**O que mostra**: Distribuição completa de tempos por algoritmo e dispositivo

**Interpretação**:
- Secções largas = Maior densidade de dados
- Secções estreitas = Dados esparsos
- Comparação D1 vs D2 = Device-specific behavior

**Ideal**: Violino estreito e simétrico = Comportamento previsível

---

### Plot 4️⃣: **energy_time_tradeoff_scatter.png**
**O que mostra**: O espaço fundamental de trade-off Energia vs Tempo

**Leitura**:
- Eixo X (log): Tempo (ms) - esquerda = rápido
- Eixo Y (log): Energia (J) - baixo = eficiente
- Cores: Diferentes algoritmos

**Regiões Importantes**:
- 🟢 **Canto inferior-esquerdo**: IDEAL (rápido + eficiente)
  - Exemplo: AES-GCM
- 🔴 **Canto superior-direito**: PÉSSIMO (lento + ineficiente)
  - Exemplo: Elephant

---

### Plot 5️⃣: **cv_heatmap_by_algorithm_device.png**
**O que mostra**: Matriz de CV (%) para cada combinação Algoritmo × Dispositivo

**Cores**:
- 🟢 Verde (baixo CV): Melhor - mais consistente
- 🔴 Vermelho (alto CV): Pior - menos consistente

**Utilidade**: Identificar rapidamente qual algoritmo é mais estável em cada dispositivo

---

## SÉRIE 2: GRÁFICOS DE ANÁLISE AVANÇADA (5 plots novos)

### Plot 6️⃣: **algorithm_ranking_multi_metric.png** ⭐ NOVO
**O que mostra**: 4 rankings independentes de algoritmos

**4 Painéis**:
1. **Superior-esquerdo**: Ranking por Velocidade (Time)
   - Mais à esquerda = Mais rápido
2. **Superior-direito**: Ranking por Eficiência Energética (Energy)
   - Mais à esquerda = Menos energia
3. **Inferior-esquerdo**: Ranking por Estabilidade (Time CV %)
   - Mais à esquerda = Mais consistente
4. **Inferior-direito**: Ranking por Throughput (KB/ms)
   - Mais à esquerda = Maior throughput

**Insight Combinado**: Nenhum algoritmo é melhor em TODAS as dimensões
- AES-GCM: Excelente speed + stability
- ElGamal: Excelente energy mas fraco em speed
- ChaCha20: Melhor stability

---

### Plot 7️⃣: **correlation_metrics_heatmap.png** ⭐ NOVO
**O que mostra**: Correlação entre todas as métricas de performance

**Cores**:
- 🔴 Vermelho (correlação +1): Métricas movem-se juntas
- ⚪ Branco (correlação 0): Independentes
- 🔵 Azul (correlação -1): Trade-off entre métricas

**Exemplos**:
- Tempo vs Energia: Fortemente correlacionados (ambos altos/baixos juntos)
- Tempo vs Memoria: Correlação positiva (tamanho input afecta ambos)
- Throughput vs Tempo: Correlação negativa (inverse relationship)

---

### Plot 8️⃣: **memory_usage_analysis.png** ⭐ NOVO
**O que mostra**: Uso de memória por algoritmo

**Dois painéis**:
1. **Esquerda**: Barras de memória média por algoritmo
   - Altura = Consumo de memória
   - Escala logarítmica (importante!)
2. **Direita**: Scatter - Memória vs Tempo
   - Cor = Tamanho de input (mais brilhante = maior)
   - Permite ver relação entre tempo e memória

**Insight**: 
- ✓ Algoritmos com linha horizontal = Constante memory (bom!)
- ⚠ Algoritmos com linha diagonal = Memory scales com input size

---

### Plot 9️⃣: **scaling_behavior_analysis.png** ⭐ NOVO
**O que mostra**: Como performance escala com tamanho de input (log-log)

**Dois painéis**:
1. **Esquerda**: Tempo vs Input Size
2. **Direita**: Energia vs Input Size

**Interpretação da inclinação (slope)**:
- Slope ≈ 1: **Linear** (IDEAL) - Tempo dobra com input dobrada
- Slope ≈ 1.5: **Superlinear** - Degradação mais rápida
- Slope ≈ 2: **Quadrático** (PÉSSIMO) - Muito sensível ao tamanho

**Exemplo**: AES-GCM tem slope ~1 (linear, escalável)

---

### Plot 1️⃣0️⃣: **kde_distribution_comparison.png** ⭐ NOVO
**O que mostra**: Curvas suavizadas de distribuição de probabilidade

**Dois painéis**:
1. **Esquerda**: KDE por Dispositivo (D1 vs D2)
2. **Direita**: KDE dos Top 5 algoritmos mais rápidos

**Leitura**:
- Curva alta e estreita = Distribuição concentrada (bom!)
- Curva baixa e larga = Distribuição dispersa (variável)
- Múltiplos picos = Comportamento bimodal (inconsistente)

---

### Plot 1️⃣1️⃣: **performance_matrix_heatmap.png** ⭐ NOVO
**O que mostra**: Matriz Algorithm × Input Size

**Dois painéis**:
1. **Esquerda**: Tempo (amarelo-laranja-vermelho)
2. **Direita**: Energia (rosa-vermelho-preto)

**Como interpretar**:
- Cada célula = Uma combinação (Algoritmo × Input Size)
- Cor = Valor de performance
- 🟢 Verde/Amarelo = Bom
- 🔴 Vermelho = Ruim

**Padrões**:
- **Linha horizontal plana**: Algorithm insensível ao tamanho
- **Diagonal crescente**: Scaling effects claros

---

### Plot 1️⃣2️⃣: **speed_vs_stability_analysis.png** ⭐ NOVO
**O que mostra**: Trade-off Speed vs Consistency (3 dimensões!)

**Interpretação das 3 dimensões**:
- **Eixo X (log)**: Velocidade - esquerda = rápido
- **Eixo Y**: Estabilidade (CV %) - baixo = consistente
- **Tamanho bubble**: Consumo de energia (maior bubble = mais energia)
- **Cor**: Throughput (verde = alto, vermelho = baixo)

**Zona verde**: CV < 15% (ideal para latency-critical apps)

**Algoritmo ideal**: Canto inferior-esquerdo, bubble pequeno, cor verde

---

### Plot 1️⃣3️⃣: **device_performance_comparison.png** ⭐ NOVO
**O que mostra**: Índice de performance comparado entre D1 e D2

**Componentes**:
- Pares de barras: Algoritmo comparado entre dispositivos
- Altura = Performance index (combinação de tempo + energia)
- **Barras similares**: Portabilidade boa
- **Barras muito diferentes**: Device-specific behavior

**Ideal**: Todos os algoritmos com barras de altura similar entre D1 e D2

---

### Plot 1️⃣4️⃣: **percentile_distribution_analysis.png** ⭐ NOVO
**O que mostra**: Análise de tail latency - pior-caso scenarios

**4 painéis** (Top 4 algoritmos rápidos):
- Cada gráfico mostra curva percentil (0-100%)
- Linha mostra como tempo aumenta nos percentis extremos

**Métricas-chave**:
- **Mean**: Valor médio
- **Median (P50)**: Valor central
- **P95**: 95% das execuções são mais rápidas que isto
- **P99**: 99% das execuções são mais rápidas que isto

**Ideal**: Curva plana (sem degradação em tail)

---

## 📈 RESUMO VISUAL: Qual Gráfico Usar Para Quê?

| Pergunta | Use este Gráfico |
|----------|---|
| Qual algoritmo é mais consistente? | CV Heatmap ou CV Analysis |
| Como cada algoritmo escala com input size? | Scaling Behavior (log-log) |
| Qual é o trade-off energia vs tempo? | Energy-Time Tradeoff Scatter |
| Qual algoritmo é mais rápido em cada device? | Device Performance Comparison |
| Como é distribuída a performance? | Histograms ou Violin Plots |
| Que combinações são piores? | Performance Matrix Heatmap |
| Qual é o pior-caso? | Percentile Distribution |
| Qual algoritmo é "melhor overall"? | Algorithm Ranking Multi-Metric |
| Existe relação entre métricas? | Correlation Heatmap |
| Quanto tempo/energia consome memória? | Memory Usage Analysis |

---

## 🎯 WORKFLOW RECOMENDADO DE ANÁLISE

### Etapa 1: Entender o básico (5 mins)
1. Ver `algorithm_ranking_multi_metric.png`
2. Ver `energy_time_tradeoff_scatter.png`

### Etapa 2: Aprofundar (15 mins)
3. Ver `coefficient_of_variation_analysis.png`
4. Ver `scaling_behavior_analysis.png`
5. Ver `performance_matrix_heatmap.png`

### Etapa 3: Análise técnica detalhada (20 mins)
6. Ver `percentile_distribution_analysis.png`
7. Ver `correlation_metrics_heatmap.png`
8. Ver `memory_usage_analysis.png`

### Etapa 4: Decisões de deployment
9. Ver `device_performance_comparison.png`
10. Ver `speed_vs_stability_analysis.png`

---

## 📍 LOCALIZAÇÃO DOS FICHEIROS

Todos os gráficos estão em:
```
c:\Tese\results\benchmark_analysis_outputs\plots\
```

**Total**: 20+ gráficos (5 originais + 5 estatísticos iniciais + 10 novos avançados)

---

## ✅ MELHORIAS IMPLEMENTADAS

Comparando com a versão anterior:

| Aspecto | Antes | Depois |
|--------|-------|--------|
| Gráficos totais | 11 | 20+ |
| Dimensões analisadas | 2 (Tempo + Energia) | 5+ (Tempo, Energia, Memória, Stability, Throughput) |
| Análise de trade-offs | Básica | Avançada (Pareto, 3D) |
| Scaling analysis | Não | Sim (log-log) |
| Tail latency | Não | Sim (Percentiles) |
| Device comparison | 1 plot | 3+ plots |
| Correlações | Não | Sim (Heatmap) |

---

## 🚀 PRÓXIMAS POSSIBILIDADES

Para análise ainda mais profunda:

- [ ] Heatmap de "Best Algorithm" por combinação (Device × InputSize × Metric)
- [ ] 3D scatter: Tempo vs Energia vs Memória
- [ ] Regressão: Tempo previsto baseado em input size
- [ ] Ranking dinâmico: Qual algoritmo é melhor para cada device
- [ ] Sensitivity analysis: Como mudanças no alpha afectam rankings
- [ ] Outlier timeline: Quando ocorrem slowdowns
- [ ] Cross-platform comparison: PC vs Mobile (mais detalhado)

---

**Análise Completa: Pronta para Publicação e Apresentação! 🎓**
