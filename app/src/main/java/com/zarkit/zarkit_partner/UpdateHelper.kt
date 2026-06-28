package com.zarkit.zarkit_partner

object UpdateHelper {

    /**
     * MAJOR.MINOR.PATCH
     * 1.0.0 → 1.0.1  = FLEXIBLE  (sirf PATCH badla)
     * 1.0.0 → 1.1.0  = IMMEDIATE (MINOR badla)
     * 1.0.0 → 2.0.0  = IMMEDIATE (MAJOR badla)
     */
    fun decideUpdateType(currentVersion: String, newVersion: String): UpdateDecision {
        return try {
            val current = parse(currentVersion)
            val new = parse(newVersion)

            when {
                new.major != current.major || new.minor != current.minor ->
                    UpdateDecision.IMMEDIATE

                new.patch > current.patch ->
                    UpdateDecision.FLEXIBLE

                else ->
                    UpdateDecision.NONE
            }

        } catch (e: Exception) {
            UpdateDecision.NONE
        }
    }

    private fun parse(version: String): Version {
        val parts = version.trim().split(".")
        return Version(
            major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }

    data class Version(val major: Int, val minor: Int, val patch: Int)

    enum class UpdateDecision { IMMEDIATE, FLEXIBLE, NONE }
}