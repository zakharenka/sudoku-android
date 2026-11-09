package me.zakharenka.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import me.zakharenka.sudoku.ui.SudokuScreen
import me.zakharenka.sudoku.ui.theme.SudokuTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SudokuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent {
            SudokuTheme(themeMode = viewModel.themeMode) {
                SudokuScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (!isChangingConfigurations) {
            viewModel.onAppBackgrounded()
        }
    }
}
