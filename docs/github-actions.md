# CI/CD — Build do APK e publicação na Release

Workflow: `.github/workflows/build-apk.yml`

## O que faz

A cada **push na branch `main`** (ou execução manual em **Actions → Build APK e publicar release → Run workflow**):

1. Configura JDK 17 (Temurin) no runner.
2. Remove o pin `org.gradle.java.home` do `gradle.properties` (esse caminho só
   existe na máquina local; no runner usamos o JDK 17).
3. Recria o `local.properties` (gitignored) com os IDs do Appwrite vindos dos
   Secrets do repositório.
4. Roda os testes unitários (`testDebugUnitTest`).
5. Compila o APK de debug (`assembleDebug`).
6. Publica uma **Release** com a tag `v1.0.<número-da-execução>` e anexa o
   arquivo `cost-debug-v1.0.<n>.apk` para download.

O APK de debug já vem assinado com a chave de debug — instala direto no celular,
sem keystore.

## Secrets necessários

Cadastrar em **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Conteúdo (ver `local.properties`) |
|---|---|
| `APPWRITE_PROJECT_ID` | `appwrite.projectId` |
| `APPWRITE_DATABASE_ID` | `appwrite.databaseId` |
| `APPWRITE_COLLECTION_GRUPOS` | `appwrite.collectionGrupos` |
| `APPWRITE_COLLECTION_PESSOAS` | `appwrite.collectionPessoas` |
| `APPWRITE_COLLECTION_GASTOS` | `appwrite.collectionGastos` |

## Instalar no celular

1. Abra a aba **Releases** do repositório no GitHub.
2. Baixe o `.apk` da release mais recente.
3. Toque no arquivo no celular e autorize "instalar de fontes desconhecidas".
