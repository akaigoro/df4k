/**
 * Provides the core classes for dataflow and actor programming.
 * Dataflow programming model has many flavors.
 * This implementation has following features:
 *
 *
 * - computational dataflow graph is represented as a set of nodes
 *
 *
 * - nodes are created dynamically
 *
 *
 * - nodes have pins to accept tokens (messages).
 *
 *
 * - the framework activates node after all the pins carry incoming tokens
 *
 *
 * - nodes are executed by an executor determined at the time of node creation.
 * Executor can be set directly as a constructor argument, or taken implicitly
 * from thread context.
 *
 *
 * - nodes are subclasses of abstract class [org.df4k.core.Actor].
 * User have to override  method to handle tokens
 * [org.df4k.core.Actor.act]
 *
 *
 * - pins may be of several predefined types, and user can create specific pin types
 * by subclassing class [org.df4k.core.Pin]
 */
package org.df4k.core