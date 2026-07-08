package com.borrowhub.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Tes berinstrumen, yang bakal jalan di perangkat Android.
 *
 * Liat [dokumentasi testing](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * Fungsi buat ngecek apakah context aplikasi bener atau nggak.
     * 
     * Langkah-langkahnya:
     * 1. Dapetin instrumentation registry.
     * 2. Ambil target context dari registry itu.
     * 3. Cocokin nama package aplikasi sama yang kita harepin.
     */
    @Test
    fun useAppContext() {
        // Ambil context aplikasi yang lagi di-test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Pastiin kalo nama package-nya itu "com.borrowhub.app".
        assertEquals("com.borrowhub.app", appContext.packageName)
    }
}