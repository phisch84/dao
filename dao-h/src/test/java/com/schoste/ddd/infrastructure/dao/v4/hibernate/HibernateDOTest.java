package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

import com.schoste.ddd.testing.v1.GenericObjectTest;

/**
 * Explicitly tests the HibernateDO class and implicitly test the DataObject class.
 * 
 * HibernateDO instances are equal if their id and their time stamps match.
 * Data of the instances may be different.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class HibernateDOTest extends GenericObjectTest<HibernateDO>
{

	/**
	 * Returns sets of HibernateDO instances which are expected to be equal to each other:
	 * "sameObjects" (the same instance twice) and "equalObjects" (instances with the same id and time stamps,
	 * one of them with a different example string property).
	 *
	 * @return the sets of equal objects by name
	 */
	@Override
	protected Map<String, Collection<HibernateDO>> getEqualObjects()
	{
		Map<String, Collection<HibernateDO>> equalObjectsSet = new HashMap<String, Collection<HibernateDO>>();
		Collection<HibernateDO> sameObjects = new ArrayList<HibernateDO>();
		HibernateDO sameObject = new HibernateDO();
		sameObjects.add(sameObject);
		sameObjects.add(sameObject);

		Collection<HibernateDO> equalObjects = new ArrayList<HibernateDO>();
		
		// Objects with same data
		HibernateDO equalObject1 = new HibernateDO();
		HibernateDO equalObject2 = new HibernateDO();
		
		// Object with different data
		HibernateDO equalObject3 = new HibernateDO();

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
	 * Returns sets of HibernateDO instances which are expected to differ from each other:
	 * "notEqualObjectsSameTSDifferentId" (same time stamps, different ids) and
	 * "notEqualObjectsSameIdDifferentTS" (same id, different time stamps).
	 *
	 * @return the sets of not equal objects by name
	 */
	@Override
	protected Map<String, Collection<HibernateDO>> getNotEqualObjects()
	{
		Map<String, Collection<HibernateDO>> notEqualObjectsSet = new HashMap<String, Collection<HibernateDO>>();
		Collection<HibernateDO> notEqualObjectsSameTSDifferentId = new ArrayList<HibernateDO>();
		Collection<HibernateDO> notEqualObjectsSameIdDifferentTS = new ArrayList<HibernateDO>();

		// Objects with same data
		HibernateDO notEqualObject1 = new HibernateDO();
		HibernateDO notEqualObject2 = new HibernateDO();
		
		// Object with different data
		HibernateDO notEqualObject3 = new HibernateDO();

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
		HibernateDO notEqualObject4 = new HibernateDO();
		HibernateDO notEqualObject5 = new HibernateDO();
		HibernateDO notEqualObject6 = new HibernateDO();

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
