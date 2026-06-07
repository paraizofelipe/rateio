package dev.paraizo.cost.ui.nav

import dev.paraizo.cost.ui.auth.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutesTest {

    @Test
    fun startDestinationForLoggedOutReturnsLogin() {
        assertEquals(Routes.LOGIN, startDestinationFor(AuthState.LoggedOut))
    }

    @Test
    fun startDestinationForLoadingReturnsLogin() {
        assertEquals(Routes.LOGIN, startDestinationFor(AuthState.Loading))
    }

    @Test
    fun startDestinationForLoggedInReturnsGrupos() {
        assertEquals(Routes.GRUPOS, startDestinationFor(AuthState.LoggedIn))
    }

    @Test
    fun startDestinationForErrorReturnsLogin() {
        assertEquals(Routes.LOGIN, startDestinationFor(AuthState.Error("x")))
    }

    @Test
    fun routesPessoasInterpolatesGroupId() {
        assertEquals("grupos/g1/pessoas", Routes.pessoas("g1"))
    }

    @Test
    fun routesGastosInterpolatesGroupId() {
        assertEquals("grupos/g1/gastos", Routes.gastos("g1"))
    }

    @Test
    fun routesSettleInterpolatesGroupId() {
        assertEquals("grupos/g1/settle", Routes.settle("g1"))
    }
}
