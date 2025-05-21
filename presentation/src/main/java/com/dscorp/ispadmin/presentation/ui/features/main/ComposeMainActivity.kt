package com.dscorp.ispadmin.presentation.ui.features.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.dscorp.ispadmin.navigation.IpsAdminNavHost
import com.dscorp.ispadmin.presentation.theme.MyTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ComposeMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MyTheme {
                IpsAdminNavHost()

            }
        }
        

}}
