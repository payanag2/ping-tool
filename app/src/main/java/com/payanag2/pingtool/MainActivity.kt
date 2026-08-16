package com.payanag2.pingtool

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var host: EditText
    private lateinit var output: TextView
    private lateinit var stats: TextView
    private var job: Job? = null
    private var sent = 0; private var received = 0
    private var min = Long.MAX_VALUE; private var max = 0L; private var total = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,16,16,8); setBackgroundColor(Color.BLACK) }
        host = EditText(this).apply { setText("8.8.8.8"); hint = "IP or domain"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); singleLine = true }
        val row = LinearLayout(this)
        val start = Button(this).apply { text = "START" }
        val stop = Button(this).apply { text = "STOP" }
        row.addView(start, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(stop, LinearLayout.LayoutParams(0, -2, 1f))
        stats = TextView(this).apply { setTextColor(Color.rgb(0,220,90)); typeface = android.graphics.Typeface.MONOSPACE; text = "Packets: 0 | Loss: 0%" }
        output = TextView(this).apply { setTextColor(Color.LTGRAY); typeface = android.graphics.Typeface.MONOSPACE; textSize = 14f; setTextIsSelectable(true) }
        val scroll = ScrollView(this).apply { addView(output); layoutParams = LinearLayout.LayoutParams(-1,0,1f) }
        root.addView(host); root.addView(row); root.addView(stats); root.addView(scroll); setContentView(root)
        start.setOnClickListener { startPing() }; stop.setOnClickListener { stopPing() }
    }

    private fun startPing() {
        stopPing(); sent = 0; received = 0; min = Long.MAX_VALUE; max = 0; total = 0
        output.text = "Pinging ${host.text} with 32 bytes of data\n\n"
        val target = host.text.toString().trim(); if (target.isEmpty()) return
        job = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                val n = ++sent
                val t = System.nanoTime()
                val p = Runtime.getRuntime().exec(arrayOf("/system/bin/ping", "-c", "1", "-W", "1", target))
                val code = p.waitFor(); val ms = (System.nanoTime() - t) / 1_000_000
                withContext(Dispatchers.Main) {
                    if (code == 0) { received++; min=minOf(min,ms); max=maxOf(max,ms); total+=ms; output.append("Reply from $target: time=${ms}ms\n") }
                    else output.append("Request timed out.\n")
                    val loss = if (sent == 0) 0 else (sent-received)*100/sent
                    val avg = if (received==0) 0 else total/received
                    stats.text = "Packets: $sent  Received: $received  Loss: $loss%  Min/Avg/Max: ${if(min==Long.MAX_VALUE)0 else min}/${avg}/${max} ms"
                }
                delay(1000)
            }
        }
    }

    private fun stopPing() { job?.cancel(); job = null }
    override fun onDestroy() { stopPing(); super.onDestroy() }
}
