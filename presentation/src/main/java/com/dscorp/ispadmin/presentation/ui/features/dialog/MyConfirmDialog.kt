package com.dscorp.ispadmin.presentation.ui.features.dialog

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dscorp.ispadmin.R

@Composable
fun MyConfirmDialog(
    title: String,
    @DrawableRes closeIcon: Int = R.drawable.ic_close,
    onDismissRequest: () -> Unit = {},
    body: @Composable () -> Unit,
    onAccept: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Dialog(
            onDismissRequest = {
                onDismissRequest()
            },
            properties = DialogProperties(dismissOnClickOutside = false)
        ) {

            Column(
                modifier = Modifier
                    .shadow(
                        elevation = 35.dp,
                        spotColor = Color.Black.copy(alpha = 0.3f),
                        ambientColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(size = 8.dp))
                    .padding(start = 34.dp, top = 24.dp, end = 34.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight(600),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    )
                    Image(
                        modifier = Modifier
                            .padding(1.dp)
                            .width(24.dp)
                            .height(24.dp)
                            .clickable {
                                onDismissRequest()
                            },
                        painter = painterResource(id = closeIcon),
                        contentDescription = "Cerrar diálogo",
                        contentScale = ContentScale.None,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )

                }

                Spacer(modifier = Modifier.height(16.dp))

                body()

                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = {
                    onDismissRequest()
                    onAccept()
                }) {
                    Text(text = "Aceptar")
                }
            }
        }
    }

}

@Preview(showSystemUi = true)
@Composable
fun MyConfirmCustomDialogPreview() {

    var showDialog by remember { mutableStateOf(true) }

    if (showDialog)
        MyConfirmDialog(
            title = "Información de cereza",
            onDismissRequest = {
                showDialog = false
            },
            onAccept = {},
            body = {
                Text(
                    text = "Nº de Cereza: 36253\nCampaña: CerezaCC-Ventas\nCanal: WhatsApp Meta Test Number",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight(400),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                )
            }
        )

}


