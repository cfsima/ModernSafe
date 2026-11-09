package io.github.cfsima.modernsafe

import io.github.cfsima.modernsafe.password.Master
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthManagerTest {

    private var fakeCurrentTime = 100000L

    @Before
    fun setUp() {
        fakeCurrentTime = 100000L
        AuthManager.timeProvider = { fakeCurrentTime }
        AuthManager.setSignedOut()
    }

    @After
    fun tearDown() {
        AuthManager.setSignedOut()
        Master.salt = null
        Master.masterKey = null
    }

    @Test
    fun testIsSignedIn_whenKeysMissing_returnsFalse() {
        Master.salt = null
        Master.masterKey = null
        assertFalse(AuthManager.isSignedIn())

        Master.salt = "fakeSalt"
        Master.masterKey = null
        assertFalse(AuthManager.isSignedIn())

        Master.salt = null
        Master.masterKey = "fakeKey"
        assertFalse(AuthManager.isSignedIn())
    }

    @Test
    fun testIsSignedIn_whenKeysPresent_returnsTrue() {
        Master.salt = "fakeSalt"
        Master.masterKey = "fakeKey"
        assertTrue(AuthManager.isSignedIn())
    }

    @Test
    fun testSetSignedOut_clearsState() {
        Master.salt = "fakeSalt"
        Master.masterKey = "fakeKey"
        AuthManager.lastUsedPassword = "pass"
        AuthManager.timeRemaining = 5000L
        AuthManager.updateLastActiveTime()

        AuthManager.setSignedOut()

        assertFalse(AuthManager.isSignedIn())
        assertEquals(null, Master.masterKey)
        assertEquals(null, AuthManager.lastUsedPassword)
        assertEquals(0L, AuthManager.timeRemaining)
        assertEquals(0L, AuthManager.lastActiveTimestamp)
    }

    @Test
    fun testSessionTimeout_whenNotTimedOut() {
        Master.salt = "fakeSalt"
        Master.masterKey = "fakeKey"
        AuthManager.updateLastActiveTime()

        // 4 minutes later (timeout is 5 minutes)
        fakeCurrentTime += 4 * 60 * 1000L

        assertFalse(AuthManager.isSessionTimedOut(5))
    }

    @Test
    fun testSessionTimeout_whenTimedOut() {
        Master.salt = "fakeSalt"
        Master.masterKey = "fakeKey"
        AuthManager.updateLastActiveTime()

        // 5 minutes later (timeout is 5 minutes)
        fakeCurrentTime += 5 * 60 * 1000L

        assertTrue(AuthManager.isSessionTimedOut(5))
    }

    @Test
    fun testSessionTimeout_whenSignedOut_returnsTrue() {
        AuthManager.setSignedOut()
        assertTrue(AuthManager.isSessionTimedOut(5))
    }
}
