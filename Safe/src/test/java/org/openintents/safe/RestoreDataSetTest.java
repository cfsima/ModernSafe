package org.openintents.safe;

import org.junit.Test;
import org.openintents.safe.model.RestoreDataSet;

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
}
