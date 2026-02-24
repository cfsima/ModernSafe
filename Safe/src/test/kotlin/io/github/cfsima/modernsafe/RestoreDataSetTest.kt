package io.github.cfsima.modernsafe

import io.github.cfsima.modernsafe.model.RestoreDataSet
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreDataSetTest {

    @Test
    fun testSaltAppend() {
        val dataSet = RestoreDataSet()
        // We expect setSalt to append.

        dataSet.salt = "part1"
        dataSet.salt = "part2"

        assertEquals("Salt should be appended", "part1part2", dataSet.salt)
    }

    @Test
    fun testMasterKeyAppend() {
        val dataSet = RestoreDataSet()

        dataSet.masterKeyEncrypted = "key1"
        dataSet.masterKeyEncrypted = "key2"

        assertEquals("MasterKey should be appended", "key1key2", dataSet.masterKeyEncrypted)
    }

    @Test
    fun testSaltWhitespace() {
        val dataSet = RestoreDataSet()
        dataSet.salt = "\n  someSalt  \n"
        assertEquals("Salt should be trimmed", "someSalt", dataSet.salt)
    }

    @Test
    fun testMasterKeyWhitespace() {
        val dataSet = RestoreDataSet()
        dataSet.masterKeyEncrypted = "\n  someKey  \n"
        assertEquals("MasterKeyEncrypted should be trimmed", "someKey", dataSet.masterKeyEncrypted)
    }
}
