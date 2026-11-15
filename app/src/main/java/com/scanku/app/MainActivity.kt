package com.scanku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scanku.app.ui.DocumentScreen
import com.scanku.app.ui.HomeScreen
import com.scanku.app.ui.OcrScreen
import com.scanku.app.ui.PageEditorScreen
import com.scanku.app.ui.theme.ScanKuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScanKuTheme {
                ScanKuApp()
            }
        }
    }
}

@Composable
fun ScanKuApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(vm = vm, onOpenDoc = { nav.navigate("doc/$it") })
        }
        composable(
            route = "doc/{docId}",
            arguments = listOf(navArgument("docId") { type = NavType.StringType })
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            DocumentScreen(
                vm = vm,
                docId = docId,
                onBack = { nav.popBackStack() },
                onEditPage = { pageId -> nav.navigate("editor/$docId/$pageId") },
                onOcr = { pageId -> nav.navigate("ocr/$docId/$pageId") }
            )
        }
        composable(
            route = "editor/{docId}/{pageId}",
            arguments = listOf(
                navArgument("docId") { type = NavType.StringType },
                navArgument("pageId") { type = NavType.StringType }
            )
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            val pageId = entry.arguments?.getString("pageId") ?: return@composable
            PageEditorScreen(
                vm = vm,
                docId = docId,
                pageId = pageId,
                onBack = { nav.popBackStack() },
                onOcr = { nav.navigate("ocr/$docId/$pageId") }
            )
        }
        composable(
            route = "ocr/{docId}/{pageId}",
            arguments = listOf(
                navArgument("docId") { type = NavType.StringType },
                navArgument("pageId") { type = NavType.StringType }
            )
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            val pageId = entry.arguments?.getString("pageId") ?: return@composable
            OcrScreen(vm = vm, docId = docId, pageId = pageId, onBack = { nav.popBackStack() })
        }
    }
}
