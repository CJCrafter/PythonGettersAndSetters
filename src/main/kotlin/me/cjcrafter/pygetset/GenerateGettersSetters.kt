package me.cjcrafter.pygetset

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
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

        val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(GenerateOptionsPanel(project, parent), null)
            .setTitle("Select Properties to Generate Getters and Setters")
            .setMovable(true)
            .createPopup()

        popup.showInFocusCenter()
    }
}