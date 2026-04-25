package com.mahmoud.kpdf.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahmoud.kpdf.ShowcaseScreen

@Composable
fun ScreenTabs(
    current: ShowcaseScreen,
    onSelect: (ShowcaseScreen) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShowcaseScreen.entries.forEach { screen ->
            val isSelected = screen == current

            if (isSelected) {
                Button(
                    onClick = { onSelect(screen) },
                    modifier = Modifier.widthIn(min = 120.dp),
                ) {
                    Text(
                        text = screen.label,
                        maxLines = 1,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(screen) },
                    modifier = Modifier.widthIn(min = 120.dp),
                ) {
                    Text(
                        text = screen.label,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
