# Comparação criptográfica entre Windows, Android e Raspberry Pi

**Estado da análise.** Foram comparadas as medições Windows e Android com os dados publicados por Heitor em *Green Security for IoT*. Há seis famílias em comum com dados publicados. O teste AES demonstrou equivalência de bytes entre a implementação JVM do projecto e uma referência independente configurada com os parâmetros do código C. A interoperabilidade com o binário de Heitor no Raspberry Pi continua por testar.

**Fontes.** A referência IoT é o commit [c52648f53f17009ca24259c8acd8efa056369d97](https://github.com/SpeedHammer/Green-Security-for-IoT/tree/c52648f53f17009ca24259c8acd8efa056369d97). Os dados locais provêm do [relatório experimental](Cryptographic-Benchmark-Report-EN.html), com 500 lotes Windows e 470 Android. Os CSV derivados deste anexo conservam ligações para cada ficheiro de origem.

## 1. Comparação dos dados

| Aspecto | Windows | Android | IoT de Heitor |
| --- | --- | --- | --- |
| Ambiente | Portátil Windows 11, i7-12700H, JVM | IV2201, Kotlin, Ebserver e ADB por Wi-Fi | Raspberry Pi 2 Model B v1.1, ARMv7, programas C, segundo a documentação |
| Entradas | 2–1 024 KiB | 2–1 024 KiB, com menor cobertura nos três tamanhos maiores | Ficheiros com rótulos 1, 2, 4, 8, 10 kB |
| Tempo | Média de cinco lotes, em ms por cifragem ou decifragem | Média de cinco lotes, em ms por operação | O tempo entre a primeira e a última amostra descreve o registo de potência |
| Energia | PowerJoular, CPU do computador, J por par de operações | Estimativa provisória da bateria, J por janela de instrumentação | Potência estimada pelo PowerJoular. Há séries do sistema e ficheiros separados com PID |
| Protocolo | Cinco lotes de aproximadamente três segundos por combinação, na mesma JVM | Cinco lotes de 100 pares | Cifragem e decifragem LWC em processos separados; o código C consultado contém ciclos de 10 000 chamadas. Os resultados tradicionais foram recolhidos com outro protocolo |

Há uma aparente divergência na documentação do repositório de Heitor. O README descreve os scripts clássicos actuais como processos separados, mas identifica os resultados clássicos existentes como dados de um protocolo anterior. Os ficheiros de AES e RSA publicados têm um único nome por tamanho e não identificam a operação. Neste anexo são classificados como **séries históricas, operação não atribuída**. [Método, limitações e scripts de Heitor](https://github.com/SpeedHammer/Green-Security-for-IoT/tree/c52648f53f17009ca24259c8acd8efa056369d97/scripts)

### Resultado quantitativo

O CSV IoT resume **50 séries de carga e uma série de repouso**, usando apenas os ficheiros sem sufixo PID. Para cada série apresenta número de amostras, tempo entre extremos, média amostral de `CPU Power` e integração trapezoidal em joules no intervalo observado. A série de repouso tem média de aproximadamente **1,364 W**, de uma sessão separada. Não foi subtraída dos ensaios.

Os tamanhos nominais em comum são 2, 4 e 8. Os ficheiros IoT têm, respectivamente, **2 049, 4 096 e 8 193 bytes** no repositório, enquanto a entrada local é de 2 048, 4 096 e 8 192 bytes. O executável AES C remove um LF final quando presente. Um ficheiro com rótulo igual pode produzir uma carga diferente. A tabela usa o rótulo de 4 kB, que tem 4 096 bytes no ficheiro actual, e indica as métricas disponíveis para cada plataforma.

| Família | Windows enc./dec. ms/op. | Android enc./dec. ms/op. | IoT potência CPU W enc./dec. | IoT intervalo observado s enc./dec. |
| --- | ---: | ---: | ---: | ---: |
| AES-CBC | 0,0495 / 0,0582 | 1,7566 / 1,1639 | 1,594 em série histórica | 23,0 em série histórica |
| RSA / RSA Hybrid | 0,1145 / 1,3339 | 2,4805 / 3,1822 | 1,589 em série histórica | 31,1 em série histórica |
| Ascon | 0,0346 / 0,0441 | 5,7318 / 1,2319 | 1,826 / 1,827 | 28,1 / 28,5 |
| Elephant | 1,4365 / 1,4323 | 12,8334 / 12,8770 | 1,567 / 1,566 | 6 950,8 / 6 882,9 |
| GIFT-COFB | 0,6139 / 0,6212 | 4,5780 / 2,9145 | 1,566 / 1,567 | 432,9 / 432,9 |
| Xoodyak | 0,0475 / 0,0544 | 2,6955 / 1,4522 | 1,566 / 1,566 | 214,6 / 214,3 |

O caso de Elephant mostra por que razão a potência média isolada pode induzir em erro. As séries de cifragem de Elephant e Xoodyak rondam 1,57 W, mas os intervalos observados são cerca de 6 951 s e 215 s, respectivamente. As integrações dão cerca de 10 891 J e 336 J **nas respectivas janelas gravadas**. A duração inclui a fronteira da recolha e as implementações são diferentes. Estes valores não sustentam uma razão de desempenho por operação entre plataformas. [Série Elephant](https://github.com/SpeedHammer/Green-Security-for-IoT/blob/c52648f53f17009ca24259c8acd8efa056369d97/results/elephant_results/elephant_encrypt_input_4kB.csv) · [Série Xoodyak](https://github.com/SpeedHammer/Green-Security-for-IoT/blob/c52648f53f17009ca24259c8acd8efa056369d97/results/xoodyak_results/xoodyak_encrypt_input_4kB.csv)

O CSV de comparação disponibiliza 30 linhas descritivas nos três rótulos em comum. Para AES/RSA conserva o registo IoT histórico sem atribuição de operação. Os tempos Windows/Android por operação estão no CSV local e na tabela acima. A ausência de contadores e fronteiras confirmadas nos registos IoT impede calcular ms/op. ou J/op. validados a partir destas séries. Além disso, a energia Windows mede o CPU por par, enquanto a Android estima o consumo da bateria durante uma janela mais abrangente.

## 2. Consistência e compatibilidade

| Família | Windows e Android | IoT | Avaliação |
| --- | --- | --- | --- |
| AES-CBC | AES-256-CBC com padding; saída Base64 | OpenSSL EVP AES-256-CBC; chave e IV de teste fixos; saída hexadecimal | **Parâmetros alinháveis.** Igualar chave, IV e bytes de entrada e converter a representação. Verificação JVM contra referência independente passou. |
| Ascon | Ascon-AEAD128 da Bouncy Castle | C com bloco de 8 bytes e IV inicial `0x80400c0600000000` | **Variantes diferentes.** Alinhar ambos com a mesma versão antes de testar a troca de dados. |
| Elephant | Elephant200, tag de 16 bytes | C com bloco de 20 bytes e tag de 8 bytes | **Incompatível na configuração actual.** A variante e o comprimento da tag devem coincidir. |
| GIFT-COFB | Bouncy Castle, nonce e tag de 16 bytes | Implementação C local com GIFT-128 e COFB | **Candidato.** Fixar AAD e formato, testar ciphertext e tag com vectores conhecidos e verificar rejeição de tag alterada. |
| Xoodyak | Bouncy Castle, chave, nonce e tag de 16 bytes | C com os mesmos comprimentos e AAD fixos | **Candidato.** Igualar os bytes de AAD, inicialização e serialização antes dos testes cruzados. |
| RSA | RSA-2048 OAEP SHA-256 encapsula material de AES-CBC | RSA educativo em C com primos e aritmética inteira | **Esquemas diferentes.** Para compatibilidade, implementar o mesmo esquema híbrido, padding e formato de envelope. |
| AES-GCM, ChaCha20, ElGamal Hybrid, Grain-128AEAD | Medidos localmente | Sem dados correspondentes no conjunto IoT publicado | Fora da comparação IoT nesta revisão. |

Referências de implementação: [AES C](https://github.com/SpeedHammer/Green-Security-for-IoT/blob/c52648f53f17009ca24259c8acd8efa056369d97/cripto_ciphers/ciphers/AES_Algorithm_encrypt.c), [RSA C](https://github.com/SpeedHammer/Green-Security-for-IoT/blob/c52648f53f17009ca24259c8acd8efa056369d97/cripto_ciphers/ciphers/RSA_algorithm_encrypt.c), [cifras leves C](https://github.com/SpeedHammer/Green-Security-for-IoT/tree/c52648f53f17009ca24259c8acd8efa056369d97/cripto_ciphers/lwc_ciphers) e [especificação Ascon-AEAD128](https://csrc.nist.gov/pubs/sp/800/232/final). O formato AEAD local em `BcLwcAead.kt` é Base64 de nonce || ciphertext || tag. A compatibilidade depende da sequência binária e dos parâmetros antes da codificação. Um teste local de ida e volta não demonstra descodificação cruzada.

### Verificação executada

O programa `AesCbcCompatibility.java` chamou a implementação do JAR usado no Windows e comparou os bytes cifrados com JCE `AES/CBC/PKCS5Padding`, com a chave AES-256 e o IV do código de Heitor. Verificou também a descifragem nas duas direcções em **sete tamanhos**: 0, 15, 16, 17, 2 048, 4 096 e 8 192 bytes. Os sete testes passaram. Os casos em torno de 16 bytes verificam a fronteira do padding. Desktop e Android partilham o código Kotlin de AES; os benchmarks existentes verificam a recuperação do texto no respectivo ambiente. O teste adicional foi executado no JVM Windows, sem executar o binário C ou trocar ficheiros com um Android real. A aceitação da entrada vazia pelo programa C também não foi testada.

## 3. Como fechar a verificação nas três plataformas

1. Começar por AES-256-CBC com um ficheiro de 4 096 bytes sem LF final, uma chave e IV de teste idênticos e padding documentado. Comparar os bytes cifrados após descodificar Base64 ou hexadecimal. Fazer cifragem e descifragem cruzadas entre os três executáveis.
2. Para AEAD, fixar versão, chave, nonce, AAD, tag e representação do envelope. Usar um vector conhecido, uma entrada curta e tamanhos de fronteira. Confirmar rejeição de ciphertext, tag ou AAD alterados. Alinhar primeiro Ascon e Elephant, cujas configurações actuais diferem.
3. Para comparação de desempenho, usar os mesmos ficheiros binários de 2 048, 4 096 e 8 192 bytes, medir cifragem e decifragem separadamente e exportar timestamps, operação, número efectivo de chamadas e versão do binário. Registar o domínio energético e repetir com a mesma regra de aquecimento. Até harmonizar o domínio, publicar métricas energéticas em tabelas distintas.

**Frase para o orientador.** «Já comparei as séries publicadas e verifiquei os parâmetros das implementações. O AES-CBC passou um teste de referência com os parâmetros do código do Heitor. Ascon e Elephant têm diferenças concretas de variante ou de tag, e os dois casos RSA são esquemas diferentes. O passo seguinte é executar vectores comuns nos três binários e recolher medições com fronteiras equivalentes.»

## Ficheiros

- [Resumo das 51 séries IoT](heitor-iot-power-traces.csv)
- [Comparação descritiva dos três tamanhos](desktop-android-iot-comparison.csv)
- [Código do teste AES](AesCbcCompatibility.java) e [saída dos sete testes](aes-cbc-compatibility-check.txt)
