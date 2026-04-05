package settings.store

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.mvikotlin.core.store.StoreFactory
import settings.ApiSettings
import settings.SettingsState

object SettingsStoreProvider {
    fun create(
        storeFactory: StoreFactory,
        initialState: SettingsState = SettingsState(),
        onSettingsSaved: (ApiSettings) -> Unit = {},
    ): SettingsStore = SettingsStoreFactory(
        storeFactory = storeFactory,
        onSettingsSaved = onSettingsSaved,
    ).create(initialState)
}

@Composable
fun rememberSettingsStore(
    storeFactory: StoreFactory,
    initialState: SettingsState = SettingsState(),
    onSettingsSaved: (ApiSettings) -> Unit = {},
): SettingsStore = remember(storeFactory, initialState, onSettingsSaved) {
    SettingsStoreProvider.create(
        storeFactory = storeFactory,
        initialState = initialState,
        onSettingsSaved = onSettingsSaved,
    )
}
