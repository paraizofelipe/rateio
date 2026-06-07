package dev.paraizo.cost.data

import io.appwrite.ID
import io.appwrite.Query

class RendaMensalRepository(private val client: AppwriteClient) : RendaMensalRepo {

    override suspend fun rendasDe(groupId: String, competencia: String): Map<String, Long> {
        val result = client.databases.listDocuments(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_RENDAS,
            queries = listOf(
                Query.equal("groupId", groupId),
                Query.equal("competencia", competencia),
                Query.limit(500)
            )
        )
        return result.documents.associate { doc ->
            @Suppress("UNCHECKED_CAST")
            val data = doc.data as Map<String, Any>
            (data["pessoaId"] as String) to (data["rendaCentavos"] as Number).toLong()
        }
    }

    override suspend fun criarSnapshot(groupId: String, competencia: String, rendas: Map<String, Long>) {
        rendas.forEach { (pessoaId, cents) ->
            client.databases.createDocument(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.COLLECTION_RENDAS,
                documentId = ID.unique(),
                data = mapOf(
                    "groupId" to groupId,
                    "competencia" to competencia,
                    "pessoaId" to pessoaId,
                    "rendaCentavos" to cents
                )
            )
        }
    }

    override suspend fun limparSnapshot(groupId: String, competencia: String) {
        val result = client.databases.listDocuments(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_RENDAS,
            queries = listOf(
                Query.equal("groupId", groupId),
                Query.equal("competencia", competencia),
                Query.limit(500)
            )
        )
        result.documents.forEach { doc ->
            client.databases.deleteDocument(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.COLLECTION_RENDAS,
                documentId = doc.id
            )
        }
    }
}
