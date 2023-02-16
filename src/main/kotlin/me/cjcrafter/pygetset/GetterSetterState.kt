package me.cjcrafter.pygetset

data class GetterSetterState(

    // Default format for getters uses python's property option
    var isGetter: Boolean = true,
    var getter: String = """
            @property
            def {name}(self):
                return self.{property}
            
    """.trimIndent(),

    // Default format for setters uses python's property option
    var isSetter: Boolean = true,
    var setter: String = """
            @{name}.setter
            def {name}(self, {name}):
                self.{property} = {name}
            
    """.trimIndent(),

    // Default format for deleters uses python's property option
    var isDeleter: Boolean = true,
    var deleter: String = """
            @{name}.deleter
            def {name}(self, {name}):
                del self.{property}
            
    """.trimIndent()
)