package com.dscorp.ispadmin.presentation.ui.semantics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class TestTagResourceIdRootTest {

    @Test
    fun `MainActivity envuelve el contenido con TestTagResourceIdRoot`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/main/MainActivity.kt"
        ).readText()
        assertThat(source).contains("TestTagResourceIdRoot")
        assertThat(source).contains("IpsAdminNavHost(")
    }

    @Test
    fun `TestTagResourceIdRoot activa testTagsAsResourceId`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/semantics/TestTagResourceIdRoot.kt"
        ).readText()
        assertThat(source).contains("testTagsAsResourceId = true")
    }
}
