package me.cjcrafter.pygetset

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "gettersetterformat",
    storages = [Storage("gettersetterformat.xml")]
)
class GetterSetterService : PersistentStateComponent<GetterSetterState> {

    private var pluginState: GetterSetterState = GetterSetterState()

    override fun getState(): GetterSetterState {
        return pluginState
    }

    override fun loadState(state: GetterSetterState) {
        pluginState = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): PersistentStateComponent<GetterSetterState> {
            return ServiceManager.getService(GetterSetterService::class.java)
        }
    }
}