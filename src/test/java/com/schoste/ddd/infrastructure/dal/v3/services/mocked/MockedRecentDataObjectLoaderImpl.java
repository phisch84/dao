package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericRecentDataObjectLoader;

@Component
public class MockedRecentDataObjectLoaderImpl extends GenericRecentDataObjectLoader<Integer, Integer, MockedDO> implements InitializingBean
{
    public MockedRecentDataObjectLoaderImpl(GenericDataAccessObject<Integer, MockedDO> dataAccessObject, Function<Integer, MockedDO> sourceRecordToDataObjConversionFn, long lastModTS, boolean skipDeleted) throws IllegalArgumentException
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
    protected Integer getNextSourceRecord() 
    {
        return -1;
    }

    @Override
    public void close() 
    {

    }
}
