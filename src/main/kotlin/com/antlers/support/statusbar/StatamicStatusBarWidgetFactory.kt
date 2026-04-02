package com.antlers.support.statusbar

import com.antlers.support.AntlersIcons
import com.antlers.support.settings.AntlersSettings
import com.antlers.support.statamic.IndexingStatus
import com.antlers.support.statamic.StatamicDriver
import com.antlers.support.statamic.StatamicProjectCollections
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.Consumer
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*

class StatamicStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "StatamicIndexingStatus"
    override fun getDisplayName(): String = "Statamic Indexing Status"
    override fun isAvailable(project: Project): Boolean = true
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = StatamicStatusBarWidget(project)
}

private class StatamicStatusBarWidget(private val project: Project) :
    StatusBarWidget, StatusBarWidget.IconPresentation {

    private var statusBar: StatusBar? = null
    private var timer: javax.swing.Timer? = null

    override fun ID(): String = "StatamicIndexingStatus"

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        StatamicProjectCollections.getInstance(project).ensureLoaded()
        timer = javax.swing.Timer(500) {
            statusBar.updateWidget(ID())
            val svc = StatamicProjectCollections.getInstance(project)
            if (svc.status == IndexingStatus.READY || svc.status == IndexingStatus.ERROR) timer?.delay = 5000
        }.apply { start() }
    }

    override fun dispose() { timer?.stop(); timer = null; statusBar = null }
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
    override fun getIcon(): Icon = AntlersIcons.FILE

    override fun getTooltipText(): String {
        val svc = StatamicProjectCollections.getInstance(project)
        return when (svc.status) {
            IndexingStatus.NOT_STARTED -> "Statamic: not indexed"
            IndexingStatus.INDEXING -> "Statamic: ${svc.currentStep}"
            IndexingStatus.READY -> "Statamic: ${svc.statusMessage}"
            IndexingStatus.ERROR -> "Statamic: ${svc.statusMessage}"
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer { event ->
        val panel = buildPopupPanel()
        val popup: JBPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, null)
            .setRequestFocus(true)
            .createPopup()

        val component = event.component
        panel.doLayout()
        val h = panel.preferredSize.height + 8
        popup.show(RelativePoint(component, Point(0, -h)))
    }

    private fun buildPopupPanel(): JPanel {
        val svc = StatamicProjectCollections.getInstance(project)
        val idx = svc.index
        val dim = JBUI.CurrentTheme.Label.disabledForeground()
        val midDim = Color(
            (dim.red + JBUI.CurrentTheme.Label.foreground().red) / 2,
            (dim.green + JBUI.CurrentTheme.Label.foreground().green) / 2,
            (dim.blue + JBUI.CurrentTheme.Label.foreground().blue) / 2
        )
        val bright = JBUI.CurrentTheme.Label.foreground()
        val baseFont = UIManager.getFont("Label.font")
        val tinyFont = baseFont.deriveFont(baseFont.size2D - 2.5f)
        val boldFont = baseFont.deriveFont(Font.BOLD, baseFont.size2D - 1f)

        val grid = JPanel(GridBagLayout())
        grid.border = JBUI.Borders.empty(6, 10, 6, 10)
        var row = 0

        fun gbc(x: Int, y: Int, anchor: Int = GridBagConstraints.WEST, weightx: Double = 0.0) =
            GridBagConstraints().apply {
                gridx = x; gridy = y
                this.anchor = anchor; this.weightx = weightx
                insets = Insets(2, if (x == 0) 0 else 12, 2, 0)
                fill = GridBagConstraints.NONE
            }

        fun addHeader(text: String) {
            grid.add(JBLabel(text).apply { font = boldFont; foreground = bright },
                gbc(0, row).apply { gridwidth = 2; insets = Insets(4, 0, 2, 0) })
            row++
        }

        fun addRow(label: String, value: String, active: Boolean = true) {
            grid.add(JBLabel(label).apply { font = tinyFont; foreground = dim },
                gbc(0, row))
            grid.add(JBLabel(value).apply {
                font = tinyFont
                foreground = if (active) midDim else dim
                horizontalAlignment = SwingConstants.RIGHT
            }, gbc(1, row, GridBagConstraints.EAST, 1.0))
            row++
        }

        // Connection section
        addHeader("Connection")
        addRow("Driver", when (svc.driver) {
            StatamicDriver.ELOQUENT -> "Eloquent (database)"
            StatamicDriver.FLAT_FILE -> "Flat file"
            StatamicDriver.UNKNOWN -> "—"
        })
        addRow("Status", when (svc.status) {
            IndexingStatus.READY -> "✓  Indexed"
            IndexingStatus.INDEXING -> "⟳  " + svc.currentStep.ifEmpty { "indexing…" }
            IndexingStatus.ERROR -> "✗  Error"
            IndexingStatus.NOT_STARTED -> "Not indexed"
        })

        // Spacer
        grid.add(Box.createVerticalStrut(4), gbc(0, row).apply { gridwidth = 2 })
        row++

        // Resources section
        addHeader("Resources")
        fun resourceRow(label: String, items: List<String>) {
            val count = items.size
            val active = items.isNotEmpty()

            // Col 0: label with count
            grid.add(JBLabel("$label  ($count)").apply {
                font = tinyFont
                foreground = if (active) midDim else dim
            }, gbc(0, row))

            // Col 1: handles
            val handlesText = if (active) {
                val joined = items.joinToString(", ")
                if (joined.length > 28) joined.take(26) + "…" else joined
            } else ""
            if (handlesText.isNotEmpty()) {
                grid.add(JBLabel(handlesText).apply {
                    font = tinyFont
                    foreground = dim
                    horizontalAlignment = SwingConstants.RIGHT
                    toolTipText = items.joinToString(", ")
                }, gbc(1, row, GridBagConstraints.EAST, 1.0))
            }
            row++
        }
        resourceRow("Collections", idx.collections)
        resourceRow("Navigations", idx.navigations)
        resourceRow("Taxonomies", idx.taxonomies)
        resourceRow("Global Sets", idx.globalSets)
        resourceRow("Forms", idx.forms)
        resourceRow("Assets", idx.assetContainers)

        // Separator + controls
        grid.add(Box.createVerticalStrut(4), gbc(0, row).apply { gridwidth = 2 })
        row++
        grid.add(JSeparator().apply { preferredSize = Dimension(0, 1) },
            gbc(0, row).apply { gridwidth = 2; fill = GridBagConstraints.HORIZONTAL; weightx = 1.0 })
        row++
        grid.add(Box.createVerticalStrut(2), gbc(0, row).apply { gridwidth = 2 })
        row++

        // Auto-index checkbox
        val checkbox = JBCheckBox("Auto-index").apply {
            font = tinyFont
            isSelected = AntlersSettings.getInstance().state.enableAutoIndex
            addActionListener {
                AntlersSettings.getInstance().state.enableAutoIndex = isSelected
                val s = StatamicProjectCollections.getInstance(project)
                if (isSelected) s.startFileWatcher() else s.stopFileWatcher()
            }
        }
        grid.add(checkbox, gbc(0, row))

        val refreshBtn = JButton("Refresh").apply {
            font = tinyFont
            margin = Insets(1, 6, 1, 6)
            isEnabled = svc.status != IndexingStatus.INDEXING
            addActionListener {
                StatamicProjectCollections.getInstance(project).refresh()
                timer?.delay = 500; statusBar?.updateWidget(ID())
                SwingUtilities.getWindowAncestor(this)?.dispose()
            }
        }
        grid.add(refreshBtn, gbc(1, row, GridBagConstraints.EAST, 1.0))

        return grid
    }
}
