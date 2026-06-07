package dev.paraizo.cost.data

import dev.paraizo.cost.domain.Gasto

interface GastoRepo {
    suspend fun create(gasto: Gasto): Gasto
    suspend fun listByGroupAndCompetencia(groupId: String, competencia: String): List<Gasto>
    suspend fun listByGroup(groupId: String): List<Gasto>
    suspend fun update(gasto: Gasto): Gasto
    suspend fun delete(id: String)
}
