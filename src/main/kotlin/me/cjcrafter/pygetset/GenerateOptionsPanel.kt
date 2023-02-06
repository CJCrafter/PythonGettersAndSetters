package me.cjcrafter.pygetset

import com.intellij.ui.components.JBList
import com.jetbrains.python.psi.PyClass
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListSelectionModel

class GenerateOptionsPanel(clazz: PyClass) : JPanel() {

    private var getterFormat: JTextField? = null
    private var setterFormat: JTextField? = null
    private var deleterFormat: JTextField? = null

    init {
        val propertiesList = JBList(clazz.instanceAttributes.stream().map { a -> a.name })
        propertiesList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        add(propertiesList)
        getterFormat = JTextField("Getter Format")
        setterFormat = JTextField("Setter Format")
        deleterFormat = JTextField("Deleter Format")
        add(getterFormat)
        add(setterFormat)
        add(deleterFormat)
    }
}