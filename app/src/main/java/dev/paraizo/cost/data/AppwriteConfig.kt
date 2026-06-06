package dev.paraizo.cost.data

import dev.paraizo.cost.BuildConfig

/**
 * Configuração do Appwrite. O ENDPOINT é público; os IDs vêm do BuildConfig,
 * que por sua vez os lê de local.properties (não versionado). Ver docs/appwrite-setup.md.
 */
object AppwriteConfig {
    const val ENDPOINT = "https://YOUR_APPWRITE_ENDPOINT/v1"
    val PROJECT_ID: String = BuildConfig.APPWRITE_PROJECT_ID
    val DATABASE_ID: String = BuildConfig.APPWRITE_DATABASE_ID
    val COLLECTION_GRUPOS: String = BuildConfig.APPWRITE_COLLECTION_GRUPOS
    val COLLECTION_PESSOAS: String = BuildConfig.APPWRITE_COLLECTION_PESSOAS
    val COLLECTION_GASTOS: String = BuildConfig.APPWRITE_COLLECTION_GASTOS
}
