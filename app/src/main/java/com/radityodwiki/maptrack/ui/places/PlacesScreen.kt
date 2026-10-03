package com.radityodwiki.maptrack.ui.places

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radityodwiki.maptrack.R

@Composable
fun PlacesScreen(
    onPlaceClick: (String) -> Unit,
    onAddPlace: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize()) {
        val list = items
        when {
            list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            list.isEmpty() -> Text(stringResource(R.string.places_empty), Modifier.align(Alignment.Center))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Bottom padding keeps the last card clear of the add button.
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.placeId }) { item ->
                    PlaceRow(item, onClick = { onPlaceClick(item.placeId) })
                }
            }
        }
        FloatingActionButton(
            onClick = onAddPlace,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.place_add))
        }
    }
}

@Composable
private fun PlaceRow(item: PlaceListItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.place_radius, item.radius), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (item.lastVisitDate == null) {
                    stringResource(R.string.place_no_visits)
                } else {
                    stringResource(R.string.place_visit_summary, item.visitCount, item.lastVisitDate)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
