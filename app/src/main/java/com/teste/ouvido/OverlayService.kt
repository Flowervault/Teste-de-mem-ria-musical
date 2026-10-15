package com.teste.ouvido

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout

class OverlayService : Service() {
    companion object { @Volatile var running = false }

    private lateinit var wm: WindowManager
    private lateinit var card: LinearLayout
    private lateinit var lp: WindowManager.LayoutParams
    private val cur = IntArray(3) { Ui.UNSET }
    private var choosing = -1

    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_NOT_STICKY

    override fun onCreate() {
        super.onCreate()
        running = true
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ov", "Pop-up", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "ov").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Teste de ouvido").setContentText("Pop-up ativo. Toque no × dele para fechar.").build())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lp = WindowManager.LayoutParams(-2, -2, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = Ui.dp(this@OverlayService, 16); y = Ui.dp(this@OverlayService, 220) }
        card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; minimumWidth = Ui.dp(this@OverlayService, 220)
            setPadding(Ui.dp(this@OverlayService, 14), Ui.dp(this@OverlayService, 4), Ui.dp(this@OverlayService, 14), Ui.dp(this@OverlayService, 14))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = Ui.dp(this@OverlayService, 18).toFloat(); setStroke(Ui.dp(this@OverlayService, 2), Color.BLACK)
            }
        }
        draw()
        wm.addView(card, lp)
    }

    override fun onDestroy() {
        running = false
        try { wm.removeView(card) } catch (e: Exception) {}
        super.onDestroy()
    }

    private fun row(v: Int, label: String, onClick: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = Ui.dp(this@OverlayService, 54)
        addView(Ui.circle(this@OverlayService, v, 38)); addView(Ui.tv(this@OverlayService, label, 20f, Color.BLACK))
        setOnClickListener { onClick() }
    }

    private fun draw() {
        card.removeAllViews()
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = Ui.tv(this, if (choosing < 0) "Música ${Store.load(this).size + 1}" else Ui.ITEMS[choosing].uppercase(), 14f, Color.DKGRAY)
        title.gravity = Gravity.CENTER_VERTICAL
        title.layoutParams = LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f)
        var sx = 0f; var sy = 0f; var ix = 0; var iy = 0
        title.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ix = lp.x; iy = lp.y }
                MotionEvent.ACTION_MOVE -> { lp.x = ix + (e.rawX - sx).toInt(); lp.y = iy + (e.rawY - sy).toInt(); wm.updateViewLayout(card, lp) }
            }
            true
        }
        val close = Ui.tv(this, "×", 26f, Color.DKGRAY).apply {
            gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(Ui.dp(this@OverlayService, 44), Ui.dp(this@OverlayService, 44))
            setOnClickListener { stopSelf() }
        }
        head.addView(title); head.addView(close); card.addView(head)

        if (choosing < 0) {
            Ui.ITEMS.forEachIndexed { i, n -> card.addView(row(cur[i], n.uppercase()) { choosing = i; draw() }) }
            val ready = cur.none { it == Ui.UNSET }
            val next = Ui.btn(this, "PRÓXIMO  »", Ui.ORANGE, Ui.DARKTXT) {
                if (ready) { Store.add(this, cur.copyOf()); cur.fill(Ui.UNSET); draw() }
            }
            next.textSize = 19f; next.alpha = if (ready) 1f else 0.35f
            card.addView(next)
        } else {
            listOf(1 to "ACERTO", 0 to "ERRO", -1 to "ANULA").forEach { (v, l) ->
                card.addView(row(v, l) { cur[choosing] = v; choosing = -1; draw() })
            }
        }
    }
}
