package me.cjcrafter.pygetset

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import java.util.Objects

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

        // Setup variables for the loop
        val factory = PyElementGenerator.getInstance(project)
        val language = LanguageLevel.getLatest()

        val gettersAndSetters = ArrayList<PyFunction>()
        for (property in parent.instanceAttributes) {
            var name = property.name ?: return
            name = if (name.startsWith("__")) name.substring(2) else name

            val getter = factory.createFromText(
                language, PyFunction::class.java, """
                @property
                def ${name}(self):
                    return self.${property.name} 
                
            """.trimIndent()
            )

            val setter = factory.createFromText(
                language, PyFunction::class.java, """
                @${name}.setter
                def ${name}(self, ${name}):
                    self.${property.name} = $name
                    
            """.trimIndent()
            )

            gettersAndSetters.add(getter)
            gettersAndSetters.add(setter)
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

        Messages.showInfoMessage(
            project, "Generated ${gettersAndSetters.size} methods\n\n" +
                    gettersAndSetters, "Generated Getters and Setters"
        )
    }
}