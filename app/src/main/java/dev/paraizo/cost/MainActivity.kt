package dev.paraizo.cost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.paraizo.cost.data.AppwriteClient
import dev.paraizo.cost.ui.auth.AppwriteAuthGateway
import dev.paraizo.cost.ui.auth.AuthViewModel
import dev.paraizo.cost.ui.nav.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    val client = remember { AppwriteClient(applicationContext) }
                    val gateway = remember { AppwriteAuthGateway(client.account) }
                    // viewModel(factory) garante sobrevivencia a mudancas de configuracao
                    val authViewModel: AuthViewModel = viewModel(
                        factory = viewModelFactory { initializer { AuthViewModel(gateway) } }
                    )

                    LaunchedEffect(Unit) {
                        authViewModel.checkSession()
                    }

                    AppNav(authViewModel = authViewModel, appwriteClient = client)
                }
            }
        }
    }
}
