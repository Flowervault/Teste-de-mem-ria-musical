package com.teste.ouvido

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var box: LinearLayout
    private val grey = Color.parseColor("#9AA1B2")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = Ui.dp(this, 20)
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, Ui.dp(this@MainActivity, 48), p, p * 2) }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#0F1118")); addView(box) })
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() { super.onResume(); draw() }

    private fun draw() {
        box.removeAllViews()
        val rounds = Store.load(this); val n = rounds.size
        fun cnt(i: Int, v: Int) = rounds.count { it[i] == v }
        fun pct(h: Int, m: Int) = if (h + m == 0) null else Math.round(h * 100f / (h + m))
        val h = (0..2).sumOf { cnt(it, 1) }; val m = (0..2).sumOf { cnt(it, 0) }
        val o = pct(h, m)
        box.addView(Ui.tv(this, if (o == null) "—" else "$o%", 64f, Color.parseColor("#7B97FF")))
        box.addView(Ui.tv(this, "acerto geral · $n ${if (n == 1) "música" else "músicas"}", 14f, grey))
        box.addView(Ui.tv(this, "pontos por música: " + if (n == 0) "—" else String.format("%.1f de 3", h / n.toFloat()), 16f, Color.WHITE)
            .apply { setPadding(0, Ui.dp(this@MainActivity, 12), 0, Ui.dp(this@MainActivity, 6)) })
        Ui.ITEMS.forEachIndexed { i, name ->
            val p = pct(cnt(i, 1), cnt(i, 0))
            box.addView(Ui.tv(this, "$name: ${if (p == null) "—" else "$p%"}  (${cnt(i, 1)} acertos, ${cnt(i, 0)} erros)", 16f, Color.WHITE))
        }

        val on = OverlayService.running
        box.addView(Ui.btn(this, if (on) "Fechar pop-up" else "Abrir pop-up", Ui.ORANGE, Ui.DARKTXT) {
            if (on) { stopService(Intent(this, OverlayService::class.java)); OverlayService.running = false; draw() }
            else if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Ative \"Exibir sobre outros apps\" e volte aqui", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                startForegroundService(Intent(this, OverlayService::class.java)); OverlayService.running = true
                moveTaskToBack(true)
            }
        })

        box.addView(Ui.tv(this, "Histórico", 18f, Color.WHITE).apply { setPadding(0, Ui.dp(this@MainActivity, 24), 0, Ui.dp(this@MainActivity, 6)) })
        box.addView(Ui.tv(this, "Anuladas não entram na média.", 13f, grey))
        if (n == 0) box.addView(Ui.tv(this, "Nenhuma música salva ainda.", 15f, grey))
        for (j in rounds.indices.reversed()) {
            val r = rounds[j]
            val line = "${j + 1}   " + Ui.ITEMS.indices.joinToString("  ") { "${Ui.ITEMS[it].take(3)} ${Ui.mark(r[it])}" } + "   ${r.count { it == 1 }}/3"
            box.addView(LinearLayout(this).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                addView(Ui.tv(this@MainActivity, line, 15f, Color.WHITE).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                addView(Ui.tv(this@MainActivity, "×", 24f, grey).apply {
                    setPadding(Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 8))
                    setOnClickListener { rounds.removeAt(j); Store.save(this@MainActivity, rounds); draw() }
                })
            })
        }

        var armed = false
        box.addView(Ui.btn(this, "Zerar tudo", Color.parseColor("#2B3040"), grey) { t: TextView ->
            if (armed) { Store.save(this, emptyList()); draw() }
            else { armed = true; t.text = "Toque de novo para zerar"; t.postDelayed({ armed = false; t.text = "Zerar tudo" }, 3000) }
        })
    }
}
