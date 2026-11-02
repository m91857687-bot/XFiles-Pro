package com.xfiles.pro.core

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.core.result.getOrDefault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultWrapperTest {

    @Test
    fun `success returns data correctly`() {
        val success: ResultWrapper<String> = ResultWrapper.Success("file_data")
        assertTrue(success.isSuccess)
        assertEquals("file_data", success.getOrNull())
        assertEquals("file_data", success.getOrDefault("default"))
    }

    @Test
    fun `error returns null data and contains throwable`() {
        val error: ResultWrapper<String> = ResultWrapper.Error(IllegalStateException("Failed"))
        assertTrue(error.isError)
        assertNull(error.getOrNull())
        assertEquals("default", error.getOrDefault("default"))
        assertEquals("Failed", (error as ResultWrapper.Error).message)
    }

    @Test
    fun `loading state identifies as loading`() {
        val loading: ResultWrapper<String> = ResultWrapper.Loading
        assertTrue(loading.isLoading)
        assertNull(loading.getOrNull())
    }
}
