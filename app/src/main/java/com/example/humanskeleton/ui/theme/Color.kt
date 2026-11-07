package com.example.humanskeleton.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 2D 骨骼动画编辑器 — Material Design 3 配色令牌。
 * 种子色 seed = #6750A4（与设计方案一致）。界面只允许使用语义角色，
 * 不写死十六进制，以便 Android 12+ 动态取色能整体替换而不破坏对比度。
 * 分工：primary=骨架、secondary=皮肤/贴图、tertiary=时间/选中态。
 */
val PurpleSeed        = Color(0xFF6750A4)
val PurpleOnSeed      = Color(0xFFFFFFFF)
val PurpleContainer    = Color(0xFFEADDFF)
val PurpleOnContainer  = Color(0xFF21005D)

val SecondarySeed     = Color(0xFF625B71)
val SecondaryOnSeed   = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE8DEF8)
val SecondaryOnContainer = Color(0xFF1D1922)

val TertiarySeed      = Color(0xFF7D5260)
val TertiaryOnSeed    = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFD8E4)
val TertiaryOnContainer = Color(0xFF31111D)

val NeutralBg         = Color(0xFFFEF7FF)
val NeutralSurface    = Color(0xFFFEF7FF)
val NeutralSurfaceVariant = Color(0xFFE7E0EC)
val NeutralOnSurface  = Color(0xFF1C1B1F)
val NeutralOnSurfaceVariant = Color(0xFF49454F)
val NeutralOutline    = Color(0xFF79747E)
val NeutralOutlineVariant = Color(0xFFCAC4D0)

// 浅色方案
val LightPrimary       = PurpleSeed
val LightOnPrimary     = PurpleOnSeed
val LightPrimaryContainer = PurpleContainer
val LightOnPrimaryContainer = PurpleOnContainer
val LightSecondary     = SecondarySeed
val LightOnSecondary   = SecondaryOnSeed
val LightSecondaryContainer = SecondaryContainer
val LightOnSecondaryContainer = SecondaryOnContainer
val LightTertiary      = TertiarySeed
val LightOnTertiary    = TertiaryOnSeed
val LightTertiaryContainer = TertiaryContainer
val LightOnTertiaryContainer = TertiaryOnContainer
val LightBackground    = NeutralBg
val LightOnBackground  = NeutralOnSurface
val LightSurface       = NeutralSurface
val LightOnSurface     = NeutralOnSurface
val LightSurfaceVariant = NeutralSurfaceVariant
val LightOnSurfaceVariant = NeutralOnSurfaceVariant
val LightOutline       = NeutralOutline
val LightOutlineVariant = NeutralOutlineVariant

// 深色方案（画布用 #1D1B20，不用纯黑）
val DarkPrimary       = Color(0xFFD0BCFF)
val DarkOnPrimary     = Color(0xFF381E72)
val DarkPrimaryContainer = Color(0xFF4F378B)
val DarkOnPrimaryContainer = Color(0xFFEADDFF)
val DarkSecondary     = Color(0xFFCCC2DC)
val DarkOnSecondary   = Color(0xFF332D41)
val DarkSecondaryContainer = Color(0xFF4A4458)
val DarkOnSecondaryContainer = Color(0xFFE8DEF8)
val DarkTertiary      = Color(0xFFEFB8C8)
val DarkOnTertiary    = Color(0xFF492532)
val DarkTertiaryContainer = Color(0xFF633B48)
val DarkOnTertiaryContainer = Color(0xFFFFD8E4)
val DarkBackground    = Color(0xFF1D1B20)
val DarkOnBackground  = Color(0xFFE6E0E9)
val DarkSurface       = Color(0xFF1D1B20)
val DarkOnSurface     = Color(0xFFE6E0E9)
val DarkSurfaceVariant = Color(0xFF49454F)
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
val DarkOutline       = Color(0xFF938F99)
val DarkOutlineVariant = Color(0xFF49454F)
