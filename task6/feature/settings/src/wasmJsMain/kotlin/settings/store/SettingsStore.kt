package settings.store

import com.arkivanov.mvikotlin.core.store.Store
import settings.SettingsIntent
import settings.SettingsLabel
import settings.SettingsState

interface SettingsStore : Store<SettingsIntent, SettingsState, SettingsLabel>
