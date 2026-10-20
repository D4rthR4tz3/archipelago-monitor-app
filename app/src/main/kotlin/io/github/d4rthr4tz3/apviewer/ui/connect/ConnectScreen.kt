package io.github.d4rthr4tz3.apviewer.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.monitor.ConnectionConfig
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason
import io.github.d4rthr4tz3.apviewer.protocol.monitor.MonitorState
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme

/** Form for the server address and slot credentials. */
@Composable
fun ConnectScreen(
    state: MonitorState,
    onConnect: (ConnectionConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var address by rememberSaveable { mutableStateOf("") }
    var slotName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val isConnecting = state is MonitorState.Connecting
    val canConnect = !isConnecting && address.isNotBlank() && slotName.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ApColors.Background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.connect_title),
                color = ApColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ApTextField(
                value = address,
                onValueChange = { address = it },
                label = stringResource(R.string.connect_address),
                placeholder = stringResource(R.string.connect_address_hint),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            )
            ApTextField(
                value = slotName,
                onValueChange = { slotName = it },
                label = stringResource(R.string.connect_slot_name),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            ApTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.connect_password),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )
            Button(
                onClick = { onConnect(ConnectionConfig(address, slotName.trim(), password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = canConnect,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApColors.TextPrimary,
                    contentColor = ApColors.Background,
                    disabledContainerColor = ApColors.SegmentActive,
                    disabledContentColor = ApColors.TextTertiary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.connect_button),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            // Status text keeps a fixed minimum height so the form does not shift.
            Box(modifier = Modifier.heightIn(min = 40.dp)) {
                when (state) {
                    is MonitorState.Connecting -> Text(
                        text = stringResource(R.string.connect_connecting, state.url),
                        color = ApColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    is MonitorState.Failed -> Text(
                        text = failureText(state.reason),
                        color = ApColors.Error,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun ApTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ApColors.Surface,
            unfocusedContainerColor = ApColors.Surface,
            focusedBorderColor = ApColors.TextSecondary,
            unfocusedBorderColor = ApColors.InputBorder,
            focusedLabelColor = ApColors.TextSecondary,
            unfocusedLabelColor = ApColors.TextTertiary,
            focusedPlaceholderColor = ApColors.TextTertiary,
            unfocusedPlaceholderColor = ApColors.TextTertiary,
            focusedTextColor = ApColors.TextPrimary,
            unfocusedTextColor = ApColors.TextPrimary,
            cursorColor = ApColors.TextPrimary,
        ),
    )
}

@Composable
private fun failureText(reason: FailureReason): String = when (reason) {
    is FailureReason.Unreachable -> reason.message?.let {
        stringResource(R.string.error_unreachable_detail, it)
    } ?: stringResource(R.string.error_unreachable)

    is FailureReason.ConnectionLost -> reason.message?.let {
        stringResource(R.string.error_connection_lost_detail, it)
    } ?: stringResource(R.string.error_connection_lost)

    is FailureReason.Refused -> stringResource(
        R.string.error_refused,
        reason.errors.map { refusalText(it) }.joinToString(),
    )
}

@Composable
private fun refusalText(error: String): String = when (error) {
    "InvalidSlot" -> stringResource(R.string.error_invalid_slot)
    "InvalidPassword" -> stringResource(R.string.error_invalid_password)
    "IncompatibleVersion" -> stringResource(R.string.error_incompatible_version)
    else -> error
}

@Preview(backgroundColor = 0xFF141414, showBackground = true)
@Composable
private fun ConnectScreenPreview() {
    ApViewerTheme {
        ConnectScreen(
            state = MonitorState.Failed(FailureReason.Refused(listOf("InvalidSlot"))),
            onConnect = {},
        )
    }
}
