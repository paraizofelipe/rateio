package dev.paraizo.cost.data

import dev.paraizo.cost.data.dto.gastoFromDocument
import dev.paraizo.cost.data.dto.gastoToData
import dev.paraizo.cost.domain.Gasto
import io.appwrite.ID
import io.appwrite.Query

class GastoRepository(private val client: AppwriteClient) : GastoRepo {

    override suspend fun create(gasto: Gasto): Gasto {
        val doc = client.databases.createDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_GASTOS,
            documentId = ID.unique(),
            data = gastoToData(gasto)
        )
        @Suppress("UNCHECKED_CAST")
        return gastoFromDocument(doc.id, doc.data as Map<String, Any>)
    }

    override suspend fun listByGroupAndCompetencia(groupId: String, competencia: String): List<Gasto> {
        val result = client.databases.listDocuments(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_GASTOS,
            queries = listOf(
                Query.equal("groupId", groupId),
                Query.equal("competencia", competencia),
                Query.limit(100)
            )
        )
        @Suppress("UNCHECKED_CAST")
        return result.documents.map { gastoFromDocument(it.id, it.data as Map<String, Any>) }
    }

    suspend fun update(gasto: Gasto): Gasto {
        val doc = client.databases.updateDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_GASTOS,
            documentId = gasto.id,
            data = gastoToData(gasto)
        )
        @Suppress("UNCHECKED_CAST")
        return gastoFromDocument(doc.id, doc.data as Map<String, Any>)
    }

    suspend fun delete(id: String) {
        client.databases.deleteDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_GASTOS,
            documentId = id
        )
    }
}
