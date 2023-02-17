package me.cjcrafter.pygetset

import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.Messages
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.python.psi.*

// Creates a popup window that lists the current python class
// properties so the user can generate getters and setters
// for their python variables
class GenerateGettersSetters : AnAction() {

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

        // Access the current live templates that the user may edit
        val getterFormat = TemplateSettings.getInstance().getTemplate("getter", "Python")!!
        val setterFormat = TemplateSettings.getInstance().getTemplate("setter", "Python")!!
        val deleterFormat = TemplateSettings.getInstance().getTemplate("deleter", "Python")!!

        // Prompt the user with checkboxes, so they can control which
        // properties they want getters and setters for
        var getterCheckbox: Cell<JBCheckBox>? = null
        var setterCheckbox: Cell<JBCheckBox>? = null
        var deleterCheckbox: Cell<JBCheckBox>? = null
        val properties = HashMap<String, Cell<JBCheckBox>>()
        val panel = panel {
            row {
                getterCheckbox = checkBox("Generate Getters")
                getterCheckbox!!.component.isSelected = true

            }
            row {
                setterCheckbox = checkBox("Generate Setters")
                setterCheckbox!!.component.isSelected = true
            }
            row {
                deleterCheckbox = checkBox("Generate Deleters")
            }

            row {
                button("Edit Format") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Live Templates")
                }
            }

            group("Properties:") {
                for (property in parent.instanceAttributes) {
                    row {
                        val temp = checkBox(property.text.substring("self.".length))
                        temp.component.isSelected = property.text.startsWith("self.__")
                        properties[property.text] = temp
                    }
                }
            }
        }

        // Only generate the getters and setters if the user presses 'Generate'
        val popup = PanelDialog(panel, "Generate Getters and Setters")
        popup.setOkText("Generate")
        val isGenerate = popup.showAndGet()
        if (!isGenerate)
            return

        // Check the user input to determine which methods to generate
        val isGetter = getterCheckbox!!.component.isSelected
        val isSetter = setterCheckbox!!.component.isSelected
        val isDeleter = deleterCheckbox!!.component.isSelected

        val factory = PyElementGenerator.getInstance(project)
        val language = LanguageLevel.getLatest()
        val gettersAndSetters = ArrayList<PyFunction>()
        for (property in parent.instanceAttributes) {

            // Check to see if the user skipped this property
            if (properties[property.text]?.component?.isSelected == false)
                continue

            var name = property.name ?: return
            name = if (name.startsWith("__")) name.substring(2) else name

            if (isGetter) {
                val getter = factory.createFromText(language, PyFunction::class.java, getterFormat.string.replace("${'$'}name$", name).replace("${'$'}property$", property.name.orEmpty()))
                gettersAndSetters.add(getter)
            }
            if (isSetter) {
                val setter = factory.createFromText(language, PyFunction::class.java, setterFormat.string.replace("${'$'}name$", name).replace("${'$'}property$", property.name.orEmpty()))
                gettersAndSetters.add(setter)
            }
            if (isDeleter) {
                val deleter = factory.createFromText(language, PyFunction::class.java, deleterFormat.string.replace("${'$'}name$", name).replace("${'$'}property$", property.name.orEmpty()))
                gettersAndSetters.add(deleter)
            }
        }

        val initMethod = parent.findMethodByName("__init__", false, null)
        if (initMethod == null) {
            Messages.showErrorDialog(project, "Could not find __init__ method in ${parent.name}", "Error")
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
    }
}