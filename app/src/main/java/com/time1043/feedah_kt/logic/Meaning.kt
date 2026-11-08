package com.time1043.feedah_kt.logic

// Matches a leading part-of-speech tag: n. / v. / adj. / prep. / conj. …
private val POS_TAG = Regex("^[a-z]+\\.", RegexOption.IGNORE_CASE)

/**
 * Groups a meaning's "、"-separated senses by part of speech: a sense that
 * starts with a POS tag opens a new line, and any following bare senses join
 * it with "、" so one POS group stays together. Returns null when no break
 * is needed: the meaning is short, or every sense belongs to one group.
 *   "prep. 为了……、对于……、conj. 因为"
 *     -> ["prep. 为了……、对于……", "conj. 因为"]
 */
fun meaningLines(meaning: String): List<String>? {
    val senses = meaning
        .split('、')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (senses.size < 3) return null
    val lines = mutableListOf<String>()
    for (sense in senses) {
        if (lines.isEmpty() || POS_TAG.containsMatchIn(sense)) lines.add(sense)
        else lines[lines.size - 1] += "、$sense"
    }
    return if (lines.size > 1) lines else null
}
