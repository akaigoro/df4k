package org.df4k.core.connector.reactivestream

interface ReactivePublisher<M> {

    fun <S : ReactiveSubscriber<in M>> subscribe(ReactiveSubscriber: S): S

}
