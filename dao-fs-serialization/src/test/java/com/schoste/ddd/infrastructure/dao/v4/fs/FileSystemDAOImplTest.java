package com.schoste.ddd.infrastructure.dao.v4.fs;

import java.io.File;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of the raw file DAO which go beyond the DAO contract tests ({@link RawFileDAOContractTest})
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class FileSystemDAOImplTest
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
		FileSystemDAOImpl dao = new FileSystemDAOImpl(daoPath);

		Assert.assertTrue(Files.isDirectory(dao.getStoragePath()));

		Files.delete(dao.getStoragePath());
		Files.delete(dao.getStoragePath().getParent());
	}
}
