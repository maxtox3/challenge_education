package settings.store

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.mvikotlin.core.store.StoreFactory
import settings.ApiSettings
import settings.SettingsState
import storage.StorageService

object SettingsStoreProvider {
    fun create(
        storeFactory: StoreFactory,
        storageService: StorageService,
        initialState: SettingsState = SettingsState(),
        onSettingsSaved: (ApiSettings) -> Unit = {},
    ): SettingsStore = SettingsStoreFactory(
        storeFactory = storeFactory,
        storageService = storageService,
        onSettingsSaved = onSettingsSaved,
    ).create(initialState)
}

@Composable
fun rememberSettingsStore(
    storeFactory: StoreFactory,
    storageService: StorageService,
    initialState: SettingsState = SettingsState(),
    onSettingsSaved: (ApiSettings) -> Unit = {},
): SettingsStore = remember(storeFactory, storageService, initialState, onSettingsSaved) {
    SettingsStoreProvider.create(
        storeFactory = storeFactory,
        storageService = storageService,
        initialState = initialState,
        onSettingsSaved = onSettingsSaved,
    )
}
