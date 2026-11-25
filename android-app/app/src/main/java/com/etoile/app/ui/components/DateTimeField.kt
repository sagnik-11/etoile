package com.etoile.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.etoile.app.ui.formatWhen
import com.etoile.app.ui.local
import com.etoile.app.ui.theme.Cream
import com.etoile.app.ui.theme.Dim
import com.etoile.app.ui.theme.Etoile
import com.etoile.app.ui.theme.Glass
import com.etoile.app.ui.theme.Hairline
import com.etoile.app.ui.theme.Muted
import com.etoile.app.ui.theme.NavyRaised
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Looks like a text field; tapping it opens a calendar, then a clock.
 * Only today and later can be picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    value: Instant?,
    onChange: (Instant) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val accent = Etoile.accent
    var step by rememberSaveable { mutableStateOf(0) }       // 0 closed, 1 calendar, 2 clock
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }   // epoch day

    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Glass)
                .border(BorderStroke(0.5.dp, if (value != null) accent.line else Hairline), RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) { step = 1 }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = if (value != null) accent.main else Dim, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                value?.let(::formatWhen) ?: "Choose date & time",
                style = MaterialTheme.typography.bodyLarge.copy(color = if (value != null) Cream else Muted),
            )
        }
    }

    val zone = ZoneId.systemDefault()
    if (step == 1) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val initial = value?.local()?.toLocalDate()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial ?: todayUtc,
            selectableDates = remember {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
                    override fun isSelectableYear(year: Int) = year >= LocalDate.now().year
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            colors = DatePickerDefaults.colors(containerColor = NavyRaised),
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        pickedDay = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                        step = 2
                    },
                ) { Text("Next", color = accent.main) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancel", color = Muted) } },
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = NavyRaised), showModeToggle = false)
        }
    }

    if (step == 2) {
        val initialTime = value?.local()?.toLocalTime() ?: LocalTime.of(19, 0)
        val state = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { step = 0 },
            containerColor = NavyRaised,
            title = { Text("What time?", style = MaterialTheme.typography.titleMedium) },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(clockDialColor = Glass, timeSelectorSelectedContainerColor = accent.soft, timeSelectorSelectedContentColor = accent.main),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val day = LocalDate.ofEpochDay(pickedDay ?: LocalDate.now().toEpochDay())
                    onChange(day.atTime(state.hour, state.minute).atZone(zone).toInstant())
                    step = 0
                }) { Text("Done", color = accent.main) }
            },
            dismissButton = { TextButton(onClick = { step = 1 }) { Text("Back", color = Muted) } },
        )
    }
    Spacer(Modifier.height(0.dp))
}
