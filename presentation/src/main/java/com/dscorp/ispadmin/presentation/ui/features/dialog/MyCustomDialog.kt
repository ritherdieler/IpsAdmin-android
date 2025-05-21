package com.dscorp.ispadmin.presentation.ui.features.dialog

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyIconButton

@Composable
fun MyCustomDialog(
    modifier: Modifier = Modifier,
    title: String = "",
    paddingValues: PaddingValues = PaddingValues(
        start = 34.dp,
        top = 26.dp,
        end = 34.dp,
        bottom = 37.dp
    ),
    @DrawableRes closeIcon: Int = R.drawable.ic_close,
    showCloseIcon: Boolean = true,
    backGroundColor: Color = Color.White,
    cancelable: Boolean = true,
    usePlatformDefaultWidth: Boolean = true,
    onDismissRequest: () -> Unit = {},
    content: @Composable (ColumnScope) -> Unit,
    ) {
    Dialog(
        onDismissRequest = {
            onDismissRequest()
        },
        properties = DialogProperties(
            dismissOnClickOutside = cancelable,
            usePlatformDefaultWidth = usePlatformDefaultWidth
        )
    ) {
        Box(
            modifier = Modifier
                .background(backGroundColor, shape = RoundedCornerShape(8.dp))
                .padding(paddingValues)
        ) {
            if (showCloseIcon) {
                MyIconButton (
                    onClick = { onDismissRequest() },
                    modifier = Modifier
                        .padding(8.dp)
                        .zIndex(3f)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(id = closeIcon),
                        contentDescription = "image description",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = modifier
                    .zIndex(2f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight(600),
                            color = Color(0xFF000034),
                            textAlign = TextAlign.Center,
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                content(this)
            }
        }

    }

}


@Preview(showSystemUi = true)
@Composable
fun MyCustomDialogPreview() {
//    CerezaAppTheme {

    var showDialog by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (showDialog)
            MyCustomDialog(
                title = "Información de cereza",
                onDismissRequest = {
                    showDialog = false
                }, content = {
                    Text(
                        text = "Nº de Cereza: 36253\nCampaña: CerezaCC-Ventas\nCanal: WhatsApp Meta Test Number",
                        style = TextStyle(
                            fontSize = 12.sp,
//                                fontFamily = FontFamily(Font(R.font.poppins)),
                            fontWeight = FontWeight(400),
                            color = Color(0xFF000034),
                            textAlign = TextAlign.Center,
                        )
                    )
                })
    }

}
