package dev.paraizo.cost.data

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases

class AppwriteClient(context: Context) {
    // applicationContext evita memory leak caso o client seja mantido em escopo de Application
    val client: Client = Client(context.applicationContext)
        .setEndpoint(AppwriteConfig.ENDPOINT)
        .setProject(AppwriteConfig.PROJECT_ID)

    val account: Account = Account(client)
    val databases: Databases = Databases(client)
}
