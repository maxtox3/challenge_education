import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

class StatePropertyDelegate<T, S>(
    private val stateFlow: MutableStateFlow<S>,
    private val getter: (S) -> T,
    private val setter: (S, T) -> S,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = getter(stateFlow.value)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        stateFlow.update { setter(it, value) }
    }
}

fun <T, S> stateProperty(
    stateFlow: MutableStateFlow<S>,
    getter: (S) -> T,
    setter: (S, T) -> S,
): StatePropertyDelegate<T, S> = StatePropertyDelegate(stateFlow, getter, setter)

fun <T, S> MutableStateFlow<S>.typedProp(getter: (S) -> T, setter: (S, T) -> S,): StatePropertyDelegate<T, S> =
    stateProperty(this, getter, setter)

class ViewModelStateHolder<S>(initialState: S,) {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<SideEffect>()
    val sideEffects: SharedFlow<SideEffect> = _sideEffects.asSharedFlow()

    val state: S get() = _uiState.value

    fun updateState(reducer: (S) -> S) {
        _uiState.update(reducer)
    }

    fun <T> updateField(getter: (S) -> T, setter: (S, T) -> S, value: T) {
        _uiState.update { currentState -> setter(currentState, value) }
    }

    suspend fun emitSideEffect(effect: SideEffect) {
        _sideEffects.emit(effect)
    }

    fun emitSideEffectInScope(scope: CoroutineScope, effect: SideEffect) {
        scope.launch { _sideEffects.emit(effect) }
    }

    fun <T> createProperty(getter: (S) -> T, setter: (S, T) -> S,): StatePropertyDelegate<T, S> =
        stateProperty(_uiState, getter, setter)
}

interface SideEffect

fun <S> MutableStateFlow<S>.updateField(block: (S) -> S) {
    update(block)
}

fun <S> MutableStateFlow<S>.toggleBoolean(getter: (S) -> Boolean, setter: (S, Boolean) -> S) {
    update { currentState -> setter(currentState, !getter(currentState)) }
}

fun <S, T> MutableStateFlow<S>.updateNested(nestedGetter: (S) -> T, nestedSetter: (S, T) -> S, block: (T) -> T,) {
    update { currentState ->
        val currentValue = nestedGetter(currentState)
        val newValue = block(currentValue)
        nestedSetter(currentState, newValue)
    }
}
