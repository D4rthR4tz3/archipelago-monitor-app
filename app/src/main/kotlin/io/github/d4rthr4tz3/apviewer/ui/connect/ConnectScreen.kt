package io.github.d4rthr4tz3.apviewer.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.monitor.ConnectionConfig
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason
import io.github.d4rthr4tz3.apviewer.protocol.monitor.MonitorState
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme

/** Form for the server address and slot credentials. */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.connect_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.connect_address)) },
                placeholder = { Text(stringResource(R.string.connect_address_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = slotName,
                onValueChange = { slotName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.connect_slot_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.connect_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )
            Button(
                onClick = { onConnect(ConnectionConfig(address, slotName.trim(), password)) },
                modifier = Modifier.align(Alignment.End),
                enabled = canConnect,
            ) {
                Text(stringResource(R.string.connect_button))
            }
            when (state) {
                is MonitorState.Connecting -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.connect_connecting, state.url))
                }

                is MonitorState.Failed -> Text(
                    text = failureText(state.reason),
                    color = MaterialTheme.colorScheme.error,
                )

                else -> Unit
            }
        }
    }
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

@Preview(showBackground = true)
@Composable
private fun ConnectScreenPreview() {
    ApViewerTheme {
        ConnectScreen(
            state = MonitorState.Failed(FailureReason.Refused(listOf("InvalidSlot"))),
            onConnect = {},
        )
    }
}
