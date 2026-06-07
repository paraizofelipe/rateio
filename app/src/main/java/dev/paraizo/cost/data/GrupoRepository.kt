package dev.paraizo.cost.data

import dev.paraizo.cost.data.dto.grupoFromDocument
import dev.paraizo.cost.data.dto.grupoToData
import dev.paraizo.cost.domain.Grupo
import io.appwrite.ID
import io.appwrite.Query

class GrupoRepository(private val client: AppwriteClient) : GrupoRepo {

    override suspend fun create(grupo: Grupo): Grupo {
        val doc = client.databases.createDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_GRUPOS,
            documentId = ID.unique(),
            data = grupoToData(grupo)
        )
        @Suppress("UNCHECKED_CAST")
        return grupoFromDocument(doc.id, doc.data as Map<String, Any>)
    }

    override suspend fun list(): List<Grupo> {
        val result = client.databases.listDocuments(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_GRUPOS,
            queries = listOf(Query.limit(100))
        )
        @Suppress("UNCHECKED_CAST")
        return result.documents.map { grupoFromDocument(it.id, it.data as Map<String, Any>) }
    }

    override suspend fun update(grupo: Grupo): Grupo {
        val doc = client.databases.updateDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_GRUPOS,
            documentId = grupo.id,
            data = grupoToData(grupo)
        )
        @Suppress("UNCHECKED_CAST")
        return grupoFromDocument(doc.id, doc.data as Map<String, Any>)
    }

    override suspend fun delete(id: String) {
        client.databases.deleteDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_GRUPOS,
            documentId = id
        )
    }
}
