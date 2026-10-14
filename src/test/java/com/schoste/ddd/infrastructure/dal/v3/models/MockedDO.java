package com.schoste.ddd.infrastructure.dal.v3.models;

/**
 * Example data object used in unit testing of the {@link MockedDAOImpl} implementation.
 * Uses a {@link Long} as identifier {@link GenericDataObject#getId()}.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class MockedDO  extends GenericDataObject<Long>
{
	private static final long serialVersionUID = 6293303128845439392L;

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
