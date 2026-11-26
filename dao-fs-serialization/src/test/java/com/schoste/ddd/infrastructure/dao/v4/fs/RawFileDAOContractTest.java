package com.schoste.ddd.infrastructure.dao.v4.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAOContractTest;

/**
 * Runs the DAO contract tests ({@link AbstractDAOContractTest}) on the raw file DAO, which deletes physically
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class RawFileDAOContractTest extends AbstractDAOContractTest<Long, FileSystemDO, FileSystemDAOImpl>
{
	@Autowired
	private FileSystemDAOImpl dao;

	@Override
	protected FileSystemDAOImpl getDAO() { return this.dao; }

	@Override
	protected void setValue(FileSystemDO dataObject, String value) { dataObject.setData(value.getBytes(StandardCharsets.UTF_8)); }

	@Override
	protected String getValue(FileSystemDO dataObject) { return new String(dataObject.getData(), StandardCharsets.UTF_8); }

	@Override
	protected Long getNotExistingId() { return Long.MAX_VALUE; }

	/**
	 * {@inheritDoc}
	 *
	 * The raw file DAO stores nothing but the data of a file object, so it can't store a deleted flag.
	 */
	@Override
	protected boolean deletesPhysically() { return true; }

	/**
	 * {@inheritDoc}
	 *
	 * The time stamps of the raw file DAO come from the file system, whose clock can be coarser than the JVM's (e.g. on Linux,
	 * files written a few milliseconds apart can get the same modification time). Therefore a probe file next to the storage
	 * folder (on the same file system) is written again until its modification time has advanced.
	 */
	@Override
	protected void awaitStorageClockTick()
	{
		Path probe = this.dao.getStoragePath().resolveSibling(this.dao.getStoragePath().getFileName() + ".clock-probe");

		try
		{
			long start = writeAndGetModifiedTime(probe);

			while (writeAndGetModifiedTime(probe) <= start) Thread.yield();

			Files.deleteIfExists(probe);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private static long writeAndGetModifiedTime(Path file) throws IOException
	{
		Files.write(file, new byte[] { 0 });

		return Files.getLastModifiedTime(file).toMillis();
	}
}
