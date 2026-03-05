package com.example.photofriend.ui.screen.recipedetail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photofriend.domain.model.FilmSimulationRecipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val recipe by viewModel.recipe.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.name ?: "Recipe Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    recipe?.let { r ->
                        IconButton(onClick = {
                            val text = buildShareText(r)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, r.name)
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Recipe"))
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Share, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        val r = recipe
        if (r == null) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No recipe selected.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            RecipeDetailContent(recipe = r, modifier = Modifier.fillMaxSize().padding(paddingValues))
        }
    }
}

@Composable
private fun RecipeDetailContent(recipe: FilmSimulationRecipe, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = recipe.filmSimulation,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    if (recipe.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "FULL SETTINGS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    val sign = { v: Int -> if (v >= 0) "+$v" else "$v" }
                    listOf(
                        "Film Simulation" to recipe.filmSimulation,
                        "Grain Effect" to recipe.grain,
                        "Color Chrome Effect" to recipe.colorChrome,
                        "Color Chrome FX Blue" to recipe.colorChromeBlue,
                        "White Balance" to recipe.whiteBalance,
                        "WB Shift R / B" to "${sign(recipe.wbShiftR)} / ${sign(recipe.wbShiftB)}",
                        "Highlight Tone" to sign(recipe.highlights),
                        "Shadow Tone" to sign(recipe.shadows),
                        "Color" to sign(recipe.color),
                        "Sharpness" to sign(recipe.sharpness),
                        "Noise Reduction" to sign(recipe.noiseReduction),
                        "Clarity" to sign(recipe.clarity),
                        "ISO Range" to "${recipe.isoMin}–${recipe.isoMax}"
                    ).forEachIndexed { index, (label, value) ->
                        DetailRow(label = label, value = value)
                        if (index < 12) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        if (recipe.tags.isNotEmpty()) {
            item {
                Text(
                    text = "TAGS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = recipe.tags.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun buildShareText(recipe: FilmSimulationRecipe): String {
    val sign = { v: Int -> if (v >= 0) "+$v" else "$v" }
    return """
${recipe.name}
${recipe.description}

Film Simulation: ${recipe.filmSimulation}
Grain Effect: ${recipe.grain}
Color Chrome Effect: ${recipe.colorChrome}
Color Chrome FX Blue: ${recipe.colorChromeBlue}
White Balance: ${recipe.whiteBalance}
WB Shift: R${sign(recipe.wbShiftR)} / B${sign(recipe.wbShiftB)}
Highlight Tone: ${sign(recipe.highlights)}
Shadow Tone: ${sign(recipe.shadows)}
Color: ${sign(recipe.color)}
Sharpness: ${sign(recipe.sharpness)}
Noise Reduction: ${sign(recipe.noiseReduction)}
Clarity: ${sign(recipe.clarity)}
ISO Range: ${recipe.isoMin}–${recipe.isoMax}

Shared via Photofriend
    """.trimIndent()
}
