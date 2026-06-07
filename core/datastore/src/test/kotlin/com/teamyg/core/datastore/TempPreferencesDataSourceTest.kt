package com.teamyg.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.teamyg.core.datastore.temp.TempPreferencesDataSource
import com.teamyg.core.datastore.temp.TempPreferencesDataSourceImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TempPreferencesDataSourceTest {
    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var dataSource: TempPreferencesDataSource

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { File(tempFolder.root, "test_user.preferences_pb") },
        )
        dataSource = TempPreferencesDataSourceImpl(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `accessToken defaults to null when nothing has been written`() = testScope.runTest {
        assertNull(dataSource.accessToken.first())
    }

    @Test
    fun `setAccessToken persists the provided token`() = testScope.runTest {
        dataSource.setAccessToken("token-123")

        assertEquals("token-123", dataSource.accessToken.first())
    }

    @Test
    fun `setAccessToken overwrites the previously stored token`() = testScope.runTest {
        dataSource.setAccessToken("first-token")
        dataSource.setAccessToken("second-token")

        assertEquals("second-token", dataSource.accessToken.first())
    }

    @Test
    fun `clearAccessToken removes the stored token`() = testScope.runTest {
        dataSource.setAccessToken("token-123")
        dataSource.clearAccessToken()

        assertNull(dataSource.accessToken.first())
    }
}
