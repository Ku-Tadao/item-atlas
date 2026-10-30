package com.itematlas;

import static com.itematlas.ItemOwnership.Status.BANK;
import static com.itematlas.ItemOwnership.Status.CARRIED;
import static com.itematlas.ItemOwnership.Status.GROUP_STORAGE;
import static com.itematlas.ItemOwnership.Status.MISSING;
import static com.itematlas.ItemOwnership.Status.UNKNOWN;
import static com.itematlas.ItemOwnership.status;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ItemOwnershipTest
{
	private static final int MUDDY_KEY = 991;

	@Test
	public void statusOrder()
	{
		assertEquals(CARRIED, status(true, "991", "991", MUDDY_KEY));
		assertEquals(BANK, status(false, "995,991", "991", MUDDY_KEY));
		assertEquals(GROUP_STORAGE, status(false, "995", "991", MUDDY_KEY));
		assertEquals(MISSING, status(false, "995", null, MUDDY_KEY));
		// an empty bank was still seen
		assertEquals(MISSING, status(false, "", null, MUDDY_KEY));
		// the group storage alone doesn't say what the bank holds
		assertEquals(UNKNOWN, status(false, null, "995", MUDDY_KEY));
	}

	@Test
	public void snapshotMatchesWholeIds()
	{
		assertEquals(MISSING, status(false, "9910,1991", null, MUDDY_KEY));
	}
}
