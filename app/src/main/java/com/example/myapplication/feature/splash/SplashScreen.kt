package com.example.myapplication.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.remote.runCatchingCancellable
import kotlinx.coroutines.delay

private val LogoBackground = Color(0xFFDCE0E6)
private val TextPrimary = Color(0xFF111827)
private val IndicatorTrack = Color(0xFFE5E7EB)

/**
 * Vises når appen starter. Tjekker med [checkSignedIn] om brugeren er logget ind
 * og kalder [onFinished] med svaret, tidligst efter [durationMillis].
 */
@Composable
fun SplashScreen(
    checkSignedIn: suspend () -> Boolean,
    onFinished: (isSignedIn: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 1500
) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        val isSignedIn = runCatchingCancellable { checkSignedIn() }.getOrDefault(false)
        delay(durationMillis - (System.currentTimeMillis() - start))
        currentOnFinished(isSignedIn)
    }

    LoadingContent(modifier = modifier)
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(LogoBackground)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = TextPrimary,
                trackColor = IndicatorTrack,
                strokeWidth = 3.dp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun SplashScreenPreview() {
    LoadingContent()
}
