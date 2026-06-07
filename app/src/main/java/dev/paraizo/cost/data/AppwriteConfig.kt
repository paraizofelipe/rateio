package dev.paraizo.cost.data

import dev.paraizo.cost.BuildConfig

/**
 * Configuração do Appwrite. Endpoint e IDs vêm do BuildConfig, que por sua vez
 * os lê de local.properties (não versionado). Ver docs/appwrite-setup.md.
 */
object AppwriteConfig {
    val ENDPOINT: String = BuildConfig.APPWRITE_ENDPOINT
    val PROJECT_ID: String = BuildConfig.APPWRITE_PROJECT_ID
    val DATABASE_ID: String = BuildConfig.APPWRITE_DATABASE_ID
    val COLLECTION_GRUPOS: String = BuildConfig.APPWRITE_COLLECTION_GRUPOS
    val COLLECTION_PESSOAS: String = BuildConfig.APPWRITE_COLLECTION_PESSOAS
    val COLLECTION_GASTOS: String = BuildConfig.APPWRITE_COLLECTION_GASTOS
    val COLLECTION_RENDAS: String = BuildConfig.APPWRITE_COLLECTION_RENDAS
}
