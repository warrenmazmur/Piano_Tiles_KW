package com.example.piano_tiles_kw.view.engines.tilt

import android.content.Context
import android.graphics.Color
import android.hardware.SensorManager
import android.os.Looper
import android.text.TextPaint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.example.piano_tiles_kw.R
import com.example.piano_tiles_kw.model.audio.PianoPlayer
import com.example.piano_tiles_kw.model.audio.Song
import com.example.piano_tiles_kw.view.SensorData
import com.example.piano_tiles_kw.view.UIThreadWrapper
import com.example.piano_tiles_kw.view.engines.GameEngine
import com.example.piano_tiles_kw.view.engines.TileDrawer
import kotlin.collections.ArrayList

/**
 * Manages a piano tile game in the given ImageView
 * @param context The context of the running game
 * @param iv The ImageView where the game will be displayed and played
 */
class TiltGameEngine(
    private val context: Context,
    private val iv: ImageView,
    private val gameListener : GameListener,
    private val sensorData: SensorData,
    private var numberOfLanes: Int = 4
) : GameEngine(context, iv), View.OnTouchListener {
    val handler = UIThreadWrapper(this, Looper.getMainLooper())
    val laneWidth = (iv.width.toFloat()) / numberOfLanes
    val laneCenters = ArrayList<Float>()
    val scoreTextSize = 24
    val textPaint = TextPaint()

    val orchestrator = TIltTileOrchestrator(
        this,
        handler,
        laneCenters,
        laneWidth,
        1000,
        15f,
        iv.height.toFloat(),
        iv.width.toFloat(),
        Color.BLACK,
        pianoPlayer = PianoPlayer(context, Song.values().random()),
        sensorData
    )
    var isOver = false

    init {
        iv.setOnTouchListener(this)

        for (i in 1..numberOfLanes) {
            laneCenters.add((i - 0.5f) * laneWidth)
        }
        for (i in laneCenters) {
            println(i)
        }

        initiatePaints()
        clearCanvas()
    }

    override fun initiatePaints() {
        super.initiatePaints()
        textPaint.color = ResourcesCompat.getColor(context.resources, R.color.colorPrimary, null)
        textPaint.textSize = scoreTextSize * context.resources.displayMetrics.density
    }

    /**
     * Resets the canvas' background and redraw its lane separator
     */
    override fun clearCanvas() {
        mCanvas.drawColor(Color.WHITE)

        for (i in 1 until numberOfLanes) {
            mCanvas.drawLine(
                i * laneWidth.toFloat(),
                0f,
                i * laneWidth.toFloat(),
                iv.height.toFloat(),
                strokePaint
            )
        }
    }

    /**
     * Resets the canvas and then redraw all the elements (tiles)
     */
    override fun redraw(drawers: ArrayList<TileDrawer>) {
        clearCanvas()
        for (i in drawers) {
            i.drawTile(mCanvas)
        }

        gameListener.onScoreChanged(getScore())

        iv.invalidate()
    }

    override fun startGame() {
        println("game started")
        orchestrator.start()
    }

    override fun stopGame() {
        if (!isOver){
            isOver = true
            orchestrator.stop()
            gameListener.onEndGame()
        }
    }

    override fun pauseGame() {
        orchestrator.pause()
    }

    override fun resumeGame() {
        orchestrator.resume()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (!isOver) {
            val pointerIndex = event!!.actionIndex
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(
                        "touch_listener",
                        "down ${event.getX(pointerIndex)}, ${event.getY(pointerIndex)}"
                    )
                    orchestrator.handleTouch(event.getX(pointerIndex), event.getY(pointerIndex))
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    Log.d(
                        "touch_listener",
                        "pointer_down ${event.getX(pointerIndex)}, ${event.getY(pointerIndex)}"
                    )
                    orchestrator.handleTouch(event.getX(pointerIndex), event.getY(pointerIndex))
                }
            }
        }

        return true
    }

    override fun getScore(): Number = orchestrator.getScore()
}