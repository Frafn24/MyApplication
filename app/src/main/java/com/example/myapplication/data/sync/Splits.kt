package com.example.myapplication.data.sync

fun equalSplit(amountInCents: Long, userIds: List<String>): Map<String, Long> {
    val people = userIds.distinct()
    if (people.isEmpty()) return emptyMap()

    val share = amountInCents / people.size
    val extra = (amountInCents % people.size).toInt()

    return people.mapIndexed { index, userId ->
        userId to share + if (index < extra) 1L else 0L
    }.toMap()
}

fun encodeSplits(splits: Map<String, Long>): String = splits.entries.joinToString("|") { "${it.key}:${it.value}" }

fun decodeSplits(value: String): Map<String, Long> = value.split("|")
    .filter(String::isNotBlank)
    .mapNotNull { entry ->
        val parts = entry.split(":", limit = 2)
        val cents = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
        parts[0] to cents
    }
    .toMap()
