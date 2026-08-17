package com.schoste.ddd.infrastructure.dal.v2.services.mocked;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;

import com.schoste.ddd.infrastructure.dal.v2.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericLazyLoader;

/**
 * Mock implementation of {@link LazyLoader} for unit testing
 */
public class MockedLazyLoaderImpl extends GenericLazyLoader<Integer, MockedDO> implements InitializingBean
{
    protected Collection<Integer> mockedDataSource;
    protected Iterator<Integer> mdsIterator;

    public MockedLazyLoaderImpl(Function<Integer, MockedDO> sourceRecordTodataObjConversionFn, Collection<Integer> mockedDataSource)
    {
        super(sourceRecordTodataObjConversionFn);

        if (mockedDataSource == null) throw new IllegalArgumentException();

        this.mockedDataSource = mockedDataSource;
        this.mdsIterator = this.mockedDataSource.iterator();
    }

    @Override
    public boolean tryAdvance(Consumer<? super MockedDO> action) 
    {
        if (!this.mdsIterator.hasNext()) return false;

        Integer nextDataObjId = this.mdsIterator.next();
        MockedDO nextDataObj = this.sourceRecordToDataObjConversionFn.apply(nextDataObjId);

        action.accept(nextDataObj);

        return true;
    }

    @Override
    public Spliterator<MockedDO> trySplit() 
    {
        return null;
    }

    @Override
    public long estimateSize() 
    {
        return this.mockedDataSource.size();
    }

    @Override
    public int characteristics() 
    {
        return Spliterator.IMMUTABLE;
    }

    @Override
    public void close()
    {
        return;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {

    }
}
