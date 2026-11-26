package ao.taxihub
 
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
 
const val SUGGESTION_EMAIL = "taxiagil.sugestoes@gmail.com"
 
@Composable
fun SuggestCard() {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Conheces outro táxi?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Sugere uma app de táxi que funcione em Angola e nós vemos se a acrescentamos.")
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { suggestTaxi(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink,
                    contentColor = Yellow
                )
            ) {
                Text("Sugerir um táxi", fontWeight = FontWeight.Bold)
            }
        }
    }
}
 
fun suggestTaxi(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUGGESTION_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Sugestão de táxi para a Táxi Ágil")
        putExtra(
            Intent.EXTRA_TEXT,
            "Nome da app de táxi:\n\nLigação da Play Store:\n\nCidades de Angola onde funciona:\n"
        )
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Não encontrei uma app de email neste telemóvel.", Toast.LENGTH_LONG).show()
    }
}
