package ao.taxihub
 
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
 
const val SUGGESTION_EMAIL = "taxiagil.sugestoes@gmail.com"
 
@Composable
fun SuggestBlock() {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Conheces outro táxi?", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Sugere uma app de táxi que funcione em Angola e nós vemos se a acrescentamos.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Secondary
        )
        Spacer(Modifier.height(12.dp))
        QuietButton("Sugerir um táxi") { suggestTaxi(context) }
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
