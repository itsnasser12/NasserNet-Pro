
package com.nassernet.pro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val backgroundColor = Color.rgb(12, 25, 43)
    private val cardColor = Color.rgb(22, 40, 62)
    private val accentColor = Color.rgb(66, 184, 255)
    private val textColor = Color.WHITE
    private val secondaryColor = Color.LTGRAY

    private lateinit var repository: InstalledAppsRepository
    private lateinit var appList: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var userAppsButton: Button
    private lateinit var systemAppsButton: Button
    private lateinit var searchInput: EditText

    private var showingSystemApps = false
    private val selectedGames = mutableSetOf<String>()

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
        }
    }

    private fun makeButton(value: String): Button {
        return Button(this).apply {
            text = value
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
    }

    private fun updateTabButtons() {
        userAppsButton.setBackgroundColor(
            if (!showingSystemApps) accentColor else cardColor
        )

        systemAppsButton.setBackgroundColor(
            if (showingSystemApps) accentColor else cardColor
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        repository = InstalledAppsRepository(this)
        selectedGames.addAll(AppPrefs.getSelectedGames(this))

        buildScreen()
        showApps()
    }

    private fun buildScreen() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(12))
            setBackgroundColor(backgroundColor)
        }

        val title = makeText(
            "NasserNet Pro",
            28f,
            textColor,
            true
        )

        val subtitle = makeText(
            "GAMING NETWORK FIREWALL",
            12f,
            accentColor,
            true
        )

        statusText = makeText(
            "Selected games: ${selectedGames.size}",
            15f,
            textColor,
            true
        )

        root.addView(title)
        root.addView(subtitle)

        val statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = dp(12).toFloat()
            }
        }

        statusContainer.addView(statusText)

        root.addView(
            statusContainer,
            LinearLayout.LayoutParams(-1, dp(58)).apply {
                topMargin = dp(20)
                bottomMargin = dp(18)
            }
        )

        root.addView(
            makeText("APPLICATIONS", 16f, textColor, true)
        )

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        userAppsButton = makeButton("User Apps & Games")
        systemAppsButton = makeButton("System Apps")

        tabs.addView(
            userAppsButton,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(6)
            }
        )

        tabs.addView(
            systemAppsButton,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(6)
            }
        )

        root.addView(
            tabs,
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                topMargin = dp(14)
                bottomMargin = dp(12)
            }
        )

        userAppsButton.setOnClickListener {
            showingSystemApps = false
            updateTabButtons()
            showApps()
        }

        systemAppsButton.setOnClickListener {
            showingSystemApps = true
            updateTabButtons()
            showApps()
        }

        searchInput = EditText(this).apply {
            hint = "Search apps..."
            setHintTextColor(secondaryColor)
            setTextColor(textColor)
            textSize = 15f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), accentColor)
            }
        }

        root.addView(
            searchInput,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                bottomMargin = dp(12)
            }
        )

        searchInput.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                showApps()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val scrollView = ScrollView(this)

        appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(appList)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        updateTabButtons()
        setContentView(root)
    }

    private fun showApps() {

        appList.removeAllViews()

        val allApps = if (showingSystemApps) {
            repository.getSystemApps()
        } else {
            repository.getUserApps()
        }

        val query = searchInput.text.toString().trim()

        val apps = allApps.filter { app ->
            app.appName.contains(query, ignoreCase = true) ||
            app.packageName.contains(query, ignoreCase = true)
        }

        if (apps.isEmpty()) {
            val emptyText = makeText(
                "No applications found",
                15f,
                secondaryColor
            )

            emptyText.gravity = Gravity.CENTER

            appList.addView(
                emptyText,
                LinearLayout.LayoutParams(-1, dp(100))
            )

            updateStatus()
            return
        }

        for (app in apps) {
            addAppRow(app)
        }

        updateStatus()
    }

    private fun addAppRow(app: AppInfo) {

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = dp(10).toFloat()
            }
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.appIcon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        row.addView(
            icon,
            LinearLayout.LayoutParams(dp(42), dp(42))
        )

        val checkBox = CheckBox(this).apply {
            text = app.appName
            textSize = 14f
            setTextColor(textColor)
            isChecked = selectedGames.contains(app.packageName)
            buttonTintList = ColorStateList.valueOf(accentColor)

            setOnCheckedChangeListener { _, isChecked ->

                if (isChecked) {
                    selectedGames.add(app.packageName)
                } else {
                    selectedGames.remove(app.packageName)
                }

                AppPrefs.saveSelectedGames(
                    this@MainActivity,
                    selectedGames
                )

                updateStatus()
            }
        }

        row.addView(
            checkBox,
            LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                marginStart = dp(10)
            }
        )

        appList.addView(
            row,
            LinearLayout.LayoutParams(-1, dp(66)).apply {
                bottomMargin = dp(7)
            }
        )
    }

    private fun updateStatus() {
        statusText.text = "Selected games: ${selectedGames.size}"
    }
}
