# Barra de orçamento na tela de gastos — Design

**Data:** 2026-06-07
**Status:** Aprovado (visual e regras confirmados com o usuário)

## Objetivo

Exibir, fixada na base da tela de Gastos, uma barra de progresso que representa a
**renda restante** do grupo na competência selecionada. A barra começa cheia (100% da
renda disponível) e **diminui a cada gasto cadastrado**. A cor segue 3 thresholds que
funcionam como um termômetro de consumo da renda.

## Regras de negócio

### Base da renda (o "100%")
- Usa a **renda congelada da competência** (snapshot `RendaMensal`, mesma base do acerto
  de contas): `rendaTotal = soma dos valores do snapshot da competência`.
- **Fallback:** se a competência ainda não tem snapshot (nenhum gasto lançado no mês),
  usa a soma das rendas atuais das pessoas do grupo (`Σ Pessoa.renda`).

### Cálculo
- `totalGasto` = soma dos gastos da competência selecionada.
- `restante` = `max(0, rendaTotal − totalGasto)`.
- `excedente` = `max(0, totalGasto − rendaTotal)` (> 0 apenas no estouro).
- `fracaoRestante` = `clamp(restante / rendaTotal, 0..1)` → tamanho da barra.
- `percentualGasto` = `totalGasto / rendaTotal` (quando há renda).

### Cor (por **% gasto**)
| Condição (% gasto) | Nível | Cor |
|---|---|---|
| `< 60%` | SAUDÁVEL | verde `#18B26B` |
| `60% – 85%` (inclusive) | ATENÇÃO | âmbar `#F59E0B` |
| `> 85%` | CRÍTICO | vermelho `#EF4444` |
| renda total = 0 | SEM_RENDA | cinza `#CBD5E1` |

Limites precisos: `gasto < 0.60` → SAUDÁVEL; `0.60 ≤ gasto ≤ 0.85` → ATENÇÃO;
`gasto > 0.85` → CRÍTICO.

### Casos de borda
- **Estouro** (`totalGasto > rendaTotal`): barra vazia (fração 0) e vermelha; rótulo
  mostra `"Estourou R$ X,XX"`.
- **Sem renda** (`rendaTotal == 0`): barra cinza; rótulo `"Sem renda cadastrada no mês"`.
- **Sem gasto** (`totalGasto == 0`): barra cheia e verde.

## Arquitetura (abordagem escolhida: função de domínio pura + ViewModel + UI burra)

Segue o padrão já usado em `domain/Settlement.kt`: lógica pura testável no domínio, o
ViewModel orquestra o carregamento e popula o estado, a UI apenas desenha.

### 1. Domínio — `domain/Orcamento.kt` (novo)
```kotlin
enum class NivelOrcamento { SAUDAVEL, ATENCAO, CRITICO, SEM_RENDA }

data class OrcamentoMensal(
    val rendaTotal: Money,
    val totalGasto: Money,
    val restante: Money,        // max(0, renda - gasto)
    val excedente: Money,       // max(0, gasto - renda)
    val fracaoRestante: Float,  // 0f..1f  → largura da barra
    val nivel: NivelOrcamento,
)

fun calcularOrcamento(rendaTotal: Money, totalGasto: Money): OrcamentoMensal
```
Função pura, sem dependência de Android. Concentra todas as regras acima.

### 2. Estado — `ui/gastos/GastosUiState.kt`
`Ready` ganha um campo:
```kotlin
data class Ready(
    val gastos: List<Gasto>,
    val pessoas: List<Pessoa>,
    val competencia: String,
    val orcamento: OrcamentoMensal,   // novo
) : GastosUiState
```

### 3. ViewModel — `ui/gastos/GastosViewModel.kt`
`buildReady(competencia)` passa a:
1. carregar `gastos` e `pessoas` (como hoje);
2. carregar o snapshot via `rendaRepo.rendasDe(groupId, competencia)`;
3. derivar `rendaTotal` (snapshot, ou fallback `Σ pessoas.renda`) e `totalGasto`
   (`Σ gastos.valor`);
4. `orcamento = calcularOrcamento(rendaTotal, totalGasto)` e incluir no `Ready`.

Nenhuma escrita nova; só uma leitura adicional (rendas) já disponível no repo.

### 4. UI — `OrcamentoBar` (novo composable) no `bottomBar` do Scaffold
- Renderizado apenas quando `state is Ready`.
- Layout: container estilo bottom bar (fundo da superfície, borda superior sutil),
  uma linha de rótulo + a barra de progresso arredondada.
- Rótulo: `"Restam R$ {restante} de R$ {rendaTotal} · {percentRestante}%"`; no estouro,
  `"Estourou R$ {excedente}"`; sem renda, `"Sem renda cadastrada no mês"`.
- Barra: track cinza + preenchimento `fracaoRestante` na cor do `nivel`.
- A fração anima suavemente em mudanças (`animateFloatAsState`).
- Formatação monetária reaproveita `ui/common/formatReais`.

## Testes

`app/src/test/.../domain/OrcamentoTest.kt` cobrindo `calcularOrcamento`:
- gasto 0% → SAUDÁVEL, fração 1.0;
- 59% e 60% → SAUDÁVEL / ATENÇÃO (limite inferior);
- 85% e 86% → ATENÇÃO / CRÍTICO (limite superior);
- estouro (gasto > renda) → fração 0, excedente correto, CRÍTICO;
- renda 0 → SEM_RENDA, fração 0;
- restante = renda − gasto em valores intermediários.

Sem testes de UI novos (segue o padrão atual: lógica testada no domínio).

## Fora de escopo
- Backend, schema e permissões do Appwrite (nenhuma mudança).
- Configuração/edição dos thresholds pelo usuário (fixos por enquanto).
- Persistência de qualquer estado novo.

## Arquivos tocados
- **Novo:** `app/src/main/java/dev/paraizo/cost/domain/Orcamento.kt`
- **Novo:** `app/src/test/java/dev/paraizo/cost/domain/OrcamentoTest.kt`
- **Novo:** componente `OrcamentoBar` (em `ui/gastos/` ou `ui/common/`)
- **Editado:** `ui/gastos/GastosUiState.kt`, `ui/gastos/GastosViewModel.kt`,
  `ui/gastos/GastosScreen.kt`
