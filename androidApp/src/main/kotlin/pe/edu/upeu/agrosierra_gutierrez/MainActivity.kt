package pe.edu.upeu.agrosierra_gutierrez
import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.koin.androidx.viewmodel.ext.android.viewModel
import pe.edu.upeu.agrosierra_gutierrez.presentation.AgroViewModel
class MainActivity: ComponentActivity() {
    private val vm: AgroViewModel by viewModel()
    private val solicitarRedLocal=registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
        if(permitido) vm.sincronizar()
    }
    private lateinit var conectividad: ConnectivityManager
    private val callback=object: ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network,caps: NetworkCapabilities) {
            if(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) runOnUiThread { vm.sincronizar() }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        conectividad=getSystemService(ConnectivityManager::class.java)
        setContent { App() }
        if(Build.VERSION.SDK_INT >= 37 && checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            solicitarRedLocal.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
    override fun onStart() { super.onStart(); conectividad.registerDefaultNetworkCallback(callback) }
    override fun onResume() { super.onResume(); vm.sincronizar() }
    override fun onStop() { conectividad.unregisterNetworkCallback(callback); super.onStop() }
}
