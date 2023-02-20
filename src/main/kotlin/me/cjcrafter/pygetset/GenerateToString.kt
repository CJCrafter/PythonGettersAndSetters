package me.cjcrafter.pygetset

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.jetbrains.python.psi.*
import me.cjcrafter.pygetset.utils.MapWrapper

/**
 * This action creates a popup window. The window includes options that allows
 * people to generate getters, setters, and deleters for python. It uses the
 * "Live Templates" to control the format of the generated methods. Note that
 * in Python, getter/setters should be generally avoided. If you must use a
 * private variable, then this generator is useful for you.
 */
class GenerateToString : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return

        // Find the class that the caret is currently in
        val parent = PsiTreeUtil.getParentOfType(element, PyClass::class.java)
        if (parent == null) {
            Messages.showErrorDialog(project, "Could not find python class: $element", "Error")
            return
        }

        // This is the "preview" of the generated code. Start with an empty
        // code block. We will fill it later.
        val preview = CustomEditorField(parent)

        // Prompt the user with a radio button, so they may choose the template
        // for the generated method.
        val panel = panel { // line 47
            /*buttonsGroup(title = "Template:") {
                for (value in ToStringTemplate.values()) {
                    row {
                        radioButton(value.name, value)
                    }
                }
            }.bind({ preview.model::template }, {
                preview.model.template = it.get() // I think this is the error...template is an enum ToStringTemplate
            })*/

            row {
                checkBox("Use Single Quotes")
                    .bindSelected(preview.model::isSingleQuotes)
            }

            group("Properties:") {
                for (property in parent.instanceAttributes) {
                    row {
                        checkBox(property.text.substring("self.".length))
                            .bindSelected({ preview.model.properties[property.text] == true }, { preview.model.properties[property.text] = it })
                    }
                }
            }

            // Now we can update the editor with the code preview
            preview.updateCode()
            group("Preview:") {
                row {
                    cell(preview)
                }
            }
        }

        // Only generate the getters and setters if the user presses 'Generate'
        val popup = PanelDialog(panel, "Generate ToString")
        popup.setOkText("Generate")
        val isGenerate = popup.showAndGet()
        if (!isGenerate)
            return

        val factory = PyElementGenerator.getInstance(project)
        val language = LanguageLevel.getLatest()

        // WriteCommandAction lets users undo the generation
        WriteCommandAction.runWriteCommandAction(project) {
            val toString = factory.createFromText(language, PyFunction::class.java, preview.generateCode())
            parent.add(toString)
        }
    }
}

class CustomEditorField(private val clazz: PyClass) : LanguageTextField(clazz.language, clazz.project, "") {

    val model: Model = Model(this)

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setHorizontalScrollbarVisible(true)

        setPreferredWidth(380)
        font = font.deriveFont(font.size * 1.25f) // make font 25% bigger
        isViewer = true
        isOneLineMode = false
        isVisible = true
        setCaretPosition(0)

        return editor
    }

    fun updateCode() {
        text = generateCode()
    }

    fun generateCode(): String {
        println("Generated code")

        // Loop through the python class' instance variables to generate methods
        val selectedProperties = ArrayList<PyTargetExpression>()
        for (property in clazz.instanceAttributes) {

            // Check to see if the user skipped this property
            if (model.properties[property.text] == false)
                continue

            selectedProperties.add(property)
        }

        val quote = if (model.isSingleQuotes) '\'' else '\"'
        return model.template.generate(clazz, selectedProperties, quote)
    }
}

class Model(private val editor: CustomEditorField) {

    var template: ToStringTemplate = ToStringTemplate.TABULAR_TEMPLATE
        set(value) {
            editor.updateCode()
            field = value
        }
    var isSingleQuotes: Boolean = false
        set(value) {
            editor.updateCode()
            field = value
        }

    var properties: MapWrapper<String, Boolean> = MapWrapper(HashMap()) { editor.updateCode() }
}