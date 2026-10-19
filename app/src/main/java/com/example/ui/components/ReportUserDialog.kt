package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReportUserDialog(
    partnerName: String,
    onReportConfirmed: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf(
        "Black screen / Face not visible",
        "Inappropriate or explicit behavior",
        "Underage user",
        "Harassment or offensive language",
        "Spam or commercial advertising"
    )

    var selectedReason by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1F28),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Report",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Report & Block $partnerName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Why are you reporting this user? This user will be immediately blocked and skipped.",
                    fontSize = 13.sp,
                    color = Color(0xFFB0B0BA)
                )
                Spacer(modifier = Modifier.height(14.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFFF473A),
                                unselectedColor = Color(0xFF70707C)
                            )
                        )
                        Text(
                            text = reason,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReportConfirmed(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_report_button")
            ) {
                Text("Report & Ban", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_report_button")
            ) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
