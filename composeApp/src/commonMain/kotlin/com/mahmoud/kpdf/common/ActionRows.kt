package com.mahmoud.kpdf.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickActionRow(
    firstLabel: String,
    firstAction: () -> Unit,
    secondLabel: String,
    secondAction: () -> Unit,
    thirdLabel: String,
    thirdAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilledTonalButton(
            onClick = firstAction,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Text(firstLabel)
        }
        OutlinedButton(
            onClick = secondAction,
            modifier = Modifier.weight(1f),
        ) {
            Text(secondLabel)
        }
        OutlinedButton(
            onClick = thirdAction,
            modifier = Modifier.weight(1f),
        ) {
            Text(thirdLabel)
        }
    }
}
