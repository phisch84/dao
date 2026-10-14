package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericRecentDataObjectLoader;

/**
 * Mock implementation of {@link GenericRecentDataObjectLoader} for unit testing
 */
@Component
public class MockedRecentDataObjectLoaderImpl extends GenericRecentDataObjectLoader<Long, Long, MockedDO> implements InitializingBean
{
    /**
     * Creates a new instance of the loader
     * 
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecordToDataObjConversionFn a method to get the next data object of the mocked storage (see {@link GenericMockedDAO#doGet(Long)})
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
     * Always returns a virtual id. It makes {@link GenericMockedDAO#doGet(Long)} return the next data object
     * of the mocked storage, or null after the last one, which ends the loading.
     * 
     * @return always -1
     */
    @Override
    protected Long getNextSourceRecord()
    {
        return -1L;
    }

    /**
     * Implements {@link AutoCloseable#close()}. There is nothing to close in this implementation.
     */
    @Override
    public void close()
    {

    }
}
