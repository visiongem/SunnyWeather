package com.pyn.sunnyweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pyn.sunnyweather.R
import com.pyn.sunnyweather.ui.theme.LocalSkyColors
import kotlin.random.Random

@Composable
fun SkyBackdrop(modifier: Modifier = Modifier) {
    val sky = LocalSkyColors.current
    val stars = remember {
        val random = Random(24)
        List(70) { Triple(random.nextFloat(), random.nextFloat() * .7f, random.nextFloat()) }
    }
    val gradient = remember(sky) { Brush.verticalGradient(listOf(sky.top, sky.middle, sky.bottom)) }
    Box(modifier.background(gradient)) {
        Image(painterResource(R.drawable.sky_clouds), null, Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop, alpha = if (sky.night) .30f else .22f)
        if (sky.night) Canvas(Modifier.fillMaxSize()) {
            stars.forEach { (x,y,brightness) ->
                drawCircle(Color.White.copy(alpha = .12f + brightness * .24f),
                    radius = (.45f + brightness * .65f).dp.toPx(), center = Offset(x * size.width, y * size.height))
            }
        }
    }
}
