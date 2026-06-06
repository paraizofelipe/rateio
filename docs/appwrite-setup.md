# Configuração do Appwrite — projeto `cost`

Guia para criar manualmente, no Console do Appwrite, a base de dados e as permissões que o app Android vai consumir. Siga na ordem. Ao final, você terá os IDs que vão para `AppwriteConfig` (task T4).

> **Já existe:** projeto `cost` e plataforma Android. Não precisa recriar.
>
> | Item | Valor |
> |------|-------|
> | Endpoint | `https://backend.paraizo.dev/v1` |
> | Project ID | `6a248ab90027fe50cfbf` |
> | Plataforma Android | package `dev.paraizo.cost` (já registrada) |
> | SDK | `io.appwrite:sdk-for-android:7.0.1` |
> | Certificado TLS | válido (Google Trust Services) → **NÃO** usar `setSelfSigned(true)` |

---

## 1. Acessar o projeto

1. Abra o Console: **https://backend.paraizo.dev**
2. Faça login e entre no projeto **cost** (ID `6a248ab90027fe50cfbf`).

## 2. Habilitar autenticação Email/Senha (decisão D1)

1. Menu lateral → **Auth**.
2. Aba **Settings** → seção **Auth Methods**.
3. Garanta que **Email/Password** está **habilitado** (toggle ligado).
4. Aba **Security** (opcional): mantenha **Sessions limit** padrão. Como é uso pessoal, não precisa mexer.

> Você criará seu usuário (email + senha) no primeiro login pelo app — ou pode criar manualmente em **Auth → Users → Create user** para já testar.

## 3. Criar a Database

1. Menu lateral → **Databases** → **Create database**.
2. **Name:** `cost-db` (o nome é livre).
3. Após criar, **copie o Database ID** → anote como `DATABASE_ID`.

## 4. Criar as 3 Collections

Para cada collection abaixo: dentro da database, clique **Create collection**, dê o **nome exato** indicado, e **anote o Collection ID** gerado.

> ⚠️ Os **nomes dos atributos (Key)** precisam ser **exatamente** os listados — o app mapeia por esses nomes. Use o tipo e as opções indicadas.

### 4.1. Collection `grupos`  → anote como `COLLECTION_GRUPOS`

Aba **Attributes** → **Create attribute**:

| Key  | Tipo   | Size | Required | Default |
|------|--------|------|----------|---------|
| nome | String | 255  | ✅ Sim   | —       |

### 4.2. Collection `pessoas`  → anote como `COLLECTION_PESSOAS`

| Key          | Tipo    | Size / Min | Required | Observação |
|--------------|---------|------------|----------|------------|
| nome         | String  | 255        | ✅ Sim   | —          |
| rendaCentavos| Integer | Min: `0`   | ✅ Sim   | Renda em **centavos** (R$ 6.000,00 → `600000`) |
| groupId      | String  | 255        | ✅ Sim   | ID do documento em `grupos` |

Aba **Indexes** → **Create index**:

| Index Key       | Type | Attributes |
|-----------------|------|------------|
| `idx_groupId`   | Key  | `groupId` (ASC) |

### 4.3. Collection `gastos`  → anote como `COLLECTION_GASTOS`

| Key          | Tipo    | Size / Min | Required | Observação |
|--------------|---------|------------|----------|------------|
| descricao    | String  | 255        | ✅ Sim   | Ex.: "mercado" |
| valorCentavos| Integer | Min: `0`   | ✅ Sim   | Valor em **centavos** (R$ 1.000,00 → `100000`) |
| pagadorId    | String  | 255        | ✅ Sim   | ID do documento em `pessoas` |
| groupId      | String  | 255        | ✅ Sim   | ID do documento em `grupos` |
| competencia  | String  | 7          | ✅ Sim   | Mês no formato `YYYY-MM` (ex.: `2026-05`) |

Aba **Indexes** → **Create index**:

| Index Key                 | Type | Attributes |
|---------------------------|------|------------|
| `idx_group_competencia`   | Key  | `groupId` (ASC), `competencia` (ASC) |

> Esse índice composto cobre a consulta principal do app (gastos de um grupo num mês).

## 5. Permissões (decisão D1 — somente para o usuário logado)

A ideia: **só o usuário autenticado** pode ler/escrever; visitantes (guests) não acessam nada.

Para **cada uma das 3 collections**:

1. Abra a collection → aba **Settings**.
2. Em **Permissions**, clique **Add role** e adicione o role **Users** (= qualquer usuário autenticado).
3. Marque os 4 checkboxes para esse role: **Create**, **Read**, **Update**, **Delete**.
4. **Não** adicione o role **Any** (isso liberaria acesso público).
5. Em **Document Security**: deixe **desligado** (permissão a nível de collection é suficiente para single-user). Salve.

> Como é uso pessoal com um único usuário, o role `Users` já restringe o acesso a você (autenticado). Não é necessário document-level security.

## 6. Conferir a plataforma Android

1. Menu lateral → **Overview** (ou **Settings → Platforms**).
2. Confirme que existe uma plataforma **Android** com package name **`dev.paraizo.cost`**.
3. Se não existir, clique **Add platform → Android App**, name `cost`, package `dev.paraizo.cost`.

## 7. Coletar os valores para o app (usados na task T4)

Ao final, você deve ter estes 5 valores. Eles vão para `app/src/main/java/dev/paraizo/cost/data/AppwriteConfig.kt`:

| Constante           | Onde obter                                   | Valor |
|---------------------|----------------------------------------------|-------|
| `ENDPOINT`          | fixo                                         | `https://backend.paraizo.dev/v1` |
| `PROJECT_ID`        | fixo (já conhecido)                          | `6a248ab90027fe50cfbf` |
| `DATABASE_ID`       | passo 3                                       | `6a249b5e00222dff6baf` |
| `COLLECTION_GRUPOS` | passo 4.1                                     | `6a249ba30003f86d75e1` |
| `COLLECTION_PESSOAS`| passo 4.2                                     | `6a249bf8000bf31fb56b` |
| `COLLECTION_GASTOS` | passo 4.3                                     | `6a249c89001da73ade1a` |

> Preencha os 4 IDs em branco e me envie — eu uso na implementação da T4. (Database/Collection IDs **não** são segredo de segurança como uma API key, então pode colar aqui; mesmo assim, o acesso fica protegido pelas permissões do passo 5 + login.)

---

## Checklist de verificação

- [ ] Email/Password habilitado (passo 2)
- [ ] Database criada e `DATABASE_ID` anotado
- [ ] Collection `grupos` com atributo `nome`
- [ ] Collection `pessoas` com `nome`, `rendaCentavos`, `groupId` + índice `groupId`
- [ ] Collection `gastos` com `descricao`, `valorCentavos`, `pagadorId`, `groupId`, `competencia` + índice composto
- [ ] Permissões: role **Users** com CRUD nas 3 collections; **Any** ausente
- [ ] Plataforma Android `dev.paraizo.cost` presente
- [ ] 4 IDs coletados (Database + 3 Collections)
