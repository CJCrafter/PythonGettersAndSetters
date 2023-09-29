package me.cjcrafter.pygetset

import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.python.psi.*
import java.lang.IllegalArgumentException

/**
 * This action creates a popup window. The window includes options that allows
 * people to generate getters, setters, and deleters for python. It uses the
 * "Live Templates" to control the format of the generated methods. Note that
 * in Python, getter/setters should be generally avoided. If you must use a
 * private variable, then this generator is useful for you.
 */
class GenerateGettersSetters : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        var element = psiFile.findElementAt(editor.caretModel.offset) ?: return

        // We might click on a whitespace, which is not always associated with
        // a PyClass. Usually, this happens because the user clicked on a new line
        // character. Just go 1 character before.
        if (element is PsiWhiteSpace) {
            element = psiFile.findElementAt(editor.caretModel.offset - 1) ?: return
        }

        // Find the class that the caret is currently in
        var parent = PsiTreeUtil.getContextOfType(element, PyClass::class.java)

        // If parent is null, then the user's caret is not in a class. We should
        // search the file for a class. Otherwise, show an error to the user.
        if (parent == null) {
            val classes = PsiTreeUtil.findChildrenOfType(psiFile, PyClass::class.java)
            if (classes.size == 1) {
                parent = classes.first()
            }
        }

        if (parent == null) {
            Messages.showErrorDialog(project, "Could not find any class... Move your caret so it is inside of a Python class!", "Error")
            return
        }

        // Prompt the user with checkboxes, so they can control which
        // properties they want getters and setters for
        lateinit var getterCheckbox: Cell<JBCheckBox>
        lateinit var setterCheckbox: Cell<JBCheckBox>
        lateinit var deleterCheckbox: Cell<JBCheckBox>
        val properties = HashMap<String, Cell<JBCheckBox>>()
        val panel = panel {
            row {
                getterCheckbox = checkBox("Generate Getters")
                getterCheckbox.component.isSelected = true
            }
            row {
                setterCheckbox = checkBox("Generate Setters")
                setterCheckbox.component.isSelected = true
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

                // Get properties from @dataclass annotation
                for (property in parent.classAttributes) {
                    row {
                        val name = property.name ?: throw IllegalArgumentException("Property $property has no name")
                        val temp = checkBox(name)
                        temp.component.isSelected = name.startsWith("_")
                        properties[name] = temp
                    }
                }

                // Get properties from __init__ method
                for (property in parent.instanceAttributes) {
                    row {
                        val name = property.name ?: throw IllegalArgumentException("Property $property has no name")
                        val temp = checkBox(name)
                        temp.component.isSelected = name.startsWith("_")
                        properties[name] = temp
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
        val isGetter = getterCheckbox.component.isSelected
        val isSetter = setterCheckbox.component.isSelected
        val isDeleter = deleterCheckbox.component.isSelected

        val factory = PyElementGenerator.getInstance(project)
        val language = LanguageLevel.getLatest()
        val gettersAndSetters = ArrayList<PyFunction>()

        // Access the current live templates that the user may edit
        val getterFormat = TemplateSettings.getInstance().getTemplate("getter", "Python")!!
        val setterFormat = TemplateSettings.getInstance().getTemplate("setter", "Python")!!
        val deleterFormat = TemplateSettings.getInstance().getTemplate("deleter", "Python")!!

        // Loop through the python class' instance variables to generate methods
        for (pair in properties) {

            // Check to see if the user skipped this property
            if (!pair.value.component.isSelected)
                continue

            val name = pair.key.trim('_')

            // Order is important, getter MUST come before setter/deleter
            if (isGetter) {
                val code = getterFormat.string.replace("\$name$", name).replace("\$property$", pair.key) + "\n"
                val getter = factory.createFromText(language, PyFunction::class.java, code)
                gettersAndSetters.add(getter)
            }
            if (isSetter) {
                val code = setterFormat.string.replace("\$name$", name).replace("\$property$", pair.key) + "\n"
                val setter = factory.createFromText(language, PyFunction::class.java, code)
                gettersAndSetters.add(setter)
            }
            if (isDeleter) {
                val code = deleterFormat.string.replace("\$name$", name).replace("\$property$", pair.key) + "\n"
                val deleter = factory.createFromText(language, PyFunction::class.java, code)
                gettersAndSetters.add(deleter)
            }
        }

        // The initMethod is used to determine order. Ideally, we can put the
        // getters/setters right after the init method.
        val initMethod = parent.findMethodByName("__init__", false, null)

        // Using the "add after" method reverses the order of our getters/setters.
        // This is a problem since getters MUST come before setters/deleters
        if (initMethod != null) {
            gettersAndSetters.reverse()
        }

        // WriteCommandAction lets users undo the generation
        WriteCommandAction.runWriteCommandAction(project) {
            for (method in gettersAndSetters) {
                if (initMethod != null)
                    parent.statementList.addAfter(method, initMethod)
                else
                    parent.statementList.add(method)
            }
        }
    }
}