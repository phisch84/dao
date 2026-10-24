package com.schoste.ddd.infrastructure.dao.v4.serialization;

import java.io.File;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of the serialization DAO which go beyond the DAO contract tests ({@link SerializationDAOContractTest})
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class SerializationDAOImplTest
{
	/**
	 * The DAO creates its storage folder, including missing parent folders.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void constructor_nestedNotExistingPath_createsFolders() throws Exception
	{
		String daoPath = String.format("%s%s%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, System.nanoTime(), File.separator, "TEST");
		SerializationDAOImpl dao = new SerializationDAOImpl(daoPath);

		Assert.assertTrue(Files.isDirectory(dao.getStoragePath()));

		Files.delete(dao.getStoragePath());
		Files.delete(dao.getStoragePath().getParent());
	}
}
