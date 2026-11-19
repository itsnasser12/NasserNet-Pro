
package com.nassernet.pro

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val VPN_REQUEST_CODE = 501
        private const val NOTIFICATION_REQUEST_CODE = 502
    }

    // Premium dark theme
    private val backgroundColor = Color.rgb(9, 18, 32)
    private val cardColor = Color.rgb(20, 37, 57)
    private val cardLightColor = Color.rgb(27, 47, 70)
    private val accentColor = Color.rgb(62, 184, 255)
    private val accentDarkColor = Color.rgb(24, 125, 190)
    private val textColor = Color.WHITE
    private val secondaryColor = Color.rgb(151, 172, 193)
    private val greenColor = Color.rgb(36, 190, 126)
    private val redColor = Color.rgb(220, 75, 85)
    private val warningColor = Color.rgb(255, 190, 65)

    private lateinit var repository: InstalledAppsRepository

    private lateinit var appList: LinearLayout
    private lateinit var protectionStatusText: TextView
    private lateinit var protectionDescriptionText: TextView
    private lateinit var userAppsButton: Button
    private lateinit var systemAppsButton: Button
    private lateinit var searchInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var selectedCountText: TextView
    private lateinit var sectionCountText: TextView

    private var showingSystemApps = false
    private var protectionRequestPending = false
    private var receiverRegistered = false

    // Both sections start collapsed whenever the activity is created.
    private var selectedSectionExpanded = false
    private var otherSectionExpanded = false

    private var protectionState =
        GameFirewallVpnService.STATUS_STOPPED

    private val selectedGames = mutableSetOf<String>()

    private val vpnStatusReceiver = object : BroadcastReceiver() {

        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {

            if (
                intent?.action !=
                GameFirewallVpnService.ACTION_STATUS
            ) {
                return
            }

            val newStatus = intent.getStringExtra(
                GameFirewallVpnService.EXTRA_STATUS
            ) ?: return

            val message = intent.getStringExtra(
                GameFirewallVpnService.EXTRA_MESSAGE
            ) ?: ""

            protectionState = newStatus

            runOnUiThread {

                updateProtectionUi()

                if (
                    newStatus ==
                    GameFirewallVpnService.STATUS_FAILED
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // --------------------------------------------------
    // UI helpers
    // --------------------------------------------------

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
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

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun makeCard(
        color: Int = cardColor,
        radius: Int = 16
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun makeOutline(
        color: Int,
        strokeColor: Int,
        radius: Int = 14
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), strokeColor)
        }
    }

    private fun makeButton(
        value: String,
        backgroundColor: Int,
        textColor: Int = Color.WHITE
    ): Button {

        return Button(this).apply {
            text = value
            textSize = 13f
            isAllCaps = false
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER

            background = makeCard(
                backgroundColor,
                13
            )

            setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(4)
            )

            stateListAnimator = null
        }
    }

    private fun makeSectionTitle(
        value: String
    ): TextView {

        return makeText(
            value,
            16f,
            textColor,
            true
        )
    }

    private fun addVerticalSpace(
        parent: LinearLayout,
        height: Int
    ) {

        parent.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    // --------------------------------------------------
    // Activity lifecycle
    // --------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        repository = InstalledAppsRepository(this)

        selectedGames.addAll(
            AppPrefs.getSelectedGames(this)
        )

        buildScreen()
        showApps()
        updateProtectionUi()
    }

    override fun onStart() {
        super.onStart()

        registerVpnStatusReceiver()
        requestCurrentVpnStatus()
    }

    override fun onStop() {
        unregisterVpnStatusReceiver()
        super.onStop()
    }

    // --------------------------------------------------
    // VPN status receiver
    // --------------------------------------------------

    private fun registerVpnStatusReceiver() {

        if (receiverRegistered) {
            return
        }

        val filter = IntentFilter(
            GameFirewallVpnService.ACTION_STATUS
        )

        if (Build.VERSION.SDK_INT >= 33) {

            registerReceiver(
                vpnStatusReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )

        } else {

            @Suppress("DEPRECATION")
            registerReceiver(
                vpnStatusReceiver,
                filter
            )
        }

        receiverRegistered = true
    }

    private fun requestCurrentVpnStatus() {

        val intent = Intent(
            this,
            GameFirewallVpnService::class.java
        ).apply {
            action = GameFirewallVpnService.ACTION_STATUS
        }

        try {
            startService(intent)
        } catch (exception: Exception) {

            protectionState =
                GameFirewallVpnService.STATUS_FAILED

            updateProtectionUi()

            Toast.makeText(
                this,
                "Unable to retrieve VPN status: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun unregisterVpnStatusReceiver() {

        if (!receiverRegistered) {
            return
        }

        try {
            unregisterReceiver(vpnStatusReceiver)
        } catch (exception: Exception) {
            // Receiver may already be unregistered.
        }

        receiverRegistered = false
    }

    // --------------------------------------------------
    // Main screen
    // --------------------------------------------------

    private fun buildScreen() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(16),
                dp(18),
                dp(10)
            )

            setBackgroundColor(backgroundColor)
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(0, 0, 0, dp(8))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        buildHeader(content)
        buildProtectionCard(content)
        buildProtectionButtons(content)
        buildApplicationsSection(content)

        scrollView.addView(content)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)
    }

    // --------------------------------------------------
    // Header
    // --------------------------------------------------

    private fun buildHeader(
        parent: LinearLayout
    ) {

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = makeText(
            "NasserNet Pro",
            27f,
            textColor,
            true
        )

        val subtitle = makeText(
            "GAMING NETWORK FIREWALL",
            11f,
            accentColor,
            true
        )

        titleContainer.addView(title)

        titleContainer.addView(
            subtitle,
            LinearLayout.LayoutParams(
                -2,
                -2
            ).apply {
                topMargin = dp(3)
            }
        )

        header.addView(
            titleContainer,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        val shield = TextView(this).apply {
            text = "✦"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(accentColor)

            background = makeOutline(
                cardLightColor,
                accentDarkColor,
                15
            )
        }

        header.addView(
            shield,
            LinearLayout.LayoutParams(
                dp(50),
                dp(50)
            )
        )

        parent.addView(header)

        addVerticalSpace(parent, 22)
    }

    // --------------------------------------------------
    // Protection card
    // --------------------------------------------------

    private fun buildProtectionCard(
        parent: LinearLayout
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(15),
                dp(16),
                dp(15)
            )

            background = makeCard(cardColor, 17)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = makeText(
            "PROTECTION STATUS",
            12f,
            secondaryColor,
            true
        )

        topRow.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        protectionStatusText = makeText(
            "OFF",
            12f,
            warningColor,
            true
        ).apply {
            gravity = Gravity.CENTER

            setPadding(
                dp(12),
                dp(5),
                dp(12),
                dp(5)
            )

            background = makeCard(
                Color.rgb(65, 52, 30),
                20
            )
        }

        topRow.addView(protectionStatusText)
        card.addView(topRow)

        val countRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        selectedCountText = makeText(
            "${selectedGames.size}",
            30f,
            textColor,
            true
        )

        val countLabel = makeText(
            "Apps Selected",
            14f,
            secondaryColor,
            true
        )

        countRow.addView(selectedCountText)

        countRow.addView(
            countLabel,
            LinearLayout.LayoutParams(
                -2,
                -2
            ).apply {
                marginStart = dp(9)
            }
        )

        card.addView(
            countRow,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                topMargin = dp(10)
            }
        )

        protectionDescriptionText = makeText(
            "Your gaming apps are ready for protection",
            11f,
            secondaryColor
        )

        card.addView(
            protectionDescriptionText,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                topMargin = dp(3)
            }
        )

        val progressBackground = View(this).apply {
            background = makeCard(
                Color.rgb(39, 57, 76),
                10
            )
        }

        card.addView(
            progressBackground,
            LinearLayout.LayoutParams(
                -1,
                dp(5)
            ).apply {
                topMargin = dp(14)
            }
        )

        parent.addView(card)

        addVerticalSpace(parent, 13)
    }

    // --------------------------------------------------
    // Protection buttons
    // --------------------------------------------------

    private fun buildProtectionButtons(
        parent: LinearLayout
    ) {

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        startButton = makeButton(
            "▶   START PROTECTION",
            greenColor
        ).apply {
            setOnClickListener {
                requestVpnPermission()
            }
        }

        stopButton = makeButton(
            "■   STOP PROTECTION",
            Color.rgb(48, 62, 80),
            secondaryColor
        ).apply {
            setOnClickListener {
                stopProtection()
            }
        }

        buttons.addView(
            startButton,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            ).apply {
                marginEnd = dp(5)
            }
        )

        buttons.addView(
            stopButton,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            ).apply {
                marginStart = dp(5)
            }
        )

        parent.addView(buttons)

        addVerticalSpace(parent, 24)
    }

    // --------------------------------------------------
    // Applications section
    // --------------------------------------------------

    private fun buildApplicationsSection(
        parent: LinearLayout
    ) {

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        heading.addView(
            makeSectionTitle("Your Applications"),
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        sectionCountText = makeText(
            "${selectedGames.size} SELECTED",
            10f,
            accentColor,
            true
        ).apply {
            gravity = Gravity.CENTER

            setPadding(
                dp(10),
                dp(6),
                dp(10),
                dp(6)
            )

            background = makeCard(
                Color.rgb(20, 52, 77),
                20
            )
        }

        heading.addView(sectionCountText)
        parent.addView(heading)

        addVerticalSpace(parent, 13)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        userAppsButton = makeButton(
            "User Apps & Games",
            accentDarkColor
        )

        systemAppsButton = makeButton(
            "System Apps",
            cardColor,
            secondaryColor
        )

        tabs.addView(
            userAppsButton,
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            ).apply {
                marginEnd = dp(5)
            }
        )

        tabs.addView(
            systemAppsButton,
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            ).apply {
                marginStart = dp(5)
            }
        )

        parent.addView(tabs)

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

        addVerticalSpace(parent, 12)

        searchInput = EditText(this).apply {
            hint = "⌕   Search applications..."
            setHintTextColor(secondaryColor)
            setTextColor(textColor)
            textSize = 14f
            setSingleLine(true)

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT

            imeOptions = EditorInfo.IME_ACTION_SEARCH

            setPadding(
                dp(15),
                dp(8),
                dp(15),
                dp(8)
            )

            background = makeOutline(
                cardColor,
                Color.rgb(49, 105, 145),
                13
            )
        }

        parent.addView(
            searchInput,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        searchInput.addTextChangedListener(
            object : TextWatcher {

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

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )

        addVerticalSpace(parent, 13)

        appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        parent.addView(appList)

        updateTabButtons()
    }

    // --------------------------------------------------
    // Collapsible application group header
    // --------------------------------------------------

    private fun createGroupHeader(
        title: String,
        count: Int,
        expanded: Boolean,
        selectedGroup: Boolean,
        onClick: () -> Unit
    ): LinearLayout {

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                dp(13),
                dp(12),
                dp(13),
                dp(12)
            )

            background = makeCard(
                if (selectedGroup) {
                    Color.rgb(19, 48, 70)
                } else {
                    cardLightColor
                },
                12
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                onClick()
            }
        }

        val indicator = View(this).apply {
            background = makeCard(
                if (selectedGroup) {
                    accentColor
                } else {
                    secondaryColor
                },
                4
            )
        }

        header.addView(
            indicator,
            LinearLayout.LayoutParams(
                dp(4),
                dp(23)
            )
        )

        val titleText = makeText(
            title,
            12f,
            if (selectedGroup) accentColor else textColor,
            true
        )

        header.addView(
            titleText,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            ).apply {
                marginStart = dp(10)
            }
        )

        val countText = makeText(
            "$count",
            11f,
            secondaryColor,
            true
        ).apply {
            gravity = Gravity.CENTER

            setPadding(
                dp(9),
                dp(4),
                dp(9),
                dp(4)
            )

            background = makeCard(
                Color.rgb(39, 57, 76),
                20
            )
        }

        header.addView(countText)

        val arrow = makeText(
            if (expanded) "⌃" else "⌄",
            20f,
            secondaryColor,
            true
        ).apply {
            gravity = Gravity.CENTER
        }

        header.addView(
            arrow,
            LinearLayout.LayoutParams(
                dp(30),
                dp(30)
            ).apply {
                marginStart = dp(5)
            }
        )

        return header
    }

    // --------------------------------------------------
    // VPN permission flow
    // --------------------------------------------------

    private fun requestVpnPermission() {

        if (
            protectionState ==
                GameFirewallVpnService.STATUS_ACTIVE ||
            protectionState ==
                GameFirewallVpnService.STATUS_STARTING ||
            protectionRequestPending
        ) {
            return
        }

        if (selectedGames.isEmpty()) {

            Toast.makeText(
                this,
                "Select at least one game first",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            protectionRequestPending = true
            updateProtectionUi()

            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_REQUEST_CODE
            )

            return
        }

        continueVpnPermissionFlow()
    }

    private fun continueVpnPermissionFlow() {

        val permissionIntent = VpnService.prepare(this)

        if (permissionIntent != null) {

            protectionRequestPending = true
            updateProtectionUi()

            startActivityForResult(
                permissionIntent,
                VPN_REQUEST_CODE
            )

        } else {

            startVpnService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == NOTIFICATION_REQUEST_CODE) {

            protectionRequestPending = false

            val granted =
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (granted) {

                updateProtectionUi()
                continueVpnPermissionFlow()

            } else {

                updateProtectionUi()

                Toast.makeText(
                    this,
                    "Notification permission is required for visible VPN status",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @Deprecated("Using Activity result API for VPN consent")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == VPN_REQUEST_CODE) {

            protectionRequestPending = false

            if (resultCode == RESULT_OK) {

                startVpnService()

            } else {

                protectionState =
                    GameFirewallVpnService.STATUS_STOPPED

                updateProtectionUi()

                Toast.makeText(
                    this,
                    "VPN permission was not granted",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // --------------------------------------------------
    // Start VPN
    // --------------------------------------------------

    private fun startVpnService() {

        val intent = Intent(
            this,
            GameFirewallVpnService::class.java
        ).apply {
            action = GameFirewallVpnService.ACTION_START
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            protectionState =
                GameFirewallVpnService.STATUS_STARTING

            updateProtectionUi()

        } catch (exception: Exception) {

            protectionState =
                GameFirewallVpnService.STATUS_FAILED

            updateProtectionUi()

            Toast.makeText(
                this,
                "Unable to start VPN service: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --------------------------------------------------
    // Stop VPN
    // --------------------------------------------------

    private fun stopProtection() {

        if (
            protectionState ==
                GameFirewallVpnService.STATUS_STOPPED ||
            protectionState ==
                GameFirewallVpnService.STATUS_FAILED
        ) {
            return
        }

        val intent = Intent(
            this,
            GameFirewallVpnService::class.java
        ).apply {
            action = GameFirewallVpnService.ACTION_STOP
        }

        try {
            startService(intent)
        } catch (exception: Exception) {

            Toast.makeText(
                this,
                "Unable to stop VPN service: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --------------------------------------------------
    // Update protection card and controls
    // --------------------------------------------------

    private fun updateProtectionUi() {

        val isActive =
            protectionState ==
                GameFirewallVpnService.STATUS_ACTIVE

        val isStarting =
            protectionState ==
                GameFirewallVpnService.STATUS_STARTING

        val isFailed =
            protectionState ==
                GameFirewallVpnService.STATUS_FAILED

        when {

            isStarting -> {

                protectionStatusText.text = "STARTING"
                protectionStatusText.setTextColor(accentColor)

                protectionDescriptionText.text =
                    "Establishing secure local VPN tunnel"

                protectionStatusText.background = makeCard(
                    Color.rgb(20, 52, 77),
                    20
                )
            }

            isActive -> {

                protectionStatusText.text = "ACTIVE"
                protectionStatusText.setTextColor(greenColor)

                protectionDescriptionText.text =
                    "Gaming network protection is running"

                protectionStatusText.background = makeCard(
                    Color.rgb(20, 66, 52),
                    20
                )
            }

            isFailed -> {

                protectionStatusText.text = "FAILED"
                protectionStatusText.setTextColor(redColor)

                protectionDescriptionText.text =
                    "Unable to start protection"

                protectionStatusText.background = makeCard(
                    Color.rgb(75, 34, 43),
                    20
                )
            }

            else -> {

                protectionStatusText.text = "OFF"
                protectionStatusText.setTextColor(warningColor)

                protectionDescriptionText.text =
                    "Your gaming apps are ready for protection"

                protectionStatusText.background = makeCard(
                    Color.rgb(65, 52, 30),
                    20
                )
            }
        }

        val isLocked =
            isStarting ||
            isActive ||
            protectionRequestPending

        startButton.isEnabled = !isLocked
        stopButton.isEnabled = isLocked

        startButton.alpha =
            if (isLocked) 0.45f else 1f

        stopButton.alpha =
            if (isLocked) 1f else 0.55f

        startButton.background = makeCard(
            if (isLocked) {
                Color.rgb(48, 76, 75)
            } else {
                greenColor
            },
            13
        )

        stopButton.background = makeCard(
            if (isLocked) {
                redColor
            } else {
                Color.rgb(48, 62, 80)
            },
            13
        )

        showApps()
        updateSelectedCounters()
    }

    // --------------------------------------------------
    // Tabs
    // --------------------------------------------------

    private fun updateTabButtons() {

        userAppsButton.background = makeCard(
            if (!showingSystemApps) {
                accentDarkColor
            } else {
                cardColor
            },
            13
        )

        systemAppsButton.background = makeCard(
            if (showingSystemApps) {
                accentDarkColor
            } else {
                cardColor
            },
            13
        )

        userAppsButton.setTextColor(
            if (!showingSystemApps) {
                Color.WHITE
            } else {
                secondaryColor
            }
        )

        systemAppsButton.setTextColor(
            if (showingSystemApps) {
                Color.WHITE
            } else {
                secondaryColor
            }
        )
    }

    // --------------------------------------------------
    // Application list with two collapsible sections
    // --------------------------------------------------

    private fun showApps() {

        appList.removeAllViews()

        val allApps = if (showingSystemApps) {
            repository.getSystemApps()
        } else {
            repository.getUserApps()
        }

        val query = searchInput.text.toString().trim()

        val filteredApps = allApps.filter { app ->

            app.appName.contains(
                query,
                ignoreCase = true
            ) || app.packageName.contains(
                query,
                ignoreCase = true
            )
        }

        val selectedApps = filteredApps.filter {
            selectedGames.contains(it.packageName)
        }

        val otherApps = filteredApps.filter {
            !selectedGames.contains(it.packageName)
        }

        if (filteredApps.isEmpty()) {

            val emptyContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                setPadding(
                    dp(15),
                    dp(30),
                    dp(15),
                    dp(30)
                )

                background = makeCard(cardColor, 15)
            }

            emptyContainer.addView(
                makeText(
                    "No applications found",
                    15f,
                    textColor,
                    true
                )
            )

            emptyContainer.addView(
                makeText(
                    "Try another search term",
                    12f,
                    secondaryColor
                ),
                LinearLayout.LayoutParams(
                    -2,
                    -2
                ).apply {
                    topMargin = dp(5)
                }
            )

            appList.addView(
                emptyContainer,
                LinearLayout.LayoutParams(
                    -1,
                    -2
                )
            )

            updateSelectedCounters()
            return
        }

        // Selected applications section
        if (selectedApps.isNotEmpty()) {

            val selectedHeader = createGroupHeader(
                title = "SELECTED APPS",
                count = selectedApps.size,
                expanded = selectedSectionExpanded,
                selectedGroup = true
            ) {
                selectedSectionExpanded =
                    !selectedSectionExpanded

                showApps()
            }

            appList.addView(
                selectedHeader,
                LinearLayout.LayoutParams(
                    -1,
                    -2
                ).apply {
                    bottomMargin = dp(8)
                }
            )

            if (selectedSectionExpanded) {

                for (app in selectedApps) {
                    addAppRow(app)
                }

                addVerticalSpace(appList, 12)
            }
        }

        // Other applications section
        if (otherApps.isNotEmpty()) {

            val otherHeader = createGroupHeader(
                title = "OTHER APPLICATIONS",
                count = otherApps.size,
                expanded = otherSectionExpanded,
                selectedGroup = false
            ) {
                otherSectionExpanded =
                    !otherSectionExpanded

                showApps()
            }

            appList.addView(
                otherHeader,
                LinearLayout.LayoutParams(
                    -1,
                    -2
                ).apply {
                    bottomMargin = dp(8)
                }
            )

            if (otherSectionExpanded) {

                for (app in otherApps) {
                    addAppRow(app)
                }
            }
        }

        updateSelectedCounters()
    }

    // --------------------------------------------------
    // Application row
    // --------------------------------------------------

    private fun addAppRow(app: AppInfo) {

        val isSelected =
            selectedGames.contains(app.packageName)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )

            background = makeCard(
                if (isSelected) {
                    Color.rgb(20, 43, 64)
                } else {
                    cardColor
                },
                14
            )
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.appIcon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        row.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(43),
                dp(43)
            )
        )

        val checkBox = CheckBox(this).apply {

            text = app.appName
            textSize = 13.5f

            setTextColor(
                if (isSelected) {
                    Color.WHITE
                } else {
                    Color.rgb(222, 232, 242)
                }
            )

            typeface = if (isSelected) {
                Typeface.DEFAULT_BOLD
            } else {
                Typeface.DEFAULT
            }

            isChecked = isSelected

            buttonTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(
                    accentColor,
                    Color.rgb(105, 133, 158)
                )
            )

            isEnabled =
                protectionState !=
                    GameFirewallVpnService.STATUS_ACTIVE &&
                protectionState !=
                    GameFirewallVpnService.STATUS_STARTING &&
                !protectionRequestPending

            setPadding(
                dp(5),
                0,
                0,
                0
            )

            setOnCheckedChangeListener { _, checked ->

                if (
                    protectionState ==
                        GameFirewallVpnService.STATUS_ACTIVE ||
                    protectionState ==
                        GameFirewallVpnService.STATUS_STARTING ||
                    protectionRequestPending
                ) {
                    return@setOnCheckedChangeListener
                }

                if (checked) {
                    selectedGames.add(app.packageName)
                } else {
                    selectedGames.remove(app.packageName)
                }

                AppPrefs.saveSelectedGames(
                    this@MainActivity,
                    selectedGames
                )

                // Move the app to its new section.
                showApps()
            }
        }

        row.addView(
            checkBox,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            ).apply {
                marginStart = dp(7)
            }
        )

        if (isSelected) {

            val selectedIndicator = TextView(this).apply {
                text = "✓"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentColor)
                gravity = Gravity.CENTER
            }

            row.addView(
                selectedIndicator,
                LinearLayout.LayoutParams(
                    dp(24),
                    dp(24)
                )
            )
        }

        appList.addView(
            row,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                bottomMargin = dp(7)
            }
        )
    }

    // --------------------------------------------------
    // Selected applications counters
    // --------------------------------------------------

    private fun updateSelectedCounters() {

        val count = selectedGames.size

        if (::selectedCountText.isInitialized) {
            selectedCountText.text = count.toString()
        }

        if (::sectionCountText.isInitialized) {
            sectionCountText.text = "$count SELECTED"
        }
    }
}
