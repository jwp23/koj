package io.github.jwp23.koj.storage

import io.github.jwp23.koj.record.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SourceStore(private val dao: SourceDao) {
    /** Sources that are not archived, ordered by name without regard to case. */
    fun sources(): Flow<List<Source>> = dao.all().map { rows -> rows.map { it.toSource() } }

    /** The source with this id, archived or not, or null. */
    suspend fun source(id: Long): Source? = dao.find(id)?.toSource()

    /**
     * Stores a new source (id 0) or replaces the stored one. Returns its id. Throws if the source
     * has an id but no stored source has it, such as one deleted since it was loaded.
     */
    suspend fun save(source: Source): Long {
        val row = source.toRow()
        if (source.id == 0L) return dao.insert(row)
        check(dao.update(row) > 0) { "No stored source has id ${source.id}" }
        return source.id
    }

    /** Archives the source when any drink refers to it; removes it otherwise. */
    suspend fun delete(source: Source) = dao.archiveOrRemove(source.toRow())
}

private fun SourceRow.toSource() =
    Source(
        id = id,
        name = name,
        caffeineMgPerServing = caffeineMgPerServing,
        theanineMgPerServing = theanineMgPerServing,
        style = style,
        defaultDuration = defaultDuration,
        usualTime = usualTime,
        archived = archived,
    )

private fun Source.toRow() =
    SourceRow(
        id = id,
        name = name,
        caffeineMgPerServing = caffeineMgPerServing,
        theanineMgPerServing = theanineMgPerServing,
        style = style,
        defaultDuration = defaultDuration,
        usualTime = usualTime,
        archived = archived,
    )
