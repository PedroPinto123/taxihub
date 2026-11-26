package ao.taxihub
 
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
 
val Ink = Color(0xFF171717)
val Secondary = Color(0xFF525252)
val Muted = Color(0xFF737373)
val Bg = Color(0xFFFAFAFA)
val Raised = Color(0xFFF4F4F5)
val Hairline = Color(0x14000000)
val Track = Color(0x1A000000)
val Yellow = Color(0xFFFFC107)
 
enum class LineIcon { Home, Star, Pin, More }
 
@Composable
fun AppIcon(
    kind: LineIcon,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    filled: Boolean = false
) {
    Canvas(modifier = modifier.size(iconSize)) {
        val s = size.width / 24f
        val line = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            LineIcon.Home -> {
                val roof = Path().apply {
                    moveTo(3.5f * s, 11f * s)
                    lineTo(12f * s, 4f * s)
                    lineTo(20.5f * s, 11f * s)
                }
                drawPath(roof, color, style = line)
                val walls = Path().apply {
                    moveTo(6f * s, 10f * s)
                    lineTo(6f * s, 20f * s)
                    lineTo(18f * s, 20f * s)
                    lineTo(18f * s, 10f * s)
                }
                drawPath(walls, color, style = line)
            }
            LineIcon.Star -> {
                val star = Path()
                for (i in 0 until 10) {
                    val angle = Math.toRadians((-90 + i * 36).toDouble())
                    val r = if (i % 2 == 0) 9.5f else 4.4f
                    val x = (12f + r * cos(angle).toFloat()) * s
                    val y = (12.8f + r * sin(angle).toFloat()) * s
                    if (i == 0) {
                        star.moveTo(x, y)
                    } else {
                        star.lineTo(x, y)
                    }
                }
                star.close()
                if (filled) {
                    drawPath(star, color, style = Fill)
                }
                drawPath(star, color, style = line)
            }
            LineIcon.Pin -> {
                drawCircle(color = color, radius = 6f * s, center = Offset(12f * s, 9.5f * s), style = line)
                drawCircle(color = color, radius = 2f * s, center = Offset(12f * s, 9.5f * s), style = line)
                val tail = Path().apply {
                    moveTo(7.6f * s, 13.9f * s)
                    lineTo(12f * s, 20.5f * s)
                    lineTo(16.4f * s, 13.9f * s)
                }
                drawPath(tail, color, style = line)
            }
            LineIcon.More -> {
                drawCircle(color = color, radius = 1.7f * s, center = Offset(5f * s, 12f * s))
                drawCircle(color = color, radius = 1.7f * s, center = Offset(12f * s, 12f * s))
                drawCircle(color = color, radius = 1.7f * s, center = Offset(19f * s, 12f * s))
            }
        }
    }
}
 
@Composable
fun RoofSign() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Ink)
        )
        Box(
            modifier = Modifier
                .size(width = 8.dp, height = 4.dp)
                .background(Ink)
        )
    }
}
 
@Composable
fun PageTitle(text: String) {
    Text(
        text,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Medium,
        color = Ink
    )
}
 
@Composable
fun QuietButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .border(1.dp, Hairline, shape)
            .background(Raised)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
    }
}
 
@Composable
fun AccentButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Yellow)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
    }
}
 
@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}
 
@Composable
fun RowDivider(startInset: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(1.dp)
            .background(Hairline)
    )
}
