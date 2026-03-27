package model

enum class ModelType(val id: String, val displayName: String, val level: String) {
    AIR("glm4.5-air", "GLM-4.5 Air", "Слабая"),
    PLUS("glm4.7", "GLM-4.7", "Средняя"),
    PRO("glm5", "GLM-5", "Сильная");

    companion object {
        fun fromId(id: String): ModelType = entries.find { it.id == id } ?: PRO

        val allIds: List<String> get() = entries.map { it.id }
    }
}
