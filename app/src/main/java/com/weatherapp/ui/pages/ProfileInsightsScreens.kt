package com.weatherapp.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileDashboardScreen(
    onOpenIntegrations: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeniorMode by rememberSaveable { mutableStateOf(true) }
    var statusMessage by rememberSaveable {
        mutableStateOf("Modo idoso ativo: conteúdos e alertas estão adaptados.")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GradientContainer(colors = HeroGradient) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Maria Silva", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Membro desde janeiro de 2024", color = Color.White.copy(alpha = 0.8f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Modo idoso", color = Color.White)
                    Switch(
                        checked = isSeniorMode,
                        onCheckedChange = {
                            isSeniorMode = it
                            statusMessage = if (it) {
                                "Modo idoso ativo: você receberá lembretes suaves."
                            } else {
                                "Modo padrão ativado: notificações mais energéticas liberadas."
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.White.copy(alpha = 0.4f),
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardStatCard(
                icon = Icons.Filled.CalendarMonth,
                value = "28",
                label = "Dias ativos",
                modifier = Modifier.weight(1f)
            )
            DashboardStatCard(
                icon = Icons.Filled.Star,
                value = "12",
                label = "Conquistas",
                modifier = Modifier.weight(1f)
            )
        }

        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileOptionRow(Icons.Filled.Notifications, "Estatisticas", "Veja seu progresso detalhado")
                ProfileOptionRow(Icons.Filled.Settings, "Configuracoes", "Preferencias e alertas personalizados")
                ProfileOptionRow(Icons.Filled.VerifiedUser, "Ajuda", "Tecnologia, saude e motivacao em um so toque")
            }
        }

        Button(
            onClick = onOpenIntegrations,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ED572))
        ) {
            Text("Explorar recursos inteligentes")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }

        Text(
            text = statusMessage,
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun InsightsScreen(
    onSeeJourneys: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        IntegrationsCard()
        AdaptiveAICard()
        DailyActivityCard()
        Button(
            onClick = onSeeJourneys,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6CF7))
        ) {
            Text("Ver conquistas e treinos")
        }
    }
}

@Composable
private fun DashboardStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2ED572))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray)
        }
    }
}

@Composable
private fun ProfileOptionRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF4A6CF7),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF1FE))
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
private fun IntegrationsCard() {
    val items = listOf(
        IntegrationItem("Google Fit", "Sincronizar dados de atividade", true),
        IntegrationItem("HealthKit", "Dados de saude do iPhone", false),
        IntegrationItem("YouTube", "Videos de exercicios", true)
    )
    GradientContainer(colors = PurpleGradient) {
        Text("Integracoes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Conecte com seus apps favoritos", color = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { integration ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Color(0xFF7C4DFF))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(integration.title, fontWeight = FontWeight.Bold)
                            Text(integration.description, color = Color.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = integration.enabled,
                            onCheckedChange = {},
                            enabled = false,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF48D17B),
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Videos recomendados")
            }
        }
    }
}

@Composable
private fun AdaptiveAICard() {
    GradientContainer(colors = BlueGradient) {
        Text("IA adaptativa", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Personalizacao inteligente para voce", color = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Recomendacao de hoje", fontWeight = FontWeight.Bold)
                Text("Hoje o treino sera leve, perfeito para o seu ritmo", color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                        Text("Aceitar")
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6CF7))
                    ) {
                        Text("Personalizar")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Analise de progresso", fontWeight = FontWeight.Bold)
                ProgressBadge("Melhoria na resistencia", "+15%")
                ProgressBadge("Frequencia cardiaca", "Estavel")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Dica inteligente", fontWeight = FontWeight.Bold)
                Text(
                    text = "A IA notou que voce rende melhor pela manha. Que tal agendar treinos para esse horario?",
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun ProgressBadge(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFEDF1FE))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF4A4A4A), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF2ED572))
    }
}

@Composable
private fun DailyActivityCard() {
    GradientContainer(colors = TealGradient) {
        Text("Ola, Maria!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Que tal se movimentar hoje?", color = Color.White.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ActivityMetric(
                title = "Passos",
                value = "3.247",
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                tint = Color(0xFF2ED572),
                modifier = Modifier.weight(1f)
            )
            ActivityMetric(
                title = "Calorias",
                value = "245",
                icon = Icons.Filled.LocalFireDepartment,
                tint = Color(0xFFF95F62),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Tempo de treino hoje", color = Color.White.copy(alpha = 0.8f))
        TrainingProgressBar(0.65f)
        Text("25 min", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF07BEB8))
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar treino")
        }
    }
}

@Composable
private fun ActivityMetric(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TrainingProgressBar(value: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(value)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        )
    }
}

private data class IntegrationItem(val title: String, val description: String, val enabled: Boolean)
