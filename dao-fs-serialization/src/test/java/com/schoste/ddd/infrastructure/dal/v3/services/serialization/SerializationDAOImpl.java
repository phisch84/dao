package com.schoste.ddd.infrastructure.dal.v3.services.serialization;

import com.schoste.ddd.infrastructure.dal.v3.models.SerializationDO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Example serialization data access object used in unit testing of the GenericSerializationDAO implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class SerializationDAOImpl extends GenericSerializationDAO<Long, SerializationDO>
{
	@Autowired
	protected ApplicationContext applicationContext;

	protected long lastVirtualId=0;

	/**
	 * {@inheritDoc}
	 */
	public SerializationDAOImpl(String storagePath) throws IllegalArgumentException, IllegalStateException, Exception 
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
		if (id > 0) return false;

		return true;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Long getNextVirtualId(Object... args) throws Exception 
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
	protected Long getActualId(Object... args) throws Exception 
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
	@Override 
	protected Long toID(String value)
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
	protected SerializationDO doCreateDataObject(Object... args) throws Exception
	{
		return (SerializationDO) this.applicationContext.getBean(SerializationDO.class);
	}
}
