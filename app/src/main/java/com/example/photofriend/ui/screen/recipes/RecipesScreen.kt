package com.example.photofriend.ui.screen.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photofriend.domain.model.FilmSimulationRecipe
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onBack: () -> Unit,
    onRecipeClick: () -> Unit,
    viewModel: RecipesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Film Recipes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            is RecipesUiState.Loading -> {
                Box(
                    Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is RecipesUiState.Success -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    TabRow(selectedTabIndex = state.activeTab.ordinal) {
                        Tab(
                            selected = state.activeTab == RecipeTab.BUILT_IN,
                            onClick = { viewModel.setTab(RecipeTab.BUILT_IN) },
                            text = { Text("Built-in") }
                        )
                        Tab(
                            selected = state.activeTab == RecipeTab.SAVED,
                            onClick = { viewModel.setTab(RecipeTab.SAVED) },
                            text = { Text("My Recipes (${state.savedRecipes.size})") }
                        )
                    }

                    when (state.activeTab) {
                        RecipeTab.BUILT_IN -> {
                            RecipeList(
                                recipes = state.builtInRecipes,
                                onRecipeClick = { recipe ->
                                    viewModel.selectRecipe(recipe)
                                    onRecipeClick()
                                }
                            )
                        }
                        RecipeTab.SAVED -> {
                            if (state.savedRecipes.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No saved recipes yet.\nAnalyse a scene and tap Save.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                SwipeableRecipeList(
                                    recipes = state.savedRecipes,
                                    onRecipeClick = { recipe ->
                                        viewModel.selectRecipe(recipe)
                                        onRecipeClick()
                                    },
                                    onDelete = { recipe ->
                                        viewModel.deleteRecipe(recipe)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "\"${recipe.name}\" deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeList(
    recipes: List<FilmSimulationRecipe>,
    onRecipeClick: (FilmSimulationRecipe) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recipes, key = { it.id }) { recipe ->
            RecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRecipeList(
    recipes: List<FilmSimulationRecipe>,
    onRecipeClick: (FilmSimulationRecipe) -> Unit,
    onDelete: (FilmSimulationRecipe) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recipes, key = { it.id }) { recipe ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onDelete(recipe)
                        true
                    } else false
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            ) {
                RecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeCard(recipe: FilmSimulationRecipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = recipe.filmSimulation,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingSummaryRow("Grain", recipe.grain)
                SettingSummaryRow("Color Chrome", recipe.colorChrome)
                SettingSummaryRow("White Balance", recipe.whiteBalance)
                val hi = if (recipe.highlights >= 0) "+${recipe.highlights}" else "${recipe.highlights}"
                val sh = if (recipe.shadows >= 0) "+${recipe.shadows}" else "${recipe.shadows}"
                SettingSummaryRow("Highlights / Shadows", "$hi / $sh")
                SettingSummaryRow("Color / Sharpness / NR", "${recipe.color} / ${recipe.sharpness} / ${recipe.noiseReduction}")
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                recipe.tags.take(5).forEach { tag ->
                    SuggestionChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    }
}

@Composable
private fun SettingSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
