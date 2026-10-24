package com.schoste.ddd.infrastructure.dao.v4.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines the method which should be used to get the value which should be set
 * during model conversion. The annotated method (setter method) must accept only
 * one parameter. This one parameter will be obtained by the referred method (getter method).
 *
 * This library only defines the annotation. It is evaluated by the model converter
 * that performs the conversion, which is not part of this library (e.g. the converter of a repository in the domain layer).
 * It is defined here because it annotates data objects, which belong to the infrastructure layer; the infrastructure layer
 * must not depend on the layers above it.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AutoSet
{
	/**
	 * Defines the class which defines the method to use to get the value.
	 * If this value is not defined (it is {@link Object} by default), the converter will try to find a class named {@link AutoSet#className()}.
	 * If neither {@link AutoSet#clazz()} nor {@link AutoSet#className()} are defined,
	 * the converter will search the conversion methods in the instance of the source model.
	 *
	 * @return a class, or {@link Object} if not defined
	 */
	Class<?> clazz() default Object.class;

	/**
	 * Defines the name of the class which defines the method to use to get the value.
	 * If {@link AutoSet#clazz()} is defined (i.e. it is not {@link Object}), this property is ignored.
	 *
	 * @return a full class name (with package name), or an empty string if not defined
	 */
	String className() default "";

	/**
	 * Defines the name of the method to use to get the value (getter method).
	 * If this property is not defined, the converter will try to guess the conversion method itself.
	 *
	 * @return a method name, or an empty string if not defined
	 */
	String methodName() default "";
}
