package dev.paraizo.cost.data.dto

import dev.paraizo.cost.domain.Gasto
import dev.paraizo.cost.domain.Grupo
import dev.paraizo.cost.domain.Money
import dev.paraizo.cost.domain.Pessoa

fun grupoToData(grupo: Grupo): Map<String, Any> = mapOf(
    "nome" to grupo.nome
)

fun grupoFromDocument(id: String, data: Map<String, Any>): Grupo = Grupo(
    id = id,
    nome = data["nome"] as String
)

fun pessoaToData(pessoa: Pessoa): Map<String, Any> = mapOf(
    "nome" to pessoa.nome,
    "rendaCentavos" to pessoa.renda.cents,
    "groupId" to pessoa.groupId
)

fun pessoaFromDocument(id: String, data: Map<String, Any>): Pessoa = Pessoa(
    id = id,
    nome = data["nome"] as String,
    renda = Money((data["rendaCentavos"] as Number).toLong()),
    groupId = data["groupId"] as String
)

fun gastoToData(gasto: Gasto): Map<String, Any> = mapOf(
    "descricao" to gasto.descricao,
    "valorCentavos" to gasto.valor.cents,
    "pagadorId" to gasto.pagadorId,
    "groupId" to gasto.groupId,
    "competencia" to gasto.competencia
)

fun gastoFromDocument(id: String, data: Map<String, Any>): Gasto = Gasto(
    id = id,
    descricao = data["descricao"] as String,
    valor = Money((data["valorCentavos"] as Number).toLong()),
    pagadorId = data["pagadorId"] as String,
    groupId = data["groupId"] as String,
    competencia = data["competencia"] as String
)
