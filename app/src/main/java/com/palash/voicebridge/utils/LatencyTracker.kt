package com.palash.voicebridge.utils

/**
 * Tracks pipeline latency for the translation workflow.
 *
 * Requirements:
 * - Real measured stage timings, NEVER hard-coded or fabricated.
 * - Stage-by-stage breakdown (ASR, Normalization, Matching, Translation, TTS).
 * - Multi-trial benchmark recording (min, max, average over trials).
 * - Target: <= 3000ms (sub-3-second official PS target).
 */
class LatencyTracker {

    private var startTime: Long = 0L
    private var endTime: Long = 0L
    private val stages = mutableListOf<Pair<String, Long>>()

    // Multi-trial history for benchmark demo
    private val trials = mutableListOf<Long>()

    /** Record the start of the pipeline */
    fun markStart() {
        startTime = System.currentTimeMillis()
        stages.clear()
        endTime = 0L
    }

    /** Record an intermediate stage */
    fun mark(stageName: String) {
        val now = System.currentTimeMillis()
        val elapsed = if (startTime > 0) now - startTime else 0L
        stages.add(stageName to elapsed)
    }

    /** Record the end of the pipeline and record trial */
    fun markEnd() {
        endTime = System.currentTimeMillis()
        val total = totalMs()
        if (total > 0) {
            trials.add(total)
            if (trials.size > 20) trials.removeAt(0)
        }
    }

    /** Total pipeline latency in milliseconds */
    fun totalMs(): Long {
        val end = if (endTime > 0) endTime else System.currentTimeMillis()
        return if (startTime > 0) end - startTime else 0L
    }

    /** Total pipeline latency formatted */
    fun totalFormatted(): String {
        val ms = totalMs()
        return when {
            ms < 1000 -> "${ms}ms"
            else -> String.format("%.2fs", ms / 1000.0)
        }
    }

    /** Whether pipeline met the 3-second target */
    fun meetsTarget(targetMs: Long = 3000L): Boolean = totalMs() <= targetMs

    /** Stage-by-stage breakdown in milliseconds */
    fun stageReport(): Map<String, Long> {
        val report = linkedMapOf<String, Long>()
        for (i in stages.indices) {
            val (name, elapsed) = stages[i]
            val stageMs = if (i == 0) elapsed else elapsed - stages[i - 1].second
            report[name] = stageMs
        }
        return report
    }

    /** Identifies the slowest stage in the pipeline */
    fun getSlowestStage(): Pair<String, Long>? {
        val report = stageReport()
        return report.maxByOrNull { it.value }?.toPair()
    }

    /** Get multi-trial metrics */
    fun getTrialStats(): TrialStats {
        if (trials.isEmpty()) return TrialStats(0, 0, 0, 0, emptyList())
        val min = trials.minOrNull() ?: 0L
        val max = trials.maxOrNull() ?: 0L
        val avg = trials.average().toLong()
        return TrialStats(trials.size, min, max, avg, trials.toList())
    }

    fun clearHistory() {
        trials.clear()
    }

    data class TrialStats(
        val count: Int,
        val minMs: Long,
        val maxMs: Long,
        val avgMs: Long,
        val trials: List<Long>
    ) {
        fun summary(): String {
            if (count == 0) return "No trials recorded yet"
            return "Trials: $count | Min: ${minMs}ms | Avg: ${avgMs}ms | Max: ${maxMs}ms"
        }
    }

    companion object {
        const val STAGE_ASR = "ASR Recognition"
        const val STAGE_NORMALIZATION = "Normalization"
        const val STAGE_MATCHING = "Curriculum Matching"
        const val STAGE_TRANSLATION = "Translation Lookup"
        const val STAGE_TTS = "TTS Synthesis"
        const val STAGE_PLAYBACK = "Audio Playback"
    }
}
