package com.teste.ouvido

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/** Guarda só os resultados: 1 = acerto, 0 = erro, -1 = anulado. Nada de nomes de música. */
object Store {
    private fun sp(c: Context) = c.getSharedPreferences("teste", Context.MODE_PRIVATE)
    fun load(c: Context): MutableList<IntArray> =
        (sp(c).getString("rounds", "") ?: "").split(";").filter { it.isNotBlank() }
            .map { r -> r.split(",").map { it.toInt() }.toIntArray() }.toMutableList()
    fun save(c: Context, l: List<IntArray>) {
        sp(c).edit().putString("rounds", l.joinToString(";") { it.joinToString(",") }).apply()
    }
    fun add(c: Context, r: IntArray) { val l = load(c); l.add(r); save(c, l) }
}

object Ui {
    val ITEMS = listOf("Nome", "Álbum", "Artista")
    const val UNSET = 9
    val OK = Color.parseColor("#1FB85A")
    val NO = Color.parseColor("#E03A3A")
    val VOID = Color.parseColor("#8A8F9C")
    val ORANGE = Color.parseColor("#FF9A1F")
    val DARKTXT = Color.parseColor("#1A1203")

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()
    fun mark(v: Int) = when (v) { 1 -> "✓"; 0 -> "✕"; else -> "?" }

    fun tv(c: Context, t: String, size: Float, color: Int) = TextView(c).apply {
        text = t; textSize = size; setTextColor(color); typeface = Typeface.DEFAULT_BOLD
    }
    fun pill(c: Context, color: Int, r: Int = 14) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(c, r).toFloat()
    }
    fun btn(c: Context, t: String, bg: Int, fg: Int, onClick: (TextView) -> Unit) = tv(c, t, 17f, fg).apply {
        background = pill(c, bg); gravity = Gravity.CENTER; minHeight = dp(c, 52)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(c, 14) }
        setOnClickListener { onClick(this) }
    }
    fun circle(c: Context, v: Int, size: Int) = TextView(c).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (v == UNSET) { setColor(Color.WHITE); setStroke(dp(c, 3), Color.BLACK) }
            else setColor(when (v) { 1 -> OK; 0 -> NO; else -> VOID })
        }
        text = if (v == UNSET) "" else mark(v)
        setTextColor(Color.WHITE); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(dp(c, size), dp(c, size)).apply { marginEnd = dp(c, 12) }
    }
}
