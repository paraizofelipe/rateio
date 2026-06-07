package dev.paraizo.cost.ui.nav

import dev.paraizo.cost.ui.auth.AuthState

object Routes {
    const val LOGIN = "login"
    const val GRUPOS = "grupos"
    const val PESSOAS = "grupos/{groupId}/pessoas"
    const val GASTOS = "grupos/{groupId}/gastos"
    const val SETTLE = "grupos/{groupId}/settle/{competencia}"

    fun pessoas(groupId: String) = "grupos/$groupId/pessoas"
    fun gastos(groupId: String) = "grupos/$groupId/gastos"
    fun settle(groupId: String, competencia: String) = "grupos/$groupId/settle/$competencia"
}

fun startDestinationFor(state: AuthState): String = when (state) {
    is AuthState.LoggedIn -> Routes.GRUPOS
    else -> Routes.LOGIN
}
