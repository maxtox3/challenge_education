package model

enum class ModelType(val id: String, val displayName: String, val level: String, val provider: ApiProvider) {
    LIGHT("glm-4-32b-0414-128k", "GLM-4 32b", "Супер легкая, слабая и дешевая", ApiProvider.ZAI),
    AIR("glm-4.5-air", "GLM-4.5 Air", "Слабая", ApiProvider.ZAI),
    MIDDLE("glm-4.7", "GLM-4.7", "Средняя", ApiProvider.ZAI),
    PRO("glm-5", "GLM-5", "Сильная", ApiProvider.ZAI),
    OPENROUTER_QWEN("stepfun/step-3.5-flash:free", "StepFun: Step 3.5 Flash", "Free", ApiProvider.OPENROUTER);

    companion object {
        fun fromId(id: String): ModelType = entries.find { it.id == id } ?: PRO
        fun forProvider(provider: ApiProvider): List<ModelType> = entries.filter { it.provider == provider }
        fun defaultFor(provider: ApiProvider): ModelType = forProvider(provider).firstOrNull() ?: PRO
    }
}
