# Barra de orçamento na tela de gastos — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar uma barra fixa na base da tela de Gastos que mostra a renda restante do grupo na competência e muda de cor (verde/amarela/vermelha) conforme o % gasto.

**Architecture:** Função pura `calcularOrcamento` no domínio (testável, segue o padrão de `Settlement.kt`); `GastosViewModel.buildReady` carrega o snapshot de renda e popula `OrcamentoMensal` no estado; o composable `OrcamentoBar` no `bottomBar` do Scaffold apenas desenha.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), JUnit + kotlin.test + kotlinx-coroutines-test.

**Comando base de teste (todas as tasks):**
```bash
export JAVA_HOME=/opt/android-studio/jbr
```

---

### Task 1: Domínio — `calcularOrcamento`

**Files:**
- Create: `app/src/main/java/dev/paraizo/cost/domain/Orcamento.kt`
- Test: `app/src/test/java/dev/paraizo/cost/domain/OrcamentoTest.kt`

- [ ] **Step 1: Escrever o teste que falha**

Create `app/src/test/java/dev/paraizo/cost/domain/OrcamentoTest.kt`:
```kotlin
package dev.paraizo.cost.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class OrcamentoTest {

    @Test
    fun semGastoBarraCheiaVerde() {
        val o = calcularOrcamento(Money(1000), Money.ZERO)
        assertEquals(Money(1000), o.restante)
        assertEquals(Money.ZERO, o.excedente)
        assertEquals(1.0f, o.fracaoRestante, 0.001f)
        assertEquals(NivelOrcamento.SAUDAVEL, o.nivel)
    }

    @Test
    fun gasto59PorCentoSaudavel() {
        val o = calcularOrcamento(Money(1000), Money(590))
        assertEquals(NivelOrcamento.SAUDAVEL, o.nivel)
        assertEquals(Money(410), o.restante)
        assertEquals(0.41f, o.fracaoRestante, 0.001f)
    }

    @Test
    fun gasto60PorCentoAtencao() {
        val o = calcularOrcamento(Money(1000), Money(600))
        assertEquals(NivelOrcamento.ATENCAO, o.nivel)
    }

    @Test
    fun gasto85PorCentoAtencao() {
        val o = calcularOrcamento(Money(1000), Money(850))
        assertEquals(NivelOrcamento.ATENCAO, o.nivel)
    }

    @Test
    fun gasto86PorCentoCritico() {
        val o = calcularOrcamento(Money(1000), Money(851))
        assertEquals(NivelOrcamento.CRITICO, o.nivel)
    }

    @Test
    fun estouroZeraBarraECalculaExcedente() {
        val o = calcularOrcamento(Money(1000), Money(1200))
        assertEquals(Money.ZERO, o.restante)
        assertEquals(Money(200), o.excedente)
        assertEquals(0.0f, o.fracaoRestante, 0.001f)
        assertEquals(NivelOrcamento.CRITICO, o.nivel)
    }

    @Test
    fun rendaZeroResultaSemRenda() {
        val o = calcularOrcamento(Money.ZERO, Money(500))
        assertEquals(NivelOrcamento.SEM_RENDA, o.nivel)
        assertEquals(0.0f, o.fracaoRestante, 0.001f)
        assertEquals(Money.ZERO, o.restante)
        assertEquals(Money.ZERO, o.excedente)
    }

    @Test
    fun valoresIntermediarios() {
        val o = calcularOrcamento(Money(1000), Money(300))
        assertEquals(Money(700), o.restante)
        assertEquals(0.7f, o.fracaoRestante, 0.001f)
        assertEquals(NivelOrcamento.SAUDAVEL, o.nivel)
    }
}
```

- [ ] **Step 2: Rodar o teste e ver falhar (não compila)**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.paraizo.cost.domain.OrcamentoTest"`
Expected: FALHA de compilação ("unresolved reference: calcularOrcamento / NivelOrcamento / OrcamentoMensal").

- [ ] **Step 3: Implementar o domínio**

Create `app/src/main/java/dev/paraizo/cost/domain/Orcamento.kt`:
```kotlin
package dev.paraizo.cost.domain

/** Termômetro de consumo da renda no mês. Verde/amarela/vermelha por % gasto; cinza sem renda. */
enum class NivelOrcamento { SAUDAVEL, ATENCAO, CRITICO, SEM_RENDA }

data class OrcamentoMensal(
    val rendaTotal: Money,
    val totalGasto: Money,
    val restante: Money,        // max(0, renda - gasto)
    val excedente: Money,       // max(0, gasto - renda)
    val fracaoRestante: Float,  // 0f..1f -> largura da barra
    val nivel: NivelOrcamento,
)

/**
 * Calcula o estado do orçamento mensal. A barra representa a renda RESTANTE;
 * a cor segue o % JÁ GASTO: <60% saudável, 60%–85% atenção, >85% crítico.
 */
fun calcularOrcamento(rendaTotal: Money, totalGasto: Money): OrcamentoMensal {
    if (rendaTotal.cents <= 0L) {
        return OrcamentoMensal(
            rendaTotal = rendaTotal,
            totalGasto = totalGasto,
            restante = Money.ZERO,
            excedente = Money.ZERO,
            fracaoRestante = 0f,
            nivel = NivelOrcamento.SEM_RENDA,
        )
    }
    val restanteCents = (rendaTotal.cents - totalGasto.cents).coerceAtLeast(0L)
    val excedenteCents = (totalGasto.cents - rendaTotal.cents).coerceAtLeast(0L)
    val fracao = (restanteCents.toDouble() / rendaTotal.cents.toDouble()).toFloat().coerceIn(0f, 1f)
    val percentualGasto = totalGasto.cents.toDouble() / rendaTotal.cents.toDouble()
    val nivel = when {
        percentualGasto < 0.60 -> NivelOrcamento.SAUDAVEL
        percentualGasto <= 0.85 -> NivelOrcamento.ATENCAO
        else -> NivelOrcamento.CRITICO
    }
    return OrcamentoMensal(
        rendaTotal = rendaTotal,
        totalGasto = totalGasto,
        restante = Money(restanteCents),
        excedente = Money(excedenteCents),
        fracaoRestante = fracao,
        nivel = nivel,
    )
}
```

- [ ] **Step 4: Rodar o teste e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.paraizo.cost.domain.OrcamentoTest"`
Expected: PASS (8 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/paraizo/cost/domain/Orcamento.kt app/src/test/java/dev/paraizo/cost/domain/OrcamentoTest.kt
git commit -m "feat(domain): calcularOrcamento para a barra de orcamento mensal"
```

---

### Task 2: Estado + ViewModel populando o orçamento

**Files:**
- Modify: `app/src/main/java/dev/paraizo/cost/ui/gastos/GastosUiState.kt`
- Modify: `app/src/main/java/dev/paraizo/cost/ui/gastos/GastosViewModel.kt:144-147`
- Test: `app/src/test/java/dev/paraizo/cost/ui/gastos/GastosViewModelTest.kt`

- [ ] **Step 1: Escrever os testes que falham**

Adicionar estes dois testes dentro da classe `GastosViewModelTest` (antes da última `}` da classe, junto aos demais `@Test`). Também adicionar o import no topo do arquivo: `import dev.paraizo.cost.domain.NivelOrcamento`.
```kotlin
    @Test
    fun orcamentoUsaSnapshotQuandoExiste() = runTest(testDispatcher) {
        // snapshot congelado: renda total 1000; gasto 600 = 60% -> ATENCAO
        rendaRepo.snapshots = mutableMapOf("2026-06" to mapOf("p1" to 600L, "p2" to 400L))
        gastoRepo.gastosPorCompetencia = mapOf(
            "2026-06" to listOf(Gasto("b", "Luz", Money(600), "p1", "g1", "2026-06"))
        )
        viewModel.selecionarCompetencia("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GastosUiState.Ready)
        assertEquals(Money(1000), state.orcamento.rendaTotal)
        assertEquals(Money(600), state.orcamento.totalGasto)
        assertEquals(Money(400), state.orcamento.restante)
        assertEquals(NivelOrcamento.ATENCAO, state.orcamento.nivel)
    }

    @Test
    fun orcamentoUsaRendaAtualQuandoSemSnapshot() = runTest(testDispatcher) {
        pessoaRepo.pessoas = listOf(Pessoa("p1", "Ana", Money(1000), "g1"))
        gastoRepo.gastosPorCompetencia = mapOf(
            "2026-06" to listOf(Gasto("b", "Luz", Money(300), "p1", "g1", "2026-06"))
        )
        viewModel.selecionarCompetencia("2026-06")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is GastosUiState.Ready)
        assertEquals(Money(1000), state.orcamento.rendaTotal)
        assertEquals(NivelOrcamento.SAUDAVEL, state.orcamento.nivel)
    }
```

- [ ] **Step 2: Rodar e ver falhar (não compila)**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.paraizo.cost.ui.gastos.GastosViewModelTest"`
Expected: FALHA de compilação ("unresolved reference: orcamento").

- [ ] **Step 3: Adicionar o campo ao estado**

Em `app/src/main/java/dev/paraizo/cost/ui/gastos/GastosUiState.kt`, adicionar o import e o campo `orcamento` ao `Ready`:
```kotlin
package dev.paraizo.cost.ui.gastos

import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.OrcamentoMensal
import dev.paraizo.cost.domain.Pessoa

sealed interface GastosUiState {
    data object Loading : GastosUiState
    data class Ready(
        val gastos: List<Gasto>,
        val pessoas: List<Pessoa>,
        val competencia: String,
        val orcamento: OrcamentoMensal,
    ) : GastosUiState
    data class Error(val message: String) : GastosUiState
}
```

- [ ] **Step 4: Popular o orçamento no `buildReady`**

Em `app/src/main/java/dev/paraizo/cost/ui/gastos/GastosViewModel.kt`, adicionar o import `import dev.paraizo.cost.domain.calcularOrcamento` no topo e substituir a função `buildReady` (linhas ~144-147) por:
```kotlin
    private suspend fun buildReady(competencia: String): GastosUiState.Ready {
        val gastos = gastoRepo.listByGroupAndCompetencia(groupId, competencia)
        val pessoas = pessoaRepo.listByGroup(groupId)
        val totalGasto = gastos.fold(Money.ZERO) { acc, g -> acc + g.valor }
        // Base da renda: snapshot congelado da competência; se ainda não há, soma das rendas atuais.
        val snapshot = rendaRepo.rendasDe(groupId, competencia)
        val rendaTotalCents = if (snapshot.isNotEmpty()) {
            snapshot.values.sum()
        } else {
            pessoas.fold(0L) { acc, p -> acc + p.renda.cents }
        }
        val orcamento = calcularOrcamento(Money(rendaTotalCents), totalGasto)
        return GastosUiState.Ready(
            gastos = gastos,
            pessoas = pessoas,
            competencia = competencia,
            orcamento = orcamento,
        )
    }
```

- [ ] **Step 5: Rodar o teste e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.paraizo.cost.ui.gastos.GastosViewModelTest"`
Expected: PASS (todos, incluindo os 2 novos).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/paraizo/cost/ui/gastos/GastosUiState.kt app/src/main/java/dev/paraizo/cost/ui/gastos/GastosViewModel.kt app/src/test/java/dev/paraizo/cost/ui/gastos/GastosViewModelTest.kt
git commit -m "feat(gastos): popular OrcamentoMensal no estado da tela"
```

---

### Task 3: Componente `OrcamentoBar` + integração no Scaffold

**Files:**
- Create: `app/src/main/java/dev/paraizo/cost/ui/gastos/OrcamentoBar.kt`
- Modify: `app/src/main/java/dev/paraizo/cost/ui/gastos/GastosScreen.kt` (adicionar `bottomBar` ao Scaffold)

Sem teste unitário novo (segue o padrão do projeto: UI burra, lógica já testada no domínio). A verificação é por compilação + visual.

- [ ] **Step 1: Criar o composable da barra**

Create `app/src/main/java/dev/paraizo/cost/ui/gastos/OrcamentoBar.kt`:
```kotlin
package dev.paraizo.cost.ui.gastos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.paraizo.cost.domain.NivelOrcamento
import dev.paraizo.cost.domain.OrcamentoMensal
import dev.paraizo.cost.ui.common.formatReais
import dev.paraizo.cost.ui.theme.RateioTheme
import kotlin.math.roundToInt

@Composable
fun OrcamentoBar(orcamento: OrcamentoMensal, modifier: Modifier = Modifier) {
    val extras = RateioTheme.extras

    val cor = when (orcamento.nivel) {
        NivelOrcamento.SAUDAVEL -> Color(0xFF18B26B)
        NivelOrcamento.ATENCAO -> Color(0xFFF59E0B)
        NivelOrcamento.CRITICO -> Color(0xFFEF4444)
        NivelOrcamento.SEM_RENDA -> Color(0xFFCBD5E1)
    }

    val fracao by animateFloatAsState(targetValue = orcamento.fracaoRestante, label = "orcamentoFracao")

    val rotulo = when {
        orcamento.nivel == NivelOrcamento.SEM_RENDA -> "Sem renda cadastrada no mês"
        orcamento.excedente.cents > 0L -> "Estourou ${formatReais(orcamento.excedente)}"
        else -> {
            val pct = (orcamento.fracaoRestante * 100).roundToInt()
            "Restam ${formatReais(orcamento.restante)} de ${formatReais(orcamento.rendaTotal)} · $pct%"
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = extras.textMuted.copy(alpha = 0.2f))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(rotulo, color = extras.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE5E7EB)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = fracao.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(cor),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Ligar a barra no `bottomBar` do Scaffold**

Em `app/src/main/java/dev/paraizo/cost/ui/gastos/GastosScreen.kt`, no `Scaffold(...)`, adicionar o parâmetro `bottomBar` logo após o bloco `topBar = { ... },` (mesmo nível de `floatingActionButton`). `OrcamentoBar` está no mesmo pacote, não precisa import.
```kotlin
        bottomBar = {
            if (state is GastosUiState.Ready) {
                OrcamentoBar(orcamento = state.orcamento)
            }
        },
```

- [ ] **Step 3: Compilar o app**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Rodar a suíte de testes completa (garantir nada quebrou)**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (todos os testes passam).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/paraizo/cost/ui/gastos/OrcamentoBar.kt app/src/main/java/dev/paraizo/cost/ui/gastos/GastosScreen.kt
git commit -m "feat(gastos): barra de orcamento fixa na base da tela"
```

---

### Verificação final (visual, opcional)

- [ ] Gerar o APK e instalar para conferir a barra nos 3 níveis cadastrando gastos:

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug
# instalar: adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Conferir: barra verde com poucos gastos; vira âmbar ao passar de 60% da renda; vermelha acima de 85%; "Estourou R$ X" quando os gastos passam da renda; animação suave ao cadastrar.
