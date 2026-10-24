package com.schoste.ddd.infrastructure.dao.v4.inmemory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

import com.schoste.ddd.testing.v1.GenericObjectTest;

/**
 * Explicitly tests the InMemoryDO class and implicitly test the DataObject class.
 * 
 * InMemoryDO instances are equal if their id and their time stamps match.
 * Data of the instances may be different.
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class InMemoryDOTest extends GenericObjectTest<InMemoryDO>
{
	/**
	 * {@inheritDoc}
	 * @see com.schoste.ddd.testing.v1.GenericObjectTest#getEqualObjects()
	 */
	@Override
	protected Map<String, Collection<InMemoryDO>> getEqualObjects() 
	{
		Map<String, Collection<InMemoryDO>> equalObjectsSet = new HashMap<String, Collection<InMemoryDO>>();
		Collection<InMemoryDO> sameObjects = new ArrayList<InMemoryDO>();
		InMemoryDO sameObject = new InMemoryDO();
		sameObjects.add(sameObject);
		sameObjects.add(sameObject);

		Collection<InMemoryDO> equalObjects = new ArrayList<InMemoryDO>();
		
		// Objects with same data
		InMemoryDO equalObject1 = new InMemoryDO();
		InMemoryDO equalObject2 = new InMemoryDO();
		
		// Object with different data
		InMemoryDO equalObject3 = new InMemoryDO();

		equalObject1.setId(1L);
		equalObject1.setCreatedTimestamp(1);
		equalObject1.setModifiedTimestamp(1);
		equalObject1.setExampleStringProperty("HELLO");

		equalObject2.setId(1L);
		equalObject2.setCreatedTimestamp(1);
		equalObject2.setModifiedTimestamp(1);
		equalObject2.setExampleStringProperty("HELLO");

		equalObject3.setId(1L);
		equalObject3.setCreatedTimestamp(1);
		equalObject3.setModifiedTimestamp(1);
		equalObject3.setExampleStringProperty("WORLD");

		equalObjects.add(equalObject1);
		equalObjects.add(equalObject2);
		equalObjects.add(equalObject3);


		equalObjectsSet.put("sameObjects", sameObjects);
		equalObjectsSet.put("equalObjects", equalObjects);

		return equalObjectsSet;
	}

	/**
	 * {@inheritDoc}
	 * @see com.schoste.ddd.testing.v1.GenericObjectTest#getNotEqualObjects()
	 */
	@Override
	protected Map<String, Collection<InMemoryDO>> getNotEqualObjects() 
	{
		Map<String, Collection<InMemoryDO>> notEqualObjectsSet = new HashMap<String, Collection<InMemoryDO>>();
		Collection<InMemoryDO> notEqualObjectsSameTSDifferentId = new ArrayList<InMemoryDO>();
		Collection<InMemoryDO> notEqualObjectsSameIdDifferentTS = new ArrayList<InMemoryDO>();

		// Objects with same data
		InMemoryDO notEqualObject1 = new InMemoryDO();
		InMemoryDO notEqualObject2 = new InMemoryDO();
		
		// Object with different data
		InMemoryDO notEqualObject3 = new InMemoryDO();

		notEqualObject1.setId(1L);
		notEqualObject1.setCreatedTimestamp(1);
		notEqualObject1.setModifiedTimestamp(1);
		notEqualObject1.setExampleStringProperty("HELLO");

		notEqualObject2.setId(2L);
		notEqualObject2.setCreatedTimestamp(1);
		notEqualObject2.setModifiedTimestamp(1);
		notEqualObject2.setExampleStringProperty("HELLO");

		notEqualObject3.setId(3L);
		notEqualObject3.setCreatedTimestamp(1);
		notEqualObject3.setModifiedTimestamp(1);
		notEqualObject3.setExampleStringProperty("WORLD");

		notEqualObjectsSameTSDifferentId.add(notEqualObject1);
		notEqualObjectsSameTSDifferentId.add(notEqualObject2);
		notEqualObjectsSameTSDifferentId.add(notEqualObject3);
		
		notEqualObjectsSet.put("notEqualObjectsSameTSDifferentId", notEqualObjectsSameTSDifferentId);

		// Objects with same id but different time stamps
		InMemoryDO notEqualObject4 = new InMemoryDO();
		InMemoryDO notEqualObject5 = new InMemoryDO();
		InMemoryDO notEqualObject6 = new InMemoryDO();

		notEqualObject4.setId(4L);
		notEqualObject4.setCreatedTimestamp(1);
		notEqualObject4.setModifiedTimestamp(1);
		notEqualObject4.setExampleStringProperty("HELLO");

		notEqualObject5.setId(4L);
		notEqualObject5.setCreatedTimestamp(2);
		notEqualObject5.setModifiedTimestamp(3);
		notEqualObject5.setExampleStringProperty("HELLO");

		notEqualObject6.setId(4L);
		notEqualObject6.setCreatedTimestamp(2);
		notEqualObject6.setModifiedTimestamp(4);
		notEqualObject6.setExampleStringProperty("WORLD");

		notEqualObjectsSameIdDifferentTS.add(notEqualObject4);
		notEqualObjectsSameIdDifferentTS.add(notEqualObject5);
		notEqualObjectsSameIdDifferentTS.add(notEqualObject6);
		
		notEqualObjectsSet.put("notEqualObjectsSameIdDifferentTS", notEqualObjectsSameIdDifferentTS);

		return notEqualObjectsSet;
	}
}
