package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDefaultDataObjectLoader;

/**
 * Mock implementation of {@link GenericDefaultDataObjectLoader} for unit testing.
 * The source records of this loader are the ids of the data objects to load.
 */
@Component
public class MockedDefaultDataObjectLoaderImpl extends GenericDefaultDataObjectLoader<Long, Long, MockedDO> implements InitializingBean
{
    protected Iterator<Long> sourceRecordIterator;

    /**
     * Creates a new instance of the loader that loads all data objects
     * 
     * @param dataAccessObject the DAO for which the loader was created. Must be a {@link GenericMockedDAO}
     * @param sourceRecordTodataObjConversionFn a method to get the data object for an id
     */
    public MockedDefaultDataObjectLoaderImpl(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<Long, MockedDO> sourceRecordTodataObjConversionFn)
    {
        super(dataAccessObject, sourceRecordTodataObjConversionFn);
    }

    /**
     * Creates a new instance of the loader that loads the data objects with given ids
     * 
     * @param dataAccessObject the DAO for which the loader was created. Must be a {@link GenericMockedDAO}
     * @param sourceRecordTodataObjConversionFn a method to get the data object for an id
     * @param idsToFilterIn the ids of the data objects to load. If null or empty all data objects are loaded
     */
    public MockedDefaultDataObjectLoaderImpl(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<Long, MockedDO> sourceRecordTodataObjConversionFn, Set<Long> idsToFilterIn)
    {
        super(dataAccessObject, sourceRecordTodataObjConversionFn, idsToFilterIn);
    }

    /**
     * Implements {@link InitializingBean#afterPropertiesSet()}. Does nothing in this implementation.
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {

    }

    /**
     * Gets the id of the next data object to load. These are the ids to filter for if there are any. Otherwise
     * these are the ids of all data objects in the mocked storage at the time this method is called first.
     * 
     * @return the id of the next data object to load, or null if there are no more ids
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Long getNextSourceRecord()
    {
        if (this.sourceRecordIterator == null)
        {
            Collection<Long> ids = (this.idsToFilterIn.size() > 0) ? this.idsToFilterIn : ((GenericMockedDAO<MockedDO>) this.dataAccessObject).getDataObjectIds();

            this.sourceRecordIterator = ids.iterator();
        }

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
