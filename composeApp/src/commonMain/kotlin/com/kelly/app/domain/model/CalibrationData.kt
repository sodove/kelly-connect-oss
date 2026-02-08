package com.kelly.app.domain.model

import com.kelly.protocol.ControllerModel
import com.kelly.protocol.ParameterDef

data class CalibrationData(
    val model: ControllerModel,
    val dataValue: IntArray,
    val parameters: List<ParameterDef>,
    val currentPage: Int = 0
) {
    val totalPages: Int get() = 3 // Pages 0-2, each covering 128 offsets

    fun parametersForPage(page: Int): List<ParameterDef> {
        val startOffset = page * 128
        val endOffset = (page + 1) * 128
        return parameters.filter { it.offset in startOffset until endOffset && it.visible }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CalibrationData) return false
        return model == other.model &&
                dataValue.contentEquals(other.dataValue) &&
                parameters == other.parameters &&
                currentPage == other.currentPage
    }

    override fun hashCode(): Int {
        var result = model.hashCode()
        result = 31 * result + dataValue.contentHashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + currentPage
        return result
    }
}
