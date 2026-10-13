package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericRecentDataObjectLoader;

@Component
public class MockedRecentDataObjectLoaderImpl extends GenericRecentDataObjectLoader<Long, Long, MockedDO> implements InitializingBean
{
    public MockedRecentDataObjectLoaderImpl(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<Long, MockedDO> sourceRecordToDataObjConversionFn, long lastModTS, boolean skipDeleted) throws IllegalArgumentException
    {
        super(dataAccessObject, sourceRecordToDataObjConversionFn, lastModTS, skipDeleted);
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
        return -1L;
    }

    @Override
    public void close() 
    {

    }
}
