package dev.paraizo.cost.ui.auth

import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account

interface AuthGateway {
    suspend fun isLoggedIn(): Boolean
    suspend fun login(email: String, senha: String)
    suspend fun logout()
}

class AppwriteAuthGateway(private val account: Account) : AuthGateway {

    // Só uma exceção do próprio Appwrite (ex.: sessão inexistente) significa "deslogado".
    // Demais exceções (inclusive CancellationException) propagam.
    override suspend fun isLoggedIn(): Boolean = try {
        account.get()
        true
    } catch (e: AppwriteException) {
        false
    }

    override suspend fun login(email: String, senha: String) {
        account.createEmailPasswordSession(email, senha)
    }

    override suspend fun logout() {
        account.deleteSession("current")
    }
}
