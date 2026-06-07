package dev.paraizo.cost.data

import dev.paraizo.cost.domain.Grupo

/** Abstração de acesso a grupos, na camada de dados, para permitir fakes em teste. */
interface GrupoRepo {
    suspend fun create(grupo: Grupo): Grupo
    suspend fun list(): List<Grupo>
}
