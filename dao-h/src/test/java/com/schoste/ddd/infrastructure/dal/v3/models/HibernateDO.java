package com.schoste.ddd.infrastructure.dal.v3.models;

/**
 * Example Hibernate entity (data object) used in unit testing of the HibernateDAOImpl implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class HibernateDO extends GenericDataObject<Long>
{
	private static final long serialVersionUID = -6293303128845439392L;

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
