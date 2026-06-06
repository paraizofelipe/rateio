package dev.paraizo.cost.data

import dev.paraizo.cost.data.dto.pessoaFromDocument
import dev.paraizo.cost.data.dto.pessoaToData
import dev.paraizo.cost.domain.Pessoa
import io.appwrite.ID
import io.appwrite.Query

class PessoaRepository(private val client: AppwriteClient) {

    suspend fun create(pessoa: Pessoa): Pessoa {
        val doc = client.databases.createDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_PESSOAS,
            documentId = ID.unique(),
            data = pessoaToData(pessoa)
        )
        @Suppress("UNCHECKED_CAST")
        return pessoaFromDocument(doc.id, doc.data as Map<String, Any>)
    }

    suspend fun listByGroup(groupId: String): List<Pessoa> {
        val result = client.databases.listDocuments(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_PESSOAS,
            queries = listOf(Query.equal("groupId", groupId), Query.limit(100))
        )
        @Suppress("UNCHECKED_CAST")
        return result.documents.map { pessoaFromDocument(it.id, it.data as Map<String, Any>) }
    }

    suspend fun update(pessoa: Pessoa): Pessoa {
        val doc = client.databases.updateDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_PESSOAS,
            documentId = pessoa.id,
            data = pessoaToData(pessoa)
        )
        @Suppress("UNCHECKED_CAST")
        return pessoaFromDocument(doc.id, doc.data as Map<String, Any>)
    }

    suspend fun delete(id: String) {
        client.databases.deleteDocument(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_PESSOAS,
            documentId = id
        )
    }
}
