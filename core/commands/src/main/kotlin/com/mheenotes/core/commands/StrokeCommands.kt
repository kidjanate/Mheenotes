package com.mheenotes.core.commands

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PageState
import com.mheenotes.core.model.StrokeId

sealed interface PageCommand {
    fun apply(page: PageState): PageState
}

data class AddStroke(val stroke: InkStroke) : PageCommand {
    override fun apply(page: PageState): PageState {
        require(stroke.pageId == page.id) { "Stroke belongs to another page" }
        require(page.strokes.none { it.id == stroke.id }) { "Duplicate stroke ID" }
        return page.copy(strokes = page.strokes + stroke, revision = page.revision + 1)
    }
}

data class AddStrokes(val strokes: List<InkStroke>) : PageCommand {
    override fun apply(page: PageState): PageState {
        require(strokes.isNotEmpty()) { "No strokes supplied" }
        require(strokes.all { it.pageId == page.id }) { "Stroke belongs to another page" }
        require(strokes.map { it.id }.distinct().size == strokes.size) { "Duplicate stroke IDs" }
        require(page.strokes.none { existing -> strokes.any { it.id == existing.id } }) { "Stroke already exists" }
        return page.copy(strokes = page.strokes + strokes, revision = page.revision + 1)
    }
}

data class DeleteStroke(val strokeId: StrokeId) : PageCommand {
    override fun apply(page: PageState): PageState {
        require(page.strokes.any { it.id == strokeId }) { "Stroke does not exist" }
        return page.copy(
            strokes = page.strokes.filterNot { it.id == strokeId },
            revision = page.revision + 1,
        )
    }
}

data class DeleteStrokes(val strokeIds: Set<StrokeId>) : PageCommand {
    override fun apply(page: PageState): PageState {
        require(strokeIds.isNotEmpty()) { "No strokes selected" }
        require(page.strokes.count { it.id in strokeIds } == strokeIds.size) {
            "One or more strokes do not exist"
        }
        return page.copy(
            strokes = page.strokes.filterNot { it.id in strokeIds },
            revision = page.revision + 1,
        )
    }
}

data class ReplaceStroke(
    val strokeId: StrokeId,
    val replacements: List<InkStroke>,
) : PageCommand {
    override fun apply(page: PageState): PageState {
        val index = page.strokes.indexOfFirst { it.id == strokeId }
        require(index >= 0) { "Stroke does not exist" }
        require(replacements.all { it.pageId == page.id }) { "Replacement belongs to another page" }
        require(replacements.map { it.id }.distinct().size == replacements.size) {
            "Replacement IDs must be unique"
        }
        val updated = page.strokes.toMutableList().apply {
            removeAt(index)
            addAll(index, replacements)
        }
        return page.copy(strokes = updated, revision = page.revision + 1)
    }
}

data class ReplaceStrokes(
    val strokeIds: Set<StrokeId>,
    val replacements: List<InkStroke>,
) : PageCommand {
    override fun apply(page: PageState): PageState {
        require(strokeIds.isNotEmpty()) { "No strokes selected" }
        require(page.strokes.count { it.id in strokeIds } == strokeIds.size) { "One or more strokes do not exist" }
        require(replacements.all { it.pageId == page.id }) { "Replacement belongs to another page" }
        require(replacements.map { it.id }.distinct().size == replacements.size) { "Replacement IDs must be unique" }
        val retained = page.strokes.filterNot { it.id in strokeIds }
        return page.copy(strokes = (retained + replacements).sortedBy(InkStroke::zIndex), revision = page.revision + 1)
    }
}

class PageHistory(initial: PageState, private val capacity: Int = 200) {
    private val undo = ArrayDeque<PageState>()
    private val redo = ArrayDeque<PageState>()

    var current: PageState = initial
        private set

    init {
        require(capacity > 0)
    }

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()

    fun execute(command: PageCommand): PageState {
        undo.addLast(current)
        while (undo.size > capacity) undo.removeFirst()
        current = command.apply(current)
        redo.clear()
        return current
    }

    fun undo(): PageState {
        if (undo.isEmpty()) return current
        redo.addLast(current)
        current = undo.removeLast()
        return current
    }

    fun redo(): PageState {
        if (redo.isEmpty()) return current
        undo.addLast(current)
        current = redo.removeLast()
        return current
    }
}
