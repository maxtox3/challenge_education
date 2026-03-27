package model

enum class ModelType(val id: String, val displayName: String, val level: String) {
    LIGHT("glm-4-32b-0414-128k", "GLM-4 32b", "Супер легкая, слабая и дешевая"),
    AIR("glm-4.5-air", "GLM-4.5 Air", "Слабая"),
    MIDDLE("glm-4.7", "GLM-4.7", "Средняя"),
    PRO("glm-5", "GLM-5", "Сильная");

    companion object {
        fun fromId(id: String): ModelType = entries.find { it.id == id } ?: PRO

        val allIds: List<String> get() = entries.map { it.id }
    }
}
