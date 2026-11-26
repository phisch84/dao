/**
 * Annotations which mark data objects for code in higher layers, e.g. a repository which converts data objects into domain
 * entities and back ({@link com.schoste.ddd.infrastructure.dao.v4.annotations.AutoSet}). The DAOs of this library don't
 * evaluate them; a DAO user who doesn't use such a converter doesn't need them.
 *
 * The annotations are part of this library, although they are evaluated elsewhere, because they annotate data objects, which
 * belong to the infrastructure layer: the infrastructure layer must never depend on a layer above it (e.g. the domain layer),
 * so data objects can't use annotations defined there.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.infrastructure.dao.v4.annotations;
