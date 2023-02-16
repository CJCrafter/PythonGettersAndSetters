package me.cjcrafter.pygetset

import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyFunction
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel


class GenerateOptionsPanel(project: Project, clazz: PyClass) : JPanel() {

    private val project: Project
    private val clazz: PyClass

    private val getterFormat: EditorTextField
    private val setterFormat: EditorTextField
    private val deleterFormat: EditorTextField

    private val getterCheckbox : JCheckBox
    private val setterCheckbox : JCheckBox
    private val deleterCheckbox : JCheckBox

    init {
        this.project = project
        this.clazz = clazz

        var persistentData = GetterSetterService.getInstance().state
        if (persistentData == null) {
            println("Could not find persistent data for getters/setters/deleters... Did something go wrong?");
            persistentData = GetterSetterState()
        }

        layout = GridLayout(4, 2)
        getterCheckbox = JCheckBox("Generate Getters?", persistentData.isGetter)
        add(getterCheckbox)
        getterFormat = createField(clazz.language, project, persistentData.getter)

        setterCheckbox = JCheckBox("Generate Setters?", persistentData.isSetter)
        add(setterCheckbox)
        setterFormat = createField(clazz.language, project, persistentData.setter)

        deleterCheckbox = JCheckBox("Generate Deleters?", persistentData.isDeleter)
        add(deleterCheckbox)
        deleterFormat = createField(clazz.language, project, persistentData.deleter)

        val generateButton = JButton("Generate")
        generateButton.addActionListener { generate(persistentData) }
        add(generateButton)
    }

    private fun generate(persistentData: GetterSetterState) {
        // Setup variables for the loop
        val factory = PyElementGenerator.getInstance(project)
        val language = LanguageLevel.getLatest()

        // First we need to update the getter/setter format since
        // the user might have made some changes
        persistentData.getter = getterFormat.text
        persistentData.setter = setterFormat.text
        persistentData.deleter = deleterFormat.text
        persistentData.isGetter = getterCheckbox.isSelected
        persistentData.isSetter = setterCheckbox.isSelected
        persistentData.isDeleter = deleterCheckbox.isSelected

        val gettersAndSetters = ArrayList<PyFunction>()
        for (property in clazz.instanceAttributes) {
            var name = property.name ?: return
            name = if (name.startsWith("__")) name.substring(2) else name

            if (persistentData.isGetter) {
                val getter = factory.createFromText(language, PyFunction::class.java, persistentData.getter.replace("{name}", name).replace("{property}", property.name.orEmpty()))
                gettersAndSetters.add(getter)
            }
            if (persistentData.isSetter) {
                val setter = factory.createFromText(language, PyFunction::class.java, persistentData.setter.replace("{name}", name).replace("{property}", property.name.orEmpty()))
                gettersAndSetters.add(setter)
            }
            if (persistentData.isDeleter) {
                val deleter = factory.createFromText(language, PyFunction::class.java, persistentData.deleter.replace("{name}", name).replace("{property}", property.name.orEmpty()))
                gettersAndSetters.add(deleter)
            }
        }

        val initMethod = clazz.findMethodByName("__init__", false, null)
        if (initMethod == null) {
            Messages.showErrorDialog(project, "Could not find __init__ method in ${clazz.name}", "Error")
            return
        }

        // We need these reversed since we always add the method right below
        // the __init__ method (which pushes the other methods down)
        gettersAndSetters.reverse()

        // WriteCommandAction lets users undo the generation
        val classElement = initMethod.parent
        WriteCommandAction.runWriteCommandAction(project) {
            for (method in gettersAndSetters)
                classElement.addAfter(method, initMethod)
        }

        Messages.showInfoMessage(
            project, "Generated ${gettersAndSetters.size} methods\n\n" +
                    gettersAndSetters, "Generated Getters and Setters"
        )
    }

    private fun createField(language: Language, project: Project, code: String): EditorTextField {
        val field = CustomEditorField(language, project, code)
        field.setOneLineMode(false)
        field.isVisible = true
        field.isViewer = false
        field.isEnabled = true
        field.setCaretPosition(0)

        add(field)
        return field
    }
}


class CustomEditorField(language: Language, project: Project, s: String) : LanguageTextField(language, project, s) {

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setVerticalScrollbarVisible(true)
        editor.setHorizontalScrollbarVisible(true)

        val settings = editor.settings
        settings.isLineNumbersShown = true
        settings.isAutoCodeFoldingEnabled = true
        settings.isFoldingOutlineShown = true
        settings.isAllowSingleLogicalLineFolding = true
        settings.isRightMarginShown=true
        return editor
    }
}