package io.github.jwp23.koj.storage

import io.github.jwp23.koj.record.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SourceStore(private val dao: SourceDao) {
    /** Sources that are not archived, ordered by name without regard to case. */
    fun sources(): Flow<List<Source>> = dao.all().map { rows -> rows.map { it.toSource() } }

    /** The source with this id, archived or not, or null. */
    suspend fun source(id: Long): Source? = dao.find(id)?.toSource()

    /** Stores a new source (id 0) or replaces the stored one. Returns its id. */
    suspend fun save(source: Source): Long {
        val row = source.toRow()
        if (source.id == 0L) return dao.insert(row)
        dao.update(row)
        return source.id
    }

    /** Removes the source. */
    suspend fun delete(source: Source) = dao.delete(source.toRow())
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
