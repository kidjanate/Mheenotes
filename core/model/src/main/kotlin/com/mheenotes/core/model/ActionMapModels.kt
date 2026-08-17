package com.mheenotes.core.model

data class GesturePoint(val x: Float, val y: Float) {
    init {
        require(x.isFinite() && y.isFinite()) { "Gesture coordinates must be finite" }
    }
}

data class GestureDrawing(val strokes: List<List<GesturePoint>>) {
    init {
        require(strokes.isNotEmpty() && strokes.all { it.isNotEmpty() }) {
            "A gesture must contain non-empty strokes"
        }
    }
}

enum class MappedAction(val label: String) {
    Undo("Undo"),
    Redo("Redo"),
    Pen("Choose pen"),
    Pencil("Choose pencil"),
    Highlighter("Choose highlighter"),
    Eraser("Choose eraser"),
    Lasso("Choose lasso"),
    Shapes("Choose shapes"),
    Text("Choose text"),
    AddPage("Add page"),
    PreviousPage("Previous page"),
    NextPage("Next page"),
    Image("Insert image"),
    Bookmark("Toggle bookmark"),
    PageOverview("Toggle page overview"),
    ResetZoom("Reset zoom"),
    WidthPreset1("Choose width preset 1"),
    WidthPreset2("Choose width preset 2"),
    WidthPreset3("Choose width preset 3"),
}

data class ActionMapping(
    val id: String,
    val name: String,
    val action: MappedAction,
    val examples: List<GestureDrawing>,
    val rotationInvariant: Boolean = false,
    val confidenceThreshold: Float = 0.78f,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(examples.size in 3..12) { "Action Maps need 3 to 12 examples" }
        require(confidenceThreshold in 0f..1f)
    }
}
