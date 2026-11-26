package com.schoste.ddd.infrastructure.dao.v4.serialization;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

/**
 * Example data object used in unit testing of the SerializationDAOImpl implementation
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class SerializationDO extends DataObject<Long>
{
	private static final long serialVersionUID = -6293383128445439392L;

	private String exampleStringProperty;

	/**
	 * Gets a text
	 * @return a text
	 */
	public String getExampleStringProperty() 
	{
		return exampleStringProperty;
	}

	/**
	 * Sets a text
	 * @param exampleStringProperty the text to set
	 */
	public void setExampleStringProperty(String exampleStringProperty) 
	{
		this.exampleStringProperty = exampleStringProperty;
	}
}
