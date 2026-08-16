package com.payanag2.pingtool

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var host: EditText
    private lateinit var output: TextView
    private lateinit var stats: TextView
    private lateinit var scroll: ScrollView
    private var job: Job? = null
    private var sent = 0
    private var received = 0
    private var min = Long.MAX_VALUE
    private var max = 0L
    private var total = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 8)
            setBackgroundColor(Color.BLACK)
        }
        host = EditText(this).apply {
            setText("8.8.8.8")
            hint = "IP or domain"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            isSingleLine = true
        }
        val row = LinearLayout(this)
        val start = Button(this).apply { text = "START" }
        val stop = Button(this).apply { text = "STOP" }
        row.addView(start, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(stop, LinearLayout.LayoutParams(0, -2, 1f))
        stats = TextView(this).apply {
            setTextColor(Color.rgb(0, 220, 90))
            typeface = android.graphics.Typeface.MONOSPACE
            text = "Packets: 0 | Loss: 0%"
        }
        output = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 14f
            setTextIsSelectable(true)
        }
        scroll = ScrollView(this).apply {
            addView(output)
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        root.addView(host)
        root.addView(row)
        root.addView(stats)
        root.addView(scroll)
        setContentView(root)
        start.setOnClickListener { startPing() }
        stop.setOnClickListener { stopPing() }
    }

    private fun smoothScrollToLatest() {
        scroll.post {
            val target = (output.height - scroll.height).coerceAtLeast(0)
            val startY = scroll.scrollY
            if (target <= startY) return@post
            ValueAnimator.ofInt(startY, target).apply {
                duration = 280L
                interpolator = DecelerateInterpolator()
                addUpdateListener { scroll.scrollTo(0, it.animatedValue as Int) }
                start()
            }
        }
    }

    private fun addLine(line: String) {
        output.append(line)
        output.post {
            output.alpha = 0.72f
            output.animate().alpha(1f).setDuration(140L).start()
            smoothScrollToLatest()
        }
    }

    private fun startPing() {
        stopPing()
        sent = 0
        received = 0
        min = Long.MAX_VALUE
        max = 0L
        total = 0L
        val target = host.text.toString().trim()
        if (target.isEmpty()) return
        output.text = "Pinging $target with 32 bytes of data\n\n"
        smoothScrollToLatest()
        job = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                sent++
                val t = System.nanoTime()
                val process = Runtime.getRuntime().exec(
                    arrayOf("/system/bin/ping", "-c", "1", "-W", "1", target)
                )
                val code = process.waitFor()
                val ms = (System.nanoTime() - t) / 1_000_000
                withContext(Dispatchers.Main) {
                    if (code == 0) {
                        received++
                        min = minOf(min, ms)
                        max = maxOf(max, ms)
                        total += ms
                        addLine("Reply from $target: time=${ms}ms\n")
                    } else {
                        addLine("Request timed out.\n")
                    }
                    val loss = (sent - received) * 100 / sent
                    val avg = if (received == 0) 0 else total / received
                    val minValue = if (min == Long.MAX_VALUE) 0 else min
                    stats.text = "Packets: $sent  Received: $received  Loss: $loss%  Min/Avg/Max: $minValue/$avg/$max ms"
                }
                delay(1000)
            }
        }
    }

    private fun stopPing() {
        job?.cancel()
        job = null
    }

    override fun onDestroy() {
        stopPing()
        super.onDestroy()
    }
}
