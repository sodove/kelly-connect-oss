package com.kelly.protocol

/**
 * Supported Kelly controller models.
 * Only KBLS (KLS) series with firmware v262+ is supported.
 */
enum class ControllerModel {
    KBLS_0106,  // KLS firmware v262-264
    KBLS_0109;  // KLS firmware v265+

    companion object {
        /**
         * Detect controller model from module name and software version.
         *
         * Module name is 8 ASCII chars at DataValue[0..7].
         * Software version is big-endian 16-bit at DataValue[16..17].
         *
         * Detection logic (from original KellyPage.java:252-354):
         *   substring(1,4) == "BLS" or "BSS" or substring(1,3) == "LS" -> KBLS series
         *     v265+ -> KBLS_0109
         *     v262-264 -> KBLS_0106
         *     < v262 -> unsupported
         *   Everything else -> unsupported
         *
         * @param moduleName ASCII module name (8 chars, e.g. "KBLS7218")
         * @param softwareVersion firmware version number (e.g. 265)
         * @return detected model or null if unsupported
         */
        fun detect(moduleName: String, softwareVersion: Int): Result<ControllerModel> {
            if (moduleName.length < 4) {
                return Result.failure(UnsupportedControllerException("Module name too short: '$moduleName'"))
            }

            val name3 = moduleName.substring(1, 4)
            val name2 = moduleName.substring(1, 3)

            val isKBLS = name3 == "BLS" || name3 == "BSS" || name2 == "LS"

            if (!isKBLS) {
                return Result.failure(UnsupportedControllerException(
                    "Unsupported controller type: '$moduleName'. Only KBLS (KLS) series is supported."
                ))
            }

            return when {
                softwareVersion >= 265 -> Result.success(KBLS_0109)
                softwareVersion >= 262 -> Result.success(KBLS_0106)
                else -> Result.failure(UnsupportedControllerException(
                    "Unsupported firmware version: $softwareVersion. Minimum required: 262"
                ))
            }
        }
    }
}

class UnsupportedControllerException(message: String) : Exception(message)
