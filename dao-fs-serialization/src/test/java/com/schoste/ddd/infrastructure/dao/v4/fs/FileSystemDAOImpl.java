package com.schoste.ddd.infrastructure.dao.v4.fs;

import com.schoste.ddd.infrastructure.dao.v4.fs.FileSystemDO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Example file system data access object used in unit testing of the AbstractRawFileDAO implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class FileSystemDAOImpl extends AbstractRawFileDAO<Long, FileSystemDO>
{
	@Autowired
	protected ApplicationContext applicationContext;

	protected long lastVirtualId=0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected  FileSystemDO doCreateDataObject() throws Exception
	{
		FileSystemDO newFsDO = this.applicationContext.getBean(FileSystemDO.class);

		newFsDO.setId(this.doCreateVirtualId());

		return newFsDO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Long toId(String value)
	{
		if (value == null) return 0L;

		try
		{
			return Long.valueOf(value);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);

			return 0L;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Long doCreateVirtualId() throws Exception 
	{
		long virtualId = System.currentTimeMillis()*(-1);

		while (this.lastVirtualId <= virtualId) virtualId--;

		this.lastVirtualId = virtualId;

		return virtualId;
	}

	/**
	 * {@inheritDoc}
	 */

	@Override
	protected Long doCreateActualId(Long virtualId) throws Exception
	{
		Long actualId = this.getNewFileId();

		return actualId;
	}

	protected synchronized long getNewFileId()
	{
		long now = System.currentTimeMillis();
		Path fileToCheck = Paths.get(this.storagePath.toString(), String.valueOf(now));
		
		while (Files.exists(fileToCheck)) fileToCheck = Paths.get(this.storagePath.toString(), String.valueOf(++now));
		
		return now;
	}

	/**
	 * {@inheritDoc}
	 */
	public FileSystemDAOImpl(String storagePath) throws IllegalArgumentException, IllegalStateException, Exception 
	{
		super(storagePath);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public boolean isVirtualId(Long id)
	{
		if (id == null) return false;
		if (id >= 0) return false;

		return true;
	}

}
