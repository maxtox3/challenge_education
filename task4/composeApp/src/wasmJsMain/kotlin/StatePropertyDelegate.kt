import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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

fun <T, S> MutableStateFlow<S>.typedProp(getter: (S) -> T, setter: (S, T) -> S): StatePropertyDelegate<T, S> =
    stateProperty(this, getter, setter)

fun <S> MutableStateFlow<S>.updateField(block: (S) -> S) {
    update(block)
}

fun <S> MutableStateFlow<S>.toggleBoolean(getter: (S) -> Boolean, setter: (S, Boolean) -> S) {
    update { currentState -> setter(currentState, !getter(currentState)) }
}

fun <S, T> MutableStateFlow<S>.updateNested(nestedGetter: (S) -> T, nestedSetter: (S, T) -> S, block: (T) -> T) {
    update { currentState ->
        val currentValue = nestedGetter(currentState)
        val newValue = block(currentValue)
        nestedSetter(currentState, newValue)
    }
}
