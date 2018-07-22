/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4k.core.node.messagestream

import org.df4k.core.connector.messagescalar.SimpleSubscription
import org.df4k.core.connector.messagestream.StreamSubscriber
import org.df4k.core.connector.messagestream.StreamInput

/**
 * A dataflow Actor with one predefined input stream port.
 * It mimics the Actors described by Carl Hewitt.
 * This class, however, still can have other (named) ports.
 * @param <M> the type of messages, accepted via predefined port.
</M> */
abstract class Actor1<M> : Actor(), StreamSubscriber<M> {
    protected val mainInput = StreamInput<M>(this)

    val isClosed: Boolean
        get() = mainInput.isClosed

    @Synchronized
    override fun onSubscribe(subscription: SimpleSubscription) {
        mainInput.onSubscribe(subscription)
    }

    override fun post(m: M) {
        mainInput.post(m)
    }

    override fun postFailure(ex: Throwable) {
        mainInput.postFailure(ex)
    }

    /**
     * processes closing signal
     * @throws Exception
     */
    override fun complete() {
        mainInput.complete()
    }
}
