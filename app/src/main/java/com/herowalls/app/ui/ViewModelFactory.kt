package com.herowalls.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.herowalls.app.AppContainer
import com.herowalls.app.HeroWallsApp

/** Minimal factory so screens stay free of reflection-based DI. */
class HeroViewModelFactory(private val create: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}

@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { (context.applicationContext as HeroWallsApp).container }
}

@Composable
fun factory(create: () -> ViewModel): HeroViewModelFactory =
    remember(create) { HeroViewModelFactory(create) }
