package dev.paraizo.cost.data

import dev.paraizo.cost.domain.Pessoa

interface PessoaRepo {
    suspend fun create(pessoa: Pessoa): Pessoa
    suspend fun listByGroup(groupId: String): List<Pessoa>
    suspend fun update(pessoa: Pessoa): Pessoa
    suspend fun delete(id: String)
}
