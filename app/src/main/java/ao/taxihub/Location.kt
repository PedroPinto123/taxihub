package ao.taxihub
 
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale
 
@Composable
fun ZoneCard() {
    val context = LocalContext.current
    var zone by remember { mutableStateOf<String?>(null) }
    var message by remember {
        mutableStateOf("Vê em que zona estás. A app não guarda nem envia a tua localização.")
    }
    var loading by remember { mutableStateOf(false) }
 
    val start: () -> Unit = {
        loading = true
        findZone(context) { result ->
            loading = false
            if (result != null) {
                zone = result
            } else {
                message = "Não consegui obter a localização. Liga a localização do telemóvel e tenta de novo."
            }
        }
    }
 
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            start()
        } else {
            message = "Sem permissão não consigo mostrar a tua zona."
        }
    }
 
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
 
    LaunchedEffect(Unit) {
        if (hasPermission) {
            start()
        }
    }
 
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("\uD83D\uDCCD A tua zona", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            val currentZone = zone
            if (currentZone != null) {
                Text(currentZone, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "A app não guarda nem envia a tua localização.",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
            } else if (loading) {
                Text("A procurar a tua zona...")
            } else {
                Text(message)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (hasPermission) {
                            start()
                        } else {
                            launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink,
                        contentColor = Yellow
                    )
                ) {
                    Text("Mostrar a minha zona", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
 
fun findZone(context: Context, onResult: (String?) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    var answered = false
    val deliver: (String?) -> Unit = { text ->
        if (!answered) {
            answered = true
            onResult(text)
        }
    }
    try {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val last = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (last != null) {
            describeZone(context, last, deliver)
            return
        }
        if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            deliver(null)
            return
        }
        handler.postDelayed({ deliver(null) }, 15000)
        manager.requestSingleUpdate(
            LocationManager.NETWORK_PROVIDER,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    describeZone(context, location, deliver)
                }
 
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
 
                override fun onProviderEnabled(provider: String) {}
 
                override fun onProviderDisabled(provider: String) {}
            },
            Looper.getMainLooper()
        )
    } catch (e: Exception) {
        deliver(null)
    }
}
 
fun describeZone(context: Context, location: Location, deliver: (String?) -> Unit) {
    Thread {
        var text = ""
        try {
            val addresses = Geocoder(context, Locale("pt", "AO"))
                .getFromLocation(location.latitude, location.longitude, 1)
            val first = addresses?.firstOrNull()
            if (first != null) {
                text = first.subLocality ?: first.locality ?: first.subAdminArea ?: ""
            }
        } catch (e: Exception) {
            text = ""
        }
        if (text.isBlank()) {
            text = "Lat " + String.format(Locale.US, "%.2f", location.latitude) +
                ", Long " + String.format(Locale.US, "%.2f", location.longitude)
        }
        Handler(Looper.getMainLooper()).post { deliver(text) }
    }.start()
}
