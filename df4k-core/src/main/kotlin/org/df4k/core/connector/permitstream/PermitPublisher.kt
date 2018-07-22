/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 * *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 * *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http:></http:>//creativecommons.org/publicdomain/zero/1.0/>.*
 */

package org.df4k.core.connector.permitstream

/**
 * A [PermitPublisher] is a provider of a potentially unbounded number of permits
 *
 *
 */
interface PermitPublisher {

    /**
     * was: onSendTo
     *
     * @param subscriber
     * the [PermitSubscriber] that will consume signals from this [PermitPublisher]
     */
    fun subscribe(subscriber: PermitSubscriber)
}
