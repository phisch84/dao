package com.schoste.ddd.infrastructure.dao.v4.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a setter of a data object which a model converter calls with the value of a getter of the source object, e.g. when
 * a repository converts an entity into its data object.
 *
 * This library only defines the annotation; it is evaluated by the converter which performs the conversion, which is not part
 * of this library. It is defined here because it annotates data objects, which belong to the infrastructure layer; the
 * infrastructure layer must not depend on the layers above it. Classes of the domain layer and above (e.g. entities) use
 * {@code com.schoste.ddd.domain.v3.annotations.AutoSet} of the {@code domain-repository3} library instead, which has the
 * same attributes.
 *
 * The converter of {@code domain-repository3} ({@code com.schoste.ddd.domain.v3.services.standard.AutoObjectConverterImpl})
 * evaluates both annotations the same way. The setter must be public and take exactly one parameter; it gets the value
 * returned by a public getter without parameters:
 * <ul>
 * <li>the getter's name is {@link AutoSet#methodName()}, or, if that isn't defined, the setter's name with "set" replaced
 * by "get" (e.g. {@code getName()} for {@code setName(String)}; a setter whose name doesn't start with "set" uses its own
 * name). Getters starting with "is" aren't found automatically; name them by {@link AutoSet#methodName()};</li>
 * <li>the getter is called on the source object (e.g. the entity), unless {@link AutoSet#clazz()} or
 * {@link AutoSet#className()} names another class: then it is called on an instance of that class
 * (e.g. a class which provides a default value).</li>
 * </ul>
 * If the getter doesn't exist, the conversion fails. Setters which aren't annotated aren't called by the converter.
 *
 * Example: the repository sets the data object's name and email address from the entity's {@code getName()} and
 * {@code getEmail()}.
 * <pre>
 * public class CustomerDO extends DataObject&lt;Long&gt;
 * {
 *     &#64;AutoSet
 *     public void setName(String name) { ... }
 *
 *     &#64;AutoSet(methodName = "getEmail")
 *     public void setEmailAddress(String emailAddress) { ... }
 * }
 * </pre>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AutoSet
{
	/**
	 * Defines the class on whose instance the getter is called, instead of the source object.
	 * The instance is created by the converter (the converter of {@code domain-repository3} creates it once per converter and
	 * pair of source and destination class, and reuses it for all their conversions).
	 *
	 * @return the class, or {@link Object} if not defined
	 */
	Class<?> clazz() default Object.class;

	/**
	 * Defines the full name (with package) of the class on whose instance the getter is called, instead of the source object.
	 * The instance is created like the one of {@link AutoSet#clazz()}.
	 * Ignored if {@link AutoSet#clazz()} is defined.
	 *
	 * @return the full class name, or an empty string if not defined
	 */
	String className() default "";

	/**
	 * Defines the name of the getter. If it isn't defined, the getter's name is the setter's name with "set" replaced by "get".
	 *
	 * @return the getter's name, or an empty string if not defined
	 */
	String methodName() default "";
}
