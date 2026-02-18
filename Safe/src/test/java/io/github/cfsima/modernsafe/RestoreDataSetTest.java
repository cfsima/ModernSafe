package io.github.cfsima.modernsafe;

import org.junit.Test;
import io.github.cfsima.modernsafe.model.RestoreDataSet;

import static org.junit.Assert.assertEquals;

public class RestoreDataSetTest {

    @Test
    public void testSaltAppend() {
        RestoreDataSet dataSet = new RestoreDataSet();
        // We expect setSalt to append.
        // Currently it overwrites, so this test should fail.

        dataSet.setSalt("part1");
        dataSet.setSalt("part2");

        assertEquals("Salt should be appended", "part1part2", dataSet.getSalt());
    }

    @Test
    public void testMasterKeyAppend() {
        RestoreDataSet dataSet = new RestoreDataSet();

        dataSet.setMasterKeyEncrypted("key1");
        dataSet.setMasterKeyEncrypted("key2");

        assertEquals("MasterKey should be appended", "key1key2", dataSet.getMasterKeyEncrypted());
    }

    @Test
    public void testSaltWhitespace() {
        RestoreDataSet dataSet = new RestoreDataSet();
        dataSet.setSalt("\n  someSalt  \n");
        assertEquals("Salt should be trimmed", "someSalt", dataSet.getSalt());
    }

    @Test
    public void testMasterKeyWhitespace() {
        RestoreDataSet dataSet = new RestoreDataSet();
        dataSet.setMasterKeyEncrypted("\n  someKey  \n");
        assertEquals("MasterKeyEncrypted should be trimmed", "someKey", dataSet.getMasterKeyEncrypted());
    }
}
