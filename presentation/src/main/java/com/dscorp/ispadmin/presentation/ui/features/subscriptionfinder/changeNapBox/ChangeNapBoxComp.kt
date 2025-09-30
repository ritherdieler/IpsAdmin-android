package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.changeNapBox

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.NapBox
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.presentation.ui.components.Loader
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.NapBoxesState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeNapBoxComp(
    currentNapBox: NapBox,
    napBoxListState: NapBoxesState,
    onChangeNapBoxClick: (NapBoxResponse) -> Unit,
    onNapBoxChanged: () -> Unit
) {
    var color = colorResource(id = R.color.white)
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current
    Column(
        modifier = Modifier.background(color = color, shape = RoundedCornerShape(8.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        when (napBoxListState) {
            is NapBoxesState.Error -> {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
            }

            is NapBoxesState.Loading -> {
                Loader()
            }

            is NapBoxesState.NapBoxListLoaded -> {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = "${currentNapBox.code} - (${currentNapBox.address})",
                        onValueChange = {},
                        enabled = false,
                    )
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = !menuExpanded }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .clickable(onClick = {
                                    menuExpanded = !menuExpanded
                                }),
                            value = TextFieldValue(

                                if (selectedIndex == -1) "Seleccione una opción"
                                else "${napBoxListState.items[selectedIndex].code} - ${napBoxListState.items[selectedIndex].address}"
                            ),
                            onValueChange = { },
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = menuExpanded
                                )
                            },
                        )

                        ExposedDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            napBoxListState.items.forEachIndexed { index, napBox ->
                                DropdownMenuItem(
                                    text = { Text("${napBox.code} - (${napBox.address})") },
                                    onClick = {
                                        selectedIndex = index
                                        menuExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onChangeNapBoxClick(napBoxListState.items[selectedIndex]) },
                    ) {
                        Text(text = "Cambiar")
                    }
                }
            }


            NapBoxesState.NapBoxChanged -> {
                Toast.makeText(context, "Caja nap cambiada correctamente", Toast.LENGTH_LONG).show()
                onNapBoxChanged()
            }
        }

    }

}


//@Preview(showBackground = true)
//@Composable
//private fun ChangeNapBoxPreview() {
//    ChangeNapBoxComp(
//        modifier = Modifier.fillMaxWidth(),
//        currentNapBox = NapBox("4", "NapBox 4", "Calle 4"),
//        napBoxListFlow = listOf(
//            NapBox("1", "NapBox 1", "Calle 1"),
//            NapBox("2", "NapBox 2", "Calle 2")
//        ),
//        onChangeNapBoxClick = {}
//    )
//}