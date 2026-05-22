package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AgriRepository
import com.example.ui.AgriViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun testAppInitializationAndPreload() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = AgriRepository(context)
    
    // Explicitly run the preloader
    repository.preloadDataIfEmpty()
    
    val products = repository.allProducts.first()
    assertEquals(6, products.size)
    
    val users = repository.allUsers.first()
    assertEquals(3, users.size)
    
    val viewModel = AgriViewModel(repository)
    assertNotNull(viewModel)
  }
}
