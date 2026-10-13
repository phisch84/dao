package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDefaultDataObjectLoader;

/**
 * Mock implementation of {@link GenericDefaultDataObjectLoader} for unit testing
 */
@Component
public class MockedDefaultDataObjectLoaderImpl extends GenericDefaultDataObjectLoader<Long, Long, MockedDO> implements InitializingBean
{
    protected Iterator<Long> filterInIterator;

    /**
     * Creates a new instance of the loader
     * 
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecordToDataObjConversionFn UNUSED
     */
    public MockedDefaultDataObjectLoaderImpl(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<Long, MockedDO> sourceRecordTodataObjConversionFn)
    {
        super(dataAccessObject, sourceRecordTodataObjConversionFn);

        this.filterInIterator = this.idsToFilterIn.iterator();
    }

    /**
     * Creates a new instance of the loader
     * 
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecordToDataObjConversionFn UNUSED
     */
    public MockedDefaultDataObjectLoaderImpl(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<Long, MockedDO> sourceRecordTodataObjConversionFn, Set<Long> idsToFilterIn)
    {
        super(dataAccessObject, sourceRecordTodataObjConversionFn, idsToFilterIn);

        this.filterInIterator = this.idsToFilterIn.iterator();
    }

    /**
     * {@inhertDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {

    }

    @Override
    protected Long getNextSourceRecord() 
    {
        if ((this.idsToFilterIn.size() > 0) && this.filterInIterator.hasNext()) return this.filterInIterator.next();

        if (this.idsToFilterIn.size() < 1) return -1L;

        return null;
    }

    @Override
    public void close()
    {

    }
}
