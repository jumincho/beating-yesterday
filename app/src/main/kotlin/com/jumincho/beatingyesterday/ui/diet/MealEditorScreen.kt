package com.jumincho.beatingyesterday.ui.diet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.food.FoodItem
import com.jumincho.beatingyesterday.core.food.FoodSearchError
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.core.validation.FieldError
import com.jumincho.beatingyesterday.ui.components.InfoCard
import com.jumincho.beatingyesterday.ui.components.LoadingState
import com.jumincho.beatingyesterday.ui.format.errorSupportingText
import com.jumincho.beatingyesterday.ui.format.kcalText
import com.jumincho.beatingyesterday.ui.format.label
import com.jumincho.beatingyesterday.ui.format.message
import com.jumincho.beatingyesterday.ui.theme.BeatingYesterdayTheme
import kotlin.math.roundToInt

/** Adds or edits a meal. Closes itself once the meal is saved. */
@Composable
fun MealEditorScreen(viewModel: MealEditorViewModel, onClose: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onClose()
    }
    MealEditorScreen(
        uiState = uiState,
        onClose = onClose,
        onTypeChange = viewModel::onTypeChange,
        onNameChange = viewModel::onNameChange,
        onKcalChange = viewModel::onKcalChange,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::search,
        onFoodSelected = viewModel::onFoodSelected,
        onSave = viewModel::save,
    )
}

/** Stateless meal editor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEditorScreen(
    uiState: MealEditorUiState,
    onClose: () -> Unit,
    onTypeChange: (MealType) -> Unit,
    onNameChange: (String) -> Unit,
    onKcalChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFoodSelected: (FoodItem) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSave = !uiState.isLoading && !uiState.isSaving
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.meal_editor_edit_title else R.string.meal_editor_add_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MealTypePicker(selected = uiState.type, onSelect = onTypeChange)
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.meal_editor_name)) },
                    singleLine = true,
                    isError = uiState.nameError != null,
                    supportingText = errorSupportingText(uiState.nameError),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = uiState.kcal,
                    onValueChange = onKcalChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.meal_editor_kcal)) },
                    suffix = { Text(stringResource(R.string.unit_kcal)) },
                    singleLine = true,
                    isError = uiState.kcalError != null,
                    supportingText = errorSupportingText(uiState.kcalError),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                )
                Button(
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_save))
                }
                HorizontalDivider()
                FoodSearchSection(
                    query = uiState.query,
                    search = uiState.search,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onFoodSelected = onFoodSelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealTypePicker(selected: MealType, onSelect: (MealType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.meal_editor_type), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MealType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = type == selected,
                    onClick = { onSelect(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = MealType.entries.size),
                    icon = {},
                    label = { Text(text = stringResource(type.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}

@Composable
private fun FoodSearchSection(
    query: String,
    search: FoodSearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFoodSelected: (FoodItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.food_search_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (search == FoodSearchUiState.Unavailable) {
            InfoCard(icon = R.drawable.ic_info, text = stringResource(R.string.food_search_unavailable))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.food_search_label)) },
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )
                OutlinedButton(
                    onClick = onSearch,
                    enabled = query.isNotBlank() && search != FoodSearchUiState.Loading,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.food_search_action))
                }
            }
            FoodSearchResults(search = search, onRetry = onSearch, onFoodSelected = onFoodSelected)
        }
    }
}

@Composable
private fun FoodSearchResults(search: FoodSearchUiState, onRetry: () -> Unit, onFoodSelected: (FoodItem) -> Unit) {
    when (search) {
        FoodSearchUiState.Unavailable -> {}

        FoodSearchUiState.Idle -> Text(
            text = stringResource(R.string.food_search_source),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FoodSearchUiState.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is FoodSearchUiState.Results -> if (search.items.isEmpty()) {
            Text(
                text = stringResource(R.string.food_search_no_results, search.query),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column {
                Text(
                    text = stringResource(R.string.food_search_count, search.items.size, search.totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                search.items.forEach { food -> FoodResultRow(food = food, onClick = { onFoodSelected(food) }) }
            }
        }

        is FoodSearchUiState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoCard(
                icon = R.drawable.ic_error,
                text = search.error.message(),
                containerColor = MaterialTheme.colorScheme.errorContainer,
            )
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

@Composable
private fun FoodResultRow(food: FoodItem, onClick: () -> Unit) {
    val details = listOfNotNull(
        food.servingSize?.let { stringResource(R.string.food_serving_size, it) },
        food.maker,
    ).joinToString(separator = " · ")
    ListItem(
        headlineContent = { Text(food.name) },
        supportingContent = if (details.isEmpty()) {
            null
        } else {
            { Text(details) }
        },
        trailingContent = {
            Text(
                text =
                    food.kcalPerServing?.let {
                        kcalText(it.roundToInt())
                    } ?: stringResource(R.string.food_kcal_unknown),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.food_use), onClick = onClick),
    )
}

@Composable
private fun FoodSearchError.message(): String = when (this) {
    FoodSearchError.Timeout -> stringResource(R.string.food_error_timeout)
    FoodSearchError.Network -> stringResource(R.string.food_error_network)
    is FoodSearchError.Http -> stringResource(R.string.food_error_http, code)
    is FoodSearchError.Api -> stringResource(R.string.food_error_api, code)
    FoodSearchError.InvalidResponse -> stringResource(R.string.food_error_invalid)
}

@PreviewLightDark
@Composable
private fun MealEditorScreenPreview() {
    BeatingYesterdayTheme(dynamicColor = false) {
        MealEditorScreen(
            uiState = MealEditorUiState(
                type = MealType.LUNCH,
                name = "Kimchi stew",
                kcal = "",
                kcalError = FieldError.Required,
                query = "Kimchi",
                search = FoodSearchUiState.Results(
                    query = "Kimchi",
                    items = listOf(
                        FoodItem("Kimchi stew", 412.5, "300", "Example Foods"),
                        FoodItem("Kimchi fried rice", null, null, null),
                    ),
                    totalCount = 2,
                ),
            ),
            onClose = {},
            onTypeChange = {},
            onNameChange = {},
            onKcalChange = {},
            onQueryChange = {},
            onSearch = {},
            onFoodSelected = {},
            onSave = {},
        )
    }
}
