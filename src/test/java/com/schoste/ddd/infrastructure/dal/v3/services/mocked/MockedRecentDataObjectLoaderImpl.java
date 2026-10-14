package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.Iterator;
import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericRecentDataObjectLoader;

/**
 * Mock implementation of {@link GenericRecentDataObjectLoader} for unit testing.
 * The source records of this loader are the ids of the data objects to load.
 */
@Component
public class MockedRecentDataObjectLoaderImpl extends GenericRecentDataObjectLoader<Long, Long, MockedDO> implements InitializingBean
{
    protected Iterator<Long> sourceRecordIterator;

    /**
     * Creates a new instance of the loader
     *
     * @param dataAccessObject the DAO for which the loader was created. Must be a {@link GenericMockedDAO}
     * @param sourceRecordToDataObjConversionFn a method to get the data object for an id
     * @param lastModTS only data objects with a modification time stamp greater than this value are loaded
     * @param returnDeleted if true, data objects flagged as deleted are loaded too
     * @throws IllegalArgumentException thrown if either dataAccessObject or sourceRecordToDataObjConversionFn are null
     */
    public MockedRecentDataObjectLoaderImpl(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<Long, MockedDO> sourceRecordToDataObjConversionFn, long lastModTS, boolean returnDeleted) throws IllegalArgumentException
    {
        super(dataAccessObject, sourceRecordToDataObjConversionFn, lastModTS, returnDeleted);
    }

    /**
     * Implements {@link InitializingBean#afterPropertiesSet()}. Does nothing in this implementation.
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {

    }

    /**
     * Gets the id of the next data object to load. These are the ids of all data objects in the mocked storage
     * at the time this method is called first.
     *
     * @return the id of the next data object to load, or null if there are no more ids
     */
    @Override
    protected Long getNextSourceRecord()
    {
        if (this.sourceRecordIterator == null) this.sourceRecordIterator = ((GenericMockedDAO<MockedDO>) this.dataAccessObject).getDataObjectIds().iterator();

        if (this.sourceRecordIterator.hasNext()) return this.sourceRecordIterator.next();

        return null;
    }

    /**
     * Implements {@link AutoCloseable#close()}. There is nothing to close in this implementation.
     */
    @Override
    public void close()
    {

    }
}
