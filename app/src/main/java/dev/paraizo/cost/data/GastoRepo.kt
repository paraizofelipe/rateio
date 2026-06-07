package dev.paraizo.cost.data

import dev.paraizo.cost.domain.Gasto

interface GastoRepo {
    suspend fun create(gasto: Gasto): Gasto
    suspend fun listByGroupAndCompetencia(groupId: String, competencia: String): List<Gasto>
}
