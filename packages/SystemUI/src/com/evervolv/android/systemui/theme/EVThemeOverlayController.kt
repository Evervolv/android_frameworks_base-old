/*
 * Copyright (C) 2021 The Proton AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evervolv.android.systemui.theme

import android.annotation.ColorInt
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.om.FabricatedOverlay
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import com.android.systemui.broadcast.BroadcastDispatcher
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.dump.DumpManager
import com.android.systemui.keyguard.WakefulnessLifecycle
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.FeatureFlags
import com.android.systemui.statusbar.policy.DeviceProvisionedController
import com.android.systemui.theme.ThemeOverlayApplier
import com.android.systemui.theme.ThemeOverlayController
import com.android.systemui.util.settings.SecureSettings
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.cam.Zcam
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.data.Illuminants
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.colorkt.tristimulus.CieXyzAbs.Companion.toAbs
import dev.kdrag0n.colorkt.ucs.lab.CieLab
import dev.kdrag0n.monet.theme.DynamicColorScheme
import dev.kdrag0n.monet.theme.MaterialYouTargets
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow
import org.json.JSONObject

@SysUISingleton
class EVThemeOverlayController @Inject constructor(
    private val context: Context,
    broadcastDispatcher: BroadcastDispatcher,
    @Background bgHandler: Handler,
    @Main mainExecutor: Executor,
    @Background bgExecutor: Executor,
    themeOverlayApplier: ThemeOverlayApplier,
    secureSettings: SecureSettings,
    wallpaperManager: WallpaperManager,
    userManager: UserManager,
    deviceProvisionedController: DeviceProvisionedController,
    userTracker: UserTracker,
    dumpManager: DumpManager,
    featureFlags: FeatureFlags,
    wakefulnessLifecycle: WakefulnessLifecycle,
) : ThemeOverlayController(
    context,
    broadcastDispatcher,
    bgHandler,
    mainExecutor,
    bgExecutor,
    themeOverlayApplier,
    secureSettings,
    wallpaperManager,
    userManager,
    deviceProvisionedController,
    userTracker,
    dumpManager,
    featureFlags,
    wakefulnessLifecycle,
) {
    private var cond: Zcam.ViewingConditions
    private var targets: MaterialYouTargets

    private val colorOverride get() = Settings.Secure.getString(mContext.contentResolver, PREF_COLOR_OVERRIDE)
    private val chromaFactor get() = Settings.Secure.getFloat(mContext.contentResolver, PREF_CHROMA_FACTOR, 1.0f).toDouble()
    private val accurateShades get() = Settings.Secure.getInt(mContext.contentResolver, PREF_ACCURATE_SHADES, 1) != 0

    private val linearLightness get() = Settings.Secure.getInt(mContext.contentResolver, PREF_LINEAR_LIGHTNESS, 0) != 0
    private val whiteLuminance get() = parseWhiteLuminanceUser(
            Settings.Secure.getInt(mContext.contentResolver, PREF_WHITE_LUMINANCE, WHITE_LUMINANCE_USER_DEFAULT))

    init {
        val whiteLuminance = parseWhiteLuminanceUser(
            Settings.Secure.getInt(mContext.contentResolver, PREF_WHITE_LUMINANCE, WHITE_LUMINANCE_USER_DEFAULT)
        )
        val linearLightness = Settings.Secure.getInt(mContext.contentResolver, PREF_LINEAR_LIGHTNESS, 0) != 0

        cond = Zcam.ViewingConditions(
            surroundFactor = Zcam.ViewingConditions.SURROUND_AVERAGE,
            // sRGB
            adaptingLuminance = 0.4 * whiteLuminance,
            // Gray world
            backgroundLuminance = CieLab(
                L = 50.0,
                a = 0.0,
                b = 0.0,
            ).toXyz().y * whiteLuminance,
            referenceWhite = Illuminants.D65.toAbs(whiteLuminance),
        )

        targets = MaterialYouTargets(
            chromaFactor = chromaFactor,
            useLinearLightness = linearLightness,
            cond = cond,
        )
        ThemeSettingsObserver(bgHandler).observe()
    }

    // Seed colors
    override fun getNeutralColor(colors: WallpaperColors) = colors.primaryColor.toArgb()
    override fun getAccentColor(colors: WallpaperColors) = getNeutralColor(colors)

    override fun getOverlay(primaryColor: Int, type: Int): FabricatedOverlay {
        // Generate color scheme
        val colorScheme = DynamicColorScheme(
            targets = targets,
            seedColor = Srgb(primaryColor),
            chromaFactor = chromaFactor,
            cond = cond,
            accurateShades = accurateShades,
        )

        val (groupKey, colorsList) = when (type) {
            ACCENT -> "accent" to colorScheme.accentColors
            NEUTRAL -> "neutral" to colorScheme.neutralColors
            else -> error("Unknown type $type")
        }

        return FabricatedOverlay.Builder(context.packageName, groupKey, "android").run {
            colorsList.withIndex().forEach { listEntry ->
                val group = "$groupKey${listEntry.index + 1}"

                listEntry.value.forEach { (shade, color) ->
                    val colorSrgb = color.convert<Srgb>()
                    Log.d(TAG, "Color $group $shade = ${colorSrgb.toHex()}")
                    setColor("system_${group}_$shade", colorSrgb)
                }
            }

            // Override special modulated surface colors for performance and consistency
            if (type == NEUTRAL) {
                // surface light = neutral1 20 (L* 98)
                colorsList[0][20]?.let { setColor("surface_light", it) }

                // surface highlight dark = neutral1 650 (L* 35)
                colorsList[0][650]?.let { setColor("surface_highlight_dark", it) }

                // surface_header_dark_sysui = neutral1 950 (L* 5)
                colorsList[0][950]?.let { setColor("surface_header_dark_sysui", it) }
            }

            build()
        }
    }

    inner class ThemeSettingsObserver(handler: Handler?) : ContentObserver(handler) {
        private val COLOR_OVERRIDE_URI = Settings.Secure.getUriFor(PREF_COLOR_OVERRIDE)

        fun observe() {
            val resolver = mContext.contentResolver
            resolver.registerContentObserver(COLOR_OVERRIDE_URI, false, this)
            resolver.registerContentObserver(Settings.Secure.getUriFor(PREF_CHROMA_FACTOR), false, this)
            resolver.registerContentObserver(Settings.Secure.getUriFor(PREF_ACCURATE_SHADES), false, this)
            resolver.registerContentObserver(Settings.Secure.getUriFor(PREF_LINEAR_LIGHTNESS), false, this)
            resolver.registerContentObserver(Settings.Secure.getUriFor(PREF_WHITE_LUMINANCE), false, this)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri == COLOR_OVERRIDE_URI) {
                val color = Settings.Secure.getInt(mContext.contentResolver, PREF_COLOR_OVERRIDE)
                var jsonObj = JSONObject(Settings.Secure.getString(mContext.contentResolver, Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES))
                jsonObj.remove(OVERLAY_CATEGORY_ACCENT_COLOR)
                jsonObj.remove(OVERLAY_COLOR_SOURCE)
                jsonObj.remove(OVERLAY_CATEGORY_SYSTEM_PALETTE)
                if (color != -1) {
                    val colorStr = String.format("#%08x", color or (0xff shl 24))
                    jsonObj.put(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_PRESET)
                    jsonObj.put(OVERLAY_CATEGORY_ACCENT_COLOR, colorStr)
                    jsonObj.put(OVERLAY_CATEGORY_SYSTEM_PALETTE, colorStr)
                    jsonObj.put(TIMESTAMP_FIELD, System.currentTimeMillis())
                } else {
                    jsonObj.put(OVERLAY_SOURCE_BOTH, 1)
                    jsonObj.put(OVERLAY_COLOR_SOURCE, "home_wallpaper")
                    jsonObj.put(TIMESTAMP_FIELD, System.currentTimeMillis())
                }
                Settings.Secure.putString(mContext.contentResolver, Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, jsonObj.toString())
            } else {
                // update fields not taken care of in getOverlay()
                cond = Zcam.ViewingConditions(
                    surroundFactor = Zcam.ViewingConditions.SURROUND_AVERAGE,
                    // sRGB
                    adaptingLuminance = 0.4 * whiteLuminance,
                    // Gray world
                    backgroundLuminance = CieLab(
                        L = 50.0,
                        a = 0.0,
                        b = 0.0,
                    ).toXyz().y * whiteLuminance,
                    referenceWhite = Illuminants.D65.toAbs(whiteLuminance),
                )

                targets = MaterialYouTargets(
                    chromaFactor = chromaFactor,
                    useLinearLightness = linearLightness,
                    cond = cond,
                )
                // Just do a dummy update, update timestamp_field
                var jsonObj = JSONObject(Settings.Secure.getString(mContext.contentResolver, Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES))
                jsonObj.put(TIMESTAMP_FIELD, System.currentTimeMillis())
                Settings.Secure.putString(mContext.contentResolver, Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, jsonObj.toString())
            }
        }
    }

    companion object {
        private const val TAG = "EVThemeOverlayController"

        private const val PREF_PREFIX = "monet_engine"
        private const val PREF_COLOR_OVERRIDE = "${PREF_PREFIX}_color_override"
        private const val PREF_CHROMA_FACTOR = "${PREF_PREFIX}_chroma_factor"
        private const val PREF_ACCURATE_SHADES = "${PREF_PREFIX}_accurate_shades"
        private const val PREF_LINEAR_LIGHTNESS = "${PREF_PREFIX}_linear_lightness"
        private const val PREF_WHITE_LUMINANCE = "${PREF_PREFIX}_white_luminance_user"

        private const val OVERLAY_PREFIX = "android.theme.customization"
        private const val OVERLAY_CATEGORY_ACCENT_COLOR = "${OVERLAY_PREFIX}.accent_color"
        private const val OVERLAY_CATEGORY_SYSTEM_PALETTE = "${OVERLAY_PREFIX}.system_palette"
        private const val OVERLAY_COLOR_SOURCE = "${OVERLAY_PREFIX}.color_source"
        private const val OVERLAY_SOURCE_BOTH = "${OVERLAY_PREFIX}.color_both"
        private const val COLOR_SOURCE_PRESET = "preset"
        private const val TIMESTAMP_FIELD = "_applied_timestamp"

        private const val WHITE_LUMINANCE_MIN = 1.0
        private const val WHITE_LUMINANCE_MAX = 10000.0
        private const val WHITE_LUMINANCE_USER_MAX = 1000
        private const val WHITE_LUMINANCE_USER_DEFAULT = 425 // ~200.0 divisible by step (decoded = 199.526)

        private fun parseWhiteLuminanceUser(userValue: Int): Double {
            val userSrc = userValue.toDouble() / WHITE_LUMINANCE_USER_MAX
            val userInv = 1.0 - userSrc
            return (10.0).pow(userInv * log10(WHITE_LUMINANCE_MAX))
                    .coerceAtLeast(WHITE_LUMINANCE_MIN)
        }

        private fun FabricatedOverlay.Builder.setColor(name: String, @ColorInt color: Int) =
            setResourceValue("android:color/$name", TypedValue.TYPE_INT_COLOR_ARGB8, color)

        private fun FabricatedOverlay.Builder.setColor(name: String, color: Color): FabricatedOverlay.Builder {
            val rgb = color.convert<Srgb>().toRgb8()
            val argb = rgb or (0xff shl 24)
            return setColor(name, argb)
        }
    }
}
