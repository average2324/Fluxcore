package com.luminadigitale.fluxcore.core.ui

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.luminadigitale.fluxcore.core.CommercePlatform
import com.luminadigitale.fluxcore.core.GameDependencies
import com.luminadigitale.fluxcore.core.engine.CollisionEngine
import com.luminadigitale.fluxcore.core.engine.GameSimulation
import com.luminadigitale.fluxcore.core.engine.LevelCatalog
import com.luminadigitale.fluxcore.core.engine.LevelConfig
import com.luminadigitale.fluxcore.core.engine.Obstacle
import com.luminadigitale.fluxcore.core.engine.RunPhase
import com.luminadigitale.fluxcore.core.math.AngleMath
import com.luminadigitale.fluxcore.core.ads.RewardedLifeResult
import com.luminadigitale.fluxcore.core.lives.LivesManager
import com.luminadigitale.fluxcore.core.lives.LivesState
import com.luminadigitale.fluxcore.core.lives.PreferencesLivesRepository
import com.luminadigitale.fluxcore.core.premium.PremiumProduct
import com.luminadigitale.fluxcore.core.premium.PremiumPurchaseResult
import com.luminadigitale.fluxcore.core.premium.PremiumStatus
import com.luminadigitale.fluxcore.core.profile.AppLanguage
import com.luminadigitale.fluxcore.core.profile.BestScoreManager
import com.luminadigitale.fluxcore.core.profile.GameDifficulty
import com.luminadigitale.fluxcore.core.profile.PreferencesBestScoreRepository
import com.luminadigitale.fluxcore.core.profile.PreferencesSettingsRepository
import com.luminadigitale.fluxcore.core.profile.SettingsManager
import com.luminadigitale.fluxcore.core.profile.SettingsState
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class GameScreen(
    private val dependencies: GameDependencies
) : ScreenAdapter() {
    private enum class OverlayMode {
        SPLASH,
        EPILEPSY_WARNING,
        INTRO,
        MENU,
        PAUSE,
        SHOP,
        PREMIUM,
        POLICY,
        LEVEL_SELECT,
        TRANSITION,
        GAME
    }

    private enum class PolicyPage {
        PRIVACY,
        TERMS,
        LICENSE
    }

    private enum class ShopCategory {
        SHIPS,
        SHIELDS
    }

    private enum class AsteroidVariant {
        SLAB,
        CHIPPED,
        DENSE
    }

    private data class TouchControlsLayout(
        val leftX: Float,
        val rightX: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    private data class UiRect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    private data class ArenaLayout(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val yScale: Float
    )

    private data class ResultOverlayLayout(
        val card: UiRect,
        val titleY: Float,
        val summaryY: Float,
        val timeY: Float,
        val primaryButton: UiRect,
        val secondaryButton: UiRect,
        val offerCard: UiRect,
        val offerButton: UiRect
    )

    private data class UiScaleTokens(
        val xs: Float,
        val sm: Float,
        val md: Float,
        val lg: Float,
        val xl: Float,
        val insetX: Float,
        val safeTop: Float,
        val safeBottom: Float
    )

    private data class HudLayoutModel(
        val leftGroup: UiRect,
        val centerGroup: UiRect,
        val rightGroup: UiRect,
        val supportGroup: UiRect,
        val progressTrack: UiRect
    )

    private data class FlowTimingModel(
        val countdownSeconds: Float,
        val readyCardInSeconds: Float,
        val resultCardInSeconds: Float,
        val drainCueSeconds: Float
    )

    private enum class ChipTone {
        NEUTRAL,
        WARNING,
        ALERT,
        SUCCESS
    }

    private enum class PremiumDialogType {
        NONE,
        PROCESSING,
        SUCCESS,
        FAILURE
    }

    private enum class ArenaLinePurpose {
        BOUNDARY_REINFORCE,
        SAFE_ORBIT_GUIDE,
        TIMING_CADENCE,
        PROGRESS_CONTEXT,
        REVERSE_WARNING
    }

    private data class ArenaLineLayer(
        val purpose: ArenaLinePurpose,
        val color: Color,
        val alpha: Float,
        val enabled: Boolean
    )

    private data class ShipSkin(
        val id: String,
        val displayName: String,
        val assetPath: String?,
        val texture: Texture?,
        val price: Int,
        val noseDirectionDeg: Float,
        val hitRadiusNorm: Float
    )

    private data class ShieldStoreItem(
        val id: String,
        val displayName: String,
        val price: Int,
        val amount: Int,
        val kind: SupportStoreKind
    )

    private enum class SupportStoreKind {
        SHIELD,
        SLOW
    }

    private enum class TutorialAnchor {
        READY_CARD,
        LEFT_CONTROL,
        RIGHT_CONTROL,
        ARENA,
        SUPPORT_PANEL
    }

    private data class TutorialStep(
        val titleEn: String,
        val titleTr: String,
        val detailEn: String,
        val detailTr: String,
        val anchor: TutorialAnchor
    )

    private data class BlockBriefingStep(
        val titleEn: String,
        val titleTr: String,
        val detailEn: String,
        val detailTr: String,
        val anchor: TutorialAnchor
    )

    private data class StarSample(
        val xNorm: Float,
        val yNorm: Float,
        val size: Float,
        val twinkleSpeed: Float,
        val phase: Float,
        val alphaBase: Float
    )

    private companion object {
        private const val PROFILE_PREFS_KEY = "fluxcore_profile"
        private const val LEGACY_PROFILE_PREFS_KEY = "orbit_flux_profile"
        private const val PREFS_MIGRATION_DONE_KEY = "prefs_migrated_to_fluxcore"
        private const val STORE_SELECTED_KEY = "store_selected"
        private const val STORE_UNLOCKED_KEY = "store_unlocked"
        private const val STORE_ROTATION_KEY = "store_rotation_"
        private const val STORE_COINS_KEY = "store_coins"
        private const val STORE_TUTORIAL_DONE_KEY = "tutorial_done"
        private const val STORE_BLOCK_BRIEFING_KEY = "block_briefing_seen"
        private const val STORE_PREMIUM_OWNED_KEY = "premium_owned_cache_v2"
        private const val STORE_LEVEL_CLEAR_COUNT_KEY = "level_clear_count"
        private const val STORE_SHIELD_COUNT_KEY = "shield_count"
        private const val STORE_SLOW_POWER_COUNT_KEY = "slow_power_count"
        private const val STORE_SLOW_STOCK_SEEDED_KEY = "slow_stock_seeded_v1"
        private const val STORE_LIVES_EXPANDED_KEY = "lives_expanded_to_10"
        private const val MAX_LIVES = 10
        private const val MAX_SHIELDS = 5
        private const val MAX_SLOW_POWERS = 5
        private const val SLOW_POWER_PRICE = 760
        private const val SHIELD_PRICE = 620
        private const val LIFE_REFILL_SECONDS = 1_800L
        private const val MAX_SIMULATION_STEPS_PER_FRAME = 6
    }

    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(1080f, 1920f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = createFont("fonts/NotoSans-SemiBold.ttf", 34)
    private val titleFont = createFont("fonts/NotoSans-SemiBold.ttf", 58)
    private val uiTitleFont = createFont("fonts/NotoSans-SemiBold.ttf", 44)
    private val metricFont = createFont("fonts/NotoSans-SemiBold.ttf", 68)
    private val bodyFont = createFont("fonts/NotoSans-SemiBold.ttf", 30)
    private val metaFont = createFont("fonts/NotoSans-SemiBold.ttf", 24)
    private val chipFont = createFont("fonts/NotoSans-SemiBold.ttf", 22)
    private val buttonFont = createFont("fonts/NotoSans-SemiBold.ttf", 30)
    private val fontLayout = GlyphLayout()
    private val titleLayout = GlyphLayout()
    private val textWidthCache = HashMap<String, Float>()
    private val wrappedTextCache = HashMap<String, List<String>>()

    private val levels = LevelCatalog.create(100)
    private val simulation = GameSimulation(levels, dependencies.campaignSeed)
    private val chromeBackground = Color(0.05f, 0.07f, 0.11f, 1f)
    private val chromeBackgroundLift = Color(0.07f, 0.1f, 0.16f, 1f)
    private val chromeSurface = Color(0.08f, 0.14f, 0.2f, 0.98f)
    private val chromeSurfaceRaised = Color(0.1f, 0.17f, 0.24f, 1f)
    private val chromeInset = Color(0.06f, 0.11f, 0.17f, 1f)
    private val chromeAccent = Color(0.95f, 0.62f, 0.3f, 1f)
    private val chromeAccentSoft = Color(0.95f, 0.62f, 0.3f, 0.22f)
    private val chromeStroke = Color(0.72f, 0.86f, 0.93f, 0.72f)
    private val chromeInk = Color(0.94f, 0.98f, 1f, 1f)
    private val chromeMuted = Color(0.7f, 0.82f, 0.88f, 1f)
    private val reactorGround = Color.valueOf("020614")
    private val reactorTrack = Color.valueOf("0A2A3A")
    private val reactorSafeGap = Color.valueOf("7FFFD4")
    private val reactorDanger = Color.valueOf("FF6A3D")
    private val spaceBackground = Color.valueOf("040B1D")
    private val spaceBackgroundDeep = Color.valueOf("020612")
    private val shipTierCoreNeon = listOf(
        Color.valueOf("2DF8FF"),
        Color.valueOf("3E8BFF"),
        Color.valueOf("4DFFDE"),
        Color.valueOf("79FF63"),
        Color.valueOf("FFC34D"),
        Color.valueOf("FF5AD4"),
        Color.valueOf("7ACBFF"),
        Color.valueOf("7BFF8E"),
        Color.valueOf("FF8F72"),
        Color.valueOf("D1A2FF"),
        Color.valueOf("9DFFE8")
    )
    private val shipTierOutlineNeon = listOf(
        Color.valueOf("63EDFF"),
        Color.valueOf("7AB0FF"),
        Color.valueOf("70FFE9"),
        Color.valueOf("B4FF68"),
        Color.valueOf("FFD76C"),
        Color.valueOf("FF8FF1"),
        Color.valueOf("A6DDFF"),
        Color.valueOf("B9FFB0"),
        Color.valueOf("FFC1A8"),
        Color.valueOf("E6CBFF"),
        Color.valueOf("C8FFF3")
    )
    private val shipPoints = FloatArray(42)
    private val colorScratchA = Color()
    private val colorScratchB = Color()
    private val starfieldSamples = buildStarfieldSamples(156)
    private val flashNeonPalette = listOf(
        Color.valueOf("63EDFF"),
        Color.valueOf("7AB0FF"),
        Color.valueOf("70FFE9"),
        Color.valueOf("B4FF68"),
        Color.valueOf("FFD76C"),
        Color.valueOf("FF8FF1"),
        Color.valueOf("FF6A9D")
    )

    private val profilePreferences = Gdx.app.getPreferences(PROFILE_PREFS_KEY)
    private val legacyProfilePreferences = Gdx.app.getPreferences(LEGACY_PROFILE_PREFS_KEY)
    private val settingsManager = SettingsManager(
        repository = PreferencesSettingsRepository(profilePreferences)
    )
    private val bestScoreManager = BestScoreManager(
        repository = PreferencesBestScoreRepository(profilePreferences)
    )
    private val livesManager = LivesManager(
        repository = PreferencesLivesRepository(profilePreferences),
        maxLives = MAX_LIVES,
        refillIntervalSeconds = LIFE_REFILL_SECONDS
    )

    private var settingsState: SettingsState = settingsManager.snapshot()
    private var overlayMode: OverlayMode = OverlayMode.SPLASH
    private var selectedLevelIndex = 0
    private var accumulator = 0f
    private var runResultCommitted = true
    private var statusMessage = ""
    private var menuHint = "SURVIVE THE MAZE"
    private val tempTouch = Vector2()
    private var worldTime = 0f
    private var smoothedFrameDelta = GameSimulation.FIXED_TIMESTEP_SECONDS
    private var lowPerformanceMode = false
    private var performanceSampleSeconds = 0f
    private var performanceSampleFrames = 0
    private var smoothedFps = 60f
    private var shakeTimeRemaining = 0f
    private var shakePower = 0f
    private var screenFlashAlpha = 0f
    private val flashColor = Color(0.72f, 0.86f, 1f, 1f)
    private var motionSpeedIntensity = 0f
    private var motionSignedIntensity = 0f
    private var motionTiltDegrees = 0f
    private var speedEdgeGate = 0f
    private var appliedCameraTiltDegrees = 0f
    private var lastPlayerAngleSampleRad = 0f
    private var visualPlayerAngleRad = 0f
    private var angleSamplerReady = false
    private var lastPhaseActive = false
    private var lastShieldBreakCounter = 0
    private var lastTimeBubbleActive = false
    private var lastObstacleCountForPassCue = 0
    private var selectedPolicyPage: PolicyPage = PolicyPage.PRIVACY
    private var splashRemainingSeconds = 1.35f
    private var transitionRemainingSeconds = 0f
    private var policyScrollOffset = 0f
    private var policyDragPointer = -1
    private var lastPolicyDragY = 0f
    private var levelSelectScrollOffset = 0f
    private var levelSelectDragPointer = -1
    private var lastLevelSelectDragY = 0f
    private var levelSelectDragStartY = 0f
    private var levelSelectDragActive = false
    private var levelSelectScrollbarDrag = false
    private var levelSelectTapCandidateLevel = -1
    private var levelSelectScrollHintDismissed = false
    private var freeTurnImpulseDirection = 0
    private var freeTurnImpulseRemaining = 0f
    private var leftStepPressedLastFrame = false
    private var rightStepPressedLastFrame = false
    private var readyCountdownActive = false
    private var readyCountdownSeconds = 0f
    private var lastObservedRunPhase: RunPhase = RunPhase.READY
    private var simulationControlEnabled = dependencies.simulationModeEnabled
    private var menuReturnToPause = false
    private var premiumReturnOverlay = OverlayMode.INTRO
    private var simulationResultDelaySeconds = 0f
    private var simDiagFrames = 0
    private var simulationStepCooldownSeconds = 0f
    private val shipSkins = ArrayList<ShipSkin>()
    private val unlockedShipIds = LinkedHashSet<String>()
    private val shipRotationOverrides = HashMap<String, Float>()
    private val shieldStoreItems = listOf(
        ShieldStoreItem(
            id = "shield_single",
            displayName = t("Impact Shield", "Darbe Kalkanı"),
            price = SHIELD_PRICE,
            amount = 1,
            kind = SupportStoreKind.SHIELD
        ),
        ShieldStoreItem(
            id = "slow_single",
            displayName = t("Slowdown Charge", "Yavaşlatma Yükü"),
            price = SLOW_POWER_PRICE,
            amount = 1,
            kind = SupportStoreKind.SLOW
        )
    )
    private var selectedShipId = ""
    private var selectedShopShipIndex = 0
    private var selectedShopShieldIndex = 0
    private var selectedShopCategory = ShopCategory.SHIPS
    private var coinBalance = profilePreferences.getInteger(STORE_COINS_KEY, 0).coerceAtLeast(0)
    private var premiumEnabled = profilePreferences.getBoolean(STORE_PREMIUM_OWNED_KEY, false)
    private var premiumStoreStatus = PremiumStatus(
        isOwned = premiumEnabled,
        isLoading = true,
        isAvailableForPurchase = false,
        product = null
    )
    private var premiumDialogType = PremiumDialogType.NONE
    private var premiumDialogTitle = ""
    private var premiumDialogBody = ""
    private var premiumDialogPrimaryLabel = ""
    private var premiumDialogSecondaryLabel = ""
    private var premiumDialogTone = ChipTone.NEUTRAL
    private var premiumDialogRetryOnPrimary = false
    private var premiumProcessingSeconds = 0f
    private var levelClearCount = profilePreferences.getInteger(STORE_LEVEL_CLEAR_COUNT_KEY, 0)
    private var shieldCount = profilePreferences.getInteger(STORE_SHIELD_COUNT_KEY, MAX_SHIELDS).coerceIn(0, MAX_SHIELDS)
    private var slowPowerCount = profilePreferences.getInteger(STORE_SLOW_POWER_COUNT_KEY, MAX_SLOW_POWERS).coerceIn(0, MAX_SLOW_POWERS)
    private var shieldArmedForRun = false
    private var shieldUsedThisRun = false
    private var slowUsedThisRun = false
    private var reviveUsedThisRun = false
    private var adActionInProgress = false
    private var pendingInterstitialAfterClear = false
    private var bannerVisible = false
    private var lastLevelClearCoinsAwarded = 0
    private var levelClearDoubleClaimed = false
    private var livesState: LivesState = livesManager.snapshot(dependencies.epochSecondsProvider.nowEpochSeconds())
    private var tutorialActive = false
    private var tutorialStepIndex = 0
    private var tutorialPaused = false
    private var tutorialTouchIcon: Texture? = null
    private var uiHeartIcon: Texture? = null
    private var uiShieldIcon: Texture? = null
    private var uiCoinIcon: Texture? = null
    private val shownBlockBriefings = LinkedHashSet<Int>()
    private var blockBriefingVisible = false
    private var blockBriefingLevel = 1
    private var blockBriefingStepIndex = 0
    private var shopNoticeMessage = ""
    private var shopNoticeTimer = 0f
    private var uiStartSound: Sound? = null
    private var uiConfirmSound: Sound? = null
    private var hitSound: Sound? = null
    private var clearSound: Sound? = null
    private var stormSound: Sound? = null
    private var wallPassSound: Sound? = null
    private var shieldActivateSound: Sound? = null
    private var slowActivateSound: Sound? = null
    private var gameMusic: Music? = null
    private var secondaryGameMusic: Music? = null
    private var uiMusic: Music? = null
    private var gameTrackSwapTimerSeconds = 0f
    private val tutorialSteps = listOf(
        TutorialStep(
            titleEn = "Start The Run",
            titleTr = "Koşuyu Başlat",
            detailEn = "Tap the ready card to launch the level.",
            detailTr = "Bölümü başlatmak için hazır kartına dokun.",
            anchor = TutorialAnchor.READY_CARD
        ),
        TutorialStep(
            titleEn = "Move Left",
            titleTr = "Sola Hareket Et",
            detailEn = "Tap or hold the left control area.",
            detailTr = "Sol kontrol alanına dokun veya basılı tut.",
            anchor = TutorialAnchor.LEFT_CONTROL
        ),
        TutorialStep(
            titleEn = "Move Right",
            titleTr = "Sağa Hareket Et",
            detailEn = "Tap or hold the right control area.",
            detailTr = "Sağ kontrol alanına dokun veya basılı tut.",
            anchor = TutorialAnchor.RIGHT_CONTROL
        ),
        TutorialStep(
            titleEn = "Read The Arena",
            titleTr = "Alanı Oku",
            detailEn = "Stay in open gaps and avoid red rings.",
            detailTr = "Açık boşlukta kal ve kırmızı halkalardan kaç.",
            anchor = TutorialAnchor.ARENA
        ),
        TutorialStep(
            titleEn = "Use Recovery Help",
            titleTr = "Yardımı Kullan",
            detailEn = "After failures, use support tools for life/shield and watch the support panel.",
            detailTr = "Kaybedince can/kalkan yardımı için destek araçlarını kullan ve destek panelini takip et.",
            anchor = TutorialAnchor.SUPPORT_PANEL
        )
    )
    private val flowTimings = FlowTimingModel(
        countdownSeconds = 1.8f,
        readyCardInSeconds = 0.2f,
        resultCardInSeconds = 0.18f,
        drainCueSeconds = 0.12f
    )

    init {
        migrateLegacyProfileIfNeeded()
        migrateLegacyPremiumToggle()
        settingsState = settingsManager.snapshot()
        applyLivesExpansionMigration()
        applyShieldStockMigration()
        loadShipStore()
        syncSelectedShipHitbox()
        loadTutorialTouchIcon()
        loadUiStatusIcons()
        loadBlockBriefingState()
        loadAudioFx()
        tutorialActive = !profilePreferences.getBoolean(STORE_TUTORIAL_DONE_KEY, false)
        tutorialPaused = tutorialActive
        dependencies.simulationStartLevel?.let { requestedLevel ->
            selectedLevelIndex = requestedLevel.coerceIn(0, levels.lastIndex)
        }
        simulation.setLevel(selectedLevelIndex)
        applyDifficultySettings()
        visualPlayerAngleRad = simulation.playerAngleRad
        lastPlayerAngleSampleRad = simulation.playerAngleRad
        angleSamplerReady = true
        updateBlockBriefingForCurrentLevel()
        refreshPremiumStoreStatus(showLoading = false)
        if (simulationControlEnabled) {
            overlayMode = OverlayMode.GAME
            openLevelReady()
        }
        syncBannerVisibility(force = true)
    }

    private fun storeDisplayName(): String {
        return when (dependencies.commercePlatform) {
            CommercePlatform.GOOGLE_PLAY -> "Google Play"
            CommercePlatform.APP_STORE -> "App Store"
            CommercePlatform.GENERIC -> "the app store"
        }
    }

    private fun storeDisplayNameTr(): String {
        return when (dependencies.commercePlatform) {
            CommercePlatform.GOOGLE_PLAY -> "Google Play"
            CommercePlatform.APP_STORE -> "App Store"
            CommercePlatform.GENERIC -> "uygulama mağazası"
        }
    }

    private fun adsEnabled(): Boolean = dependencies.adsEnabled

    private fun applyLivesExpansionMigration() {
        if (profilePreferences.getBoolean(STORE_LIVES_EXPANDED_KEY, false)) {
            return
        }
        val now = nowEpochSeconds()
        livesManager.grantLife(now, amount = MAX_LIVES)
        livesState = livesManager.snapshot(now)
        profilePreferences.putBoolean(STORE_LIVES_EXPANDED_KEY, true).flush()
    }

    private fun applyShieldStockMigration() {
        if (!profilePreferences.contains(STORE_SHIELD_COUNT_KEY)) {
            shieldCount = MAX_SHIELDS
            saveShieldCount()
        } else {
            shieldCount = shieldCount.coerceIn(0, MAX_SHIELDS)
        }
        if (!profilePreferences.getBoolean(STORE_SLOW_STOCK_SEEDED_KEY, false)) {
            slowPowerCount = MAX_SLOW_POWERS
            profilePreferences.putBoolean(STORE_SLOW_STOCK_SEEDED_KEY, true).flush()
        } else if (!profilePreferences.contains(STORE_SLOW_POWER_COUNT_KEY)) {
            slowPowerCount = MAX_SLOW_POWERS
        } else {
            slowPowerCount = slowPowerCount.coerceIn(0, MAX_SLOW_POWERS)
        }
        saveSlowPowerCount()
    }

    private fun migrateLegacyProfileIfNeeded() {
        if (profilePreferences.getBoolean(PREFS_MIGRATION_DONE_KEY, false)) {
            return
        }
        if (profilePreferences.get().isNotEmpty()) {
            profilePreferences.putBoolean(PREFS_MIGRATION_DONE_KEY, true).flush()
            return
        }
        val legacyValues = legacyProfilePreferences.get()
        if (legacyValues.isEmpty()) {
            profilePreferences.putBoolean(PREFS_MIGRATION_DONE_KEY, true).flush()
            return
        }
        for ((key, value) in legacyValues) {
            when (value) {
                is Boolean -> profilePreferences.putBoolean(key, value)
                is Int -> profilePreferences.putInteger(key, value)
                is Long -> profilePreferences.putLong(key, value)
                is Float -> profilePreferences.putFloat(key, value)
                is String -> profilePreferences.putString(key, value)
            }
        }
        profilePreferences.putBoolean(PREFS_MIGRATION_DONE_KEY, true).flush()
    }

    private fun migrateLegacyPremiumToggle() {
        if (profilePreferences.contains("premium_enabled")) {
            profilePreferences.remove("premium_enabled")
            profilePreferences.flush()
        }
    }

    override fun render(delta: Float) {
        refreshLivesState()
        syncBannerVisibility()
        processMetaInput()
        syncAudioPlayback()

        val frameDelta = delta.coerceIn(0f, 0.16f)
        val blend = (frameDelta * 12f).coerceIn(0f, 1f)
        smoothedFrameDelta += (frameDelta - smoothedFrameDelta) * blend
        val fluidDelta = smoothedFrameDelta.coerceIn(1f / 180f, 0.16f)
        updatePerformanceProfile(frameDelta)
        worldTime += fluidDelta
        simulationStepCooldownSeconds = (simulationStepCooldownSeconds - fluidDelta).coerceAtLeast(0f)
        updateOverlayFlow(fluidDelta)
        updatePolicyScroll(fluidDelta)
        updateLevelSelectScroll(fluidDelta)
        updateReadyCountdown(fluidDelta)
        updateTutorialFlow(fluidDelta)
        updateShopNotice(fluidDelta)
        if (
            overlayMode == OverlayMode.GAME &&
            !tutorialPaused &&
            (simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING)
        ) {
            val rawInput = readPlayerInput()
            var snapStep = rawInput.stepDirection
            accumulator += fluidDelta
            var simulationSteps = 0
            while (
                accumulator >= GameSimulation.FIXED_TIMESTEP_SECONDS &&
                simulationSteps < MAX_SIMULATION_STEPS_PER_FRAME
            ) {
                var resolvedHold = rawInput.holdDirection
                if (!simulation.usesStepMovement && resolvedHold == 0 && freeTurnImpulseRemaining > 0f) {
                    resolvedHold = freeTurnImpulseDirection
                }
                simulation.step(
                    GameSimulation.FIXED_TIMESTEP_SECONDS,
                    GameSimulation.PlayerInput(
                        holdDirection = resolvedHold,
                        stepDirection = snapStep
                    )
                )
                freeTurnImpulseRemaining = (
                    freeTurnImpulseRemaining - GameSimulation.FIXED_TIMESTEP_SECONDS
                    ).coerceAtLeast(0f)
                if (freeTurnImpulseRemaining == 0f) {
                    freeTurnImpulseDirection = 0
                }
                snapStep = 0
                accumulator -= GameSimulation.FIXED_TIMESTEP_SECONDS
                simulationSteps += 1
            }
            if (simulationSteps == MAX_SIMULATION_STEPS_PER_FRAME) {
                accumulator = accumulator.coerceAtMost(GameSimulation.FIXED_TIMESTEP_SECONDS * 0.5f)
            }
        }

        handleRunResultTransitions()
        updatePresentationEffects(fluidDelta)
        drawFrame()

        // Sim-mode-only diagnostic. Release builds never enable simulationControlEnabled,
        // so this is silent in production; it lets the CI simulator repro confirm that
        // gameplay actually reaches RUNNING and renders obstacles.
        if (simulationControlEnabled) {
            simDiagFrames += 1
            if (simDiagFrames % 120 == 0) {
                Gdx.app.log(
                    "SimRepro",
                    "frame=$simDiagFrames overlay=$overlayMode phase=${simulation.runPhase} " +
                        "obstacles=${simulation.obstacles.size} missiles=${simulation.missiles.size}"
                )
            }
        }
    }

    private fun updatePerformanceProfile(frameDelta: Float) {
        if (frameDelta <= 0f) {
            return
        }
        performanceSampleSeconds += frameDelta
        performanceSampleFrames += 1
        if (performanceSampleSeconds < 1.2f) {
            return
        }
        val fps = performanceSampleFrames / performanceSampleSeconds
        smoothedFps += (fps - smoothedFps) * 0.34f
        if (Gdx.app.type == Application.ApplicationType.Android) {
            if (!lowPerformanceMode && smoothedFps < 52f) {
                lowPerformanceMode = true
            } else if (lowPerformanceMode && smoothedFps > 57f) {
                lowPerformanceMode = false
            }
        }
        performanceSampleSeconds = 0f
        performanceSampleFrames = 0
    }

    private fun updateOverlayFlow(delta: Float) {
        // The PROCESSING dialog blocks all touch input; without this watchdog a missing
        // StoreKit callback (network stall, deferred approval) locks the app permanently.
        if (premiumDialogType == PremiumDialogType.PROCESSING) {
            premiumProcessingSeconds += delta
            if (premiumProcessingSeconds >= 45f) {
                showPremiumDialog(
                    type = PremiumDialogType.FAILURE,
                    title = t("STORE NOT RESPONDING", "MAĞAZA YANIT VERMİYOR"),
                    body = t(
                        "The App Store has not responded yet. If the purchase completes later, premium unlocks automatically.",
                        "App Store henüz yanıt vermedi. Satın alma daha sonra tamamlanırsa premium otomatik olarak açılır."
                    ),
                    primaryLabel = t("TRY AGAIN", "TEKRAR DENE"),
                    secondaryLabel = t("CLOSE", "KAPAT"),
                    tone = ChipTone.ALERT,
                    retryOnPrimary = true
                )
            }
        } else {
            premiumProcessingSeconds = 0f
        }

        when (overlayMode) {
            OverlayMode.SPLASH -> {
                splashRemainingSeconds = (splashRemainingSeconds - delta).coerceAtLeast(0f)
                if (splashRemainingSeconds <= 0f) {
                    overlayMode = OverlayMode.EPILEPSY_WARNING
                }
            }

            OverlayMode.TRANSITION -> {
                transitionRemainingSeconds = (transitionRemainingSeconds - delta).coerceAtLeast(0f)
                if (transitionRemainingSeconds <= 0f) {
                    overlayMode = OverlayMode.GAME
                    openLevelReady()
                }
            }

            else -> Unit
        }
    }

    private fun updateShopNotice(delta: Float) {
        if (shopNoticeTimer <= 0f) {
            return
        }
        shopNoticeTimer = (shopNoticeTimer - delta).coerceAtLeast(0f)
        if (shopNoticeTimer <= 0f) {
            shopNoticeMessage = ""
        }
    }

    private fun updatePolicyScroll(delta: Float) {
        if (overlayMode != OverlayMode.POLICY) {
            policyDragPointer = -1
            lastPolicyDragY = 0f
            return
        }

        val step = 720f * delta
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            scrollPolicyBy(step)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            scrollPolicyBy(-step)
        }

        val textRect = policyTextRect()
        if (policyDragPointer >= 0 && !Gdx.input.isTouched(policyDragPointer)) {
            policyDragPointer = -1
            lastPolicyDragY = 0f
        }

        for (pointer in 0..4) {
            if (!Gdx.input.isTouched(pointer)) {
                continue
            }

            tempTouch.set(Gdx.input.getX(pointer).toFloat(), Gdx.input.getY(pointer).toFloat())
            viewport.unproject(tempTouch)
            val insideTextRect = contains(textRect, tempTouch.x, tempTouch.y)

            if (policyDragPointer == pointer) {
                val deltaY = tempTouch.y - lastPolicyDragY
                if (deltaY != 0f) {
                    scrollPolicyBy(deltaY)
                }
                lastPolicyDragY = tempTouch.y
                return
            }

            if (insideTextRect && policyDragPointer == -1) {
                policyDragPointer = pointer
                lastPolicyDragY = tempTouch.y
                return
            }
        }
    }

    private fun resetPolicyScroll() {
        policyScrollOffset = 0f
        policyDragPointer = -1
        lastPolicyDragY = 0f
    }

    private fun scrollPolicyBy(delta: Float) {
        policyScrollOffset = (policyScrollOffset + delta).coerceIn(0f, maxPolicyScrollOffset())
    }

    private fun updateLevelSelectScroll(delta: Float) {
        if (overlayMode != OverlayMode.LEVEL_SELECT) {
            levelSelectDragPointer = -1
            lastLevelSelectDragY = 0f
            levelSelectDragStartY = 0f
            levelSelectDragActive = false
            levelSelectScrollbarDrag = false
            levelSelectTapCandidateLevel = -1
            levelSelectScrollOffset = 0f
            return
        }

        val step = 760f * delta
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            scrollLevelSelectBy(step)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            scrollLevelSelectBy(-step)
        }

        val scrollRect = levelSelectScrollTouchRect()
        val scrollbarTrack = levelSelectScrollbarTrackRect()
        if (levelSelectDragPointer >= 0 && !Gdx.input.isTouched(levelSelectDragPointer)) {
            if (!levelSelectDragActive && !levelSelectScrollbarDrag && levelSelectTapCandidateLevel >= 0) {
                applyLevelSelection(levelSelectTapCandidateLevel)
            }
            levelSelectDragPointer = -1
            lastLevelSelectDragY = 0f
            levelSelectDragStartY = 0f
            levelSelectDragActive = false
            levelSelectScrollbarDrag = false
            levelSelectTapCandidateLevel = -1
        }

        for (pointer in 0..4) {
            if (!Gdx.input.isTouched(pointer)) {
                continue
            }

            tempTouch.set(Gdx.input.getX(pointer).toFloat(), Gdx.input.getY(pointer).toFloat())
            viewport.unproject(tempTouch)
            val insidePanel = contains(scrollRect, tempTouch.x, tempTouch.y)
            val insideScrollbar = contains(scrollbarTrack, tempTouch.x, tempTouch.y)

            if (levelSelectDragPointer == pointer) {
                if (levelSelectScrollbarDrag) {
                    setLevelSelectScrollFromTrackPosition(tempTouch.y, scrollbarTrack)
                    levelSelectScrollHintDismissed = true
                    lastLevelSelectDragY = tempTouch.y
                    return
                }
                val deltaFromStart = abs(tempTouch.y - levelSelectDragStartY)
                if (!levelSelectDragActive) {
                    val dragThreshold = sy(14f).coerceIn(10f, 22f)
                    if (deltaFromStart >= dragThreshold) {
                        levelSelectDragActive = true
                        levelSelectScrollHintDismissed = true
                        levelSelectTapCandidateLevel = -1
                    }
                }
                if (levelSelectDragActive) {
                    val deltaY = tempTouch.y - lastLevelSelectDragY
                    if (deltaY != 0f) {
                        scrollLevelSelectBy(deltaY)
                    }
                }
                lastLevelSelectDragY = tempTouch.y
                return
            }

            if ((insidePanel || insideScrollbar) && levelSelectDragPointer == -1) {
                levelSelectDragPointer = pointer
                lastLevelSelectDragY = tempTouch.y
                levelSelectDragStartY = tempTouch.y
                levelSelectDragActive = insideScrollbar
                levelSelectScrollbarDrag = insideScrollbar
                levelSelectTapCandidateLevel = if (insideScrollbar) {
                    -1
                } else {
                    detectGroupedLevelFromTouch(tempTouch.x, tempTouch.y) ?: -1
                }
                if (insideScrollbar) {
                    setLevelSelectScrollFromTrackPosition(tempTouch.y, scrollbarTrack)
                    levelSelectScrollHintDismissed = true
                }
                return
            }
        }
    }

    private fun scrollLevelSelectBy(delta: Float) {
        levelSelectScrollOffset = (levelSelectScrollOffset + delta).coerceIn(0f, maxLevelSelectScrollOffset())
        if (kotlin.math.abs(levelSelectScrollOffset) > sy(6f).coerceIn(3f, 12f)) {
            levelSelectScrollHintDismissed = true
        }
    }

    private fun processMetaInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) {
            simulationControlEnabled = !simulationControlEnabled
            simulationResultDelaySeconds = 0f
            simulationStepCooldownSeconds = 0f
            freeTurnImpulseDirection = 0
            freeTurnImpulseRemaining = 0f
            statusMessage = if (simulationControlEnabled) {
                t("SIMULATION ON", "SİMÜLASYON AÇIK")
            } else {
                t("MANUAL MODE", "ELLE KONTROL")
            }
            if (simulationControlEnabled && overlayMode != OverlayMode.GAME && overlayMode != OverlayMode.TRANSITION) {
                startSelectedLevelRun()
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            when (overlayMode) {
                OverlayMode.SPLASH -> overlayMode = OverlayMode.EPILEPSY_WARNING
                OverlayMode.EPILEPSY_WARNING -> overlayMode = OverlayMode.INTRO
                OverlayMode.LEVEL_SELECT -> overlayMode = OverlayMode.INTRO
                OverlayMode.GAME -> openPauseMenu()
                OverlayMode.PAUSE -> closePauseMenu()
                OverlayMode.MENU -> {
                    overlayMode = if (menuReturnToPause) OverlayMode.PAUSE else OverlayMode.INTRO
                    menuReturnToPause = false
                }
                OverlayMode.SHOP -> overlayMode = OverlayMode.INTRO
                OverlayMode.PREMIUM -> closePremiumPage()
                OverlayMode.POLICY -> overlayMode = OverlayMode.MENU
                OverlayMode.TRANSITION -> Unit
                OverlayMode.INTRO -> Unit
            }
            return
        }

        when (overlayMode) {
            OverlayMode.SPLASH -> processSplashInput()
            OverlayMode.EPILEPSY_WARNING -> processEpilepsyWarningInput()
            OverlayMode.INTRO -> processIntroInput()
            OverlayMode.MENU -> processMenuInput()
            OverlayMode.SHOP -> processShopInput()
            OverlayMode.PREMIUM -> processPremiumInput()
            OverlayMode.POLICY -> processPolicyInput()
            OverlayMode.LEVEL_SELECT -> processLevelSelectInput()
            OverlayMode.TRANSITION -> Unit
            OverlayMode.GAME -> processGameInput()
            OverlayMode.PAUSE -> processPauseInput()
        }
    }

    private fun processSplashInput() {
        if (
            Gdx.input.justTouched() ||
            Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
        ) {
            splashRemainingSeconds = 0f
            overlayMode = OverlayMode.EPILEPSY_WARNING
        }
    }

    private fun processEpilepsyWarningInput() {
        if (
            Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
        ) {
            overlayMode = OverlayMode.INTRO
            return
        }
        if (!readJustTouchedWorld()) {
            return
        }
        if (contains(epilepsyContinueRect(), tempTouch.x, tempTouch.y)) {
            overlayMode = OverlayMode.INTRO
        }
    }

    private fun processIntroInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            openLevelSelect()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            overlayMode = OverlayMode.MENU
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            overlayMode = OverlayMode.MENU
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            openPremiumPage(OverlayMode.INTRO)
            return
        }

        if (!readJustTouchedWorld()) {
            return
        }

        when {
            contains(introPlayButtonRect(), tempTouch.x, tempTouch.y) -> openLevelSelect()
            contains(introSettingsButtonRect(), tempTouch.x, tempTouch.y) -> overlayMode = OverlayMode.MENU
            contains(introShopButtonRect(), tempTouch.x, tempTouch.y) -> openShop()
            contains(introPremiumButtonRect(), tempTouch.x, tempTouch.y) -> openPremiumPage(OverlayMode.INTRO)
            adsEnabled() && contains(introLifeRewardButtonRect(), tempTouch.x, tempTouch.y) -> requestExtraLifeFromRewardedAd()
            adsEnabled() && contains(introShieldRewardButtonRect(), tempTouch.x, tempTouch.y) -> requestShieldFromRewardedAd()
            adsEnabled() && contains(introSlowRewardButtonRect(), tempTouch.x, tempTouch.y) -> requestSlowFromRewardedAd()
        }
    }

    private fun processMenuInput() {
        if (updateMenuVolumeSliderDrag()) {
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            if (menuReturnToPause) {
                statusMessage = t("Level select is disabled while paused", "Duraklamada seviye seçimi kapalı")
                return
            }
            openLevelSelect()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            if (menuReturnToPause) {
                statusMessage = t("Legal page is unavailable while paused", "Duraklamada yasal sayfa kapalı")
                return
            }
            selectedPolicyPage = PolicyPage.PRIVACY
            overlayMode = OverlayMode.POLICY
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            settingsState = settingsManager.toggleLanguage()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            settingsState = settingsManager.cycleDifficulty()
            applyDifficultySettings()
            return
        }

        if (!readJustTouchedWorld()) {
            return
        }

        when {
            contains(menuSoundRect(), tempTouch.x, tempTouch.y) -> {
                settingsState = settingsManager.toggleSound()
                if (settingsState.soundEnabled) {
                    playUiSound(uiConfirmSound, 0.72f, 1.02f)
                }
            }
            contains(menuHapticsRect(), tempTouch.x, tempTouch.y) -> settingsState = settingsManager.toggleHaptics()
            contains(menuLanguageRect(), tempTouch.x, tempTouch.y) -> settingsState = settingsManager.toggleLanguage()
            contains(menuDifficultyRect(), tempTouch.x, tempTouch.y) -> {
                settingsState = settingsManager.cycleDifficulty()
                applyDifficultySettings()
            }
            contains(menuLevelsRect(), tempTouch.x, tempTouch.y) -> {
                if (menuReturnToPause) {
                    statusMessage = t("Level select is disabled while paused", "Duraklamada seviye seçimi kapalı")
                } else {
                    openLevelSelect()
                }
            }
            contains(menuPremiumRect(), tempTouch.x, tempTouch.y) -> openPremiumPage(OverlayMode.MENU)
            contains(menuPolicyRect(), tempTouch.x, tempTouch.y) -> {
                if (menuReturnToPause) {
                    statusMessage = t("Legal page is unavailable while paused", "Duraklamada yasal sayfa kapalı")
                } else {
                    openPolicyPage(PolicyPage.PRIVACY)
                }
            }
            contains(menuBackRect(), tempTouch.x, tempTouch.y) -> {
                overlayMode = if (menuReturnToPause) OverlayMode.PAUSE else OverlayMode.INTRO
                menuReturnToPause = false
            }
        }
    }

    private fun processShopInput() {
        if (!readJustTouchedWorld()) {
            return
        }

        if (contains(shopTabShipsRect(), tempTouch.x, tempTouch.y)) {
            selectedShopCategory = ShopCategory.SHIPS
            return
        }
        if (contains(shopTabShieldsRect(), tempTouch.x, tempTouch.y)) {
            selectedShopCategory = ShopCategory.SHIELDS
            return
        }

        if (contains(shopBackRect(), tempTouch.x, tempTouch.y)) {
            overlayMode = OverlayMode.INTRO
            return
        }

        if (adsEnabled() && selectedShopCategory == ShopCategory.SHIELDS && contains(shopRewardRect(), tempTouch.x, tempTouch.y)) {
            requestShieldFromRewardedAd()
            return
        }
        if (adsEnabled() && selectedShopCategory == ShopCategory.SHIELDS && contains(shopSlowRewardRect(), tempTouch.x, tempTouch.y)) {
            requestSlowFromRewardedAd()
            return
        }
        if (selectedShopCategory == ShopCategory.SHIELDS && contains(shopSlowActionRect(), tempTouch.x, tempTouch.y)) {
            performShopSupportAction(SupportStoreKind.SLOW)
            return
        }
        if (contains(shopActionRect(), tempTouch.x, tempTouch.y)) {
            if (selectedShopCategory == ShopCategory.SHIELDS) {
                performShopSupportAction(SupportStoreKind.SHIELD)
                return
            }
            performShopPrimaryAction()
            return
        }

        when (selectedShopCategory) {
            ShopCategory.SHIPS -> {
                val selected = detectShopShipFromTouch(tempTouch.x, tempTouch.y)
                if (selected != null) {
                    selectedShopShipIndex = selected
                }
            }

            ShopCategory.SHIELDS -> {
                val selected = detectShopShieldFromTouch(tempTouch.x, tempTouch.y)
                if (selected != null) {
                    selectedShopShieldIndex = selected
                }
            }
        }
    }

    private fun processPremiumInput() {
        if (premiumDialogType != PremiumDialogType.NONE) {
            if (premiumDialogType == PremiumDialogType.PROCESSING) {
                return
            }
            if (!readJustTouchedWorld()) {
                return
            }
            when {
                contains(premiumDialogPrimaryRect(), tempTouch.x, tempTouch.y) -> {
                    when (premiumDialogType) {
                        PremiumDialogType.SUCCESS -> dismissPremiumDialog(closePage = premiumEnabled)
                        PremiumDialogType.FAILURE -> {
                            val retry = premiumDialogRetryOnPrimary
                            dismissPremiumDialog(closePage = false)
                            if (retry) {
                                triggerPremiumPurchase()
                            }
                        }
                        else -> dismissPremiumDialog(closePage = false)
                    }
                }
                premiumDialogSecondaryLabel.isNotBlank() &&
                    contains(premiumDialogSecondaryRect(), tempTouch.x, tempTouch.y) -> {
                        dismissPremiumDialog(closePage = false)
                    }
            }
            return
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            triggerPremiumPurchase()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            refreshPremiumStoreStatus()
            return
        }

        if (!readJustTouchedWorld()) {
            return
        }

        when {
            contains(premiumPurchaseButtonRect(), tempTouch.x, tempTouch.y) -> triggerPremiumPurchase()
            contains(premiumRefreshButtonRect(), tempTouch.x, tempTouch.y) -> refreshPremiumStoreStatus(restore = true)
            contains(premiumBackButtonRect(), tempTouch.x, tempTouch.y) -> closePremiumPage()
        }
    }

    private fun processPolicyInput() {
        if (!readJustTouchedWorld()) {
            return
        }

        when {
            contains(policyTabLeftRect(), tempTouch.x, tempTouch.y) -> openPolicyPage(PolicyPage.PRIVACY)
            contains(policyTabCenterRect(), tempTouch.x, tempTouch.y) -> openPolicyPage(PolicyPage.TERMS)
            contains(policyTabRightRect(), tempTouch.x, tempTouch.y) -> openPolicyPage(PolicyPage.LICENSE)
            contains(policyBackRect(), tempTouch.x, tempTouch.y) -> {
                resetPolicyScroll()
                overlayMode = OverlayMode.MENU
            }
        }
    }

    private fun processLevelSelectInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            startSelectedLevelRun()
            return
        }

        if (!readJustTouchedWorld()) {
            return
        }

        if (contains(levelSelectBackRect(), tempTouch.x, tempTouch.y)) {
            overlayMode = OverlayMode.INTRO
            return
        }

        if (contains(levelSelectStartRect(), tempTouch.x, tempTouch.y)) {
            startSelectedLevelRun()
            return
        }

    }

    private fun applyLevelSelection(levelIndex: Int) {
        val clampedIndex = levelIndex.coerceIn(0, levels.lastIndex)
        if (!isLevelUnlocked(clampedIndex)) {
            statusMessage = t("LOCKED: CLEAR LEVEL ${maxUnlockedLevelIndex() + 1}", "KİLİTLİ: ${maxUnlockedLevelIndex() + 1}. SEVİYEYİ GEÇ")
            playUiSound(uiConfirmSound, 0.24f, 0.76f)
            return
        }
        selectedLevelIndex = clampedIndex
        simulation.setLevel(selectedLevelIndex)
        updateBlockBriefingForCurrentLevel()
        menuHint = "LEVEL ${selectedLevelIndex + 1} SELECTED"
        playUiSound(uiConfirmSound, 0.34f, 1.08f)
    }

    private fun openLevelSelect() {
        selectedLevelIndex = selectedLevelIndex.coerceIn(0, maxUnlockedLevelIndex())
        simulation.setLevel(selectedLevelIndex)
        updateBlockBriefingForCurrentLevel()
        menuReturnToPause = false
        overlayMode = OverlayMode.LEVEL_SELECT
        levelSelectScrollOffset = 0f
        levelSelectDragPointer = -1
        lastLevelSelectDragY = 0f
        levelSelectDragStartY = 0f
        levelSelectDragActive = false
        levelSelectScrollbarDrag = false
        levelSelectTapCandidateLevel = -1
        levelSelectScrollHintDismissed = false
        menuHint = "LEVEL ${selectedLevelIndex + 1} READY"
        cancelReadyCountdown()
        lastObservedRunPhase = simulation.runPhase
    }

    private fun openShop() {
        loadShipStore()
        selectedShopCategory = ShopCategory.SHIPS
        selectedShopShipIndex = shipSkins.indexOfFirst { it.id == selectedShipId }.coerceAtLeast(0)
        selectedShopShieldIndex = selectedShopShieldIndex.coerceIn(0, shieldStoreItems.lastIndex)
        overlayMode = OverlayMode.SHOP
    }

    private fun openPremiumPage(returnOverlay: OverlayMode) {
        premiumReturnOverlay = returnOverlay
        premiumDialogType = PremiumDialogType.NONE
        premiumDialogTitle = ""
        premiumDialogBody = ""
        premiumDialogPrimaryLabel = ""
        premiumDialogSecondaryLabel = ""
        premiumDialogRetryOnPrimary = false
        overlayMode = OverlayMode.PREMIUM
        refreshPremiumStoreStatus()
    }

    private fun closePremiumPage() {
        premiumDialogType = PremiumDialogType.NONE
        premiumDialogTitle = ""
        premiumDialogBody = ""
        premiumDialogPrimaryLabel = ""
        premiumDialogSecondaryLabel = ""
        premiumDialogRetryOnPrimary = false
        overlayMode = when (premiumReturnOverlay) {
            OverlayMode.MENU -> OverlayMode.MENU
            else -> OverlayMode.INTRO
        }
    }

    private fun beginReadyCountdown() {
        if (readyCountdownActive || simulation.runPhase != RunPhase.READY || overlayMode != OverlayMode.GAME) {
            return
        }
        readyCountdownActive = true
        readyCountdownSeconds = flowTimings.countdownSeconds
        statusMessage = t("LOCK IN", "HAZIR OL")
        playUiSound(uiStartSound, 0.62f)
    }

    private fun cancelReadyCountdown() {
        readyCountdownActive = false
        readyCountdownSeconds = 0f
    }

    private fun updateReadyCountdown(delta: Float) {
        if (!readyCountdownActive) {
            return
        }
        if (overlayMode != OverlayMode.GAME || simulation.runPhase != RunPhase.READY) {
            cancelReadyCountdown()
            return
        }
        readyCountdownSeconds = (readyCountdownSeconds - delta).coerceAtLeast(0f)
        if (readyCountdownSeconds <= 0f) {
            cancelReadyCountdown()
            startAttempt()
        }
    }

    private fun readyCountdownStep(): Int {
        if (!readyCountdownActive) {
            return 0
        }
        val segment = (flowTimings.countdownSeconds / 3f).coerceAtLeast(0.01f)
        return ceil(readyCountdownSeconds / segment).toInt().coerceIn(1, 3)
    }

    private fun processGameInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            openPauseMenu()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            returnToMenu()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            openLevelSelect()
            return
        }
        if (tutorialActive && Gdx.input.justTouched()) {
            tempTouch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            viewport.unproject(tempTouch)
            if (contains(tutorialPassRect(), tempTouch.x, tempTouch.y)) {
                onTutorialContinuePressed()
                return
            }
            val step = tutorialSteps.getOrNull(tutorialStepIndex)
            if (step != null && contains(tutorialFocusRect(step.anchor), tempTouch.x, tempTouch.y)) {
                onTutorialContinuePressed()
                return
            }
        }
        if (tutorialActive && tutorialPaused) {
            return
        }

        if (isButtonPressed(pauseButtonRect())) {
            openPauseMenu()
            return
        }

        if (simulationControlEnabled) {
            val autoDelta = Gdx.graphics.deltaTime.coerceIn(0f, 0.16f)
            when (simulation.runPhase) {
                RunPhase.READY -> {
                    beginReadyCountdown()
                    return
                }

                RunPhase.RUNNING,
                RunPhase.DRAINING -> {
                    simulationResultDelaySeconds = 0f
                    return
                }

                RunPhase.GAME_OVER -> {
                    simulationResultDelaySeconds += autoDelta
                    if (simulationResultDelaySeconds >= 0.7f) {
                        simulationResultDelaySeconds = 0f
                        startAttempt()
                    }
                    return
                }

                RunPhase.LEVEL_CLEARED -> {
                    simulationResultDelaySeconds += autoDelta
                    if (simulationResultDelaySeconds >= 0.82f) {
                        simulationResultDelaySeconds = 0f
                        advanceLevelOrLoopSimulation()
                    }
                    return
                }
            }
        }

        when (simulation.runPhase) {
            RunPhase.READY -> {
                if (isLifeLocked()) {
                    val activate = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    if (activate) {
                        requestExtraLifeFromRewardedAd()
                        return
                    }
                    if (!readJustTouchedWorld()) {
                        return
                    }
                    if (contains(readyPromptRect(), tempTouch.x, tempTouch.y)) {
                        requestExtraLifeFromRewardedAd()
                    }
                    return
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
                    requestShieldFromRewardedAd()
                    return
                }
                if (readJustTouchedWorld()) {
                    if (blockBriefingVisible) {
                        val done = advanceBlockBriefingStep()
                        if (!done) {
                            return
                        }
                    }
                    beginReadyCountdown()
                    return
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    if (blockBriefingVisible) {
                        val done = advanceBlockBriefingStep()
                        if (!done) {
                            return
                        }
                    }
                    beginReadyCountdown()
                }
            }

            RunPhase.RUNNING,
            RunPhase.DRAINING -> {
                val supportButtonsVisible = areSupportActionButtonsVisible()
                if (Gdx.input.justTouched()) {
                    tempTouch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                    viewport.unproject(tempTouch)
                    if (supportButtonsVisible && contains(touchSlowButtonRect(), tempTouch.x, tempTouch.y)) {
                        handleSlowPowerButton()
                    } else if (supportButtonsVisible && contains(touchShieldButtonRect(), tempTouch.x, tempTouch.y)) {
                        handleShieldActionButton()
                    }
                }
            }

            RunPhase.GAME_OVER -> {
                if (adActionInProgress) {
                    return
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    startAttempt()
                    return
                }
                if (!readJustTouchedWorld()) {
                    return
                }
                when {
                    contains(primaryResultButtonRect(), tempTouch.x, tempTouch.y) -> {
                        startAttempt()
                    }
                    contains(secondaryResultButtonRect(), tempTouch.x, tempTouch.y) -> {
                        if (canUseShieldAd()) {
                            requestShieldFromRewardedAd()
                        } else if (canUseExtraLifeAd()) {
                            requestExtraLifeFromRewardedAd()
                        } else {
                            returnToMenu()
                        }
                    }
                }
            }

            RunPhase.LEVEL_CLEARED -> {
                if (adActionInProgress) {
                    return
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.N) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    advanceLevelOrReturnToMenu()
                    return
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.R) && canUseCoinDoubleAd()) {
                    requestCoinDoubleFromRewardedAd()
                    return
                }
                if (!readJustTouchedWorld()) {
                    return
                }
                when {
                    contains(primaryResultButtonRect(), tempTouch.x, tempTouch.y) -> advanceLevelOrReturnToMenu()
                    contains(resultOfferButtonRect(), tempTouch.x, tempTouch.y) -> {
                        if (levelClearDoubleClaimed) {
                            statusMessage = t("2X already claimed", "2x zaten alındı")
                        } else if (canUseCoinDoubleAd()) {
                            requestCoinDoubleFromRewardedAd()
                        } else {
                            statusMessage = t("Rewarded ad unavailable", "Ödüllü reklam hazır değil")
                        }
                    }
                    contains(secondaryResultButtonRect(), tempTouch.x, tempTouch.y) -> {
                        returnToMenu()
                    }
                }
            }
        }
    }

    private fun processPauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closePauseMenu()
            return
        }
        if (!readJustTouchedWorld()) {
            return
        }
        when {
            contains(pauseResumeRect(), tempTouch.x, tempTouch.y) -> closePauseMenu()
            contains(pauseRestartRect(), tempTouch.x, tempTouch.y) -> {
                closePauseMenu()
                startAttempt()
            }
            contains(pauseMenuRect(), tempTouch.x, tempTouch.y) -> returnToMenu()
            contains(pauseSettingsRect(), tempTouch.x, tempTouch.y) -> {
                menuReturnToPause = true
                overlayMode = OverlayMode.MENU
            }
        }
    }

    private fun openPauseMenu() {
        if (overlayMode != OverlayMode.GAME) {
            return
        }
        cancelReadyCountdown()
        menuReturnToPause = false
        overlayMode = OverlayMode.PAUSE
    }

    private fun closePauseMenu() {
        if (overlayMode != OverlayMode.PAUSE) {
            return
        }
        overlayMode = OverlayMode.GAME
    }

    private fun startSelectedLevelRun() {
        if (!isLevelUnlocked(selectedLevelIndex)) {
            selectedLevelIndex = maxUnlockedLevelIndex()
            simulation.setLevel(selectedLevelIndex)
            statusMessage = t("LOCKED: CLEAR PREVIOUS LEVEL", "KİLİTLİ: ÖNCEKİ BÖLÜMÜ GEÇ")
            menuHint = t("LOCKED", "KİLİTLİ")
            playUiSound(uiConfirmSound, 0.28f, 0.78f)
            return
        }
        simulation.setLevel(selectedLevelIndex)
        updateBlockBriefingForCurrentLevel()
        overlayMode = OverlayMode.GAME
        simulationResultDelaySeconds = 0f
        simulationStepCooldownSeconds = 0f
        openLevelReady()
    }

    private fun advanceLevelOrReturnToMenu() {
        if (showInterstitialIfNeeded { advanceLevelOrReturnToMenu() }) {
            return
        }
        val advanced = simulation.advanceLevel()
        if (!advanced) {
            returnToMenu()
            menuHint = "CAMPAIGN COMPLETE"
            return
        }

        selectedLevelIndex = simulation.levelIndex
        beginLevelTransition()
    }

    private fun advanceLevelOrLoopSimulation() {
        if (showInterstitialIfNeeded { advanceLevelOrLoopSimulation() }) {
            return
        }
        val advanced = simulation.advanceLevel()
        if (advanced) {
            selectedLevelIndex = simulation.levelIndex
            beginLevelTransition()
            return
        }

        selectedLevelIndex = 0
        simulation.setLevel(selectedLevelIndex)
        overlayMode = OverlayMode.GAME
        menuHint = "SIMULATION LOOP"
        openLevelReady()
    }

    private fun beginLevelTransition() {
        accumulator = 0f
        transitionRemainingSeconds = 1.05f
        overlayMode = OverlayMode.TRANSITION
        runResultCommitted = true
        cancelReadyCountdown()
        statusMessage = t("LOADING", "YÜKLENİYOR")
        shakeTimeRemaining = 0f
        shakePower = 0f
        screenFlashAlpha = 0f
        lastPhaseActive = false
        lastObservedRunPhase = simulation.runPhase
        freeTurnImpulseDirection = 0
        freeTurnImpulseRemaining = 0f
        simulationResultDelaySeconds = 0f
        simulationStepCooldownSeconds = 0f
        resetMotionFx()
    }

    private fun openLevelReady() {
        accumulator = 0f
        runResultCommitted = true
        pendingInterstitialAfterClear = false
        shieldArmedForRun = false
        shieldUsedThisRun = false
        slowUsedThisRun = false
        syncSelectedShipHitbox()
        simulation.resetLevelToReady()
        updateBlockBriefingForCurrentLevel()
        cancelReadyCountdown()
        statusMessage = if (simulationControlEnabled) {
            t("SIM READY", "SİMÜLASYON HAZIR")
        } else {
            controlHintForLevel(simulation.levelConfig)
        }
        shakeTimeRemaining = 0f
        shakePower = 0f
        screenFlashAlpha = 0f
        lastPhaseActive = false
        lastObservedRunPhase = simulation.runPhase
        freeTurnImpulseDirection = 0
        freeTurnImpulseRemaining = 0f
        simulationResultDelaySeconds = 0f
        simulationStepCooldownSeconds = 0f
        lastShieldBreakCounter = simulation.shieldBreakCounter
        lastObstacleCountForPassCue = simulation.obstacles.size
        lastLevelClearCoinsAwarded = 0
        levelClearDoubleClaimed = false
        resetMotionFx()
    }

    private fun startAttempt() {
        if (isLifeLocked()) {
            statusMessage = buildLifeLockedMessage()
            return
        }
        accumulator = 0f
        runResultCommitted = false
        pendingInterstitialAfterClear = false
        reviveUsedThisRun = false
        shieldUsedThisRun = false
        slowUsedThisRun = false
        syncSelectedShipHitbox()
        simulation.startRun()
        val armShieldThisRun = shieldArmedForRun && shieldCount > 0
        if (armShieldThisRun) {
            shieldCount = (shieldCount - 1).coerceAtLeast(0)
            saveShieldCount()
            simulation.activateShield(1)
            shieldUsedThisRun = true
        }
        shieldArmedForRun = false
        blockBriefingVisible = false
        cancelReadyCountdown()
        statusMessage = if (simulationControlEnabled) t("SIM RUN", "SİMÜLASYON KOŞUSU") else ""
        shakeTimeRemaining = 0f
        shakePower = 0f
        screenFlashAlpha = 0f
        lastPhaseActive = false
        lastObservedRunPhase = simulation.runPhase
        freeTurnImpulseDirection = 0
        freeTurnImpulseRemaining = 0f
        simulationResultDelaySeconds = 0f
        lastShieldBreakCounter = simulation.shieldBreakCounter
        lastObstacleCountForPassCue = simulation.obstacles.size
        lastLevelClearCoinsAwarded = 0
        levelClearDoubleClaimed = false
        resetMotionFx()
    }

    private fun openPolicyPage(page: PolicyPage) {
        selectedPolicyPage = page
        resetPolicyScroll()
        overlayMode = OverlayMode.POLICY
    }

    private fun returnToMenu() {
        selectedLevelIndex = simulation.levelIndex
        simulation.resetLevelToReady()
        menuReturnToPause = false
        overlayMode = OverlayMode.INTRO
        pendingInterstitialAfterClear = false
        cancelReadyCountdown()
        statusMessage = ""
        lastPhaseActive = false
        shieldUsedThisRun = false
        slowUsedThisRun = false
        lastObservedRunPhase = simulation.runPhase
        freeTurnImpulseDirection = 0
        freeTurnImpulseRemaining = 0f
        simulationResultDelaySeconds = 0f
        simulationStepCooldownSeconds = 0f
        lastObstacleCountForPassCue = simulation.obstacles.size
        lastLevelClearCoinsAwarded = 0
        levelClearDoubleClaimed = false
        resetMotionFx()
    }

    private fun nowEpochSeconds(): Long = dependencies.epochSecondsProvider.nowEpochSeconds()

    private fun refreshLivesState() {
        if (premiumEnabled) {
            livesState = LivesState(lives = MAX_LIVES, lastUpdatedEpochSeconds = nowEpochSeconds())
            return
        }
        livesState = livesManager.snapshot(nowEpochSeconds())
    }

    private fun consumeLifeAfterFailure() {
        if (premiumEnabled) {
            return
        }
        val now = nowEpochSeconds()
        livesManager.consumeLifeForAttempt(now)
        livesState = livesManager.snapshot(now)
    }

    private fun saveShieldCount() {
        profilePreferences.putInteger(STORE_SHIELD_COUNT_KEY, shieldCount.coerceIn(0, MAX_SHIELDS)).flush()
    }

    private fun saveSlowPowerCount() {
        profilePreferences.putInteger(STORE_SLOW_POWER_COUNT_KEY, slowPowerCount.coerceIn(0, MAX_SLOW_POWERS)).flush()
    }

    private fun saveCoinBalance() {
        profilePreferences.putInteger(STORE_COINS_KEY, coinBalance.coerceAtLeast(0)).flush()
    }

    private fun awardCoinsForLevelClear(): Int {
        val base = 2
        val tierBonus = simulation.levelConfig.featureTier
        val levelBonus = ((simulation.levelConfig.index - 1) / 20)
        val reward = (base + tierBonus + levelBonus).coerceAtLeast(4)
        coinBalance += reward
        saveCoinBalance()
        return reward
    }

    private fun requestRewardedAd(onResult: (RewardedLifeResult) -> Unit) {
        // Guarantee the callback fires so adActionInProgress can never stay true forever.
        try {
            dependencies.rewardedLifeService.requestLifeReward(onResult)
        } catch (throwable: Throwable) {
            onResult(RewardedLifeResult.Failed(throwable.message ?: "Rewarded ad unavailable"))
        }
    }

    private fun canUseCoinDoubleAd(): Boolean {
        if (!adsEnabled() || adActionInProgress || simulationControlEnabled || premiumEnabled) {
            return false
        }
        if (lastLevelClearCoinsAwarded <= 0 || levelClearDoubleClaimed) {
            return false
        }
        return try {
            dependencies.rewardedLifeService.isRewardAvailable()
        } catch (_: Throwable) {
            false
        }
    }

    private fun requestCoinDoubleFromRewardedAd() {
        if (!canUseCoinDoubleAd()) {
            return
        }
        adActionInProgress = true
        statusMessage = t("WATCHING AD...", "REKLAM İZLENİYOR...")
        requestRewardedAd { result ->
            Gdx.app.postRunnable {
                adActionInProgress = false
                when (result) {
                    is RewardedLifeResult.Granted -> {
                        if (!levelClearDoubleClaimed && lastLevelClearCoinsAwarded > 0) {
                            coinBalance += lastLevelClearCoinsAwarded
                            saveCoinBalance()
                            levelClearDoubleClaimed = true
                            statusMessage = t(
                                "COINS DOUBLED +${lastLevelClearCoinsAwarded}",
                                "COIN x2 +${lastLevelClearCoinsAwarded}"
                            )
                        }
                    }
                    is RewardedLifeResult.Failed -> {
                        statusMessage = t("Ad failed: ${result.reason}", "Reklam başarısız: ${result.reason}")
                    }
                }
            }
        }
    }

    private fun isLifeLocked(): Boolean {
        return !premiumEnabled && livesState.lives <= 0
    }

    private fun buildLivesLabel(): String {
        if (premiumEnabled) {
            return t("PREMIUM • INF HEART", "PREMIUM • SONSUZ CAN")
        }
        val nextLife = livesManager.secondsUntilNextLife(nowEpochSeconds())
        return if (livesState.lives >= MAX_LIVES) {
            t("LIVES ${livesState.lives}/$MAX_LIVES • FULL", "CAN ${livesState.lives}/$MAX_LIVES • DOLU")
        } else {
            val refill = formatRefill(nextLife)
            t("LIVES ${livesState.lives}/$MAX_LIVES • NEXT $refill", "CAN ${livesState.lives}/$MAX_LIVES • SONRAKİ $refill")
        }
    }

    private fun buildLivesHeartLabel(): String {
        if (premiumEnabled) {
            return t("INF HEART • ¦ $shieldCount • ? $slowPowerCount", "SONSUZ CAN • ¦ $shieldCount • ? $slowPowerCount")
        }
        val nextLife = livesManager.secondsUntilNextLife(nowEpochSeconds())
        val shieldInfo = t("¦ $shieldCount • ? $slowPowerCount", "¦ $shieldCount • ? $slowPowerCount")
        return if (livesState.lives >= MAX_LIVES) {
            t("\u2665 ${livesState.lives}/$MAX_LIVES • Full • $shieldInfo", "\u2665 ${livesState.lives}/$MAX_LIVES • Dolu • $shieldInfo")
        } else {
            val refill = formatRefill(nextLife)
            t("\u2665 ${livesState.lives}/$MAX_LIVES • Next $refill • $shieldInfo", "\u2665 ${livesState.lives}/$MAX_LIVES • Sonraki $refill • $shieldInfo")
        }
    }

    private fun buildLifeLockedMessage(): String {
        if (premiumEnabled) {
            return t("READY", "HAZIR")
        }
        val seconds = livesManager.secondsUntilNextLife(nowEpochSeconds())
        return t("OUT OF LIVES • ${formatRefill(seconds)}", "CAN BİTTİ • ${formatRefill(seconds)}")
    }

    private fun buildFailureStatusLabel(): String {
        val assistTag = if (simulation.adaptiveAssistIntensity > 0.05f) {
            val percent = (simulation.adaptiveAssistIntensity * 100f).toInt().coerceIn(0, 100)
            t(" • HELP $percent%", " • YARDIM $percent%")
        } else {
            ""
        }
        val reason = when (simulation.lastDeathCause) {
            GameSimulation.DeathCause.MISSILE -> t("MISSILE HIT", "FÜZEYE ÇARPTIN")
            GameSimulation.DeathCause.CORE -> t("CORE COLLISION", "ÇEKİRDEĞE ÇARPTIN")
            GameSimulation.DeathCause.WALL -> t("HIT THE WALL", "DUVARA ÇARPTIN")
        }
        if (premiumEnabled) {
            return t("FAILURE • $reason$assistTag", "BAŞARISIZ • $reason$assistTag")
        }
        return t("FAILURE • $reason • ${buildLivesLabel()}$assistTag", "BAŞARISIZ • $reason • ${buildLivesLabel()}$assistTag")
    }

    private fun formatRefill(seconds: Long): String {
        val clamped = seconds.coerceAtLeast(0L)
        val minutes = clamped / 60L
        val remainingSeconds = clamped % 60L
        return "%02d:%02d".format(minutes, remainingSeconds)
    }

    private fun canUseReviveAd(): Boolean {
        return adsEnabled() &&
            !premiumEnabled &&
            !adActionInProgress &&
            simulation.runPhase == RunPhase.GAME_OVER &&
            !reviveUsedThisRun &&
            dependencies.rewardedLifeService.isRewardAvailable()
    }

    private fun canUseExtraLifeAd(): Boolean {
        return adsEnabled() &&
            !premiumEnabled &&
            !adActionInProgress &&
            livesState.lives < MAX_LIVES &&
            dependencies.rewardedLifeService.isRewardAvailable()
    }

    private fun canUseShieldAd(): Boolean {
        return adsEnabled() &&
            !premiumEnabled &&
            !adActionInProgress &&
            shieldCount < MAX_SHIELDS &&
            dependencies.rewardedLifeService.isRewardAvailable()
    }

    private fun canUseSlowAd(): Boolean {
        return adsEnabled() &&
            !premiumEnabled &&
            !adActionInProgress &&
            slowPowerCount < MAX_SLOW_POWERS &&
            dependencies.rewardedLifeService.isRewardAvailable()
    }

    private fun canArmShieldForNextRun(): Boolean {
        return shieldCount > 0 && simulation.runPhase == RunPhase.READY && !shieldUsedThisRun
    }

    private fun toggleShieldArm() {
        if (!canArmShieldForNextRun()) {
            statusMessage = if (shieldCount <= 0) {
                t("No shield in stock", "Kalkan stoğu yok")
            } else if (shieldUsedThisRun) {
                t("Shield already used this run", "Bu koşuda kalkan kullanıldı")
            } else {
                t("Shield can only be armed before run", "Kalkan sadece koşu başlamadan açılabilir")
            }
            return
        }
        shieldArmedForRun = !shieldArmedForRun
        statusMessage = if (shieldArmedForRun) {
            t("Shield armed for this run", "Bu koşu için kalkan açıldı")
        } else {
            t("Shield disarmed", "Kalkan kapatıldı")
        }
    }

    private fun handleShieldActionButton() {
        when (simulation.runPhase) {
            RunPhase.READY -> {
                toggleShieldArm()
            }

            RunPhase.RUNNING,
            RunPhase.DRAINING -> {
                if (shieldUsedThisRun) {
                    statusMessage = t("Shield already used this run", "Bu koşuda kalkan kullanıldı")
                    return
                }
                if (simulation.shieldActive) {
                    statusMessage = t("Shield already active", "Kalkan zaten aktif")
                    return
                }
                if (shieldCount <= 0) {
                    statusMessage = t("No shield in stock", "Kalkan stoğu yok")
                    return
                }
                shieldCount = (shieldCount - 1).coerceAtLeast(0)
                saveShieldCount()
                shieldArmedForRun = false
                shieldUsedThisRun = true
                simulation.activateShield(1)
                playUiSound(shieldActivateSound ?: uiConfirmSound, 0.72f, 1.04f)
                statusMessage = t("Shield activated", "Kalkan aktif edildi")
            }

            else -> Unit
        }
    }

    private fun handleSlowPowerButton() {
        if (simulation.runPhase != RunPhase.RUNNING && simulation.runPhase != RunPhase.DRAINING) {
            statusMessage = t("Start run to use time slow", "Zaman yavaşlatma için koşuyu başlat")
            return
        }
        if (simulation.manualSlowActive) {
            statusMessage = t("Time slow already active", "Zaman yavaşlatma zaten aktif")
            return
        }
        if (slowUsedThisRun) {
            statusMessage = t("Slowdown already used this run", "Bu koşuda yavaşlatma kullanıldı")
            return
        }
        if (slowPowerCount <= 0) {
            statusMessage = t("No time slow power left", "Zaman yavaşlatma stoğu bitti")
            return
        }
        slowPowerCount = (slowPowerCount - 1).coerceAtLeast(0)
        saveSlowPowerCount()
        slowUsedThisRun = true
        simulation.activateManualSlow(2.6f)
        playUiSound(slowActivateSound ?: uiConfirmSound, 0.7f, 0.92f)
        statusMessage = t("Time slowed", "Zaman yavaşlatıldı")
    }

    private fun requestReviveFromRewardedAd() {
        if (!canUseReviveAd()) {
            statusMessage = t("Rewarded ad unavailable", "Ödüllü reklam hazır değil")
            return
        }
        adActionInProgress = true
        statusMessage = t("LOADING REWARD AD...", "ÖDÜLLÜ REKLAM YÜKLENİYOR...")
        requestRewardedAd { result ->
            Gdx.app.postRunnable {
                adActionInProgress = false
                when (result) {
                    RewardedLifeResult.Granted -> {
                        val revived = simulation.reviveAfterGameOver()
                        if (revived) {
                            reviveUsedThisRun = true
                            runResultCommitted = false
                            statusMessage = t("REVIVED", "DEVAM")
                        } else {
                            statusMessage = t("Revive unavailable", "Devam kullanılamadı")
                        }
                    }
                    is RewardedLifeResult.Failed -> {
                        statusMessage = t("Ad failed: ${result.reason}", "Reklam başarısız: ${result.reason}")
                    }
                }
            }
        }
    }

    private fun requestExtraLifeFromRewardedAd() {
        if (!canUseExtraLifeAd()) {
            statusMessage = if (livesState.lives >= MAX_LIVES) {
                t("LIVES ALREADY FULL", "CANLAR ZATEN DOLU")
            } else {
                t("REWARDED AD UNAVAILABLE", "ÖDÜLLÜ REKLAM HAZIR DEĞİL")
            }
            return
        }
        adActionInProgress = true
        statusMessage = t("LOADING REWARD AD...", "ÖDÜLLÜ REKLAM YÜKLENİYOR...")
        requestRewardedAd { result ->
            Gdx.app.postRunnable {
                adActionInProgress = false
                when (result) {
                    RewardedLifeResult.Granted -> {
                        val now = nowEpochSeconds()
                        livesManager.grantLife(now, amount = 1)
                        livesState = livesManager.snapshot(now)
                        statusMessage = t("+1 LIFE GRANTED", "+1 CAN VERİLDİ")
                    }
                    is RewardedLifeResult.Failed -> {
                        statusMessage = t("Ad failed: ${result.reason}", "Reklam başarısız: ${result.reason}")
                    }
                }
            }
        }
    }

    private fun requestShieldFromRewardedAd() {
        if (!canUseShieldAd()) {
            statusMessage = if (shieldCount >= MAX_SHIELDS) {
                t("SHIELDS ALREADY FULL", "KALKANLAR ZATEN DOLU")
            } else {
                t("SHIELD AD UNAVAILABLE", "KALKAN REKLAMI HAZIR DEĞİL")
            }
            return
        }
        adActionInProgress = true
        statusMessage = t("LOADING REWARD AD...", "ÖDÜLLÜ REKLAM YÜKLENİYOR...")
        requestRewardedAd { result ->
            Gdx.app.postRunnable {
                adActionInProgress = false
                when (result) {
                    RewardedLifeResult.Granted -> {
                        shieldCount = (shieldCount + 1).coerceAtMost(MAX_SHIELDS)
                        saveShieldCount()
                        statusMessage = t("+1 SHIELD GRANTED", "+1 KALKAN VERİLDİ")
                    }
                    is RewardedLifeResult.Failed -> {
                        statusMessage = t("Ad failed: ${result.reason}", "Reklam başarısız: ${result.reason}")
                    }
                }
            }
        }
    }

    private fun requestSlowFromRewardedAd() {
        if (!canUseSlowAd()) {
            statusMessage = if (slowPowerCount >= MAX_SLOW_POWERS) {
                t("TIME SLOW STOCK FULL", "ZAMAN YAVAŞLATMA STOKU DOLU")
            } else {
                t("SLOW AD UNAVAILABLE", "YAVAŞLATMA REKLAMI HAZIR DEĞİL")
            }
            return
        }
        adActionInProgress = true
        statusMessage = t("LOADING REWARD AD...", "ÖDÜLLÜ REKLAM YÜKLENİYOR...")
        requestRewardedAd { result ->
            Gdx.app.postRunnable {
                adActionInProgress = false
                when (result) {
                    RewardedLifeResult.Granted -> {
                        slowPowerCount = (slowPowerCount + 1).coerceAtMost(MAX_SLOW_POWERS)
                        saveSlowPowerCount()
                        statusMessage = t("+1 TIME SLOW GRANTED", "+1 ZAMAN YAVAŞLATMA VERİLDİ")
                    }
                    is RewardedLifeResult.Failed -> {
                        statusMessage = t("Ad failed: ${result.reason}", "Reklam başarısız: ${result.reason}")
                    }
                }
            }
        }
    }

    private fun showInterstitialIfNeeded(_onDismissed: () -> Unit): Boolean {
        // Level-transition interstitials are disabled by design.
        pendingInterstitialAfterClear = false
        return false
    }

    private fun syncBannerVisibility(force: Boolean = false) {
        val shouldShow = adsEnabled() && !premiumEnabled && when (overlayMode) {
            OverlayMode.INTRO,
            OverlayMode.MENU,
            OverlayMode.LEVEL_SELECT,
            OverlayMode.POLICY -> true
            else -> false
        }
        if (!force && bannerVisible == shouldShow) {
            return
        }
        bannerVisible = shouldShow
        dependencies.bannerAdService.setBannerVisible(shouldShow)
    }

    private fun refreshPremiumStoreStatus(showLoading: Boolean = true, restore: Boolean = false) {
        if (showLoading) {
            premiumStoreStatus = premiumStoreStatus.copy(
                isOwned = premiumEnabled,
                isLoading = true,
                message = null
            )
        }
        val request: ((PremiumStatus) -> Unit) -> Unit =
            if (restore) dependencies.premiumPurchaseService::restorePurchases
            else dependencies.premiumPurchaseService::refreshStatus
        request { result ->
            Gdx.app.postRunnable {
                val resolvedOwned = when {
                    result.isOwned -> true
                    result.message == null -> false
                    else -> premiumEnabled
                }
                premiumStoreStatus = result.copy(isOwned = resolvedOwned)
                applyPremiumOwnership(resolvedOwned)
            }
        }
    }

    private fun triggerPremiumPurchase() {
        if (premiumEnabled) {
            showPremiumDialog(
                type = PremiumDialogType.SUCCESS,
                title = t("PREMIUM ACTIVE", "PREMIUM AKTİF"),
                body = t(
                    "This device already has the one-time premium unlock verified.",
                    "Bu cihazda tek seferlik premium satın alımı zaten doğrulanmış durumda."
                ),
                primaryLabel = t("CONTINUE", "DEVAM"),
                secondaryLabel = t("STAY HERE", "BURADA KAL"),
                tone = ChipTone.SUCCESS
            )
            return
        }
        premiumDialogType = PremiumDialogType.PROCESSING
        premiumDialogTitle = t("CONNECTING STORE", "MAĞAZAYA BAĞLANIYOR")
        premiumDialogBody = t(
            "Preparing the one-time purchase flow for FluxCore Premium.",
            "FluxCore Premium için tek seferlik satın alma akışı hazırlanıyor."
        )
        premiumDialogPrimaryLabel = ""
        premiumDialogSecondaryLabel = ""
        premiumDialogTone = ChipTone.WARNING
        dependencies.premiumPurchaseService.launchPremiumPurchase { result ->
            Gdx.app.postRunnable {
                when (result) {
                    is PremiumPurchaseResult.Success -> {
                        premiumStoreStatus = premiumStoreStatus.copy(
                            isOwned = true,
                            isLoading = false,
                            isAvailableForPurchase = true,
                            product = result.product ?: premiumStoreStatus.product,
                            message = null
                        )
                        applyPremiumOwnership(true)
                        val successBody = if (adsEnabled()) {
                            t(
                                "FluxCore Premium is now active. Ads are removed and lives are unlimited on this device.",
                                "FluxCore Premium artık aktif. Reklamlar kaldırıldı ve bu cihazda can sınırı kalktı."
                            )
                        } else {
                            t(
                                "FluxCore Premium is now active. Lives are unlimited on this device.",
                                "FluxCore Premium artık aktif. Bu cihazda can sınırı kalktı."
                            )
                        }
                        showPremiumDialog(
                            type = PremiumDialogType.SUCCESS,
                            title = t("PURCHASE COMPLETE", "SATIN ALMA TAMAMLANDI"),
                            body = successBody,
                            primaryLabel = t("START PREMIUM", "PREMIUM BAŞLAT"),
                            secondaryLabel = t("STAY HERE", "BURADA KAL"),
                            tone = ChipTone.SUCCESS
                        )
                    }

                    PremiumPurchaseResult.Cancelled -> {
                        showPremiumDialog(
                            type = PremiumDialogType.FAILURE,
                            title = t("PURCHASE CANCELLED", "SATIN ALMA İPTAL"),
                            body = t(
                                "The one-time premium purchase was closed before completion.",
                                "Tek seferlik premium satın alma tamamlanmadan kapatıldı."
                            ),
                            primaryLabel = t("OK", "TAMAM"),
                            secondaryLabel = "",
                            tone = ChipTone.WARNING,
                            retryOnPrimary = false
                        )
                        refreshPremiumStoreStatus(showLoading = false)
                    }

                    is PremiumPurchaseResult.Failed -> {
                        showPremiumDialog(
                            type = PremiumDialogType.FAILURE,
                            title = t("PURCHASE FAILED", "SATIN ALMA BAŞARISIZ"),
                            body = t(
                                "Premium verification failed. Reason: ${result.reason}",
                                "Premium doğrulaması başarısız. Sebep: ${result.reason}"
                            ),
                            primaryLabel = t("TRY AGAIN", "TEKRAR DENE"),
                            secondaryLabel = t("CLOSE", "KAPAT"),
                            tone = ChipTone.ALERT,
                            retryOnPrimary = true
                        )
                        refreshPremiumStoreStatus(showLoading = false)
                    }
                }
            }
        }
    }

    private fun applyPremiumOwnership(isOwned: Boolean) {
        premiumEnabled = isOwned
        profilePreferences.putBoolean(STORE_PREMIUM_OWNED_KEY, premiumEnabled).flush()
        refreshLivesState()
        syncBannerVisibility(force = true)
        statusMessage = if (premiumEnabled) {
            t("PREMIUM VERIFIED", "PREMIUM DOĞRULANDI")
        } else {
            t("PREMIUM LOCKED", "PREMIUM KAPALI")
        }
    }

    private fun showPremiumDialog(
        type: PremiumDialogType,
        title: String,
        body: String,
        primaryLabel: String,
        secondaryLabel: String,
        tone: ChipTone,
        retryOnPrimary: Boolean = false
    ) {
        premiumDialogType = type
        premiumDialogTitle = title
        premiumDialogBody = body
        premiumDialogPrimaryLabel = primaryLabel
        premiumDialogSecondaryLabel = secondaryLabel
        premiumDialogTone = tone
        premiumDialogRetryOnPrimary = retryOnPrimary
    }

    private fun dismissPremiumDialog(closePage: Boolean) {
        premiumDialogType = PremiumDialogType.NONE
        premiumDialogTitle = ""
        premiumDialogBody = ""
        premiumDialogPrimaryLabel = ""
        premiumDialogSecondaryLabel = ""
        premiumDialogRetryOnPrimary = false
        if (closePage) {
            closePremiumPage()
        }
    }

    private fun applyDifficultySettings() {
        val (gameplayMultiplier, readabilityMultiplier) = when (settingsState.difficulty) {
            GameDifficulty.RELAXED -> 0.9f to 1.16f
            GameDifficulty.STANDARD -> 1f to 1f
            GameDifficulty.EXPERT -> 1.12f to 0.9f
        }
        simulation.setDifficultyTuning(
            gameplayMultiplier = gameplayMultiplier,
            readabilityMultiplier = readabilityMultiplier,
            adaptiveAssist = true
        )
    }

    private fun difficultyLabelLong(): String {
        return when (settingsState.difficulty) {
            GameDifficulty.RELAXED -> t("DIFFICULTY RELAXED", "ZORLUK RAHAT")
            GameDifficulty.STANDARD -> t("DIFFICULTY STANDARD", "ZORLUK NORMAL")
            GameDifficulty.EXPERT -> t("DIFFICULTY EXPERT", "ZORLUK UZMAN")
        }
    }

    private fun difficultyLabelShort(): String {
        return when (settingsState.difficulty) {
            GameDifficulty.RELAXED -> t("RELAXED", "RAHAT")
            GameDifficulty.STANDARD -> t("STANDARD", "NORMAL")
            GameDifficulty.EXPERT -> t("EXPERT", "UZMAN")
        }
    }

    private fun readJustTouchedWorld(): Boolean {
        if (!Gdx.input.justTouched()) {
            return false
        }
        tempTouch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(tempTouch)
        return true
    }

    private fun updateMenuVolumeSliderDrag(): Boolean {
        if (overlayMode != OverlayMode.MENU) {
            return false
        }
        for (pointer in 0..4) {
            if (!Gdx.input.isTouched(pointer)) {
                continue
            }
            tempTouch.set(Gdx.input.getX(pointer).toFloat(), Gdx.input.getY(pointer).toFloat())
            viewport.unproject(tempTouch)
            val musicRect = menuMusicVolumeRect()
            val musicTrack = volumeSliderTrackRect(musicRect)
            if (contains(musicRect, tempTouch.x, tempTouch.y) || contains(musicTrack, tempTouch.x, tempTouch.y)) {
                val normalized = ((tempTouch.x - musicTrack.x) / musicTrack.width).coerceIn(0f, 1f)
                settingsState = settingsManager.setMusicVolume(normalized)
                return true
            }
            val effectsRect = menuEffectsVolumeRect()
            val effectsTrack = volumeSliderTrackRect(effectsRect)
            if (contains(effectsRect, tempTouch.x, tempTouch.y) || contains(effectsTrack, tempTouch.x, tempTouch.y)) {
                val normalized = ((tempTouch.x - effectsTrack.x) / effectsTrack.width).coerceIn(0f, 1f)
                settingsState = settingsManager.setEffectsVolume(normalized)
                return true
            }
        }
        return false
    }

    private fun readPlayerInput(): GameSimulation.PlayerInput {
        if (
            simulationControlEnabled &&
            overlayMode == OverlayMode.GAME &&
            (simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING)
        ) {
            return simulationInput()
        }

        val controls = touchControlsLayout()
        val supportButtonsVisible = areSupportActionButtonsVisible()
        if (simulation.usesStepMovement) {
            var stepDirection = 0
            val leftPressedNow = (
                Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.A) ||
                    isTouchControlPressed(leftControl = true)
                )
            val rightPressedNow = (
                Gdx.input.isKeyPressed(Input.Keys.RIGHT) ||
                    Gdx.input.isKeyPressed(Input.Keys.D) ||
                    isTouchControlPressed(leftControl = false)
                )
            if (
                Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ||
                Gdx.input.isKeyJustPressed(Input.Keys.A) ||
                (leftPressedNow && !leftStepPressedLastFrame)
            ) {
                stepDirection += 1
            }
            if (
                Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) ||
                Gdx.input.isKeyJustPressed(Input.Keys.D) ||
                (rightPressedNow && !rightStepPressedLastFrame)
            ) {
                stepDirection -= 1
            }
            if (Gdx.input.justTouched()) {
                tempTouch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                viewport.unproject(tempTouch)
                if (
                    supportButtonsVisible &&
                    (
                        contains(touchShieldButtonRect(), tempTouch.x, tempTouch.y) ||
                            contains(touchSlowButtonRect(), tempTouch.x, tempTouch.y)
                    )
                ) {
                    leftStepPressedLastFrame = leftPressedNow
                    rightStepPressedLastFrame = rightPressedNow
                    return GameSimulation.PlayerInput(stepDirection = stepDirection.coerceIn(-1, 1))
                }
                when {
                    tempTouch.x in controls.leftX..(controls.leftX + controls.width) &&
                        tempTouch.y in controls.y..(controls.y + controls.height) -> stepDirection += 1

                    tempTouch.x in controls.rightX..(controls.rightX + controls.width) &&
                        tempTouch.y in controls.y..(controls.y + controls.height) -> stepDirection -= 1
                }
            }
            leftStepPressedLastFrame = leftPressedNow
            rightStepPressedLastFrame = rightPressedNow
            return GameSimulation.PlayerInput(stepDirection = stepDirection.coerceIn(-1, 1))
        }

        leftStepPressedLastFrame = false
        rightStepPressedLastFrame = false
        var holdDirection = 0
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            holdDirection += 1
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            holdDirection -= 1
        }
        for (pointer in 0..4) {
            if (!Gdx.input.isTouched(pointer)) {
                continue
            }

            if (isPointerInside(pointer, controls.leftX, controls.y, controls.width, controls.height)) {
                holdDirection += 1
            }
            if (isPointerInside(pointer, controls.rightX, controls.y, controls.width, controls.height)) {
                holdDirection -= 1
            }
        }

        if (Gdx.input.justTouched()) {
            tempTouch.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            viewport.unproject(tempTouch)
            if (
                supportButtonsVisible &&
                (
                    contains(touchShieldButtonRect(), tempTouch.x, tempTouch.y) ||
                        contains(touchSlowButtonRect(), tempTouch.x, tempTouch.y)
                )
            ) {
                return GameSimulation.PlayerInput(holdDirection = holdDirection.coerceIn(-1, 1))
            }
            val levelIndex = simulation.levelConfig.index
            val controlImpulse = when (levelIndex) {
                in 61..100 -> 0.065f
                in 51..60 -> 0.06f
                in 41..50 -> 0.075f
                else -> 0.12f
            }
            val screenImpulse = when (levelIndex) {
                in 61..100 -> 0.05f
                in 51..60 -> 0.045f
                in 41..50 -> 0.055f
                else -> 0.09f
            }
            when {
                tempTouch.x in controls.leftX..(controls.leftX + controls.width) &&
                    tempTouch.y in controls.y..(controls.y + controls.height) -> {
                    freeTurnImpulseDirection = 1
                    freeTurnImpulseRemaining = controlImpulse
                }

                tempTouch.x in controls.rightX..(controls.rightX + controls.width) &&
                    tempTouch.y in controls.y..(controls.y + controls.height) -> {
                    freeTurnImpulseDirection = -1
                    freeTurnImpulseRemaining = controlImpulse
                }

                tempTouch.x < viewport.worldWidth * 0.5f -> {
                    freeTurnImpulseDirection = 1
                    freeTurnImpulseRemaining = screenImpulse
                }

                else -> {
                    freeTurnImpulseDirection = -1
                    freeTurnImpulseRemaining = screenImpulse
                }
            }
        }

        return GameSimulation.PlayerInput(holdDirection = holdDirection.coerceIn(-1, 1))
    }

    private fun simulationInput(): GameSimulation.PlayerInput {
        val targetWorldAngle = simulationTargetAngleRad()
        val delta = shortestAngleDeltaRad(simulation.playerAngleRad, targetWorldAngle)
        val reverseMultiplier = simulation.phaseGateStatus.directionMultiplier

        if (simulation.usesStepMovement) {
            val sectorAngle = MathUtils.PI2 / simulation.levelConfig.sectorCount.coerceIn(3, 16).toFloat()
            if (abs(delta) < sectorAngle * 0.22f || simulationStepCooldownSeconds > 0f) {
                return GameSimulation.PlayerInput(stepDirection = 0)
            }

            simulationStepCooldownSeconds = 0.065f
            val worldDirection = if (delta > 0f) 1 else -1
            val inputDirection = (worldDirection * reverseMultiplier).coerceIn(-1, 1)
            return GameSimulation.PlayerInput(stepDirection = inputDirection)
        }

        val deadZone = 0.03f
        val worldDirection = when {
            delta > deadZone -> 1
            delta < -deadZone -> -1
            else -> 0
        }
        val inputDirection = (worldDirection * reverseMultiplier).coerceIn(-1, 1)
        return GameSimulation.PlayerInput(holdDirection = inputDirection)
    }

    private fun simulationTargetAngleRad(): Float {
        val nextThreat = simulation.obstacles
            .asSequence()
            .filter { it.radius + it.thickness >= simulation.playerOrbitRadiusNormalized - 0.01f }
            .minByOrNull { obstacleTimeToPlayerRing(it) }

        if (nextThreat == null) {
            return simulation.playerAngleRad
        }

        val targetLocalAngle = predictedGapCenterAtPlayerRing(nextThreat)
        return AngleMath.normalizeRadians(targetLocalAngle + simulation.arenaRotationRad)
    }

    private fun predictedGapCenterAtPlayerRing(obstacle: Obstacle): Float {
        val sectorAngle = MathUtils.PI2 / obstacle.sectorCount.toFloat()
        val gapCenterAtSpawn = AngleMath.normalizeRadians(
            obstacle.rotationRad + (obstacle.gapStartSector + obstacle.gapSectorCount * 0.5f) * sectorAngle
        )
        val travelSeconds = obstacleTimeToPlayerRing(obstacle)
        return AngleMath.normalizeRadians(gapCenterAtSpawn + obstacle.spinRadPerSecond * travelSeconds)
    }

    private fun obstacleTimeToPlayerRing(obstacle: Obstacle): Float {
        val radialDistance = (obstacle.radius - simulation.playerOrbitRadiusNormalized).coerceAtLeast(0f)
        return radialDistance / obstacle.speed.coerceAtLeast(0.01f)
    }

    private fun shortestAngleDeltaRad(from: Float, to: Float): Float {
        var delta = AngleMath.normalizeRadians(to - from)
        if (delta > MathUtils.PI) {
            delta -= MathUtils.PI2
        }
        return delta
    }

    private fun touchControlsLayout(): TouchControlsLayout {
        val gap = sx(24f).coerceIn(14f, 36f)
        val horizontalInset = sx(22f).coerceIn(12f, 30f)
        val width = ((viewport.worldWidth - horizontalInset * 2f - gap) * 0.5f)
            .coerceIn(sx(190f), sx(596f))
        val height = sy(236f).coerceIn(176f, 288f)
        val y = sy(28f).coerceIn(16f, 64f)
        val startX = (viewport.worldWidth - (width * 2f + gap)) * 0.5f
        return TouchControlsLayout(
            leftX = startX,
            rightX = startX + width + gap,
            y = y,
            width = width,
            height = height
        )
    }

    private fun touchShieldButtonRect(): UiRect {
        val controls = touchControlsLayout()
        val width = (viewport.worldWidth * 0.58f).coerceIn(sx(250f), sx(430f))
        val height = sy(62f).coerceIn(sy(52f), sy(84f))
        return UiRect(
            x = viewport.worldWidth * 0.5f - width * 0.5f,
            y = controls.y + controls.height + sy(18f).coerceIn(12f, 28f),
            width = width,
            height = height
        )
    }

    private fun touchSlowButtonRect(): UiRect {
        val shield = touchShieldButtonRect()
        val height = shield.height
        return UiRect(
            x = shield.x,
            y = shield.y + height + sy(12f).coerceIn(10f, 20f),
            width = shield.width,
            height = height
        )
    }

    private fun areSupportActionButtonsVisible(): Boolean {
        if (tutorialActive) {
            return false
        }
        return simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING
    }

    private fun isPointerInside(pointer: Int, x: Float, y: Float, width: Float, height: Float): Boolean {
        tempTouch.set(Gdx.input.getX(pointer).toFloat(), Gdx.input.getY(pointer).toFloat())
        viewport.unproject(tempTouch)
        return tempTouch.x in x..(x + width) && tempTouch.y in y..(y + height)
    }

    private fun isTouchControlPressed(leftControl: Boolean): Boolean {
        val controls = touchControlsLayout()
        val controlX = if (leftControl) controls.leftX else controls.rightX
        for (pointer in 0..4) {
            if (!Gdx.input.isTouched(pointer)) {
                continue
            }
            if (isPointerInside(pointer, controlX, controls.y, controls.width, controls.height)) {
                return true
            }
        }
        return false
    }

    private fun handleRunResultTransitions() {
        if (runResultCommitted || overlayMode != OverlayMode.GAME) {
            return
        }

        when (simulation.runPhase) {
            RunPhase.GAME_OVER -> {
                bestScoreManager.registerRunResult(
                    levelIndex = simulation.levelIndex,
                    survivedSeconds = simulation.survivedSeconds,
                    levelCleared = false
                )
                simulation.registerRunOutcome(levelCleared = false)
                if (!premiumEnabled) {
                    consumeLifeAfterFailure()
                }
                statusMessage = buildFailureStatusLabel()
                triggerHaptic(24)
                triggerImpact(0.46f, 26f, 0.34f)
                playUiSound(hitSound, 0.72f, 0.92f)
                runResultCommitted = true
            }

            RunPhase.LEVEL_CLEARED -> {
                bestScoreManager.registerRunResult(
                    levelIndex = simulation.levelIndex,
                    survivedSeconds = simulation.survivedSeconds,
                    levelCleared = true
                )
                simulation.registerRunOutcome(levelCleared = true)
                levelClearCount += 1
                profilePreferences.putInteger(STORE_LEVEL_CLEAR_COUNT_KEY, levelClearCount).flush()
                pendingInterstitialAfterClear = false
                val earnedCoins = awardCoinsForLevelClear()
                lastLevelClearCoinsAwarded = earnedCoins
                levelClearDoubleClaimed = false
                statusMessage = t("CLEAR +$earnedCoins COINS", "TAMAMLANDI +$earnedCoins COIN")
                triggerHaptic(36)
                triggerImpact(0.3f, 16f, 0.2f)
                playUiSound(clearSound, 0.7f, 1.05f)
                runResultCommitted = true
            }

            RunPhase.READY,
            RunPhase.DRAINING,
            RunPhase.RUNNING -> Unit
        }
    }

    private fun updatePresentationEffects(delta: Float) {
        updateRunPhaseCues()
        val runIsActive = simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING
        val phaseActive = overlayMode == OverlayMode.GAME &&
            runIsActive &&
            simulation.phaseGateStatus.active
        if (phaseActive && !lastPhaseActive) {
            triggerImpact(0.08f, 5f, 0.05f)
            statusMessage = t("REVERSE", "TERS")
        } else if (!phaseActive && lastPhaseActive && runIsActive) {
            statusMessage = t("FLOW", "AKIŞ")
        }
        lastPhaseActive = phaseActive

        if (overlayMode == OverlayMode.GAME) {
            val runActiveForPassCue = simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING
            val obstacleCount = simulation.obstacles.size
            if (runActiveForPassCue && lastObstacleCountForPassCue > obstacleCount) {
                val removedCount = (lastObstacleCountForPassCue - obstacleCount).coerceAtLeast(1)
                val passPitch = (0.95f + removedCount * 0.03f).coerceIn(0.92f, 1.14f)
                playUiSound(wallPassSound, 0.2f, passPitch)
            }
            lastObstacleCountForPassCue = obstacleCount

            if (simulation.shieldBreakCounter > lastShieldBreakCounter) {
                statusMessage = t("SHIELD BROKEN", "KALKAN KIRILDI")
                triggerImpact(0.2f, 12f, 0.18f)
                playUiSound(hitSound, 0.44f, 1.18f)
            }
            lastShieldBreakCounter = simulation.shieldBreakCounter
            val bubbleActive = simulation.timeBubbleActive
            if (bubbleActive && !lastTimeBubbleActive) {
                playUiSound(stormSound, 0.5f, 0.86f)
            }
            lastTimeBubbleActive = bubbleActive

            val targetAngle = simulation.playerAngleRad
            if (!angleSamplerReady) {
                visualPlayerAngleRad = targetAngle
                lastPlayerAngleSampleRad = targetAngle
                angleSamplerReady = true
            }

            val safeDelta = delta.coerceAtLeast(1f / 240f)
            val sampledDelta = shortestAngleDeltaRad(lastPlayerAngleSampleRad, targetAngle)
            val signedAngularSpeed = sampledDelta / safeDelta
            val baseSpeed = simulation.levelConfig.needleAngularSpeedRad.coerceAtLeast(0.4f)
            val targetIntensity = (abs(signedAngularSpeed) / (baseSpeed * 1.34f)).coerceIn(0f, 1.65f)
            val targetSigned = (signedAngularSpeed / (baseSpeed * 1.05f)).coerceIn(-1.4f, 1.4f)

            motionSpeedIntensity += (targetIntensity - motionSpeedIntensity) * (delta * 8.6f).coerceAtMost(1f)
            motionSignedIntensity += (targetSigned - motionSignedIntensity) * (delta * 9.4f).coerceAtMost(1f)
            val tiltTarget = motionSignedIntensity * (0.34f + motionSpeedIntensity * 1.08f)
            motionTiltDegrees += (tiltTarget - motionTiltDegrees) * (delta * 8.4f).coerceAtMost(1f)

            val smoothFactor = (delta * (12f + motionSpeedIntensity * 5f)).coerceIn(0f, 1f)
            val visualDelta = shortestAngleDeltaRad(visualPlayerAngleRad, targetAngle)
            visualPlayerAngleRad = AngleMath.normalizeRadians(visualPlayerAngleRad + visualDelta * smoothFactor)

            val hardLevel = simulation.levelConfig.index >= 41
            val obstacleBaseSpeed = simulation.levelConfig.baseObstacleSpeed.coerceAtLeast(0.01f)
            val playerRadius = simulation.playerOrbitRadiusNormalized
            var wallPressure = 0f
            simulation.obstacles.forEach { obstacle ->
                val normalizedSpeed = obstacle.speed / obstacleBaseSpeed
                val speedPressure = ((normalizedSpeed - 1.08f) / 0.62f).coerceIn(0f, 1.25f)
                if (speedPressure <= 0f) return@forEach
                val distanceToPlayer = (obstacle.radius - playerRadius).coerceAtLeast(0f)
                val proximity = (1f - (distanceToPlayer / 0.56f)).coerceIn(0f, 1f)
                wallPressure = maxOf(wallPressure, speedPressure * proximity)
            }
            val gateTarget = if (hardLevel) {
                ((wallPressure - 0.52f) / 0.7f).coerceIn(0f, 1f)
            } else {
                0f
            }
            speedEdgeGate += (gateTarget - speedEdgeGate) * (delta * 7.6f).coerceAtMost(1f)

            lastPlayerAngleSampleRad = targetAngle
        } else {
            motionSpeedIntensity += (0f - motionSpeedIntensity) * (delta * 7.4f).coerceAtMost(1f)
            motionSignedIntensity += (0f - motionSignedIntensity) * (delta * 7.4f).coerceAtMost(1f)
            motionTiltDegrees += (0f - motionTiltDegrees) * (delta * 9f).coerceAtMost(1f)
            speedEdgeGate += (0f - speedEdgeGate) * (delta * 8.4f).coerceAtMost(1f)
            visualPlayerAngleRad = simulation.playerAngleRad
            lastPlayerAngleSampleRad = simulation.playerAngleRad
            angleSamplerReady = false
            lastShieldBreakCounter = simulation.shieldBreakCounter
            lastTimeBubbleActive = false
        }

        shakeTimeRemaining = (shakeTimeRemaining - delta).coerceAtLeast(0f)
        if (shakeTimeRemaining == 0f) {
            shakePower += (0f - shakePower) * (delta * 14f).coerceAtMost(1f)
        }
        screenFlashAlpha = (screenFlashAlpha - delta * 1.5f).coerceAtLeast(0f)
    }

    private fun updateRunPhaseCues() {
        if (overlayMode != OverlayMode.GAME) {
            lastObservedRunPhase = simulation.runPhase
            return
        }
        val current = simulation.runPhase
        if (current == lastObservedRunPhase) {
            return
        }
        if (current == RunPhase.DRAINING && lastObservedRunPhase == RunPhase.RUNNING) {
            statusMessage = t("DRAINING", "TEMİZLENİYOR")
            playUiSound(uiConfirmSound, 0.45f, 1.1f)
        }
        if (current == RunPhase.RUNNING && lastObservedRunPhase == RunPhase.READY) {
            playUiSound(uiStartSound, 0.42f, 1f)
        }
        lastObservedRunPhase = current
    }

    private fun triggerImpact(duration: Float, power: Float, flashAlpha: Float) {
        shakeTimeRemaining = shakeTimeRemaining.coerceAtLeast(duration)
        shakePower = shakePower.coerceAtLeast(power)
        screenFlashAlpha = screenFlashAlpha.coerceAtLeast(flashAlpha)
        if (flashNeonPalette.isNotEmpty()) {
            val index = MathUtils.random(0, flashNeonPalette.lastIndex)
            flashColor.set(flashNeonPalette[index])
        }
    }

    private fun triggerHaptic(milliseconds: Int) {
        if (!settingsState.hapticsEnabled) {
            return
        }

        try {
            Gdx.input.vibrate(milliseconds)
        } catch (_: Throwable) {
            // Keep gameplay safe on platforms without haptic support.
        }
    }

    private fun resetMotionFx() {
        motionSpeedIntensity = 0f
        motionSignedIntensity = 0f
        motionTiltDegrees = 0f
        speedEdgeGate = 0f
        lastPlayerAngleSampleRad = simulation.playerAngleRad
        visualPlayerAngleRad = simulation.playerAngleRad
        angleSamplerReady = true
    }

    private fun drawFrame() {
        val gameplayOverlay = overlayMode == OverlayMode.GAME || overlayMode == OverlayMode.PAUSE
        val levelForPalette = if (gameplayOverlay) {
            simulation.levelConfig.index
        } else {
            selectedLevelIndex + 1
        }
        val palette = NeonPaletteRamp.forLevel(levelForPalette)
        val isChromeScreen = !gameplayOverlay
        val bg = if (isChromeScreen) chromeBackground else spaceBackground
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        viewport.apply()
        appliedCameraTiltDegrees = 0f
        val shakeMix = if (shakeTimeRemaining > 0f) {
            (shakeTimeRemaining * 5.2f).coerceAtMost(1f)
        } else {
            0f
        }
        val motionLateral = if (gameplayOverlay) {
            0f
        } else {
            0f
        }
        camera.position.set(
            viewport.worldWidth * 0.5f + sin(worldTime * 78f) * shakePower * shakeMix + motionLateral,
            viewport.worldHeight * 0.5f + cos(worldTime * 91f) * shakePower * 0.65f * shakeMix,
            0f
        )
        camera.update()

        val activeLevel = if (gameplayOverlay) {
            simulation.levelConfig
        } else {
            levels[selectedLevelIndex]
        }
        val arenaLayout = if (gameplayOverlay) gameArenaLayout() else previewArenaLayout()
        val rotation = if (gameplayOverlay) simulation.arenaRotationRad else previewRotation(activeLevel)

        font.color = if (isChromeScreen) chromeInk else Color.WHITE
        titleFont.color = if (isChromeScreen) chromeInk else Color.WHITE

        if (gameplayOverlay) {
            drawBackgroundAura(palette, activeLevel, rotation, arenaLayout)
        } else if (
            overlayMode == OverlayMode.INTRO ||
            overlayMode == OverlayMode.SPLASH ||
            overlayMode == OverlayMode.EPILEPSY_WARNING
        ) {
            drawIntroBackdrop(palette)
        } else {
            drawChromeBackdrop(palette)
        }

        when (overlayMode) {
            OverlayMode.SPLASH -> drawSplashOverlay(palette)

            OverlayMode.EPILEPSY_WARNING -> drawEpilepsyWarningOverlay(palette)

            OverlayMode.INTRO -> drawIntroOverlay(palette, activeLevel)

            OverlayMode.SHOP -> drawShopOverlay(palette)

            OverlayMode.PREMIUM -> drawPremiumOverlay(palette)

            OverlayMode.POLICY -> drawPolicyOverlay(palette)

            OverlayMode.GAME -> {
                drawArena(palette)
                drawSpeedEdgeEffects(palette)
                drawGameHud(palette)
                if (simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING) {
                    drawTouchControls(palette)
                } else if (simulation.runPhase == RunPhase.READY && blockBriefingVisible) {
                    drawTouchControls(palette)
                } else if (simulation.runPhase == RunPhase.GAME_OVER || simulation.runPhase == RunPhase.LEVEL_CLEARED) {
                    drawResultOverlay(palette)
                }
                drawTutorialOverlay(palette)
            }

            OverlayMode.PAUSE -> {
                drawArena(palette)
                drawGameHud(palette)
                drawPauseOverlay(palette)
            }

            OverlayMode.MENU -> drawMenuOverlay(palette)

            OverlayMode.LEVEL_SELECT -> drawLevelSelectOverlayGrouped(palette)

            OverlayMode.TRANSITION -> drawTransitionOverlay(palette)
        }

        if (screenFlashAlpha > 0f) {
            drawFlashOverlay(palette)
        }
    }

    private fun drawBackgroundAura(
        palette: NeonPalette,
        level: LevelConfig,
        rotationRad: Float,
        layout: ArenaLayout
    ) {
        val cx = layout.cx
        val cy = layout.cy
        val radius = layout.radius * 1.01f
        val guideSides = guideSidesForLevel(level.sectorCount)
        val pulse = 1f + sin(worldTime * 4.8f + level.index * 0.2f) * 0.02f
        val chromeScreen = overlayMode != OverlayMode.GAME && overlayMode != OverlayMode.PAUSE
        val haloSides = (guideSides * 2).coerceIn(12, 48)

        shapes.projectionMatrix = camera.combined
        if (!chromeScreen) {
            val w = viewport.worldWidth
            val h = viewport.worldHeight
            if (level.index in 21..30) {
                drawNatureBackdrop(w, h)
            } else if (level.index in 61..70) {
                drawWarBackdrop(w, h)
            } else if (level.index in 71..80) {
                drawBioBackdrop(w, h, level.index)
            } else if (level.index in 91..100) {
                drawKitchenBackdrop(w, h, level.index)
            } else {
                shapes.begin(ShapeRenderer.ShapeType.Filled)
                shapes.color = spaceBackground
                shapes.rect(0f, 0f, w, h)
                drawStarfield(w, h)
                shapes.end()
            }
            return
        }

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(palette.uiAccent).mul(1f, 1f, 1f, 0.08f)
        drawFilledPolygon(cx, cy, radius * 1.22f * pulse, haloSides, rotationRad * 0.35f, layout.yScale)
        shapes.color = Color(chromeInk).mul(1f, 1f, 1f, 0.04f)
        drawFilledPolygon(cx, cy, radius * 0.9f, haloSides, -rotationRad * 0.18f, layout.yScale)
        shapes.color = chromeBackground
        drawFilledPolygon(cx, cy, radius * 0.24f, guideSides, rotationRad * -0.6f, layout.yScale)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(chromeInk).mul(1f, 1f, 1f, 0.08f)
        drawPolygonOutline(cx, cy, radius * 0.52f, guideSides, rotationRad, layout.yScale)
        drawPolygonOutline(cx, cy, radius, guideSides, rotationRad, layout.yScale)
        shapes.end()
    }

    private fun drawStarfield(width: Float, height: Float) {
        val stride = if (lowPerformanceMode) 2 else 1
        var index = 0
        while (index < starfieldSamples.size) {
            val sample = starfieldSamples[index]
            val twinkle = 0.62f + sin(worldTime * sample.twinkleSpeed + sample.phase) * 0.38f
            val alpha = (sample.alphaBase * twinkle).coerceIn(0.08f, 0.44f)
            shapes.color = colorScratchA.set(0.82f, 0.9f, 1f, alpha)
            shapes.circle(sample.xNorm * width, sample.yNorm * height, sample.size, 12)
            index += stride
        }
    }

    private fun drawNatureBackdrop(width: Float, height: Float) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.6f, 0.82f, 0.96f, 1f)
        shapes.rect(0f, height * 0.48f, width, height * 0.52f)
        shapes.color = Color(0.72f, 0.9f, 1f, 1f)
        shapes.rect(0f, height * 0.7f, width, height * 0.3f)
        shapes.color = Color(0.29f, 0.62f, 0.26f, 1f)
        shapes.rect(0f, 0f, width, height * 0.48f)
        shapes.color = Color(0.2f, 0.5f, 0.22f, 1f)
        shapes.triangle(0f, height * 0.48f, width * 0.18f, height * 0.62f, width * 0.36f, height * 0.48f)
        shapes.triangle(width * 0.24f, height * 0.48f, width * 0.46f, height * 0.66f, width * 0.66f, height * 0.48f)
        shapes.triangle(width * 0.62f, height * 0.48f, width * 0.78f, height * 0.61f, width, height * 0.48f)

        val cloudAlpha = 0.32f + 0.08f * sin(worldTime * 0.9f)
        shapes.color = Color(1f, 1f, 1f, cloudAlpha)
        shapes.circle(width * 0.22f, height * 0.8f, sx(62f), 36)
        shapes.circle(width * 0.3f, height * 0.81f, sx(48f), 30)
        shapes.circle(width * 0.7f, height * 0.76f, sx(54f), 34)
        shapes.circle(width * 0.78f, height * 0.77f, sx(42f), 28)
        shapes.end()
    }

    private fun drawWarBackdrop(width: Float, height: Float) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.08f, 0.08f, 0.09f, 1f)
        shapes.rect(0f, 0f, width, height)
        shapes.color = Color(0.12f, 0.12f, 0.13f, 1f)
        shapes.rect(0f, height * 0.56f, width, height * 0.44f)
        shapes.color = Color(0.18f, 0.2f, 0.22f, 0.9f)
        shapes.triangle(0f, height * 0.42f, width * 0.26f, height * 0.64f, width * 0.52f, height * 0.42f)
        shapes.triangle(width * 0.44f, height * 0.42f, width * 0.7f, height * 0.62f, width, height * 0.42f)
        shapes.color = Color(0.3f, 0.17f, 0.12f, 0.82f)
        shapes.rect(0f, 0f, width, height * 0.24f)
        shapes.color = Color(1f, 0.58f, 0.26f, 0.18f + 0.08f * sin(worldTime * 2.2f))
        shapes.triangle(width * 0.08f, height * 0.24f, width * 0.2f, height * 0.34f, width * 0.32f, height * 0.24f)
        shapes.triangle(width * 0.62f, height * 0.24f, width * 0.78f, height * 0.38f, width * 0.94f, height * 0.24f)
        shapes.color = Color(0.85f, 0.92f, 1f, 0.1f)
        shapes.rectLine(width * 0.06f, height * 0.86f, width * 0.46f, height * 0.94f, sy(2.2f).coerceIn(1f, 4f))
        shapes.rectLine(width * 0.52f, height * 0.82f, width * 0.94f, height * 0.9f, sy(2f).coerceIn(1f, 4f))
        shapes.end()
    }

    private fun drawBioBackdrop(width: Float, height: Float, levelIndex: Int) {
        val variant = (levelIndex - 71).mod(4)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val baseColor = when (variant) {
            0 -> Color(0.03f, 0.012f, 0.06f, 1f) // Cell interior
            1 -> Color(0.06f, 0.014f, 0.04f, 1f) // Tissue layer
            2 -> Color(0.02f, 0.03f, 0.06f, 1f) // Bloodstream
            else -> Color(0.014f, 0.04f, 0.06f, 1f) // Lab field
        }
        shapes.color = baseColor
        shapes.rect(0f, 0f, width, height)
        shapes.color = when (variant) {
            0 -> Color(0.08f, 0.02f, 0.12f, 0.94f)
            1 -> Color(0.12f, 0.03f, 0.1f, 0.94f)
            2 -> Color(0.06f, 0.08f, 0.14f, 0.94f)
            else -> Color(0.05f, 0.12f, 0.1f, 0.94f)
        }
        shapes.rect(0f, height * 0.56f, width, height * 0.44f)
        shapes.color = when (variant) {
            0 -> Color(0.11f, 0.03f, 0.16f, 0.84f)
            1 -> Color(0.16f, 0.05f, 0.14f, 0.84f)
            2 -> Color(0.08f, 0.1f, 0.17f, 0.84f)
            else -> Color(0.08f, 0.16f, 0.14f, 0.84f)
        }
        shapes.triangle(0f, height * 0.22f, width * 0.2f, height * 0.44f, width * 0.44f, height * 0.18f)
        shapes.triangle(width * 0.52f, height * 0.18f, width * 0.78f, height * 0.46f, width, height * 0.2f)

        val pulse = 0.86f + 0.14f * (0.5f + 0.5f * sin(worldTime * 2.3f))
        shapes.color = Color(0.76f, 0.22f, 0.64f, 0.22f * pulse)
        shapes.circle(width * 0.18f, height * 0.76f, sx(112f), 40)
        shapes.circle(width * 0.72f, height * 0.66f, sx(94f), 34)
        shapes.color = Color(0.44f, 0.9f, 0.76f, 0.2f * pulse)
        shapes.circle(width * 0.36f, height * 0.32f, sx(86f), 32)
        shapes.circle(width * 0.86f, height * 0.3f, sx(72f), 28)

        val spikeBase = sy(3.2f).coerceIn(1.8f, 5.2f)
        for (index in 0 until 6) {
            val cx = width * (0.14f + index * 0.14f)
            val cy = height * (0.12f + (index % 2) * 0.09f)
            val r = sx(22f + (index % 3) * 7f)
            shapes.color = Color(0.62f, 0.14f, 0.48f, 0.44f)
            shapes.circle(cx, cy, r, 24)
            shapes.color = Color(0.86f, 0.32f, 0.62f, 0.5f)
            for (spike in 0 until 8) {
                val angle = spike * (MathUtils.PI2 / 8f) + worldTime * 0.25f + index * 0.2f
                val sx0 = cx + cos(angle) * r
                val sy0 = cy + sin(angle) * r
                val sx1 = cx + cos(angle) * (r + spikeBase * 3.4f)
                val sy1 = cy + sin(angle) * (r + spikeBase * 3.4f)
                shapes.rectLine(sx0, sy0, sx1, sy1, spikeBase)
            }
        }
        shapes.end()
    }

    private fun drawKitchenBackdrop(width: Float, height: Float, levelIndex: Int) {
        val variant = (levelIndex - 91).mod(3)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = when (variant) {
            0 -> Color(0.92f, 0.88f, 0.8f, 1f)
            1 -> Color(0.9f, 0.86f, 0.78f, 1f)
            else -> Color(0.95f, 0.9f, 0.82f, 1f)
        }
        shapes.rect(0f, 0f, width, height)
        shapes.color = Color(0.98f, 0.95f, 0.9f, 1f)
        shapes.rect(0f, height * 0.62f, width, height * 0.38f)
        shapes.color = Color(0.82f, 0.72f, 0.58f, 1f)
        shapes.rect(0f, height * 0.44f, width, height * 0.06f)
        shapes.color = Color(0.94f, 0.78f, 0.56f, 0.92f)
        shapes.rect(0f, 0f, width, height * 0.26f)

        // Bread-like stains / sauce splashes.
        val pulse = 0.84f + 0.16f * (0.5f + 0.5f * sin(worldTime * 1.9f))
        shapes.color = Color(0.92f, 0.44f, 0.26f, 0.2f * pulse)
        shapes.circle(width * 0.18f, height * 0.24f, sx(72f), 32)
        shapes.circle(width * 0.46f, height * 0.22f, sx(58f), 30)
        shapes.color = Color(1f, 0.9f, 0.62f, 0.16f * pulse)
        shapes.circle(width * 0.78f, height * 0.2f, sx(66f), 30)

        // Kitchen tile hints.
        val tile = sx(120f).coerceIn(72f, 148f)
        shapes.color = Color(0.88f, 0.8f, 0.68f, 0.2f)
        var x = 0f
        while (x < width + tile) {
            shapes.rectLine(x, height * 0.62f, x, height, sy(2f).coerceIn(1f, 3f))
            x += tile
        }
        var y = height * 0.62f
        while (y < height + tile) {
            shapes.rectLine(0f, y, width, y, sy(2f).coerceIn(1f, 3f))
            y += tile
        }
        shapes.end()
    }

    private fun buildStarfieldSamples(starCount: Int): List<StarSample> {
        return List(starCount) { index ->
            StarSample(
                xNorm = starNoise(index * 5 + 11, 1.3f),
                yNorm = starNoise(index * 5 + 17, 2.1f),
                size = 0.7f + starNoise(index * 5 + 23, 3.8f) * 2f,
                twinkleSpeed = 0.45f + starNoise(index * 5 + 29, 5.1f) * 0.75f,
                phase = starNoise(index * 5 + 31, 6.7f) * MathUtils.PI2,
                alphaBase = 0.18f + starNoise(index * 5 + 37, 7.9f) * 0.44f
            )
        }
    }

    private fun starNoise(index: Int, salt: Float): Float {
        val raw = kotlin.math.sin(index * 12.9898f + salt * 78.233f) * 43758.5453f
        return (raw - kotlin.math.floor(raw)).toFloat()
    }

    private fun drawChromeBackdrop(_palette: NeonPalette) {
        val w = viewport.worldWidth
        val h = viewport.worldHeight

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeBackground
        shapes.rect(0f, 0f, w, h)
        shapes.color = chromeBackgroundLift
        shapes.rect(0f, h * 0.52f, w, h * 0.48f)
        shapes.color = chromeSurfaceRaised.cpy().mul(1f, 1f, 1f, 0.3f)
        shapes.triangle(0f, h, w * 0.52f, h, 0f, h * 0.54f)
        shapes.color = chromeAccentSoft
        shapes.triangle(w * 0.66f, 0f, w, 0f, w, h * 0.24f)
        shapes.color = chromeSurface.cpy().mul(1f, 1f, 1f, 0.18f)
        drawRoundedRect(w * 0.06f, h * 0.08f, w * 0.88f, h * 0.84f, 44f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = chromeStroke.cpy().mul(1f, 1f, 1f, 0.2f)
        shapes.line(0f, h * 0.82f, w * 0.44f, h)
        shapes.line(w * 0.78f, 0f, w, h * 0.2f)
        shapes.end()
    }

    private fun drawIntroBackdrop(palette: NeonPalette) {
        val w = viewport.worldWidth
        val h = viewport.worldHeight

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeBackground
        shapes.rect(0f, 0f, w, h)
        shapes.color = chromeBackgroundLift
        shapes.rect(0f, h * 0.48f, w, h * 0.52f)
        shapes.color = chromeAccentSoft.cpy().mul(1f, 1f, 1f, 0.72f)
        shapes.triangle(0f, h, w * 0.32f, h, 0f, h * 0.74f)
        shapes.color = Color(palette.uiAccent).mul(1f, 1f, 1f, 0.12f)
        shapes.triangle(w, 0f, w, h * 0.22f, w * 0.74f, 0f)
        shapes.end()
    }

    private fun drawArena(palette: NeonPalette) {
        val layout = gameArenaLayout()
        val cx = layout.cx
        val cy = layout.cy
        val arenaRadiusPixels = layout.radius
        val rotation = simulation.arenaRotationRad
        val phaseActive = simulation.phaseGateStatus.active
        val guideSides = guideSidesForLevel(simulation.levelConfig.sectorCount)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        for (obstacle in simulation.obstacles) {
            val alpha = 0.72f + (1.18f - obstacle.radius).coerceIn(0f, 0.7f) * 0.24f
            drawObstacleRing(
                cx = cx,
                cy = cy,
                arenaRadiusPixels = arenaRadiusPixels,
                yScale = layout.yScale,
                obstacle = obstacle,
                arenaRotationRad = rotation,
                contextLevelIndex = simulation.levelConfig.index,
                color = Color(colorForPattern(obstacle.patternId, palette)).mul(1f, 1f, 1f, alpha)
            )
        }

        if (simulation.levelConfig.index in 61..70 || simulation.levelConfig.index in 81..100) {
            val missiles = simulation.missiles
            val step = if (lowPerformanceMode && missiles.size > 18) 2 else 1
            var index = 0
            val enemyEmitters = LinkedHashMap<Int, Pair<Float, Int>>()
            while (index < missiles.size) {
                val missile = missiles[index]
                val absoluteAngle = rotation + missile.angleRad
                val pulse = 0.82f + 0.18f * (0.5f + 0.5f * sin(worldTime * 10.2f + missile.radius * 9.8f))
                if (simulation.levelConfig.index in 81..90 && missile.source == GameSimulation.MissileSource.ENEMY_LASER) {
                    drawEnemyLaserHazard(
                        cx = cx,
                        cy = cy,
                        arenaRadiusPixels = arenaRadiusPixels,
                        yScale = layout.yScale,
                        angleRad = absoluteAngle,
                        radiusNorm = missile.radius,
                        pulse = pulse
                    )
                    val emitterKey = kotlin.math.round((absoluteAngle / 0.22f).toDouble()).toInt()
                    if (!enemyEmitters.containsKey(emitterKey)) {
                        enemyEmitters[emitterKey] = absoluteAngle to missile.emitterVariant
                    }
                } else if (simulation.levelConfig.index in 91..100 && missile.source == GameSimulation.MissileSource.KITCHEN_KNIFE) {
                    drawKnifeHazard(
                        cx = cx,
                        cy = cy,
                        arenaRadiusPixels = arenaRadiusPixels,
                        yScale = layout.yScale,
                        angleRad = absoluteAngle,
                        radiusNorm = missile.radius,
                        pulse = pulse
                    )
                } else {
                    drawMissileHazard(
                        cx = cx,
                        cy = cy,
                        arenaRadiusPixels = arenaRadiusPixels,
                        yScale = layout.yScale,
                        angleRad = absoluteAngle,
                        radiusNorm = missile.radius,
                        pulse = pulse
                    )
                }
                index += step
            }
            if (simulation.levelConfig.index in 81..90) {
                val maxShips = if (lowPerformanceMode) 4 else 6
                var drawn = 0
                for ((angleRad, variant) in enemyEmitters.values) {
                    try {
                        drawEnemyShipEmitter(
                            cx = cx,
                            cy = cy,
                            arenaRadiusPixels = arenaRadiusPixels,
                            yScale = layout.yScale,
                            angleRad = angleRad,
                            palette = palette,
                            variant = variant
                        )
                    } catch (_: Throwable) {
                        // Keep gameplay alive even if a decorative emitter fails on a specific device.
                    }
                    drawn += 1
                    if (drawn >= maxShips) {
                        break
                    }
                }
            }
        }

        drawCoreCluster(cx, cy, arenaRadiusPixels, layout.yScale, guideSides, rotation, phaseActive)
        shapes.end()

        drawNeedle(cx, cy, arenaRadiusPixels, layout.yScale, visualPlayerAngleRad, palette, phaseActive)
    }

    private fun colorForPattern(patternId: String, palette: NeonPalette): Color {
        val baseColor = if (simulation.levelConfig.index in 61..70) {
            when (patternId) {
                "missile_volley" -> Color(0.98f, 0.45f, 0.26f, 1f)
                "gravity_pull" -> Color(0.92f, 0.56f, 0.3f, 1f)
                "time_bubble" -> Color(0.78f, 0.74f, 0.58f, 1f)
                else -> Color(0.54f, 0.56f, 0.6f, 1f)
            }
        } else if (simulation.levelConfig.index in 81..90) {
            when (patternId) {
                "gravity_pull" -> Color(0.46f, 0.82f, 1f, 1f)
                "time_bubble" -> Color(0.58f, 0.72f, 1f, 1f)
                "dense_blades", "final_crush" -> Color(0.34f, 0.64f, 1f, 1f)
                else -> Color(0.24f, 0.52f, 0.92f, 1f)
            }
        } else if (simulation.levelConfig.index in 91..100) {
            when (patternId) {
                "gravity_pull" -> Color(0.9f, 0.72f, 0.36f, 1f)
                "time_bubble" -> Color(0.88f, 0.58f, 0.3f, 1f)
                "dense_blades", "final_crush" -> Color(0.74f, 0.42f, 0.18f, 1f)
                else -> Color(0.82f, 0.54f, 0.24f, 1f)
            }
        } else if (simulation.levelConfig.index in 71..80) {
            when (patternId) {
                "gravity_pull" -> Color(0.44f, 0.92f, 0.78f, 1f)
                "time_bubble" -> Color(0.62f, 0.74f, 1f, 1f)
                "dense_blades", "final_crush" -> Color(0.88f, 0.28f, 0.58f, 1f)
                else -> Color(0.74f, 0.22f, 0.62f, 1f)
            }
        } else if (simulation.levelConfig.index in 21..30) {
            when (patternId) {
                "gravity_pull" -> Color(0.22f, 0.78f, 0.34f, 1f)
                "time_bubble" -> Color(0.22f, 0.62f, 0.98f, 1f)
                else -> Color(0.16f, 0.56f, 0.28f, 1f)
            }
        } else {
            val baseDanger = Color(reactorDanger)
            val wideDanger = lerpColor(baseDanger, Color(1f, 0.78f, 0.36f, 1f), 0.28f)
            val heavyDanger = lerpColor(baseDanger, Color(1f, 0.42f, 0.2f, 1f), 0.26f)
            val gravityTint = Color(0.58f, 0.82f, 1f, 1f)
            val timeBubbleTint = Color(0.72f, 0.78f, 1f, 1f)
            val tierShift = lerpColor(baseDanger, palette.obstacleWide, 0.08f)
            when (patternId) {
                "missile_volley" -> Color(0.98f, 0.42f, 0.22f, 1f)
                "dense_blades", "final_crush" -> heavyDanger
                "wide_ring", "pulse_ring" -> wideDanger
                "gravity_pull" -> gravityTint
                "time_bubble" -> timeBubbleTint
                "drift_gap", "split_lane", "needle_window", "tight_teeth" -> lerpColor(baseDanger, tierShift, 0.36f)
                else -> tierShift
            }
        }
        val flashMix = hazardInvertFlashMix(simulation.levelConfig.index)
        return applyColorInvertMix(baseColor, flashMix)
    }

    private fun applyColorInvertMix(color: Color, mix: Float): Color {
        if (mix <= 0f) {
            return color
        }
        val clamped = mix.coerceIn(0f, 1f)
        return Color(
            MathUtils.lerp(color.r, 1f - color.r, clamped),
            MathUtils.lerp(color.g, 1f - color.g, clamped),
            MathUtils.lerp(color.b, 1f - color.b, clamped),
            color.a
        )
    }

    private fun hazardInvertFlashMix(levelIndex: Int): Float {
        val runActive = overlayMode == OverlayMode.GAME &&
            (simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING)
        if (!runActive || levelIndex !in 51..60) {
            return 0f
        }
        val cycleSeconds = 2.35f
        val burstSeconds = 0.26f
        val local = worldTime % cycleSeconds
        if (local > burstSeconds) {
            return 0f
        }
        val t = (local / burstSeconds).coerceIn(0f, 1f)
        val wave = sin(t * MathUtils.PI).coerceIn(0f, 1f)
        return (0.86f * wave).coerceIn(0f, 0.9f)
    }

    private fun lerpColor(from: Color, to: Color, t: Float): Color {
        val p = t.coerceIn(0f, 1f)
        return Color(
            from.r + (to.r - from.r) * p,
            from.g + (to.g - from.g) * p,
            from.b + (to.b - from.b) * p,
            1f
        )
    }

    private fun drawNeedle(
        cx: Float,
        cy: Float,
        arenaRadiusPixels: Float,
        yScale: Float,
        angle: Float,
        palette: NeonPalette,
        phaseActive: Boolean
    ) {
        val shipRadius = simulation.playerOrbitRadiusNormalized * arenaRadiusPixels
        val dirX = cos(angle)
        val dirY = sin(angle)
        val shipX = cx + dirX * shipRadius
        val shipY = cy + dirY * shipRadius * yScale

        val shipId = activeShipSkin()?.id ?: "specter_7"
        val shipStyleIndex = shipStyleIndexForId(shipId)
        val manualOffsetDeg = shipRotationOverrides[shipId] ?: 0f
        val levelIndex = simulation.levelConfig.index
        val angleRad = angle + manualOffsetDeg * MathUtils.degreesToRadians
        val shieldVisualActive = simulation.shieldActive || (simulation.runPhase == RunPhase.READY && shieldArmedForRun)
        if (levelIndex in 21..30) {
            drawFlyModel(
                centerX = shipX,
                centerY = shipY,
                angleRad = angleRad,
                yScale = yScale,
                scale = (arenaRadiusPixels * 0.00502f).coerceIn(0.48f, 1.85f)
            )
            if (shieldVisualActive) {
                drawShipShieldBubble(shipX, shipY, arenaRadiusPixels, yScale)
            }
            return
        }
        if (levelIndex in 61..70) {
            drawJetModel(
                centerX = shipX,
                centerY = shipY,
                angleRad = angleRad,
                yScale = yScale,
                scale = (arenaRadiusPixels * 0.00205f).coerceIn(0.2f, 0.62f),
                f35Style = levelIndex % 2 == 0
            )
            if (shieldVisualActive) {
                drawShipShieldBubble(shipX, shipY, arenaRadiusPixels, yScale)
            }
            return
        }
        if (levelIndex in 71..80) {
            drawVirusModel(
                centerX = shipX,
                centerY = shipY,
                angleRad = angleRad,
                yScale = yScale,
                scale = (arenaRadiusPixels * 0.0022f).coerceIn(0.24f, 0.7f)
            )
            if (shieldVisualActive) {
                drawShipShieldBubble(shipX, shipY, arenaRadiusPixels, yScale)
            }
            return
        }
        if (levelIndex in 91..100) {
            drawAppleModel(
                centerX = shipX,
                centerY = shipY,
                angleRad = angleRad,
                yScale = yScale,
                scale = (arenaRadiusPixels * 0.0058f).coerceIn(0.72f, 1.42f),
                levelIndex = levelIndex
            )
            if (shieldVisualActive) {
                drawShipShieldBubble(shipX, shipY, arenaRadiusPixels, yScale)
            }
            return
        }
        val miniMultiplier = if (levelIndex in 31..40) 0.62f else 1f
        // On the non-themed level ranges the flown ship is the ship chosen in the store,
        // drawn from its ShipArt texture (themed ranges above keep their special models).
        val storeShipTex = activeShipSkin()?.texture
        if (storeShipTex != null) {
            val shipLen = (arenaRadiusPixels * 0.2f * miniMultiplier).coerceIn(sx(30f), sx(120f))
            drawShipThruster(shipX, shipY, angleRad, shipLen, shipStyleIndex)
            drawShipSprite(storeShipTex, shipX, shipY, shipLen, angleRad * MathUtils.radiansToDegrees - 90f)
        } else {
            drawCodeShipModel(
                centerX = shipX,
                centerY = shipY,
                angleRad = angleRad,
                yScale = yScale,
                modelScale = (arenaRadiusPixels * 0.00132f * miniMultiplier).coerceIn(0.15f, 0.42f),
                levelIndex = levelIndex,
                palette = palette,
                phaseActive = phaseActive,
                dimmed = false,
                shipStyleIndex = shipStyleIndex
            )
        }
        if (shieldVisualActive) {
            drawShipShieldBubble(shipX, shipY, arenaRadiusPixels, yScale)
        }
    }

    private fun drawShipThruster(cx: Float, cy: Float, angleRad: Float, lengthPx: Float, styleIndex: Int) {
        // Animated exhaust flame from the tail (opposite the nose), tinted per ship so each
        // one has its own trail. Drawn before the hull sprite so it sits behind the ship.
        val dirX = cos(angleRad)
        val dirY = sin(angleRad)
        val perpX = -dirY
        val perpY = dirX
        val tailX = cx - dirX * lengthPx * 0.40f
        val tailY = cy - dirY * lengthPx * 0.40f
        val flicker = 0.72f + 0.22f * sin(worldTime * 27f + cx * 0.05f) + 0.1f * sin(worldTime * 61f)
        val flameLen = lengthPx * 0.6f * flicker
        val baseHalf = lengthPx * 0.17f
        val tint = shipTierCoreNeon[styleIndex.coerceAtLeast(0) % shipTierCoreNeon.size]

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // outer warm plume
        shapes.color = Color(1f, 0.5f, 0.16f, 0.62f)
        shapes.triangle(
            tailX + perpX * baseHalf, tailY + perpY * baseHalf,
            tailX - perpX * baseHalf, tailY - perpY * baseHalf,
            tailX - dirX * flameLen, tailY - dirY * flameLen
        )
        // mid flame tinted by the ship colour
        shapes.color = Color(tint.r, tint.g, tint.b, 0.8f)
        shapes.triangle(
            tailX + perpX * baseHalf * 0.62f, tailY + perpY * baseHalf * 0.62f,
            tailX - perpX * baseHalf * 0.62f, tailY - perpY * baseHalf * 0.62f,
            tailX - dirX * flameLen * 0.72f, tailY - dirY * flameLen * 0.72f
        )
        // hot white core
        shapes.color = Color(1f, 1f, 1f, 0.9f)
        shapes.triangle(
            tailX + perpX * baseHalf * 0.3f, tailY + perpY * baseHalf * 0.3f,
            tailX - perpX * baseHalf * 0.3f, tailY - perpY * baseHalf * 0.3f,
            tailX - dirX * flameLen * 0.42f, tailY - dirY * flameLen * 0.42f
        )
        shapes.end()
    }

    private fun drawShipSprite(tex: Texture, cx: Float, cy: Float, lengthPx: Float, rotationDeg: Float) {
        val drawH = lengthPx
        val drawW = lengthPx * (tex.width.toFloat() / tex.height.toFloat())
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(
            tex,
            cx - drawW * 0.5f,
            cy - drawH * 0.5f,
            drawW * 0.5f,
            drawH * 0.5f,
            drawW,
            drawH,
            1f,
            1f,
            rotationDeg,
            0,
            0,
            tex.width,
            tex.height,
            false,
            false
        )
        batch.end()
    }

    private fun drawShipShieldBubble(centerX: Float, centerY: Float, arenaRadiusPixels: Float, yScale: Float) {
        val radius = (arenaRadiusPixels * 0.086f).coerceIn(sx(20f), sx(48f))
        val highlightRadius = radius * 0.72f
        val pulse = 0.88f + 0.12f * (0.5f + 0.5f * sin(worldTime * 5.2f))
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.58f, 0.88f, 1f, 0.24f * pulse)
        shapes.ellipse(centerX - radius, centerY - radius * yScale, radius * 2f, radius * 2f * yScale, 56)
        shapes.color = Color(0.76f, 0.94f, 1f, 0.28f * pulse)
        shapes.ellipse(
            centerX - highlightRadius * 0.9f,
            centerY - highlightRadius * yScale * 1.02f,
            highlightRadius * 1.8f,
            highlightRadius * 2f * yScale,
            48
        )
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(0.84f, 0.97f, 1f, 0.86f * pulse)
        shapes.ellipse(centerX - radius, centerY - radius * yScale, radius * 2f, radius * 2f * yScale, 64)
        shapes.end()
    }

    private fun drawJetModel(
        centerX: Float,
        centerY: Float,
        angleRad: Float,
        yScale: Float,
        scale: Float,
        f35Style: Boolean
    ) {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        fun tx(localX: Float, localY: Float): Float {
            val rx = localX * scale
            val ry = localY * scale
            return centerX + (rx * cosA - ry * sinA)
        }
        fun ty(localX: Float, localY: Float): Float {
            val rx = localX * scale
            val ry = localY * scale
            return centerY + (rx * sinA + ry * cosA) * yScale
        }

        val hull = if (f35Style) Color(0.62f, 0.67f, 0.73f, 1f) else Color(0.54f, 0.6f, 0.67f, 1f)
        val panel = if (f35Style) Color(0.3f, 0.34f, 0.38f, 1f) else Color(0.26f, 0.3f, 0.35f, 1f)
        val neon = if (f35Style) Color(0.32f, 0.9f, 1f, 1f) else Color(0.26f, 0.78f, 0.98f, 1f)
        val afterburner = Color(1f, 0.56f, 0.28f, 0.96f)
        val wingReach = if (f35Style) 33f else 39f
        val tailWidth = if (f35Style) 20f else 24f

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = hull
        shapes.triangle(
            tx(58f, 0f), ty(58f, 0f),
            tx(-34f, 20f), ty(-34f, 20f),
            tx(-34f, -20f), ty(-34f, -20f)
        )
        shapes.color = panel
        shapes.triangle(
            tx(36f, 0f), ty(36f, 0f),
            tx(-16f, 10f), ty(-16f, 10f),
            tx(-16f, -10f), ty(-16f, -10f)
        )
        shapes.color = hull.cpy().mul(1f, 1f, 1f, 0.95f)
        shapes.triangle(
            tx(8f, 14f), ty(8f, 14f),
            tx(-44f, wingReach), ty(-44f, wingReach),
            tx(-18f, 12f), ty(-18f, 12f)
        )
        shapes.triangle(
            tx(8f, -14f), ty(8f, -14f),
            tx(-44f, -wingReach), ty(-44f, -wingReach),
            tx(-18f, -12f), ty(-18f, -12f)
        )
        shapes.color = panel.cpy().mul(1f, 1f, 1f, 0.96f)
        shapes.triangle(
            tx(-20f, 16f), ty(-20f, 16f),
            tx(-58f, tailWidth), ty(-58f, tailWidth),
            tx(-52f, 6f), ty(-52f, 6f)
        )
        shapes.triangle(
            tx(-20f, -16f), ty(-20f, -16f),
            tx(-58f, -tailWidth), ty(-58f, -tailWidth),
            tx(-52f, -6f), ty(-52f, -6f)
        )
        shapes.color = neon.cpy().mul(1f, 1f, 1f, 0.9f)
        shapes.triangle(
            tx(20f, 0f), ty(20f, 0f),
            tx(-6f, 4.5f), ty(-6f, 4.5f),
            tx(-6f, -4.5f), ty(-6f, -4.5f)
        )
        shapes.color = afterburner
        shapes.circle(tx(-58f, 0f), ty(-58f, 0f), 4.2f * scale, 18)
        shapes.color = Color(1f, 0.78f, 0.52f, 0.92f)
        shapes.circle(tx(-58f, 0f), ty(-58f, 0f), 2.4f * scale, 16)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = neon.cpy().mul(1f, 1f, 1f, 0.84f)
        shapes.rectLine(tx(56f, 0f), ty(56f, 0f), tx(-56f, 0f), ty(-56f, 0f), (1.4f * scale).coerceAtLeast(0.8f))
        shapes.rectLine(tx(10f, 12f), ty(10f, 12f), tx(-42f, wingReach), ty(-42f, wingReach), (1.2f * scale).coerceAtLeast(0.7f))
        shapes.rectLine(tx(10f, -12f), ty(10f, -12f), tx(-42f, -wingReach), ty(-42f, -wingReach), (1.2f * scale).coerceAtLeast(0.7f))
        shapes.end()
    }

    private fun drawFlyModel(
        centerX: Float,
        centerY: Float,
        angleRad: Float,
        yScale: Float,
        scale: Float
    ) {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        fun tx(localX: Float, localY: Float): Float {
            val rx = localX * scale
            val ry = localY * scale
            return centerX + (rx * cosA - ry * sinA)
        }
        fun ty(localX: Float, localY: Float): Float {
            val rx = localX * scale
            val ry = localY * scale
            return centerY + (rx * sinA + ry * cosA) * yScale
        }

        val wingBeat = 0.86f + 0.2f * sin(worldTime * 28f)
        val wingAlpha = (0.4f + 0.2f * sin(worldTime * 23f)).coerceIn(0.3f, 0.66f)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.1f, 0.1f, 0.12f, 0.92f)
        shapes.circle(tx(0f, 0f), ty(0f, 0f), 13f * scale, 26)
        shapes.circle(tx(-13f, 0f), ty(-13f, 0f), 8f * scale, 22)
        shapes.color = Color(0.2f, 0.74f, 0.52f, wingAlpha)
        shapes.triangle(
            tx(-4f, 8f), ty(-4f, 8f),
            tx(-26f, 20f * wingBeat), ty(-26f, 20f * wingBeat),
            tx(-26f, -2f * wingBeat), ty(-26f, -2f * wingBeat)
        )
        shapes.triangle(
            tx(-4f, -8f), ty(-4f, -8f),
            tx(-26f, -20f * wingBeat), ty(-26f, -20f * wingBeat),
            tx(-26f, 2f * wingBeat), ty(-26f, 2f * wingBeat)
        )
        shapes.color = Color(0.98f, 0.35f, 0.24f, 1f)
        shapes.circle(tx(10f, 0f), ty(10f, 0f), 3.8f * scale, 16)
        shapes.color = Color(0.86f, 0.94f, 0.99f, 1f)
        shapes.circle(tx(-13f, 2.6f), ty(-13f, 2.6f), 1.6f * scale, 12)
        shapes.circle(tx(-13f, -2.6f), ty(-13f, -2.6f), 1.6f * scale, 12)
        shapes.end()
    }

    private fun drawVirusModel(
        centerX: Float,
        centerY: Float,
        angleRad: Float,
        yScale: Float,
        scale: Float
    ) {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        fun tx(localX: Float, localY: Float): Float {
            val rx = localX * scale
            val ry = localY * scale
            return centerX + (rx * cosA - ry * sinA)
        }
        fun ty(localX: Float, localY: Float): Float {
            val rx = localX * scale
            val ry = localY * scale
            return centerY + (rx * sinA + ry * cosA) * yScale
        }

        val pulse = 0.84f + 0.16f * sin(worldTime * 6.1f)
        val core = Color(0.78f, 0.22f, 0.62f, 1f)
        val shell = Color(0.34f, 0.08f, 0.38f, 1f)
        val tip = Color(0.56f, 0.94f, 0.8f, 0.96f)
        val drift = worldTime * 5.2f

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (trail in 0 until 4) {
            val t = trail / 4f
            val trailRadius = 13f * scale * (1f - t * 0.22f)
            val offset = (24f + trail * 10f) * scale
            val txTrail = centerX - cosA * offset
            val tyTrail = centerY - sinA * offset * yScale
            shapes.color = Color(0.4f, 0.88f, 0.82f, (0.16f - t * 0.03f).coerceAtLeast(0.05f))
            shapes.circle(
                txTrail + sin(drift + trail) * 2.2f * scale,
                tyTrail + cos(drift + trail * 0.9f) * 1.8f * scale * yScale,
                trailRadius,
                20
            )
        }
        shapes.color = shell
        shapes.circle(tx(0f, 0f), ty(0f, 0f), 13f * scale, 26)
        shapes.color = core
        shapes.circle(tx(0f, 0f), ty(0f, 0f), 8.5f * scale, 24)
        val armLen = 28f + pulse * 5f
        val armWidth = 7.6f
        for (branch in 0 until 8) {
            val armAngle = branch * (MathUtils.PI2 / 8f) + worldTime * 0.16f
            val ax = cos(armAngle) * 10f
            val ay = sin(armAngle) * 10f
            val bx = cos(armAngle) * armLen
            val by = sin(armAngle) * armLen
            val nx = -sin(armAngle) * armWidth
            val ny = cos(armAngle) * armWidth
            shapes.color = shell.cpy().mul(1f, 1f, 1f, 0.95f)
            shapes.triangle(
                tx(ax + nx, ay + ny), ty(ax + nx, ay + ny),
                tx(ax - nx, ay - ny), ty(ax - nx, ay - ny),
                tx(bx, by), ty(bx, by)
            )
            shapes.color = tip
            shapes.circle(tx(bx, by), ty(bx, by), 4.2f * scale, 14)
        }
        shapes.end()
    }

    private fun drawAppleModel(
        centerX: Float,
        centerY: Float,
        angleRad: Float,
        yScale: Float,
        scale: Float,
        levelIndex: Int
    ) {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        fun tx(localX: Float, localY: Float): Float {
            val rx = localX * scale
            val ry = localY * scale
            return centerX + (rx * cosA - ry * sinA)
        }
        fun ty(localX: Float, localY: Float): Float {
            val rx = localX * scale
            val ry = localY * scale
            return centerY + (rx * sinA + ry * cosA) * yScale
        }

        val wobble = 0.94f + 0.06f * sin(worldTime * 4.6f)
        val visibilityBoost = ((levelIndex - 91).coerceAtLeast(0) / 9f).coerceIn(0f, 1f)
        val outlineBoost = 1f + visibilityBoost * 0.28f
        val highlightScale = 1f + visibilityBoost * 0.18f
        val bodySegments = qualitySegments(48, minimum = 28)
        val bodyVertices = FloatArray(bodySegments * 2)
        for (index in 0 until bodySegments) {
            val t = MathUtils.PI2 * (index / bodySegments.toFloat())
            val radiusX = 13.4f * (1f + 0.1f * sin(t).coerceAtLeast(0f))
            val radiusY = 13.1f * (1f - 0.06f * cos(t))
            val localX = cos(t) * radiusX
            val localY = sin(t) * radiusY - 1.5f
            bodyVertices[index * 2] = tx(localX, localY)
            bodyVertices[index * 2 + 1] = ty(localX, localY)
        }
        val outlineVertices = FloatArray(bodyVertices.size + 2)
        bodyVertices.copyInto(outlineVertices)
        outlineVertices[outlineVertices.size - 2] = bodyVertices[0]
        outlineVertices[outlineVertices.size - 1] = bodyVertices[1]

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val appleRed = Color(0.9f + visibilityBoost * 0.08f, 0.2f + visibilityBoost * 0.08f, 0.16f + visibilityBoost * 0.05f, 0.98f)
        val appleDark = Color(0.62f, 0.1f, 0.1f, 0.94f)
        val appleGlow = Color(1f, 0.56f + visibilityBoost * 0.14f, 0.48f + visibilityBoost * 0.12f, 0.5f + visibilityBoost * 0.26f)
        val outerGlow = Color(1f, 0.84f, 0.62f, 0.12f + visibilityBoost * 0.16f)
        val leafGreen = Color(0.28f, 0.68f + visibilityBoost * 0.08f, 0.28f, 0.96f)

        shapes.color = outerGlow
        shapes.circle(centerX, centerY, 19.8f * scale * outlineBoost, 30)

        shapes.color = appleRed
        drawConvexPolygon(bodyVertices)

        shapes.color = appleDark
        shapes.circle(tx(-5.8f, -2.8f), ty(-5.8f, -2.8f), 4.1f * scale * wobble * highlightScale, 18)
        shapes.circle(tx(6.2f, -2.2f), ty(6.2f, -2.2f), 3.6f * scale * wobble * highlightScale, 16)

        shapes.color = appleGlow
        shapes.circle(tx(4.2f, 6.4f), ty(4.2f, 6.4f), 4.2f * scale * wobble * highlightScale, 16)
        shapes.circle(tx(-2.2f, 7.6f), ty(-2.2f, 7.6f), 2.6f * scale * wobble * highlightScale, 14)
        shapes.circle(tx(0.8f, 4.4f), ty(0.8f, 4.4f), 8.2f * scale * (0.82f + visibilityBoost * 0.22f), 18)

        shapes.color = Color(0.36f, 0.22f, 0.1f, 0.96f)
        shapes.rectLine(tx(0f, 9f), ty(0f, 9f), tx(1.4f, 16f), ty(1.4f, 16f), (2.2f * scale * outlineBoost).coerceAtLeast(1.2f))
        shapes.color = leafGreen
        shapes.triangle(
            tx(1.6f, 12.4f), ty(1.6f, 12.4f),
            tx(10.8f, 14f), ty(10.8f, 14f),
            tx(4.8f, 18.2f), ty(4.8f, 18.2f)
        )
        shapes.color = Color(1f, 0.98f, 0.9f, 0.1f + visibilityBoost * 0.16f)
        shapes.circle(tx(-1.2f, 3.2f), ty(-1.2f, 3.2f), 13.6f * scale * outlineBoost, 22)
        shapes.end()

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(0.44f + visibilityBoost * 0.18f, 0.08f + visibilityBoost * 0.06f, 0.08f + visibilityBoost * 0.05f, 0.9f)
        shapes.polyline(outlineVertices)
        if (visibilityBoost > 0f) {
            shapes.color = Color(1f, 0.9f, 0.78f, 0.24f + visibilityBoost * 0.22f)
            shapes.circle(centerX, centerY, 18.3f * scale * outlineBoost, 28)
        }
        shapes.color = Color(0.82f, 1f, 0.82f, 0.7f)
        shapes.triangle(
            tx(1.6f, 12.4f), ty(1.6f, 12.4f),
            tx(10.8f, 14f), ty(10.8f, 14f),
            tx(4.8f, 18.2f), ty(4.8f, 18.2f)
        )
        shapes.end()
    }

    private fun drawShipPreviewIcon(
        rect: UiRect,
        palette: NeonPalette,
        rotationDeg: Float,
        dimmed: Boolean,
        levelIndex: Int = (selectedLevelIndex + 1),
        shipStyleIndex: Int = 0
    ) {
        val scale = (minOf(rect.width, rect.height) / 180f).coerceIn(0.26f, 0.54f)
        drawCodeShipModel(
            centerX = rect.x + rect.width * 0.5f,
            centerY = rect.y + rect.height * 0.52f,
            angleRad = rotationDeg * MathUtils.degreesToRadians,
            yScale = 1f,
            modelScale = scale,
            levelIndex = levelIndex,
            palette = palette,
            phaseActive = false,
            dimmed = dimmed,
            shipStyleIndex = shipStyleIndex
        )
    }

    private fun drawShipPreviewTexture(
        skin: ShipSkin?,
        rect: UiRect,
        palette: NeonPalette,
        rotationDeg: Float,
        dimmed: Boolean
    ) {
        val tex = skin?.texture
        if (tex != null) {
            // Draw the actual ShipArt hull, horizontal (nose right), fit into the card so
            // each ship's distinct silhouette is readable. Falls back to the vector icon
            // only if a skin somehow has no texture.
            // Contain the ship fully inside the card with a small margin (was overflowing).
            val aspect = tex.width.toFloat() / tex.height.toFloat()
            var drawH = rect.height * 0.94f
            var drawW = drawH * aspect
            if (drawW > rect.width * 0.94f) {
                drawW = rect.width * 0.94f
                drawH = drawW / aspect
            }
            val cx = rect.x + rect.width * 0.5f
            val cy = rect.y + rect.height * 0.5f
            batch.projectionMatrix = camera.combined
            batch.begin()
            if (dimmed) batch.setColor(1f, 1f, 1f, 0.42f) else batch.setColor(1f, 1f, 1f, 1f)
            batch.draw(
                tex,
                cx - drawW * 0.5f,
                cy - drawH * 0.5f,
                drawW * 0.5f,
                drawH * 0.5f,
                drawW,
                drawH,
                1f,
                1f,
                0f,
                0,
                0,
                tex.width,
                tex.height,
                false,
                false
            )
            batch.setColor(1f, 1f, 1f, 1f)
            batch.end()
            return
        }
        val styleIndex = shipStyleIndexForSkin(skin)
        drawShipPreviewIcon(
            rect = rect,
            palette = palette,
            rotationDeg = rotationDeg,
            dimmed = dimmed,
            levelIndex = selectedLevelIndex + 1,
            shipStyleIndex = styleIndex
        )
    }

    private fun shipStyleIndexForSkin(skin: ShipSkin?): Int {
        return shipStyleIndexForId(skin?.id)
    }

    private fun shipStyleIndexForId(shipId: String?): Int {
        if (shipId.isNullOrBlank()) {
            return 0
        }
        val index = shipSkins.indexOfFirst { it.id == shipId }
        return if (index >= 0) index else 0
    }

    private fun shipHullTemplate(styleIndex: Int): FloatArray {
        return when (styleIndex.mod(11)) {
            0 -> floatArrayOf(
                62f, 0f, 18f, 12f, 18f, -12f, 8f, 8f, 8f, -8f, -36f, 0f, 14f, 20f, 14f, -20f, -20f, 28f, -20f, -28f, -82f, 34f, -82f, -34f, -52f, 12f, -52f, -12f, -30f, 34f, -30f, -34f, -14f, 18f, -14f, -18f, -10f, 0f, -12f, 20f, -12f, -20f
            )
            1 -> floatArrayOf(
                58f, 0f, 22f, 14f, 22f, -14f, 12f, 10f, 12f, -10f, -28f, 0f, 20f, 30f, 20f, -30f, -4f, 42f, -4f, -42f, -46f, 44f, -46f, -44f, -34f, 20f, -34f, -20f, -22f, 22f, -22f, -22f, -12f, 14f, -12f, -14f, -4f, 0f, -6f, 16f, -6f, -16f
            )
            2 -> floatArrayOf(
                52f, 0f, 24f, 10f, 24f, -10f, 16f, 6f, 16f, -6f, -20f, 0f, 18f, 24f, 18f, -24f, 2f, 30f, 2f, -30f, -44f, 30f, -44f, -30f, -44f, 18f, -44f, -18f, -18f, 30f, -18f, -30f, -10f, 16f, -10f, -16f, 0f, 0f, -2f, 16f, -2f, -16f
            )
            3 -> floatArrayOf(
                56f, 0f, 18f, 16f, 18f, -16f, 8f, 12f, 8f, -12f, -26f, 0f, 12f, 26f, 12f, -26f, -10f, 34f, -10f, -34f, -52f, 38f, -52f, -38f, -38f, 22f, -38f, -22f, -14f, 26f, -14f, -26f, -8f, 16f, -8f, -16f, -2f, 0f, -4f, 14f, -4f, -14f
            )
            4 -> floatArrayOf(
                60f, 0f, 20f, 12f, 20f, -12f, 10f, 8f, 10f, -8f, -32f, 0f, 18f, 18f, 18f, -18f, -8f, 24f, -8f, -24f, -64f, 28f, -64f, -28f, -48f, 12f, -48f, -12f, -22f, 30f, -22f, -30f, -14f, 18f, -14f, -18f, -6f, 0f, -8f, 16f, -8f, -16f
            )
            5 -> floatArrayOf(
                54f, 0f, 16f, 10f, 16f, -10f, 8f, 6f, 8f, -6f, -18f, 0f, 8f, 24f, 8f, -24f, -38f, 36f, -38f, -36f, -64f, 44f, -64f, -44f, -38f, 18f, -38f, -18f, -8f, 34f, -8f, -34f, -6f, 16f, -6f, -16f, -2f, 0f, -3f, 14f, -3f, -14f
            )
            6 -> floatArrayOf(
                66f, 0f, 14f, 9f, 14f, -9f, 4f, 6f, 4f, -6f, -42f, 0f, 4f, 14f, 4f, -14f, -26f, 18f, -26f, -18f, -98f, 20f, -98f, -20f, -70f, 8f, -70f, -8f, -18f, 28f, -18f, -28f, -10f, 14f, -10f, -14f, -8f, 0f, -9f, 14f, -9f, -14f
            )
            7 -> floatArrayOf(
                50f, 0f, 20f, 14f, 20f, -14f, 14f, 10f, 14f, -10f, -18f, 0f, 20f, 26f, 20f, -26f, 8f, 34f, 8f, -34f, -36f, 36f, -36f, -36f, -30f, 20f, -30f, -20f, -24f, 18f, -24f, -18f, -12f, 12f, -12f, -12f, -2f, 0f, -3f, 12f, -3f, -12f
            )
            8 -> floatArrayOf(
                58f, 0f, 18f, 11f, 18f, -11f, 10f, 8f, 10f, -8f, -26f, 0f, 14f, 18f, 14f, -18f, -20f, 28f, -20f, -28f, -70f, 34f, -70f, -34f, -46f, 12f, -46f, -12f, -16f, 36f, -16f, -36f, -12f, 18f, -12f, -18f, -6f, 0f, -8f, 18f, -8f, -18f
            )
            9 -> floatArrayOf(
                48f, 0f, 18f, 16f, 18f, -16f, 10f, 12f, 10f, -12f, -22f, 0f, 10f, 28f, 10f, -28f, -18f, 34f, -18f, -34f, -46f, 34f, -46f, -34f, -34f, 20f, -34f, -20f, -10f, 24f, -10f, -24f, -6f, 14f, -6f, -14f, -1f, 0f, -2f, 12f, -2f, -12f
            )
            else -> floatArrayOf(
                57f, 0f, 19f, 12f, 19f, -12f, 10f, 7f, 10f, -7f, -30f, 0f, 16f, 22f, 16f, -22f, -6f, 30f, -6f, -30f, -58f, 32f, -58f, -32f, -44f, 14f, -44f, -14f, -20f, 30f, -20f, -30f, -12f, 16f, -12f, -16f, -5f, 0f, -7f, 16f, -7f, -16f
            )
        }
    }

    private fun drawCodeShipModel(
        centerX: Float,
        centerY: Float,
        angleRad: Float,
        yScale: Float,
        modelScale: Float,
        levelIndex: Int,
        palette: NeonPalette,
        phaseActive: Boolean,
        dimmed: Boolean,
        shipStyleIndex: Int
    ) {
        fun setPoint(index: Int, localX: Float, localY: Float, cosA: Float, sinA: Float) {
            val bufferIndex = index * 2
            val rx = localX * modelScale
            val ry = localY * modelScale
            shipPoints[bufferIndex] = centerX + (rx * cosA - ry * sinA)
            shipPoints[bufferIndex + 1] = centerY + (rx * sinA + ry * cosA) * yScale
        }

        fun px(index: Int): Float = shipPoints[index * 2]
        fun py(index: Int): Float = shipPoints[index * 2 + 1]

        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        val styleVariant = shipStyleIndex.mod(11)
        val hull = shipHullTemplate(shipStyleIndex)
        for (pointIndex in 0..20) {
            val offset = pointIndex * 2
            setPoint(pointIndex, hull[offset], hull[offset + 1], cosA, sinA)
        }

        val tierIndex = shipStyleIndex.coerceAtLeast(0) % shipTierCoreNeon.size
        val tierCore = shipTierCoreNeon[tierIndex]
        val tierOutline = shipTierOutlineNeon[tierIndex]
        val pulse = (0.62f + sin(worldTime * 7f + tierIndex * 0.84f) * 0.22f + motionSpeedIntensity * 0.16f).coerceIn(0.45f, 1.08f)
        val speedMix = motionSpeedIntensity.coerceIn(0f, 1.35f)
        val flashMix = hazardInvertFlashMix(levelIndex)
        fun flashChannel(value: Float): Float {
            if (flashMix <= 0f) {
                return value
            }
            return MathUtils.lerp(value, 1f - value, flashMix).coerceIn(0f, 1f)
        }

        val hullAlpha = if (dimmed) 0.82f else 0.97f
        val edgeAlpha = if (dimmed) 0.58f else 0.94f
        val coreAlpha = if (dimmed) 0.86f else 0.99f
        colorScratchA.set(0.12f, 0.16f, 0.22f, hullAlpha)
        colorScratchA.lerp(tierCore, if (dimmed) 0.12f else 0.16f + speedMix * 0.04f)
        val hullDeepR = colorScratchA.r
        val hullDeepG = colorScratchA.g
        val hullDeepB = colorScratchA.b
        val hullDeepA = colorScratchA.a
        colorScratchB.set(0.34f, 0.39f, 0.49f, hullAlpha)
        colorScratchB.lerp(tierCore, if (dimmed) 0.2f else 0.34f + speedMix * 0.06f)
        val hullMidR = colorScratchB.r
        val hullMidG = colorScratchB.g
        val hullMidB = colorScratchB.b
        val hullMidA = colorScratchB.a
        colorScratchA.set(tierCore).lerp(Color.WHITE, if (dimmed) 0.08f else 0.16f + pulse * 0.1f)
        colorScratchA.a = coreAlpha
        val coreR = colorScratchA.r
        val coreG = colorScratchA.g
        val coreB = colorScratchA.b
        val coreA = colorScratchA.a
        colorScratchB.set(tierCore).lerp(tierOutline, 0.36f).lerp(Color.WHITE, 0.16f + pulse * 0.12f)
        colorScratchB.a = coreAlpha
        val coreAccentR = colorScratchB.r
        val coreAccentG = colorScratchB.g
        val coreAccentB = colorScratchB.b
        val coreAccentA = colorScratchB.a
        colorScratchA.set(1f, 0.32f, 0.3f, if (dimmed) 0.72f else 0.96f).lerp(tierOutline, 0.24f)
        val engineR = colorScratchA.r
        val engineG = colorScratchA.g
        val engineB = colorScratchA.b
        val engineA = colorScratchA.a
        colorScratchB.set(tierOutline).lerp(palette.uiAccent, 0.22f).lerp(Color.WHITE, 0.08f + pulse * 0.12f)
        if (phaseActive) {
            colorScratchB.lerp(palette.needlePhase, 0.4f)
        }
        colorScratchB.a = edgeAlpha
        val edgeR = colorScratchB.r
        val edgeG = colorScratchB.g
        val edgeB = colorScratchB.b
        val edgeA = colorScratchB.a
        val hullDeepRf = flashChannel(hullDeepR)
        val hullDeepGf = flashChannel(hullDeepG)
        val hullDeepBf = flashChannel(hullDeepB)
        val hullMidRf = flashChannel(hullMidR)
        val hullMidGf = flashChannel(hullMidG)
        val hullMidBf = flashChannel(hullMidB)
        val coreRf = flashChannel(coreR)
        val coreGf = flashChannel(coreG)
        val coreBf = flashChannel(coreB)
        val coreAccentRf = flashChannel(coreAccentR)
        val coreAccentGf = flashChannel(coreAccentG)
        val coreAccentBf = flashChannel(coreAccentB)
        val engineRf = flashChannel(engineR)
        val engineGf = flashChannel(engineG)
        val engineBf = flashChannel(engineB)
        val edgeRf = flashChannel(edgeR)
        val edgeGf = flashChannel(edgeG)
        val edgeBf = flashChannel(edgeB)
        val darkHull = applyColorInvertMix(Color(0.07f, 0.1f, 0.14f, if (dimmed) 0.76f else 0.94f), flashMix)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = colorScratchA.set(hullDeepRf, hullDeepGf, hullDeepBf, hullDeepA)
        shapes.triangle(px(6), py(6), px(8), py(8), px(10), py(10))
        shapes.triangle(px(6), py(6), px(10), py(10), px(12), py(12))
        shapes.triangle(px(7), py(7), px(9), py(9), px(11), py(11))
        shapes.triangle(px(7), py(7), px(11), py(11), px(13), py(13))
        shapes.triangle(px(5), py(5), px(12), py(12), px(13), py(13))

        shapes.color = colorScratchA.set(hullMidRf, hullMidGf, hullMidBf, hullMidA)
        shapes.triangle(px(0), py(0), px(1), py(1), px(5), py(5))
        shapes.triangle(px(0), py(0), px(5), py(5), px(2), py(2))
        shapes.triangle(px(1), py(1), px(6), py(6), px(5), py(5))
        shapes.triangle(px(2), py(2), px(5), py(5), px(7), py(7))

        shapes.color = colorScratchA.set(coreRf, coreGf, coreBf, coreA)
        shapes.triangle(px(0), py(0), px(3), py(3), px(4), py(4))
        shapes.triangle(px(3), py(3), px(5), py(5), px(4), py(4))
        shapes.color = colorScratchA.set(coreAccentRf, coreAccentGf, coreAccentBf, coreAccentA)
        shapes.triangle(px(3), py(3), px(18), py(18), px(4), py(4))

        shapes.color = colorScratchA.set(hullDeepRf, hullDeepGf, hullDeepBf, hullDeepA)
        shapes.triangle(px(3), py(3), px(14), py(14), px(19), py(19))
        shapes.triangle(px(4), py(4), px(15), py(15), px(20), py(20))
        drawShipStyleSignature(
            styleVariant = styleVariant,
            points = shipPoints,
            modelScale = modelScale,
            coreR = coreRf,
            coreG = coreGf,
            coreB = coreBf,
            coreA = coreA,
            edgeR = edgeRf,
            edgeG = edgeGf,
            edgeB = edgeBf,
            edgeA = edgeA
        )

        val runLive = overlayMode == OverlayMode.GAME &&
            (simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING)
        val thrustIntensity = if (runLive && !dimmed) {
            (0.12f + motionSpeedIntensity * 0.86f + abs(motionSignedIntensity) * 0.2f).coerceIn(0f, 1.45f)
        } else {
            0f
        }
        if (thrustIntensity > 0.06f) {
            drawShipThrusterFlames(
                points = shipPoints,
                modelScale = modelScale,
                thrust = thrustIntensity,
                coreR = coreRf,
                coreG = coreGf,
                coreB = coreBf,
                edgeR = edgeRf,
                edgeG = edgeGf,
                edgeB = edgeBf
            )
        }

        shapes.color = darkHull
        shapes.circle(px(16), py(16), 6.5f * modelScale, 24)
        shapes.circle(px(17), py(17), 6.5f * modelScale, 24)
        shapes.color = colorScratchA.set(engineRf, engineGf, engineBf, engineA)
        shapes.circle(px(16), py(16), 3.4f * modelScale, 18)
        shapes.circle(px(17), py(17), 3.4f * modelScale, 18)

        val glowWidth = (3.8f * modelScale + speedMix * 0.7f).coerceAtLeast(1.8f)
        val edgeWidth = (2.2f * modelScale).coerceAtLeast(1.2f)
        shapes.color = colorScratchA.set(edgeRf, edgeGf, edgeBf, edgeA * 0.34f)
        drawShipOutlineSegments(shipPoints, glowWidth, styleVariant)
        shapes.color = colorScratchA.set(edgeRf, edgeGf, edgeBf, edgeA)
        drawShipOutlineSegments(shipPoints, edgeWidth, styleVariant)
        shapes.end()
    }

    private fun drawShipStyleSignature(
        styleVariant: Int,
        points: FloatArray,
        modelScale: Float,
        coreR: Float,
        coreG: Float,
        coreB: Float,
        coreA: Float,
        edgeR: Float,
        edgeG: Float,
        edgeB: Float,
        edgeA: Float
    ) {
        fun px(index: Int): Float = points[index * 2]
        fun py(index: Int): Float = points[index * 2 + 1]
        val coreX = (px(0) + px(5)) * 0.5f
        val coreY = (py(0) + py(5)) * 0.5f
        fun extendedPoint(index: Int, reach: Float, lateral: Float = 0f): Pair<Float, Float> {
            val vx = px(index) - coreX
            val vy = py(index) - coreY
            val len = sqrt(vx * vx + vy * vy).coerceAtLeast(0.001f)
            val nx = vx / len
            val ny = vy / len
            val tx = -ny
            val ty = nx
            val span = reach * modelScale
            val side = lateral * modelScale
            return Pair(
                px(index) + nx * span + tx * side,
                py(index) + ny * span + ty * side
            )
        }

        when (styleVariant) {
            0 -> {
                val (tipX, tipY) = extendedPoint(0, 28f)
                shapes.color = colorScratchA.set(coreR, coreG, coreB, coreA * 0.68f)
                shapes.triangle(px(0), py(0), px(3), py(3), tipX, tipY)
                shapes.triangle(px(0), py(0), px(4), py(4), tipX, tipY)
            }
            1 -> {
                val (leftWingX, leftWingY) = extendedPoint(10, 42f, 10f)
                val (rightWingX, rightWingY) = extendedPoint(11, 42f, -10f)
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.42f)
                shapes.triangle(px(10), py(10), px(12), py(12), leftWingX, leftWingY)
                shapes.triangle(px(11), py(11), px(13), py(13), rightWingX, rightWingY)
            }
            2 -> {
                val (leftBoxX, leftBoxY) = extendedPoint(12, 34f)
                val (rightBoxX, rightBoxY) = extendedPoint(13, 34f)
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.4f)
                shapes.triangle(px(6), py(6), px(12), py(12), leftBoxX, leftBoxY)
                shapes.triangle(px(7), py(7), px(13), py(13), rightBoxX, rightBoxY)
                shapes.color = colorScratchA.set(coreR, coreG, coreB, coreA * 0.66f)
                shapes.triangle(px(1), py(1), px(6), py(6), px(14), py(14))
                shapes.triangle(px(2), py(2), px(7), py(7), px(15), py(15))
            }
            3 -> {
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.5f)
                shapes.circle(px(3), py(3), 4.8f * modelScale, 18)
                shapes.circle(px(4), py(4), 4.8f * modelScale, 18)
                shapes.circle(px(10), py(10), 4.1f * modelScale, 16)
                shapes.circle(px(11), py(11), 4.1f * modelScale, 16)
            }
            4 -> {
                shapes.color = colorScratchA.set(coreR, coreG, coreB, coreA * 0.58f)
                shapes.triangle(px(0), py(0), px(8), py(8), px(9), py(9))
                val (leftHexX, leftHexY) = extendedPoint(12, 24f, 6f)
                val (rightHexX, rightHexY) = extendedPoint(13, 24f, -6f)
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.46f)
                shapes.triangle(px(12), py(12), leftHexX, leftHexY, px(14), py(14))
                shapes.triangle(px(13), py(13), rightHexX, rightHexY, px(15), py(15))
            }
            5 -> {
                val (leftFangX, leftFangY) = extendedPoint(18, 36f, 12f)
                val (rightFangX, rightFangY) = extendedPoint(20, 36f, -12f)
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.52f)
                shapes.triangle(px(18), py(18), px(10), py(10), leftFangX, leftFangY)
                shapes.triangle(px(20), py(20), px(11), py(11), rightFangX, rightFangY)
            }
            6 -> {
                val (spikeX, spikeY) = extendedPoint(0, 34f)
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.56f)
                shapes.triangle(px(0), py(0), px(10), py(10), px(11), py(11))
                shapes.triangle(px(5), py(5), px(12), py(12), px(13), py(13))
                shapes.color = colorScratchA.set(coreR, coreG, coreB, coreA * 0.58f)
                shapes.triangle(px(0), py(0), px(3), py(3), spikeX, spikeY)
                shapes.triangle(px(0), py(0), px(4), py(4), spikeX, spikeY)
            }
            7 -> {
                shapes.color = colorScratchA.set(coreR, coreG, coreB, coreA * 0.5f)
                shapes.circle(px(14), py(14), 3.8f * modelScale, 16)
                shapes.circle(px(15), py(15), 3.8f * modelScale, 16)
                shapes.circle(px(5), py(5), 4.2f * modelScale, 18)
            }
            8 -> {
                val (leftArcX, leftArcY) = extendedPoint(8, 20f, 16f)
                val (rightArcX, rightArcY) = extendedPoint(9, 20f, -16f)
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.45f)
                shapes.triangle(px(8), py(8), leftArcX, leftArcY, px(12), py(12))
                shapes.triangle(px(9), py(9), rightArcX, rightArcY, px(13), py(13))
            }
            9 -> {
                val (leftHookX, leftHookY) = extendedPoint(10, 38f, 18f)
                val (rightHookX, rightHookY) = extendedPoint(11, 38f, -18f)
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.5f)
                shapes.triangle(px(10), py(10), px(6), py(6), leftHookX, leftHookY)
                shapes.triangle(px(11), py(11), px(7), py(7), rightHookX, rightHookY)
            }
            else -> {
                val (leftClawX, leftClawY) = extendedPoint(12, 28f, 14f)
                val (rightClawX, rightClawY) = extendedPoint(13, 28f, -14f)
                shapes.color = colorScratchA.set(coreR, coreG, coreB, coreA * 0.52f)
                shapes.triangle(px(0), py(0), px(10), py(10), px(11), py(11))
                shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, edgeA * 0.48f)
                shapes.triangle(px(12), py(12), leftClawX, leftClawY, px(14), py(14))
                shapes.triangle(px(13), py(13), rightClawX, rightClawY, px(15), py(15))
            }
        }
    }

    private fun drawShipThrusterFlames(
        points: FloatArray,
        modelScale: Float,
        thrust: Float,
        coreR: Float,
        coreG: Float,
        coreB: Float,
        edgeR: Float,
        edgeG: Float,
        edgeB: Float
    ) {
        fun px(index: Int): Float = points[index * 2]
        fun py(index: Int): Float = points[index * 2 + 1]

        val noseX = px(0)
        val noseY = py(0)
        val tailX = px(5)
        val tailY = py(5)
        var dirX = tailX - noseX
        var dirY = tailY - noseY
        val dirLen = sqrt(dirX * dirX + dirY * dirY).coerceAtLeast(0.0001f)
        dirX /= dirLen
        dirY /= dirLen
        val sideX = -dirY
        val sideY = dirX

        val baseLength = (14f + thrust * 30f) * modelScale
        val baseWidth = (3.4f + thrust * 4.8f) * modelScale
        val pulse = 0.86f + sin(worldTime * 22f) * 0.14f

        fun drawNozzle(nozzleX: Float, nozzleY: Float, phaseOffset: Float, stretch: Float) {
            val flicker = 0.84f + 0.16f * sin(worldTime * 31f + phaseOffset)
            val length = baseLength * pulse * flicker * stretch
            val width = baseWidth * (0.92f + flicker * 0.22f) * stretch
            val tipX = nozzleX + dirX * length
            val tipY = nozzleY + dirY * length
            val leftX = nozzleX + sideX * width
            val leftY = nozzleY + sideY * width
            val rightX = nozzleX - sideX * width
            val rightY = nozzleY - sideY * width

            shapes.color = colorScratchA.set(edgeR, edgeG, edgeB, (0.26f + thrust * 0.18f) * flicker)
            shapes.triangle(leftX, leftY, rightX, rightY, tipX, tipY)
            val innerTipX = nozzleX + dirX * length * 0.58f
            val innerTipY = nozzleY + dirY * length * 0.58f
            val innerWidth = width * 0.54f
            val innerLeftX = nozzleX + sideX * innerWidth
            val innerLeftY = nozzleY + sideY * innerWidth
            val innerRightX = nozzleX - sideX * innerWidth
            val innerRightY = nozzleY - sideY * innerWidth
            shapes.color = colorScratchA.set(coreR, coreG, coreB, (0.32f + thrust * 0.24f) * flicker)
            shapes.triangle(innerLeftX, innerLeftY, innerRightX, innerRightY, innerTipX, innerTipY)
        }

        drawNozzle(px(16), py(16), 0.7f, 0.92f)
        drawNozzle(px(17), py(17), 1.9f, 0.92f)
        drawNozzle(px(5), py(5), 1.2f, 1.08f)
    }

    private fun drawShipOutlineSegments(points: FloatArray, width: Float, styleVariant: Int) {
        fun px(index: Int): Float = points[index * 2]
        fun py(index: Int): Float = points[index * 2 + 1]
        shapes.rectLine(px(0), py(0), px(6), py(6), width)
        shapes.rectLine(px(6), py(6), px(8), py(8), width)
        shapes.rectLine(px(8), py(8), px(10), py(10), width)
        shapes.rectLine(px(10), py(10), px(12), py(12), width)
        shapes.rectLine(px(12), py(12), px(5), py(5), width)
        shapes.rectLine(px(5), py(5), px(13), py(13), width)
        shapes.rectLine(px(13), py(13), px(11), py(11), width)
        shapes.rectLine(px(11), py(11), px(9), py(9), width)
        shapes.rectLine(px(9), py(9), px(7), py(7), width)
        shapes.rectLine(px(7), py(7), px(0), py(0), width)
        when (styleVariant) {
            0 -> {
                shapes.rectLine(px(3), py(3), px(18), py(18), width * 0.78f)
                shapes.rectLine(px(4), py(4), px(20), py(20), width * 0.78f)
            }
            1 -> {
                shapes.rectLine(px(8), py(8), px(18), py(18), width * 0.9f)
                shapes.rectLine(px(9), py(9), px(20), py(20), width * 0.9f)
            }
            2 -> {
                shapes.rectLine(px(6), py(6), px(14), py(14), width * 0.85f)
                shapes.rectLine(px(7), py(7), px(15), py(15), width * 0.85f)
                shapes.rectLine(px(14), py(14), px(12), py(12), width * 0.72f)
                shapes.rectLine(px(15), py(15), px(13), py(13), width * 0.72f)
            }
            3 -> {
                shapes.rectLine(px(1), py(1), px(3), py(3), width * 0.82f)
                shapes.rectLine(px(2), py(2), px(4), py(4), width * 0.82f)
            }
            4 -> {
                shapes.rectLine(px(0), py(0), px(8), py(8), width * 0.88f)
                shapes.rectLine(px(0), py(0), px(9), py(9), width * 0.88f)
            }
            5 -> {
                shapes.rectLine(px(10), py(10), px(18), py(18), width * 0.86f)
                shapes.rectLine(px(11), py(11), px(20), py(20), width * 0.86f)
            }
            6 -> {
                shapes.rectLine(px(0), py(0), px(10), py(10), width * 0.92f)
                shapes.rectLine(px(0), py(0), px(11), py(11), width * 0.92f)
            }
            7 -> {
                shapes.rectLine(px(6), py(6), px(10), py(10), width * 0.8f)
                shapes.rectLine(px(7), py(7), px(11), py(11), width * 0.8f)
            }
            8 -> {
                shapes.rectLine(px(8), py(8), px(12), py(12), width * 0.82f)
                shapes.rectLine(px(9), py(9), px(13), py(13), width * 0.82f)
            }
            9 -> {
                shapes.rectLine(px(1), py(1), px(10), py(10), width * 0.9f)
                shapes.rectLine(px(2), py(2), px(11), py(11), width * 0.9f)
            }
            else -> {
                shapes.rectLine(px(0), py(0), px(18), py(18), width * 0.82f)
                shapes.rectLine(px(0), py(0), px(20), py(20), width * 0.82f)
            }
        }
    }

    private fun drawGameHud(palette: NeonPalette) {
        val phase = simulation.phaseGateStatus
        val tokens = uiScaleTokens()
        val hud = hudLayout(tokens)
        val phasePreWarning =
            simulation.hasReversePhaseGate &&
                !phase.active &&
                (simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING) &&
                phase.secondsToStateChange in 0f..0.45f
        val levelLabel = t(
            "LEVEL ${simulation.levelConfig.index.toString().padStart(2, '0')}",
            "SEVİYE ${simulation.levelConfig.index.toString().padStart(2, '0')}"
        )
        val timeLabel = formatSeconds(simulation.elapsedRunSeconds)
        val changePrimary = realtimeChangePrimaryLabel()

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawCombatHudPanel(hud.leftGroup, accent = false)
        drawCombatHudPanel(hud.centerGroup, accent = true)
        drawCombatHudPanel(hud.rightGroup, accent = false)
        drawCombatHudPanel(hud.supportGroup, accent = true)
        shapes.color = Color(palette.grid).mul(1f, 1f, 1f, 0.22f)
        drawRoundedRect(hud.progressTrack.x, hud.progressTrack.y, hud.progressTrack.width, hud.progressTrack.height, hud.progressTrack.height * 0.5f)
        shapes.color = Color(palette.uiAccent).mul(1f, 1f, 1f, 0.92f)
        drawRoundedRect(
            hud.progressTrack.x,
            hud.progressTrack.y,
            (hud.progressTrack.width * simulation.runIntensity).coerceIn(0f, hud.progressTrack.width),
            hud.progressTrack.height,
            hud.progressTrack.height * 0.5f
        )
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(palette.uiAccent).mul(1f, 1f, 1f, 0.88f)
        drawCombatHudPanelOutline(hud.leftGroup)
        drawCombatHudPanelOutline(hud.centerGroup)
        drawCombatHudPanelOutline(hud.rightGroup)
        drawCombatHudPanelOutline(hud.supportGroup)
        shapes.color = Color(palette.uiAccent).mul(1f, 1f, 1f, 0.54f)
        shapes.line(
            hud.leftGroup.x + hud.leftGroup.width,
            hud.leftGroup.y + hud.leftGroup.height - sy(26f),
            hud.centerGroup.x,
            hud.centerGroup.y + hud.centerGroup.height - sy(26f)
        )
        shapes.line(
            hud.centerGroup.x + hud.centerGroup.width,
            hud.centerGroup.y + hud.centerGroup.height - sy(26f),
            hud.rightGroup.x,
            hud.rightGroup.y + hud.rightGroup.height - sy(26f)
        )
        shapes.line(
            hud.centerGroup.x + hud.centerGroup.width * 0.5f,
            hud.centerGroup.y,
            hud.supportGroup.x + hud.supportGroup.width * 0.5f,
            hud.supportGroup.y + hud.supportGroup.height
        )
        shapes.end()

        val supportIconY = hud.supportGroup.y + hud.supportGroup.height * 0.66f
        val supportSlotWidth = hud.supportGroup.width / 4f
        val supportIconScale = sy(0.52f).coerceIn(0.42f, 0.68f)
        val supportIconSize = sy(30f).coerceIn(18f, 38f)
        val slot1CenterX = hud.supportGroup.x + supportSlotWidth * 0.5f
        val slot2CenterX = slot1CenterX + supportSlotWidth
        val slot3CenterX = slot2CenterX + supportSlotWidth
        val slot4CenterX = slot3CenterX + supportSlotWidth

        batch.projectionMatrix = camera.combined
        batch.begin()
        metaFont.color = chromeMuted
        metaFont.draw(batch, t("MISSION", "GÖREV"), hud.leftGroup.x + tokens.sm, hud.leftGroup.y + hud.leftGroup.height - tokens.sm)
        val fittedLevel = fitLabelToWidth(levelLabel, hud.leftGroup.width - tokens.sm * 2f, font)
        font.color = chromeInk
        font.draw(
            batch,
            fittedLevel,
            hud.leftGroup.x + tokens.sm,
            hud.leftGroup.y + hud.leftGroup.height * 0.56f + lineHeight(font) * 0.32f
        )
        val fittedTier = fitLabelToWidth(localizedTierName(palette.tierName).uppercase(Locale.US), hud.leftGroup.width - tokens.sm * 2f, chipFont)
        metaFont.color = chromeMuted
        metaFont.draw(
            batch,
            fittedTier,
            hud.leftGroup.x + tokens.sm,
            hud.leftGroup.y + tokens.sm + lineHeight(chipFont) * 0.9f
        )

        metricFont.data.setScale(1.18f)
        metricFont.color = chromeInk
        val timerX = hud.centerGroup.x + hud.centerGroup.width * 0.5f - estimateTextWidth(metricFont, timeLabel) * 0.5f
        val timerY = hud.centerGroup.y + hud.centerGroup.height * 0.5f + lineHeight(metricFont) * 0.32f
        metricFont.draw(batch, timeLabel, timerX, timerY)
        metricFont.data.setScale(1f)

        metaFont.color = chromeMuted
        metaFont.draw(batch, t("STATUS", "DURUM"), hud.rightGroup.x + tokens.sm, hud.rightGroup.y + hud.rightGroup.height - tokens.sm)
        val primaryMaxWidth = hud.rightGroup.width - tokens.sm * 2f
        val primaryStatusLabel = listOf(changePrimary, compactStatusPrimaryLabel())
            .firstOrNull { estimateTextWidth(chipFont, it) <= primaryMaxWidth }
            ?: compactStatusPrimaryLabel()
        chipFont.color = chromeInk
        chipFont.draw(
            batch,
            primaryStatusLabel,
            hud.rightGroup.x + tokens.sm,
            hud.rightGroup.y + hud.rightGroup.height * 0.56f + lineHeight(chipFont) * 0.28f
        )
        val movementInfo = if (simulation.usesStepMovement) {
            t("Control: Step Input", "Kontrol: Adım Girişi")
        } else {
            t("Control: Glide Input", "Kontrol: Süzülme Girişi")
        }
        val threatInfo = t("Threats: ${simulation.activeLethalThreatCount}", "Tehdit: ${simulation.activeLethalThreatCount}")
        val rightInfo = if (estimateTextWidth(metaFont, movementInfo) <= hud.rightGroup.width - tokens.sm * 2f) {
            movementInfo
        } else {
            threatInfo
        }
        metaFont.color = chromeMuted
        metaFont.draw(batch, rightInfo, hud.rightGroup.x + tokens.sm, hud.rightGroup.y + tokens.sm + lineHeight(metaFont) * 0.88f)

        val livesValue = if (premiumEnabled) t("INF HEART", "SONSUZ CAN") else "${livesState.lives}/$MAX_LIVES"
        val shieldValue = "$shieldCount/$MAX_SHIELDS"
        val slowValue = "$slowPowerCount/$MAX_SLOW_POWERS"
        val coinValue = "$coinBalance"
        val lifeText = fitLabelToWidth(livesValue, supportSlotWidth * 0.5f, bodyFont)
        val shieldText = fitLabelToWidth(shieldValue, supportSlotWidth * 0.5f, bodyFont)
        val slowText = fitLabelToWidth(slowValue, supportSlotWidth * 0.5f, bodyFont)
        val coinText = fitLabelToWidth(coinValue, supportSlotWidth * 0.5f, bodyFont)
        val iconGap = (supportSlotWidth * 0.22f).coerceIn(sy(18f), sy(30f))
        val lifeTextWidth = estimateTextWidth(bodyFont, lifeText)
        val shieldTextWidth = estimateTextWidth(bodyFont, shieldText)
        val slowTextWidth = estimateTextWidth(bodyFont, slowText)
        val coinTextWidth = estimateTextWidth(bodyFont, coinText)
        val lifeTextX = slot1CenterX - lifeTextWidth * 0.5f + iconGap * 0.35f
        val shieldTextX = slot2CenterX - shieldTextWidth * 0.5f + iconGap * 0.35f
        val slowTextX = slot3CenterX - slowTextWidth * 0.5f + iconGap * 0.35f
        val coinTextX = slot4CenterX - coinTextWidth * 0.5f + iconGap * 0.35f
        val lifeIconX = lifeTextX - iconGap
        val shieldIconX = shieldTextX - iconGap
        val slowIconX = slowTextX - iconGap
        val coinIconX = coinTextX - iconGap
        drawUiTextureIcon(uiHeartIcon, lifeIconX, supportIconY, supportIconSize)
        drawUiTextureIcon(uiShieldIcon, shieldIconX, supportIconY, supportIconSize)
        drawUiTextureIcon(uiCoinIcon, coinIconX, supportIconY, supportIconSize)
        bodyFont.color = chromeInk
        bodyFont.draw(batch, lifeText, lifeTextX, supportIconY + lineHeight(bodyFont) * 0.26f)
        bodyFont.draw(batch, shieldText, shieldTextX, supportIconY + lineHeight(bodyFont) * 0.26f)
        bodyFont.draw(batch, slowText, slowTextX, supportIconY + lineHeight(bodyFont) * 0.26f)
        bodyFont.draw(batch, coinText, coinTextX, supportIconY + lineHeight(bodyFont) * 0.26f)
        batch.end()

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (uiHeartIcon == null) {
            drawHeartIcon(lifeIconX, supportIconY, supportIconScale, chromeAccent)
        }
        if (uiShieldIcon == null) {
            drawShieldIcon(shieldIconX, supportIconY, supportIconScale, Color(0.54f, 0.9f, 1f, 1f))
        }
        drawTimeSlowIcon(slowIconX, supportIconY, supportIconSize / 18f)
        if (uiCoinIcon == null) {
            drawCoinIcon(coinIconX, supportIconY, supportIconScale)
        }
        shapes.end()

        if (simulation.hasReversePhaseGate && (simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING)) {
            val phaseLabel = if (phase.active) {
                t("INPUT INVERTED", "KONTROL TERS")
            } else if (phasePreWarning) {
                t("SHIFT INCOMING", "TERS FAZ GELİYOR")
            } else {
                t("STABLE WINDOW", "STABİL ARALIK")
            }
            val tone = if (phase.active) {
                ChipTone.ALERT
            } else if (phasePreWarning) {
                ChipTone.WARNING
            } else {
                ChipTone.NEUTRAL
            }
            val width = hud.centerGroup.width.coerceIn(sx(240f), sx(360f))
            val chipRect = UiRect(
                x = hud.centerGroup.x + hud.centerGroup.width * 0.5f - width * 0.5f,
                y = hud.progressTrack.y - tokens.sm - sy(46f).coerceIn(38f, 56f),
                width = width,
                height = sy(46f).coerceIn(38f, 56f)
            )
            drawStatusChip(chipRect, phaseLabel, tone, palette)
        }

        if (simulation.runPhase == RunPhase.READY) {
            drawReadyOverlayCard(tokens, palette)
        }

        if (overlayMode == OverlayMode.GAME || overlayMode == OverlayMode.PAUSE) {
            drawPauseHudButton()
        }
    }

    private fun drawIntroOverlay(palette: NeonPalette, level: LevelConfig) {
        val playRect = introPlayButtonRect()
        val settingsRect = introSettingsButtonRect()
        val shopRect = introShopButtonRect()
        val premiumRect = introPremiumButtonRect()
        val resourcePanel = introResourcePanelRect()
        val lifeRewardRect = introLifeRewardButtonRect()
        val shieldRewardRect = introShieldRewardButtonRect()
        val slowRewardRect = introSlowRewardButtonRect()
        val controlLabel = controlHintForLevel(level)
        val formLabel = guideLabel(level.sectorCount)
        val featureLabel = tierFeatureLabel(level)
        val blockTotal = ((levels.size + 9) / 10).coerceAtLeast(1)
        val blockLabel = t("BLOCK ${level.featureTier}/$blockTotal", "BLOK ${level.featureTier}/$blockTotal")
        val levelBadge = t("LEVEL ${selectedLevelIndex + 1}", "SEV\u0130YE ${selectedLevelIndex + 1}")
        val heroPanel = introHeroPanelRect()
        val buttonPanel = introButtonsPanelRect()
        val headerLift = introHeaderLift()
        val titleY = heroPanel.y + heroPanel.height + sy(142f) - headerLift
        val subtitleY = titleY - sy(68f)
        val textLeft = heroPanel.x + sx(44f)
        val textWidth = heroPanel.width - sx(88f)
        drawGlassPanel(heroPanel, palette, accent = true)
        drawGlassPanel(resourcePanel, palette)
        drawGlassPanel(buttonPanel, palette)
        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = chromeInk
        val homeTitle = fitLabelToWidth("FLUXCORE", heroPanel.width - sx(80f), titleFont)
        titleFont.draw(batch, homeTitle, centeredX(homeTitle, titleFont, heroPanel), titleY)
        font.color = chromeMuted
        val homeSubtitle = t("Minimal reflex run", "Minimal refleks akışı")
        val fittedSubtitle = fitLabelToWidth(homeSubtitle, heroPanel.width - sx(80f), font)
        font.draw(batch, fittedSubtitle, centeredX(fittedSubtitle, font, heroPanel), subtitleY)

        font.color = chromeMuted
        val blockLabelFit = fitLabelToWidth(blockLabel, textWidth * 0.46f, font)
        font.draw(batch, blockLabelFit, textLeft, heroPanel.y + heroPanel.height - sy(54f) - headerLift)
        val levelBadgeFit = fitLabelToWidth(levelBadge, textWidth * 0.46f, font)
        font.draw(
            batch,
            levelBadgeFit,
            heroPanel.x + heroPanel.width - sx(44f) - estimateTextWidth(font, levelBadgeFit),
            heroPanel.y + heroPanel.height - sy(54f) - headerLift
        )

        val startLabel = t("START FROM LEVEL ${selectedLevelIndex + 1}", "${selectedLevelIndex + 1}. SEVİYEDEN BAŞLA")
        val fittedStart = fitLabelToWidth(startLabel, textWidth, uiTitleFont)
        uiTitleFont.color = chromeInk
        uiTitleFont.draw(batch, fittedStart, textLeft, heroPanel.y + heroPanel.height - sy(128f) - headerLift)

        bodyFont.color = chromeInk
        bodyFont.draw(batch, fitLabelToWidth(formLabel, textWidth, bodyFont), textLeft, heroPanel.y + heroPanel.height - sy(192f) - headerLift)
        metaFont.color = chromeMuted
        metaFont.draw(batch, fitLabelToWidth(featureLabel, textWidth, metaFont), textLeft, heroPanel.y + heroPanel.height - sy(236f) - headerLift)
        metaFont.color = chromeInk
        metaFont.draw(batch, fitLabelToWidth(controlLabel, textWidth, metaFont), textLeft, heroPanel.y + heroPanel.height - sy(286f) - headerLift)

        val summary = fitLabelToWidth(
            t(
                "Difficulty ${difficultyLabelShort()}",
                "Zorluk ${difficultyLabelShort()}"
            ),
            textWidth - (heroPanel.width * 0.36f).coerceAtLeast(sx(240f)),
            metaFont
        )
        val coinSummary = "$coinBalance"
        val coinSummaryWidth = estimateTextWidth(metaFont, coinSummary)
        val coinSummaryX = heroPanel.x + heroPanel.width - sx(44f) - coinSummaryWidth
        metaFont.color = chromeMuted
        metaFont.draw(batch, summary, textLeft, heroPanel.y + sy(48f))
        metaFont.color = chromeInk
        metaFont.draw(batch, coinSummary, coinSummaryX, heroPanel.y + sy(48f))

        val resourceTitle = t("RESOURCES", "KAYNAKLAR")
        val resourceHint = if (adsEnabled()) {
            t(
                "Use rewarded ads for life/shield. Buy time-slow in Hangar.",
                "Can/kalkan için ödüllü reklam kullan. Zaman yavaşlatmayı Hangar'dan al."
            )
        } else {
            t(
                "Manage lives and shields carefully. Buy time-slow in Hangar.",
                "Can ve kalkanları dikkatli kullan. Zaman yavaşlatmayı Hangar'dan al."
            )
        }
        uiTitleFont.color = chromeInk
        val fittedResourceTitle = fitLabelToWidth(resourceTitle, resourcePanel.width - sx(68f), uiTitleFont)
        uiTitleFont.draw(batch, fittedResourceTitle, centeredX(fittedResourceTitle, uiTitleFont, resourcePanel), resourcePanel.y + resourcePanel.height - sy(34f))
        metaFont.color = chromeMuted
        val hintLines = wrappedLines(resourceHint, resourcePanel.width - sx(68f)).take(2)
        var hintY = resourcePanel.y + resourcePanel.height - sy(92f)
        val hintLineStep = lineHeight(metaFont) * 1.04f
        for (line in hintLines) {
            val fitted = fitLabelToWidth(line, resourcePanel.width - sx(68f), metaFont)
            metaFont.draw(batch, fitted, centeredX(fitted, metaFont, resourcePanel), hintY)
            hintY -= hintLineStep
        }

        val statsY = resourcePanel.y + resourcePanel.height - sy(166f)
        val slotWidth = resourcePanel.width / 3f
        val statValues = listOf(
            if (premiumEnabled) t("INF HEART", "SONSUZ CAN") else "${livesState.lives}/$MAX_LIVES",
            "$shieldCount/$MAX_SHIELDS",
            "$slowPowerCount/$MAX_SLOW_POWERS"
        )
        val statLabels = listOf(
            t("LIVES", "CAN"),
            t("SHIELDS", "KALKAN"),
            t("SLOWDOWN", "YAVAŞLATMA")
        )
        val iconGap = sx(14f).coerceIn(10f, 22f)
        val slotInset = sx(18f).coerceIn(12f, 26f)
        val statIconSize = sy(24f).coerceIn(16f, 30f)
        val iconXBySlot = FloatArray(3)
        for (index in 0..2) {
            val slotCenterX = resourcePanel.x + slotWidth * (index + 0.5f)
            bodyFont.color = chromeInk
            val maxValueWidth = (slotWidth - statIconSize - iconGap - slotInset * 2f).coerceAtLeast(sx(64f))
            val value = fitLabelToWidth(statValues[index], maxValueWidth, bodyFont)
            val valueWidth = estimateTextWidth(bodyFont, value)
            val groupWidth = (statIconSize + iconGap + valueWidth).coerceAtMost(slotWidth - slotInset * 2f)
            val groupStartX = slotCenterX - groupWidth * 0.5f
            val iconMinX = resourcePanel.x + slotInset + statIconSize * 0.5f
            val iconMaxX = resourcePanel.x + resourcePanel.width - slotInset - statIconSize * 0.5f
            val iconCenterX = (groupStartX + statIconSize * 0.5f).coerceIn(iconMinX, iconMaxX)
            iconXBySlot[index] = iconCenterX
            bodyFont.draw(batch, value, groupStartX + statIconSize + iconGap, statsY)
            metaFont.color = chromeMuted
            val label = fitLabelToWidth(statLabels[index], slotWidth - slotInset * 2f, metaFont)
            metaFont.draw(batch, label, slotCenterX - estimateTextWidth(metaFont, label) * 0.5f, statsY - lineHeight(metaFont) * 1.08f)
        }
        batch.end()
        val iconCx = coinSummaryX - sx(62f)
        val iconCy = heroPanel.y + sy(48f) - lineHeight(metaFont) * 0.32f
        val iconSize = sy(30f).coerceIn(20f, 38f)
        val lifeIconX = iconXBySlot[0]
        val shieldIconX = iconXBySlot[1]
        val slowIconX = iconXBySlot[2]
        val lifeIconY = statsY - sy(24f)
        val supportIconLift = sy(10f).coerceIn(6f, 14f)
        val shieldIconY = lifeIconY + supportIconLift
        val slowIconY = lifeIconY + supportIconLift
        batch.begin()
        val usedTextureIcon = drawUiTextureIcon(uiCoinIcon, iconCx, iconCy, iconSize)
        val drewLifeTexture = drawUiTextureIcon(uiHeartIcon, lifeIconX, lifeIconY, statIconSize)
        val drewShieldTexture = drawUiTextureIcon(uiShieldIcon, shieldIconX, shieldIconY, statIconSize)
        batch.end()
        if (!usedTextureIcon || !drewLifeTexture || !drewShieldTexture) {
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            if (!usedTextureIcon) {
                drawCoinIcon(iconCx, iconCy, sy(0.9f).coerceIn(0.72f, 1.2f))
            }
            if (!drewLifeTexture) {
                drawHeartIcon(lifeIconX, lifeIconY, sy(0.52f).coerceIn(0.4f, 0.7f), chromeAccent)
            }
            if (!drewShieldTexture) {
                drawShieldIcon(shieldIconX, shieldIconY, sy(0.52f).coerceIn(0.4f, 0.7f), Color(0.54f, 0.9f, 1f, 1f))
            }
            shapes.end()
        }
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawTimeSlowIcon(slowIconX, slowIconY, statIconSize / 18f)
        shapes.end()
        if (adsEnabled()) {
            drawButton(lifeRewardRect, t("WATCH AD +1 LIFE", "REKLAM İZLE +1 CAN"), chromeInk)
            drawButton(shieldRewardRect, t("WATCH AD +1 SHIELD", "REKLAM İZLE +1 KALKAN"), chromeInk)
            drawButton(slowRewardRect, t("WATCH AD +1 SLOWDOWN", "REKLAM İZLE +1 YAVAŞLATMA"), chromeInk)
        }
        drawButton(playRect, t("PLAY", "OYNA"), chromeAccent)
        drawButton(settingsRect, t("SETTINGS", "AYARLAR"), chromeInk)
        drawButton(shopRect, t("HANGAR", "HANGAR"), chromeInk)
        val premiumPulse = 0.88f + 0.12f * (0.5f + 0.5f * sin(worldTime * 3.7f))
        val premiumLabel = if (premiumEnabled) {
            t("PREMIUM ACTIVE", "PREMIUM AKTİF")
        } else {
            t("OPEN PREMIUM", "PREMIUM'U AÇ")
        }
        drawButton(
            premiumRect,
            premiumLabel,
            if (premiumEnabled) chromeAccent else chromeAccent.cpy().mul(1f, 1f, 1f, premiumPulse)
        )
    }
    private fun drawSplashOverlay(palette: NeonPalette) {
        val title = "FLUXCORE"
        val subtitle = t("Focus. Rotate. Survive.", "Odaklan. Dön. Hayatta kal.")
        val titleY = viewport.worldHeight * 0.74f
        val subtitleY = titleY - sy(96f)
        val loaderRadius = sx(148f).coerceIn(92f, 168f)
        val loaderY = (viewport.worldHeight * 0.28f).coerceAtLeast(loaderRadius + sy(86f))
        drawOrbitLoader(viewport.worldWidth * 0.5f, loaderY, loaderRadius, palette)
        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = chromeInk
        titleFont.draw(batch, title, centeredX(title, true), titleY)
        font.color = chromeMuted
        font.draw(batch, subtitle, centeredX(subtitle, false), subtitleY)
        batch.end()
    }

    private fun drawEpilepsyWarningOverlay(palette: NeonPalette) {
        val card = epilepsyWarningCardRect()
        drawGlassPanel(card, palette, accent = true)
        batch.projectionMatrix = camera.combined
        batch.begin()
        uiTitleFont.color = chromeInk
        val title = t("EPILEPSY WARNING", "EPİLEPSİ UYARISI")
        uiTitleFont.draw(batch, title, centeredX(title, uiTitleFont, card), card.y + card.height - sy(52f))
        metaFont.color = chromeMuted
        val body = t(
            "This game has flashes and fast motion. Do not play if you have epilepsy, photosensitivity or seizure history.",
            "Bu oyunda ışık parlamaları ve hızlı hareket vardır. Epilepsi, fotosensitivite veya nöbet geçmişiniz varsa oynamayın."
        )
        val body2 = t(
            "Stop immediately if you feel dizziness, nausea, eye strain, headache or discomfort.",
            "Baş dönmesi, mide bulantısı, göz yorgunluğu, baş ağrısı veya rahatsızlık hissedersen hemen durdur."
        )
        bodyFont.color = chromeInk
        val lines = wrappedLines("$body\n$body2", card.width - sx(72f))
        drawWrappedText(
            lines = lines,
            x = card.x + sx(36f),
            startY = card.y + card.height - sy(126f),
            lineHeight = lineHeight(bodyFont) * 0.98f,
            clipRect = UiRect(card.x + sx(28f), card.y + sy(116f), card.width - sx(56f), card.height - sy(180f)),
            textFont = bodyFont
        )
        batch.end()
        drawButton(epilepsyContinueRect(), t("CONTINUE", "DEVAM ET"), chromeAccent)
    }
    private fun drawTransitionOverlay(palette: NeonPalette) {
        val nextLevelLabel = t(
            "LEVEL ${selectedLevelIndex + 1} CLEARED",
            "SEV\u0130YE ${selectedLevelIndex + 1} TAMAMLANDI"
        )
        val loadingLabel = t(
            "Loading next orbit...",
            "Sonraki bölüm yükleniyor..."
        )
        val titleY = viewport.worldHeight * 0.7f
        val subtitleY = titleY - sy(92f)
        val loaderRadius = sx(134f).coerceIn(88f, 154f)
        val loaderY = (viewport.worldHeight * 0.26f).coerceAtLeast(loaderRadius + sy(82f))
        drawOrbitLoader(viewport.worldWidth * 0.5f, loaderY, loaderRadius, palette)
        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = chromeInk
        titleFont.draw(batch, nextLevelLabel, centeredX(nextLevelLabel, true), titleY)
        font.color = chromeMuted
        font.draw(batch, loadingLabel, centeredX(loadingLabel, false), subtitleY)
        batch.end()
    }
    private fun drawMenuOverlay(palette: NeonPalette) {
        val panel = menuPanelRect()
        val soundRect = menuSoundRect()
        val musicVolumeRect = menuMusicVolumeRect()
        val effectsVolumeRect = menuEffectsVolumeRect()
        val hapticsRect = menuHapticsRect()
        val languageRect = menuLanguageRect()
        val difficultyRect = menuDifficultyRect()
        val levelsRect = menuLevelsRect()
        val premiumRect = menuPremiumRect()
        val policyRect = menuPolicyRect()
        val backRect = menuBackRect()
        drawGlassPanel(panel, palette)
        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = chromeInk
        titleFont.draw(
            batch,
            t("SETTINGS", "AYARLAR"),
            centeredX(t("SETTINGS", "AYARLAR"), true),
            panel.y + panel.height - sy(56f)
        )
        val subtitle = t("Audio, controls, language and difficulty", "Ses, kontrol, dil ve zorluk seçenekleri")
        font.color = chromeMuted
        font.draw(batch, subtitle, centeredX(subtitle, false), panel.y + panel.height - sy(124f))
        batch.end()
        val musicPercent = (settingsState.musicVolume * 100f).roundToInt().coerceIn(0, 100)
        val effectsPercent = (settingsState.effectsVolume * 100f).roundToInt().coerceIn(0, 100)
        drawButton(soundRect, t("SOUND ${onOff(settingsState.soundEnabled)}", "SES ${acikKapali(settingsState.soundEnabled)}"), chromeAccent)
        drawVolumeSlider(
            rect = musicVolumeRect,
            label = t("MUSIC VOLUME $musicPercent%", "MÜZİK SESİ $musicPercent%"),
            value = settingsState.musicVolume
        )
        drawVolumeSlider(
            rect = effectsVolumeRect,
            label = t("EFFECTS VOLUME $effectsPercent%", "EFEKT SESİ $effectsPercent%"),
            value = settingsState.effectsVolume
        )
        drawButton(hapticsRect, t("VIBRATION ${onOff(settingsState.hapticsEnabled)}", "TİTREŞİM ${acikKapali(settingsState.hapticsEnabled)}"), chromeAccent)
        drawButton(
            languageRect,
            t(
                "LANGUAGE ${if (settingsState.language == AppLanguage.TR) "TURKISH" else "ENGLISH"}",
                "DİL ${if (settingsState.language == AppLanguage.TR) "TÜRKÇE" else "İNGİLİZCE"}"
            ),
            chromeAccent
        )
        drawButton(difficultyRect, difficultyLabelLong(), chromeAccent)
        drawButton(levelsRect, t("LEVEL SELECT", "BÖLÜM SEÇ"), chromeInk)
        val premiumLabel = if (premiumEnabled) {
            t("PREMIUM ACTIVE", "PREMIUM AKTİF")
        } else {
            t("OPEN PREMIUM", "PREMIUM'U AÇ")
        }
        drawButton(premiumRect, premiumLabel, if (premiumEnabled) chromeAccent else chromeInk)
        drawButton(policyRect, t("LEGAL", "YASAL"), chromeInk)
        drawButton(backRect, t("BACK", "GERİ"), chromeInk)
    }

    private fun drawPremiumOverlay(palette: NeonPalette) {
        val panel = premiumPanelRect()
        val heroRect = premiumHeroRect()
        val offerRect = premiumOfferRect()
        val backRect = premiumBackButtonRect()
        val purchaseRect = premiumPurchaseButtonRect()
        val refreshRect = premiumRefreshButtonRect()
        val priceLabel = premiumStoreStatus.product?.priceLabel ?: t("ONE-TIME", "TEK SEFER")
        val tone = when {
            premiumEnabled -> ChipTone.SUCCESS
            premiumStoreStatus.message != null -> ChipTone.WARNING
            premiumStoreStatus.isLoading -> ChipTone.NEUTRAL
            else -> ChipTone.WARNING
        }
        val storeName = storeDisplayName()
        val storeNameTr = storeDisplayNameTr()
        val statusLabel = when {
            premiumEnabled -> t("VERIFIED • ONE-TIME PREMIUM ACTIVE", "DOĞRULANDI • TEK SEFERLİK PREMIUM AKTİF")
            premiumStoreStatus.isLoading -> t(
                "CHECKING ${storeName.uppercase(Locale.US)} BILLING",
                "${storeNameTr.uppercase(Locale.US)} FATURALAMA KONTROL EDİLİYOR"
            )
            premiumStoreStatus.message != null -> premiumStoreStatus.message ?: ""
            premiumStoreStatus.isAvailableForPurchase -> t("ONE-TIME PURCHASE READY", "TEK SEFERLİK SATIN ALIM HAZIR")
            else -> t("STORE SETUP REQUIRED", "MAĞAZA AYARI GEREKİYOR")
        }

        drawGlassPanel(panel, palette, accent = true)
        drawGlassPanel(heroRect, palette, accent = true)
        drawGlassPanel(offerRect, palette)

        val haloCx = heroRect.x + heroRect.width * 0.79f
        val haloCy = heroRect.y + heroRect.height * 0.56f
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0.08f, 0.2f, 0.34f, 0.92f)
        drawRoundedRect(heroRect.x + sx(22f), heroRect.y + sy(22f), heroRect.width - sx(44f), heroRect.height - sy(44f), 28f)
        shapes.end()

        drawCleanBlackHole(haloCx, haloCy, heroRect.height * 0.26f)

        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = chromeInk
        val pageTitle = t("FLUXCORE PREMIUM", "FLUXCORE PREMIUM")
        titleFont.draw(batch, pageTitle, centeredX(pageTitle, titleFont, panel), panel.y + panel.height - sy(54f))
        font.color = chromeMuted
        val subtitle = if (adsEnabled()) {
            t(
                "One-time unlock. No subscription. Removes ads and unlocks unlimited lives.",
                "Tek seferlik kilit açma. Abonelik yok. Reklamları kaldırır ve sınırsız can açar."
            )
        } else {
            t(
                "One-time unlock. No subscription. Premium survival flow with unlimited lives.",
                "Tek seferlik kilit açma. Abonelik yok. Sınırsız canlı premium hayatta kalma akışı."
            )
        }
        val subtitleLines = wrappedLines(subtitle, panel.width - sx(84f)).take(2)
        var subtitleY = panel.y + panel.height - sy(118f)
        for (line in subtitleLines) {
            val fitted = fitLabelToWidth(line, panel.width - sx(84f), font)
            font.draw(batch, fitted, centeredX(fitted, font, panel), subtitleY)
            subtitleY -= lineHeight(font) * 0.96f
        }

        uiTitleFont.color = chromeInk
        val heroTitle = t("Permanent Upgrade", "Kalıcı Yükseltme")
        uiTitleFont.draw(batch, heroTitle, heroRect.x + sx(34f), heroRect.y + heroRect.height - sy(40f))
        bodyFont.color = chromeMuted
        val heroCopy = if (adsEnabled()) {
            t(
                "Removes banner ads and unlocks unlimited lives with a verified one-time purchase.",
                "Banner reklamları kaldırır ve doğrulanmış tek seferlik satın alımla sınırsız can verir."
            )
        } else {
            t(
                "Unlocks unlimited lives with a verified one-time App Store purchase.",
                "Doğrulanmış tek seferlik App Store satın alımıyla sınırsız can verir."
            )
        }
        val heroLines = wrappedLines(heroCopy, heroRect.width * 0.56f).take(3)
        var heroTextY = heroRect.y + heroRect.height - sy(96f)
        for (line in heroLines) {
            bodyFont.draw(batch, fitLabelToWidth(line, heroRect.width * 0.56f, bodyFont), heroRect.x + sx(34f), heroTextY)
            heroTextY -= lineHeight(bodyFont) * 0.92f
        }

        metaFont.color = chromeInk
        val oneTimeLabel = t("ONE-TIME PURCHASE", "TEK SEFERLİK SATIN ALIM")
        metaFont.draw(batch, oneTimeLabel, heroRect.x + sx(34f), heroRect.y + sy(116f))
        uiTitleFont.color = chromeAccent
        val fittedPrice = fitLabelToWidth(priceLabel, heroRect.width * 0.6f, uiTitleFont)
        uiTitleFont.draw(batch, fittedPrice, heroRect.x + sx(34f), heroRect.y + sy(64f))
        batch.end()

        val firstBenefitTitle = if (adsEnabled()) {
            t("Ad-Free Navigation", "Reklamsız Akış")
        } else {
            t("Clean Premium Flow", "Temiz Premium Akış")
        }
        val firstBenefitDescription = if (adsEnabled()) {
            t(
                "Banner ads stay off while navigating intro, menu and premium screens.",
                "Intro, menü ve premium ekranlarında banner reklamlar kapalı kalır."
            )
        } else {
            t(
                "Premium screens stay focused across intro, menu and upgrade screens.",
                "Intro, menü ve yükseltme ekranlarında premium akış odaklı kalır."
            )
        }
        drawPremiumBenefitCard(
            rect = premiumBenefitRect(0),
            title = firstBenefitTitle,
            description = firstBenefitDescription,
            accent = Color(0.56f, 0.94f, 1f, 1f)
        )
        drawPremiumBenefitCard(
            rect = premiumBenefitRect(1),
            title = t("Unlimited Lives", "Sınırsız Can"),
            description = t(
                "Life refill waiting ends. Premium runs stay ready without a life timer.",
                "Can dolum beklemesi biter. Premium koşular can zamanlayıcısına takılmaz."
            ),
            accent = Color(1f, 0.7f, 0.36f, 1f)
        )
        drawPremiumBenefitCard(
            rect = premiumBenefitRect(2),
            title = t("Verified Ownership", "Doğrulanmış Sahiplik"),
            description = t(
                "Premium features only unlock after store verification. Manual toggles are disabled.",
                "Premium özellikler yalnızca mağaza doğrulamasından sonra açılır. Elle açma kapalıdır."
            ),
            accent = Color(0.66f, 1f, 0.76f, 1f)
        )

        batch.projectionMatrix = camera.combined
        batch.begin()
        uiTitleFont.color = chromeInk
        uiTitleFont.draw(batch, t("What You Get", "Ne Kazanırsın"), offerRect.x + sx(28f), offerRect.y + offerRect.height - sy(36f))
        bodyFont.color = chromeMuted
        val offerLines = wrappedLines(
            t(
                "A non-consumable one-time purchase via $storeName. Once verified, the premium ruleset applies on this device.",
                "$storeNameTr üzerinden tüketilemeyen tek seferlik satın alımdır. Doğrulanınca premium kuralları bu cihazda geçerli olur."
            ),
            offerRect.width - sx(56f)
        ).take(3)
        var offerY = offerRect.y + offerRect.height - sy(84f)
        for (line in offerLines) {
            bodyFont.draw(batch, fitLabelToWidth(line, offerRect.width - sx(56f), bodyFont), offerRect.x + sx(28f), offerY)
            offerY -= lineHeight(bodyFont) * 0.92f
        }
        batch.end()

        drawStatusChip(premiumStatusChipRect(), statusLabel, tone, palette)
        drawButton(
            purchaseRect,
            when {
                premiumEnabled -> t("PREMIUM VERIFIED", "PREMIUM DOĞRULANDI")
                premiumStoreStatus.isLoading -> t("CONNECTING STORE...", "MAĞAZAYA BAĞLANIYOR...")
                else -> t("BUY ONCE • $priceLabel", "TEK SEFER AL • $priceLabel")
            },
            if (premiumEnabled) chromeAccent else chromeAccent
        )
        drawButton(
            refreshRect,
            if (premiumEnabled) t("VERIFY OWNERSHIP", "SAHİPLİĞİ DOĞRULA") else t("RESTORE PURCHASES", "SATIN ALIMLARI GERİ YÜKLE"),
            chromeInk
        )
        drawButton(backRect, t("BACK", "GERİ"), chromeInk)

        if (premiumDialogType != PremiumDialogType.NONE) {
            drawPremiumDialogOverlay(palette)
        }
    }

    private fun drawPremiumBenefitCard(rect: UiRect, title: String, description: String, accent: Color) {
        val pulse = 0.88f + 0.12f * (0.5f + 0.5f * sin(worldTime * 2.7f + rect.y * 0.01f))
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.18f)
        drawRoundedRect(rect.x + 4f, rect.y - 8f, rect.width, rect.height, 24f)
        shapes.color = chromeSurfaceRaised
        drawRoundedRect(rect.x, rect.y, rect.width, rect.height, 24f)
        shapes.color = Color(accent).mul(1f, 1f, 1f, 0.15f + pulse * 0.12f)
        drawRoundedRect(rect.x + 8f, rect.y + rect.height - sy(18f), rect.width - 16f, sy(10f).coerceIn(8f, 14f), 6f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(accent).mul(1f, 1f, 1f, 0.8f + pulse * 0.16f)
        drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, 24f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        uiTitleFont.color = chromeInk
        val titleFit = fitLabelToWidth(title, rect.width - sx(44f), uiTitleFont)
        uiTitleFont.draw(batch, titleFit, rect.x + sx(22f), rect.y + rect.height - sy(34f))
        bodyFont.color = chromeMuted
        val lines = wrappedLines(description, rect.width - sx(44f)).take(2)
        var textY = rect.y + rect.height - sy(84f)
        for (line in lines) {
            bodyFont.draw(batch, fitLabelToWidth(line, rect.width - sx(44f), bodyFont), rect.x + sx(22f), textY)
            textY -= lineHeight(bodyFont) * 0.92f
        }
        batch.end()
    }

    private fun drawPremiumDialogOverlay(palette: NeonPalette) {
        val dialogRect = premiumDialogRect()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.58f)
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        shapes.end()
        drawGlassPanel(dialogRect, palette, accent = premiumDialogType != PremiumDialogType.FAILURE)
        if (premiumDialogType == PremiumDialogType.PROCESSING) {
            drawOrbitLoader(
                dialogRect.x + dialogRect.width * 0.5f,
                dialogRect.y + dialogRect.height * 0.62f,
                dialogRect.height * 0.16f,
                palette
            )
        }
        batch.projectionMatrix = camera.combined
        batch.begin()
        // Colour the title by tone; the separate status chip below duplicated this text
        // and overlapped it, so it is removed.
        uiTitleFont.color = when (premiumDialogTone) {
            ChipTone.SUCCESS -> Color(0.55f, 0.95f, 0.72f, 1f)
            ChipTone.WARNING -> Color(1f, 0.78f, 0.42f, 1f)
            ChipTone.ALERT -> Color(1f, 0.5f, 0.45f, 1f)
            else -> chromeInk
        }
        val titleFit = fitLabelToWidth(premiumDialogTitle, dialogRect.width - sx(72f), uiTitleFont)
        uiTitleFont.draw(batch, titleFit, centeredX(titleFit, uiTitleFont, dialogRect), dialogRect.y + dialogRect.height - sy(48f))
        bodyFont.color = chromeMuted
        val bodyLines = wrappedLines(premiumDialogBody, dialogRect.width - sx(80f)).take(4)
        var bodyY = if (premiumDialogType == PremiumDialogType.PROCESSING) {
            dialogRect.y + dialogRect.height * 0.42f
        } else {
            dialogRect.y + dialogRect.height * 0.58f
        }
        for (line in bodyLines) {
            val fitted = fitLabelToWidth(line, dialogRect.width - sx(80f), bodyFont)
            bodyFont.draw(batch, fitted, centeredX(fitted, bodyFont, dialogRect), bodyY)
            bodyY -= lineHeight(bodyFont) * 0.96f
        }
        batch.end()
        if (premiumDialogType != PremiumDialogType.PROCESSING) {
            drawButton(premiumDialogPrimaryRect(), premiumDialogPrimaryLabel, chromeAccent)
            if (premiumDialogSecondaryLabel.isNotBlank()) {
                drawButton(premiumDialogSecondaryRect(), premiumDialogSecondaryLabel, chromeInk)
            }
        }
    }

    private fun drawShopOverlay(palette: NeonPalette) {
        val panel = shopPanelRect()
        drawGlassPanel(panel, palette, accent = true)
        val selectedSkin = shipSkins.getOrNull(selectedShopShipIndex)
        val selectedUnlocked = selectedSkin?.let { unlockedShipIds.contains(it.id) } == true
        val selectedShieldItem = shieldStoreItems.getOrNull(selectedShopShieldIndex)
        val shieldItem = shieldStoreItems.firstOrNull { it.kind == SupportStoreKind.SHIELD }
        val slowItem = shieldStoreItems.firstOrNull { it.kind == SupportStoreKind.SLOW }
        val primaryLabel = when (selectedShopCategory) {
            ShopCategory.SHIPS -> when {
                selectedSkin == null -> t("NO SHIP", "GEMİ YOK")
                selectedUnlocked && selectedShipId == selectedSkin.id -> t("ACTIVE SHIP", "AKTİF GEMİ")
                selectedUnlocked -> t("USE THIS SHIP", "BU GEMİYİ KULLAN")
                else -> t("BUY • ${selectedSkin.price} COINS", "SATIN AL • ${selectedSkin.price} COIN")
            }

            ShopCategory.SHIELDS -> when {
                shieldItem == null -> t("NO SHIELD", "KALKAN YOK")
                shieldCount >= MAX_SHIELDS -> t("SHIELDS FULL", "KALKAN DOLU")
                else -> t(
                    "BUY +1 SHIELD • ${shieldItem.price} COINS",
                    "+1 KALKAN AL • ${shieldItem.price} COIN"
                )
            }
        }
        val slowActionLabel = when {
            slowItem == null -> t("NO SLOWDOWN", "YAVAŞLATMA YOK")
            slowPowerCount >= MAX_SLOW_POWERS -> t("SLOWDOWN FULL", "YAVAŞLATMA DOLU")
            else -> t("BUY +1 SLOWDOWN • ${slowItem.price} COINS", "+1 YAVAŞLATMA AL • ${slowItem.price} COIN")
        }

        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = chromeInk
        val title = t("HANGAR & STORE", "HANGAR VE MAĞAZA")
        titleFont.draw(batch, title, centeredX(title, true), panel.y + panel.height - sy(48f))
        val subtitle = if (selectedShopCategory == ShopCategory.SHIPS) {
            t("Choose a ship style and equip it. Open SHIELDS to buy shield stock.", "Gemi stilini seç ve aktif et. Kalkan stokunu KALKAN sekmesinden al.")
        } else if (!adsEnabled()) {
            t(
                "Shield blocks one hit; Slowdown briefly slows hazards. Buy with coins.",
                "Kalkan bir çarpışmayı engeller; Yavaşlatma tehlikeleri kısaca yavaşlatır."
            )
        } else {
            t(
                "Shield blocks one hit; Slowdown slows hazards. Buy with coins or ads.",
                "Kalkan çarpışma engeller; Yavaşlatma yavaşlatır. Coin veya reklamla al."
            )
        }
        font.color = chromeMuted
        val subtitleLines = wrappedLines(subtitle, panel.width - sx(52f)).take(2)
        drawWrappedText(
            lines = subtitleLines,
            x = panel.x + sx(26f),
            startY = panel.y + panel.height - sy(112f),
            lineHeight = lineHeight(font) * 0.92f,
            clipRect = UiRect(panel.x + sx(22f), panel.y + panel.height - sy(170f), panel.width - sx(44f), sy(62f)),
            textFont = font
        )
        val creditValue = "$coinBalance"
        val creditTitle = t("COINS", "COIN")
        val coinSize = sy(24f).coerceIn(16f, 30f)
        val creditY = panel.y + panel.height - sy(198f)
        val valueWidth = estimateTextWidth(font, creditValue)
        val titleWidth = estimateTextWidth(metaFont, creditTitle)
        val gap = sx(14f).coerceIn(10f, 20f)
        val rowWidth = coinSize + gap + valueWidth + gap + titleWidth
        val rowStartX = panel.x + panel.width * 0.5f - rowWidth * 0.5f
        val creditIconX = rowStartX + coinSize * 0.5f
        val creditIconY = creditY - lineHeight(font) * 0.34f
        if (!drawUiTextureIcon(uiCoinIcon, creditIconX, creditIconY, coinSize)) {
            batch.end()
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            drawCoinIcon(creditIconX, creditIconY, coinSize / 18f)
            shapes.end()
            batch.begin()
        }
        font.color = chromeInk
        font.draw(batch, creditValue, rowStartX + coinSize + gap, creditY)
        metaFont.color = chromeMuted
        metaFont.draw(batch, creditTitle, rowStartX + coinSize + gap + valueWidth + gap, creditY)
        batch.end()

        // Removed the count label that used to sit directly on top of the tabs (it
        // overlapped them and the same info already shows on the cards/cells).
        val shipsTab = shopTabShipsRect()
        val shieldsTab = shopTabShieldsRect()
        drawButton(shipsTab, t("SHIPS", "GEMİLER"), if (selectedShopCategory == ShopCategory.SHIPS) chromeAccent else chromeInk)
        drawButton(shieldsTab, t("SHIELDS", "KALKAN"), if (selectedShopCategory == ShopCategory.SHIELDS) chromeAccent else chromeInk)

        if (selectedShopCategory == ShopCategory.SHIPS) {
            drawShopFeaturedCard(selectedSkin, selectedUnlocked, palette)
            for (index in shipSkins.indices) {
                val skin = shipSkins[index]
                val rect = shopShipCellRect(index)
                val unlocked = unlockedShipIds.contains(skin.id)
                val fill = if (index == selectedShopShipIndex) chromeSurfaceRaised else chromeInset
                val outline = when {
                    index == selectedShopShipIndex -> chromeAccent
                    unlocked -> chromeStroke.cpy().mul(1f, 1f, 1f, 0.78f)
                    else -> chromeMuted.cpy().mul(1f, 1f, 1f, 0.48f)
                }

                shapes.projectionMatrix = camera.combined
                shapes.begin(ShapeRenderer.ShapeType.Filled)
                shapes.color = fill
                drawRoundedRect(rect.x, rect.y, rect.width, rect.height, 20f)
                shapes.end()

                shapes.begin(ShapeRenderer.ShapeType.Line)
                shapes.color = outline
                drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, 20f)
                shapes.end()

                drawShipPreviewCard(skin, rect, palette)
            }
        } else {
            drawShieldFeaturedCard(selectedShieldItem, palette)
        }

        drawButton(shopActionRect(), primaryLabel, chromeAccent)
        if (selectedShopCategory == ShopCategory.SHIELDS) {
            drawButton(shopSlowActionRect(), slowActionLabel, chromeAccent)
            if (adsEnabled()) {
                drawButton(
                    shopRewardRect(),
                    t("WATCH AD +1 SHIELD", "REKLAM İZLE +1 KALKAN"),
                    if (canUseShieldAd()) chromeAccent else chromeInk
                )
                drawButton(
                    shopSlowRewardRect(),
                    t("WATCH AD +1 SLOWDOWN", "REKLAM İZLE +1 YAVAŞLATMA"),
                    if (canUseSlowAd()) chromeAccent else chromeInk
                )
            }
        }
        drawButton(shopBackRect(), t("BACK", "GERİ"), chromeInk)
        if (shopNoticeTimer > 0f && shopNoticeMessage.isNotBlank()) {
            drawStatusChip(shopNoticeRect(), shopNoticeMessage, ChipTone.ALERT, palette)
        }
    }

    private fun drawShieldFeaturedCard(selectedShieldItem: ShieldStoreItem?, _palette: NeonPalette) {
        val shieldItem = shieldStoreItems.firstOrNull { it.kind == SupportStoreKind.SHIELD }
        val slowItem = shieldStoreItems.firstOrNull { it.kind == SupportStoreKind.SLOW }
        drawSupportFeaturedCard(
            rect = shopFeaturedRect(),
            item = shieldItem,
            selected = selectedShieldItem?.id == shieldItem?.id
        )
        drawSupportFeaturedCard(
            rect = shopFeaturedSecondaryRect(),
            item = slowItem,
            selected = selectedShieldItem?.id == slowItem?.id
        )
    }

    private fun drawSupportFeaturedCard(rect: UiRect, item: ShieldStoreItem?, selected: Boolean) {
        val isSlowItem = item?.kind == SupportStoreKind.SLOW
        val currentStock = if (isSlowItem) slowPowerCount else shieldCount
        val maxStock = if (isSlowItem) MAX_SLOW_POWERS else MAX_SHIELDS
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeSurface
        drawRoundedRect(rect.x, rect.y, rect.width, rect.height, 24f)
        shapes.color = chromeInset
        drawRoundedRect(rect.x + 8f, rect.y + 8f, rect.width - 16f, rect.height - 16f, 18f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = if (selected) chromeAccent else chromeStroke
        drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, 24f)
        shapes.end()

        val previewSide = (rect.height - sy(38f)).coerceAtLeast(sy(88f))
        val previewRect = UiRect(
            x = rect.x + sy(14f),
            y = rect.y + (rect.height - previewSide) * 0.5f,
            width = previewSide,
            height = previewSide
        )
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeSurfaceRaised
        drawRoundedRect(previewRect.x, previewRect.y, previewRect.width, previewRect.height, 16f)
        if (isSlowItem) {
            drawTimeSlowIcon(
                previewRect.x + previewRect.width * 0.5f,
                previewRect.y + previewRect.height * 0.5f,
                (previewRect.height / 122f).coerceIn(0.74f, 1.24f)
            )
        } else if (uiShieldIcon == null) {
            drawShieldIcon(
                previewRect.x + previewRect.width * 0.5f,
                previewRect.y + previewRect.height * 0.5f,
                (previewRect.height / 130f).coerceIn(0.7f, 1.2f),
                Color(0.54f, 0.9f, 1f, 1f)
            )
        }
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, 0.7f)
        drawRoundedRectOutline(previewRect.x, previewRect.y, previewRect.width, previewRect.height, 16f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        if (!isSlowItem && uiShieldIcon != null) {
            drawUiTextureIcon(uiShieldIcon, previewRect.x + previewRect.width * 0.5f, previewRect.y + previewRect.height * 0.5f, previewRect.height * 0.42f)
        }
        val textStartX = previewRect.x + previewRect.width + sx(18f)
        val maxWidth = rect.x + rect.width - sx(18f) - textStartX
        val name = item?.displayName ?: t("No support item selected", "Seçili destek öğesi yok")
        val state = when {
            item == null -> t("Status: Empty", "Durum: Boş")
            currentStock >= maxStock -> t("Status: Full", "Durum: Dolu")
            coinBalance >= item.price -> t("Status: Ready To Buy", "Durum: Satın Alınabilir")
            else -> t("Status: Need Coins", "Durum: Coin Gerekli")
        }
        val priceLabel = if (item == null) {
            t("Price: -", "Fiyat: -")
        } else {
            t("Price: ${item.price} coins", "Fiyat: ${item.price} coin")
        }
        val topY = rect.y + rect.height - sy(28f)
        val nameFit = fitLabelToWidth(name, maxWidth, uiTitleFont)
        uiTitleFont.color = chromeInk
        uiTitleFont.draw(batch, nameFit, textStartX, topY)
        bodyFont.color = if (coinBalance < (item?.price ?: 0)) Color(0.98f, 0.34f, 0.34f, 1f) else chromeMuted
        bodyFont.draw(batch, fitLabelToWidth(state, maxWidth, bodyFont), textStartX, topY - lineHeight(uiTitleFont) - sy(8f))
        bodyFont.color = chromeMuted
        bodyFont.draw(batch, fitLabelToWidth(priceLabel, maxWidth, bodyFont), textStartX, topY - lineHeight(uiTitleFont) - lineHeight(bodyFont) - sy(12f))
        // The per-item description was dropped: on short cards it overflowed below the card
        // into the next one, and the subtitle already explains what each item does.
        batch.end()
    }

    private fun drawShieldStoreCard(item: ShieldStoreItem, rect: UiRect) {
        val isSlowItem = item.kind == SupportStoreKind.SLOW
        val currentStock = if (isSlowItem) slowPowerCount else shieldCount
        val maxStock = if (isSlowItem) MAX_SLOW_POWERS else MAX_SHIELDS
        val isAffordable = coinBalance >= item.price
        val hasCapacity = currentStock < maxStock
        val statusLabel = when {
            !hasCapacity -> t("FULL", "DOLU")
            isAffordable -> t("BUY", "SATIN AL")
            else -> t("NO COINS", "COIN YETERSİZ")
        }
        val statusColor = when {
            !hasCapacity -> chromeMuted
            isAffordable -> Color(0.58f, 0.95f, 0.76f, 1f)
            else -> Color(0.98f, 0.34f, 0.34f, 1f)
        }
        val previewSide = rect.height * 0.34f
        val previewRect = UiRect(
            x = rect.x + rect.width * 0.5f - previewSide * 0.5f,
            y = rect.y + rect.height * 0.62f - previewSide * 0.5f,
            width = previewSide,
            height = previewSide
        )
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeSurfaceRaised
        drawRoundedRect(previewRect.x, previewRect.y, previewRect.width, previewRect.height, 16f)
        if (isSlowItem) {
            drawTimeSlowIcon(
                previewRect.x + previewRect.width * 0.5f,
                previewRect.y + previewRect.height * 0.5f,
                (previewRect.height / 132f).coerceIn(0.62f, 1.1f)
            )
        } else if (uiShieldIcon == null) {
            drawShieldIcon(
                previewRect.x + previewRect.width * 0.5f,
                previewRect.y + previewRect.height * 0.5f,
                (previewRect.height / 140f).coerceIn(0.62f, 1.1f),
                Color(0.54f, 0.9f, 1f, 1f)
            )
        }
        shapes.end()
        batch.projectionMatrix = camera.combined
        batch.begin()
        if (!isSlowItem && uiShieldIcon != null) {
            drawUiTextureIcon(uiShieldIcon, previewRect.x + previewRect.width * 0.5f, previewRect.y + previewRect.height * 0.5f, previewRect.height * 0.46f)
        }
        val name = fitLabelToWidth(item.displayName, rect.width - sx(16f), metaFont)
        metaFont.color = chromeInk
        metaFont.draw(batch, name, rect.x + rect.width * 0.5f - estimateTextWidth(metaFont, name) * 0.5f, rect.y + rect.height * 0.35f)
        chipFont.color = statusColor
        val status = fitLabelToWidth(statusLabel, rect.width - sx(18f), chipFont)
        chipFont.draw(batch, status, rect.x + rect.width * 0.5f - estimateTextWidth(chipFont, status) * 0.5f, rect.y + rect.height * 0.2f)
        val priceText = fitLabelToWidth(t("PRICE ${item.price}", "FİYAT ${item.price}"), rect.width - sx(18f), metaFont)
        metaFont.color = if (isAffordable) chromeAccent else Color(0.98f, 0.34f, 0.34f, 1f)
        metaFont.draw(batch, priceText, rect.x + rect.width * 0.5f - estimateTextWidth(metaFont, priceText) * 0.5f, rect.y + lineHeight(metaFont) * 0.98f)
        batch.end()
    }

    private fun drawShopFeaturedCard(selectedSkin: ShipSkin?, selectedUnlocked: Boolean, palette: NeonPalette) {
        val rect = shopFeaturedRect()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeSurface
        drawRoundedRect(rect.x, rect.y, rect.width, rect.height, 24f)
        shapes.color = chromeInset
        drawRoundedRect(rect.x + 8f, rect.y + 8f, rect.width - 16f, rect.height - 16f, 18f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = chromeStroke
        drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, 24f)
        shapes.end()

        val previewSide = (rect.height - sy(30f)).coerceAtLeast(sy(86f))
        val previewRect = UiRect(
            x = rect.x + sy(14f),
            y = rect.y + (rect.height - previewSide) * 0.5f,
            width = previewSide,
            height = previewSide
        )

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeSurfaceRaised
        drawRoundedRect(previewRect.x, previewRect.y, previewRect.width, previewRect.height, 16f)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, 0.7f)
        drawRoundedRectOutline(previewRect.x, previewRect.y, previewRect.width, previewRect.height, 16f)
        shapes.end()

        val previewRotation = -90f + (selectedSkin?.let { shipRotationOverrides[it.id] } ?: 0f)
        drawShipPreviewTexture(
            skin = selectedSkin,
            rect = previewRect,
            palette = palette,
            rotationDeg = previewRotation,
            dimmed = selectedSkin == null
        )

        val name = selectedSkin?.displayName ?: t("No ship selected", "Seçili gemi yok")
        val selectedLocked = selectedSkin != null && !selectedUnlocked
        val state = when {
            selectedSkin == null -> t("Status: Empty", "Durum: Boş")
            selectedUnlocked && selectedShipId == selectedSkin.id -> t("Status: Active", "Durum: Aktif")
            selectedUnlocked -> t("Status: Owned", "Durum: Sahip")
            else -> t("Status: Locked", "Durum: Kilitli")
        }
        val priceLabel = if (selectedSkin == null) {
            t("Collection: -", "Koleksiyon: -")
        } else if (selectedUnlocked) {
            t("Price: Owned", "Fiyat: Sahipsin")
        } else {
            t("Price: ${selectedSkin.price} coins", "Fiyat: ${selectedSkin.price} coin")
        }

        batch.projectionMatrix = camera.combined
        batch.begin()
        val textStartX = previewRect.x + previewRect.width + sx(18f)
        val maxWidth = rect.x + rect.width - sx(18f) - textStartX
        val nameFit = fitLabelToWidth(name, maxWidth, uiTitleFont)
        val topY = rect.y + rect.height - sy(30f)
        val stateY = topY - lineHeight(uiTitleFont) - sy(8f)
        val priceY = stateY - lineHeight(bodyFont) - sy(6f)
        uiTitleFont.color = chromeInk
        uiTitleFont.draw(batch, nameFit, textStartX, topY)
        bodyFont.color = if (selectedLocked) Color(0.98f, 0.34f, 0.34f, 1f) else chromeMuted
        bodyFont.draw(batch, fitLabelToWidth(state, maxWidth, bodyFont), textStartX, stateY)
        bodyFont.color = if (selectedLocked) chromeAccent else chromeMuted
        bodyFont.draw(batch, fitLabelToWidth(priceLabel, maxWidth, bodyFont), textStartX, priceY)
        batch.end()
    }

    private fun drawShipPreviewCard(skin: ShipSkin, rect: UiRect, palette: NeonPalette) {
        val previewHeight = rect.height * 0.34f
        val previewCenterX = rect.x + rect.width * 0.5f
        val previewCenterY = rect.y + rect.height * 0.68f
        val isLocked = !unlockedShipIds.contains(skin.id)

        val manualOffset = shipRotationOverrides[skin.id] ?: 0f
        val previewRect = UiRect(
            x = previewCenterX - previewHeight * 0.5f,
            y = previewCenterY - previewHeight * 0.5f,
            width = previewHeight,
            height = previewHeight
        )
        drawShipPreviewTexture(
            skin = skin,
            rect = previewRect,
            palette = palette,
            rotationDeg = -90f + manualOffset,
            dimmed = false
        )

        val statusLabel = when {
            selectedShipId == skin.id -> t("ACTIVE", "AKTİF")
            unlockedShipIds.contains(skin.id) -> t("OWNED", "SAHİP")
            else -> t("LOCKED", "KİLİTLİ")
        }
        val priceLabel = if (isLocked) {
            "${skin.price}"
        } else {
            t("OWNED", "SAHİP")
        }

        val statusColor = when {
            selectedShipId == skin.id -> Color(0.58f, 0.95f, 0.76f, 1f)
            isLocked -> Color(0.98f, 0.34f, 0.34f, 1f)
            else -> Color(0.58f, 0.86f, 1f, 1f)
        }

        val priceY = rect.y + rect.height * 0.08f + lineHeight(metaFont) * 0.9f
        val statusY = rect.y + rect.height * 0.2f + lineHeight(chipFont) * 0.8f
        val nameY = rect.y + rect.height * 0.32f + lineHeight(metaFont) * 0.9f
        if (isLocked) {
            val fittedStatus = fitLabelToWidth(statusLabel, rect.width - sx(34f), chipFont)
            val statusTextWidth = estimateTextWidth(chipFont, fittedStatus)
            val lockIconX = rect.x + rect.width * 0.5f - statusTextWidth * 0.5f - sx(14f)
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            drawLockIcon(lockIconX, statusY - sy(8f), 1f, statusColor)
            shapes.end()
        }

        batch.projectionMatrix = camera.combined
        batch.begin()
        val fittedName = fitLabelToWidth(skin.displayName, rect.width - sx(18f), metaFont)
        metaFont.color = chromeInk
        metaFont.draw(batch, fittedName, rect.x + rect.width * 0.5f - estimateTextWidth(metaFont, fittedName) * 0.5f, nameY)
        val fittedStatus = fitLabelToWidth(statusLabel, rect.width - sx(24f), chipFont)
        chipFont.color = statusColor
        val statusOffset = if (isLocked) sx(10f) else 0f
        chipFont.draw(
            batch,
            fittedStatus,
            rect.x + rect.width * 0.5f - estimateTextWidth(chipFont, fittedStatus) * 0.5f + statusOffset,
            statusY
        )
        val priceText = if (isLocked) {
            t("PRICE $priceLabel", "FİYAT $priceLabel")
        } else {
            t("OWNED", "SAHİP")
        }
        metaFont.color = if (isLocked) chromeAccent else Color(0.58f, 0.95f, 0.76f, 1f)
        val fittedPrice = fitLabelToWidth(priceText, rect.width - sx(18f), metaFont)
        metaFont.draw(batch, fittedPrice, rect.x + rect.width * 0.5f - estimateTextWidth(metaFont, fittedPrice) * 0.5f, priceY)
        batch.end()
    }

    private fun drawPolicyOverlay(_palette: NeonPalette) {
        val panel = policyPanelRect()
        val title = when (selectedPolicyPage) {
            PolicyPage.PRIVACY -> t("PRIVACY POLICY", "GİZLİLİK POLİTİKASI")
            PolicyPage.TERMS -> t("TERMS OF USE", "KULLANIM ŞARTLARI")
            PolicyPage.LICENSE -> t("LICENSE & ATTRIBUTION", "LİSANS VE ATIF")
        }
        val body = when (selectedPolicyPage) {
            PolicyPage.PRIVACY -> policyPrivacy()
            PolicyPage.TERMS -> policyTerms()
            PolicyPage.LICENSE -> policyLicense()
        }
        val textRect = policyTextRect()
        val lineHeight = lineHeight(bodyFont) * 1.08f
        val lines = wrappedLines(body, textRect.width)
        val totalHeight = lines.size * lineHeight
        val visibleHeight = textRect.height
        val maxScroll = (totalHeight - visibleHeight).coerceAtLeast(0f)
        if (policyScrollOffset > maxScroll) {
            policyScrollOffset = maxScroll
        }

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeSurface.cpy().mul(1f, 1f, 1f, 0.96f)
        drawRoundedRect(panel.x, panel.y, panel.width, panel.height, 34f)
        shapes.color = chromeInset.cpy().mul(1f, 1f, 1f, 0.92f)
        drawRoundedRect(
            textRect.x - sx(10f),
            textRect.y - sy(10f),
            textRect.width + sx(20f),
            textRect.height + sy(20f),
            26f
        )
        if (maxScroll > 0f) {
            val trackX = textRect.x + textRect.width + sx(10f)
            val trackY = textRect.y
            val thumbHeight = (visibleHeight * (visibleHeight / totalHeight.coerceAtLeast(visibleHeight))).coerceAtLeast(72f)
            val travel = (visibleHeight - thumbHeight).coerceAtLeast(0f)
            val progress = if (maxScroll == 0f) 0f else policyScrollOffset / maxScroll
            shapes.color = chromeMuted.cpy().mul(1f, 1f, 1f, 0.12f)
            drawRoundedRect(trackX, trackY, 12f, visibleHeight, 6f)
            shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, 0.68f)
            drawRoundedRect(trackX, trackY + travel * (1f - progress), 12f, thumbHeight, 6f)
        }
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = chromeInk
        titleFont.draw(
            batch,
            title,
            panel.x + panel.width * 0.5f - estimateTextWidth(titleFont, title) * 0.5f,
            panel.y + panel.height - sy(56f)
        )
        font.color = chromeMuted
        val subtitle = t("Scroll inside this panel to read all legal text.", "Tüm metni okumak için bu panel içinde kaydır.")
        font.draw(
            batch,
            fitLabelToWidth(subtitle, panel.width - sx(48f), font),
            panel.x + sx(24f),
            panel.y + panel.height - sy(116f)
        )
        drawWrappedText(lines, textRect.x, textRect.y + textRect.height - policyScrollOffset + sy(8f), lineHeight, textRect, bodyFont)
        batch.end()

        drawButton(policyTabLeftRect(), t("PRIVACY", "GİZLİLİK"), if (selectedPolicyPage == PolicyPage.PRIVACY) chromeAccent else chromeInk)
        drawButton(policyTabCenterRect(), t("TERMS", "ŞARTLAR"), if (selectedPolicyPage == PolicyPage.TERMS) chromeAccent else chromeInk)
        drawButton(policyTabRightRect(), t("LICENSE", "LİSANS"), if (selectedPolicyPage == PolicyPage.LICENSE) chromeAccent else chromeInk)
        drawButton(policyBackRect(), t("BACK", "GERİ"), chromeInk)
    }

    private fun drawLevelSelectOverlay(palette: NeonPalette) {
        val panel = levelSelectPanelRect()
        drawGlassPanel(panel, palette, accent = true)

        val title = t("LEVELS", "SEVİYELER")
        val hint = t("Tap a number, then press Start", "Numarayı seç, sonra Başla'ya bas")
        val selectedLabel = t("Selected: ${selectedLevelIndex + 1}", "Seçili: ${selectedLevelIndex + 1}")

        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = chromeInk
        titleFont.draw(batch, title, centeredX(title, true), panel.y + panel.height - sy(52f))
        font.color = chromeMuted
        font.draw(batch, hint, centeredX(hint, false), panel.y + panel.height - sy(124f))
        font.color = chromeInk
        font.draw(batch, selectedLabel, centeredX(selectedLabel, false), panel.y + panel.height - sy(174f))
        batch.end()

        for (index in levels.indices) {
            val cell = levelSelectCellRect(index)
            val isSelected = index == selectedLevelIndex
            val fill = if (isSelected) chromeAccentSoft.cpy().mul(1f, 1f, 1f, 1.08f) else chromeInset
            val outline = if (isSelected) chromeAccent else chromeStroke.cpy().mul(1f, 1f, 1f, 0.78f)

            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.color = fill
            drawRoundedRect(cell.x, cell.y, cell.width, cell.height, 14f)
            shapes.end()

            shapes.begin(ShapeRenderer.ShapeType.Line)
            shapes.color = outline
            drawRoundedRectOutline(cell.x, cell.y, cell.width, cell.height, 14f)
            shapes.end()

            val label = "${index + 1}"
            batch.projectionMatrix = camera.combined
            batch.begin()
            font.color = chromeInk
            val fitted = fitLabelToWidth(label, cell.width - sx(8f), font)
            font.draw(batch, fitted, cell.x + cell.width * 0.5f - estimateTextWidth(font, fitted) * 0.5f, cell.y + cell.height * 0.64f)
            batch.end()
        }

        drawButton(levelSelectBackRect(), t("BACK", "GERİ"), chromeInk)
        drawButton(levelSelectStartRect(), t("START", "BAŞLA"), chromeAccent)
    }

    private fun drawLevelSelectOverlayGrouped(palette: NeonPalette) {
        val panel = levelSelectGroupedPanelRect()
        val blockContainer = levelSelectBlockContainerRect()
        val maxUnlockedIndex = maxUnlockedLevelIndex()
        val highestUnlockedLabel = maxUnlockedIndex + 1
        drawGlassPanel(panel, palette)

        batch.projectionMatrix = camera.combined
        batch.begin()
        val title = t("LEVELS", "SEVİYELER")
        val hint = t(
            "Choose a level and press Start • Unlocked: 1-$highestUnlockedLabel",
            "Seviye seçip Başla'ya bas • Açık: 1-$highestUnlockedLabel"
        )
        val blockAreaLabel = t("LEVEL BLOCKS", "BÖLÜM BLOKLARI")
        titleFont.color = chromeInk
        titleFont.draw(batch, title, centeredX(title, true), panel.y + panel.height - sy(84f))
        font.color = chromeMuted
        font.draw(batch, hint, centeredX(hint, false), panel.y + panel.height - sy(152f))
        metaFont.color = chromeMuted.cpy().mul(1f, 1f, 1f, 0.9f)
        metaFont.draw(batch, blockAreaLabel, centeredX(blockAreaLabel, metaFont), panel.y + panel.height - sy(194f))
        batch.end()

        // The block container is no longer drawn as its own glass panel: it nested a second
        // container inside the main panel (each block already has its own frame). The rect is
        // still used for scroll bounds and the scrollbar below.
        drawButton(levelSelectBackRect(), t("BACK", "GERİ"), chromeInk)
        val startEnabled = isLevelUnlocked(selectedLevelIndex)
        drawButton(
            levelSelectStartRect(),
            if (startEnabled) t("START", "BAŞLA") else t("LOCKED", "KİLİTLİ"),
            if (startEnabled) chromeAccent else chromeStroke.cpy().mul(1f, 1f, 1f, 0.72f)
        )

        val blockCount = (levels.size + 9) / 10
        val visibleTop = blockContainer.y + blockContainer.height
        val visibleBottom = blockContainer.y
        for (blockIndex in 0 until blockCount) {
            val blockRect = levelBlockRect(blockIndex)
            if (blockRect.y < visibleBottom || blockRect.y + blockRect.height > visibleTop) {
                continue
            }
            val startLevel = blockIndex * 10 + 1
            val endLevel = minOf(startLevel + 9, levels.size)
            val activeBlock = selectedLevelIndex in (startLevel - 1)..(endLevel - 1)
            val blockTheme = blockThemeSummary(startLevel)

            drawGlassPanel(blockRect, palette, accent = activeBlock)
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.color = if (activeBlock) chromeAccent.cpy().mul(1f, 1f, 1f, 0.94f) else chromeStroke.cpy().mul(1f, 1f, 1f, 0.74f)
            drawRoundedRect(
                blockRect.x + sx(6f).coerceIn(4f, 8f),
                blockRect.y + sy(14f).coerceIn(10f, 18f),
                sx(3f).coerceIn(2f, 4f),
                blockRect.height - sy(28f).coerceAtLeast(42f),
                2f
            )
            shapes.end()

            batch.projectionMatrix = camera.combined
            batch.begin()
            metaFont.color = if (activeBlock) chromeInk else chromeMuted
            val blockLabel = t("BLOCK ${blockIndex + 1}", "BLOK ${blockIndex + 1}")
            val blockTitleY = blockRect.y + blockRect.height - sy(30f).coerceIn(22f, 40f)
            val rangeLabel = "$startLevel-$endLevel"
            metaFont.draw(batch, blockLabel, blockRect.x + sx(16f), blockTitleY)
            metaFont.draw(
                batch,
                rangeLabel,
                blockRect.x + blockRect.width - sx(16f) - estimateTextWidth(metaFont, rangeLabel),
                blockTitleY
            )
            val detailStartY = blockTitleY - lineHeight(metaFont) * 0.95f
            val maxThemeWidth = blockRect.width - sx(34f)
            metaFont.color = chromeMuted.cpy().mul(1f, 1f, 1f, if (activeBlock) 0.96f else 0.82f)
            val themeLines = wrappedLines(blockTheme, maxThemeWidth).take(3)
            val lineStep = lineHeight(metaFont) * 0.94f
            for (lineIndex in themeLines.indices) {
                val line = themeLines[lineIndex]
                metaFont.draw(batch, line, blockRect.x + sx(16f), detailStartY - lineStep * lineIndex)
            }
            batch.end()

            for (slot in 0 until 10) {
                val levelIndex = blockIndex * 10 + slot
                if (levelIndex >= levels.size) {
                    continue
                }
                val rect = levelBlockCellRect(blockIndex, slot)
                val selected = levelIndex == selectedLevelIndex
                val unlocked = levelIndex <= maxUnlockedIndex

                shapes.projectionMatrix = camera.combined
                shapes.begin(ShapeRenderer.ShapeType.Filled)
                shapes.color = when {
                    !unlocked -> chromeInset.cpy().mul(1f, 1f, 1f, 0.58f)
                    selected -> chromeAccentSoft.cpy().mul(1f, 1f, 1f, 1.3f)
                    else -> chromeInset
                }
                drawRoundedRect(rect.x, rect.y, rect.width, rect.height, 14f)
                shapes.color = when {
                    !unlocked -> chromeMuted.cpy().mul(1f, 1f, 1f, 0.46f)
                    selected -> chromeAccent.cpy().mul(1f, 1f, 1f, 0.85f)
                    else -> chromeStroke.cpy().mul(1f, 1f, 1f, 0.6f)
                }
                shapes.rect(rect.x + 2f, rect.y + 6f, 2.8f, rect.height - 12f)
                shapes.end()

                shapes.begin(ShapeRenderer.ShapeType.Line)
                shapes.color = when {
                    !unlocked -> chromeMuted.cpy().mul(1f, 1f, 1f, 0.52f)
                    selected -> chromeAccent
                    else -> chromeStroke.cpy().mul(1f, 1f, 1f, 0.72f)
                }
                drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, 14f)
                shapes.end()

                batch.projectionMatrix = camera.combined
                batch.begin()
                font.color = if (unlocked) chromeInk else chromeMuted
                val label = "${levelIndex + 1}"
                val fitted = fitLabelToWidth(label, rect.width - sx(8f), font)
                font.draw(batch, fitted, rect.x + rect.width * 0.5f - estimateTextWidth(font, fitted) * 0.5f, rect.y + rect.height * 0.64f)
                if (!unlocked) {
                    val lockLabel = t("LOCK", "KİLİT")
                    val fittedLock = fitLabelToWidth(lockLabel, rect.width - sx(8f), metaFont)
                    metaFont.color = chromeMuted.cpy().mul(1f, 1f, 1f, 0.88f)
                    metaFont.draw(
                        batch,
                        fittedLock,
                        rect.x + rect.width * 0.5f - estimateTextWidth(metaFont, fittedLock) * 0.5f,
                        rect.y + rect.height * 0.34f
                    )
                }
                batch.end()
            }
        }
        drawLevelSelectScrollbar(blockContainer)
        drawLevelSelectScrollHint(blockContainer)
    }

    private fun drawLevelSelectScrollbar(container: UiRect) {
        val maxScroll = maxLevelSelectScrollOffset()
        if (maxScroll <= 0.5f) {
            return
        }
        val track = levelSelectScrollbarTrackRect(container)
        val thumb = levelSelectScrollbarThumbRect(track, maxScroll)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeMuted.cpy().mul(1f, 1f, 1f, 0.18f)
        drawRoundedRect(track.x, track.y, track.width, track.height, track.width * 0.5f)
        val thumbAlpha = if (levelSelectScrollbarDrag) 0.96f else 0.82f
        shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, thumbAlpha)
        drawRoundedRect(thumb.x, thumb.y, thumb.width, thumb.height, thumb.width * 0.5f)
        shapes.end()
    }

    private fun drawLevelSelectScrollHint(container: UiRect) {
        val maxScroll = maxLevelSelectScrollOffset()
        if (maxScroll <= 0.5f) {
            return
        }
        // Edge-anchored directional indicators: down-chevrons at the bottom while more
        // levels sit below, up-chevrons at the top while more sit above. This reads as a
        // clear "there is more this way" state instead of a generic centred swipe hint.
        val offset = levelSelectScrollOffset
        val edge = sy(5f)
        val canDown = offset < maxScroll - edge
        val canUp = offset > edge
        val pulse = 0.55f + 0.45f * (0.5f + 0.5f * sin(worldTime * 4.2f))
        val cx = container.x + container.width * 0.5f
        val chevW = sx(30f).coerceIn(20f, 42f)
        val chevH = sy(13f).coerceIn(9f, 19f)
        val gap = sy(10f).coerceIn(7f, 15f)
        val inset = sy(18f).coerceIn(12f, 28f)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (canDown) {
            val baseY = container.y + inset
            for (k in 0..1) {
                val y = baseY + k * gap
                shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, (0.72f - k * 0.3f) * pulse)
                shapes.triangle(cx - chevW * 0.5f, y + chevH, cx + chevW * 0.5f, y + chevH, cx, y)
            }
        }
        if (canUp) {
            val baseY = container.y + container.height - inset
            for (k in 0..1) {
                val y = baseY - k * gap
                shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, (0.72f - k * 0.3f) * pulse)
                shapes.triangle(cx - chevW * 0.5f, y - chevH, cx + chevW * 0.5f, y - chevH, cx, y)
            }
        }
        shapes.end()
    }

    private fun drawResultOverlay(palette: NeonPalette) {
        val tokens = uiScaleTokens()
        val levelCleared = simulation.runPhase == RunPhase.LEVEL_CLEARED
        val primaryLabel = if (levelCleared) {
            t("NEXT LEVEL", "SONRAKİ SEVİYE")
        } else {
            t("RETRY", "TEKRAR DENE")
        }
        val secondaryLabel = if (!levelCleared && canUseShieldAd()) {
            t("+1 SHIELD + AD", "+1 KALKAN + REKLAM")
        } else if (!levelCleared && canUseExtraLifeAd()) {
            t("+1 LIFE + AD", "+1 CAN + REKLAM")
        } else {
            t("MENU", "MENÜ")
        }
        val failureReasonSummary = when (simulation.lastDeathCause) {
            GameSimulation.DeathCause.MISSILE -> t("YOU HIT A MISSILE", "FÜZEYE ÇARPTIN")
            GameSimulation.DeathCause.CORE -> t("YOU HIT THE CORE", "ÇEKİRDEĞE ÇARPTIN")
            GameSimulation.DeathCause.WALL -> t("YOU HIT THE WALL", "DUVARA ÇARPTIN")
        }
        val summaryLabel = if (levelCleared) {
            t("ORBIT STABILIZED", "ORBİT STABİLİZE EDİLDİ")
        } else {
            if (isLifeLocked()) {
                t("NO LIVES LEFT • $failureReasonSummary", "CAN BİTTİ • $failureReasonSummary")
            } else {
                failureReasonSummary
            }
        }
        val layout = resultOverlayLayout()
        val cardX = layout.card.x
        val cardY = layout.card.y
        val cardWidth = layout.card.width
        val cardHeight = layout.card.height

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeSurface.cpy().mul(1f, 1f, 1f, 0.62f)
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        if (levelCleared) {
            val confettiColors = arrayOf(
                chromeAccent,
                Color(0.64f, 0.96f, 0.86f, 1f),
                Color(0.56f, 0.86f, 1f, 1f),
                Color(1f, 0.88f, 0.56f, 1f)
            )
            val confettiCount = if (lowPerformanceMode) 20 else 42
            for (index in 0 until confettiCount) {
                val wave = worldTime * (1.4f + (index % 5) * 0.18f)
                val x = (index.toFloat() / confettiCount.toFloat()) * viewport.worldWidth + sin(wave + index * 0.61f) * sx(36f)
                val y = cardY + cardHeight + sy(60f) + (index % 7) * sy(14f) + sin(wave * 0.8f + index * 0.41f) * sy(16f)
                val size = sx(9f + (index % 3) * 4f).coerceIn(6f, 16f)
                shapes.color = confettiColors[index % confettiColors.size].cpy().mul(1f, 1f, 1f, 0.72f)
                shapes.triangle(
                    x,
                    y,
                    x + size,
                    y + size * 0.2f,
                    x + size * 0.35f,
                    y + size * 0.95f
                )
            }
        }
        shapes.color = Color(0f, 0f, 0f, 0.28f)
        drawRoundedRect(cardX + 8f, cardY - 10f, cardWidth, cardHeight, 34f)
        shapes.color = chromeSurfaceRaised
        drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 34f)
        shapes.color = chromeInset
        drawRoundedRect(cardX + 12f, cardY + 12f, cardWidth - 24f, cardHeight - 24f, 26f)
        val ribbonColor = if (levelCleared) Color(0.64f, 0.96f, 0.86f, 1f) else palette.obstacleWide
        shapes.color = Color(ribbonColor).mul(1f, 1f, 1f, 0.3f)
        drawRoundedRect(cardX + 20f, cardY + cardHeight - tokens.md - sy(10f), cardWidth - 40f, sy(10f).coerceIn(8f, 14f), 7f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = chromeStroke.cpy().mul(1f, 1f, 1f, 0.9f)
        drawRoundedRectOutline(cardX, cardY, cardWidth, cardHeight, 34f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        val textWidth = cardWidth - tokens.lg * 2f
        val titleLabel = if (levelCleared) t("VICTORY", "ZAFER") else t("FAILURE", "BAŞARISIZ")
        val detailLabel = if (statusMessage.isNotBlank()) statusMessage else summaryLabel
        val detailLines = wrappedLines(detailLabel, textWidth).take(3)
        val detailLineHeight = lineHeight(bodyFont) * 0.96f
        val detailYOffset = (detailLines.size - 1).coerceAtLeast(0) * detailLineHeight * 0.5f
        val fittedTitle = fitLabelToWidth(titleLabel, textWidth, uiTitleFont)
        uiTitleFont.color = chromeInk
        uiTitleFont.draw(batch, fittedTitle, centeredX(fittedTitle, uiTitleFont), layout.titleY)
        bodyFont.color = chromeMuted
        var detailY = layout.summaryY + detailYOffset
        for (line in detailLines) {
            val fittedLine = fitLabelToWidth(line, textWidth, bodyFont)
            bodyFont.draw(batch, fittedLine, centeredX(fittedLine, bodyFont), detailY)
            detailY -= detailLineHeight
        }
        val timeSummary = t("TIME ${formatSeconds(simulation.elapsedRunSeconds)}", "SÜRE ${formatSeconds(simulation.elapsedRunSeconds)}")
        val fittedTime = fitLabelToWidth(timeSummary, textWidth, metaFont)
        val timeRowY = layout.timeY - detailYOffset * 0.55f
        metaFont.color = chromeInk
        metaFont.draw(batch, fittedTime, centeredX(fittedTime, metaFont), timeRowY)
        if (levelCleared && lastLevelClearCoinsAwarded > 0) {
            val rewardLabel = if (levelClearDoubleClaimed) {
                t(
                    "REWARD +${lastLevelClearCoinsAwarded * 2} COINS • BALANCE $coinBalance",
                    "ÖDÜL +${lastLevelClearCoinsAwarded * 2} COIN • BAKİYE $coinBalance"
                )
            } else {
                t(
                    "REWARD +$lastLevelClearCoinsAwarded COINS • BALANCE $coinBalance",
                    "ÖDÜL +$lastLevelClearCoinsAwarded COIN • BAKİYE $coinBalance"
                )
            }
            val fittedReward = fitLabelToWidth(rewardLabel, textWidth, metaFont)
            metaFont.color = if (levelClearDoubleClaimed) Color(0.6f, 0.96f, 0.78f, 1f) else chromeMuted
            metaFont.draw(batch, fittedReward, centeredX(fittedReward, metaFont), timeRowY - lineHeight(metaFont) * 1.08f)
        }
        if (!premiumEnabled) {
            val livesLabel = buildLivesLabel()
            val fittedLives = fitLabelToWidth(livesLabel, textWidth, metaFont)
            metaFont.color = chromeMuted
            val livesY = if (levelCleared && lastLevelClearCoinsAwarded > 0) {
                timeRowY - lineHeight(metaFont) * 2.16f
            } else {
                timeRowY - lineHeight(metaFont) * 1.08f
            }
            metaFont.draw(batch, fittedLives, centeredX(fittedLives, metaFont), livesY)
        }
        batch.end()

        if (adsEnabled() && levelCleared && lastLevelClearCoinsAwarded > 0 && !premiumEnabled) {
            val offerPulse = 0.88f + 0.12f * (0.5f + 0.5f * sin(worldTime * 6.2f))
            val offerCard = layout.offerCard
            val offerButton = layout.offerButton
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.color = Color(0f, 0f, 0f, 0.24f)
            drawRoundedRect(offerCard.x + 4f, offerCard.y - 6f, offerCard.width, offerCard.height, 24f)
            shapes.color = Color(0.12f, 0.22f, 0.3f, 1f).mul(1f, 1f, 1f, 0.92f)
            drawRoundedRect(offerCard.x, offerCard.y, offerCard.width, offerCard.height, 24f)
            shapes.color = Color(0.38f, 0.9f, 0.7f, (0.18f + offerPulse * 0.22f).coerceIn(0f, 1f))
            drawRoundedRect(offerCard.x + 6f, offerCard.y + offerCard.height - sy(18f), offerCard.width - 12f, sy(10f).coerceIn(8f, 14f), 5f)
            shapes.end()

            shapes.begin(ShapeRenderer.ShapeType.Line)
            shapes.color = Color(0.58f, 1f, 0.82f, (0.76f + offerPulse * 0.2f).coerceIn(0f, 1f))
            drawRoundedRectOutline(offerCard.x, offerCard.y, offerCard.width, offerCard.height, 24f)
            shapes.end()

            batch.projectionMatrix = camera.combined
            batch.begin()
            val offerTitle = t("OFFER", "TEKLİF")
            val offerBody = if (levelClearDoubleClaimed) {
                t("Coin reward already doubled for this level.", "Bu bölüm için coin ödülü zaten ikiye katlandı.")
            } else if (!adsEnabled()) {
                t("Premium keeps runs focused with unlimited lives.", "Premium sınırsız canla koşuları odaklı tutar.")
            } else {
                t("Watch a rewarded ad and double your level coins now.", "Ödüllü reklam izle ve bölüm coin ödülünü şimdi ikiye katla.")
            }
            uiTitleFont.color = Color(0.66f, 1f, 0.86f, 1f)
            val fittedOfferTitle = fitLabelToWidth(offerTitle, offerCard.width - sx(48f), uiTitleFont)
            uiTitleFont.draw(batch, fittedOfferTitle, offerCard.x + offerCard.width * 0.5f - estimateTextWidth(uiTitleFont, fittedOfferTitle) * 0.5f, offerCard.y + offerCard.height - sy(28f))
            metaFont.color = chromeMuted
            val fittedOfferBody = fitLabelToWidth(offerBody, offerCard.width - sx(48f), metaFont)
            metaFont.draw(batch, fittedOfferBody, offerCard.x + offerCard.width * 0.5f - estimateTextWidth(metaFont, fittedOfferBody) * 0.5f, offerCard.y + offerCard.height - sy(68f))
            batch.end()

            val offerButtonLabel = if (levelClearDoubleClaimed) {
                t("2X CLAIMED", "2x ALINDI")
            } else {
                t("WATCH AD • DOUBLE COINS", "REKLAM İZLE • COINI İKİYE KATLA")
            }
            drawButton(offerButton, offerButtonLabel, chromeAccent)
        }

        val rewardBoostButton = levelCleared && canUseCoinDoubleAd() && !levelClearDoubleClaimed
        drawButton(layout.primaryButton, primaryLabel, chromeAccent)
        drawButton(layout.secondaryButton, secondaryLabel, if (!levelCleared && rewardBoostButton) chromeAccent else chromeInk)
    }

    private fun drawPauseOverlay(palette: NeonPalette) {
        val card = pauseCardRect()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.42f)
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        shapes.end()
        drawGlassPanel(card, palette, accent = true)

        batch.projectionMatrix = camera.combined
        batch.begin()
        val title = t("PAUSED", "DURAKLATILDI")
        val hint = t("Resume anytime from here", "Buradan istediğin zaman devam et")
        uiTitleFont.color = chromeInk
        uiTitleFont.draw(batch, title, centeredX(title, uiTitleFont), card.y + card.height - sy(54f))
        metaFont.color = chromeMuted
        metaFont.draw(batch, hint, centeredX(hint, metaFont), card.y + card.height - sy(104f))
        batch.end()

        drawButton(pauseResumeRect(), t("RESUME", "DEVAM ET"), chromeAccent)
        drawButton(pauseRestartRect(), t("RESTART", "YENİDEN BAŞLAT"), chromeInk)
        drawButton(pauseSettingsRect(), t("SETTINGS", "AYARLARI AÇ"), chromeInk)
        drawButton(pauseMenuRect(), t("MAIN MENU", "ANA MENÜ"), chromeInk)
    }

    private fun drawTouchControls(palette: NeonPalette) {
        val controls = touchControlsLayout()
        val supportButtonsVisible = areSupportActionButtonsVisible()
        val shieldRect = touchShieldButtonRect()
        val slowRect = touchSlowButtonRect()
        val supportGreenOutline = Color(0.3f, 0.76f, 0.56f, 0.88f)
        val supportGreenBase = Color(0.08f, 0.24f, 0.18f, 0.92f)
        val supportGreenReady = Color(0.12f, 0.34f, 0.24f, 0.96f)
        val supportGreenActive = Color(0.16f, 0.44f, 0.3f, 0.98f)
        val supportGreenLocked = Color(0.09f, 0.18f, 0.14f, 0.82f)
        val leftPressed = isTouchControlPressed(leftControl = true)
        val rightPressed = isTouchControlPressed(leftControl = false)
        val leftLabel = if (simulation.usesStepMovement) t("STEP", "ADIM") else t("GLIDE", "SÜZÜL")
        val rightLabel = leftLabel
        val leftRect = UiRect(controls.leftX, controls.y, controls.width, controls.height)
        val rightRect = UiRect(controls.rightX, controls.y, controls.width, controls.height)
        val radius = controls.height * 0.45f
        val shieldPressed = supportButtonsVisible && isButtonPressed(shieldRect)
        val slowPressed = supportButtonsVisible && isButtonPressed(slowRect)
        val shieldLocked = shieldUsedThisRun && !simulation.shieldActive && simulation.runPhase != RunPhase.READY
        val preRunShieldLocked = shieldUsedThisRun && simulation.runPhase == RunPhase.READY
        val slowLocked = slowUsedThisRun
        val shieldUsable = shieldCount > 0 &&
            !simulation.shieldActive &&
            !shieldUsedThisRun &&
            (simulation.runPhase == RunPhase.READY || simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING)
        val shieldArmedNow = simulation.runPhase == RunPhase.READY && shieldArmedForRun
        val slowUsable = slowPowerCount > 0 &&
            !simulation.manualSlowActive &&
            !slowUsedThisRun &&
            (simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeSurfaceRaised
        drawRoundedRect(leftRect.x, leftRect.y, leftRect.width, leftRect.height, radius)
        drawRoundedRect(rightRect.x, rightRect.y, rightRect.width, rightRect.height, radius)
        shapes.color = chromeInset
        drawRoundedRect(leftRect.x + 4f, leftRect.y + 4f, leftRect.width - 8f, leftRect.height - 8f, radius * 0.9f)
        drawRoundedRect(rightRect.x + 4f, rightRect.y + 4f, rightRect.width - 8f, rightRect.height - 8f, radius * 0.9f)
        if (leftPressed) {
            shapes.color = Color(palette.obstacleWide).mul(1f, 1f, 1f, 0.28f)
            drawRoundedRect(leftRect.x + 2f, leftRect.y + 2f, leftRect.width - 4f, leftRect.height - 4f, radius * 0.9f)
        }
        if (rightPressed) {
            shapes.color = Color(palette.obstacleWide).mul(1f, 1f, 1f, 0.28f)
            drawRoundedRect(rightRect.x + 2f, rightRect.y + 2f, rightRect.width - 4f, rightRect.height - 4f, radius * 0.9f)
        }
        if (supportButtonsVisible) {
            shapes.color = if (shieldArmedNow || simulation.shieldActive) {
                supportGreenActive
            } else if (shieldUsable) {
                supportGreenReady
            } else if (shieldLocked || preRunShieldLocked) {
                supportGreenLocked
            } else {
                supportGreenBase
            }
            drawRoundedRect(shieldRect.x, shieldRect.y, shieldRect.width, shieldRect.height, shieldRect.height * 0.42f)
            if (shieldPressed) {
                shapes.color = Color.WHITE.cpy().mul(1f, 1f, 1f, 0.1f)
                drawRoundedRect(
                    shieldRect.x + 2f,
                    shieldRect.y + 2f,
                    shieldRect.width - 4f,
                    shieldRect.height - 4f,
                    shieldRect.height * 0.38f
                )
            }
            shapes.color = if (simulation.manualSlowActive) {
                supportGreenActive
            } else if (slowUsable) {
                supportGreenReady
            } else if (slowLocked) {
                supportGreenLocked
            } else {
                supportGreenBase
            }
            drawRoundedRect(slowRect.x, slowRect.y, slowRect.width, slowRect.height, slowRect.height * 0.42f)
            if (slowPressed) {
                shapes.color = Color.WHITE.cpy().mul(1f, 1f, 1f, 0.1f)
                drawRoundedRect(
                    slowRect.x + 2f,
                    slowRect.y + 2f,
                    slowRect.width - 4f,
                    slowRect.height - 4f,
                    slowRect.height * 0.38f
                )
            }
        }
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(palette.uiAccent).mul(1f, 1f, 1f, 0.82f)
        drawRoundedRectOutline(leftRect.x, leftRect.y, leftRect.width, leftRect.height, radius)
        drawRoundedRectOutline(rightRect.x, rightRect.y, rightRect.width, rightRect.height, radius)
        if (supportButtonsVisible) {
            shapes.color = if (shieldLocked || preRunShieldLocked) {
                supportGreenOutline.cpy().mul(1f, 1f, 1f, 0.54f)
            } else if (shieldUsable || shieldArmedNow || simulation.shieldActive) {
                supportGreenOutline
            } else {
                supportGreenOutline.cpy().mul(1f, 1f, 1f, 0.76f)
            }
            drawRoundedRectOutline(shieldRect.x, shieldRect.y, shieldRect.width, shieldRect.height, shieldRect.height * 0.42f)
            shapes.color = if (slowLocked) {
                supportGreenOutline.cpy().mul(1f, 1f, 1f, 0.54f)
            } else if (slowUsable || simulation.manualSlowActive) {
                supportGreenOutline
            } else {
                supportGreenOutline.cpy().mul(1f, 1f, 1f, 0.76f)
            }
            drawRoundedRectOutline(slowRect.x, slowRect.y, slowRect.width, slowRect.height, slowRect.height * 0.42f)
        }
        shapes.end()

        val controlTextMaxWidth = controls.width - sx(32f)
        val leftFittedLabel = fitLabelToWidth(leftLabel, controlTextMaxWidth, font)
        val rightFittedLabel = fitLabelToWidth(rightLabel, controlTextMaxWidth, font)
        val supportIconInset = sy(16f).coerceIn(10f, 22f)
        val supportIconSize = sy(24f).coerceIn(16f, 30f)
        val supportTextPadding = sx(18f).coerceIn(12f, 28f)

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = chromeInk
        font.draw(
            batch,
            leftFittedLabel,
            controls.leftX + controls.width * 0.5f - estimateTextWidth(font, leftFittedLabel) * 0.5f,
            controls.y + controls.height * 0.56f + lineHeight(font) * 0.22f
        )
        font.draw(
            batch,
            rightFittedLabel,
            controls.rightX + controls.width * 0.5f - estimateTextWidth(font, rightFittedLabel) * 0.5f,
            controls.y + controls.height * 0.56f + lineHeight(font) * 0.22f
        )
        val shieldLabel = when {
            simulation.shieldActive -> t("SHIELD ON", "KALKAN AÇIK")
            shieldArmedNow -> t("SHIELD READY", "KALKAN HAZIR")
            shieldLocked || preRunShieldLocked -> t("SHIELD USED", "KALKAN KULLANILDI")
            shieldUsable -> t("ENABLE SHIELD", "KALKAN AÇ")
            shieldCount <= 0 -> t("NO SHIELD", "KALKAN YOK")
            else -> t("SHIELD", "KALKAN")
        }
        val slowLabel = when {
            simulation.manualSlowActive -> t("TIME SLOW ON", "YAVAŞLATMA AÇIK")
            slowLocked -> t("SLOW USED", "YAVAŞLATMA BİTTİ")
            slowUsable -> t("SLOWDOWN", "YAVAŞLATMA")
            slowPowerCount <= 0 -> t("NO SLOWDOWN", "YAVAŞLATMA YOK")
            else -> t("SLOWDOWN", "YAVAŞLATMA")
        }
        if (supportButtonsVisible) {
            val supportLabelFont = metaFont
            supportLabelFont.color = if (shieldLocked || preRunShieldLocked || slowLocked) chromeMuted else chromeInk
            val shieldTextLeft = shieldRect.x + supportIconInset + supportIconSize + supportTextPadding
            val shieldTextWidth = (shieldRect.x + shieldRect.width - sx(14f) - shieldTextLeft).coerceAtLeast(sx(80f))
            val slowTextLeft = slowRect.x + supportIconInset + supportIconSize + supportTextPadding
            val slowTextWidth = (slowRect.x + slowRect.width - sx(14f) - slowTextLeft).coerceAtLeast(sx(80f))
            val fittedShield = fitLabelToWidth(shieldLabel, shieldTextWidth, supportLabelFont)
            val fittedSlow = fitLabelToWidth(slowLabel, slowTextWidth, supportLabelFont)
            supportLabelFont.draw(
                batch,
                fittedShield,
                shieldTextLeft + shieldTextWidth * 0.5f - estimateTextWidth(supportLabelFont, fittedShield) * 0.5f,
                shieldRect.y + shieldRect.height * 0.56f + lineHeight(supportLabelFont) * 0.2f
            )
            supportLabelFont.draw(
                batch,
                fittedSlow,
                slowTextLeft + slowTextWidth * 0.5f - estimateTextWidth(supportLabelFont, fittedSlow) * 0.5f,
                slowRect.y + slowRect.height * 0.56f + lineHeight(supportLabelFont) * 0.2f
            )
            drawUiTextureIcon(
                uiShieldIcon,
                shieldRect.x + supportIconInset,
                shieldRect.y + shieldRect.height * 0.52f,
                supportIconSize
            )
        }
        batch.end()

        if (supportButtonsVisible && uiShieldIcon == null) {
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            drawShieldIcon(
                shieldRect.x + supportIconInset,
                shieldRect.y + shieldRect.height * 0.52f,
                sy(0.5f).coerceIn(0.38f, 0.64f),
                Color(0.54f, 0.9f, 1f, 1f)
            )
            shapes.end()
        }
        if (supportButtonsVisible) {
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            drawTimeSlowIcon(
                slowRect.x + supportIconInset,
                slowRect.y + slowRect.height * 0.52f,
                supportIconSize / 18f
            )
            shapes.end()
        }
    }

    private fun drawPauseHudButton() {
        val rect = pauseButtonRect()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.22f)
        drawRoundedRect(rect.x + 3f, rect.y - 5f, rect.width, rect.height, rect.height * 0.32f)
        shapes.color = chromeSurfaceRaised
        drawRoundedRect(rect.x, rect.y, rect.width, rect.height, rect.height * 0.32f)
        shapes.color = chromeInset
        drawRoundedRect(rect.x + 4f, rect.y + 4f, rect.width - 8f, rect.height - 8f, rect.height * 0.28f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, 0.9f)
        drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, rect.height * 0.32f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        buttonFont.color = chromeInk
        val pauseLabel = "||"
        buttonFont.draw(
            batch,
            pauseLabel,
            rect.x + rect.width * 0.5f - estimateTextWidth(buttonFont, pauseLabel) * 0.5f,
            rect.y + rect.height * 0.62f
        )
        batch.end()
    }

    private fun drawButton(rect: UiRect, label: String, accentColor: Color) {
        val tokens = uiScaleTokens()
        val pressed = isButtonPressed(rect)
        val pressOffset = if (pressed) 8f else 0f
        val surfaceY = rect.y - pressOffset
        val isPremiumLabel = label.contains("PREMIUM", ignoreCase = true)
        val isRewardBoostLabel =
            label.contains("DOUBLE", ignoreCase = true) ||
                label.contains("2X", ignoreCase = true) ||
                label.contains("ÖDÜLÜ", ignoreCase = true)
        val isPrimary = accentColor != chromeInk || isPremiumLabel || isRewardBoostLabel
        val pulse = if (isPrimary && !pressed) {
            0.9f + 0.1f * (0.5f + 0.5f * sin(worldTime * 4.8f + rect.y * 0.01f))
        } else {
            1f
        }
        val premiumWave = 0.5f + 0.5f * sin(worldTime * 3.2f + rect.x * 0.008f)
        val premiumOuter = Color(0.12f, 0.21f, 0.33f, 1f).lerp(Color(0.18f, 0.16f, 0.34f, 1f), premiumWave * 0.55f)
        val premiumFill = Color(0.24f, 0.62f, 1f, 0.36f).lerp(Color(0.95f, 0.54f, 0.25f, 0.4f), premiumWave)
        val premiumBorder = Color(0.46f, 0.9f, 1f, 1f).lerp(Color(1f, 0.7f, 0.32f, 1f), premiumWave)
        val outerColor = when {
            isPremiumLabel -> premiumOuter
            isRewardBoostLabel -> Color(0.13f, 0.2f, 0.3f, 1f)
            isPrimary -> chromeSurfaceRaised
            else -> chromeSurface
        }
        val fillColor = when {
            isPremiumLabel -> premiumFill
            isRewardBoostLabel -> Color(0.38f, 0.9f, 0.7f, 0.34f)
            isPrimary -> chromeAccentSoft
            else -> chromeInset
        }
        val borderColor = when {
            isPremiumLabel -> premiumBorder
            isRewardBoostLabel -> Color(0.58f, 1f, 0.82f, 1f)
            isPrimary -> chromeAccent
            else -> chromeStroke
        }
        // Bright (orange) primary buttons need dark text; white-on-orange reads too low
        // contrast. Premium (blue) and reward (green-glass) buttons keep light text.
        val textBaseColor = if (isPrimary && !isPremiumLabel && !isRewardBoostLabel) {
            Color(0.06f, 0.09f, 0.15f, 1f)
        } else {
            chromeInk
        }

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, if (pressed) 0.1f else 0.26f)
        drawRoundedRect(rect.x + 6f, rect.y - 12f, rect.width, rect.height, 20f)
        shapes.color = outerColor
        drawRoundedRect(rect.x, surfaceY, rect.width, rect.height, 20f)
        shapes.color = if (isPrimary) {
            Color(fillColor).mul(1f, 1f, 1f, (0.72f + pulse * 0.22f).coerceIn(0f, 1f))
        } else {
            fillColor
        }
        drawRoundedRect(rect.x + 6f, surfaceY + 6f, rect.width - 12f, rect.height - 12f, 16f)
        if (isPremiumLabel || isRewardBoostLabel) {
            val streakX = rect.x + 10f + ((worldTime * sx(120f)).mod((rect.width - 28f).coerceAtLeast(8f)))
            shapes.color = Color(1f, 1f, 1f, 0.18f + premiumWave * 0.16f)
            drawRoundedRect(streakX, surfaceY + 8f, sx(34f).coerceIn(20f, 44f), rect.height - 16f, 8f)
        }
        shapes.color = Color.WHITE.cpy().mul(1f, 1f, 1f, 0.05f)
        drawRoundedRect(rect.x + 10f, surfaceY + rect.height - 13f, rect.width - 20f, 5f, 4f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = if (isPrimary) {
            Color(borderColor).mul(1f, 1f, 1f, (0.82f + pulse * 0.26f).coerceIn(0f, 1f))
        } else {
            borderColor
        }
        drawRoundedRectOutline(rect.x, surfaceY, rect.width, rect.height, 20f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        val textRect = UiRect(
            x = rect.x + tokens.md,
            y = surfaceY,
            width = (rect.width - tokens.md * 2f).coerceAtLeast(sx(64f)),
            height = rect.height
        )
        drawCenteredButtonText(textRect, label, buttonFont, textBaseColor)
        batch.end()
    }

    private fun drawVolumeSlider(rect: UiRect, label: String, value: Float) {
        drawButton(rect, label, chromeAccent)
        val normalized = value.coerceIn(0f, 1f)
        val trackRect = volumeSliderTrackRect(rect)
        val trackHeight = trackRect.height
        val trackWidth = trackRect.width
        val trackX = trackRect.x
        val trackY = trackRect.y
        val fillWidth = (trackWidth * normalized).coerceIn(0f, trackWidth)
        val knobRadius = trackHeight * 0.9f
        val knobX = trackX + fillWidth
        val knobY = trackY + trackHeight * 0.5f

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.34f)
        drawRoundedRect(trackX, trackY, trackWidth, trackHeight, trackHeight * 0.5f)
        val fillColor = Color(0.23f, 0.79f, 1f, 1f)
        shapes.color = fillColor
        drawRoundedRect(trackX, trackY, fillWidth.coerceAtLeast(knobRadius), trackHeight, trackHeight * 0.5f)
        // Solid grabbable handle that stays fully on the track (was a dark "hole" that
        // overflowed the track ends).
        val knobCx = (trackX + fillWidth).coerceIn(trackX + knobRadius, trackX + trackWidth - knobRadius)
        shapes.color = Color(0f, 0f, 0f, 0.30f)
        shapes.circle(knobCx, knobY - 1f, knobRadius + 2f, 26)
        shapes.color = Color(0.96f, 0.99f, 1f, 1f)
        shapes.circle(knobCx, knobY, knobRadius, 26)
        shapes.color = fillColor
        shapes.circle(knobCx, knobY, knobRadius * 0.42f, 22)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = chromeStroke
        drawRoundedRectOutline(trackX, trackY, trackWidth, trackHeight, trackHeight * 0.5f)
        shapes.color = Color(0.05f, 0.09f, 0.16f, 0.9f)
        shapes.circle(knobCx, knobY, knobRadius, 26)
        shapes.end()
    }

    private fun volumeSliderTrackRect(sliderRect: UiRect): UiRect {
        val trackHeight = sy(14f).coerceIn(10f, 18f)
        val trackWidth = sliderRect.width - sx(56f)
        val trackX = sliderRect.x + sx(28f)
        val trackY = sliderRect.y + sy(18f).coerceIn(14f, 24f)
        return UiRect(trackX, trackY, trackWidth, trackHeight)
    }

    private fun drawCenteredButtonText(rect: UiRect, label: String, textFont: BitmapFont, textColor: Color) {
        val fitted = fitLabelToWidth(label, rect.width, textFont)
        fontLayout.setText(textFont, fitted)
        val x = pixelSnap(rect.x + (rect.width - fontLayout.width) * 0.5f)
        val y = pixelSnap(rect.y + rect.height * 0.5f + textFont.capHeight * 0.5f - textFont.descent * 0.35f)
        // Soft shadow keeps the label readable where it crosses a lighter part of the button.
        textFont.color = Color(0f, 0f, 0f, 0.42f)
        textFont.draw(batch, fitted, x + 1.5f, y - 1.5f)
        textFont.color = textColor
        textFont.draw(batch, fitted, x, y)
    }

    private fun fitLabelToWidth(label: String, maxWidth: Float, textFont: BitmapFont = font): String {
        if (estimateTextWidth(textFont, label) <= maxWidth) {
            return label
        }

        if (label.length <= 4) {
            return label
        }

        var end = label.length
        while (end > 3) {
            val candidate = "${label.substring(0, end).trimEnd()}..."
            if (estimateTextWidth(textFont, candidate) <= maxWidth) {
                return candidate
            }
            end -= 1
        }
        return label.take(3) + "..."
    }

    private fun drawGlassPanel(rect: UiRect, _palette: NeonPalette, accent: Boolean = false) {
        val panelColor = if (accent) {
            chromeSurfaceRaised
        } else {
            chromeSurface
        }

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.24f)
        drawRoundedRect(rect.x + 8f, rect.y - 12f, rect.width, rect.height, 34f)
        shapes.color = panelColor
        drawRoundedRect(rect.x, rect.y, rect.width, rect.height, 34f)
        shapes.color = if (accent) chromeAccentSoft else Color.WHITE.cpy().mul(1f, 1f, 1f, 0.04f)
        drawRoundedRect(rect.x + 14f, rect.y + rect.height - 22f, rect.width - 28f, 10f, 6f)
        shapes.color = chromeInset
        drawRoundedRect(rect.x + 10f, rect.y + 10f, rect.width - 20f, rect.height - 20f, 26f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = if (accent) chromeAccent.cpy().mul(1f, 1f, 1f, 0.72f) else chromeStroke
        drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, 34f)
        shapes.end()
    }

    private fun drawCombatHudPanel(rect: UiRect, accent: Boolean) {
        val slant = sy(18f).coerceIn(12f, 24f)
        val outer = slantedPanelVertices(rect, slant, 0f)
        val inner = slantedPanelVertices(rect, slant, 6f)
        val topStartX = outer[0]
        val topStartY = outer[1]
        val topEndX = outer[2]
        val topEndY = outer[3]
        shapes.color = Color(0f, 0f, 0f, 0.24f)
        drawConvexPolygon(slantedPanelVertices(UiRect(rect.x + 5f, rect.y - 5f, rect.width, rect.height), slant, 0f))
        shapes.color = if (accent) chromeSurfaceRaised else chromeSurface
        drawConvexPolygon(outer)
        shapes.color = chromeInset
        drawConvexPolygon(inner)
        shapes.color = if (accent) Color(chromeAccent).mul(1f, 1f, 1f, 0.34f) else Color(chromeStroke).mul(1f, 1f, 1f, 0.2f)
        drawThickLineQuad(topStartX + 10f, topStartY - 2f, topEndX - 10f, topEndY - 2f, 7f, shapes.color)
    }

    private fun drawCombatHudPanelOutline(rect: UiRect) {
        val points = slantedPanelVertices(rect, sy(18f).coerceIn(12f, 24f), 0f)
        shapes.polyline(
            floatArrayOf(
                points[0], points[1],
                points[2], points[3],
                points[4], points[5],
                points[6], points[7],
                points[8], points[9],
                points[10], points[11],
                points[0], points[1]
            )
        )
    }

    private fun slantedPanelVertices(rect: UiRect, slantRaw: Float, inset: Float): FloatArray {
        val x = rect.x + inset
        val y = rect.y + inset
        val w = (rect.width - inset * 2f).coerceAtLeast(8f)
        val h = (rect.height - inset * 2f).coerceAtLeast(8f)
        val slant = slantRaw.coerceAtMost(w * 0.25f).coerceAtMost(h * 0.72f)
        return floatArrayOf(
            x + slant, y + h,
            x + w - slant, y + h,
            x + w, y + h - slant,
            x + w, y,
            x, y,
            x, y + h - slant
        )
    }

    private fun drawStatusChip(rect: UiRect, label: String, tone: ChipTone, palette: NeonPalette) {
        val tokens = uiScaleTokens()
        val toneColor = when (tone) {
            ChipTone.NEUTRAL -> chromeStroke
            ChipTone.WARNING -> chromeAccent
            ChipTone.ALERT -> palette.needlePhase
            ChipTone.SUCCESS -> Color(0.52f, 0.92f, 0.68f, 1f)
        }

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeInset.cpy().mul(1f, 1f, 1f, 0.94f)
        drawRoundedRect(rect.x, rect.y, rect.width, rect.height, rect.height * 0.38f)
        shapes.color = Color(toneColor).mul(1f, 1f, 1f, 0.2f)
        drawRoundedRect(rect.x + 2f, rect.y + 2f, rect.width - 4f, rect.height - 4f, rect.height * 0.34f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(toneColor).mul(1f, 1f, 1f, 0.95f)
        drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, rect.height * 0.38f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        val fittedLabel = fitLabelToWidth(label, rect.width - tokens.sm * 2f, chipFont)
        chipFont.color = Color.BLACK
        val textX = rect.x + rect.width * 0.5f - estimateTextWidth(chipFont, fittedLabel) * 0.5f
        val textY = rect.y + rect.height * 0.5f + chipFont.capHeight * 0.5f - chipFont.descent * 0.35f
        chipFont.draw(batch, fittedLabel, textX, textY)
        batch.end()
    }

    private fun drawStartPromptButton(rect: UiRect, label: String) {
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.22f)
        drawRoundedRect(rect.x + 4f, rect.y - 5f, rect.width, rect.height, rect.height * 0.38f)
        shapes.color = Color(0.48f, 0.84f, 0.66f, 1f)
        drawRoundedRect(rect.x, rect.y, rect.width, rect.height, rect.height * 0.38f)
        shapes.color = Color(0.66f, 0.94f, 0.78f, 0.8f)
        drawRoundedRect(rect.x + 3f, rect.y + rect.height - 14f, rect.width - 6f, 8f, 4f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(0.03f, 0.23f, 0.17f, 1f)
        drawRoundedRectOutline(rect.x, rect.y, rect.width, rect.height, rect.height * 0.38f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        drawCenteredButtonText(
            rect = UiRect(rect.x + sx(12f), rect.y, rect.width - sx(24f), rect.height),
            label = label,
            textFont = buttonFont,
            textColor = Color(0.03f, 0.2f, 0.14f, 1f)
        )
        batch.end()
    }

    private fun drawContinueButton(rect: UiRect, label: String) {
        val pressed = isButtonPressed(rect)
        val pressOffset = if (pressed) 6f else 0f
        val surfaceY = rect.y - pressOffset

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, if (pressed) 0.1f else 0.24f)
        drawRoundedRect(rect.x + 4f, rect.y - 7f, rect.width, rect.height, 20f)
        shapes.color = chromeAccent
        drawRoundedRect(rect.x, surfaceY, rect.width, rect.height, 20f)
        shapes.color = chromeAccentSoft
        drawRoundedRect(rect.x + 4f, surfaceY + 4f, rect.width - 8f, rect.height - 8f, 16f)
        shapes.color = Color(1f, 1f, 1f, 0.16f)
        drawRoundedRect(rect.x + 8f, surfaceY + rect.height - 15f, rect.width - 16f, 8f, 6f)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = chromeAccent
        drawRoundedRectOutline(rect.x, surfaceY, rect.width, rect.height, 20f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        drawCenteredButtonText(
            rect = UiRect(rect.x + sx(12f), surfaceY, rect.width - sx(24f), rect.height),
            label = label,
            textFont = buttonFont,
            textColor = chromeInk
        )
        batch.end()
    }

    private fun drawReadyOverlayCard(tokens: UiScaleTokens, palette: NeonPalette) {
        val cardWidth = (viewport.worldWidth - tokens.insetX * 2f).coerceIn(sx(430f), sx(760f))
        val cardHeight = sy(372f).coerceIn(294f, 438f)
        val card = UiRect(
            x = viewport.worldWidth * 0.5f - cardWidth * 0.5f,
            y = (sy(270f) + tokens.sm).coerceIn(sy(210f), sy(390f)),
            width = cardWidth,
            height = cardHeight
        )
        val showBriefing = blockBriefingVisible && simulation.runPhase == RunPhase.READY
        val briefingSteps = if (showBriefing) blockBriefingSteps(blockBriefingLevel) else emptyList()
        val briefingStep = if (showBriefing && briefingSteps.isNotEmpty()) {
            briefingSteps[blockBriefingStepIndex.coerceIn(0, briefingSteps.lastIndex)]
        } else {
            null
        }
        if (briefingStep != null) {
            val focusRect = tutorialFocusRect(briefingStep.anchor)
            val anchor = Vector2(focusRect.x + focusRect.width * 0.5f, focusRect.y + focusRect.height * 0.5f)
            drawTutorialFocusMask(focusRect)
            drawTutorialTouchIcon(anchor)
        }
        drawGlassPanel(card, palette, accent = true)

        val title = t("MISSION READY", "GÖREV HAZIR")
        val hint = controlHintForLevel(simulation.levelConfig)
        val tutorial = levelStartHint(simulation.levelConfig)
        val lifeLocked = isLifeLocked()
        val prompt = if (readyCountdownActive) {
            t("LAUNCH IN ${readyCountdownStep()}", "${readyCountdownStep()} SONRA BAŞLIYOR")
        } else if (lifeLocked) {
            if (canUseExtraLifeAd()) {
                t("WATCH AD FOR +1 LIFE", "REKLAM İZLE +1 CAN")
            } else {
                buildLifeLockedMessage()
            }
        } else if (showBriefing) {
            if (briefingStep != null && blockBriefingStepIndex < briefingSteps.lastIndex) {
                t("NEXT TIP", "SONRAKİ İPUCU")
            } else {
                t("START RUN", "KOŞUYU BAŞLAT")
            }
        } else {
            t("TAP OR SPACE TO START", "BAŞLAMAK İÇİN DOKUN")
        }
        val textWidth = card.width - tokens.md * 2f
        val primaryTitle = if (showBriefing) {
            if (briefingStep != null) {
                "${blockBriefingTitle(blockBriefingLevel)} ${blockBriefingStepIndex + 1}/${briefingSteps.size}"
            } else {
                blockBriefingTitle(blockBriefingLevel)
            }
        } else {
            title
        }
        val primaryHint = when {
            showBriefing -> briefingStep?.let { t(it.titleEn, it.titleTr) } ?: tierFeatureLabel(simulation.levelConfig)
            lifeLocked -> buildLivesLabel()
            else -> "$hint\n${difficultyLabelLong()}"
        }
        val primaryDetail = when {
            showBriefing -> briefingStep?.let { t(it.detailEn, it.detailTr) } ?: blockBriefingBody(blockBriefingLevel)
            lifeLocked -> if (canUseExtraLifeAd()) {
                t("You are out of lives. Watch a rewarded ad to gain one instantly.", "Canın bitti. Hemen +1 can için ödüllü reklam izle.")
            } else {
                t("Lives refill every 30 minutes until $MAX_LIVES.", "Canlar 30 dakikada bir yenilenir (maksimum $MAX_LIVES).")
            }
            else -> {
                val assist = if (simulation.adaptiveAssistIntensity > 0.05f) {
                    val assistPct = (simulation.adaptiveAssistIntensity * 100f).toInt().coerceIn(0, 100)
                    t(
                        "Help boost active: $assistPct%.",
                        "Yardım desteği aktif: $assistPct%."
                    )
                } else {
                    t("Help boost turns on if you get stuck.", "Takılırsan yardım desteği devreye girer.")
                }
                "$tutorial $assist"
            }
        }
        val fittedTitle = fitLabelToWidth(primaryTitle, textWidth, uiTitleFont)
        val hintLines = wrappedLines(primaryHint, textWidth).take(2)
        val detailLines = wrappedLines(primaryDetail, textWidth)

        batch.projectionMatrix = camera.combined
        batch.begin()
        uiTitleFont.color = chromeInk
        uiTitleFont.draw(batch, fittedTitle, centeredX(fittedTitle, uiTitleFont), card.y + card.height - tokens.lg)
        bodyFont.color = chromeMuted
        val hintStartY = card.y + card.height - tokens.lg - lineHeight(uiTitleFont) - tokens.sm
        drawWrappedText(
            lines = hintLines,
            x = card.x + tokens.md,
            startY = hintStartY,
            lineHeight = lineHeight(bodyFont) * 0.94f,
            clipRect = UiRect(card.x + tokens.md, card.y + tokens.lg + sy(172f), card.width - tokens.md * 2f, sy(96f)),
            textFont = bodyFont
        )
        metaFont.color = chromeMuted
        val hintOffsetY = lineHeight(bodyFont) * 0.94f * (hintLines.size - 1).coerceAtLeast(0)
        val detailStartY = hintStartY - hintOffsetY - tokens.lg
        if (showBriefing) {
            drawWrappedText(
                lines = detailLines.take(3),
                x = card.x + tokens.md,
                startY = detailStartY,
                lineHeight = lineHeight(metaFont) * 0.98f,
                clipRect = UiRect(card.x + tokens.md, card.y + tokens.lg + sy(76f), card.width - tokens.md * 2f, sy(148f)),
                textFont = metaFont
            )
            if (briefingSteps.isNotEmpty()) {
                val stepDotsY = card.y + tokens.lg + sy(76f)
                batch.end()
                shapes.projectionMatrix = camera.combined
                shapes.begin(ShapeRenderer.ShapeType.Filled)
                for (index in briefingSteps.indices) {
                    val dotX = card.x + card.width * 0.5f + (index - (briefingSteps.size - 1) * 0.5f) * sx(28f)
                    shapes.color = if (index == blockBriefingStepIndex) chromeAccent else chromeStroke.cpy().mul(1f, 1f, 1f, 0.58f)
                    shapes.circle(dotX, stepDotsY, sy(6f).coerceIn(4f, 8f), 18)
                }
                shapes.end()
                batch.begin()
            }
        } else {
            drawWrappedText(
                lines = detailLines.take(4),
                x = card.x + tokens.md,
                startY = detailStartY,
                lineHeight = lineHeight(metaFont) * 0.98f,
                clipRect = UiRect(card.x + tokens.md, card.y + tokens.lg + sy(68f), card.width - tokens.md * 2f, sy(168f)),
                textFont = metaFont
            )
        }
        batch.end()

        val promptRect = UiRect(
            x = card.x + tokens.md,
            y = card.y + tokens.lg,
            width = card.width - tokens.md * 2f,
            height = sy(60f).coerceIn(48f, 70f)
        )
        if (readyCountdownActive) {
            drawStatusChip(promptRect, prompt, ChipTone.WARNING, palette)
        } else {
            drawStartPromptButton(promptRect, prompt)
        }
    }

    private fun readyPromptRect(): UiRect {
        val tokens = uiScaleTokens()
        val cardWidth = (viewport.worldWidth - tokens.insetX * 2f).coerceIn(sx(430f), sx(760f))
        val cardHeight = sy(372f).coerceIn(294f, 438f)
        val card = UiRect(
            x = viewport.worldWidth * 0.5f - cardWidth * 0.5f,
            y = (sy(270f) + tokens.sm).coerceIn(sy(210f), sy(390f)),
            width = cardWidth,
            height = cardHeight
        )
        return UiRect(
            x = card.x + tokens.md,
            y = card.y + tokens.lg,
            width = card.width - tokens.md * 2f,
            height = sy(60f).coerceIn(48f, 70f)
        )
    }

    private fun readyShieldRewardRect(): UiRect {
        val prompt = readyPromptRect()
        val height = sy(48f).coerceIn(40f, 58f)
        return UiRect(
            x = prompt.x,
            y = prompt.y + prompt.height + sy(10f).coerceIn(8f, 16f),
            width = prompt.width,
            height = height
        )
    }

    private fun updateTutorialFlow(@Suppress("UNUSED_PARAMETER") delta: Float) {
        if (!tutorialActive || overlayMode != OverlayMode.GAME) {
            tutorialPaused = false
            return
        }
        tutorialPaused = true
    }

    private fun drawTutorialOverlay(palette: NeonPalette) {
        if (!tutorialActive || overlayMode != OverlayMode.GAME) {
            return
        }
        val step = tutorialSteps.getOrNull(tutorialStepIndex) ?: return
        val focusRect = tutorialFocusRect(step.anchor)
        val anchor = Vector2(focusRect.x + focusRect.width * 0.5f, focusRect.y + focusRect.height * 0.5f)
        drawTutorialFocusMask(focusRect)
        val panel = tutorialPanelRect()
        drawGlassPanel(panel, palette, accent = true)

        val passRect = tutorialPassRect()
        drawContinueButton(passRect, t("SKIP", "GEÇ"))

        val title = t(step.titleEn, step.titleTr)
        val detail = t(step.detailEn, step.detailTr)
        batch.projectionMatrix = camera.combined
        batch.begin()
        uiTitleFont.color = chromeInk
        val textWidth = panel.width - sx(220f)
        val fittedTitle = fitLabelToWidth(title, textWidth, uiTitleFont)
        uiTitleFont.draw(
            batch,
            fittedTitle,
            panel.x + sx(26f),
            panel.y + panel.height - sy(42f)
        )
        metaFont.color = chromeMuted
        val detailLines = wrappedLines(detail, textWidth).take(2)
        drawWrappedText(
            lines = detailLines,
            x = panel.x + sx(26f),
            startY = panel.y + panel.height - sy(108f),
            lineHeight = lineHeight(metaFont) * 0.94f,
            clipRect = UiRect(
                x = panel.x + sx(24f),
                y = panel.y + sy(26f),
                width = textWidth,
                height = panel.height - sy(74f)
            ),
            textFont = metaFont
        )
        chipFont.color = chromeMuted
        val stepLabel = "${tutorialStepIndex + 1}/${tutorialSteps.size}"
        chipFont.draw(
            batch,
            stepLabel,
            panel.x + sx(26f),
            panel.y + sy(26f) + lineHeight(chipFont) * 0.86f
        )
        batch.end()

        drawTutorialTouchIcon(anchor)
    }

    private fun drawTutorialFocusMask(focusRect: UiRect) {
        val expanded = UiRect(
            x = (focusRect.x - sx(10f)).coerceAtLeast(0f),
            y = (focusRect.y - sy(8f)).coerceAtLeast(0f),
            width = (focusRect.width + sx(20f)).coerceAtMost(viewport.worldWidth),
            height = (focusRect.height + sy(16f)).coerceAtMost(viewport.worldHeight)
        )

        val pulse = 0.78f + 0.22f * (0.5f + 0.5f * sin(worldTime * 6.2f))
        val isKitchenTheme = simulation.levelConfig.index in 91..100
        if (isKitchenTheme) {
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.projectionMatrix = camera.combined
            shapes.color = Color(0.04f, 0.08f, 0.16f, 0.72f)
            drawRoundedRect(
                expanded.x,
                expanded.y,
                expanded.width,
                expanded.height,
                sy(18f).coerceIn(10f, 24f)
            )
            shapes.end()
        }
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.projectionMatrix = camera.combined
        shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, 0.96f * pulse)
        drawRoundedRectOutline(
            expanded.x,
            expanded.y,
            expanded.width,
            expanded.height,
            sy(18f).coerceIn(10f, 24f)
        )
        shapes.color = if (isKitchenTheme) {
            Color(0.86f, 0.96f, 1f, 0.64f * pulse)
        } else {
            Color.WHITE.cpy().mul(1f, 1f, 1f, 0.46f * pulse)
        }
        drawRoundedRectOutline(
            expanded.x + sx(4f),
            expanded.y + sy(4f),
            (expanded.width - sx(8f)).coerceAtLeast(sx(20f)),
            (expanded.height - sy(8f)).coerceAtLeast(sy(20f)),
            sy(14f).coerceIn(8f, 20f)
        )
        shapes.end()
    }

    private fun tutorialFocusRect(anchor: TutorialAnchor): UiRect {
        return when (anchor) {
            TutorialAnchor.READY_CARD -> readyPromptRect()
            TutorialAnchor.LEFT_CONTROL -> {
                val controls = touchControlsLayout()
                UiRect(controls.leftX, controls.y, controls.width, controls.height)
            }
            TutorialAnchor.RIGHT_CONTROL -> {
                val controls = touchControlsLayout()
                UiRect(controls.rightX, controls.y, controls.width, controls.height)
            }
            TutorialAnchor.ARENA -> {
                val arena = gameArenaLayout()
                val size = arena.radius * 0.92f
                UiRect(
                    x = arena.cx - size * 0.5f,
                    y = arena.cy - size * 0.5f,
                    width = size,
                    height = size
                )
            }
            TutorialAnchor.SUPPORT_PANEL -> {
                val hud = hudLayout(uiScaleTokens())
                hud.supportGroup
            }
        }
    }

    private fun tutorialAnchorPosition(anchor: TutorialAnchor): Vector2 {
        val position = when (anchor) {
            TutorialAnchor.READY_CARD -> {
                if (simulation.runPhase == RunPhase.READY) {
                    val promptRect = readyPromptRect()
                    Vector2(promptRect.x + promptRect.width - sy(42f), promptRect.y + promptRect.height * 0.52f)
                } else {
                    val controls = touchControlsLayout()
                    Vector2(controls.leftX + controls.width * 0.5f, controls.y + controls.height + sy(42f))
                }
            }
            TutorialAnchor.LEFT_CONTROL -> {
                val controls = touchControlsLayout()
                Vector2(controls.leftX + controls.width * 0.5f, controls.y + controls.height + sy(46f))
            }
            TutorialAnchor.RIGHT_CONTROL -> {
                val controls = touchControlsLayout()
                Vector2(controls.rightX + controls.width * 0.5f, controls.y + controls.height + sy(46f))
            }
            TutorialAnchor.ARENA -> {
                val arena = gameArenaLayout()
                Vector2(arena.cx, arena.cy + arena.radius * 0.08f)
            }
            TutorialAnchor.SUPPORT_PANEL -> {
                val hud = hudLayout(uiScaleTokens())
                Vector2(
                    hud.supportGroup.x + hud.supportGroup.width * 0.5f,
                    hud.supportGroup.y + hud.supportGroup.height * 0.5f
                )
            }
        }
        val margin = sy(62f).coerceIn(42f, 86f)
        position.x = position.x.coerceIn(margin, viewport.worldWidth - margin)
        position.y = position.y.coerceIn(margin, viewport.worldHeight - margin)
        return position
    }

    private fun tutorialPassRect(): UiRect {
        val panel = tutorialPanelRect()
        val width = sx(230f).coerceIn(186f, 290f)
        val height = sy(74f).coerceIn(62f, 94f)
        return UiRect(
            x = panel.x + panel.width - width - sx(20f),
            y = panel.y + sy(24f),
            width = width,
            height = height
        )
    }

    private fun onTutorialContinuePressed() {
        if (!tutorialActive) {
            return
        }
        if (tutorialStepIndex == 0 && simulation.runPhase == RunPhase.READY) {
            startAttempt()
        }
        tutorialStepIndex += 1
        if (tutorialStepIndex >= tutorialSteps.size) {
            completeTutorial()
        } else {
            tutorialPaused = true
        }
    }

    private fun completeTutorial() {
        tutorialActive = false
        tutorialPaused = false
        profilePreferences.putBoolean(STORE_TUTORIAL_DONE_KEY, true).flush()
    }

    private fun drawTutorialTouchIcon(anchor: Vector2) {
        val iconSize = sy(76f).coerceIn(52f, 92f)
        val x = (anchor.x - iconSize * 0.5f).coerceIn(0f, viewport.worldWidth - iconSize)
        val y = (anchor.y - iconSize * 0.5f).coerceIn(0f, viewport.worldHeight - iconSize)
        batch.projectionMatrix = camera.combined
        batch.begin()
        val drewTexture = drawUiTextureIcon(
            tutorialTouchIcon,
            x + iconSize * 0.5f,
            y + iconSize * 0.5f,
            iconSize
        )
        batch.end()
        if (drewTexture) {
            return
        }
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = chromeAccentSoft.cpy().mul(1f, 1f, 1f, 0.6f)
        shapes.circle(x + iconSize * 0.5f, y + iconSize * 0.5f, iconSize * 0.5f, 28)
        shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, 0.9f)
        shapes.triangle(
            x + iconSize * 0.35f,
            y + iconSize * 0.3f,
            x + iconSize * 0.35f,
            y + iconSize * 0.7f,
            x + iconSize * 0.72f,
            y + iconSize * 0.5f
        )
        shapes.end()
    }

    private fun tutorialPanelRect(): UiRect {
        val width = (viewport.worldWidth - sx(112f)).coerceIn(sx(460f), viewport.worldWidth - sx(30f))
        val height = sy(204f).coerceIn(sy(150f), sy(244f))
        return UiRect(
            x = viewport.worldWidth * 0.5f - width * 0.5f,
            y = viewport.worldHeight - height - sy(48f),
            width = width,
            height = height
        )
    }

    private fun drawHeroActionButton(cx: Float, cy: Float, radius: Float, _palette: NeonPalette) {
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.22f)
        shapes.circle(cx + 8f, cy - 14f, radius, 96)
        shapes.color = chromeAccentSoft
        shapes.circle(cx, cy, radius, 128)
        shapes.color = chromeSurfaceRaised
        shapes.circle(cx, cy, radius - 22f, 128)
        shapes.color = Color.WHITE.cpy().mul(1f, 1f, 1f, 0.12f)
        shapes.circle(cx, cy, radius - 28f, 128)
        shapes.color = chromeInset
        shapes.circle(cx, cy, radius - 34f, 128)
        shapes.color = chromeAccent
        shapes.triangle(cx - 16f, cy - 24f, cx - 16f, cy + 24f, cx + 28f, cy)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color.WHITE.cpy().mul(1f, 1f, 1f, 0.24f)
        shapes.circle(cx, cy, radius, 128)
        shapes.color = chromeAccent.cpy().mul(1f, 1f, 1f, 0.4f)
        shapes.circle(cx, cy, radius - 22f, 128)
        shapes.end()
    }

    private fun drawTapLoader(cx: Float, cy: Float, scale: Float, _accentColor: Color) {
        val skin = Color(0.894f, 0.773f, 0.376f, 1f)
        val handWidth = 80f * scale
        val handHeight = 60f * scale
        val originX = cx - handWidth * 0.5f
        val originY = cy - handHeight * 0.5f
        val shadowX = originX - handWidth * 0.8f
        val shadowY = originY - handHeight * 0.58f

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.3f)
        drawAsymmetricRoundedRect(
            shadowX,
            shadowY,
            handWidth * 1.8f,
            handHeight * 0.75f,
            40f * scale,
            10f * scale,
            40f * scale,
            10f * scale
        )

        shapes.color = skin
        drawAsymmetricRoundedRect(
            originX,
            originY,
            handWidth,
            handHeight,
            10f * scale,
            40f * scale,
            40f * scale,
            10f * scale
        )

        val thumbX = originX + handWidth * 0.42f
        val thumbY = originY - handHeight * 0.12f
        shapes.color = skin
        drawRotatedAsymmetricRoundedRect(
            thumbX,
            thumbY,
            handWidth * 1.2f,
            38f * scale,
            -20f * MathUtils.degreesToRadians,
            handWidth * 0.95f,
            20f * scale,
            30f * scale,
            20f * scale,
            20f * scale,
            10f * scale
        )
        shapes.color = Color(1f, 1f, 1f, 0.24f)
        drawRotatedAsymmetricRoundedRect(
            thumbX + 6f * scale,
            thumbY + 2f * scale,
            handWidth * 0.24f,
            22f * scale,
            -20f * MathUtils.degreesToRadians,
            handWidth * 0.95f,
            12f * scale,
            14f * scale,
            8f * scale,
            8f * scale,
            6f * scale
        )

        val fingerScales = floatArrayOf(0.4f, 0.6f, 0.8f, 1f)
        for (index in 0 until 4) {
            val fingerScale = fingerScales[index]
            val delay = index * 0.1f
            val phase = ((worldTime - delay) / 1.2f).mod(1f)
            val upperDeg = if (phase < 0.4f) {
                MathUtils.lerp(10f, 50f, phase / 0.4f)
            } else {
                MathUtils.lerp(50f, 10f, (phase - 0.4f) / 0.6f)
            }
            val lowerDeg = upperDeg - 60f
            val baseX = originX + handWidth * 0.288f + index * 11f * scale
            val baseY = originY + handHeight * 0.52f + index * 1.8f * scale
            val fingerWidth = 14f * fingerScale * scale
            val upperLength = 24f * fingerScale * scale
            val lowerLength = 34f * fingerScale * scale
            val tone = 0.7f + index * 0.1f
            shapes.color = Color(skin).mul(tone, tone, tone, 1f)
            drawRotatedAsymmetricRoundedRect(
                baseX - upperLength,
                baseY - fingerWidth * 0.5f,
                upperLength,
                fingerWidth,
                upperDeg * MathUtils.degreesToRadians,
                upperLength,
                fingerWidth * 0.57f,
                20f * scale * fingerScale,
                20f * scale * fingerScale,
                20f * scale * fingerScale,
                20f * scale * fingerScale
            )
            val midX = baseX - cos(upperDeg * MathUtils.degreesToRadians) * upperLength
            val midY = baseY + sin(upperDeg * MathUtils.degreesToRadians) * upperLength
            drawRotatedAsymmetricRoundedRect(
                midX - lowerLength,
                midY - fingerWidth * 0.46f,
                lowerLength,
                fingerWidth * 0.92f,
                lowerDeg * MathUtils.degreesToRadians,
                lowerLength,
                fingerWidth * 0.5f,
                20f * scale * fingerScale,
                20f * scale * fingerScale,
                20f * scale * fingerScale,
                20f * scale * fingerScale
            )
        }

        shapes.end()
    }

    private fun drawOrbitLoader(cx: Float, cy: Float, radius: Float, palette: NeonPalette) {
        val spin = worldTime * 1.95f
        val eventHorizon = radius * 0.62f * (1f + sin(worldTime * 2.6f) * 0.05f)
        val accretionOuter = radius * 1.02f
        val accretionInner = radius * 0.68f
        val ringSegA = qualitySegments(132, minimum = 44)
        val ringSegB = qualitySegments(128, minimum = 40)
        val ringSegC = qualitySegments(120, minimum = 38)
        val ringSegD = qualitySegments(104, minimum = 34)
        val coreSegA = qualitySegments(96, minimum = 30)
        val coreSegB = qualitySegments(92, minimum = 28)
        val coreSegC = qualitySegments(88, minimum = 26)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.18f)
        shapes.circle(cx + 9f, cy - 12f, radius * 1.04f, ringSegD)
        shapes.color = Color(chromeSurfaceRaised).mul(1f, 1f, 1f, 0.92f)
        shapes.circle(cx, cy, radius, ringSegA)
        shapes.color = Color(chromeAccentSoft).mul(1f, 1f, 1f, 0.9f)
        shapes.circle(cx, cy, radius * 0.88f, ringSegB)
        shapes.color = Color(chromeInset).mul(1f, 1f, 1f, 0.95f)
        shapes.circle(cx, cy, radius * 0.78f, ringSegC)

        shapes.color = Color(chromeAccent).mul(1f, 1f, 1f, 0.28f + sin(worldTime * 6.1f) * 0.06f)
        drawLoaderArc(cx, cy, accretionOuter, accretionInner, spin, MathUtils.PI * 0.96f)
        shapes.color = Color(palette.uiAccent).mul(1f, 1f, 1f, 0.24f + sin(worldTime * 5.2f + 1f) * 0.05f)
        drawLoaderArc(cx, cy, accretionOuter * 0.82f, accretionInner * 0.74f, -spin * 1.28f, MathUtils.PI * 0.7f)

        shapes.color = Color(chromeSurface).mul(1f, 1f, 1f, 0.74f)
        shapes.circle(cx, cy, eventHorizon * 1.08f, coreSegA)
        shapes.color = Color(chromeInset).mul(1f, 1f, 1f, 0.9f)
        shapes.circle(cx, cy, eventHorizon * 0.84f, coreSegB)
        shapes.color = Color(0f, 0f, 0f, 1f)
        shapes.circle(cx, cy, eventHorizon * 0.62f, coreSegC)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color(chromeStroke).mul(1f, 1f, 1f, 0.84f)
        shapes.circle(cx, cy, radius, ringSegB)
        shapes.circle(cx, cy, radius * 0.78f, ringSegC)
        shapes.end()
    }

    // Clean, cohesive black-hole emblem matching the in-game core: warm accretion disk,
    // contrasting cyan photon streams, a bright photon ring and a solid black horizon.
    private fun drawCleanBlackHole(cx: Float, cy: Float, radius: Float) {
        val sides = qualitySegments(96, minimum = 40)
        val voidRadius = radius * 0.46f * (1f + 0.02f * sin(worldTime * 1.5f))
        val diskOuter = radius
        val spin = worldTime * 0.42f

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = colorScratchA.set(0f, 0f, 0f, 0.18f)
        shapes.circle(cx + 8f, cy - 10f, radius * 1.02f, sides)

        val glowLayers = 5
        for (layer in glowLayers downTo 1) {
            val f = layer / glowLayers.toFloat()
            val r = MathUtils.lerp(voidRadius * 1.02f, diskOuter, f)
            shapes.color = colorScratchA.set(0.96f, 0.58f, 0.30f, 0.06f + (1f - f) * 0.12f)
            shapes.circle(cx, cy, r, sides)
        }
        val hotLayers = 4
        for (layer in hotLayers downTo 1) {
            val f = layer / hotLayers.toFloat()
            val r = MathUtils.lerp(voidRadius * 1.02f, voidRadius * 1.62f, f)
            shapes.color = colorScratchA.set(1f, 0.82f, 0.54f, 0.14f + (1f - f) * 0.24f)
            shapes.circle(cx, cy, r, sides)
        }

        shapes.color = colorScratchA.set(0.20f, 0.86f, 1f, 0.82f + 0.12f * sin(worldTime * 2.1f))
        drawLoaderArc(cx, cy, voidRadius * 1.5f, voidRadius * 1.2f, spin, MathUtils.PI * 1.2f)
        shapes.color = colorScratchA.set(0.55f, 0.96f, 1f, 0.62f + 0.12f * sin(worldTime * 2.5f + 1f))
        drawLoaderArc(cx, cy, voidRadius * 1.32f, voidRadius * 1.1f, -spin * 1.25f + 2.4f, MathUtils.PI * 0.82f)

        shapes.color = colorScratchA.set(0.015f, 0.025f, 0.045f, 0.92f)
        shapes.circle(cx, cy, voidRadius * 1.08f, sides)
        val ringPulse = 0.7f + 0.3f * sin(worldTime * 2.8f)
        shapes.color = colorScratchA.set(1f, 0.87f, 0.64f, 0.5f + 0.22f * ringPulse)
        shapes.circle(cx, cy, voidRadius * 1.06f, sides)
        shapes.color = colorScratchA.set(0f, 0f, 0f, 1f)
        shapes.circle(cx, cy, voidRadius, sides)
        shapes.end()
    }

    private fun drawPreviewArena(palette: NeonPalette, level: LevelConfig, rotationRad: Float) {
        val layout = previewArenaLayout()
        val cx = layout.cx
        val cy = layout.cy
        val arenaRadiusPixels = layout.radius
        val sectorCount = level.sectorCount.coerceIn(6, 24)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (index in 0..2) {
            val ringPhase = (worldTime * (0.18f + index * 0.04f) + index * 0.22f) % 1f
            drawObstacleRing(
                cx = cx,
                cy = cy,
                arenaRadiusPixels = arenaRadiusPixels,
                yScale = layout.yScale,
                obstacle = Obstacle(
                    sectorCount = sectorCount,
                    radius = 1.18f - ringPhase * 0.92f,
                    speed = 0f,
                    thickness = (level.obstacleThickness * (1.05f - index * 0.14f)).coerceAtLeast(0.05f),
                    gapStartSector = (((worldTime * (1.6f + index * 0.3f)) + index * 3f).toInt() % sectorCount),
                    gapSectorCount = level.gapSectorCount.coerceAtLeast(1),
                    patternId = "preview",
                    rotationRad = rotationRad + index * 0.12f
                ),
                arenaRotationRad = 0f,
                contextLevelIndex = level.index,
                color = Color(colorForPattern(if (index == 2) "pulse_ring" else "clean_arc", palette)).mul(
                    1f,
                    1f,
                    1f,
                    0.48f - index * 0.08f
                )
            )
        }
        drawCoreCluster(cx, cy, arenaRadiusPixels, layout.yScale, guideSidesForLevel(level.sectorCount), rotationRad, false)
        shapes.end()
        drawNeedle(cx, cy, arenaRadiusPixels, layout.yScale, worldTime * 1.2f, palette, false)
    }

    private fun drawObstacleRing(
        cx: Float,
        cy: Float,
        arenaRadiusPixels: Float,
        yScale: Float,
        obstacle: Obstacle,
        arenaRotationRad: Float,
        contextLevelIndex: Int,
        color: Color
    ) {
        val isGravityWall = obstacle.patternId == "gravity_pull"
        val isTimeBubbleWall = obstacle.patternId == "time_bubble"
        val isMissileVolley = obstacle.patternId == "missile_volley"
        val isFlyTheme = contextLevelIndex in 21..30
        val isBioTheme = contextLevelIndex in 71..80
        val isKitchenTheme = contextLevelIndex in 91..100
        val sectorAngle = MathUtils.PI2 / obstacle.sectorCount.toFloat()
        if (obstacle.gapSectorCount >= obstacle.sectorCount) {
            return
        }

        val outer = obstacle.radius * arenaRadiusPixels
        val inner = (obstacle.radius - obstacle.thickness).coerceAtLeast(0f) * arenaRadiusPixels
        val visibilityCutoff = if (contextLevelIndex in 31..40) 2.54f else 2.68f
        if (inner >= arenaRadiusPixels * visibilityCutoff) {
            return
        }
        if (outer <= inner + 0.5f) {
            return
        }

        val threatProximity = (1f - kotlin.math.abs(obstacle.radius - simulation.playerOrbitRadiusNormalized) / 0.44f)
            .coerceIn(0f, 1f)
        val ringPulse = 0.5f + 0.5f * sin(worldTime * (if (isGravityWall) 3.3f else 4.2f) + obstacle.rotationRad * 1.1f)
        val ringFlicker = 0.9f + sin(worldTime * 9.5f + obstacle.radius * 6.2f) * 0.06f
        val dangerColor = Color(color).mul(
            1f,
            1f,
            1f,
            (
                color.a *
                    (
                        if (isGravityWall) {
                            0.68f
                        } else if (isTimeBubbleWall) {
                            0.64f
                        } else {
                            0.76f + threatProximity * 0.24f
                        }
                        ) * ringFlicker
                ).coerceIn(0f, 1f)
        )
        val bandDepth = (outer - inner).coerceAtLeast(2f)
        val gapStart = arenaRotationRad + obstacle.rotationRad + obstacle.gapStartSector * sectorAngle
        val gapSpan = (obstacle.gapSectorCount * sectorAngle).coerceIn(0f, MathUtils.PI2 - 0.02f)
        val blockedStart = gapStart + gapSpan
        val blockedSpan = (MathUtils.PI2 - gapSpan).coerceAtLeast(0.02f)
        val arcPixels = blockedSpan * outer
        val renderSegments = kotlin.math.max(
            ((blockedSpan / MathUtils.PI2) * 360f).toInt(),
            (arcPixels / 2.1f).toInt()
        ).coerceIn(140, 560)

        if (isKitchenTheme) {
            drawBreadArcBand(
                cx = cx,
                cy = cy,
                outerRadius = outer,
                innerRadius = inner,
                startAngleRad = blockedStart,
                spanAngleRad = blockedSpan,
                yScale = yScale,
                fillColor = dangerColor,
                pulse = ringPulse
            )
            return
        }

        if (isBioTheme) {
            val squareGlow = Color(0.26f, 0.92f, 0.78f, (0.1f + ringPulse * 0.1f).coerceIn(0.08f, 0.2f))
            drawAngularBioObstacleBand(
                cx = cx,
                cy = cy,
                outerRadius = outer,
                innerRadius = inner,
                startAngleRad = blockedStart,
                spanAngleRad = blockedSpan,
                yScale = yScale,
                fillColor = dangerColor,
                glowColor = squareGlow,
                pulse = ringPulse
            )
            return
        }

        // Continuous single-surface barrier (no per-sector geometry).
        shapes.color = Color(0f, 0f, 0f, dangerColor.a * 0.24f)
        drawSmoothArcBand(
            cx = cx,
            cy = cy,
            outerRadius = outer + bandDepth * 0.03f,
            innerRadius = inner + bandDepth * 0.03f,
            startAngleRad = blockedStart,
            spanAngleRad = blockedSpan,
            yScale = yScale,
            segments = renderSegments
        )

        shapes.color = dangerColor
        drawSmoothArcBand(
            cx = cx,
            cy = cy,
            outerRadius = outer,
            innerRadius = inner,
            startAngleRad = blockedStart,
            spanAngleRad = blockedSpan,
            yScale = yScale,
            segments = renderSegments
        )

        // Subtle depth gradient without crack/subdivision lines.
        shapes.color = Color(0f, 0f, 0f, dangerColor.a * 0.16f)
        drawSmoothArcBand(
            cx = cx,
            cy = cy,
            outerRadius = inner + bandDepth * 0.58f,
            innerRadius = inner,
            startAngleRad = blockedStart,
            spanAngleRad = blockedSpan,
            yScale = yScale,
            segments = renderSegments
        )
        shapes.color = if (isGravityWall) {
            Color(0.62f, 0.88f, 1f, (0.1f + ringPulse * 0.1f).coerceIn(0.08f, 0.22f))
        } else if (isTimeBubbleWall) {
            Color(0.72f, 0.8f, 1f, (0.1f + ringPulse * 0.1f).coerceIn(0.08f, 0.22f))
        } else {
            Color(1f, 0.78f, 0.48f, (0.08f + ringPulse * 0.08f).coerceIn(0.08f, 0.18f))
        }
        drawSmoothArcBand(
            cx = cx,
            cy = cy,
            outerRadius = outer,
            innerRadius = (outer - bandDepth * 0.28f).coerceAtLeast(inner),
            startAngleRad = blockedStart,
            spanAngleRad = blockedSpan,
            yScale = yScale,
            segments = renderSegments
        )

        val edgeGlowColor = if (isGravityWall) {
            lerpColor(
                Color(0.45f, 0.78f, 1f, 0.14f),
                Color(0.76f, 0.9f, 1f, 0.32f),
                ringPulse
            )
        } else if (isTimeBubbleWall) {
            lerpColor(
                Color(0.66f, 0.72f, 1f, 0.16f),
                Color(0.86f, 0.88f, 1f, 0.3f),
                ringPulse
            )
        } else {
            lerpColor(
                Color(reactorDanger).mul(1f, 1f, 1f, 0.12f),
                Color(1f, 0.76f, 0.36f, 0.26f),
                ringPulse
            )
        }
        shapes.color = edgeGlowColor
        drawSmoothArcBand(
            cx = cx,
            cy = cy,
            outerRadius = outer + bandDepth * 0.1f,
            innerRadius = outer - bandDepth * 0.02f,
            startAngleRad = blockedStart,
            spanAngleRad = blockedSpan,
            yScale = yScale,
            segments = renderSegments
        )

        if (isFlyTheme) {
            val meshColor = Color(0.92f, 1f, 0.9f, (0.12f + ringPulse * 0.14f).coerceIn(0.08f, 0.28f))
            val radialCount = 5
            val ringCount = 3
            val meshThickness = (1.1f + bandDepth * 0.024f).coerceAtLeast(1f)
            for (index in 0..radialCount) {
                val t = index.toFloat() / radialCount.toFloat()
                val angle = blockedStart + blockedSpan * t
                val x1 = orbitX(cx, angle, inner + bandDepth * 0.06f)
                val y1 = orbitY(cy, angle, inner + bandDepth * 0.06f, yScale)
                val x2 = orbitX(cx, angle, outer - bandDepth * 0.08f)
                val y2 = orbitY(cy, angle, outer - bandDepth * 0.08f, yScale)
                shapes.color = meshColor
                shapes.rectLine(x1, y1, x2, y2, meshThickness)
            }
            for (ringIndex in 1..ringCount) {
                val ringT = ringIndex.toFloat() / (ringCount + 1).toFloat()
                val ringOuter = inner + bandDepth * (ringT + 0.03f)
                val ringInner = (ringOuter - bandDepth * 0.04f).coerceAtLeast(inner)
                shapes.color = Color(meshColor).mul(1f, 1f, 1f, 0.8f)
                drawSmoothArcBand(
                    cx = cx,
                    cy = cy,
                    outerRadius = ringOuter,
                    innerRadius = ringInner,
                    startAngleRad = blockedStart,
                    spanAngleRad = blockedSpan,
                    yScale = yScale,
                    segments = renderSegments
                )
            }
        }

        if (contextLevelIndex in 61..70 && isMissileVolley) {
            val missileCount = (blockedSpan / (MathUtils.PI / 6f)).toInt().coerceIn(3, 8)
            val missileBaseColor = Color(0.98f, 0.48f, 0.28f, (0.7f + ringPulse * 0.26f).coerceIn(0.66f, 0.92f))
            val missileCoreColor = Color(1f, 0.86f, 0.58f, 0.9f)
            val missileLength = (bandDepth * 1.35f).coerceIn(7f, 24f)
            val missileWidth = (bandDepth * 0.42f).coerceIn(2f, 9f)
            for (index in 0 until missileCount) {
                val t = (index + 1).toFloat() / (missileCount + 1).toFloat()
                val angle = blockedStart + blockedSpan * t
                val noseRadius = inner + bandDepth * 0.28f
                val tailRadius = (noseRadius + missileLength).coerceAtMost(outer - bandDepth * 0.08f)
                val sideSpread = missileWidth / noseRadius.coerceAtLeast(1f)
                val leftAngle = angle - sideSpread
                val rightAngle = angle + sideSpread
                val noseX = orbitX(cx, angle, noseRadius)
                val noseY = orbitY(cy, angle, noseRadius, yScale)
                val leftX = orbitX(cx, leftAngle, tailRadius)
                val leftY = orbitY(cy, leftAngle, tailRadius, yScale)
                val rightX = orbitX(cx, rightAngle, tailRadius)
                val rightY = orbitY(cy, rightAngle, tailRadius, yScale)
                shapes.color = missileBaseColor
                shapes.triangle(noseX, noseY, leftX, leftY, rightX, rightY)
                val coreTailRadius = (noseRadius + missileLength * 0.58f).coerceAtMost(tailRadius)
                val coreSpread = sideSpread * 0.58f
                val coreLeftX = orbitX(cx, angle - coreSpread, coreTailRadius)
                val coreLeftY = orbitY(cy, angle - coreSpread, coreTailRadius, yScale)
                val coreRightX = orbitX(cx, angle + coreSpread, coreTailRadius)
                val coreRightY = orbitY(cy, angle + coreSpread, coreTailRadius, yScale)
                shapes.color = missileCoreColor
                shapes.triangle(noseX, noseY, coreLeftX, coreLeftY, coreRightX, coreRightY)
            }
        }
    }

    private fun drawMissileHazard(
        cx: Float,
        cy: Float,
        arenaRadiusPixels: Float,
        yScale: Float,
        angleRad: Float,
        radiusNorm: Float,
        pulse: Float
    ) {
        val noseRadius = radiusNorm * arenaRadiusPixels
        val bodyLength = (arenaRadiusPixels * 0.052f).coerceIn(8f, 22f)
        val tailRadius = (noseRadius + bodyLength).coerceAtLeast(noseRadius + 4f)
        val spread = (0.016f + (1f - radiusNorm).coerceIn(0f, 1f) * 0.012f).coerceIn(0.014f, 0.03f)
        val leftAngle = angleRad - spread
        val rightAngle = angleRad + spread
        val noseX = orbitX(cx, angleRad, noseRadius)
        val noseY = orbitY(cy, angleRad, noseRadius, yScale)
        val leftX = orbitX(cx, leftAngle, tailRadius)
        val leftY = orbitY(cy, leftAngle, tailRadius, yScale)
        val rightX = orbitX(cx, rightAngle, tailRadius)
        val rightY = orbitY(cy, rightAngle, tailRadius, yScale)

        shapes.color = Color(0.96f, 0.44f, 0.26f, (0.7f + pulse * 0.22f).coerceIn(0.64f, 0.94f))
        shapes.triangle(noseX, noseY, leftX, leftY, rightX, rightY)

        val coreTailRadius = noseRadius + bodyLength * 0.56f
        val coreSpread = spread * 0.56f
        val coreLeftX = orbitX(cx, angleRad - coreSpread, coreTailRadius)
        val coreLeftY = orbitY(cy, angleRad - coreSpread, coreTailRadius, yScale)
        val coreRightX = orbitX(cx, angleRad + coreSpread, coreTailRadius)
        val coreRightY = orbitY(cy, angleRad + coreSpread, coreTailRadius, yScale)
        shapes.color = Color(1f, 0.9f, 0.62f, 0.9f)
        shapes.triangle(noseX, noseY, coreLeftX, coreLeftY, coreRightX, coreRightY)
    }

    private fun drawEnemyLaserHazard(
        cx: Float,
        cy: Float,
        arenaRadiusPixels: Float,
        yScale: Float,
        angleRad: Float,
        radiusNorm: Float,
        pulse: Float
    ) {
        val noseRadius = radiusNorm * arenaRadiusPixels
        val bodyLength = (arenaRadiusPixels * 0.064f).coerceIn(10f, 26f)
        val tailRadius = (noseRadius + bodyLength).coerceAtLeast(noseRadius + 5f)
        val spread = (0.013f + (1f - radiusNorm).coerceIn(0f, 1f) * 0.01f).coerceIn(0.012f, 0.025f)
        val leftAngle = angleRad - spread
        val rightAngle = angleRad + spread
        val noseX = orbitX(cx, angleRad, noseRadius)
        val noseY = orbitY(cy, angleRad, noseRadius, yScale)
        val leftX = orbitX(cx, leftAngle, tailRadius)
        val leftY = orbitY(cy, leftAngle, tailRadius, yScale)
        val rightX = orbitX(cx, rightAngle, tailRadius)
        val rightY = orbitY(cy, rightAngle, tailRadius, yScale)

        shapes.color = Color(0.56f, 0.96f, 1f, (0.74f + pulse * 0.18f).coerceIn(0.68f, 0.96f))
        shapes.triangle(noseX, noseY, leftX, leftY, rightX, rightY)

        val coreTailRadius = noseRadius + bodyLength * 0.66f
        val coreSpread = spread * 0.44f
        val coreLeftX = orbitX(cx, angleRad - coreSpread, coreTailRadius)
        val coreLeftY = orbitY(cy, angleRad - coreSpread, coreTailRadius, yScale)
        val coreRightX = orbitX(cx, angleRad + coreSpread, coreTailRadius)
        val coreRightY = orbitY(cy, angleRad + coreSpread, coreTailRadius, yScale)
        shapes.color = Color(0.92f, 0.78f, 1f, 0.94f)
        shapes.triangle(noseX, noseY, coreLeftX, coreLeftY, coreRightX, coreRightY)
    }

    private fun drawKnifeHazard(
        cx: Float,
        cy: Float,
        arenaRadiusPixels: Float,
        yScale: Float,
        angleRad: Float,
        radiusNorm: Float,
        pulse: Float
    ) {
        val centerRadius = radiusNorm * arenaRadiusPixels
        val knifeLen = (arenaRadiusPixels * 0.14f).coerceIn(18f, 52f)
        val knifeWidth = (arenaRadiusPixels * 0.03f).coerceIn(6f, 14f)
        val headRadius = (centerRadius - knifeLen * 0.28f).coerceAtLeast(0f)
        val tailRadius = centerRadius + knifeLen * 0.72f
        val spread = (knifeWidth / centerRadius.coerceAtLeast(1f)).coerceIn(0.012f, 0.042f)
        val leftAngle = angleRad - spread
        val rightAngle = angleRad + spread
        val tipX = orbitX(cx, angleRad, headRadius)
        val tipY = orbitY(cy, angleRad, headRadius, yScale)
        val leftX = orbitX(cx, leftAngle, tailRadius)
        val leftY = orbitY(cy, leftAngle, tailRadius, yScale)
        val rightX = orbitX(cx, rightAngle, tailRadius)
        val rightY = orbitY(cy, rightAngle, tailRadius, yScale)
        val handleRadius = centerRadius + knifeLen * 0.92f
        val handleSpread = spread * 1.26f
        val handleLeftX = orbitX(cx, angleRad - handleSpread, handleRadius)
        val handleLeftY = orbitY(cy, angleRad - handleSpread, handleRadius, yScale)
        val handleRightX = orbitX(cx, angleRad + handleSpread, handleRadius)
        val handleRightY = orbitY(cy, angleRad + handleSpread, handleRadius, yScale)

        shapes.color = Color(0.84f, 0.88f, 0.94f, (0.72f + pulse * 0.18f).coerceIn(0.66f, 0.94f))
        shapes.triangle(tipX, tipY, leftX, leftY, rightX, rightY)
        shapes.color = Color(0.54f, 0.58f, 0.64f, 0.86f)
        shapes.triangle(leftX, leftY, handleLeftX, handleLeftY, handleRightX, handleRightY)
        shapes.triangle(leftX, leftY, rightX, rightY, handleRightX, handleRightY)
        shapes.color = Color(0.38f, 0.22f, 0.1f, 0.92f)
        shapes.triangle(handleLeftX, handleLeftY, handleRightX, handleRightY, orbitX(cx, angleRad, handleRadius + knifeLen * 0.18f), orbitY(cy, angleRad, handleRadius + knifeLen * 0.18f, yScale))
    }

    private fun drawEnemyShipEmitter(
        cx: Float,
        cy: Float,
        arenaRadiusPixels: Float,
        yScale: Float,
        angleRad: Float,
        palette: NeonPalette,
        variant: Int
    ) {
        val safetyMargin = sx(42f).coerceIn(26f, 54f)
        val yScaleSafe = yScale.coerceAtLeast(0.1f)
        val maxVisibleRadiusX = minOf(
            cx - safetyMargin,
            viewport.worldWidth - cx - safetyMargin
        )
        val maxVisibleRadiusY = minOf(
            (cy - safetyMargin) / yScaleSafe,
            (viewport.worldHeight - cy - safetyMargin) / yScaleSafe
        )
        val maxVisibleRadius = minOf(maxVisibleRadiusX, maxVisibleRadiusY).coerceAtLeast(arenaRadiusPixels * 0.94f)
        val desiredRadius = arenaRadiusPixels * 1.18f
        val shipRadius = desiredRadius.coerceAtMost(maxVisibleRadius)
        val shipX = orbitX(cx, angleRad, shipRadius)
        val shipY = orbitY(cy, angleRad, shipRadius, yScale)
        val facingCenterAngle = angleRad + MathUtils.PI
        val activeStyle = shipStyleIndexForId(activeShipSkin()?.id ?: "specter_7")
        val styleModulo = if (shipSkins.isNotEmpty()) shipSkins.size else 11
        var enemyStyle = (variant + 3).mod(styleModulo)
        if (enemyStyle == activeStyle % styleModulo) {
            enemyStyle = (enemyStyle + 1).mod(styleModulo)
        }

        shapes.color = Color(0.44f, 0.88f, 1f, 0.2f)
        shapes.circle(shipX, shipY, sx(24f).coerceIn(14f, 30f), 22)
        drawCodeShipModel(
            centerX = shipX,
            centerY = shipY,
            angleRad = facingCenterAngle,
            yScale = yScale,
            modelScale = (arenaRadiusPixels * 0.00182f).coerceIn(0.2f, 0.46f),
            levelIndex = simulation.levelConfig.index,
            palette = palette,
            phaseActive = false,
            dimmed = false,
            shipStyleIndex = enemyStyle
        )
    }

    private fun drawSmoothArcBand(
        cx: Float,
        cy: Float,
        outerRadius: Float,
        innerRadius: Float,
        startAngleRad: Float,
        spanAngleRad: Float,
        yScale: Float,
        segments: Int
    ) {
        if (outerRadius <= innerRadius || spanAngleRad <= 0f) {
            return
        }

        val byPixelDensity = (spanAngleRad * outerRadius / 1.75f).toInt()
        val steps = kotlin.math.max(segments, byPixelDensity).coerceIn(48, 720)
        var step = 0
        while (step < steps) {
            val t0 = startAngleRad + spanAngleRad * (step / steps.toFloat())
            val t1 = startAngleRad + spanAngleRad * ((step + 1) / steps.toFloat())
            val x1 = orbitX(cx, t0, outerRadius)
            val y1 = orbitY(cy, t0, outerRadius, yScale)
            val x2 = orbitX(cx, t1, outerRadius)
            val y2 = orbitY(cy, t1, outerRadius, yScale)
            val x3 = orbitX(cx, t1, innerRadius)
            val y3 = orbitY(cy, t1, innerRadius, yScale)
            val x4 = orbitX(cx, t0, innerRadius)
            val y4 = orbitY(cy, t0, innerRadius, yScale)
            shapes.triangle(x1, y1, x2, y2, x3, y3)
            shapes.triangle(x1, y1, x4, y4, x3, y3)
            step += 1
        }
    }

    private fun drawAngularBioObstacleBand(
        cx: Float,
        cy: Float,
        outerRadius: Float,
        innerRadius: Float,
        startAngleRad: Float,
        spanAngleRad: Float,
        yScale: Float,
        fillColor: Color,
        glowColor: Color,
        pulse: Float
    ) {
        if (outerRadius <= innerRadius || spanAngleRad <= 0f) {
            return
        }
        val bandDepth = (outerRadius - innerRadius).coerceAtLeast(2f)
        val cornerCount = 4
        for (corner in 0 until cornerCount) {
            val t0 = corner / cornerCount.toFloat()
            val t1 = (corner + 1) / cornerCount.toFloat()
            val angle0 = startAngleRad + spanAngleRad * t0
            val angle1 = startAngleRad + spanAngleRad * t1
            val outerX0 = orbitX(cx, angle0, outerRadius)
            val outerY0 = orbitY(cy, angle0, outerRadius, yScale)
            val outerX1 = orbitX(cx, angle1, outerRadius)
            val outerY1 = orbitY(cy, angle1, outerRadius, yScale)
            val innerX0 = orbitX(cx, angle0, innerRadius)
            val innerY0 = orbitY(cy, angle0, innerRadius, yScale)
            val innerX1 = orbitX(cx, angle1, innerRadius)
            val innerY1 = orbitY(cy, angle1, innerRadius, yScale)
            val pulseAlpha = (0.84f + 0.16f * pulse).coerceIn(0.78f, 1f)
            shapes.color = Color(glowColor).mul(1f, 1f, 1f, (0.1f + pulse * 0.18f).coerceIn(0.08f, 0.3f))
            shapes.triangle(
                orbitX(cx, angle0, outerRadius + bandDepth * 0.06f),
                orbitY(cy, angle0, outerRadius + bandDepth * 0.06f, yScale),
                orbitX(cx, angle1, outerRadius + bandDepth * 0.06f),
                orbitY(cy, angle1, outerRadius + bandDepth * 0.06f, yScale),
                orbitX(cx, angle1, innerRadius - bandDepth * 0.04f),
                orbitY(cy, angle1, innerRadius - bandDepth * 0.04f, yScale)
            )
            shapes.triangle(
                orbitX(cx, angle0, outerRadius + bandDepth * 0.06f),
                orbitY(cy, angle0, outerRadius + bandDepth * 0.06f, yScale),
                orbitX(cx, angle0, innerRadius - bandDepth * 0.04f),
                orbitY(cy, angle0, innerRadius - bandDepth * 0.04f, yScale),
                orbitX(cx, angle1, innerRadius - bandDepth * 0.04f),
                orbitY(cy, angle1, innerRadius - bandDepth * 0.04f, yScale)
            )
            shapes.color = Color(fillColor).mul(1f, 1f, 1f, fillColor.a * pulseAlpha)
            shapes.triangle(outerX0, outerY0, outerX1, outerY1, innerX1, innerY1)
            shapes.triangle(outerX0, outerY0, innerX0, innerY0, innerX1, innerY1)
        }
    }

    private fun drawOrientedSquare(
        centerX: Float,
        centerY: Float,
        size: Float,
        tangentX: Float,
        tangentY: Float,
        radialX: Float,
        radialY: Float,
        color: Color
    ) {
        val hs = size * 0.5f
        val x1 = centerX - tangentX * hs - radialX * hs
        val y1 = centerY - tangentY * hs - radialY * hs
        val x2 = centerX + tangentX * hs - radialX * hs
        val y2 = centerY + tangentY * hs - radialY * hs
        val x3 = centerX + tangentX * hs + radialX * hs
        val y3 = centerY + tangentY * hs + radialY * hs
        val x4 = centerX - tangentX * hs + radialX * hs
        val y4 = centerY - tangentY * hs + radialY * hs
        shapes.color = color
        shapes.triangle(x1, y1, x2, y2, x3, y3)
        shapes.triangle(x1, y1, x4, y4, x3, y3)
    }

    private fun drawBreadArcBand(
        cx: Float,
        cy: Float,
        outerRadius: Float,
        innerRadius: Float,
        startAngleRad: Float,
        spanAngleRad: Float,
        yScale: Float,
        fillColor: Color,
        pulse: Float
    ) {
        if (outerRadius <= innerRadius || spanAngleRad <= 0f) {
            return
        }
        val bandDepth = (outerRadius - innerRadius).coerceAtLeast(2f)
        val crustColor = Color(0.68f, 0.42f, 0.2f, (0.84f + pulse * 0.12f).coerceIn(0.76f, 0.98f))
        val breadColor = Color(0.95f, 0.82f, 0.56f, fillColor.a.coerceIn(0.54f, 0.92f))
        val breadShadow = Color(0.74f, 0.52f, 0.3f, 0.34f)
        val toastMark = Color(0.78f, 0.56f, 0.32f, 0.5f)
        val segments = ((spanAngleRad * outerRadius) / 2.2f).toInt().coerceIn(52, 220)

        shapes.color = crustColor
        drawSmoothArcBand(
            cx = cx,
            cy = cy,
            outerRadius = outerRadius + bandDepth * 0.06f,
            innerRadius = innerRadius - bandDepth * 0.04f,
            startAngleRad = startAngleRad,
            spanAngleRad = spanAngleRad,
            yScale = yScale,
            segments = segments
        )
        shapes.color = breadColor
        drawSmoothArcBand(
            cx = cx,
            cy = cy,
            outerRadius = outerRadius - bandDepth * 0.18f,
            innerRadius = innerRadius + bandDepth * 0.2f,
            startAngleRad = startAngleRad,
            spanAngleRad = spanAngleRad,
            yScale = yScale,
            segments = segments
        )
        shapes.color = breadShadow
        drawSmoothArcBand(
            cx = cx,
            cy = cy,
            outerRadius = outerRadius - bandDepth * 0.18f,
            innerRadius = innerRadius + bandDepth * 0.48f,
            startAngleRad = startAngleRad,
            spanAngleRad = spanAngleRad,
            yScale = yScale,
            segments = segments
        )

        val grillCount = ((spanAngleRad * (innerRadius + outerRadius) * 0.5f) / 26f).toInt().coerceIn(8, 22)
        val grillThickness = (bandDepth * 0.08f).coerceIn(1.2f, 3.8f)
        for (index in 0 until grillCount) {
            val t = (index + 0.5f) / grillCount.toFloat()
            val angle = startAngleRad + spanAngleRad * t
            val x0 = orbitX(cx, angle, innerRadius + bandDepth * 0.28f)
            val y0 = orbitY(cy, angle, innerRadius + bandDepth * 0.28f, yScale)
            val x1 = orbitX(cx, angle, outerRadius - bandDepth * 0.26f)
            val y1 = orbitY(cy, angle, outerRadius - bandDepth * 0.26f, yScale)
            shapes.color = toastMark.cpy().mul(1f, 1f, 1f, 0.42f + 0.08f * ((index % 3) / 2f))
            shapes.rectLine(x0, y0, x1, y1, grillThickness)
        }

        val sesameCount = ((spanAngleRad * (innerRadius + outerRadius) * 0.5f) / 24f).toInt().coerceIn(8, 32)
        for (index in 0 until sesameCount) {
            val t = (index + 0.5f) / sesameCount.toFloat()
            val angle = startAngleRad + spanAngleRad * t
            val radius = innerRadius + bandDepth * (0.3f + 0.4f * ((index % 3) / 2f))
            val px = orbitX(cx, angle, radius)
            val py = orbitY(cy, angle, radius, yScale)
            val sesameSize = (bandDepth * 0.11f).coerceIn(1.5f, 4f)
            shapes.color = Color(0.99f, 0.9f, 0.66f, 0.82f)
            shapes.circle(px, py, sesameSize, 8)
        }
    }

    private fun asteroidNoise(index: Int, seed: Float): Float {
        val raw = kotlin.math.sin(index * 12.9898f + seed * 78.233f) * 43758.5453f
        val fract = raw - kotlin.math.floor(raw)
        return fract * 2f - 1f
    }

    private fun variantForSegment(obstacle: Obstacle, sectorIndex: Int): AsteroidVariant {
        val hash =
            obstacle.patternId.hashCode() * 31 +
                obstacle.sectorCount * 17 +
                obstacle.gapStartSector * 13 +
                sectorIndex * 101
        return when (kotlin.math.abs(hash) % 3) {
            0 -> AsteroidVariant.SLAB
            1 -> AsteroidVariant.CHIPPED
            else -> AsteroidVariant.DENSE
        }
    }

    private fun drawSegmentDetails(
        variant: AsteroidVariant,
        sectorIndex: Int,
        seed: Float,
        color: Color,
        thickness: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        x6: Float,
        y6: Float,
        x5: Float,
        y5: Float,
        x4: Float,
        y4: Float
    ) {
        val localSeed = seed + sectorIndex * 0.719f
        val jitterA = asteroidNoise(1, localSeed) * 0.05f
        val jitterB = asteroidNoise(2, localSeed) * 0.05f
        val jitterC = asteroidNoise(3, localSeed) * 0.05f

        when (variant) {
            AsteroidVariant.SLAB -> {
                drawSegmentStrokeClipped(
                    u0 = (0.22f + jitterA).coerceIn(0.1f, 0.42f),
                    v0 = 0.34f,
                    u1 = (0.78f + jitterB).coerceIn(0.58f, 0.9f),
                    v1 = 0.58f,
                    thickness = thickness,
                    color = color,
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    x3 = x3,
                    y3 = y3,
                    x6 = x6,
                    y6 = y6,
                    x5 = x5,
                    y5 = y5,
                    x4 = x4,
                    y4 = y4
                )
            }

            AsteroidVariant.CHIPPED -> {
                drawSegmentStrokeClipped(
                    u0 = (0.18f + jitterA).coerceIn(0.08f, 0.36f),
                    v0 = 0.3f,
                    u1 = (0.52f + jitterB).coerceIn(0.34f, 0.7f),
                    v1 = 0.52f,
                    thickness = thickness,
                    color = color,
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    x3 = x3,
                    y3 = y3,
                    x6 = x6,
                    y6 = y6,
                    x5 = x5,
                    y5 = y5,
                    x4 = x4,
                    y4 = y4
                )
                drawSegmentStrokeClipped(
                    u0 = (0.58f + jitterB).coerceIn(0.42f, 0.78f),
                    v0 = 0.42f,
                    u1 = (0.84f + jitterC).coerceIn(0.66f, 0.92f),
                    v1 = 0.7f,
                    thickness = thickness * 0.86f,
                    color = color,
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    x3 = x3,
                    y3 = y3,
                    x6 = x6,
                    y6 = y6,
                    x5 = x5,
                    y5 = y5,
                    x4 = x4,
                    y4 = y4
                )
            }

            AsteroidVariant.DENSE -> {
                drawSegmentStrokeClipped(
                    u0 = (0.16f + jitterA).coerceIn(0.08f, 0.3f),
                    v0 = 0.28f,
                    u1 = (0.44f + jitterB).coerceIn(0.28f, 0.6f),
                    v1 = 0.48f,
                    thickness = thickness * 0.92f,
                    color = color,
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    x3 = x3,
                    y3 = y3,
                    x6 = x6,
                    y6 = y6,
                    x5 = x5,
                    y5 = y5,
                    x4 = x4,
                    y4 = y4
                )
                drawSegmentStrokeClipped(
                    u0 = (0.4f + jitterB).coerceIn(0.24f, 0.62f),
                    v0 = 0.38f,
                    u1 = (0.68f + jitterC).coerceIn(0.52f, 0.84f),
                    v1 = 0.62f,
                    thickness = thickness * 0.78f,
                    color = color,
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    x3 = x3,
                    y3 = y3,
                    x6 = x6,
                    y6 = y6,
                    x5 = x5,
                    y5 = y5,
                    x4 = x4,
                    y4 = y4
                )
                drawSegmentStrokeClipped(
                    u0 = (0.62f + jitterA).coerceIn(0.46f, 0.84f),
                    v0 = 0.5f,
                    u1 = (0.9f + jitterC).coerceIn(0.72f, 0.94f),
                    v1 = 0.76f,
                    thickness = thickness * 0.72f,
                    color = color,
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    x3 = x3,
                    y3 = y3,
                    x6 = x6,
                    y6 = y6,
                    x5 = x5,
                    y5 = y5,
                    x4 = x4,
                    y4 = y4
                )
            }
        }
    }

    private fun drawSegmentStrokeClipped(
        u0: Float,
        v0: Float,
        u1: Float,
        v1: Float,
        thickness: Float,
        color: Color,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        x6: Float,
        y6: Float,
        x5: Float,
        y5: Float,
        x4: Float,
        y4: Float
    ) {
        val sx = segmentPointX(u0, v0, x1, x2, x3, x6, x5, x4)
        val sy = segmentPointY(u0, v0, y1, y2, y3, y6, y5, y4)
        val ex = segmentPointX(u1, v1, x1, x2, x3, x6, x5, x4)
        val ey = segmentPointY(u1, v1, y1, y2, y3, y6, y5, y4)
        drawThickLineQuad(sx, sy, ex, ey, thickness, color)
    }

    private fun segmentPointX(
        u: Float,
        v: Float,
        outerStart: Float,
        outerMid: Float,
        outerEnd: Float,
        innerStart: Float,
        innerMid: Float,
        innerEnd: Float
    ): Float {
        val t = u.coerceIn(0f, 1f)
        val inv = 1f - t
        val outer = inv * inv * outerStart + 2f * inv * t * outerMid + t * t * outerEnd
        val inner = inv * inv * innerStart + 2f * inv * t * innerMid + t * t * innerEnd
        return outer + (inner - outer) * v.coerceIn(0f, 1f)
    }

    private fun segmentPointY(
        u: Float,
        v: Float,
        outerStart: Float,
        outerMid: Float,
        outerEnd: Float,
        innerStart: Float,
        innerMid: Float,
        innerEnd: Float
    ): Float {
        val t = u.coerceIn(0f, 1f)
        val inv = 1f - t
        val outer = inv * inv * outerStart + 2f * inv * t * outerMid + t * t * outerEnd
        val inner = inv * inv * innerStart + 2f * inv * t * innerMid + t * t * innerEnd
        return outer + (inner - outer) * v.coerceIn(0f, 1f)
    }

    private fun drawThickLineQuad(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Float,
        color: Color
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceAtLeast(0.0001f)
        val nx = -dy / length * thickness * 0.5f
        val ny = dx / length * thickness * 0.5f

        val ax = x1 + nx
        val ay = y1 + ny
        val bx = x2 + nx
        val by = y2 + ny
        val cx = x2 - nx
        val cy = y2 - ny
        val dx2 = x1 - nx
        val dy2 = y1 - ny

        shapes.color = color
        shapes.triangle(ax, ay, bx, by, cx, cy)
        shapes.triangle(ax, ay, dx2, dy2, cx, cy)
    }

    private fun drawRadialBandMarker(
        cx: Float,
        cy: Float,
        innerRadius: Float,
        outerRadius: Float,
        angle: Float,
        yScale: Float,
        color: Color
    ) {
        val spread = 0.012f
        val a0 = angle - spread
        val a1 = angle + spread
        val x1 = orbitX(cx, a0, outerRadius)
        val y1 = orbitY(cy, a0, outerRadius, yScale)
        val x2 = orbitX(cx, a1, outerRadius)
        val y2 = orbitY(cy, a1, outerRadius, yScale)
        val x3 = orbitX(cx, a1, innerRadius)
        val y3 = orbitY(cy, a1, innerRadius, yScale)
        val x4 = orbitX(cx, a0, innerRadius)
        val y4 = orbitY(cy, a0, innerRadius, yScale)
        shapes.color = color
        shapes.triangle(x1, y1, x2, y2, x3, y3)
        shapes.triangle(x1, y1, x4, y4, x3, y3)
    }

    private fun drawCoreCluster(
        cx: Float,
        cy: Float,
        arenaRadiusPixels: Float,
        yScale: Float,
        guideSides: Int,
        rotationRad: Float,
        phaseActive: Boolean
    ) {
        val coreSides = guideSides.coerceAtLeast(48)
        val absorbRadius = arenaRadiusPixels * GameSimulation.BLACK_HOLE_ABSORB_RADIUS
        val baseVoidRadius = arenaRadiusPixels * GameSimulation.BLACK_HOLE_VISUAL_RADIUS
        // Slow, steady breathing — no abrupt starts/stops.
        val pulse = 1f + 0.022f * sin(worldTime * 1.5f)
        val voidRadius = baseVoidRadius * pulse
        val phaseBoost = if (phaseActive) 1.07f else 1f
        val diskOuter = absorbRadius * (1.14f + 0.018f * sin(worldTime * 1.1f)) * phaseBoost
        // The accretion disk spins continuously on its own clock, fully decoupled from
        // the gameplay arena rotation so it never jerks, reverses or freezes.
        val spin = worldTime * 0.42f

        // 1) Soft outer accretion glow: translucent discs stack into a radial gradient.
        val glowLayers = 5
        for (layer in glowLayers downTo 1) {
            val f = layer / glowLayers.toFloat()
            val r = MathUtils.lerp(voidRadius * 1.02f, diskOuter, f)
            val a = 0.05f + (1f - f) * 0.11f
            shapes.color = colorScratchA.set(0.96f, 0.58f, 0.30f, a)
            drawFilledPolygon(cx, cy, r, coreSides, 0f, yScale)
        }

        // 2) Hotter inner disk close to the horizon (warm white).
        val hotLayers = 4
        for (layer in hotLayers downTo 1) {
            val f = layer / hotLayers.toFloat()
            val r = MathUtils.lerp(voidRadius * 1.02f, voidRadius * 1.6f, f)
            val a = 0.12f + (1f - f) * 0.24f
            shapes.color = colorScratchA.set(1f, 0.82f, 0.54f, a)
            drawFilledPolygon(cx, cy, r, coreSides, 0f, yScale)
        }

        // 3) Counter-rotating photon streams in a vivid cyan that contrasts hard against
        //    the warm orange disk, so the moving parts read clearly (driven only by spin).
        shapes.color = colorScratchA.set(0.20f, 0.86f, 1f, 0.78f + 0.12f * sin(worldTime * 2.1f))
        drawCoreArc(cx, cy, voidRadius * 1.52f, voidRadius * 1.18f, 60, spin, MathUtils.PI * 1.2f, yScale)
        shapes.color = colorScratchA.set(0.55f, 0.96f, 1f, 0.6f + 0.12f * sin(worldTime * 2.5f + 1f))
        drawCoreArc(cx, cy, voidRadius * 1.34f, voidRadius * 1.1f, 60, -spin * 1.25f + 2.4f, MathUtils.PI * 0.82f, yScale)

        // 4) Shadow gap separating the disk from the void.
        shapes.color = colorScratchA.set(0.015f, 0.025f, 0.045f, 0.92f)
        drawFilledPolygon(cx, cy, voidRadius * 1.07f, coreSides, 0f, yScale)

        // 5) Photon ring — thin bright rim hugging the event horizon.
        val ringPulse = 0.7f + 0.3f * sin(worldTime * 2.8f)
        shapes.color = colorScratchA.set(1f, 0.87f, 0.64f, 0.45f + 0.22f * ringPulse)
        drawFilledPolygon(cx, cy, voidRadius * 1.05f, coreSides, 0f, yScale)

        // 6) Event horizon — the solid black hole.
        shapes.color = colorScratchA.set(0f, 0f, 0f, 1f)
        drawFilledPolygon(cx, cy, voidRadius, coreSides, 0f, yScale)
    }

    // Filled annular arc that respects the arena's yScale (perspective squash).
    private fun drawCoreArc(
        cx: Float,
        cy: Float,
        outerRadius: Float,
        innerRadius: Float,
        segments: Int,
        startAngle: Float,
        spanRad: Float,
        yScale: Float
    ) {
        val steps = segments.coerceAtLeast(4)
        for (step in 0 until steps) {
            val a0 = startAngle + spanRad * (step / steps.toFloat())
            val a1 = startAngle + spanRad * ((step + 1) / steps.toFloat())
            val ox1 = orbitX(cx, a0, outerRadius)
            val oy1 = orbitY(cy, a0, outerRadius, yScale)
            val ox2 = orbitX(cx, a1, outerRadius)
            val oy2 = orbitY(cy, a1, outerRadius, yScale)
            val ix1 = orbitX(cx, a0, innerRadius)
            val iy1 = orbitY(cy, a0, innerRadius, yScale)
            val ix2 = orbitX(cx, a1, innerRadius)
            val iy2 = orbitY(cy, a1, innerRadius, yScale)
            shapes.triangle(ox1, oy1, ox2, oy2, ix2, iy2)
            shapes.triangle(ox1, oy1, ix2, iy2, ix1, iy1)
        }
    }

    private fun drawArenaScaffold(
        cx: Float,
        cy: Float,
        arenaRadiusPixels: Float,
        yScale: Float,
        sectorCount: Int,
        rotationRad: Float,
        palette: NeonPalette,
        phaseActive: Boolean,
        phaseWarning: Boolean = false
    ) {
        val guideSides = guideSidesForLevel(sectorCount)
        val layers = arenaLineLayers(palette, phaseActive, phaseWarning)
        val guideRadius = arenaRadiusPixels * GameSimulation.PLAYER_COLLIDER.radius
        for (layer in layers) {
            if (!layer.enabled) {
                continue
            }
            shapes.color = Color(layer.color).mul(1f, 1f, 1f, layer.alpha)
            when (layer.purpose) {
                ArenaLinePurpose.SAFE_ORBIT_GUIDE -> {
                    drawPolygonOutline(cx, cy, guideRadius, guideSides, rotationRad, yScale)
                }
                ArenaLinePurpose.BOUNDARY_REINFORCE -> {
                    drawPolygonOutline(cx, cy, arenaRadiusPixels, guideSides, rotationRad, yScale)
                }
                ArenaLinePurpose.PROGRESS_CONTEXT -> {
                    val progressRadius = MathUtils.lerp(guideRadius, arenaRadiusPixels, simulation.runIntensity)
                    drawPolygonOutline(cx, cy, progressRadius, guideSides, rotationRad, yScale)
                }
                ArenaLinePurpose.TIMING_CADENCE -> {
                    val renderSectors = sectorCount.coerceIn(6, 24)
                    val markerCount = (renderSectors / 2).coerceAtLeast(6)
                    for (index in 0 until markerCount) {
                        val angle = rotationRad + MathUtils.PI2 * index.toFloat() / markerCount.toFloat()
                        shapes.line(
                            orbitX(cx, angle, arenaRadiusPixels * 1.004f),
                            orbitY(cy, angle, arenaRadiusPixels * 1.004f, yScale),
                            orbitX(cx, angle, arenaRadiusPixels * 1.032f),
                            orbitY(cy, angle, arenaRadiusPixels * 1.032f, yScale)
                        )
                    }
                }
                ArenaLinePurpose.REVERSE_WARNING -> {
                    val renderSectors = sectorCount.coerceIn(6, 24)
                    val markerCount = (renderSectors / 2).coerceAtLeast(6)
                    for (index in 0 until markerCount) {
                        val angle = rotationRad + MathUtils.PI2 * index.toFloat() / markerCount.toFloat()
                        shapes.line(
                            orbitX(cx, angle, arenaRadiusPixels * 1.01f),
                            orbitY(cy, angle, arenaRadiusPixels * 1.01f, yScale),
                            orbitX(cx, angle, arenaRadiusPixels * 1.08f),
                            orbitY(cy, angle, arenaRadiusPixels * 1.08f, yScale)
                        )
                    }
                }
            }
        }
    }

    private fun arenaLineLayers(
        palette: NeonPalette,
        phaseActive: Boolean,
        phaseWarning: Boolean
    ): List<ArenaLineLayer> {
        val runIsLive = simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING
        return listOf(
            ArenaLineLayer(
                purpose = ArenaLinePurpose.SAFE_ORBIT_GUIDE,
                color = reactorSafeGap,
                alpha = 0.12f,
                enabled = true
            ),
            ArenaLineLayer(
                purpose = ArenaLinePurpose.BOUNDARY_REINFORCE,
                color = reactorSafeGap,
                alpha = 0.18f,
                enabled = true
            ),
            ArenaLineLayer(
                purpose = ArenaLinePurpose.PROGRESS_CONTEXT,
                color = palette.uiAccent,
                alpha = 0.12f,
                enabled = runIsLive
            ),
            ArenaLineLayer(
                purpose = ArenaLinePurpose.TIMING_CADENCE,
                color = palette.grid,
                alpha = if (runIsLive) 0.06f else 0f,
                enabled = runIsLive
            ),
            ArenaLineLayer(
                purpose = ArenaLinePurpose.REVERSE_WARNING,
                color = if (phaseActive) palette.needlePhase else palette.uiAccent,
                alpha = if (phaseActive) 0.52f else if (phaseWarning) 0.34f else 0f,
                enabled = phaseActive || phaseWarning
            )
        )
    }

    private fun drawSpeedEdgeEffects(palette: NeonPalette) {
        val runLive = simulation.runPhase == RunPhase.RUNNING || simulation.runPhase == RunPhase.DRAINING
        if (!runLive) {
            return
        }
        val intensity = (motionSpeedIntensity * speedEdgeGate).coerceIn(0f, 1.45f)
        if (speedEdgeGate < 0.18f || intensity < 0.1f) {
            return
        }
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        val streakCount = (12 + intensity * 16f).toInt().coerceIn(12, 26)
        val edgeBand = sx(26f + intensity * 24f)
        val skew = sy(12f + intensity * 34f) * motionSignedIntensity.coerceIn(-1f, 1f)
        val speedColor = colorScratchA.set(palette.uiAccent).lerp(palette.needlePhase, 0.35f + intensity * 0.25f)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = colorScratchB.set(speedColor).mul(1f, 1f, 1f, (0.04f + intensity * 0.05f).coerceIn(0f, 0.14f))
        shapes.rect(0f, 0f, edgeBand, h)
        shapes.rect(w - edgeBand, 0f, edgeBand, h)

        for (index in 0 until streakCount) {
            val progress = index.toFloat() / streakCount.toFloat()
            val phase = worldTime * (6.5f + intensity * 2.2f) + index * 0.72f
            val y = h * (0.1f + progress * 0.8f) + sin(phase) * sy(32f) * intensity * 0.42f
            val length = sx(52f + intensity * 86f) * (0.76f + 0.24f * sin(phase * 1.13f + 1.8f))
            val thickness = sy(2.4f + intensity * 4.2f)
            val alpha = (0.05f + intensity * 0.1f) * (0.58f + 0.42f * sin(phase + 0.7f))
            shapes.color = colorScratchB.set(speedColor).mul(1f, 1f, 1f, alpha.coerceIn(0.03f, 0.2f))
            shapes.rectLine(edgeBand * 0.16f, y, edgeBand * 0.16f + length, y + skew, thickness)
            shapes.rectLine(w - edgeBand * 0.16f, y, w - edgeBand * 0.16f - length, y - skew, thickness)
        }
        shapes.end()
    }

    private fun drawFlashOverlay(_palette: NeonPalette) {
        val flashAlpha = screenFlashAlpha.coerceIn(0f, 0.44f)
        if (flashAlpha <= 0f) {
            return
        }
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val phaseMix = (0.12f + motionSpeedIntensity * 0.28f).coerceIn(0f, 0.66f)
        shapes.color = colorScratchA
            .set(flashColor)
            .lerp(Color.WHITE, phaseMix)
            .mul(1f, 1f, 1f, flashAlpha)
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        shapes.end()
    }

    private fun detectLevelFromTouch(x: Float, y: Float): Int? {
        for (index in levels.indices) {
            val cell = levelSelectCellRect(index)
            if (contains(cell, x, y)) {
                return index
            }
        }

        return null
    }

    private fun detectGroupedLevelFromTouch(x: Float, y: Float): Int? {
        val container = levelSelectBlockContainerRect()
        if (!contains(container, x, y)) {
            return null
        }
        val blockCount = (levels.size + 9) / 10
        for (blockIndex in 0 until blockCount) {
            val blockRect = levelBlockRect(blockIndex)
            if (blockRect.y < container.y || blockRect.y + blockRect.height > container.y + container.height) {
                continue
            }
            for (slot in 0 until 10) {
                val levelIndex = blockIndex * 10 + slot
                if (levelIndex >= levels.size) {
                    continue
                }
                val rect = levelBlockCellRect(blockIndex, slot)
                if (x in rect.x..(rect.x + rect.width) && y in rect.y..(rect.y + rect.height)) {
                    return levelIndex
                }
            }
        }
        return null
    }

    private fun detectShopShipFromTouch(x: Float, y: Float): Int? {
        for (index in shipSkins.indices) {
            if (contains(shopShipCellRect(index), x, y)) {
                return index
            }
        }
        return null
    }

    private fun detectShopShieldFromTouch(x: Float, y: Float): Int? {
        for (index in shieldStoreItems.indices) {
            if (contains(shopShieldCellRect(index), x, y)) {
                return index
            }
        }
        return null
    }

    private fun performShopPrimaryAction() {
        when (selectedShopCategory) {
            ShopCategory.SHIPS -> {
                val skin = shipSkins.getOrNull(selectedShopShipIndex) ?: return
                val unlocked = unlockedShipIds.contains(skin.id)
                if (!unlocked) {
                    if (coinBalance < skin.price) {
                        statusMessage = t("NOT ENOUGH COINS", "YETERSİZ COIN")
                        showShopNotice(
                            en = "INSUFFICIENT COINS",
                            tr = "PARA YETERSİZ"
                        )
                        return
                    }
                    coinBalance = (coinBalance - skin.price).coerceAtLeast(0)
                    unlockedShipIds.add(skin.id)
                    saveCoinBalance()
                }
                selectedShipId = skin.id
                syncSelectedShipHitbox()
                saveShipStoreState()
                statusMessage = if (unlocked) {
                    t("SHIP EQUIPPED", "GEMİ AKTİF")
                } else {
                    t("SHIP PURCHASED", "GEMİ SATIN ALINDI")
                }
                showShopNotice(
                    en = if (unlocked) "SHIP EQUIPPED" else "SHIP PURCHASED",
                    tr = if (unlocked) "GEMİ AKTİF" else "GEMİ SATIN ALINDI"
                )
            }

            ShopCategory.SHIELDS -> performShopSupportAction(SupportStoreKind.SHIELD)
        }
    }

    private fun performShopSupportAction(kind: SupportStoreKind) {
        val item = shieldStoreItems.firstOrNull { it.kind == kind } ?: return
        selectedShopShieldIndex = shieldStoreItems.indexOf(item).coerceAtLeast(0)
        val currentStock = if (kind == SupportStoreKind.SLOW) slowPowerCount else shieldCount
        val maxStock = if (kind == SupportStoreKind.SLOW) MAX_SLOW_POWERS else MAX_SHIELDS
        if (currentStock >= maxStock) {
            showShopNotice(
                en = if (kind == SupportStoreKind.SLOW) "TIME-SLOW STOCK FULL" else "SHIELDS ALREADY FULL",
                tr = if (kind == SupportStoreKind.SLOW) "ZAMAN YAVAŞLATMA STOKU DOLU" else "KALKAN STOKU ZATEN DOLU"
            )
            return
        }
        if (coinBalance < item.price) {
            showShopNotice(
                en = if (kind == SupportStoreKind.SLOW) {
                    "INSUFFICIENT COINS"
                } else if (adsEnabled()) {
                    "INSUFFICIENT COINS • WATCH AD FOR SHIELD"
                } else {
                    "INSUFFICIENT COINS"
                },
                tr = if (kind == SupportStoreKind.SLOW) {
                    "PARA YETERSİZ"
                } else if (adsEnabled()) {
                    "PARA YETERSİZ • KALKAN İÇİN REKLAM İZLE"
                } else {
                    "PARA YETERSİZ"
                }
            )
            statusMessage = t("NOT ENOUGH COINS", "YETERSİZ COIN")
            return
        }
        coinBalance = (coinBalance - item.price).coerceAtLeast(0)
        if (kind == SupportStoreKind.SLOW) {
            slowPowerCount = (slowPowerCount + item.amount).coerceAtMost(MAX_SLOW_POWERS)
            saveSlowPowerCount()
            statusMessage = t("TIME SLOW ADDED", "ZAMAN YAVAŞLATMA EKLENDİ")
            showShopNotice(
                en = "+${item.amount} TIME SLOW ADDED",
                tr = "+${item.amount} ZAMAN YAVAŞLATMA EKLENDİ"
            )
        } else {
            shieldCount = (shieldCount + item.amount).coerceAtMost(MAX_SHIELDS)
            saveShieldCount()
            statusMessage = t("SHIELDS PURCHASED", "KALKAN SATIN ALINDI")
            showShopNotice(
                en = "+${item.amount} SHIELD ADDED",
                tr = "+${item.amount} KALKAN EKLENDİ"
            )
        }
        saveCoinBalance()
    }

    private fun showShopNotice(en: String, tr: String, seconds: Float = 2.2f) {
        shopNoticeMessage = t(en, tr)
        shopNoticeTimer = seconds
    }

    private fun activeShipSkin(): ShipSkin? {
        return shipSkins.firstOrNull { it.id == selectedShipId } ?: shipSkins.firstOrNull()
    }

    private fun syncSelectedShipHitbox() {
        val radius = activeShipSkin()?.hitRadiusNorm ?: 0.03f
        simulation.setPlayerHitCircleRadius(radius)
    }

    private fun sx(value: Float): Float = value * (viewport.worldWidth / 1080f)

    private fun sy(value: Float): Float = value * (viewport.worldHeight / 1920f)

    private fun centeredPanelWidth(
        horizontalPadding: Float,
        minWidth: Float,
        maxWidth: Float
    ): Float {
        val available = (viewport.worldWidth - horizontalPadding * 2f).coerceAtLeast(minWidth)
        return available.coerceIn(minWidth, maxWidth)
    }

    private fun centeredPanelX(width: Float): Float = viewport.worldWidth * 0.5f - width * 0.5f

    private fun uiScaleTokens(): UiScaleTokens {
        val scale = (viewport.worldWidth / 1080f).coerceAtMost(viewport.worldHeight / 1920f).coerceIn(0.78f, 1.2f)
        return UiScaleTokens(
            xs = (8f * scale).coerceIn(6f, 12f),
            sm = (14f * scale).coerceIn(10f, 20f),
            md = (22f * scale).coerceIn(16f, 30f),
            lg = (32f * scale).coerceIn(22f, 42f),
            xl = (44f * scale).coerceIn(30f, 58f),
            insetX = sx(42f).coerceIn(24f, 72f),
            safeTop = sy(34f).coerceIn(26f, 56f),
            safeBottom = sy(40f).coerceIn(24f, 68f)
        )
    }

    private fun hudLayout(tokens: UiScaleTokens): HudLayoutModel {
        val hudWidth = (viewport.worldWidth - tokens.insetX * 2f).coerceAtLeast(sx(360f))
        val groupHeight = sy(118f).coerceIn(90f, 140f)
        val groupGap = tokens.sm
        var sideWidth = (hudWidth * 0.28f).coerceIn(sx(190f), sx(300f))
        var centerWidth = hudWidth - sideWidth * 2f - groupGap * 2f
        val minCenter = sx(250f).coerceAtMost(hudWidth * 0.44f)
        if (centerWidth < minCenter) {
            val needed = minCenter - centerWidth
            sideWidth = (sideWidth - needed * 0.5f).coerceAtLeast(sx(170f))
            centerWidth = hudWidth - sideWidth * 2f - groupGap * 2f
        }
        val y = viewport.worldHeight - tokens.safeTop - groupHeight - sy(84f)
        val startX = viewport.worldWidth * 0.5f - hudWidth * 0.5f
        val supportHeight = sy(74f).coerceIn(56f, 92f)
        val supportWidth = (hudWidth * 0.82f).coerceIn(sx(360f), hudWidth - sx(28f))
        val supportX = startX + hudWidth * 0.5f - supportWidth * 0.5f
        val supportY = y - tokens.md - supportHeight
        val progressHeight = sy(10f).coerceIn(8f, 14f)
        val progressY = supportY - tokens.sm - progressHeight

        return HudLayoutModel(
            leftGroup = UiRect(startX, y, sideWidth, groupHeight),
            centerGroup = UiRect(startX + sideWidth + groupGap, y, centerWidth, groupHeight),
            rightGroup = UiRect(startX + sideWidth + groupGap + centerWidth + groupGap, y, sideWidth, groupHeight),
            supportGroup = UiRect(supportX, supportY, supportWidth, supportHeight),
            progressTrack = UiRect(startX, progressY, hudWidth, progressHeight)
        )
    }

    private fun hudShieldToggleRect(hud: HudLayoutModel): UiRect {
        val slotWidth = hud.supportGroup.width / 3f
        val centerX = hud.supportGroup.x + slotWidth * 1.5f
        val width = (slotWidth * 0.98f).coerceIn(sx(112f), sx(200f))
        val height = sy(34f).coerceIn(26f, 42f)
        val x = centerX - width * 0.5f
        val y = hud.supportGroup.y + sy(2f).coerceIn(1f, 6f)
        return UiRect(x, y, width, height)
    }

    private fun centeredButtonRect(y: Float, width: Float = 560f, height: Float = 118f): UiRect {
        val horizontalPadding = (viewport.worldWidth * 0.06f).coerceIn(36f, 72f)
        val maxWidth = (viewport.worldWidth - horizontalPadding * 2f).coerceAtLeast(320f)
        val resolvedWidth = sx(width).coerceAtMost(maxWidth)
        val resolvedHeight = sy(height).coerceIn(92f, 132f)
        return UiRect(
            x = viewport.worldWidth * 0.5f - resolvedWidth * 0.5f,
            y = sy(y),
            width = resolvedWidth,
            height = resolvedHeight
        )
    }

    private fun pauseButtonRect(): UiRect {
        val tokens = uiScaleTokens()
        val w = sx(108f).coerceIn(82f, 126f)
        val h = sy(78f).coerceIn(62f, 96f)
        return UiRect(
            x = viewport.worldWidth - tokens.insetX * 0.2f - w,
            y = viewport.worldHeight - tokens.safeTop * 0.2f - h,
            width = w,
            height = h
        )
    }

    private fun pauseCardRect(): UiRect {
        val sidePad = sx(60f).coerceIn(32f, 72f)
        val width = centeredPanelWidth(sidePad, minWidth = 460f, maxWidth = 760f)
        // Sized to fit title + 4 action buttons on wide (iPad) aspects, where sy≈1 made
        // the old 620 height too short and the last button spilled below the card.
        val height = sy(664f).coerceIn(sy(560f), sy(780f))
        return UiRect(
            x = centeredPanelX(width),
            y = viewport.worldHeight * 0.5f - height * 0.5f,
            width = width,
            height = height
        )
    }

    private fun pauseActionRect(slot: Int): UiRect {
        val card = pauseCardRect()
        val width = card.width - sx(90f)
        val height = sy(92f).coerceIn(78f, 108f)
        val topInset = sy(152f).coerceIn(122f, 196f)
        val gap = sy(16f).coerceIn(12f, 24f)
        val startY = card.y + card.height - topInset - height
        return UiRect(
            x = card.x + sx(45f),
            y = startY - slot * (height + gap),
            width = width,
            height = height
        )
    }

    private fun pauseResumeRect(): UiRect = pauseActionRect(0)
    private fun pauseRestartRect(): UiRect = pauseActionRect(1)
    private fun pauseSettingsRect(): UiRect = pauseActionRect(2)
    private fun pauseMenuRect(): UiRect = pauseActionRect(3)

    private fun menuPanelRect(): UiRect {
        val sidePad = sx(56f).coerceIn(28f, 70f)
        val width = centeredPanelWidth(sidePad, minWidth = 860f, maxWidth = 940f)
        return UiRect(
            x = centeredPanelX(width),
            y = sy(262f),
            width = width,
            height = viewport.worldHeight - sy(392f)
        )
    }

    private fun menuButtonRect(slot: Int, total: Int = 6): UiRect {
        val panel = menuPanelRect()
        val width = panel.width - sx(88f)
        val buttonHeight = sy(102f).coerceIn(84f, 118f)
        val topInset = sy(176f).coerceIn(146f, 210f)
        val bottomInset = sy(56f).coerceIn(38f, 76f)
        val areaHeight = (panel.height - topInset - bottomInset).coerceAtLeast(buttonHeight * total)
        val gap = if (total > 1) {
            ((areaHeight - buttonHeight * total) / (total - 1)).coerceIn(sy(12f), sy(28f))
        } else {
            0f
        }
        val topY = panel.y + panel.height - topInset - buttonHeight
        return UiRect(
            x = panel.x + sx(44f),
            y = topY - slot * (buttonHeight + gap),
            width = width,
            height = buttonHeight
        )
    }

    private fun introHeroPanelRect(): UiRect {
        val topLift = sy(120f).coerceIn(84f, 164f)
        val sidePad = sx(52f).coerceIn(28f, 68f)
        val width = centeredPanelWidth(sidePad, minWidth = 840f, maxWidth = 980f)
        return UiRect(
            x = centeredPanelX(width),
            y = sy(1100f) + topLift,
            width = width,
            height = sy(456f)
        )
    }

    private fun introHeaderLift(): Float {
        return sy(48f).coerceIn(32f, 72f)
    }

    private fun introButtonsPanelRect(): UiRect {
        val sidePad = sx(52f).coerceIn(28f, 68f)
        val width = centeredPanelWidth(sidePad, minWidth = 840f, maxWidth = 980f)
        return UiRect(
            x = centeredPanelX(width),
            y = sy(118f),
            width = width,
            height = sy(506f)
        )
    }

    private fun introResourcePanelRect(): UiRect {
        val hero = introHeroPanelRect()
        val buttons = introButtonsPanelRect()
        val gap = sy(36f).coerceIn(sy(20f), sy(56f))
        val y = buttons.y + buttons.height + gap
        val top = hero.y - gap
        val height = (top - y).coerceIn(sy(320f), sy(540f))
        return UiRect(
            x = hero.x,
            y = y,
            width = hero.width,
            height = height
        )
    }

    private fun introResourceButtonRect(slot: Int): UiRect {
        val panel = introResourcePanelRect()
        val horizontalInset = sx(24f).coerceIn(16f, 36f)
        val buttonWidth = panel.width - horizontalInset * 2f
        val buttonHeight = sy(56f).coerceIn(46f, 68f)
        val topInset = sy(182f).coerceIn(142f, 230f)
        val bottomInset = sy(18f).coerceIn(12f, 28f)
        val slotCount = 3
        val available = (panel.height - topInset - bottomInset).coerceAtLeast(buttonHeight * slotCount)
        val gap = if (slotCount > 1) {
            ((available - buttonHeight * slotCount) / (slotCount - 1)).coerceIn(sy(12f), sy(20f))
        } else {
            0f
        }
        val stackBottom = panel.y + bottomInset
        val drawSlot = (slotCount - 1 - slot).coerceIn(0, slotCount - 1)
        return UiRect(
            x = panel.x + horizontalInset,
            y = stackBottom + drawSlot * (buttonHeight + gap),
            width = buttonWidth,
            height = buttonHeight
        )
    }

    private fun introButtonRect(slot: Int, total: Int = 4): UiRect {
        val panel = introButtonsPanelRect()
        val width = panel.width - sx(84f)
        val height = sy(102f).coerceIn(86f, 118f)
        val verticalPad = sy(24f).coerceIn(16f, 34f)
        val available = (panel.height - verticalPad * 2f).coerceAtLeast(height * total.toFloat())
        val gap = if (total > 1) {
            ((available - height * total.toFloat()) / (total - 1).toFloat()).coerceIn(sy(12f), sy(28f))
        } else {
            0f
        }
        val topY = panel.y + panel.height - verticalPad - height
        return UiRect(
            x = panel.x + sx(42f),
            y = topY - slot * (height + gap),
            width = width,
            height = height
        )
    }

    private fun introPlayButtonRect(): UiRect = introButtonRect(0, total = 4)
    private fun introSettingsButtonRect(): UiRect = introButtonRect(1, total = 4)
    private fun introShopButtonRect(): UiRect = introButtonRect(2, total = 4)
    private fun introPremiumButtonRect(): UiRect = introButtonRect(3, total = 4)
    private fun epilepsyWarningCardRect(): UiRect {
        val sidePad = sx(88f).coerceIn(42f, 124f)
        val width = centeredPanelWidth(sidePad, minWidth = 760f, maxWidth = 920f)
        val height = sy(760f).coerceIn(sy(560f), sy(840f))
        return UiRect(
            x = centeredPanelX(width),
            y = viewport.worldHeight * 0.5f - height * 0.5f + sy(48f),
            width = width,
            height = height
        )
    }

    private fun epilepsyContinueRect(): UiRect {
        val card = epilepsyWarningCardRect()
        val width = (card.width - sx(96f)).coerceAtLeast(sx(360f))
        val height = sy(92f).coerceIn(sy(72f), sy(108f))
        return UiRect(
            x = card.x + card.width * 0.5f - width * 0.5f,
            y = card.y + sy(36f),
            width = width,
            height = height
        )
    }

    private fun introLifeRewardButtonRect(): UiRect = introResourceButtonRect(0)
    private fun introShieldRewardButtonRect(): UiRect = introResourceButtonRect(1)
    private fun introSlowRewardButtonRect(): UiRect = introResourceButtonRect(2)
    private fun menuSoundRect(): UiRect = menuButtonRect(0, total = 10)
    private fun menuMusicVolumeRect(): UiRect = menuButtonRect(1, total = 10)
    private fun menuEffectsVolumeRect(): UiRect = menuButtonRect(2, total = 10)
    private fun menuHapticsRect(): UiRect = menuButtonRect(3, total = 10)
    private fun menuLanguageRect(): UiRect = menuButtonRect(4, total = 10)
    private fun menuDifficultyRect(): UiRect = menuButtonRect(5, total = 10)
    private fun menuLevelsRect(): UiRect = menuButtonRect(6, total = 10)
    private fun menuPremiumRect(): UiRect = menuButtonRect(7, total = 10)
    private fun menuPolicyRect(): UiRect = menuButtonRect(8, total = 10)
    private fun menuBackRect(): UiRect = menuButtonRect(9, total = 10)

    private fun premiumPanelRect(): UiRect {
        val sidePad = sx(26f).coerceIn(18f, 40f)
        val width = centeredPanelWidth(sidePad, minWidth = 900f, maxWidth = 1028f)
        return UiRect(
            x = centeredPanelX(width),
            y = sy(56f),
            width = width,
            height = viewport.worldHeight - sy(112f)
        )
    }

    private class PremiumLayoutRects(
        val hero: UiRect,
        val cards: List<UiRect>,
        val offer: UiRect,
        val chip: UiRect,
        val purchase: UiRect,
        val refresh: UiRect,
        val back: UiRect
    )

    // Flow layout: the cards are sized to fill the band between the header and the
    // status chip so the content never overlaps regardless of screen aspect ratio.
    private fun premiumLayout(): PremiumLayoutRects {
        val panel = premiumPanelRect()
        val left = panel.x + sx(24f)
        val width = panel.width - sx(48f)

        // Bottom controls stay stable (used for input hit-testing).
        val buttonRowH = sy(82f).coerceIn(sy(68f), sy(96f))
        val refreshW = (panel.width - sx(60f)) * 0.5f
        val refresh = UiRect(left, panel.y + sy(72f), refreshW, buttonRowH)
        val back = UiRect(panel.x + panel.width - sx(24f) - refreshW, refresh.y, refreshW, buttonRowH)
        val purchaseH = sy(96f).coerceIn(sy(78f), sy(112f))
        val purchase = UiRect(left, refresh.y + buttonRowH + sy(20f), width, purchaseH)
        val chipH = sy(52f).coerceIn(sy(44f), sy(60f))
        val chip = UiRect(left, purchase.y + purchaseH + sy(18f), width, chipH)

        // Content band between the header (title + subtitle) and the status chip.
        val contentTop = panel.y + panel.height - sy(196f)
        val contentBottom = chip.y + chipH + sy(20f)
        val available = (contentTop - contentBottom).coerceAtLeast(sy(540f))

        val heroH = (available * 0.30f).coerceIn(sy(236f), sy(330f))
        val offerH = (available * 0.205f).coerceIn(sy(150f), sy(214f))
        val cardH = ((available - heroH - offerH) * 0.235f).coerceIn(sy(104f), sy(168f))
        val gap = ((available - heroH - offerH - cardH * 3f) / 4f).coerceIn(sy(12f), sy(46f))

        val hero = UiRect(left, contentTop - heroH, width, heroH)
        val cards = ArrayList<UiRect>(3)
        var cursorTop = hero.y - gap
        for (i in 0 until 3) {
            val cy = cursorTop - cardH
            cards.add(UiRect(left, cy, width, cardH))
            cursorTop = cy - gap
        }
        val offer = UiRect(left, (cursorTop - offerH).coerceAtLeast(contentBottom), width, offerH)
        return PremiumLayoutRects(hero, cards, offer, chip, purchase, refresh, back)
    }

    private fun premiumHeroRect(): UiRect = premiumLayout().hero

    private fun premiumBenefitRect(index: Int): UiRect =
        premiumLayout().cards[index.coerceIn(0, 2)]

    private fun premiumOfferRect(): UiRect = premiumLayout().offer

    private fun premiumStatusChipRect(): UiRect = premiumLayout().chip

    private fun premiumPurchaseButtonRect(): UiRect = premiumLayout().purchase

    private fun premiumRefreshButtonRect(): UiRect = premiumLayout().refresh

    private fun premiumBackButtonRect(): UiRect = premiumLayout().back

    private fun premiumDialogRect(): UiRect {
        val sidePad = sx(82f).coerceIn(42f, 120f)
        val width = centeredPanelWidth(sidePad, minWidth = 720f, maxWidth = 880f)
        val height = sy(520f).coerceIn(sy(420f), sy(580f))
        return UiRect(
            x = centeredPanelX(width),
            y = viewport.worldHeight * 0.5f - height * 0.5f,
            width = width,
            height = height
        )
    }

    private fun premiumDialogChipRect(): UiRect {
        val dialog = premiumDialogRect()
        return UiRect(
            x = dialog.x + sx(24f),
            y = dialog.y + dialog.height - sy(108f),
            width = dialog.width - sx(48f),
            height = sy(48f).coerceIn(sy(40f), sy(54f))
        )
    }

    private fun premiumDialogPrimaryRect(): UiRect {
        val dialog = premiumDialogRect()
        val width = dialog.width - sx(72f)
        val height = sy(82f).coerceIn(sy(68f), sy(96f))
        return UiRect(
            x = dialog.x + dialog.width * 0.5f - width * 0.5f,
            y = dialog.y + sy(98f),
            width = width,
            height = height
        )
    }

    private fun premiumDialogSecondaryRect(): UiRect {
        val primary = premiumDialogPrimaryRect()
        return UiRect(
            x = primary.x,
            y = primary.y - primary.height - sy(18f),
            width = primary.width,
            height = primary.height
        )
    }

    private fun shopPanelRect(): UiRect {
        val sidePad = sx(32f).coerceIn(18f, 44f)
        val width = centeredPanelWidth(sidePad, minWidth = 900f, maxWidth = 1020f)
        return UiRect(
            x = centeredPanelX(width),
            y = sy(64f),
            width = width,
            height = viewport.worldHeight - sy(128f)
        )
    }

    private fun shopGridRect(): UiRect {
        val panel = shopPanelRect()
        val sideInset = sx(24f).coerceIn(14f, 36f)
        val shipShiftDown = sy(34f).coerceIn(22f, 40f)
        val topInset = if (selectedShopCategory == ShopCategory.SHIELDS) {
            sy(792f).coerceIn(590f, 880f)
        } else {
            sy(592f).coerceIn(432f, 668f) + shipShiftDown
        }
        val bottomInset = if (selectedShopCategory == ShopCategory.SHIELDS) {
            sy(332f).coerceIn(248f, 396f)
        } else {
            (sy(332f).coerceIn(248f, 396f) - shipShiftDown).coerceAtLeast(sy(220f))
        }
        return UiRect(
            x = panel.x + sideInset,
            y = panel.y + bottomInset,
            width = panel.width - sideInset * 2f,
            height = panel.height - topInset - bottomInset
        )
    }

    private fun shopColumns(): Int {
        return when {
            viewport.worldWidth < 680f -> 2
            viewport.worldWidth < 1320f -> 3
            else -> 4
        }
    }

    private fun shopRows(itemCount: Int): Int {
        val cols = shopColumns()
        return ((itemCount + cols - 1) / cols).coerceAtLeast(1)
    }

    private fun shopCellRect(index: Int, itemCount: Int): UiRect {
        val grid = shopGridRect()
        val cols = shopColumns()
        val rows = shopRows(itemCount)
        val gapX = sx(14f).coerceIn(8f, 20f)
        val gapY = sy(12f).coerceIn(8f, 16f)
        val width = (grid.width - gapX * (cols - 1)) / cols
        val height = (grid.height - gapY * (rows - 1)) / rows
        val row = index / cols
        val col = index % cols
        return UiRect(
            x = grid.x + col * (width + gapX),
            y = grid.y + grid.height - (row + 1) * height - row * gapY,
            width = width,
            height = height
        )
    }

    private fun shopShipCellRect(index: Int): UiRect {
        return shopCellRect(index, shipSkins.size)
    }

    private fun shopShieldCellRect(index: Int): UiRect {
        return shopCellRect(index, shieldStoreItems.size)
    }

    private fun shopTabShipsRect(): UiRect {
        val panel = shopPanelRect()
        val horizontalInset = sx(24f).coerceIn(14f, 36f)
        val gap = sx(12f).coerceIn(8f, 18f)
        val width = ((panel.width - horizontalInset * 2f - gap) * 0.5f).coerceAtLeast(sx(180f))
        val height = sy(74f).coerceIn(62f, 86f)
        return UiRect(
            x = panel.x + horizontalInset,
            y = panel.y + panel.height - sy(382f).coerceIn(270f, 438f),
            width = width,
            height = height
        )
    }

    private fun shopTabShieldsRect(): UiRect {
        val left = shopTabShipsRect()
        val gap = sx(12f).coerceIn(8f, 18f)
        return UiRect(
            x = left.x + left.width + gap,
            y = left.y,
            width = left.width,
            height = left.height
        )
    }

    private fun shopActionRect(): UiRect {
        val back = shopBackRect()
        val height = sy(86f).coerceIn(74f, 96f)
        val y = back.y + back.height + sy(12f).coerceIn(8f, 18f)
        return if (selectedShopCategory == ShopCategory.SHIELDS) {
            val gap = sx(12f).coerceIn(8f, 16f)
            val width = (back.width - gap) * 0.5f
            UiRect(
                x = back.x,
                y = y,
                width = width,
                height = height
            )
        } else {
            UiRect(
                x = back.x,
                y = y,
                width = back.width,
                height = height
            )
        }
    }

    private fun shopSlowActionRect(): UiRect {
        val shieldAction = shopActionRect()
        val gap = sx(12f).coerceIn(8f, 16f)
        return UiRect(
            x = shieldAction.x + shieldAction.width + gap,
            y = shieldAction.y,
            width = shieldAction.width,
            height = shieldAction.height
        )
    }

    private fun shopRewardAnchorRect(): UiRect {
        return if (selectedShopCategory == ShopCategory.SHIELDS) {
            shopSlowActionRect()
        } else {
            shopActionRect()
        }
    }

    private fun shopRewardRect(): UiRect {
        val anchor = shopRewardAnchorRect()
        val gap = sx(12f).coerceIn(8f, 16f)
        val height = sy(74f).coerceIn(62f, 84f)
        val width = if (selectedShopCategory == ShopCategory.SHIELDS) {
            shopActionRect().width
        } else {
            (anchor.width - gap) * 0.5f
        }
        return UiRect(
            x = if (selectedShopCategory == ShopCategory.SHIELDS) shopActionRect().x else anchor.x,
            y = anchor.y + anchor.height + sy(10f).coerceIn(8f, 16f),
            width = width,
            height = height
        )
    }

    private fun shopSlowRewardRect(): UiRect {
        val shieldReward = shopRewardRect()
        val gap = sx(12f).coerceIn(8f, 16f)
        return UiRect(
            x = shieldReward.x + shieldReward.width + gap,
            y = shieldReward.y,
            width = shieldReward.width,
            height = shieldReward.height
        )
    }

    private fun shopBackRect(): UiRect {
        val panel = shopPanelRect()
        val horizontalInset = sx(28f).coerceIn(16f, 36f)
        val height = sy(74f).coerceIn(62f, 84f)
        val width = panel.width - horizontalInset * 2f
        return UiRect(
            x = panel.x + horizontalInset,
            y = panel.y + sy(24f).coerceIn(18f, 44f),
            width = width,
            height = height
        )
    }

    private fun shopNoticeRect(): UiRect {
        val action = shopActionRect()
        val slowAction = shopSlowActionRect()
        val reward = shopRewardRect()
        val slowReward = shopSlowRewardRect()
        val topY = maxOf(
            maxOf(action.y + action.height, slowAction.y + slowAction.height),
            maxOf(reward.y + reward.height, slowReward.y + slowReward.height)
        )
        val height = sy(52f).coerceIn(42f, 64f)
        return UiRect(
            x = action.x,
            y = topY + sy(8f).coerceIn(6f, 14f),
            width = if (selectedShopCategory == ShopCategory.SHIELDS) {
                slowAction.x + slowAction.width - action.x
            } else {
                action.width
            },
            height = height
        )
    }

    private fun shopFeaturedRect(): UiRect {
        val panel = shopPanelRect()
        val horizontalInset = sx(24f).coerceIn(14f, 36f)
        val height = sy(186f).coerceIn(156f, 232f)
        return UiRect(
            x = panel.x + horizontalInset,
            y = panel.y + panel.height - sy(584f).coerceIn(432f, 680f),
            width = panel.width - horizontalInset * 2f,
            height = height
        )
    }

    private fun shopFeaturedSecondaryRect(): UiRect {
        val top = shopFeaturedRect()
        val gap = sy(14f).coerceIn(10f, 20f)
        return UiRect(
            x = top.x,
            y = top.y - top.height - gap,
            width = top.width,
            height = top.height
        )
    }

    private fun policyPanelRect(): UiRect {
        val sidePad = sx(30f).coerceIn(16f, 40f)
        val width = centeredPanelWidth(sidePad, minWidth = 900f, maxWidth = 1020f)
        return UiRect(
            x = centeredPanelX(width),
            y = sy(84f),
            width = width,
            height = viewport.worldHeight - sy(168f)
        )
    }

    private fun policyBackRect(): UiRect {
        val panel = policyPanelRect()
        val width = (panel.width - sx(56f)).coerceAtLeast(sx(220f))
        val height = sy(86f).coerceIn(74f, 96f)
        return UiRect(
            x = panel.x + (panel.width - width) * 0.5f,
            y = panel.y + sy(24f),
            width = width,
            height = height
        )
    }
    private fun levelSelectPanelRect(): UiRect {
        val sidePad = sx(24f).coerceIn(12f, 34f)
        val width = centeredPanelWidth(sidePad, minWidth = 900f, maxWidth = 1040f)
        return UiRect(
            x = centeredPanelX(width),
            y = sy(72f),
            width = width,
            height = viewport.worldHeight - sy(144f)
        )
    }

    private fun levelSelectGridRect(): UiRect {
        val panel = levelSelectPanelRect()
        val horizontalInset = sx(20f)
        val topInset = sy(220f)
        val bottomInset = sy(244f)
        return UiRect(
            x = panel.x + horizontalInset,
            y = panel.y + bottomInset,
            width = panel.width - horizontalInset * 2f,
            height = panel.height - topInset - bottomInset
        )
    }

    private fun levelSelectColumns(): Int {
        return if (viewport.worldWidth < 820f) 5 else 6
    }

    private fun levelSelectCellRect(index: Int): UiRect {
        val grid = levelSelectGridRect()
        val cols = levelSelectColumns()
        val rows = (levels.size + cols - 1) / cols
        val gapX = sx(10f).coerceIn(8f, 16f)
        val gapY = sy(10f).coerceIn(8f, 16f)
        val cellWidth = (grid.width - gapX * (cols - 1)) / cols
        val cellHeight = (grid.height - gapY * (rows - 1)) / rows
        val row = index / cols
        val col = index % cols
        return UiRect(
            x = grid.x + col * (cellWidth + gapX),
            y = grid.y + grid.height - (row + 1) * cellHeight - row * gapY,
            width = cellWidth,
            height = cellHeight
        )
    }

    private fun levelSelectBackRect(): UiRect {
        val panel = levelSelectGroupedPanelRect()
        val buttonHeight = sy(122f).coerceIn(98f, 132f)
        val horizontalInset = sx(20f)
        val gap = sx(14f).coerceIn(12f, 20f)
        val width = ((panel.width - horizontalInset * 2f - gap) * 0.5f).coerceAtLeast(sx(210f))
        return UiRect(
            x = panel.x + horizontalInset,
            y = panel.y + sy(24f),
            width = width,
            height = buttonHeight
        )
    }

    private fun levelSelectStartRect(): UiRect {
        val panel = levelSelectGroupedPanelRect()
        val backRect = levelSelectBackRect()
        val horizontalInset = sx(20f)
        return UiRect(
            x = panel.x + panel.width - horizontalInset - backRect.width,
            y = backRect.y,
            width = backRect.width,
            height = backRect.height
        )
    }

    private fun levelSelectBlockContainerRect(): UiRect {
        val panel = levelSelectGroupedPanelRect()
        val backRect = levelSelectBackRect()
        val horizontalInset = sx(10f).coerceIn(6f, 18f)
        val topInset = sy(224f).coerceIn(196f, 276f)
        val bottomInset = (backRect.y + backRect.height + sy(16f) - panel.y).coerceAtLeast(sy(156f))
        return UiRect(
            x = panel.x + horizontalInset,
            y = panel.y + bottomInset,
            width = panel.width - horizontalInset * 2f,
            height = (panel.height - topInset - bottomInset).coerceAtLeast(sy(260f))
        )
    }

    private fun levelSelectContentRect(container: UiRect = levelSelectBlockContainerRect()): UiRect {
        val leftInset = sx(8f).coerceIn(6f, 14f)
        val gapToScrollbar = sx(10f).coerceIn(8f, 16f)
        val track = levelSelectScrollbarTrackRect(container)
        val x = container.x + leftInset
        val rightEdge = track.x - gapToScrollbar
        val width = (rightEdge - x).coerceAtLeast(sx(520f).coerceIn(360f, 640f))
        return UiRect(
            x = x,
            y = container.y,
            width = width,
            height = container.height
        )
    }

    private fun levelSelectScrollbarTrackRect(container: UiRect = levelSelectBlockContainerRect()): UiRect {
        val trackWidth = sx(14f).coerceIn(11f, 18f)
        val sideInset = sx(8f).coerceIn(6f, 12f)
        val verticalInset = sy(10f).coerceIn(8f, 16f)
        return UiRect(
            x = container.x + container.width - sideInset - trackWidth,
            y = container.y + verticalInset,
            width = trackWidth,
            height = (container.height - verticalInset * 2f).coerceAtLeast(sy(180f))
        )
    }

    private fun levelSelectScrollbarThumbRect(
        trackRect: UiRect = levelSelectScrollbarTrackRect(),
        maxScroll: Float = maxLevelSelectScrollOffset()
    ): UiRect {
        if (maxScroll <= 0f) {
            return trackRect
        }
        val visibleHeight = trackRect.height
        val contentHeight = visibleHeight + maxScroll
        val minThumb = sy(88f).coerceIn(66f, 108f)
        val thumbHeight = (visibleHeight * (visibleHeight / contentHeight.coerceAtLeast(visibleHeight)))
            .coerceIn(minThumb, visibleHeight)
        val travel = (visibleHeight - thumbHeight).coerceAtLeast(0f)
        val progress = (levelSelectScrollOffset / maxScroll).coerceIn(0f, 1f)
        val y = trackRect.y + travel * (1f - progress)
        return UiRect(trackRect.x, y, trackRect.width, thumbHeight)
    }

    private fun setLevelSelectScrollFromTrackPosition(
        pointerY: Float,
        trackRect: UiRect = levelSelectScrollbarTrackRect()
    ) {
        val maxScroll = maxLevelSelectScrollOffset()
        if (maxScroll <= 0f) {
            levelSelectScrollOffset = 0f
            return
        }
        val thumbRect = levelSelectScrollbarThumbRect(trackRect, maxScroll)
        val travel = (trackRect.height - thumbRect.height).coerceAtLeast(0f)
        if (travel <= 0f) {
            levelSelectScrollOffset = 0f
            return
        }
        val thumbY = (pointerY - thumbRect.height * 0.5f).coerceIn(trackRect.y, trackRect.y + travel)
        val progress = 1f - ((thumbY - trackRect.y) / travel).coerceIn(0f, 1f)
        levelSelectScrollOffset = (progress * maxScroll).coerceIn(0f, maxScroll)
    }

    private fun levelBlockRect(blockIndex: Int): UiRect {
        val content = levelSelectContentRect()
        val verticalGap = sy(18f).coerceIn(12f, 28f)
        val width = content.width
        val height = sy(354f).coerceIn(302f, 418f)
        val topY = content.y + content.height + levelSelectScrollOffset
        return UiRect(
            x = content.x,
            y = topY - blockIndex * (height + verticalGap) - height,
            width = width,
            height = height
        )
    }

    private fun levelBlockColumns(): Int {
        return 1
    }

    private fun levelBlockCellRect(blockIndex: Int, slot: Int): UiRect {
        val blockRect = levelBlockRect(blockIndex)
        val columns = 5
        val rows = 2
        val horizontalInset = sx(16f).coerceIn(12f, 24f)
        val topInset = sy(134f).coerceIn(112f, 168f)
        val bottomInset = sy(20f).coerceIn(12f, 34f)
        val horizontalGap = sx(10f).coerceIn(8f, 16f)
        val verticalGap = sy(12f).coerceIn(8f, 18f)
        val width = (blockRect.width - horizontalInset * 2f - horizontalGap * (columns - 1)) / columns
        val height = (blockRect.height - topInset - bottomInset - verticalGap * (rows - 1)) / rows
        val row = slot / columns
        val col = slot % columns
        val x = blockRect.x + horizontalInset + col * (width + horizontalGap)
        val y = blockRect.y + blockRect.height - topInset - (row + 1) * height - row * verticalGap
        return UiRect(x, y, width, height)
    }

    private fun levelSelectGroupedPanelRect(): UiRect {
        val sidePad = sx(14f).coerceIn(8f, 22f)
        val width = centeredPanelWidth(sidePad, minWidth = 920f, maxWidth = 1060f)
        return UiRect(
            centeredPanelX(width),
            sy(70f),
            width,
            viewport.worldHeight - sy(140f)
        )
    }

    private fun levelSelectScrollTouchRect(): UiRect {
        return levelSelectBlockContainerRect()
    }

    private fun policyTabLeftRect(): UiRect {
        val panel = policyPanelRect()
        val horizontalInset = sx(24f).coerceIn(16f, 30f)
        val gap = sx(10f).coerceIn(8f, 16f)
        val width = ((panel.width - horizontalInset * 2f - gap * 2f) / 3f).coerceAtLeast(sx(120f))
        val height = sy(82f).coerceIn(70f, 92f)
        return UiRect(
            x = panel.x + horizontalInset,
            y = panel.y + sy(128f),
            width = width,
            height = height
        )
    }

    private fun policyTabCenterRect(): UiRect {
        val left = policyTabLeftRect()
        val gap = sx(10f).coerceIn(8f, 16f)
        return UiRect(
            x = left.x + left.width + gap,
            y = left.y,
            width = left.width,
            height = left.height
        )
    }

    private fun policyTabRightRect(): UiRect {
        val center = policyTabCenterRect()
        val gap = sx(10f).coerceIn(8f, 16f)
        return UiRect(
            x = center.x + center.width + gap,
            y = center.y,
            width = center.width,
            height = center.height
        )
    }

    private fun policyTextRect(): UiRect {
        val panel = policyPanelRect()
        val horizontalInset = sx(22f).coerceIn(14f, 28f)
        val topInset = sy(188f).coerceIn(150f, 230f)
        val bottomInset = sy(246f).coerceIn(196f, 286f)
        return UiRect(
            x = panel.x + horizontalInset,
            y = panel.y + bottomInset,
            width = panel.width - horizontalInset * 2f,
            height = panel.height - topInset - bottomInset
        )
    }
    private fun resultOverlayLayout(): ResultOverlayLayout {
        val tokens = uiScaleTokens()
        val cardWidth = (viewport.worldWidth - tokens.insetX * 2f).coerceIn(sx(460f), sx(860f))
        val buttonHeight = sy(108f).coerceIn(90f, 124f)
        val buttonGap = tokens.sm
        val topPad = tokens.xl
        val bottomPad = tokens.md + tokens.xs
        val cardHeight = sy(560f).coerceIn(sy(500f), sy(700f))
        val cardX = viewport.worldWidth * 0.5f - cardWidth * 0.5f
        val cardY = (viewport.worldHeight * 0.5f - cardHeight * 0.5f + sy(44f)).coerceIn(sy(180f), viewport.worldHeight - cardHeight - sy(240f))
        val buttonWidth = (cardWidth - tokens.lg * 2f).coerceAtLeast(sx(360f))
        val secondaryY = cardY + bottomPad
        val primaryY = secondaryY + buttonHeight + buttonGap
        val titleY = cardY + cardHeight - topPad
        val summaryY = titleY - lineHeight(uiTitleFont) - tokens.md
        val timeY = summaryY - lineHeight(bodyFont) - tokens.md
        val offerCardHeight = sy(136f).coerceIn(108f, 178f)
        val offerGap = sy(18f).coerceIn(12f, 26f)
        val offerCard = UiRect(
            x = cardX + tokens.md,
            y = (cardY - offerGap - offerCardHeight).coerceAtLeast(sy(26f)),
            width = cardWidth - tokens.md * 2f,
            height = offerCardHeight
        )
        val offerButtonHeight = (offerCard.height * 0.46f).coerceIn(sy(44f), sy(74f))
        val offerButton = UiRect(
            x = offerCard.x + tokens.md,
            y = offerCard.y + sy(10f).coerceIn(8f, 16f),
            width = offerCard.width - tokens.md * 2f,
            height = offerButtonHeight
        )
        return ResultOverlayLayout(
            card = UiRect(cardX, cardY, cardWidth, cardHeight),
            titleY = titleY,
            summaryY = summaryY,
            timeY = timeY,
            primaryButton = UiRect(
                x = viewport.worldWidth * 0.5f - buttonWidth * 0.5f,
                y = primaryY,
                width = buttonWidth,
                height = buttonHeight
            ),
            secondaryButton = UiRect(
                x = viewport.worldWidth * 0.5f - buttonWidth * 0.5f,
                y = secondaryY,
                width = buttonWidth,
                height = buttonHeight
            ),
            offerCard = offerCard,
            offerButton = offerButton
        )
    }

    private fun primaryResultButtonRect(): UiRect {
        return resultOverlayLayout().primaryButton
    }

    private fun secondaryResultButtonRect(): UiRect {
        return resultOverlayLayout().secondaryButton
    }

    private fun resultOfferButtonRect(): UiRect {
        return resultOverlayLayout().offerButton
    }

    private fun contains(rect: UiRect, x: Float, y: Float): Boolean {
        return x in rect.x..(rect.x + rect.width) && y in rect.y..(rect.y + rect.height)
    }

    private fun isButtonPressed(rect: UiRect): Boolean {
        for (pointer in 0..4) {
            if (!Gdx.input.isTouched(pointer)) {
                continue
            }
            if (isPointerInside(pointer, rect.x, rect.y, rect.width, rect.height)) {
                return true
            }
        }
        return false
    }

    private fun gameArenaLayout(): ArenaLayout {
        val controls = touchControlsLayout()
        val supportButtonsVisible = areSupportActionButtonsVisible()
        val shieldButton = touchShieldButtonRect()
        val slowButton = touchSlowButtonRect()
        val top = viewport.worldHeight - 126f
        val buttonsTop = if (supportButtonsVisible) {
            maxOf(shieldButton.y + shieldButton.height, slowButton.y + slowButton.height)
        } else {
            controls.y + controls.height
        }
        val bottom = maxOf(controls.y + controls.height, buttonsTop) + 38f
        val usableHeight = (top - bottom).coerceAtLeast(720f)
        val levelIndex = if (overlayMode == OverlayMode.GAME) simulation.levelConfig.index else selectedLevelIndex + 1
        val miniMultiplier = if (levelIndex in 31..40) 0.74f else 1f
        val radius = ((viewport.worldWidth * 0.42f).coerceAtMost(usableHeight * 0.38f) * miniMultiplier)
        return ArenaLayout(
            cx = viewport.worldWidth * 0.5f,
            cy = bottom + usableHeight * 0.5f,
            radius = radius,
            yScale = 1f
        )
    }

    private fun previewArenaLayout(): ArenaLayout {
        val top = viewport.worldHeight - 220f
        val bottom = 710f
        val usableHeight = (top - bottom).coerceAtLeast(620f)
        val levelIndex = if (overlayMode == OverlayMode.GAME) simulation.levelConfig.index else selectedLevelIndex + 1
        val miniMultiplier = if (levelIndex in 31..40) 0.74f else 1f
        val radius = ((viewport.worldWidth * 0.34f).coerceAtMost(usableHeight * 0.32f) * miniMultiplier)
        return ArenaLayout(
            cx = viewport.worldWidth * 0.5f,
            cy = bottom + usableHeight * 0.54f,
            radius = radius,
            yScale = 1f
        )
    }

    private fun orbitX(cx: Float, angle: Float, radius: Float): Float = cx + cos(angle) * radius

    private fun orbitY(cy: Float, angle: Float, radius: Float, yScale: Float): Float = cy + sin(angle) * radius * yScale

    private fun drawEllipseOutline(cx: Float, cy: Float, radius: Float, yScale: Float, segments: Int) {
        shapes.ellipse(cx - radius, cy - radius * yScale, radius * 2f, radius * 2f * yScale, segments)
    }

    private fun guideSidesForLevel(sectorCount: Int): Int {
        return sectorCount.coerceIn(6, 24)
    }

    private fun drawPolygonOutline(
        cx: Float,
        cy: Float,
        radius: Float,
        sides: Int,
        rotationRad: Float = 0f,
        yScale: Float = 1f
    ) {
        shapes.polygon(polygonVertices(cx, cy, radius, sides, rotationRad, yScale))
    }

    private fun drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val clampedRadius = radius.coerceAtMost(width * 0.5f).coerceAtMost(height * 0.5f)
        val cornerSegments = qualitySegments(40, minimum = 14)
        shapes.rect(x + clampedRadius, y, width - clampedRadius * 2f, height)
        shapes.rect(x, y + clampedRadius, width, height - clampedRadius * 2f)
        shapes.circle(x + clampedRadius, y + clampedRadius, clampedRadius, cornerSegments)
        shapes.circle(x + width - clampedRadius, y + clampedRadius, clampedRadius, cornerSegments)
        shapes.circle(x + clampedRadius, y + height - clampedRadius, clampedRadius, cornerSegments)
        shapes.circle(x + width - clampedRadius, y + height - clampedRadius, clampedRadius, cornerSegments)
    }

    private fun drawRoundedRectOutline(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val segments = 18
        val vertices = ArrayList<Float>(segments * 16)
        fun addArc(cx: Float, cy: Float, start: Float, end: Float) {
            for (index in 0..segments) {
                val t = start + (end - start) * (index / segments.toFloat())
                vertices.add(cx + cos(t) * radius)
                vertices.add(cy + sin(t) * radius)
            }
        }
        addArc(x + radius, y + radius, MathUtils.PI, MathUtils.PI * 1.5f)
        addArc(x + width - radius, y + radius, MathUtils.PI * 1.5f, MathUtils.PI2)
        addArc(x + width - radius, y + height - radius, 0f, MathUtils.PI * 0.5f)
        addArc(x + radius, y + height - radius, MathUtils.PI * 0.5f, MathUtils.PI)
        shapes.polyline(vertices.toFloatArray())
    }

    private fun qualitySegments(base: Int, minimum: Int = 8): Int {
        val scale = if (lowPerformanceMode) 0.62f else 1f
        return (base * scale).roundToInt().coerceAtLeast(minimum)
    }

    private fun drawAsymmetricRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float
    ) {
        drawConvexPolygon(
            asymmetricRoundedRectVertices(
                x,
                y,
                width,
                height,
                topLeft,
                topRight,
                bottomRight,
                bottomLeft
            )
        )
    }

    private fun drawRotatedAsymmetricRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotationRad: Float,
        originX: Float,
        originY: Float,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float
    ) {
        val baseVertices = asymmetricRoundedRectVertices(
            x,
            y,
            width,
            height,
            topLeft,
            topRight,
            bottomRight,
            bottomLeft
        )
        val rotated = FloatArray(baseVertices.size)
        val cosR = cos(rotationRad)
        val sinR = sin(rotationRad)
        var index = 0
        while (index < baseVertices.size) {
            val localX = baseVertices[index] - (x + originX)
            val localY = baseVertices[index + 1] - (y + originY)
            rotated[index] = x + originX + localX * cosR - localY * sinR
            rotated[index + 1] = y + originY + localX * sinR + localY * cosR
            index += 2
        }
        drawConvexPolygon(rotated)
    }

    private fun asymmetricRoundedRectVertices(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float
    ): FloatArray {
        val segments = 12
        val tl = topLeft.coerceIn(0f, minOf(width, height) * 0.5f)
        val tr = topRight.coerceIn(0f, minOf(width, height) * 0.5f)
        val br = bottomRight.coerceIn(0f, minOf(width, height) * 0.5f)
        val bl = bottomLeft.coerceIn(0f, minOf(width, height) * 0.5f)
        val vertices = ArrayList<Float>((segments + 1) * 8)

        fun addArc(cx: Float, cy: Float, radius: Float, start: Float, end: Float) {
            if (radius <= 0f) {
                vertices.add(cx)
                vertices.add(cy)
                return
            }
            for (step in 0..segments) {
                val t = start + (end - start) * (step / segments.toFloat())
                vertices.add(cx + cos(t) * radius)
                vertices.add(cy + sin(t) * radius)
            }
        }

        addArc(x + tl, y + height - tl, tl, MathUtils.PI, MathUtils.PI * 1.5f)
        addArc(x + width - tr, y + height - tr, tr, MathUtils.PI * 1.5f, MathUtils.PI2)
        addArc(x + width - br, y + br, br, 0f, MathUtils.PI * 0.5f)
        addArc(x + bl, y + bl, bl, MathUtils.PI * 0.5f, MathUtils.PI)

        return vertices.toFloatArray()
    }

    private fun drawConvexPolygon(vertices: FloatArray) {
        if (vertices.size < 6) {
            return
        }
        var centerX = 0f
        var centerY = 0f
        var index = 0
        while (index < vertices.size) {
            centerX += vertices[index]
            centerY += vertices[index + 1]
            index += 2
        }
        val pointCount = vertices.size / 2
        centerX /= pointCount.toFloat()
        centerY /= pointCount.toFloat()

        index = 0
        while (index < vertices.size) {
            val next = (index + 2) % vertices.size
            shapes.triangle(
                centerX,
                centerY,
                vertices[index],
                vertices[index + 1],
                vertices[next],
                vertices[next + 1]
            )
            index += 2
        }
    }

    private fun drawLoaderArc(
        cx: Float,
        cy: Float,
        outerRadius: Float,
        innerRadius: Float,
        rotationRad: Float,
        spanRad: Float
    ) {
        val segments = 48
        val start = rotationRad
        val end = rotationRad + spanRad
        for (step in 0 until segments) {
            val t0 = start + (end - start) * (step / segments.toFloat())
            val t1 = start + (end - start) * ((step + 1) / segments.toFloat())
            val x1 = cx + cos(t0) * outerRadius
            val y1 = cy + sin(t0) * outerRadius
            val x2 = cx + cos(t1) * outerRadius
            val y2 = cy + sin(t1) * outerRadius
            val x3 = cx + cos(t1) * innerRadius
            val y3 = cy + sin(t1) * innerRadius
            val x4 = cx + cos(t0) * innerRadius
            val y4 = cy + sin(t0) * innerRadius
            shapes.triangle(x1, y1, x2, y2, x3, y3)
            shapes.triangle(x1, y1, x4, y4, x3, y3)
        }
    }

    private fun drawButtonGlyph(cx: Float, cy: Float, unit: Float) {
        shapes.circle(cx - unit, cy + unit, unit, 28)
        shapes.circle(cx + unit, cy + unit, unit, 28)
        shapes.circle(cx - unit, cy - unit, unit, 28)
        shapes.circle(cx + unit, cy - unit, unit, 28)
    }

    private fun drawHeartIcon(cx: Float, cy: Float, scale: Float, color: Color) {
        val r = (10f * scale).coerceAtLeast(5f)
        shapes.color = color
        shapes.circle(cx - r * 0.62f, cy + r * 0.35f, r, 24)
        shapes.circle(cx + r * 0.62f, cy + r * 0.35f, r, 24)
        shapes.triangle(
            cx - r * 1.56f, cy + r * 0.32f,
            cx + r * 1.56f, cy + r * 0.32f,
            cx, cy - r * 1.9f
        )
    }

    private fun drawShieldIcon(cx: Float, cy: Float, scale: Float, color: Color) {
        val w = (26f * scale).coerceAtLeast(10f)
        val h = (32f * scale).coerceAtLeast(12f)
        fun shield(sw: Float, sh: Float): FloatArray = floatArrayOf(
            cx - sw * 0.5f, cy + sh * 0.48f,
            cx + sw * 0.5f, cy + sh * 0.48f,
            cx + sw * 0.46f, cy + sh * 0.06f,
            cx, cy - sh * 0.54f,
            cx - sw * 0.46f, cy + sh * 0.06f
        )
        // dark outline, body, top sheen
        shapes.color = Color(0.05f, 0.09f, 0.16f, 0.95f)
        drawConvexPolygon(shield(w + 4f * scale, h + 4f * scale))
        shapes.color = color
        drawConvexPolygon(shield(w, h))
        shapes.color = Color(1f, 1f, 1f, 0.26f)
        drawConvexPolygon(
            floatArrayOf(
                cx - w * 0.42f, cy + h * 0.44f,
                cx + w * 0.42f, cy + h * 0.44f,
                cx + w * 0.3f, cy + h * 0.12f,
                cx - w * 0.3f, cy + h * 0.12f
            )
        )
        // check emblem
        val t = (w * 0.15f).coerceAtLeast(2f)
        shapes.color = Color(1f, 1f, 1f, 0.92f)
        shapes.rectLine(cx - w * 0.22f, cy + h * 0.02f, cx - w * 0.04f, cy - h * 0.16f, t)
        shapes.rectLine(cx - w * 0.04f, cy - h * 0.16f, cx + w * 0.26f, cy + h * 0.16f, t)
    }

    private fun drawCoinIcon(cx: Float, cy: Float, scale: Float) {
        val radius = (10f * scale).coerceAtLeast(6f)
        shapes.color = Color(1f, 0.78f, 0.29f, 1f)
        shapes.circle(cx, cy, radius, 28)
        shapes.color = Color(1f, 0.9f, 0.56f, 0.95f)
        shapes.circle(cx - radius * 0.18f, cy + radius * 0.08f, radius * 0.56f, 24)
        shapes.color = Color(0.69f, 0.44f, 0.09f, 0.42f)
        shapes.circle(cx + radius * 0.2f, cy - radius * 0.18f, radius * 0.32f, 20)
    }

    private fun activeUiPalette(): NeonPalette {
        val level = if (overlayMode == OverlayMode.GAME || overlayMode == OverlayMode.PAUSE) {
            simulation.levelConfig.index
        } else {
            selectedLevelIndex + 1
        }
        return NeonPaletteRamp.forLevel(level)
    }

    private fun drawTimeSlowIcon(cx: Float, cy: Float, scale: Float) {
        val h = (16f * scale).coerceAtLeast(8f)
        val w = (10f * scale).coerceAtLeast(5f)
        val themeAccent = activeUiPalette().uiAccent
        val purple = Color(themeAccent).lerp(Color(0.82f, 0.52f, 1f, 1f), 0.28f)
        val purpleBright = Color(purple).lerp(Color.WHITE, 0.46f)
        val top = cy + h * 0.5f
        val bottom = cy - h * 0.5f
        val neck = h * 0.1f
        shapes.color = purple
        drawConvexPolygon(
            floatArrayOf(
                cx - w * 0.5f, top,
                cx + w * 0.5f, top,
                cx + w * 0.16f, cy + neck,
                cx - w * 0.16f, cy + neck
            )
        )
        drawConvexPolygon(
            floatArrayOf(
                cx - w * 0.16f, cy - neck,
                cx + w * 0.16f, cy - neck,
                cx + w * 0.5f, bottom,
                cx - w * 0.5f, bottom
            )
        )
        // amber sand: a pile settled in the bottom bulb + a thin falling stream
        val sand = Color(1f, 0.82f, 0.42f, 0.96f)
        shapes.color = sand
        drawConvexPolygon(
            floatArrayOf(
                cx - w * 0.42f, bottom + h * 0.04f,
                cx + w * 0.42f, bottom + h * 0.04f,
                cx, cy - neck * 2.4f
            )
        )
        shapes.rectLine(cx, cy + neck, cx, cy - neck, (w * 0.14f).coerceAtLeast(1f))
        // remaining sand in the top bulb
        shapes.color = Color(sand.r, sand.g, sand.b, 0.72f)
        drawConvexPolygon(
            floatArrayOf(
                cx - w * 0.16f, cy + neck,
                cx + w * 0.16f, cy + neck,
                cx, cy + neck + h * 0.16f
            )
        )
        // glass rims
        shapes.color = purpleBright
        shapes.rectLine(cx - w * 0.5f, top, cx + w * 0.5f, top, (w * 0.2f).coerceAtLeast(1f))
        shapes.rectLine(cx - w * 0.5f, bottom, cx + w * 0.5f, bottom, (w * 0.2f).coerceAtLeast(1f))
    }

    private fun drawLockIcon(cx: Float, cy: Float, scale: Float, color: Color) {
        val bodyW = (18f * scale).coerceAtLeast(10f)
        val bodyH = (14f * scale).coerceAtLeast(8f)
        val archR = bodyW * 0.34f
        shapes.color = color
        drawRoundedRect(cx - bodyW * 0.5f, cy - bodyH * 0.5f, bodyW, bodyH, bodyW * 0.18f)
        shapes.circle(cx - archR * 0.72f, cy + bodyH * 0.36f, archR, 18)
        shapes.circle(cx + archR * 0.72f, cy + bodyH * 0.36f, archR, 18)
        shapes.triangle(
            cx - archR * 1.72f, cy + bodyH * 0.36f,
            cx + archR * 1.72f, cy + bodyH * 0.36f,
            cx + archR * 1.72f, cy + bodyH * 0.08f
        )
        shapes.triangle(
            cx - archR * 1.72f, cy + bodyH * 0.36f,
            cx - archR * 1.72f, cy + bodyH * 0.08f,
            cx + archR * 1.72f, cy + bodyH * 0.08f
        )
        shapes.color = Color(1f, 1f, 1f, 0.32f)
        shapes.circle(cx, cy - bodyH * 0.05f, bodyW * 0.1f, 16)
    }

    private fun drawUiTextureIcon(texture: Texture?, centerX: Float, centerY: Float, size: Float): Boolean {
        if (texture == null) {
            return false
        }
        val prev = batch.color
        val prevR = prev.r
        val prevG = prev.g
        val prevB = prev.b
        val prevA = prev.a
        val drawSize = size.roundToInt().toFloat().coerceAtLeast(1f)
        val drawX = (centerX - drawSize * 0.5f).roundToInt().toFloat()
        val drawY = (centerY - drawSize * 0.5f).roundToInt().toFloat()
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(texture, drawX, drawY, drawSize, drawSize)
        batch.setColor(prevR, prevG, prevB, prevA)
        return true
    }

    private fun drawFilledPolygon(
        cx: Float,
        cy: Float,
        radius: Float,
        sides: Int,
        rotationRad: Float = 0f,
        yScale: Float = 1f
    ) {
        val angleStep = (MathUtils.PI2 / sides.toFloat())
        var index = 0
        while (index < sides) {
            val angleA = rotationRad + angleStep * index
            val angleB = rotationRad + angleStep * (index + 1)
            val x1 = orbitX(cx, angleA, radius)
            val y1 = orbitY(cy, angleA, radius, yScale)
            val x2 = orbitX(cx, angleB, radius)
            val y2 = orbitY(cy, angleB, radius, yScale)
            shapes.triangle(cx, cy, x1, y1, x2, y2)
            index += 1
        }
    }

    private fun polygonVertices(
        cx: Float,
        cy: Float,
        radius: Float,
        sides: Int,
        rotationRad: Float = 0f,
        yScale: Float = 1f
    ): FloatArray {
        val vertices = FloatArray(sides * 2)
        val step = (2.0 * PI / sides.toDouble()).toFloat()
        for (index in 0 until sides) {
            val angle = rotationRad + step * index
            vertices[index * 2] = orbitX(cx, angle, radius)
            vertices[index * 2 + 1] = orbitY(cy, angle, radius, yScale)
        }
        return vertices
    }

    private fun previewRotation(level: LevelConfig): Float {
        return worldTime * (0.35f + level.index * 0.01f)
    }

    private fun centeredX(text: String, title: Boolean): Float {
        return viewport.worldWidth * 0.5f - estimateTextWidth(text, title) * 0.5f
    }

    private fun pixelSnap(value: Float): Float {
        return kotlin.math.floor(value) + 0.5f
    }

    private fun centeredX(text: String, textFont: BitmapFont): Float {
        return viewport.worldWidth * 0.5f - estimateTextWidth(textFont, text) * 0.5f
    }

    private fun centeredX(text: String, textFont: BitmapFont, rect: UiRect): Float {
        return rect.x + rect.width * 0.5f - estimateTextWidth(textFont, text) * 0.5f
    }

    private fun lineHeight(textFont: BitmapFont): Float = textFont.lineHeight

    private fun estimateTextWidth(text: String, title: Boolean): Float {
        val cacheKey = "${if (title) 'T' else 'B'}:$text"
        return textWidthCache.getOrPut(cacheKey) {
            val layout = if (title) titleLayout else fontLayout
            layout.setText(if (title) titleFont else font, text)
            layout.width
        }
    }

    private fun estimateTextWidth(textFont: BitmapFont, text: String): Float {
        fontLayout.setText(textFont, text)
        return fontLayout.width
    }

    private fun formatSeconds(value: Float): String {
        return String.format(Locale.US, "%.1fs", value)
    }

    private fun maxPolicyScrollOffset(): Float {
        val textRect = policyTextRect()
        val lineHeight = lineHeight(bodyFont) * 1.08f
        val lines = wrappedLines(
            when (selectedPolicyPage) {
                PolicyPage.PRIVACY -> policyPrivacy()
                PolicyPage.TERMS -> policyTerms()
                PolicyPage.LICENSE -> policyLicense()
            },
            textRect.width
        )
        return (lines.size * lineHeight - textRect.height).coerceAtLeast(0f)
    }

    private fun maxLevelSelectScrollOffset(): Float {
        val container = levelSelectBlockContainerRect()
        val blockCount = (levels.size + 9) / 10
        val columns = levelBlockColumns()
        val rows = (blockCount + columns - 1) / columns
        val verticalGap = sy(18f).coerceIn(12f, 28f)
        val blockHeight = sy(354f).coerceIn(302f, 418f)
        val visibleHeight = container.height.coerceAtLeast(1f)
        val contentHeight = rows * blockHeight + (rows - 1).coerceAtLeast(0) * verticalGap
        return (contentHeight - visibleHeight).coerceAtLeast(0f)
    }

    private fun maxUnlockedLevelIndex(): Int {
        val highestCleared = bestScoreManager.snapshot().highestLevelCleared.coerceAtLeast(0)
        return highestCleared.coerceIn(0, levels.lastIndex)
    }

    private fun isLevelUnlocked(levelIndex: Int): Boolean {
        val clampedIndex = levelIndex.coerceIn(0, levels.lastIndex)
        return clampedIndex <= maxUnlockedLevelIndex()
    }

    private fun guideLabel(sectorCount: Int): String {
        return when {
            sectorCount >= 20 -> t("HIGH-SIDED REACTOR", "ÇOK YÜZLÜ REAKTÖR")
            else -> t("$sectorCount-SIDED FORM", "$sectorCount KENARLI FORM")
        }
    }

    private fun blockThemeSummary(blockStartLevel: Int): String {
        return when (blockStartLevel) {
            1 -> t("Core controls and baseline rhythm", "Temel kontrol ve başlangıç ritmi")
            11 -> t("Pull walls and rotation pressure", "İten duvar ve dönüş baskısı")
            21 -> t("Fly hunt visuals and faster reads", "Sinek avı teması ve hızlı okuma")
            31 -> t("Mini mode with time-bubble walls", "Zaman balonlu mini mod")
            41 -> t("Dense pressure and shorter recovery", "Yoğun baskı ve kısa toparlanma")
            51 -> t("Reactor storm pressure lanes", "Reaktör fırtınası baskı hatları")
            61 -> t("War visuals and missile volleys", "Savaş görseli ve füze salvosu")
            71 -> t("Bio defense: virus-form pilot vs square barriers", "Biyo savunma: virüs form pilot ve kare bariyerler")
            81 -> t("Space ambush: enemy ship laser lanes", "Uzay pususu: düşman gemi lazer şeritleri")
            91 -> t("Kitchen rush: bread walls and giant knives", "Mutfak akışı: ekmek duvarları ve dev bıçaklar")
            else -> t("New mechanics", "Yeni mekanikler")
        }
    }

    private fun tierFeatureLabel(level: LevelConfig): String {
        return when (level.index) {
            in 1..10 -> t("Core controls + tutorial", "Temel kontroller + öğretici")
            in 11..20 -> t("Pull walls + fast spin", "İten duvar + hızlı dönüş")
            in 21..30 -> t("Fly theme + swatter walls", "Sinek teması + raket duvarlar")
            in 31..40 -> t("Mini mode + time bubble", "Mini mod + zaman balonu")
            in 41..50 -> t("Hard pressure + rapid cadence", "Zorlu baskı + hızlı tempo")
            in 51..60 -> t("Reactor storm + dense pressure", "Reaktör fırtınası + yoğun baskı")
            in 61..70 -> t("War zone + inbound missiles", "Savaş alanı + içe yönlenen füzeler")
            in 71..80 -> t("Virus-form ship + square antibody walls", "Virüs form gemi + kare antikor duvarları")
            in 81..90 -> t("Enemy ships + centerline laser ambush", "Düşman gemi + merkez lazer pususu")
            in 91..100 -> t("Kitchen mode + bread lanes + giant knives", "Mutfak modu + ekmek hatları + dev bıçaklar")
            else -> t("Adaptive pressure + core hazards", "Uyarlanabilir baskı + çekirdek tehditleri")
        }
    }

    private fun localizedTierName(tierName: String): String {
        if (settingsState.language != AppLanguage.TR) {
            return tierName
        }
        return when (tierName) {
            "Dock Departure" -> "Liman Çıkışı"
            "Asteroid Belt" -> "Asteroit Kuşağı"
            "Lagrange Drift" -> "Lagrange Akışı"
            "Deep Orbit" -> "Derin Yörünge"
            "Fracture Zone" -> "Kırık Bölge"
            "Command Burn" -> "Komuta Ateşi"
            "War Horizon" -> "Savaş Ufku"
            "Bio Defense" -> "Biyo Savunma"
            "Nova Ambush" -> "Nova Pusu"
            "Kitchen Rush" -> "Mutfak Akışı"
            else -> tierName
        }
    }

    private fun levelStartHint(level: LevelConfig): String {
        return when (level.index) {
            in 1..10 -> t(
                "Use short taps for lane timing, hold for smooth correction",
                "Şerit zamanlaması için kısa dokun, düzeltme için basılı tut"
            )
            in 31..40 -> t(
                "Blue bubble walls trigger 1.5s slow motion on contact",
                "Mavi balon duvara dokunursan 1.5 sn yavaşlama açılır"
            )
            in 21..30 -> t(
                "You are the fly now: keep moving through racket arcs",
                "Artık sensin sinek: raket halkaları arasında sürekli hareket et"
            )
            in 51..60 -> t(
                "Reactor storm: no input inversion, but pressure windows stay tight",
                "Reaktör fırtınası: kontrol tersine dönmez ama baskı pencereleri dar kalır"
            )
            in 61..70 -> t(
                "War block: dodge incoming missile volleys while keeping smooth lane timing",
                "Savaş bloğu: içe akan füze dalgalarından kaçarken şerit zamanlamanı koru"
            )
            in 71..80 -> t(
                "Bio defense block: virus-form pilot versus antibody barriers. Skills are one-use per run.",
                "Biyo savunma bloğu: virüs formundaki pilot antikor bariyerlere karşı. Yetenekler koşu başına tek kullanımlık."
            )
            in 81..90 -> t(
                "Ambush block: enemy ships fire center-line lasers from up/down and left/right variations.",
                "Pusu bloğu: düşman gemileri yukarı/aşağı ve sağ/sol varyasyonlarından merkeze lazer atar."
            )
            in 91..100 -> t(
                "Kitchen block: flat bread lanes and giant knives move toward the core. Keep calm micro-corrections.",
                "Mutfak bloğu: düz ekmek hatları ve dev bıçaklar çekirdeğe akar. Sakin mikro düzeltmeleri koru."
            )
            else -> t(
                "Use both controls to stay in safe gaps",
                "Güvenli boşlukta kalmak için iki yönü de kullan"
            )
        }
    }

    private fun realtimeChangePrimaryLabel(): String {
        if (simulation.shieldActive) {
            return t("SHIELD ACTIVE", "KALKAN AKTİF")
        }
        return when (simulation.levelConfig.index) {
            in 1..10 -> t("STABLE START WINDOW", "STABİL BAŞLANGIÇ ARALIĞI")
            in 11..20 -> t("PULL WALL PRESSURE", "İTİCİ DUVAR BASKISI")
            in 21..30 -> t("FLY HUNT SPECIAL", "SİNEK AVI ÖZEL BÖLÜM")
            in 31..40 -> if (simulation.timeBubbleActive) t("TIME SLOW ACTIVE", "ZAMAN YAVAŞLATMA AKTİF") else t("MINI MODE ACTIVE", "MİNİ MOD AKTİF")
            in 41..50 -> t("HIGH DENSITY PATTERN", "YOĞUN DESEN BASKISI")
            in 51..60 -> t("STORM PRESSURE", "FIRTINA BASKISI")
            in 71..80 -> t("BIO DEFENSE", "BİYO SAVUNMA")
            in 81..90 -> t("NOVA AMBUSH", "NOVA PUSUSU")
            in 91..100 -> t("KITCHEN RUSH", "MUTFAK AKIŞI")
            else -> t("MISSILE BARRAGE", "FÜZE SALVOSU")
        }
    }

    private fun compactStatusPrimaryLabel(): String {
        if (simulation.shieldActive) {
            return t("SHIELD", "KALKAN")
        }
        return when (simulation.levelConfig.index) {
            in 1..10 -> t("STABLE", "STABİL")
            in 11..20 -> t("PULL WALL", "İTEN DUVAR")
            in 21..30 -> t("HUNT SPECIAL", "AVI ÖZEL")
            in 31..40 -> if (simulation.timeBubbleActive) t("TIME SLOW", "YAVAŞLAMA") else t("MINI MODE", "MİNİ MOD")
            in 41..50 -> t("DENSE MODE", "YOĞUN MOD")
            in 51..60 -> t("STORM MODE", "FIRTINA MODU")
            in 71..80 -> t("BIO MODE", "BİYO MOD")
            in 81..90 -> t("AMBUSH MODE", "PUSU MODU")
            in 91..100 -> t("KITCHEN MODE", "MUTFAK MODU")
            else -> t("MISSILE MODE", "FÜZE MODU")
        }
    }

    private fun realtimeChangeSecondaryLabel(): String {
        val move = if (simulation.usesStepMovement) t("Step", "Adım") else t("Free", "Serbest")
        val corners = t("${simulation.levelConfig.sectorCount} corners", "${simulation.levelConfig.sectorCount} köşe")
        val difficultyInfo = t("Diff ${difficultyLabelShort()}", "Zorluk ${difficultyLabelShort()}")
        val lifeInfo = if (premiumEnabled) {
            t("Premium", "Premium")
        } else {
            t("Lives ${livesState.lives}/$MAX_LIVES", "Can ${livesState.lives}/$MAX_LIVES")
        }
        val shieldInfo = t("Shield $shieldCount/$MAX_SHIELDS", "Kalkan $shieldCount/$MAX_SHIELDS")
        val slowInfo = t("Slowdown $slowPowerCount/$MAX_SLOW_POWERS", "Yavaşlatma $slowPowerCount/$MAX_SLOW_POWERS")
        return "$move • $corners • $difficultyInfo • $lifeInfo • $shieldInfo • $slowInfo"
    }

    private fun blockBriefingTitle(levelNumber: Int): String {
        return when (levelNumber) {
            1 -> t("Levels 1-10 Overview", "1-10 Seviye Özeti")
            11 -> t("Levels 11-20 Overview", "11-20 Seviye Özeti")
            21 -> t("Levels 21-30 Overview", "21-30 Seviye Özeti")
            31 -> t("Levels 31-40 Overview", "31-40 Seviye Özeti")
            41 -> t("Levels 41-50 Overview", "41-50 Seviye Özeti")
            50 -> t("Levels 41-50 Overview", "41-50 Seviye Özeti")
            51 -> t("Levels 51-60 Overview", "51-60 Seviye Özeti")
            61 -> t("Levels 61-70 Overview", "61-70 Seviye Özeti")
            71 -> t("Levels 71-80 Overview", "71-80 Seviye Özeti")
            75 -> t("Levels 71-80 Overview", "71-80 Seviye Özeti")
            81 -> t("Levels 81-90 Overview", "81-90 Seviye Özeti")
            91 -> t("Levels 91-100 Overview", "91-100 Seviye Özeti")
            100 -> t("Levels 91-100 Overview", "91-100 Seviye Özeti")
            else -> t("New Mechanics", "Yeni Mekanikler")
        }
    }

    private fun blockBriefingSteps(levelNumber: Int): List<BlockBriefingStep> {
        return when (levelNumber) {
            1 -> listOf(
                BlockBriefingStep(
                    titleEn = "Control Basics",
                    titleTr = "Kontrol Temelleri",
                    detailEn = "Read the safe gap and keep smooth input rhythm.",
                    detailTr = "Güvenli boşluğu oku ve giriş ritmini yumuşak tut.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Use Glide Controls",
                    titleTr = "Süzülme Kontrolleri",
                    detailEn = "Left/right touch zones steer your ship in short corrections.",
                    detailTr = "Sol/sağ dokunma bölgeleriyle gemiyi kısa düzeltmelerle yönlendir.",
                    anchor = TutorialAnchor.LEFT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Start The Run",
                    titleTr = "Koşuyu Başlat",
                    detailEn = "Tap start when you are ready. This block ramps gently.",
                    detailTr = "Hazırsan başlat'a dokun. Bu blok kademeli olarak zorlaşır.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            11 -> listOf(
                BlockBriefingStep(
                    titleEn = "Pull Walls",
                    titleTr = "İtici Duvarlar",
                    detailEn = "Contact keeps pushing you inward; break contact quickly.",
                    detailTr = "Temas sürdükçe merkeze iter; teması hızlı kes.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Higher Rotation",
                    titleTr = "Daha Hızlı Dönüş",
                    detailEn = "Use tighter left/right timing against faster lane shifts.",
                    detailTr = "Daha hızlı şerit değişimlerine karşı daha sıkı sol/sağ zamanlama kullan.",
                    anchor = TutorialAnchor.RIGHT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Shield Reserve",
                    titleTr = "Kalkan Rezervi",
                    detailEn = "Track lives and shield reserves from the center support panel.",
                    detailTr = "Can ve kalkan rezervini orta destek panelinden takip et.",
                    anchor = TutorialAnchor.SUPPORT_PANEL
                )
            )
            21 -> listOf(
                BlockBriefingStep(
                    titleEn = "Fly Hunt Tempo",
                    titleTr = "Sinek Avı Temposu",
                    detailEn = "Pattern speed rises. Keep your correction distance short.",
                    detailTr = "Desen temposu artar. Düzeltme mesafeni kısa tut.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Cross Safe Windows",
                    titleTr = "Güvenli Aralık Geçişi",
                    detailEn = "Cross open windows without over-committing to one side.",
                    detailTr = "Açık pencerelerden geçerken tek tarafa fazla yüklenme.",
                    anchor = TutorialAnchor.LEFT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Status Reading",
                    titleTr = "Durum Takibi",
                    detailEn = "Read the mid status chip before each run.",
                    detailTr = "Her koşu öncesi orta durum çipini oku.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            31 -> listOf(
                BlockBriefingStep(
                    titleEn = "Mini Mode",
                    titleTr = "Mini Mod",
                    detailEn = "Ship and threats are smaller; precision matters more.",
                    detailTr = "Gemi ve tehditler küçülür; hassasiyet daha önemlidir.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Time Bubble Walls",
                    titleTr = "Zaman Balonu Duvarları",
                    detailEn = "Bubble contact can trigger temporary slow-motion interactions.",
                    detailTr = "Balon teması geçici yavaşlatma etkileşimi başlatabilir.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Prepare Then Start",
                    titleTr = "Hazırlan ve Başla",
                    detailEn = "Use a short pre-plan, then start confidently.",
                    detailTr = "Kısa bir ön plan yap, sonra güvenle başlat.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            41 -> listOf(
                BlockBriefingStep(
                    titleEn = "Dense Pressure",
                    titleTr = "Yoğun Baskı",
                    detailEn = "Threat density is higher; avoid staying static.",
                    detailTr = "Tehdit yoğunluğu artar; sabit kalmaktan kaçın.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Faster Reactions",
                    titleTr = "Daha Hızlı Tepki",
                    detailEn = "Inputs must be earlier and cleaner in this block.",
                    detailTr = "Bu blokta girişler daha erken ve daha temiz olmalı.",
                    anchor = TutorialAnchor.RIGHT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Resource Discipline",
                    titleTr = "Kaynak Disiplini",
                    detailEn = "Manage lives and shields carefully before starting.",
                    detailTr = "Başlatmadan önce can ve kalkanı dikkatle yönet.",
                    anchor = TutorialAnchor.SUPPORT_PANEL
                )
            )
            50 -> listOf(
                BlockBriefingStep(
                    titleEn = "Dense Pressure Peak",
                    titleTr = "Yoğun Baskı Zirvesi",
                    detailEn = "Late lanes tighten quickly. Read one lane ahead before moving.",
                    detailTr = "Geç şeritler hızlı daralır. Hareket etmeden önce bir şerit ötesini oku.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Stable Inputs",
                    titleTr = "Stabil Girişler",
                    detailEn = "Avoid long holds. Use short controlled corrections on both sides.",
                    detailTr = "Uzun basılı tutmadan kaçın. İki yönde kısa ve kontrollü düzeltmeler kullan.",
                    anchor = TutorialAnchor.RIGHT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Resource Check",
                    titleTr = "Kaynak Kontrolü",
                    detailEn = "Check lives, shield and slowdown before pressing start.",
                    detailTr = "Başlatmadan önce can, kalkan ve yavaşlatmayı kontrol et.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            51 -> listOf(
                BlockBriefingStep(
                    titleEn = "Reactor Storm",
                    titleTr = "Reaktör Fırtınası",
                    detailEn = "Endgame pressure peaks and timing windows shrink.",
                    detailTr = "Final baskısı zirve yapar ve zamanlama pencereleri daralır.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Tight Timing",
                    titleTr = "Sıkı Zamanlama",
                    detailEn = "Controls stay normal; focus on short, precise corrections.",
                    detailTr = "Kontroller normal kalır; kısa ve net düzeltmelere odaklan.",
                    anchor = TutorialAnchor.RIGHT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Final Run Setup",
                    titleTr = "Final Koşu Hazırlığı",
                    detailEn = "Confirm resources and start when focused.",
                    detailTr = "Kaynakları doğrula, odaklandığında başlat.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            61 -> listOf(
                BlockBriefingStep(
                    titleEn = "Combat Sector",
                    titleTr = "Savaş Sektörü",
                    detailEn = "This block swaps to a fighter-jet profile and military visuals.",
                    detailTr = "Bu blok savaş uçağı profiline ve askeri görselliğe geçer.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Missile Volleys",
                    titleTr = "Füze Salvoları",
                    detailEn = "Missile arcs collapse toward the core. Keep moving before they stack.",
                    detailTr = "Füze yayları merkeze çöker. Üst üste binmeden önce hareket et.",
                    anchor = TutorialAnchor.RIGHT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Linear Endgame Ramp",
                    titleTr = "Lineer Final Artışı",
                    detailEn = "Difficulty rises smoothly through level 70. Focus on rhythm, not panic.",
                    detailTr = "Zorluk 70'e kadar lineer yükselir. Paniğe değil ritme odaklan.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            71 -> listOf(
                BlockBriefingStep(
                    titleEn = "Bio Defense Theme",
                    titleTr = "Biyo Savunma Teması",
                    detailEn = "Your ship switches to a virus form. Walls are square antibody barriers.",
                    detailTr = "Gemin virüs formuna geçer. Duvarlar kare antikor bariyerlerdir.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "One-Use Skills",
                    titleTr = "Tek Kullanımlık Yetenek",
                    detailEn = "Shield and slowdown can each be used only once per run. Used buttons lock and dim.",
                    detailTr = "Kalkan ve yavaşlatma koşu başına sadece bir kez kullanılabilir. Kullanılan buton kilitlenir ve solar.",
                    anchor = TutorialAnchor.SUPPORT_PANEL
                ),
                BlockBriefingStep(
                    titleEn = "Late Campaign Economy",
                    titleTr = "Geç Kampanya Ekonomisi",
                    detailEn = "Coin gain is lower in this stage. Plan support usage and coin spending carefully.",
                    detailTr = "Bu etapta coin kazanımı düşüktür. Destek kullanımını ve coin harcamasını planlı kullan.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            75 -> listOf(
                BlockBriefingStep(
                    titleEn = "Bio Mid-Block",
                    titleTr = "Biyo Orta Blok",
                    detailEn = "Angular antibody crescents close from multiple lanes. Keep center timing clean.",
                    detailTr = "Köşeli antikor yarımayları birden fazla hattan kapanır. Merkez zamanlamanı temiz tut.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "One-Use Reminder",
                    titleTr = "Tek Kullanım Hatırlatması",
                    detailEn = "Shield and slowdown stay single-use per run. Locked buttons mean no second chance.",
                    detailTr = "Kalkan ve yavaşlatma koşu başına tek kullanımlıktır. Kilitli buton ikinci şans olmadığını gösterir.",
                    anchor = TutorialAnchor.SUPPORT_PANEL
                ),
                BlockBriefingStep(
                    titleEn = "Economy Pressure",
                    titleTr = "Ekonomi Baskısı",
                    detailEn = "Coin gain is tight here. Plan support spending per run.",
                    detailTr = "Bu bölgede coin kazanımı düşüktür. Destek harcamasını koşu bazında planla.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            81 -> listOf(
                BlockBriefingStep(
                    titleEn = "Space Ambush",
                    titleTr = "Uzay Pususu",
                    detailEn = "Enemy ships that are not your equipped skin appear from fixed lines and aim at the core.",
                    detailTr = "Takılı olmayan düşman gemiler sabit hatlardan gelir ve çekirdeği hedefler.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Pattern Rotation",
                    titleTr = "Desen Rotasyonu",
                    detailEn = "Early levels are mostly up/down lanes. Late levels mix left/right and diagonal pressure.",
                    detailTr = "İlk seviyeler çoğunlukla yukarı/aşağı hatlarıdır. Sonlarda sağ/sol ve çapraz baskı karışır.",
                    anchor = TutorialAnchor.RIGHT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Laser Discipline",
                    titleTr = "Lazer Disiplini",
                    detailEn = "Lasers collapse toward the black hole; keep calm lane timing and avoid panic turns.",
                    detailTr = "Lazerler kara deliğe çöker; sakin şerit zamanlaması koru, panik dönüşlerden kaçın.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            91 -> listOf(
                BlockBriefingStep(
                    titleEn = "Kitchen Layout",
                    titleTr = "Mutfak Yerleşimi",
                    detailEn = "Walls become flat bread lanes, not circular rings.",
                    detailTr = "Duvarlar dairesel değil, düz ekmek hatlarına dönüşür.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Giant Knives",
                    titleTr = "Dev Bıçaklar",
                    detailEn = "Knives are slower than missiles but much larger. Read trajectory early.",
                    detailTr = "Bıçaklar füzelerden yavaş ama çok daha büyüktür. Yörüngeyi erken oku.",
                    anchor = TutorialAnchor.RIGHT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Steady Difficulty Ramp",
                    titleTr = "Dengeli Zorluk Artışı",
                    detailEn = "Difficulty is around war-block intensity, with smoother early seconds per level.",
                    detailTr = "Zorluk savaş bloğuna yakın tutulur, her bölümün ilk saniyeleri daha yumuşak akar.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            100 -> listOf(
                BlockBriefingStep(
                    titleEn = "Kitchen Finale",
                    titleTr = "Mutfak Finali",
                    detailEn = "Bread walls move directly toward the center gap timing. Stay calm and read early.",
                    detailTr = "Ekmek duvarları doğrudan merkeze akar. Sakin kal ve boşluğu erken oku.",
                    anchor = TutorialAnchor.ARENA
                ),
                BlockBriefingStep(
                    titleEn = "Knife Volume",
                    titleTr = "Bıçak Hacmi",
                    detailEn = "Knives are slow but wide. Micro-adjust early instead of last-second turns.",
                    detailTr = "Bıçaklar yavaş ama geniştir. Son anda dönüş yerine erken mikro düzeltme yap.",
                    anchor = TutorialAnchor.RIGHT_CONTROL
                ),
                BlockBriefingStep(
                    titleEn = "Finish Strong",
                    titleTr = "Güçlü Bitir",
                    detailEn = "Do a final resource check and keep rhythm to close the campaign.",
                    detailTr = "Kampanyayı kapatmadan önce son kaynak kontrolünü yap ve ritmi koru.",
                    anchor = TutorialAnchor.READY_CARD
                )
            )
            else -> emptyList()
        }
    }

    private fun blockBriefingBody(levelNumber: Int): String {
        return when (levelNumber) {
            1 -> t(
                "In levels 1-10, focus on control rhythm, safe-gap reading and clean lane timing. Difficulty ramps gently for onboarding.",
                "1-10 seviyelerinde kontrol ritmi, boşluk okuma ve temiz şerit zamanlaması öne çıkar. Zorluk öğretici olacak şekilde kademeli artar."
            )
            11 -> t(
                "In levels 11-20, pull walls and faster ring rotations begin. Watch center pressure and avoid long wall contact.",
                "11-20 seviyelerinde itici duvarlar ve daha hızlı halka dönüşleri başlar. Merkez baskısını izle ve duvarla uzun temas etme."
            )
            21 -> t(
                "In levels 21-30, pace increases with fly-theme patterns and tighter read windows. Keep short micro-corrections.",
                "21-30 seviyelerinde sinek temalı desenlerle tempo artar ve okuma pencereleri daralır. Kısa mikro düzeltmeler kullan."
            )
            31 -> t(
                "In levels 31-40, mini mode starts. Object scale shrinks and time-bubble walls introduce temporary slow-motion interactions.",
                "31-40 seviyelerinde mini mod başlar. Nesneler küçülür ve zaman balonu duvarları geçici yavaşlatma etkileşimi getirir."
            )
            41 -> t(
                "In levels 41-50, pressure rises sharply: denser patterns, sharper lane changes and less recovery margin between threats.",
                "41-50 seviyelerinde baskı ciddi artar: daha yoğun desenler, daha keskin şerit değişimleri ve tehditler arası daha az toparlanma payı."
            )
            50 -> t(
                "Level 50 is the dense-pressure checkpoint: keep lane reads one step ahead and avoid long commitment turns.",
                "50. bölüm yoğun baskı kontrol noktasıdır: şerit okumayı bir adım önden yap ve uzun dönüşe kilitlenme."
            )
            51 -> t(
                "In levels 51-60, reactor storm rules apply without inversion: aggressive timing checks and consistency focus.",
                "51-60 seviyelerinde ters kontrol olmadan reaktör fırtınası kuralları aktif: agresif zamanlama kontrolleri ve istikrar odakta."
            )
            61 -> t(
                "In levels 61-70, combat visuals and missile-volley lanes join the core walls. Threat pressure scales linearly to the final level.",
                "61-70 seviyelerinde çekirdek duvarlara savaş görselleri ve füze salvo şeritleri eklenir. Tehdit baskısı son levele kadar lineer artar."
            )
            71 -> t(
                "In levels 71-80, the bio-defense concept starts: your ship takes a virus form and square antibody barriers close lanes. Shield and slowdown are one-use per run, so timing and resource discipline become critical.",
                "71-80 seviyelerinde biyo savunma konsepti başlar: gemin virüs formuna geçer ve kare antikor bariyerler şeritleri kapatır. Kalkan ve yavaşlatma koşu başına tek kullanımlıktır; bu yüzden zamanlama ve kaynak disiplini kritiktir."
            )
            75 -> t(
                "Level 75 highlights bio mid-block pressure: angular antibody walls close faster, so resource timing must stay disciplined.",
                "75. bölüm biyo orta blok baskısını öne çıkarır: köşeli antikor duvarları daha hızlı kapanır, bu yüzden kaynak zamanlaması disiplinli olmalıdır."
            )
            81 -> t(
                "In levels 81-90, the scene returns to space. Enemy ships hold fixed center-lines and fire laser-like projectiles toward the core. Early waves focus on vertical lanes; late waves mix horizontal and diagonal ambush patterns.",
                "81-90 seviyelerinde sahne tekrar uzaya döner. Düşman gemiler sabit merkez hatlarına yerleşip çekirdeğe lazer benzeri mermiler atar. Erken dalgalar dikey hatlara odaklanır; son dalgalar yatay ve çapraz pusu desenlerine karışır."
            )
            91 -> t(
                "In levels 91-100, the theme shifts to kitchen chaos: flat bread-lane walls move in and giant knives descend toward the core. Knife speed is lower than missiles, but hit volume is bigger. The opening moments of each level stay readable, then pressure ramps.",
                "91-100 seviyelerinde tema mutfak kaosuna geçer: düz ekmek hatları ve dev bıçaklar çekirdeğe akar. Bıçak hızı füzeden düşüktür ancak çarpışma hacmi daha büyüktür. Her bölümün açılışı okunabilir tutulur, sonra baskı artar."
            )
            100 -> t(
                "Level 100 is the final kitchen checkpoint: keep calm micro-corrections through center-bound bread lanes and knife volume.",
                "100. bölüm son mutfak kontrol noktasıdır: merkeze akan ekmek hatları ve bıçak hacmi içinde sakin mikro düzeltmelerle ilerle."
            )
            else -> t(
                "Read the current status panel and adapt before you commit.",
                "Başlamadan önce durum panelini oku ve ona göre konum al."
            )
        }
    }

    private fun controlHintForLevel(level: LevelConfig): String {
        return if (level.sectorCount <= 16) {
            t("TAP TO STEP TO THE NEXT LANE", "Dokunarak bir sonraki şeride geç")
        } else {
            t("HOLD LEFT OR RIGHT TO GLIDE", "Sola veya sağa basılı tutarak kay")
        }
    }

    private fun t(english: String, turkish: String): String {
        return if (settingsState.language == AppLanguage.TR) turkish else english
    }

    private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"

    private fun acikKapali(value: Boolean): String = if (value) "AÇIK" else "KAPALI"

    private fun wrappedLines(text: String, maxWidth: Float): List<String> {
        val cacheKey = "${text.hashCode()}:${maxWidth.toInt()}"
        return wrappedTextCache.getOrPut(cacheKey) {
            val lines = ArrayList<String>()
            val paragraphs = text.split('\n')
            for (paragraph in paragraphs) {
                if (paragraph.isBlank()) {
                    lines.add("")
                    continue
                }

                val words = paragraph.split(' ')
                val builder = StringBuilder()
                for (word in words) {
                    val candidate = if (builder.isEmpty()) word else "${builder} $word"
                    if (estimateTextWidth(candidate, false) > maxWidth && builder.isNotEmpty()) {
                        lines.add(builder.toString())
                        builder.clear()
                        builder.append(word)
                    } else {
                        if (builder.isNotEmpty()) {
                            builder.append(' ')
                        }
                        builder.append(word)
                    }
                }
                if (builder.isNotEmpty()) {
                    lines.add(builder.toString())
                }
            }
            lines
        }
    }

    private fun drawWrappedText(
        lines: List<String>,
        x: Float,
        startY: Float,
        lineHeight: Float,
        clipRect: UiRect,
        textFont: BitmapFont = font
    ) {
        lines.forEachIndexed { index, line ->
            val y = startY - lineHeight * index
            if (y < clipRect.y - lineHeight || y > clipRect.y + clipRect.height + lineHeight) {
                return@forEachIndexed
            }
            textFont.draw(batch, line, x, y, clipRect.width, Align.left, false)
        }
    }

    private fun applyHighQualityFiltering(texture: Texture) {
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }

    private fun createHighQualityTextureFromPixmap(pixmap: Pixmap, minEdge: Int = 0): Texture {
        val source = if (minEdge > 0 && minOf(pixmap.width, pixmap.height) < minEdge) {
            val scale = (minEdge.toFloat() / minOf(pixmap.width, pixmap.height).toFloat()).coerceAtLeast(1f)
            val targetW = (pixmap.width * scale).roundToInt().coerceAtLeast(pixmap.width)
            val targetH = (pixmap.height * scale).roundToInt().coerceAtLeast(pixmap.height)
            val upscaled = Pixmap(targetW, targetH, Pixmap.Format.RGBA8888)
            upscaled.drawPixmap(pixmap, 0, 0, pixmap.width, pixmap.height, 0, 0, targetW, targetH)
            upscaled
        } else {
            pixmap
        }
        val texture = Texture(source, true)
        applyHighQualityFiltering(texture)
        if (source !== pixmap) {
            source.dispose()
        }
        return texture
    }

    private fun loadTextureWithQuality(handle: FileHandle, minEdge: Int = 0): Texture? {
        return try {
            val pixmap = Pixmap(handle)
            val texture = createHighQualityTextureFromPixmap(pixmap, minEdge)
            pixmap.dispose()
            texture
        } catch (_: Throwable) {
            null
        }
    }

    // FluxCore ships no third-party raster icons. These stay null on purpose so every HUD
    // glyph comes from the ShapeRenderer vector paths below (drawHeartIcon, drawShieldIcon,
    // drawCoinIcon, drawTimeSlowIcon), which is also what makes the icon set self-consistent.
    private fun loadTutorialTouchIcon() {
        tutorialTouchIcon?.dispose()
        tutorialTouchIcon = null
    }

    private fun loadUiStatusIcons() {
        uiHeartIcon?.dispose()
        uiShieldIcon?.dispose()
        uiCoinIcon?.dispose()
        uiHeartIcon = null
        uiShieldIcon = null
        uiCoinIcon = null
    }

    private fun loadBlockBriefingState() {
        shownBlockBriefings.clear()
        val saved = profilePreferences.getString(STORE_BLOCK_BRIEFING_KEY, "")
        saved.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it >= 1 }
            .forEach { shownBlockBriefings.add(it) }
    }

    private fun saveBlockBriefingState() {
        profilePreferences.putString(
            STORE_BLOCK_BRIEFING_KEY,
            shownBlockBriefings.sorted().joinToString(",")
        ).flush()
    }

    private fun updateBlockBriefingForCurrentLevel() {
        val levelNumber = selectedLevelIndex + 1
        val briefingMilestones = setOf(21, 50, 75, 100)
        blockBriefingLevel = levelNumber
        blockBriefingStepIndex = 0
        blockBriefingVisible = levelNumber in briefingMilestones && !shownBlockBriefings.contains(levelNumber)
    }

    private fun advanceBlockBriefingStep(): Boolean {
        if (!blockBriefingVisible) {
            return true
        }
        val steps = blockBriefingSteps(blockBriefingLevel)
        if (steps.isEmpty()) {
            consumeBlockBriefingAndContinue()
            return true
        }
        if (blockBriefingStepIndex < steps.lastIndex) {
            blockBriefingStepIndex += 1
            playUiSound(uiConfirmSound, 0.6f, 1.04f)
            return false
        }
        consumeBlockBriefingAndContinue()
        return true
    }

    private fun consumeBlockBriefingAndContinue() {
        if (!blockBriefingVisible) {
            return
        }
        shownBlockBriefings.add(blockBriefingLevel)
        saveBlockBriefingState()
        blockBriefingVisible = false
        playUiSound(uiConfirmSound, 0.75f)
    }

    private fun loadAudioFx() {
        fun load(path: String): Sound? {
            return try {
                Gdx.audio.newSound(Gdx.files.internal(path))
            } catch (throwable: Throwable) {
                Gdx.app.error("Audio", "Unable to load sound: $path", throwable)
                null
            }
        }
        uiStartSound = load("sfx/ui_start.wav")
        uiConfirmSound = load("sfx/ui_confirm.wav")
        hitSound = load("sfx/hit.wav")
        clearSound = load("sfx/clear.wav")
        stormSound = load("sfx/storm.wav")
        wallPassSound = load("sfx/wall_pass.wav")
        shieldActivateSound = if (Gdx.files.internal("sfx/shield_on.wav").exists()) load("sfx/shield_on.wav") else null
        slowActivateSound = if (Gdx.files.internal("sfx/slow_on.wav").exists()) load("sfx/slow_on.wav") else null
        gameMusic?.dispose()
        if (secondaryGameMusic !== gameMusic) {
            secondaryGameMusic?.dispose()
        }
        uiMusic?.dispose()
        gameMusic = null
        secondaryGameMusic = null
        uiMusic = null

        val introTrack = loadBackgroundMusicCandidates(
            listOf("music/fluxcore_drift_loop.wav"),
            loop = false
        )
        val mainTrack = loadBackgroundMusicCandidates(
            listOf("music/fluxcore_pulse_loop.wav"),
            loop = false
        )
        uiMusic = loadBackgroundMusicCandidates(
            listOf(
                "music/fluxcore_ui_theme.wav",
                "music/fluxcore_ui_theme.ogg",
                "music/fluxcore_ui_theme.mp3"
            ),
            loop = true
        )

        if (introTrack != null && mainTrack != null) {
            introTrack.setOnCompletionListener { music ->
                onPlaylistTrackCompleted(music)
            }
            mainTrack.setOnCompletionListener { music ->
                onPlaylistTrackCompleted(music)
            }
            gameMusic = introTrack
            secondaryGameMusic = mainTrack
            gameTrackSwapTimerSeconds = 72f
            return
        }

        val singleTrack = introTrack ?: mainTrack
        singleTrack?.isLooping = true
        gameMusic = singleTrack
        secondaryGameMusic = null
        gameTrackSwapTimerSeconds = 0f
    }

    private fun onPlaylistTrackCompleted(completedTrack: Music) {
        if (secondaryGameMusic == null) {
            return
        }
        if (gameMusic !== completedTrack) {
            return
        }
        val next = secondaryGameMusic ?: return
        gameMusic = next
        secondaryGameMusic = completedTrack
        gameTrackSwapTimerSeconds = 72f
        try {
            val shouldPlayMusic = settingsState.soundEnabled && (overlayMode == OverlayMode.GAME || overlayMode == OverlayMode.PAUSE)
            completedTrack.pause()
            completedTrack.setPosition(0f)
            next.pause()
            next.setPosition(0f)
            gameMusic?.isLooping = false
            gameMusic?.volume = settingsState.musicVolume.coerceIn(0f, 1f)
            secondaryGameMusic?.pause()
            if (shouldPlayMusic) {
                gameMusic?.play()
            } else {
                gameMusic?.pause()
            }
        } catch (_: Throwable) {
            // Ignore backend issues.
        }
    }

    private fun loadBackgroundMusicCandidates(candidatePaths: List<String>, loop: Boolean): Music? {
        val selectedPath = candidatePaths.firstOrNull { path ->
            runCatching { Gdx.files.internal(path).exists() }.getOrDefault(false)
        } ?: return null
        return try {
            Gdx.audio.newMusic(Gdx.files.internal(selectedPath)).also {
                it.isLooping = loop
                it.volume = settingsState.musicVolume.coerceIn(0f, 1f)
            }
        } catch (throwable: Throwable) {
            Gdx.app.error("Audio", "Unable to load background music: $selectedPath", throwable)
            null
        }
    }

    private fun playUiSound(sound: Sound?, volume: Float = 1f, pitch: Float = 1f) {
        if (!settingsState.soundEnabled) {
            return
        }
        try {
            val boosted = (volume * settingsState.effectsVolume.coerceIn(0f, 1f) * 1.1f).coerceIn(0f, 1f)
            val levelIndex = if (overlayMode == OverlayMode.GAME || overlayMode == OverlayMode.PAUSE) {
                simulation.levelConfig.index
            } else {
                selectedLevelIndex + 1
            }
            val themePitch = when (levelIndex) {
                in 61..70 -> 0.96f
                in 71..80 -> 0.92f
                in 81..90 -> 1.06f
                in 91..100 -> 0.9f
                else -> 1f
            }
            sound?.play(boosted, (pitch * themePitch).coerceIn(0.5f, 2f), 0f)
        } catch (_: Throwable) {
            // Ignore missing audio backends.
        }
    }

    private fun syncAudioPlayback() {
        val shouldPlayGameMusic = settingsState.soundEnabled && (overlayMode == OverlayMode.GAME || overlayMode == OverlayMode.PAUSE)
        val shouldPlayUiMusic = settingsState.soundEnabled &&
            (
                overlayMode == OverlayMode.SPLASH ||
                    overlayMode == OverlayMode.EPILEPSY_WARNING ||
                    overlayMode == OverlayMode.INTRO ||
                    overlayMode == OverlayMode.MENU ||
                    overlayMode == OverlayMode.SHOP ||
                    overlayMode == OverlayMode.POLICY ||
                    overlayMode == OverlayMode.LEVEL_SELECT
                )
        val music = gameMusic
        val ui = uiMusic
        try {
            music?.volume = settingsState.musicVolume.coerceIn(0f, 1f)
            ui?.volume = settingsState.musicVolume.coerceIn(0f, 1f)
            if (music != null && secondaryGameMusic !== music) {
                secondaryGameMusic?.volume = settingsState.musicVolume.coerceIn(0f, 1f)
                if (secondaryGameMusic?.isPlaying == true) {
                    secondaryGameMusic?.pause()
                }
            }
            if (shouldPlayGameMusic) {
                if (ui?.isPlaying == true) {
                    ui.pause()
                }
                if (secondaryGameMusic?.isPlaying == true) {
                    secondaryGameMusic?.pause()
                }
                if (music != null && !music.isPlaying) {
                    music.play()
                }
                if (music != null && secondaryGameMusic != null) {
                    gameTrackSwapTimerSeconds = (gameTrackSwapTimerSeconds - Gdx.graphics.deltaTime).coerceAtLeast(0f)
                    if (gameTrackSwapTimerSeconds <= 0f && music.isPlaying) {
                        onPlaylistTrackCompleted(music)
                    }
                } else {
                    gameTrackSwapTimerSeconds = 0f
                }
            } else if (music?.isPlaying == true) {
                music.pause()
                if (secondaryGameMusic !== music && secondaryGameMusic?.isPlaying == true) {
                    secondaryGameMusic?.pause()
                }
            }
            if (shouldPlayUiMusic) {
                if (music?.isPlaying == true && !shouldPlayGameMusic) {
                    music.pause()
                }
                if (secondaryGameMusic?.isPlaying == true) {
                    secondaryGameMusic?.pause()
                }
                if (ui != null && !ui.isPlaying) {
                    ui.play()
                }
            } else if (ui?.isPlaying == true) {
                ui.pause()
            }
        } catch (_: Throwable) {
            // Keep game running on audio backend errors.
        }
    }

    private fun loadShipStore() {
        shipSkins.forEach { it.texture?.dispose() }
        shipSkins.clear()
        unlockedShipIds.clear()
        shipRotationOverrides.clear()

        val shipDefs = listOf(
            Triple("specter_7", t("Specter-7", "Hayalet-7"), 0),
            Triple("nova_arc", t("Nova Arc", "Nova Yay"), 80),
            Triple("ember_wing", t("Ember Wing", "Kor Kanat"), 120),
            Triple("ion_lancer", t("Ion Lancer", "İyon Mızrak"), 160),
            Triple("vortex_tail", t("Vortex Tail", "Vorteks Kuyruk"), 200),
            Triple("pulse_hawk", t("Pulse Hawk", "Nabız Şahin"), 250),
            Triple("aether_blade", t("Aether Blade", "Aether Bıçak"), 300),
            Triple("zenith_ray", t("Zenith Ray", "Zenith Işın"), 360),
            Triple("rift_runner", t("Rift Runner", "Yarık Koşucusu"), 420),
            Triple("gravity_falcon", t("Gravity Falcon", "Yerçekim Şahini"), 500),
            Triple("orion_prime", t("Orion Prime", "Orion Prime"), 620)
        )
        shipDefs.forEachIndexed { index, (id, name, price) ->
            shipSkins.add(createProceduralShipSkin(index, id, name, price))
        }

        val savedUnlockedRaw = profilePreferences.getString(STORE_UNLOCKED_KEY, "")
        val savedUnlocked = savedUnlockedRaw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        shipSkins.forEach { skin ->
            if (skin.price <= 0 || skin.id in savedUnlocked) {
                unlockedShipIds.add(skin.id)
            }
        }
        if (unlockedShipIds.isEmpty()) {
            unlockedShipIds.add(shipSkins.first().id)
        }
        val savedSelected = profilePreferences.getString(STORE_SELECTED_KEY, shipSkins.first().id)
        selectedShipId = if (savedSelected in unlockedShipIds) {
            savedSelected
        } else {
            unlockedShipIds.firstOrNull() ?: shipSkins.first().id
        }

        selectedShopShipIndex = shipSkins.indexOfFirst { it.id == selectedShipId }.coerceAtLeast(0)
        syncSelectedShipHitbox()
        saveShipStoreState()
        Gdx.app.log("ShipStore", "Loaded code ship profile, selected=$selectedShipId")
    }

    private fun createProceduralShipSkin(index: Int, id: String, displayName: String, price: Int): ShipSkin {
        val texture = createProceduralShipTexture(index).also { applyHighQualityFiltering(it) }
        val hitRadius = (0.022f + (index % 5) * 0.0012f).coerceIn(0.022f, 0.03f)
        return ShipSkin(
            id = id,
            displayName = displayName,
            assetPath = null,
            texture = texture,
            price = price,
            noseDirectionDeg = 0f,
            hitRadiusNorm = hitRadius
        )
    }

    private fun createProceduralShipTexture(index: Int): Texture {
        val pixmap = ShipArt.buildShipPixmap(index)
        val texture = createHighQualityTextureFromPixmap(pixmap, minEdge = 320)
        pixmap.dispose()
        return texture
    }

    private fun createForcedShipSkin(assetPath: String): ShipSkin? {
        return try {
            val handle = resolveShipHandle(assetPath) ?: return null
            if (!handle.exists()) {
                return null
            }
            val created = createShipSkin(handle, 1)
            if (created.texture != null) {
                return created.copy(price = 0)
            }
            val texture = loadTextureWithQuality(handle, minEdge = 320) ?: return null
            ShipSkin(
                id = "gemi_1_forced",
                displayName = "Gemi 1",
                assetPath = handle.path(),
                texture = texture,
                price = 0,
                noseDirectionDeg = 0f,
                hitRadiusNorm = 0.026f
            )
        } catch (throwable: Throwable) {
            Gdx.app.error("ShipStore", "Failed loading forced ship: $assetPath", throwable)
            null
        }
    }

    private fun createEmergencyShipSkin(): ShipSkin {
        val pixmap = Pixmap(128, 128, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        pixmap.setColor(0.2f, 0.92f, 1f, 1f)
        pixmap.fillTriangle(96, 64, 34, 28, 34, 100)
        pixmap.setColor(0.78f, 0.96f, 1f, 1f)
        pixmap.fillTriangle(78, 64, 42, 42, 42, 86)
        val texture = createHighQualityTextureFromPixmap(pixmap, minEdge = 256).also { applyHighQualityFiltering(it) }
        pixmap.dispose()
        return ShipSkin(
            id = "emergency_ship",
            displayName = t("Emergency Ship", "Acil Gemi"),
            assetPath = null,
            texture = texture,
            price = 0,
            noseDirectionDeg = 0f,
            hitRadiusNorm = 0.024f
        )
    }

    private fun discoverShipAssets(): List<FileHandle> {
        val discovered = LinkedHashMap<String, FileHandle>()

        fun collect(dir: FileHandle) {
            if (!dir.exists()) {
                return
            }
            dir.list().forEach { handle ->
                if (handle.isDirectory) {
                    if (!handle.path().contains("ui", ignoreCase = true)) {
                        collect(handle)
                    }
                    return@forEach
                }
                if (!handle.extension().equals("png", ignoreCase = true)) {
                    return@forEach
                }
                val filename = handle.name()
                val isUiAsset =
                    filename.contains("touch", ignoreCase = true) ||
                        filename.contains("icon", ignoreCase = true) ||
                        filename.contains("button", ignoreCase = true) ||
                        filename.contains("hud", ignoreCase = true)
                val isShipAsset =
                    filename.contains("gemi", ignoreCase = true) ||
                        filename.contains("ship", ignoreCase = true) ||
                        handle.path().contains("/ships/", ignoreCase = true) ||
                        handle.path().contains("\\ships\\", ignoreCase = true)
                if (!isUiAsset && isShipAsset) {
                    val key = filename.lowercase(Locale.US)
                    if (key !in discovered) {
                        discovered[key] = handle
                    }
                }
            }
        }

        shipSearchRoots().forEach { collect(it) }
        return discovered.values.sortedBy { it.name().lowercase(Locale.US) }
    }

    private fun shipSearchRoots(): List<FileHandle> {
        val roots = LinkedHashMap<String, FileHandle>()
        fun add(handle: FileHandle) {
            if (!handle.exists()) {
                return
            }
            roots[handle.path().lowercase(Locale.US)] = handle
        }

        add(Gdx.files.internal(""))
        add(Gdx.files.local("../../assets"))
        add(Gdx.files.local("../assets"))
        add(Gdx.files.local("assets"))

        val cwd = System.getProperty("user.dir")?.let { File(it) }
        if (cwd != null) {
            add(Gdx.files.absolute(cwd.resolve("../../assets").normalize().path))
            add(Gdx.files.absolute(cwd.resolve("../assets").normalize().path))
            add(Gdx.files.absolute(cwd.resolve("assets").normalize().path))
            var probe: File? = cwd
            repeat(6) {
                val assetsDir = probe?.resolve("assets")
                if (assetsDir != null) {
                    add(Gdx.files.absolute(assetsDir.normalize().path))
                }
                probe = probe?.parentFile
            }
        }
        return roots.values.toList()
    }

    private fun resolveShipHandle(assetPath: String): FileHandle? {
        shipSearchRoots().forEach { root ->
            val directChild = root.child(assetPath)
            if (directChild.exists()) {
                return directChild
            }
        }
        val candidates = listOf(
            Gdx.files.internal(assetPath),
            Gdx.files.local(assetPath),
            Gdx.files.local("assets/$assetPath"),
            Gdx.files.local("../../$assetPath"),
            Gdx.files.local("../../assets/$assetPath"),
            Gdx.files.local("../$assetPath"),
            Gdx.files.local("../assets/$assetPath"),
            Gdx.files.absolute(assetPath)
        )
        return candidates.firstOrNull { it.exists() }
    }

    private fun createShipSkin(handle: FileHandle, order: Int): ShipSkin {
        return try {
            val source = Pixmap(handle)
            var cleaned = source.stripEdgeBlackBackground()
            if (!cleaned.hasVisiblePixels()) {
                cleaned.dispose()
                cleaned = Pixmap(source.width, source.height, Pixmap.Format.RGBA8888).also { fallback ->
                    fallback.drawPixmap(source, 0, 0)
                }
            }
            val compact = cleaned.cropToOpaqueBounds() ?: cleaned
            val texture = createHighQualityTextureFromPixmap(compact, minEdge = 320).also { applyHighQualityFiltering(it) }
            val displayName = handle.nameWithoutExtension()
                .replace('_', ' ')
                .replace('-', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
            val id = displayName.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val noseDirection = compact.inferNoseDirectionDegrees()
            val hitRadius = compact.estimateShipHitRadiusNorm()
            source.dispose()
            if (compact !== cleaned) {
                cleaned.dispose()
            }
            compact.dispose()
            ShipSkin(
                id = id.ifBlank { "ship_$order" },
                displayName = displayName.ifBlank { "Ship $order" },
                assetPath = handle.path(),
                texture = texture,
                price = 0,
                noseDirectionDeg = noseDirection,
                hitRadiusNorm = hitRadius
            )
        } catch (throwable: Throwable) {
            Gdx.app.error("ShipStore", "Failed processing ship texture: ${handle.path()}", throwable)
            val directTexture = loadTextureWithQuality(handle, minEdge = 320)
            val fallbackName = handle.nameWithoutExtension().ifBlank { "Ship $order" }
            val fallbackId = fallbackName.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
            ShipSkin(
                id = fallbackId.ifBlank { "ship_$order" },
                displayName = fallbackName,
                assetPath = handle.path(),
                texture = directTexture,
                price = 0,
                noseDirectionDeg = 0f,
                hitRadiusNorm = 0.021f
            )
        }
    }

    private fun Pixmap.hasVisiblePixels(): Boolean {
        var visible = 0
        val threshold = maxOf(28, (width * height * 0.0035f).toInt())
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = decodePixelChannels(getPixel(x, y)).a
                if (alpha > 22) {
                    visible += 1
                    if (visible >= threshold) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun Pixmap.cropToOpaqueBounds(): Pixmap? {
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = decodePixelChannels(getPixel(x, y)).a
                if (alpha <= 20) {
                    continue
                }
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }
        if (maxX < minX || maxY < minY) {
            return null
        }
        val compactWidth = (maxX - minX + 1).coerceAtLeast(1)
        val compactHeight = (maxY - minY + 1).coerceAtLeast(1)
        val compact = Pixmap(compactWidth, compactHeight, Pixmap.Format.RGBA8888)
        compact.drawPixmap(this, 0, 0, minX, minY, compactWidth, compactHeight)
        return compact
    }

    private fun Pixmap.estimateShipHitRadiusNorm(): Float {
        var opaquePixels = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = decodePixelChannels(getPixel(x, y)).a
                if (alpha > 20) {
                    opaquePixels += 1
                }
            }
        }
        val area = (width * height).coerceAtLeast(1)
        val coverage = opaquePixels.toFloat() / area.toFloat()
        return (0.019f + coverage * 0.022f).coerceIn(0.02f, 0.038f)
    }

    private fun Pixmap.stripEdgeBlackBackground(): Pixmap {
        val processed = Pixmap(width, height, Pixmap.Format.RGBA8888)
        processed.drawPixmap(this, 0, 0)
        val visited = BooleanArray(width * height)
        val queue = java.util.ArrayDeque<Int>()
        val edgePalette = processed.collectEdgeBackgroundPalette()

        fun push(x: Int, y: Int) {
            if (x < 0 || y < 0 || x >= width || y >= height) {
                return
            }
            val index = y * width + x
            if (visited[index]) {
                return
            }
            if (!processed.isLikelyBackgroundPixel(x, y, edgePalette)) {
                return
            }
            visited[index] = true
            queue.addLast(index)
        }

        for (x in 0 until width) {
            push(x, 0)
            push(x, height - 1)
        }
        for (y in 0 until height) {
            push(0, y)
            push(width - 1, y)
        }

        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val x = index % width
            val y = index / width
            processed.drawPixel(x, y, 0x00000000)
            push(x - 1, y)
            push(x + 1, y)
            push(x, y - 1)
            push(x, y + 1)
        }
        processed.trimDarkMatteOnTransparentEdges(edgePalette)
        return processed
    }

    private fun Pixmap.collectEdgeBackgroundPalette(): List<PixelChannels> {
        val samples = ArrayList<PixelChannels>()
        val step = (minOf(width, height) / 36).coerceAtLeast(1)
        fun sample(x: Int, y: Int) {
            val channels = decodePixelChannels(getPixel(x, y))
            if (channels.a <= 18) {
                return
            }
            if (channels.luma() <= 82) {
                samples.add(channels)
            }
        }

        for (x in 0 until width step step) {
            sample(x, 0)
            sample(x, height - 1)
        }
        for (y in 0 until height step step) {
            sample(0, y)
            sample(width - 1, y)
        }

        if (samples.isEmpty()) {
            val fallback = listOf(
                decodePixelChannels(getPixel(0, 0)),
                decodePixelChannels(getPixel(width - 1, 0)),
                decodePixelChannels(getPixel(0, height - 1)),
                decodePixelChannels(getPixel(width - 1, height - 1))
            )
            return fallback.filter { it.a > 18 }
        }
        return samples.take(96)
    }

    private fun Pixmap.isLikelyBackgroundPixel(x: Int, y: Int, palette: List<PixelChannels>): Boolean {
        val channels = decodePixelChannels(getPixel(x, y))
        if (channels.a <= 18) {
            return false
        }
        if (channels.r < 34 && channels.g < 34 && channels.b < 34) {
            return true
        }
        if (channels.luma() > 108) {
            return false
        }
        if (palette.isEmpty()) {
            return false
        }
        val minDistance = palette.minOf { channels.distanceSq(it) }
        return minDistance <= 2500
    }

    private fun Pixmap.trimDarkMatteOnTransparentEdges(palette: List<PixelChannels>) {
        val toClear = ArrayList<Int>()
        for (y in 1 until (height - 1)) {
            for (x in 1 until (width - 1)) {
                val channels = decodePixelChannels(getPixel(x, y))
                if (channels.a <= 18 || channels.luma() > 95) {
                    continue
                }
                val touchesTransparent =
                    decodePixelChannels(getPixel(x - 1, y)).a <= 6 ||
                        decodePixelChannels(getPixel(x + 1, y)).a <= 6 ||
                        decodePixelChannels(getPixel(x, y - 1)).a <= 6 ||
                        decodePixelChannels(getPixel(x, y + 1)).a <= 6
                if (!touchesTransparent) {
                    continue
                }
                if (palette.isNotEmpty()) {
                    val minDistance = palette.minOf { channels.distanceSq(it) }
                    if (minDistance > 2200) {
                        continue
                    }
                }
                toClear.add(y * width + x)
            }
        }
        toClear.forEach { index ->
            val x = index % width
            val y = index / width
            drawPixel(x, y, 0x00000000)
        }
    }

    private fun Pixmap.inferNoseDirectionDegrees(): Float {
        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0
        var visibleCount = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = decodePixelChannels(getPixel(x, y)).a
                if (alpha < 20) {
                    continue
                }
                visibleCount += 1
                minX = minOf(minX, x)
                maxX = maxOf(maxX, x)
                minY = minOf(minY, y)
                maxY = maxOf(maxY, y)
            }
        }
        if (visibleCount == 0) {
            return 0f
        }

        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f
        var bestDistanceSq = 0f
        var bestAngleDeg = 0f

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val alpha = decodePixelChannels(getPixel(x, y)).a
                if (alpha < 20) {
                    continue
                }
                val dx = x - centerX
                val dy = (height - 1 - y) - (height - 1 - centerY)
                val distanceSq = dx * dx + dy * dy
                if (distanceSq > bestDistanceSq) {
                    bestDistanceSq = distanceSq
                    bestAngleDeg = (atan2(dy, dx) * MathUtils.radiansToDegrees)
                }
            }
        }
        return bestAngleDeg
    }

    private data class PixelChannels(
        val r: Int,
        val g: Int,
        val b: Int,
        val a: Int
    ) {
        fun luma(): Int {
            return ((r * 299) + (g * 587) + (b * 114)) / 1000
        }

        fun distanceSq(other: PixelChannels): Int {
            val dr = r - other.r
            val dg = g - other.g
            val db = b - other.b
            return dr * dr + dg * dg + db * db
        }
    }

    private fun decodePixelChannels(pixel: Int): PixelChannels {
        val lowAlpha = pixel and 0xff
        val highAlpha = (pixel ushr 24) and 0xff
        return if (highAlpha > lowAlpha + 2) {
            PixelChannels(
                r = pixel and 0xff,
                g = (pixel ushr 8) and 0xff,
                b = (pixel ushr 16) and 0xff,
                a = highAlpha
            )
        } else {
            PixelChannels(
                r = (pixel ushr 24) and 0xff,
                g = (pixel ushr 16) and 0xff,
                b = (pixel ushr 8) and 0xff,
                a = lowAlpha
            )
        }
    }

    private fun saveShipStoreState() {
        profilePreferences.putString(STORE_SELECTED_KEY, selectedShipId)
        profilePreferences.putString(STORE_UNLOCKED_KEY, unlockedShipIds.joinToString(","))
        shipRotationOverrides.forEach { (id, rotation) ->
            profilePreferences.putFloat("${STORE_ROTATION_KEY}$id", rotation)
        }
        profilePreferences.flush()
    }

    private fun policyPrivacy(): String {
        if (!adsEnabled()) {
            val en = """
                FLUXCORE – iOS PRIVACY, PREMIUM, AND VIRTUAL ITEMS POLICY
                Last Updated: 04.07.2026
                This policy applies to the iOS App Store version of FluxCore.

                1. Advertising
                The iOS App Store version does not include advertising SDKs and does not show banner, interstitial, or rewarded ads.

                2. Purchases and Premium
                FluxCore Premium, when offered, is a non-consumable one-time purchase processed by Apple App Store / StoreKit.
                The developer does not collect or store full payment card information.
                Premium ownership is used only to unlock premium gameplay rules such as unlimited lives on this device.

                3. Gameplay and Local Data
                FluxCore may store gameplay progress, settings, lives, shields, coins, equipped ships, premium status, and preferences locally on the device.
                This data is used to operate the game, restore progress, and provide purchased or earned gameplay features.

                4. Virtual Items
                Coins, ships, shields, premium benefits, and other virtual items are for in-game use only.
                They have no cash value, are non-transferable, and may not be sold, exchanged, or redeemed for money.

                5. Abuse and Security
                Cheating, purchase abuse, save manipulation, reverse engineering, bot/script use, or unauthorized system activity may lead to restricted access or loss of in-game progress.

                6. Contact
                For privacy questions, support requests, or legal notices, contact:
                luminadigitale@gmail.com
            """.trimIndent()
            val tr = """
                FLUXCORE – iOS GİZLİLİK, PREMIUM VE SANAL ÖĞE POLİTİKASI
                Son Güncelleme: 04.07.2026
                Bu politika FluxCore'un iOS App Store sürümü için geçerlidir.

                1. Reklamlar
                iOS App Store sürümü reklam SDK'sı içermez ve banner, geçiş veya ödüllü reklam göstermez.

                2. Satın Alımlar ve Premium
                FluxCore Premium sunulduğunda, Apple App Store / StoreKit üzerinden işlenen tüketilemeyen tek seferlik satın alımdır.
                Geliştirici tam ödeme kartı bilgilerini toplamaz veya saklamaz.
                Premium sahipliği yalnızca bu cihazda sınırsız can gibi premium oyun kurallarını açmak için kullanılır.

                3. Oyun ve Yerel Veri
                FluxCore oyun ilerlemesi, ayarlar, canlar, kalkanlar, coinler, seçili gemiler, premium durumu ve tercihleri cihazda yerel olarak saklayabilir.
                Bu veriler oyunu çalıştırmak, ilerlemeyi geri yüklemek ve satın alınan veya kazanılan oyun özelliklerini sunmak için kullanılır.

                4. Sanal Öğeler
                Coinler, gemiler, kalkanlar, premium avantajlar ve diğer sanal öğeler yalnızca oyun içi kullanım içindir.
                Gerçek para değeri taşımaz, devredilemez, satılamaz, takas edilemez ve nakde çevrilemez.

                5. Kötüye Kullanım ve Güvenlik
                Hile, satın alma suistimali, kayıt verisi manipülasyonu, tersine mühendislik, bot/script kullanımı veya yetkisiz sistem etkinliği erişim kısıtına veya oyun ilerlemesinin kaybına yol açabilir.

                6. İletişim
                Gizlilik soruları, destek talepleri veya yasal bildirimler için:
                luminadigitale@gmail.com
            """.trimIndent()
            return t(en, tr)
        }
        val en = """
            FLUXCORE – ADS, PREMIUM, AND VIRTUAL ITEMS POLICY
            Last Updated: 28.03.2026
            This policy applies to ads, premium features, coins, virtual items, and rewarded ad flows in FluxCore.

            1. Types of Ads
            - Interstitial ads
            - Banner or similar display ads
            - Optional rewarded ads

            2. Rewarded Ads
            Players may watch rewarded ads to receive:
            - Extra lives
            - Extra shields
            - Temporary gameplay benefits
            - Double coin rewards after level completion
            Reward delivery requires successful load, playback, and completion.

            3. Technical Interruptions
            Rewards may fail due to:
            - Early ad close
            - Internet connectivity issues
            - Ad provider failures
            - Device incompatibility
            - Server synchronization errors

            4. Premium Content
            Premium may include:
            - Ad-free gameplay
            - Unlimited lives
            - Verified one-time ownership
            - Access to specific content
            Premium is intended as an optional one-time purchase, not a subscription. Premium scope may vary by version and can change over time.

            5. Coins and Virtual Items
            Coins and virtual items:
            - Are for in-game use only
            - Have no cash value
            - Are non-transferable
            - May not be sold, exchanged, or redeemed for cash
            - Do not create real-world ownership rights

            6. Abuse
            In case of cheating, exploit use, ad manipulation, purchase abuse, or unauthorized system activity, FluxCore may:
            - Remove rewards
            - Revoke coins
            - Restrict premium access
            - Suspend or terminate game access

            Contact: luminadigitale@gmail.com
        """.trimIndent()
        val tr = """
            FLUXCORE – REKLAM, PREMIUM VE SANAL ÖĞE POLİTİKASI
            Son Güncelleme: 28.03.2026
            Bu politika FluxCore içindeki reklamlar, premium özellikler, coinler, sanal öğeler ve ödüllü reklam akışı için geçerlidir.

            1. Reklam Türleri
            - Geçiş reklamları
            - Banner veya benzeri gösterimler
            - İsteğe bağlı ödüllü reklamlar

            2. Ödüllü Reklamlar
            Oyuncu reklam izleyerek şunları kazanabilir:
            - Ek can
            - Ek kalkan
            - Geçici oyun içi avantajlar
            - Bölüm sonu coin ödülünü ikiye katlama
            Ödül teslimi için reklamın doğru yüklenmesi, oynatılması ve tamamlanması gerekir.

            3. Teknik Kesintiler
            Ödül teslim edilmeyebilir:
            - Reklamın erken kapanması
            - İnternet bağlantısı sorunu
            - Reklam sağlayıcı hatası
            - Cihaz uyumsuzluğu
            - Sunucu senkronizasyon hatası

            4. Premium İçerik
            Premium şunları içerebilir:
            - Reklamsız deneyim
            - Sınırsız can
            - Doğrulanmış tek seferlik sahiplik
            - Belirli içeriklere erişim
            Premium; abonelik değil, isteğe bağlı tek seferlik satın alım olarak kurgulanır. Premium kapsamı sürüme göre farklılık gösterebilir ve zamanla güncellenebilir.

            5. Coin ve Sanal Öğeler
            Coin ve diğer sanal öğeler:
            - Yalnızca oyun içi kullanım içindir
            - Gerçek para karşılığı değildir
            - Devredilemez, satılamaz, takas edilemez
            - Nakde çevrilemez
            - Hukuken gerçek dünya mülkiyet hakkı oluşturmaz

            6. Kötüye Kullanım
            Hile, exploit, reklam manipülasyonu, satın alma suistimali veya yetkisiz sistem kullanımı halinde FluxCore:
            - Ödülleri iptal edebilir
            - Coinleri geri alabilir
            - Premium erişimi sınırlayabilir
            - Oyuna erişimi askıya alabilir veya sonlandırabilir

            İletişim: luminadigitale@gmail.com
        """.trimIndent()
        return t(en, tr)
    }

    private fun policyTerms(): String {
        if (!adsEnabled()) {
            val en = """
                FLUXCORE – iOS TERMS OF USE
                Last Updated: 04.07.2026
                By downloading, installing, accessing, or playing FluxCore, you accept these Terms.

                1. Description of the Service
                FluxCore is a reflex and timing focused space survival game with levels, lives, shields, coins, premium features, virtual items, and multilingual support.
                The iOS App Store version does not show ads.

                2. Eligibility
                You represent that you meet your local digital consent age and have parental/legal permission where required.

                3. License
                You are granted a limited, revocable, non-transferable, non-exclusive license for personal, non-commercial use.
                You may not copy, sell, rent, redistribute, reverse engineer, manipulate security/economy/premium systems, use cheats/bots/scripts/exploits, or interfere with operation.

                4. Gameplay Experience and Accessibility
                FluxCore may contain bright light effects, rapid movement, sudden visual transitions, and flashing/rhythmic patterns.
                The game may not be suitable for light-sensitive users.

                5. In-Game Systems
                FluxCore may include lives, shields, coins, hangar/shop systems, premium access, and virtual items.
                Scope, pricing, balance, and availability may change over time.

                6. Premium, Purchases, and Virtual Items
                Premium, when offered, is a one-time non-consumable purchase processed by Apple App Store / StoreKit.
                Virtual items are non-transferable, may not be sold/exchanged/cashed out, and have no real-world monetary value.

                7. Progress, Storage, and Data Retention
                Progress may be stored locally and/or via platform services.
                Data loss or sync mismatch may occur due to device/app/platform/technical issues.

                8. Prohibited Conduct
                Cheating, exploit use, unfair advantage, payment abuse, platform rule violations, unlawful use, and technical sabotage are prohibited.

                9. Updates and Changes
                FluxCore may change level structure, visuals, balance, economy, premium scope, and infrastructure over time.

                10. Contact
                luminadigitale@gmail.com

                FLUXCORE – HEALTH AND SAFETY WARNING
                FluxCore may contain bright, fast, and flashing visual effects. Stop playing immediately if eye strain, dizziness, nausea, severe headache, disorientation, or seizure-like symptoms occur.
                Play in a well-lit room, lower brightness, take regular breaks, avoid long sessions, and avoid playing while tired.
            """.trimIndent()
            val tr = """
                FLUXCORE – iOS KULLANIM ŞARTLARI
                Son Güncelleme: 04.07.2026
                FluxCore oyununu indirerek, kurarak, erişerek veya oynayarak bu Şartları kabul etmiş olursunuz.

                1. Hizmetin Tanımı
                FluxCore; seviyeler, can sistemi, kalkan sistemi, coin, premium özellikler, sanal öğeler ve çoklu dil desteği içerebilen refleks tabanlı bir uzay hayatta kalma oyunudur.
                iOS App Store sürümü reklam göstermez.

                2. Uygunluk
                Yerel dijital onay yaşını karşıladığınızı ve gerekli durumlarda ebeveyn/yasal temsilci iznine sahip olduğunuzu kabul edersiniz.

                3. Lisans ve Kullanım Hakkı
                FluxCore'u kişisel ve ticari olmayan amaçla kullanmanız için sınırlı, geri alınabilir, devredilemez, münhasır olmayan lisans verilir.
                Oyunu kopyalamak, satmak, kiralamak, yeniden dağıtmak, tersine mühendislik yapmak, güvenlik/ekonomi/premium sistemlerini manipüle etmek, hile-bot-script-exploit kullanmak yasaktır.

                4. Oyun Deneyimi ve Erişilebilirlik
                Oyunda parlak ışık, hızlı hareket, ani görsel geçişler ve ritmik/yanıp sönen efektler bulunabilir.
                Işığa duyarlı kullanıcılar için uygun olmayabilir.

                5. Oyun İçi Sistemler
                FluxCore içinde can, kalkan, coin, hangar/mağaza, premium erişim ve sanal öğeler bulunabilir.
                Kapsam, denge, fiyatlandırma ve erişim koşulları zamanla değişebilir.

                6. Premium, Satın Alımlar ve Sanal Öğeler
                Premium sunulduğunda, Apple App Store / StoreKit üzerinden işlenen tüketilemeyen tek seferlik satın alımdır.
                Coin, gemi, kalkan, premium avantajlar ve sanal öğeler gerçek para değeri taşımaz; devredilemez, satılamaz, takas edilemez, nakde çevrilemez.

                7. İlerleme ve Veri Saklama
                İlerleme verileri cihazda ve/veya platform hizmetlerinde saklanabilir.
                Teknik nedenlerle veri kaybı veya senkronizasyon hatası yaşanabilir.

                8. Yasaklı Kullanımlar
                Oyun dengesini bozma, haksız avantaj, ödeme suistimali, platform kurallarını ihlal etme, yasa dışı kullanım ve teknik sabotaj yasaktır.

                9. Güncellemeler ve Değişiklikler
                Seviye yapısı, görseller, zorluk, coin ekonomisi, premium kapsamı ve teknik altyapı değiştirilebilir.

                10. İletişim
                luminadigitale@gmail.com

                FLUXCORE – SAĞLIK VE GÜVENLİK UYARISI
                FluxCore parlak, hızlı ve yanıp sönen görsel efektler içerebilir. Göz yorgunluğu, baş dönmesi, mide bulantısı, şiddetli baş ağrısı, bilinç bulanıklığı veya nöbet benzeri belirti olursa hemen oynamayı bırakın.
                İyi aydınlatılmış ortamda oynayın, parlaklığı düşürün, düzenli mola verin, uzun oturumlardan kaçının ve yorgunken oynamayın.
            """.trimIndent()
            return t(en, tr)
        }
        val en = """
            FLUXCORE – TERMS OF USE
            Last Updated: 28.03.2026
            By downloading, installing, accessing, or playing FluxCore, you accept these Terms.

            1. Description of the Service
            FluxCore is a reflex and timing focused space survival game with levels, lives, shields, coins, rewarded ads, premium features, and multilingual support.

            2. Eligibility
            You represent that:
            - You are at least 13 years old, or meet your local digital consent age
            - You have parental/legal permission where required
            - You have legal capacity to accept these Terms

            3. License
            You are granted a limited, revocable, non-transferable, non-exclusive license for personal, non-commercial use.
            You may not copy, sell, rent, redistribute, reverse engineer, manipulate security/economy/ads/premium, use cheats/bots/scripts/exploits, or interfere with operation.

            4. Gameplay Experience and Accessibility
            FluxCore may contain bright light effects, rapid movement, sudden visual transitions, and flashing/rhythmic patterns.
            The game may not be suitable for light-sensitive users.

            5. In-Game Systems
            FluxCore may include:
            - Life system
            - Shield system
            - Coin system
            - Hangar/shop systems
            - Rewarded ads
            - Premium access
            Scope, pricing, balance, and availability may change over time.

            6. Advertising
            Ads can be shown as part of free use. Some ads are automatic; some are optional rewarded ads.
            Reward delivery is not guaranteed in all technical conditions.

            7. Premium, Purchases, and Virtual Items
            Premium, when offered, is a one-time purchase processed by app stores/payment providers.
            Virtual items are non-transferable, may not be sold/exchanged/cashed out, and have no real-world monetary value.

            8. Progress, Storage, and Data Retention
            Progress may be stored locally and/or via platform/cloud services.
            Data loss or sync mismatch may occur due to device/app/platform/technical issues.

            9. Language Support
            FluxCore supports Turkish and English.
            Device language can determine startup language.

            10. Prohibited Conduct
            Cheating, exploit use, unfair advantage, payment abuse, violation of platform rules, unlawful use, and technical sabotage are prohibited.

            11. Updates and Changes
            FluxCore may change level structure, visuals, balance, economy, ad behavior, premium scope, and infrastructure over time.

            12. Suspension or Termination
            FluxCore may be modified, suspended, or discontinued for maintenance, security, legal, anti-abuse, technical, or business reasons.

            13. Disclaimer
            FluxCore is provided “as is” without warranties of uninterrupted availability, error-free operation, full compatibility on every device, or continuous access.

            14. Limitation of Liability
            To the maximum extent permitted by law, developer liability is limited regarding progress/data loss, incompatibility, ad failures, purchase disputes, health effects, indirect damages, and revenue loss.

            15. Contact
            luminadigitale@gmail.com

            FLUXCORE – HEALTH AND SAFETY WARNING
            Important: FluxCore may contain bright, fast, and flashing visual effects.

            1. Photosensitivity and Epilepsy
            Visual effects may cause dizziness, nausea, eye strain, headache, disorientation, balance loss, or seizure-like reactions in sensitive users.

            2. Do Not Play If
            Do not play (or consult a doctor first) if you have epilepsy history, photosensitivity, neurological sensitivity, severe migraine/balance issues, exhaustion, or if substances/medication affect reaction.

            3. Stop Immediately If Symptoms Occur
            Stop immediately if eye twitching, involuntary movement, visual disturbance, dizziness, faintness, severe headache, nausea, disorientation, or seizure-like symptoms occur.

            4. Safe Play Recommendations
            Play in a well-lit room, lower brightness, take regular breaks, avoid long sessions, do not sit too close, and avoid playing while tired.

            5. Children
            Parents/guardians should monitor children for sensitivity and overstimulation symptoms, and limit playtime when needed.

            6. Responsibility
            You use FluxCore at your own risk. Consult a healthcare professional if needed.
        """.trimIndent()
        val tr = """
            FLUXCORE – KULLANIM ŞARTLARI
            Son Güncelleme: 28.03.2026
            FluxCore oyununu indirerek, kurarak, erişerek veya oynayarak bu Şartları kabul etmiş olursunuz.

            1. Hizmetin Tanımı
            FluxCore; seviyeler, can sistemi, kalkan sistemi, coin, ödüllü reklam, premium özellikler ve çoklu dil desteği içerebilen refleks tabanlı bir uzay hayatta kalma oyunudur.

            2. Uygunluk
            Şunları beyan etmiş olursunuz:
            - En az 13 yaşında olmak veya ülkenizdeki dijital onay yaşını karşılamak
            - Gerekli durumlarda ebeveyn/yasal temsilci iznine sahip olmak
            - Bu şartları yasal olarak kabul etme ehliyetine sahip olmak

            3. Lisans ve Kullanım Hakkı
            FluxCore’u kişisel ve ticari olmayan amaçla kullanmanız için sınırlı, geri alınabilir, devredilemez, münhasır olmayan lisans verilir.
            Oyunu kopyalamak, satmak, kiralamak, yeniden dağıtmak, tersine mühendislik yapmak, güvenlik/ekonomi/reklam/premium sistemini manipüle etmek, hile-bot-script-exploit kullanmak yasaktır.

            4. Oyun Deneyimi ve Erişilebilirlik
            Oyunda parlak ışık, hızlı hareket, ani görsel geçişler ve ritmik/yanıp sönen efektler bulunabilir.
            Işığa duyarlı kullanıcılar için uygun olmayabilir.

            5. Oyun İçi Sistemler
            FluxCore içinde can, kalkan, coin, hangar/mağaza, ödüllü reklam ve premium sistemleri bulunabilir.
            Kapsam, denge, fiyatlandırma ve erişim koşulları zamanla değişebilir.

            6. Reklamlar
            Ücretsiz kullanım kapsamında reklam gösterilebilir.
            Ödüllü reklamlarda ödül yalnızca reklam başarıyla tamamlandığında verilir.
            Teknik nedenlerle ödül her zaman teslim edilemeyebilir.

            7. Premium, Satın Alımlar ve Sanal Öğeler
            Premium sunulduğunda, tek seferlik satın alım ilgili mağaza/ödeme platformunda işlenir.
            Coin, gemi, kalkan, premium avantajlar ve sanal öğeler gerçek para değeri taşımaz; devredilemez, satılamaz, takas edilemez, nakde çevrilemez.

            8. İlerleme ve Veri Saklama
            İlerleme verileri cihazda/platformda saklanabilir.
            Teknik nedenlerle veri kaybı veya senkronizasyon hatası yaşanabilir.

            9. Dil Desteği
            FluxCore Türkçe ve İngilizceyi destekler.
            Cihaz dili Türkçe ise Türkçe, diğer dillerde varsayılan İngilizce açılış uygulanabilir.

            10. Yasaklı Kullanımlar
            Oyun dengesini bozma, haksız avantaj, ödeme suistimali, teknik açık arama/kullanma/yayma, platform kurallarını ihlal etme ve yasa dışı kullanım yasaktır.

            11. Güncellemeler ve Değişiklikler
            Seviye yapısı, görseller, zorluk, coin ekonomisi, reklam sıklığı, premium kapsamı ve teknik altyapı değiştirilebilir.

            12. Hizmetin Askıya Alınması/Sonlandırılması
            Bakım, güvenlik, teknik/yasal zorunluluk, kötüye kullanım önleme veya ticari kararlarla hizmet değiştirilebilir, askıya alınabilir veya sonlandırılabilir.

            13. Feragat
            FluxCore; hatasız, kesintisiz, her cihazda tam uyumlu veya sürekli erişilebilir olacağı garantisi olmadan sunulur.

            14. Sorumluluğun Sınırlandırılması
            Hukukun izin verdiği ölçüde geliştirici; veri/ilerleme kaybı, cihaz uyumsuzluğu, reklam kaynaklı aksama, satın alma uyuşmazlığı, sağlık etkileri ve dolaylı zararlardan sorumlu tutulamaz.

            15. İletişim
            luminadigitale@gmail.com

            FLUXCORE – SAĞLIK VE GÜVENLİK UYARISI
            Önemli: FluxCore parlak, hızlı ve yanıp sönen görsel efektler içerebilir.

            1. Işığa Duyarlılık ve Epilepsi
            Bu görseller hassas kişilerde baş dönmesi, mide bulantısı, göz yorgunluğu, baş ağrısı, denge kaybı, bilinç bulanıklığı veya nöbet benzeri reaksiyonlara yol açabilir.

            2. Aşağıdaki Durumlarda Oynamayın
            Epilepsi geçmişi, ışığa duyarlılık, nörolojik hassasiyet/migren/denge bozukluğu, aşırı yorgunluk/hastalık, dikkat etkileyen madde/ilaç etkisi varsa oynamayın veya önce doktora danışın.

            3. Belirti Görülürse Derhal Bırakın
            Göz seğirmesi, istemsiz hareket, görme bozulması, sersemlik, bayılma hissi, şiddetli baş ağrısı, mide bulantısı, yön kaybı, nöbet veya bilinç değişikliği halinde oynamayı bırakın.

            4. Güvenli Oynama Önerileri
            İyi aydınlatma, düşük ekran parlaklığı, düzenli mola, yeterli mesafe ve yorgunken oynamama önerilir.

            5. Çocuklar
            Ebeveynler çocuklarda ışığa duyarlılık/rahatsızlık/aşırı uyarılma belirtilerini takip etmeli ve gerekirse süreyi sınırlandırmalıdır.

            6. Sorumluluk
            Oyunun kullanımı kullanıcının kendi sorumluluğundadır; özel sağlık durumlarında kullanım öncesi sağlık uzmanına danışılmalıdır.
        """.trimIndent()
        return t(en, tr)
    }

    private fun policyLicense(): String {
        val en = """
            FLUXCORE – ASSET OWNERSHIP & ATTRIBUTION
            Owner: Taha Bayar
            Contact: luminadigitale@gmail.com

            Audio:
            Every music loop and sound effect in FluxCore is original work owned by the
            developer, synthesised in-house for this game. No sample pack, stock library,
            royalty-free catalogue, or third-party recording is bundled or used.

            Graphics:
            All ships, icons, HUD glyphs, effects, and level visuals are generated at runtime
            from FluxCore's own procedural drawing code. No stock art, icon pack, sprite
            sheet, purchased template, or emoji artwork is bundled or used.

            Third-party components:
            The only third-party asset in this app is the Noto Sans typeface, used for
            multilingual text and distributed under the SIL Open Font License 1.1.
            https://openfontlicense.org

            FluxCore's gameplay code, level design, and content are written for this app and
            are not derived from any template, asset flip, or reused project.
        """.trimIndent()
        val tr = """
            FLUXCORE – VARLIK SAHİPLİĞİ VE ATIF
            Sahibi: Taha Bayar
            İletişim: luminadigitale@gmail.com

            Ses:
            FluxCore içindeki tüm müzik döngüleri ve ses efektleri geliştiriciye ait özgün
            eserlerdir ve bu oyun için kurum içinde sentezlenmiştir. Hiçbir sample paketi,
            stok kütüphane, telifsiz katalog veya üçüncü taraf kayıt kullanılmamıştır.

            Grafik:
            Tüm gemiler, ikonlar, HUD sembolleri, efektler ve seviye görselleri FluxCore'un
            kendi prosedürel çizim koduyla çalışma anında üretilir. Hiçbir stok görsel, ikon
            paketi, sprite sayfası, satın alınmış şablon veya emoji grafiği kullanılmamıştır.

            Üçüncü taraf bileşenler:
            Uygulamadaki tek üçüncü taraf varlık, çok dilli metin için kullanılan ve SIL Open
            Font License 1.1 ile dağıtılan Noto Sans yazı tipidir.
            https://openfontlicense.org

            FluxCore'un oyun kodu, seviye tasarımı ve içeriği bu uygulama için yazılmıştır;
            herhangi bir şablondan, hazır varlık paketinden veya yeniden paketlenmiş bir
            projeden türetilmemiştir.
        """.trimIndent()
        return t(en, tr)
    }

    private fun createFont(path: String, size: Int): BitmapFont {
        val generator = FreeTypeFontGenerator(Gdx.files.internal(path))
        val font = generator.generateFont(
            FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                this.size = size
                minFilter = Texture.TextureFilter.MipMapLinearLinear
                magFilter = Texture.TextureFilter.Linear
                genMipMaps = true
                hinting = FreeTypeFontGenerator.Hinting.Slight
                characters =
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
                        " .,;:!?+-/()[]{}'\"&%#@=<>|_" +
                        "ÇĞİÖŞÜçğıöşüâîûÂÎÛ¦¦"
            }
        )
        generator.dispose()
        return font
    }

    override fun pause() {
        try {
            gameMusic?.pause()
            if (secondaryGameMusic !== gameMusic) {
                secondaryGameMusic?.pause()
            }
            uiMusic?.pause()
        } catch (_: Throwable) {
            // Ignore.
        }
    }

    override fun resume() {
        syncAudioPlayback()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        try {
            gameMusic?.stop()
            if (secondaryGameMusic !== gameMusic) {
                secondaryGameMusic?.stop()
            }
            uiMusic?.stop()
        } catch (_: Throwable) {
            // Ignore.
        }
        gameMusic?.dispose()
        if (secondaryGameMusic !== gameMusic) {
            secondaryGameMusic?.dispose()
        }
        uiMusic?.dispose()
        uiStartSound?.dispose()
        uiConfirmSound?.dispose()
        hitSound?.dispose()
        clearSound?.dispose()
        stormSound?.dispose()
        wallPassSound?.dispose()
        shieldActivateSound?.dispose()
        slowActivateSound?.dispose()
        shapes.dispose()
        batch.dispose()
        tutorialTouchIcon?.dispose()
        uiHeartIcon?.dispose()
        uiShieldIcon?.dispose()
        uiCoinIcon?.dispose()
        shipSkins.forEach { it.texture?.dispose() }
        font.dispose()
        titleFont.dispose()
        uiTitleFont.dispose()
        metricFont.dispose()
        bodyFont.dispose()
        metaFont.dispose()
        chipFont.dispose()
        buttonFont.dispose()
    }
}
