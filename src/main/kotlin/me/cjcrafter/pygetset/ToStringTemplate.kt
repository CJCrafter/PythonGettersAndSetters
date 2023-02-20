package me.cjcrafter.pygetset

import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyTargetExpression

enum class ToStringTemplate {

    TABULAR_TEMPLATE {
        override fun generate(clazz: PyClass, properties: List<PyTargetExpression>, quote: Char): String {
            var str = "f$quote| "
            var comment = "\n                    # print(f$quote"
            val invertedQuote = if (quote == '\"') '\'' else '\"'
            for (property in properties) {
                str += "{self.${property.name}:<15} | "
                comment += "{$invertedQuote${property.name}$invertedQuote:<15} | "
            }

            // Take away the trailing space, and return the method
            return wrap(str.trim() + quote, comment.trimEnd() + ")" + quote)
        }
    },
    JSON_TEMPLATE {
        override fun generate(clazz: PyClass, properties: List<PyTargetExpression>, quote: Char): String {
            var str = "f$quote| ${clazz.name}{"
            for (property in properties) {
                str += "${property.name}={self.${property.name}}, "
            }

            // Take away the trailing space, and return the method
            return wrap(str.substring(0, str.length - 2) + "}" + quote)
        }
    };

    /**
     * Generates the string content of the python method for this template.
     */
    abstract fun generate(clazz: PyClass, properties: List<PyTargetExpression>, quote: Char): String

    /**
     * Wraps the given content in a python __str__ method. The passed
     * content should be a python f-string.
     */
    internal fun wrap(content: String, comment: String = ""): String {
        return """
                def __str__(self):$comment
                    return $content
                    
            """.trimIndent()
    }
}