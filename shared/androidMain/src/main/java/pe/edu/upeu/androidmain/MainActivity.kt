package pe.edu.upeu.androidmain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pe.edu.upeu.pharmamobil.App
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Llamamos directamente a App(), que ya contiene el tema, Koin y la navegación
        setContent {
            App()
        }
    }
}