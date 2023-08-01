package me.cjcrafter.pygetset

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class PanelDialog(private val panel: JComponent, text: String) : DialogWrapper(true) {

    init {
        title = text
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel
    }

    fun setOkText(text: String) {
        setOKButtonText(text)
    }
}