# Plano de Implementação — App de Divisão de Gastos Proporcional à Renda

> Gerado pelo pipeline `lead` (analyzer → clarifier → decisões → planner → detailer).
> Data: 2026-06-06

## Contexto

O projeto `cost` é hoje um scaffold Android single-module (`:app`, namespace `dev.paraizo.cost`) com apenas `MainActivity.kt` (Hello World), sem testes, sem version catalog (versões inline), sem Appwrite SDK e sem permissão de INTERNET. Stack: Kotlin 2.0.21, Compose BOM 2024.12.01, Material3, AGP 8.7.3, Gradle 8.11.1, compileSdk/targetSdk 35, minSdk 24. **Build exige JBR 21** (`JAVA_HOME=/opt/android-studio/jbr`; o JDK 26 do sistema é incompatível).

## Objetivo

Substituir o Hello World por um MVP funcional (online) onde o usuário autentica via Appwrite, gerencia grupos/pessoas/gastos e visualiza o **settle-up mensal** (quem deve quanto a quem), com rateio proporcional à renda e arredondamento exato por maior resto.

## Regras de negócio

- Pessoa = nome + renda. Percentual de contribuição = renda da pessoa / renda total do grupo (soma 100%).
- Pessoas pertencem a um grupo (múltiplos grupos suportados).
- Gasto = descrição + valor + quem pagou (pessoa do grupo) + competência (`YYYY-MM`).
- Settle-up por competência: devido = percentual × total de gastos; comparado ao pago; gera lista "pessoa X deve R$Y para pessoa Z".
- Edge cases tratados como estados de UI: renda total = 0 bloqueia o cálculo; gasto exige pagador; grupo de 1 pessoa; pessoa sem gasto.

## Decisões registradas (rastreáveis)

| ID | Decisão | Justificativa |
|----|---------|---------------|
| D1 | **Auth:** login único Appwrite email/senha. Pessoas são registros de dados (nome+renda), sem vínculo com usuários Appwrite. Permissões fixas para o usuário logado. | Uso pessoal single-user; sessão persistente sem complexidade de multiusuário/permissão por documento. |
| D2 | **Dinheiro:** centavos como `Long` no Appwrite; cálculo em `BigDecimal`; arredondamento por **maior resto** (largest remainder), soma das parcelas == total exato. Desempate de restos: determinístico, menor índice (ordem estável) recebe o centavo. | Elimina erro de ponto flutuante; `Long` é filtrável/ordenável; maior resto garante soma exata sem viés. |
| D3 | **Modelagem Appwrite:** 3 collections — `grupos`(nome); `pessoas`(nome, rendaCentavos, groupId); `gastos`(descricao, valorCentavos, pagadorId, groupId, competencia `YYYY-MM`). Settle-up calculado sob demanda (sem persistir fechamento). | "Pessoas pertencem a um grupo" exige a entidade grupo; persistir fechamento é estado redundante (YAGNI). |
| D4 | **Arquitetura:** MVVM + StateFlow + Compose Navigation; só online (cache em memória via ViewModel, sem Room); MVP completo (login, grupos, pessoas, gastos, settle-up); edge cases como estados de UI. | Padrão idiomático Compose; lógica financeira testável fora da UI; Room é overhead para single-user. |

### Premissas e limitações do MVP
- **Exclusão em cascata FORA do escopo.** Exclusão é simples; remover grupo/pessoa pode deixar gastos órfãos (`groupId`/`pagadorId` são chaves manuais, sem relationship nativo). Documentado como limitação; não implementar lógica de cascata.
- **Contrato renda total = 0 (D2):** `percentuais` e `ratear` retornam todas as parcelas em `Money.ZERO` quando a soma das rendas é 0; a UI bloqueia o cálculo. Fixar isso nos testes de T3.
- **Sem offline:** toda tela depende de chamada remota (D4); perda de conexão degrada a UX (tratada como estado de erro).
- **Sem `androidTest` no escopo mínimo:** lógica de navegação extraída em função pura testável via `testDebugUnitTest`. UI tests reais exigiriam deps + emulador (fora do MVP).

## Arquivos afetados

- `gradle/libs.versions.toml` (novo), `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`
- `app/src/main/AndroidManifest.xml`, `res/values/{strings,themes}.xml`
- `app/src/main/java/dev/paraizo/cost/MainActivity.kt` (remover Greeting; virar host)
- `app/src/main/java/dev/paraizo/cost/domain/` (novo — modelos + cálculo)
- `app/src/main/java/dev/paraizo/cost/data/` (novo — Appwrite client, DTOs, repositórios)
- `app/src/main/java/dev/paraizo/cost/ui/` (novo — telas, ViewModels, navegação, common)
- `app/src/test/java/dev/paraizo/cost/` (novo — testes unitários)
- `docs/appwrite-setup.md` (novo — instruções de configuração do backend)

## Tasks

| ID | Título | Esforço | Depende de |
|----|--------|---------|------------|
| T1 | Limpar scaffold + setup de build (version catalog, Appwrite SDK, test deps, INTERNET) | M | — |
| T2 | Instruções de configuração do Appwrite (schema das 3 collections + permissões) | P | — |
| T3 | Modelos de domínio + cálculo proporcional e settle-up (núcleo de corretude, testado) | M | — |
| T4 | Camada de dados Appwrite (client + DTOs + repositórios CRUD) | G | T1, T2 |
| T5 | Autenticação Appwrite (login email/senha, ViewModel) | M | T4 |
| T6 | Navegação + host logado/deslogado (Compose Navigation) | M | T5 |
| T7 | Tela de Grupos (listar/criar/selecionar) | M | T6 |
| T8 | Tela de Pessoas por grupo (renda + percentual via T3) | G | T7, T3 |
| T9 | Tela de Gastos por grupo/competência (pagador obrigatório) | G | T7, T8 |
| T10 | Tela de Settle-up mensal (devido vs pago + transferências) | M | T3, T8, T9 |

> Dependências lógicas explícitas (ajuste do detailer): T8 usa `percentuais` (T3); T9 reusa `MoneyInput` de T8; T10 usa `settleUp` (T3).

---

## Detalhamento das tasks

### T1 — Limpar scaffold + setup de build  ·  Esforço M  ·  dep: —
**Por quê:** todas as tasks dependem de um build limpo que resolva o Appwrite SDK e rode testes JVM.
**Objetivo:** `assembleDebug` compila com JBR21, app abre sem crash, Appwrite SDK + libs de teste resolvidos via version catalog, **e package renomeado para `dev.paraizo.cost`** (D5, igual à plataforma Appwrite).
**Arquivos:** `gradle/libs.versions.toml` (novo), `build.gradle.kts`, `app/build.gradle.kts` (namespace + applicationId → `dev.paraizo.cost`), mover `app/src/main/java/com/felipe/cost/MainActivity.kt` → `app/src/main/java/dev/paraizo/cost/MainActivity.kt` (ajustar `package`, remover Greeting/GreetingPreview), `AndroidManifest.xml` (+`uses-permission INTERNET`), `res/values/{themes,strings}.xml`.
**Rename (D5):** após a troca, `grep -rn "com.felipe.cost\|com/felipe/cost" app/` deve ser vazio; o diretório `app/src/main/java/com/` não deve mais existir.
**Preview:**
```toml
# gradle/libs.versions.toml
[versions]
appwrite = "7.0.1"   # SDK Android oficial — coordenada informada pela própria app Appwrite
[libraries]
appwrite-sdk = { group = "io.appwrite", name = "sdk-for-android", version.ref = "appwrite" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test-junit", version.ref = "kotlin" }
```
**Testes:** opcional `BuildSanityTest` para validar que o source set de teste roda.
**Aceite:** `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL; `./gradlew dependencies` lista `io.appwrite`; `grep -R "Greeting" app/src` vazio; app abre sem crash.
**Proibido:** criar pacotes `domain/`, `data/`, telas (T3/T4/T5+).

### T2 — Instruções de configuração do Appwrite  ·  Esforço P  ·  dep: —
**Por quê:** a app não cria schema; sem guia exato, T4 falha por mismatch de atributos/permissões.
**Objetivo:** `docs/appwrite-setup.md` permitindo criar manualmente projeto/database/collections/atributos/índices/permissões em `backend.paraizo.dev/v1`, e o app conectar sem ajuste.
**Conteúdo obrigatório:**
- Endpoint `https://backend.paraizo.dev/v1`; onde obter ProjectID, DatabaseID e os 3 CollectionIDs.
- Atributos exatos (D3): `grupos`(nome:string 255 req); `pessoas`(nome:string req, rendaCentavos:integer 64-bit req min0, groupId:string req); `gastos`(descricao:string req, valorCentavos:integer req min0, pagadorId:string req, groupId:string req, competencia:string 7 req `YYYY-MM`).
- Índices: `groupId` (pessoas/gastos), `competencia` (gastos) — ou composto `groupId+competencia`.
- Auth: habilitar Email/Password.
- Permissões fixas para o usuário logado (D1); restringir plataforma Android (`dev.paraizo.cost`).
- Nomes de constante que T4 consumirá (ENDPOINT, PROJECT_ID, DATABASE_ID, COLLECTION_GRUPOS/PESSOAS/GASTOS).
**Já existe:** projeto Appwrite `cost` (ID `6a248ab90027fe50cfbf`) e plataforma Android registrada. T2 foca em: criar **Database**, as **3 collections** (atributos/índices), habilitar **Email/Password** e ajustar **permissões**.
**Aceite:** atributos conferem 1:1 com D3; índices especificados; permissões e plataforma descritas; nomes de config batem com T4.
**Proibido:** código de cliente (é T4).

### T3 — Modelos de domínio + cálculo proporcional e settle-up  ·  Esforço M  ·  dep: —
**Por quê:** núcleo de corretude (D2). Kotlin puro, testável em JVM, sem Appwrite/Compose.
**Arquivos:** `domain/Money.kt`, `domain/Model.kt`, `domain/Settlement.kt`; testes `test/.../domain/{SettlementTest,MoneyTest}.kt`.
**Contrato (público):**
```kotlin
@JvmInline value class Money(val cents: Long) { /* +, -, toBigDecimal, ZERO */ }
data class Grupo(val id: String, val nome: String)
data class Pessoa(val id: String, val nome: String, val renda: Money, val groupId: String)
data class Gasto(val id: String, val descricao: String, val valor: Money,
                 val pagadorId: String, val groupId: String, val competencia: String)
data class Transferencia(val deId: String, val paraId: String, val valor: Money)
data class SettleResult(
    val devidoPorPessoa: Map<String, Money>, val pagoPorPessoa: Map<String, Money>,
    val saldoPorPessoa: Map<String, Money>,  // + a receber, - a pagar
    val transferencias: List<Transferencia>)

fun percentuais(pessoas: List<Pessoa>): Map<String, BigDecimal>          // renda total 0 → todos ZERO
fun ratear(total: Money, pessoas: List<Pessoa>): Map<String, Money>      // maior resto; soma == total
fun settleUp(pessoas: List<Pessoa>, gastos: List<Gasto>, competencia: String): SettleResult
```
**Testes/casos:** soma percentuais ≈100%; `ratear` com restos → soma das parcelas == total exato; 3 rendas iguais + total ímpar → centavo extra ao menor índice (determinístico); `total=0` → tudo ZERO; **renda total=0 → tudo ZERO** (sem dividir por zero); grupo de 1 pessoa → transferências vazias, devido==total; pessoa sem gasto → aparece devedora; soma das transferências zera saldos.
**Aceite:** `./gradlew testDebugUnitTest --tests "dev.paraizo.cost.domain.*"` verde; invariante soma==total em todos os casos; `grep` por `io.appwrite|androidx.compose` em `domain/` vazio.
**Proibido:** qualquer import de Appwrite/Compose em `domain/`.

### T4 — Camada de dados Appwrite  ·  Esforço G  ·  dep: T1, T2
**Por quê:** centraliza o backend (D3/D1), mapeia centavos↔`Money` e isola DTOs das entidades de T3.
**Arquivos:** `data/AppwriteConfig.kt`, `data/AppwriteClient.kt`, `data/dto/Dtos.kt`, `data/{Grupo,Pessoa,Gasto}Repository.kt`; teste `test/.../data/MapperTest.kt`.
**Valores reais (confirmados):** SDK `io.appwrite:sdk-for-android:7.0.1`; ENDPOINT `https://backend.paraizo.dev/v1`; PROJECT_ID `6a248ab90027fe50cfbf`; DATABASE_ID `6a249b5e00222dff6baf`; COLLECTION_GRUPOS `6a249ba30003f86d75e1`; COLLECTION_PESSOAS `6a249bf8000bf31fb56b`; COLLECTION_GASTOS `6a249c89001da73ade1a`. Package do app = `dev.paraizo.cost` (igual à plataforma Appwrite). Certificado é válido (Google Trust Services) → **NÃO** usar `setSelfSigned(true)`. Init:
```kotlin
val client = Client(context)
    .setEndpoint(AppwriteConfig.ENDPOINT)
    .setProject(AppwriteConfig.PROJECT_ID)   // sem setSelfSigned: cert válido
```
**Contrato (público):**
```kotlin
class AppwriteClient(context: Context) { val client; val account: Account; val databases: Databases }
class GrupoRepository(db) { suspend create(Grupo):Grupo; list():List<Grupo>; update(Grupo); delete(id) }
class PessoaRepository(db){ suspend create(Pessoa); listByGroup(groupId):List<Pessoa>; update; delete(id) }
class GastoRepository(db) { suspend create(Gasto); listByGroupAndCompetencia(groupId,competencia):List<Gasto>; update; delete(id) }
```
**Testes:** `MapperTest` (sem rede) — round-trip preserva centavos: `Money(12345).toData()…toGasto().valor.cents == 12345`; valor 0; competencia `YYYY-MM`. Repositórios: smoke manual contra backend após T2 (não automatizar no MVP).
**Aceite:** repositórios expõem `suspend` create/list/update/delete com tipos de domínio na fronteira; `listByGroupAndCompetencia` filtra por ambos (Query.equal); `AppwriteConfig` concentra ENDPOINT/projectId/databaseId/3 collectionIds.
**Proibido:** tocar em `domain/`; adicionar Compose/ViewModel.
**Nota:** SDK e config confirmados (`sdk-for-android:7.0.1`, projectId real, cert válido). Validar apenas a API concreta da 7.x (`Client/Account/Databases`, `Query.equal`, `createDocument`) durante a implementação.

### T5 — Autenticação Appwrite  ·  Esforço M  ·  dep: T4
**Por quê:** D1 fixa login único; a sessão governa acesso à área logada (T6).
**Arquivos:** `ui/auth/{AuthViewModel,LoginScreen,AuthUiState}.kt`; teste `test/.../ui/auth/AuthViewModelTest.kt`.
**Contrato:**
```kotlin
sealed interface AuthState { Loading; LoggedOut; LoggedIn; data class Error(message) }
class AuthViewModel(account: Account) { val state: StateFlow<AuthState>; checkSession(); login(email,senha); logout() }
```
**Testes:** extrair `AuthGateway` (interface fina sobre `Account`) p/ testar sem rede — login ok→LoggedIn; erro→Error; sessão ativa→LoggedIn; logout→LoggedOut.
**Aceite:** válidas→LoggedIn; inválidas→Error(msg); checkSession detecta sessão; logout→LoggedOut.
**Proibido:** definir NavHost (T6); criar repositórios (T4).

### T6 — Navegação + host logado/deslogado  ·  Esforço M  ·  dep: T5
**Por quê:** D4 (Compose Navigation); esqueleto onde T7–T10 plugam.
**Arquivos:** `ui/nav/{AppNav,Routes}.kt`, `MainActivity.kt` (montar AppRoot), `app/build.gradle.kts` (+`androidx.navigation:navigation-compose`).
**Contrato:**
```kotlin
object Routes { LOGIN; GRUPOS; PESSOAS="grupos/{groupId}/pessoas"; GASTOS; SETTLE; fun pessoas(id)/gastos(id)/settle(id) }
@Composable fun AppNav(authVm: AuthViewModel)
fun startDestinationFor(state: AuthState): String   // testável em JVM
```
**Testes:** `startDestinationFor(LoggedOut)==LOGIN`; `(LoggedIn)==GRUPOS`; logout limpa back stack (popUpTo inclusive); `Routes.pessoas("g1")=="grupos/g1/pessoas"`.
**Aceite:** sem sessão→login; com sessão→grupos; logout volta a login sem permitir voltar; rotas com groupId corretas.
**Proibido:** implementar conteúdo de T7–T10 (só placeholders).

### T7 — Tela de Grupos  ·  Esforço M  ·  dep: T6
**Arquivos:** `ui/grupos/{GruposViewModel,GruposScreen,GruposUiState}.kt`, `ui/nav/AppNav.kt` (parcial).
**Contrato:**
```kotlin
sealed interface GruposUiState { Loading; data class Ready(grupos:List<Grupo>); data class Error(message) }
class GruposViewModel(repo: GrupoRepository) { val state; load(); criar(nome) }
```
**Testes:** load→Ready; vazio→Ready(emptyList); erro→Error; criar(nome válido)→repo.create+reload; criar("")→não persiste. (fake repo via interface)
**Aceite:** criar grupo persiste e lista; selecionar habilita telas dependentes; estados vazio/loading/erro.
**Proibido:** lógica de cascata ao deletar (premissa: exclusão simples).

### T8 — Tela de Pessoas por grupo  ·  Esforço G  ·  dep: T7, T3
**Arquivos:** `ui/pessoas/{PessoasViewModel,PessoasScreen,PessoasUiState}.kt`, `ui/common/MoneyInput.kt` (reusado por T9), `ui/nav/AppNav.kt` (parcial).
**Contrato:**
```kotlin
data class PessoaRow(val pessoa: Pessoa, val percentual: BigDecimal)
sealed interface PessoasUiState { Loading; data class Ready(rows, rendaTotalZero:Boolean); data class Error(message) }
class PessoasViewModel(repo, groupId) { val state; load(); salvar(nome, rendaCentavos); editar(...) }
// ui/common: fun parseCentavos(input):Long; fun formatReais(Money):String; @Composable MoneyField(...)
```
**Testes:** 3 pessoas→percentuais somam ~100% (usa T3); renda total 0→rendaTotalZero=true, percentuais 0; adicionar→reload+recalcula; `parseCentavos("12,34")==1234`, `("")==0`, `formatReais(Money(1234))=="R$ 12,34"`; nome vazio→bloqueia.
**Aceite:** soma percentuais==100% com renda>0; renda 0 sinaliza bloqueio sem dividir por zero; persiste e recalcula.
**Proibido:** reimplementar cálculo de percentual na UI (usar `percentuais` de T3).

### T9 — Tela de Gastos por grupo/competência  ·  Esforço G  ·  dep: T7, T8
**Arquivos:** `ui/gastos/{GastosViewModel,GastosScreen,GastosUiState}.kt`, `ui/nav/AppNav.kt` (parcial).
**Contrato:**
```kotlin
class GastosViewModel(gastoRepo, pessoaRepo, groupId) {
    val state; selecionarCompetencia(comp /*YYYY-MM*/); criar(descricao, valorCentavos, pagadorId?, competencia) }
```
**Testes:** criar válido→persiste e aparece; pagador null→não persiste, erro; filtro: gastos "2026-05" não aparecem em "2026-06"; descricao vazia→bloqueia; valor não-negativo.
**Aceite:** gasto sem pagador bloqueado; `selecionarCompetencia` chama `listByGroupAndCompetencia`; pagadores vêm das pessoas do grupo (não texto livre).
**Proibido:** `MoneyInput` divergente (importar de `ui/common`); calcular settle-up aqui (T10).

### T10 — Tela de Settle-up mensal  ·  Esforço M  ·  dep: T3, T8, T9
**Arquivos:** `ui/settle/{SettleViewModel,SettleScreen,SettleUiState}.kt`, `ui/nav/AppNav.kt` (parcial).
**Contrato:**
```kotlin
enum class BlockReason { RENDA_TOTAL_ZERO, SEM_PESSOAS }
sealed interface SettleUiState { Loading; data class Ready(result:SettleResult, pessoasById:Map<String,Pessoa>);
                                 data class Blocked(reason:BlockReason); data class Error(message) }
class SettleViewModel(pessoaRepo, gastoRepo, groupId) { val state; carregar(competencia) } // chama settleUp de T3
```
**Testes:** pessoas+gastos→Ready com transferências que equilibram o rateio; renda 0→Blocked(RENDA_TOTAL_ZERO); 1 pessoa→Ready sem transferências; pessoa sem gasto→devedora; sem pessoas→Blocked(SEM_PESSOAS).
**Aceite:** soma das transferências equilibra o rateio exato; bordas renderizam mensagem dedicada; nomes via `pessoasById`.
**Proibido:** duplicar cálculo (vem de `domain`/T3); persistir fechamento (D3).

---

## Riscos principais

1. **Corretude do arredondamento (T3)** — maior resto com desempate determinístico; mitigado por testes que verificam soma==total e determinismo. Fixar o contrato de renda total=0 (tudo ZERO).
2. **Appwrite SDK (T1/T4)** — ✅ resolvido: `io.appwrite:sdk-for-android:7.0.1`, projectId `6a248ab90027fe50cfbf`, endpoint `https://backend.paraizo.dev/v1`, cert válido (sem `setSelfSigned`). Resta validar a API concreta da 7.x na implementação.
3. **T2 fora do controle do app** — schema/permissões criados manualmente; divergência quebra T4 em runtime (não em compilação). Aceite de T4 só verificável após T2 aplicada no backend.
4. **Integridade referencial sem cascata** — exclusões podem deixar gastos órfãos (limitação aceita do MVP).
5. **Build com toolchain** — sempre `JAVA_HOME=/opt/android-studio/jbr` ao rodar gradle, senão o aceite falha por motivo não relacionado ao código.

## Histórico de iterações

- **2026-06-06 — Dados reais do Appwrite.** Usuário forneceu as instruções da app `cost` criada no console. Ajustes: SDK corrigido de `sdk-for-kotlin:9.0.0` (suposição) para **`io.appwrite:sdk-for-android:7.0.1`** (Android); registrados PROJECT_ID `6a248ab90027fe50cfbf` e endpoint `https://backend.paraizo.dev/v1`; verificado que o certificado TLS é válido (Google Trust Services) → **sem `setSelfSigned`**; risco do SDK rebaixado para "resolvido"; T2 ajustada (projeto/plataforma já existem, foco em Database/collections/auth/permissões).
- **2026-06-06 — T2 concluída + rename de package + IDs.** Guia `docs/appwrite-setup.md` publicado. Usuário criou Database e collections e enviou os IDs: DATABASE_ID `6a249b5e00222dff6baf`, COLLECTION_GRUPOS `6a249ba30003f86d75e1`, COLLECTION_PESSOAS `6a249bf8000bf31fb56b`, COLLECTION_GASTOS `6a249c89001da73ade1a`. Decisão **D5**: renomear o package do app de `com.felipe.cost` para **`dev.paraizo.cost`** (igual à plataforma Appwrite registrada — packages precisam coincidir). Rename do código será executado na **T1**. Todas as referências no plano e na doc atualizadas.
