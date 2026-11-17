
package com.nassernet.pro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(12, 25, 43)
        window.navigationBarColor = Color.rgb(12, 25, 43)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(32), dp(24), dp(32))
            setBackgroundColor(Color.rgb(12, 25, 43))
        }

        val title = makeText(
            "NasserNet Pro",
            30f,
            Color.WHITE,
            true
        )

        val subtitle = makeText(
            "GAMING NETWORK FIREWALL",
            12f,
            Color.rgb(66, 184, 255),
            true
        )

        val status = makeText(
            "FIREWALL NOT ACTIVE",
            16f,
            Color.rgb(255, 190, 60),
            true
        )

        val description = makeText(
            "Your gaming connection manager\n\n" +
            "Choose your games and protect your network.",
            15f,
            Color.LTGRAY
        )

        root.addView(title)
        root.addView(subtitle)
        root.addView(status, LinearLayout.LayoutParams(
            -1, dp(90)
        ))
        root.addView(description)

        setContentView(root)
    }
}
