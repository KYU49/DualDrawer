package icu.ejapon.dual_drawer

import android.annotation.SuppressLint
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Possible values of [DrawerState].
 */
enum class DrawerValue {
    /**
     * The state of the drawer when it is closed.
     */
    Closed,

    /**
     * The state of the drawer when it is open.
     */
    LeftOpen,   //FIX 左の値用に変更
    RightOpen   //ADD 右の値用に追加
}

/**
 * State of the [DoubleModalDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@OptIn(ExperimentalMaterialApi::class)
@Stable
class DrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
) {

    internal val swipeableState = SwipeableV2State(
        initialValue = initialValue,
        animationSpec = AnimationSpec,
        confirmValueChange = confirmStateChange,
        velocityThreshold = DrawerVelocityThreshold
    )

    /**
     * Whether the drawer is open.
     */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.LeftOpen || currentValue == DrawerValue.RightOpen   //FIX 右の場合もtrueになるように

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer
     * currently in. If a swipe or an animation is in progress, this corresponds the state drawer
     * was in before the swipe or animation started.
     */
    val currentValue: DrawerValue
        get() {
            return swipeableState.currentValue
        }

    /**
     * Whether the state is currently animating.
     */
    val isAnimationRunning: Boolean
        get() {
            return swipeableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun openLeft() = swipeableState.animateTo(DrawerValue.LeftOpen)
    suspend fun openRight() = swipeableState.animateTo(DrawerValue.RightOpen)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() = swipeableState.animateTo(DrawerValue.Closed)


    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: DrawerValue) {
        swipeableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalMaterialApi
    val targetValue: DrawerValue
        get() = swipeableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet, or null before the offset is
     * initialized.
     * @see [SwipeableV2State.offset] for more information.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:Suppress("AutoBoxing")
    @get:ExperimentalMaterialApi
    val offset: Float?
        get() = swipeableState.offset

    internal fun requireOffset(): Float = swipeableState.requireOffset()

    companion object {
        /**
         * The default [Saver] implementation for [DrawerState].
         */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it, confirmStateChange) }
            )
    }
}


/**
 * Create and [remember] a [DrawerState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberCustomDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
): DrawerState {
    return rememberSaveable(saver = DrawerState.Saver(confirmStateChange)) {
        DrawerState(initialValue, confirmStateChange)
    }
}


/**
 * <a href="https://material.io/components/navigation-drawer#modal-drawer" class="external" target="_blank">Material Design modal navigation drawer</a>.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim.
 * They are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * ![Modal drawer image](https://developer.android.com/images/reference/androidx/compose/material/modal-drawer.png)
 *
 * See [BottomDrawer] for a layout that introduces a bottom drawer, suitable when
 * using bottom navigation.
 *
 * @sample androidx.compose.material.samples.ModalDrawerSample
 *
 * @param leftDrawerContent composable that represents content inside the drawer
 * @param modifier optional modifier for the drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerShape shape of the drawer sheet
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow below the
 * drawer sheet
 * @param drawerBackgroundColor background color to be used for the drawer sheet
 * @param drawerContentColor color of the content to use inside the drawer sheet. Defaults to
 * either the matching content color for [drawerBackgroundColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param content content of the rest of the UI
 *
 * @throws IllegalStateException when parent has [Float.POSITIVE_INFINITY] width
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun DoubleModalDrawer(
    leftDrawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    rightDrawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberCustomDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val modalDrawerConstraints = constraints

        // TODO : think about Infinite max bounds case
        if (!modalDrawerConstraints.hasBoundedWidth) {
            throw IllegalStateException("Drawer shouldn't have infinite width")
        }

        val minValue = -modalDrawerConstraints.maxWidth.toFloat()
        val maxValue = 0f

        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            Modifier
                .swipeableV2(
                    state = drawerState.swipeableState,
                    orientation = Orientation.Horizontal,
                    enabled = gesturesEnabled,
                    reverseDirection = isRtl
                )
                .swipeAnchors(
                    drawerState.swipeableState,
                    possibleValues = setOf(DrawerValue.LeftOpen, DrawerValue.Closed, DrawerValue.RightOpen) //ADD 右Drawerを開いた際の値を追加
                ) { value, _ ->
                    when (value) {
                        //FIX Closeの際にoffsetが0になるように修正
                        DrawerValue.LeftOpen -> leftDrawerContent?.let{-minValue}?:0f   //FIX 左Drawerがnullなら、開かないようにする
                        DrawerValue.Closed -> maxValue
                        DrawerValue.RightOpen -> rightDrawerContent?.let{minValue}?:0f   //FIX 右Drawerがnullなら、開かないようにする
                    }
                }
        ) {
            Box {
                content()
            }
            Scrim(
                open = drawerState.isOpen,
                onClose = {
                    if (
                        gesturesEnabled &&
                        drawerState.swipeableState.confirmValueChange(DrawerValue.Closed)
                    ) {
                        scope.launch { drawerState.close() }
                    }
                },
                fraction = {
                    calculateFraction(minValue, maxValue, drawerState.requireOffset())
                },
                color = scrimColor
            )
            val navigationMenu = NavigationMenu
            //FIX Left drawer (null as disabled)
            leftDrawerContent?.let{
                Surface(
                    modifier = with(LocalDensity.current) {
                        Modifier
                            .sizeIn(
                                minWidth = modalDrawerConstraints.minWidth.toDp(),
                                minHeight = modalDrawerConstraints.minHeight.toDp(),
                                maxWidth = modalDrawerConstraints.maxWidth.toDp(),
                                maxHeight = modalDrawerConstraints.maxHeight.toDp()
                            )
                    }
                        .offset {
                            IntOffset(
                                drawerState
                                    .requireOffset()
                                    .roundToInt()
                                        + minValue.toInt(), 0   //FIX Close状態をoffset = 0に調整
                            )
                        }
                        .padding(end = EndDrawerPadding)
                        .semantics {
                            paneTitle = navigationMenu
                            if (drawerState.isOpen) {
                                dismiss {
                                    if (
                                        drawerState.swipeableState
                                            .confirmValueChange(DrawerValue.Closed)
                                    ) {
                                        scope.launch { drawerState.close() }
                                    }; true
                                }
                            }
                        },
                    shape = drawerShape,
                    color = drawerBackgroundColor,
                    contentColor = drawerContentColor,
                    elevation = drawerElevation
                ) {
                    Column(Modifier.fillMaxSize(), content = leftDrawerContent)
                }
            }
            //ADD Right drawer
            rightDrawerContent?.let {
                Surface(
                    modifier = with(LocalDensity.current) {
                        Modifier
                            .sizeIn(
                                minWidth = modalDrawerConstraints.minWidth.toDp(),
                                minHeight = modalDrawerConstraints.minHeight.toDp(),
                                maxWidth = modalDrawerConstraints.maxWidth.toDp(),
                                maxHeight = modalDrawerConstraints.maxHeight.toDp()
                            )
                    }
                        .offset {
                            IntOffset(
                                drawerState
                                    .requireOffset()
                                    .roundToInt()
                                        - minValue.toInt(), 0   //FIX Close状態をoffset = 0に調整
                            )
                        }
                        .padding(start = EndDrawerPadding)  //FIX 余白の左右変更
                        .semantics {
                            paneTitle = navigationMenu
                            if (drawerState.isOpen) {
                                dismiss {
                                    if (
                                        drawerState.swipeableState
                                            .confirmValueChange(DrawerValue.Closed)
                                    ) {
                                        scope.launch { drawerState.close() }
                                    }; true
                                }
                            }
                        },
                    shape = drawerShape,
                    color = drawerBackgroundColor,
                    contentColor = drawerContentColor,
                    elevation = drawerElevation
                ) {
                    Column(Modifier.fillMaxSize(), content = rightDrawerContent)
                }
            }
        }
    }
}

/**
 * Object to hold default values for [DoubleModalDrawer] and [BottomDrawer]
 */
object DrawerDefaults {

    /**
     * Default Elevation for drawer sheet as specified in material specs
     */
    val Elevation = 16.dp

    val scrimColor: Color
        @Composable
        get() = MaterialTheme.colors.onSurface.copy(alpha = ScrimOpacity)

    /**
     * Default alpha for scrim color
     */
    const val ScrimOpacity = 0.32f
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    (abs(pos) / (b - a)).coerceIn(0f, 1f)   //FIX Close状態をoffset = 0に調整


@Composable
private fun Scrim(
    open: Boolean,
    onClose: () -> Unit,
    fraction: () -> Float,
    color: Color
) {
    val closeDrawer = CloseDrawer
    val dismissDrawer = if (open) {
        Modifier
            .pointerInput(onClose) { detectTapGestures { onClose() } }
            .semantics(mergeDescendants = true) {
                contentDescription = closeDrawer
                onClick { onClose(); true }
            }
    } else {
        Modifier
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .then(dismissDrawer)
    ) {
        drawRect(color, alpha = fraction())
    }
}

private val EndDrawerPadding = 56.dp
private val DrawerVelocityThreshold = 400.dp

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)

private const val BottomDrawerOpenFraction = 0.5f

private const val CloseDrawer = "Close Drawer"
private const val NavigationMenu = "Navigation Menu"