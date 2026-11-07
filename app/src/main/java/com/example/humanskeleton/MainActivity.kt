package com.example.humanskeleton

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.humanskeleton.ui.EditorScreen
import com.example.humanskeleton.ui.theme.HumanSkeletonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 设计稿固定使用 #6750A4 基线色板，深色仅作为画布变体（设置项）
            HumanSkeletonTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EditorScreen()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditorScreenPreview() {
    HumanSkeletonTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            EditorScreen()
        }
    }
}
