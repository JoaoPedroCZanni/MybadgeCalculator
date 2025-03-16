package br.com.fiap.calculadora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.calculadora.ui.theme.CalculadoraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculadoraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CalculadoraScreen()
                }
            }
        }
    }
}

@Composable
fun CalculadoraScreen() {
    var aporteInicial by remember { mutableStateOf("") }
    var depositoRecorrente by remember { mutableStateOf("") }
    var frequencia by remember { mutableStateOf("") }
    var periodo by remember { mutableStateOf("") }
    var rendimentoTotal by remember { mutableStateOf("") }
    var usarSelic by remember { mutableStateOf(false) }
    var taxaSelic by remember { mutableStateOf(0.0) }

    val coroutineScope = rememberCoroutineScope()
    val customColor = Color(0x990026FF) // Azul com 60% de opacidade

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Calculadora", fontSize = 24.sp, color = Color.Black)
            IconButton(onClick = { /* Ação do menu */ }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = aporteInicial,
            onValueChange = { aporteInicial = it },
            label = { Text("Aporte inicial") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = customColor,
                unfocusedBorderColor = customColor
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = depositoRecorrente,
            onValueChange = { depositoRecorrente = it },
            label = { Text("Depósito recorrente") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = customColor,
                unfocusedBorderColor = customColor
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedTextField(
                value = frequencia,
                onValueChange = { frequencia = it },
                label = { Text("Frequência") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = customColor,
                    unfocusedBorderColor = customColor
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = periodo,
                onValueChange = { periodo = it },
                label = { Text("Período") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = customColor,
                    unfocusedBorderColor = customColor
                )
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Checkbox para usar a taxa Selic
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = usarSelic,
                onCheckedChange = { isChecked ->
                    usarSelic = isChecked
                    if (isChecked) {
                        coroutineScope.launch {
                            taxaSelic = obterTaxaSelic() ?: 0.0
                        }
                    }
                }
            )
            Text("Usar Taxa Selic")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                rendimentoTotal = calcularRendimento(
                    aporteInicial.toDoubleOrNull() ?: 0.0,
                    depositoRecorrente.toDoubleOrNull() ?: 0.0,
                    frequencia.toIntOrNull() ?: 1,
                    periodo.toIntOrNull() ?: 1,
                    if (usarSelic) taxaSelic else 0.05 // Se usar Selic, pega o valor da API, senão usa 5% fixo
                ).toString()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = customColor)
        ) {
            Text("CALCULAR")
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = rendimentoTotal,
            onValueChange = {},
            label = { Text("Rendimentos totais") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = customColor,
                unfocusedBorderColor = customColor
            )
        )
    }
}

// Função para calcular o rendimento total
fun calcularRendimento(
    aporte: Double, deposito: Double, frequencia: Int, periodo: Int, taxa: Double
): Double {
    return (aporte + (deposito * frequencia * periodo)) * (1 + taxa)
}

// Função suspend para buscar a taxa Selic da API do Banco Central
suspend fun obterTaxaSelic(): Double? {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.432/dados/ultimos/1?formato=json"
            val response = URL(url).readText()
            val jsonArray = JSONObject("{\"data\": $response}").getJSONArray("data")
            if (jsonArray.length() > 0) {
                val taxaSelic = jsonArray.getJSONObject(0).getString("valor").replace(",", ".").toDoubleOrNull()
                taxaSelic?.div(100) // Convertendo para decimal (ex: 10% vira 0.1)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
