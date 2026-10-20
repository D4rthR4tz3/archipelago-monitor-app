package io.github.d4rthr4tz3.apviewer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import io.github.d4rthr4tz3.apviewer.ui.theme.GeistMono

/** A 48 dp icon button in the primary text color. */
@Composable
fun ApIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ApColors.TextPrimary,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp), enabled = enabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) tint else ApColors.TextTertiary,
        )
    }
}

/** Header row of a screen: 64 dp high, optional navigation icon, title and actions. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = ApColors.TextSecondary,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = if (navigation == null) 20.dp else 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        navigation?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = if (navigation == null && subtitle == null) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = ApColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

/** Small gray heading above a group of settings. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = ApColors.TextSecondary,
    )
}

/** Text field with its label above, as in the design. */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    monospace: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ApColors.TextSecondary,
        )
        ApTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder,
            textStyle = if (monospace) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = GeistMono)
            } else {
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
                )
            },
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
        )
    }
}

/**
 * Single-line field: surface background, gray border, 8 dp corners, [height] high. Built on
 * [BasicTextField] because [androidx.compose.material3.OutlinedTextField] enforces its own minimum
 * height and padding, which made it taller than the controls next to it.
 */
@Composable
fun ApTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = ApColors.Surface,
        unfocusedContainerColor = ApColors.Surface,
        disabledContainerColor = ApColors.Surface,
        focusedBorderColor = ApColors.TextSecondary,
        unfocusedBorderColor = ApColors.InputBorder,
        disabledBorderColor = ApColors.InputBorder,
        focusedPlaceholderColor = ApColors.TextTertiary,
        unfocusedPlaceholderColor = ApColors.TextTertiary,
        disabledPlaceholderColor = ApColors.TextTertiary,
        focusedTextColor = ApColors.TextPrimary,
        unfocusedTextColor = ApColors.TextPrimary,
        disabledTextColor = ApColors.TextSecondary,
        cursorColor = ApColors.TextPrimary,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(height),
        enabled = enabled,
        textStyle = textStyle.copy(
            color = if (enabled) ApColors.TextPrimary else ApColors.TextSecondary,
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(ApColors.TextPrimary),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                placeholder = placeholder?.let { { Text(text = it, style = textStyle) } },
                trailingIcon = trailingIcon,
                colors = colors,
                contentPadding = PaddingValues(horizontal = 12.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = MaterialTheme.shapes.small,
                        focusedBorderThickness = 1.dp,
                        unfocusedBorderThickness = 1.dp,
                    )
                },
            )
        },
    )
}

/** Light filled button for the main action of a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = if (icon == null) 16.dp else 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ApColors.TextPrimary,
            contentColor = ApColors.Background,
            disabledContainerColor = ApColors.SegmentActive,
            disabledContentColor = ApColors.TextTertiary,
        ),
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null)
            Box(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Neutral outlined button for secondary actions. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, ApColors.SegmentActive),
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ApColors.TextPrimary,
            disabledContentColor = ApColors.TextTertiary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

/** Text-only button, in red for destructive actions. */
@Composable
fun PlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ApColors.TextSecondary,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(text = text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

/** Switch drawn like the design: light track when on, gray when off, no thumb icon. */
@Composable
fun ApSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 60.dp, height = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .background(
                    if (checked) ApColors.TextPrimary else ApColors.SegmentActive,
                    RoundedCornerShape(12.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (checked) 23.dp else 3.dp, y = 3.dp)
                    .size(18.dp)
                    .background(
                        if (checked) ApColors.Background else ApColors.TextTertiary,
                        CircleShape,
                    ),
            )
        }
    }
}

/**
 * A row with a label, an optional description and a switch on the right. Without [inset] the row
 * has no horizontal padding, for sections that are padded already.
 */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    leading: (@Composable () -> Unit)? = null,
    inset: Boolean = true,
    labelColor: Color = ApColors.TextPrimary,
    labelWeight: FontWeight? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(
                start = if (inset) 16.dp else 0.dp,
                end = if (inset) 4.dp else 0.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                fontWeight = labelWeight,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ApColors.TextTertiary,
                )
            }
        }
        ApSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Segmented control: equal segments in a surface track, the selected one lighter. */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(ApColors.Surface, MaterialTheme.shapes.small)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) ApColors.SegmentActive else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = {
                        onSelect(value)
                    }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) ApColors.TextPrimary else ApColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Radio indicator: a ring, with a dot when selected. */
@Composable
fun RadioDot(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .border(2.dp, if (selected) ApColors.TextPrimary else RadioOff, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(modifier = Modifier.size(10.dp).background(ApColors.TextPrimary, CircleShape))
        }
    }
}

private val RadioOff = Color(0xFF5A5A5A)
