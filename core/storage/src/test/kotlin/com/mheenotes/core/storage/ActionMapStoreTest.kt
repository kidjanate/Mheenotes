package com.mheenotes.core.storage

import com.mheenotes.core.model.ActionMapping
import com.mheenotes.core.model.GestureDrawing
import com.mheenotes.core.model.GesturePoint
import com.mheenotes.core.model.MappedAction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class ActionMapStoreTest {
    @Test
    fun roundTripsMappings() {
        val file = Files.createTempDirectory("action-map-test").resolve("maps.bin").toFile()
        val drawing = GestureDrawing(listOf(listOf(GesturePoint(1f, 2f), GesturePoint(3f, 4f))))
        val source = listOf(ActionMapping("id", "Next", MappedAction.NextPage, List(3) { drawing }, true, .81f))
        val store = ActionMapStore(file)
        store.save(source)
        assertEquals(source, store.load())
    }
}
