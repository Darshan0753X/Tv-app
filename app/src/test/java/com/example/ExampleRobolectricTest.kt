package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.NetworkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TV Cast & Remote Hub", appName)
    }

    @Test
    fun `file size formatting utility formats large movie files accurately`() {
        val formatted1GB = NetworkUtils.formatFileSize(1500000000L)
        assertTrue(formatted1GB.contains("GB") || formatted1GB.contains("1.5"))

        val formattedMB = NetworkUtils.formatFileSize(15000000L)
        assertTrue(formattedMB.contains("MB"))
    }
}
