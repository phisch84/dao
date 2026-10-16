package com.schoste.ddd.infrastructure.dal.v3.services.fs;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import com.schoste.ddd.infrastructure.dal.v3.models.FileSystemDO;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedDeleteListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedGetListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedSaveListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.SaveListener;

/**
 * Test class of the FileSystemDAOImpl implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class FileSystemDAOImplTest extends GenericFileSystemDAOTest<Long, FileSystemDO, FileSystemDAOImpl>
{
	@Autowired
	protected FileSystemDAOImpl fileSystemDAOImpl;


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersBeforeGet(int numOfexpectedDOs)
	{
		Collection<GetListener<Long, FileSystemDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, FileSystemDO> getListener : getListeners)
		{
			MockedGetListener<FileSystemDO> mockedGetListener = (MockedGetListener<FileSystemDO>) getListener;

			if ((mockedGetListener).getIdsReceivedBeforeGetting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersDOLsBeforeGet(int numOfexpectedDOLs)
	{
		Collection<GetListener<Long, FileSystemDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, FileSystemDO> getListener : getListeners)
		{
			MockedGetListener<FileSystemDO> mockedGetListener = (MockedGetListener<FileSystemDO>) getListener;

			if ((mockedGetListener).getDOLsReceivedBeforeGetting().size() != numOfexpectedDOLs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersAfterGet(int numOfexpectedDOs)
	{
		Collection<GetListener<Long, FileSystemDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, FileSystemDO> getListener : getListeners)
		{
			MockedGetListener<FileSystemDO> mockedGetListener = (MockedGetListener<FileSystemDO>) getListener;

			if ((mockedGetListener).getDOsReceivedAfterGetting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultSaveListenersBeforeSave(int numOfexpectedDOs)
	{
		Collection<SaveListener<Long, FileSystemDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, FileSystemDO> saveListener : saveListeners)
		{
			MockedSaveListener<FileSystemDO> mockedSaveListener = (MockedSaveListener<FileSystemDO>) saveListener;

			if ((mockedSaveListener).getDOsReceivedBeforeSaving().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultSaveListenersAfterSave(int numOfexpectedDOs)
	{
		Collection<SaveListener<Long, FileSystemDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, FileSystemDO> saveListener : saveListeners)
		{
			MockedSaveListener<FileSystemDO> mockedSaveListener = (MockedSaveListener<FileSystemDO>) saveListener;

			if ((mockedSaveListener).getDOsReceivedAfterSaving().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultDeleteListenersBeforeDelete(int numOfexpectedDOs)
	{
		Collection<DeleteListener<Long, FileSystemDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, FileSystemDO> deleteListener : deleteListeners)
		{
			MockedDeleteListener<FileSystemDO> mockedSaveListener = (MockedDeleteListener<FileSystemDO>) deleteListener;

			if ((mockedSaveListener).getDOsReceivedBeforeDeleting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultDeleteListenersAfterDelete(int numOfexpectedDOs)
	{
		Collection<DeleteListener<Long, FileSystemDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, FileSystemDO> deleteListener : deleteListeners)
		{
			MockedDeleteListener<FileSystemDO> mockedDeleteListener = (MockedDeleteListener<FileSystemDO>) deleteListener;

			if ((mockedDeleteListener).getDOsReceivedAfterDeleting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Long getNotExistingId() 
	{
		return 9999L;
	}

	/**
	 * Executed before every test is executed.
	 * Clears the DAO and stores new data objects into it.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Before
	public void initializeTest() throws Exception
	{
		this.clearAndSetUpExistingDataObjects();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected FileSystemDAOImpl getDataAccessObject() 
	{
		return this.fileSystemDAOImpl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected FileSystemDO createDataObject(Object... parameters) throws Exception
	{
		FileSystemDO dataObject = this.fileSystemDAOImpl.createDataObject();
		
		if ((parameters == null) || (parameters.length < 1)) return dataObject;	
		if (parameters.length > 0) dataObject.setId((Long) parameters[0]);
		
		return dataObject;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getDefaultResourceName() 
	{
		return "fs-dao-testfile.zip";
	}
	
	/**
	 * Asserts that the DAO can be created with nested paths
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testConstructorWithNestedPaths() throws Exception
	{
		String daoPath = String.format("%s%s%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, System.nanoTime(), File.separator, "TEST");
		FileSystemDAOImpl dao = new FileSystemDAOImpl(daoPath);

		Assert.notNull(dao, "");

		Files.delete(dao.getStoragePath());
		Files.delete(dao.getStoragePath().getParent());
	}

	@Override
	protected void modifyDataObject(FileSystemDO dataObject, Object... parameters) 
	{
		return;
	}

	@Override
	protected boolean clearRepositorySucceeded() throws Exception 
	{
		this.deployDataObjects(this.getDefaultResourceName(), Arrays.asList(new String[] {"1"}));
		this.getDataAccessObject().clear();

		return (this.getDataAccessObject().getAll().collect(Collectors.toList()).size() == 0);
	}

}
