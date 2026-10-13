package com.hananakick.arrrr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hananakick.arrrr.reader.ReaderApp
import com.hananakick.arrrr.ui.theme.ArrrrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArrrrApp() }
    }
}

@Composable
fun ArrrrApp() {
    ArrrrTheme {
        ReaderApp()
    }
}

@Preview(showBackground = true, name = "RSS Reader — Material 3 Expressive")
@Composable
private fun ReaderPreview() {
    ArrrrApp()
}
