package dev.paraizo.cost.data

/**
 * Snapshot ("foto") da renda de cada pessoa numa competência. Permite congelar o
 * rateio de meses já lançados: alterar a renda atual de uma pessoa não afeta os
 * meses que já têm foto. Mapa retornado é pessoaId -> renda em centavos.
 */
interface RendaMensalRepo {
    suspend fun rendasDe(groupId: String, competencia: String): Map<String, Long>
    suspend fun criarSnapshot(groupId: String, competencia: String, rendas: Map<String, Long>)
    suspend fun limparSnapshot(groupId: String, competencia: String)
}
