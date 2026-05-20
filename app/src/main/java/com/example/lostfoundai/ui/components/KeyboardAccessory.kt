package com.example.lostfoundai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class KeyboardAccessoryState {
    var activeValue by mutableStateOf(TextFieldValue(""))
    var onValueChange: (TextFieldValue) -> Unit = {}
    var onDone: () -> Unit = {}
    var isActive by mutableStateOf(false)
}

val LocalKeyboardAccessory = staticCompositionLocalOf { KeyboardAccessoryState() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardAccessoryProvider(content: @Composable () -> Unit) {
    val state = remember { KeyboardAccessoryState() }
    val isImeVisible = WindowInsets.isImeVisible

    CompositionLocalProvider(LocalKeyboardAccessory provides state) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            // The user requested to hide the accessory input bar for now.
            // If it needs to be shown in the future, just uncomment this block.
            /*
            if (isImeVisible && state.isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = state.activeValue,
                            onValueChange = {
                                state.activeValue = it
                                state.onValueChange(it)
                            },
                            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                            cursorBrush = SolidColor(Color.Black),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { state.onDone() }),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { state.onDone() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                        ) {
                            Text("OK", color = Color.Black, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            */
        }
    }
}

@Composable
fun AccessoryOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: androidx.compose.ui.graphics.Shape = OutlinedTextFieldDefaults.shape,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val accessoryState = LocalKeyboardAccessory.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(value) {
        if (accessoryState.isActive && accessoryState.onValueChange == onValueChange) {
            accessoryState.activeValue = value
        }
    }

    val doneAction = {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        colors = colors,
        shape = shape,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardActions.onDone?.invoke(this)
                doneAction()
            },
            onGo = keyboardActions.onGo,
            onNext = keyboardActions.onNext,
            onPrevious = keyboardActions.onPrevious,
            onSearch = keyboardActions.onSearch,
            onSend = keyboardActions.onSend
        ),
        modifier = modifier.onFocusChanged {
            if (it.isFocused && !readOnly) {
                accessoryState.activeValue = value
                accessoryState.onValueChange = onValueChange
                accessoryState.onDone = doneAction
                accessoryState.isActive = true
            } else {
                if (accessoryState.onValueChange == onValueChange) {
                    accessoryState.isActive = false
                }
            }
        }
    )
}

@Composable
fun AccessoryOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: androidx.compose.ui.graphics.Shape = OutlinedTextFieldDefaults.shape,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(value)) }
    
    if (textFieldValueState.text != value) {
        textFieldValueState = textFieldValueState.copy(text = value)
    }

    AccessoryOutlinedTextField(
        value = textFieldValueState,
        onValueChange = {
            textFieldValueState = it
            if (it.text != value) {
                onValueChange(it.text)
            }
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        colors = colors,
        shape = shape,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        readOnly = readOnly,
        trailingIcon = trailingIcon
    )
}
