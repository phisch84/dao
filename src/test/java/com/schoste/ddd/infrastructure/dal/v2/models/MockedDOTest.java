package com.schoste.ddd.infrastructure.dal.v2.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.schoste.ddd.infrastructure.dal.v2.models.MockedDO;
import com.schoste.ddd.testing.v1.GenericObjectTest;

/**
 * Explicitly tests the MockedDO class and implicitly test the GenericDataObject class.
 * 
 * MockedDO instances are equal if their id and their time stamps match.
 * Data of the instances may be different.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class MockedDOTest extends GenericObjectTest<MockedDO>
{
	@Override
	protected Map<String, Collection<MockedDO>> getEqualObjects() 
	{
		Map<String, Collection<MockedDO>> equalObjectsSet = new HashMap<String, Collection<MockedDO>>();
		Collection<MockedDO> sameObjects = new ArrayList<MockedDO>();
		MockedDO sameObject = new MockedDO();
		sameObjects.add(sameObject);
		sameObjects.add(sameObject);

		Collection<MockedDO> equalObjects = new ArrayList<MockedDO>();
		
		// Objects with same data
		MockedDO equalObject1 = new MockedDO();
		MockedDO equalObject2 = new MockedDO();
		
		// Object with different data
		MockedDO equalObject3 = new MockedDO();

		equalObject1.setId(1);
		equalObject1.setCreatedTimeStamp(1);
		equalObject1.setModifiedTimeStamp(1);
		equalObject1.setExampleStringProperty("HELLO");

		equalObject2.setId(1);
		equalObject2.setCreatedTimeStamp(1);
		equalObject2.setModifiedTimeStamp(1);
		equalObject2.setExampleStringProperty("HELLO");

		equalObject3.setId(1);
		equalObject3.setCreatedTimeStamp(1);
		equalObject3.setModifiedTimeStamp(1);
		equalObject3.setExampleStringProperty("WORLD");

		equalObjects.add(equalObject1);
		equalObjects.add(equalObject2);
		equalObjects.add(equalObject3);


		equalObjectsSet.put("sameObjects", sameObjects);
		equalObjectsSet.put("equalObjects", equalObjects);

		return equalObjectsSet;
	}

	@Override
	protected Map<String, Collection<MockedDO>> getNotEqualObjects() 
	{
		Map<String, Collection<MockedDO>> notEqualObjectsSet = new HashMap<String, Collection<MockedDO>>();
		Collection<MockedDO> notEqualObjectsSameTSDifferentId = new ArrayList<MockedDO>();
		Collection<MockedDO> notEqualObjectsSameIdDifferentTS = new ArrayList<MockedDO>();

		// Objects with same data
		MockedDO notEqualObject1 = new MockedDO();
		MockedDO notEqualObject2 = new MockedDO();
		
		// Object with different data
		MockedDO notEqualObject3 = new MockedDO();

		notEqualObject1.setId(1);
		notEqualObject1.setCreatedTimeStamp(1);
		notEqualObject1.setModifiedTimeStamp(1);
		notEqualObject1.setExampleStringProperty("HELLO");

		notEqualObject2.setId(2);
		notEqualObject2.setCreatedTimeStamp(1);
		notEqualObject2.setModifiedTimeStamp(1);
		notEqualObject2.setExampleStringProperty("HELLO");

		notEqualObject3.setId(3);
		notEqualObject3.setCreatedTimeStamp(1);
		notEqualObject3.setModifiedTimeStamp(1);
		notEqualObject3.setExampleStringProperty("WORLD");

		notEqualObjectsSameTSDifferentId.add(notEqualObject1);
		notEqualObjectsSameTSDifferentId.add(notEqualObject2);
		notEqualObjectsSameTSDifferentId.add(notEqualObject3);
		
		notEqualObjectsSet.put("notEqualObjectsSameTSDifferentId", notEqualObjectsSameTSDifferentId);

		// Objects with same id but different time stamps
		MockedDO notEqualObject4 = new MockedDO();
		MockedDO notEqualObject5 = new MockedDO();
		MockedDO notEqualObject6 = new MockedDO();

		notEqualObject4.setId(4);
		notEqualObject4.setCreatedTimeStamp(1);
		notEqualObject4.setModifiedTimeStamp(1);
		notEqualObject4.setExampleStringProperty("HELLO");

		notEqualObject5.setId(4);
		notEqualObject5.setCreatedTimeStamp(2);
		notEqualObject5.setModifiedTimeStamp(3);
		notEqualObject5.setExampleStringProperty("HELLO");

		notEqualObject6.setId(4);
		notEqualObject6.setCreatedTimeStamp(2);
		notEqualObject6.setModifiedTimeStamp(4);
		notEqualObject6.setExampleStringProperty("WORLD");

		notEqualObjectsSameIdDifferentTS.add(notEqualObject4);
		notEqualObjectsSameIdDifferentTS.add(notEqualObject5);
		notEqualObjectsSameIdDifferentTS.add(notEqualObject6);
		
		notEqualObjectsSet.put("notEqualObjectsSameIdDifferentTS", notEqualObjectsSameIdDifferentTS);

		return notEqualObjectsSet;
	}
}
