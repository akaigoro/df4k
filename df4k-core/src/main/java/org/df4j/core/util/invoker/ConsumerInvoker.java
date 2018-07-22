package org.df4j.core.util.invoker;

import java.util.function.Consumer;

public class ConsumerInvoker<U,R> extends AbstractInvoker<Consumer<U>, R> {

    public ConsumerInvoker(Consumer<U> consumer) {
        super(consumer);
    }

    public R apply(Object... args) {
        assert args.length == 1;
        function.accept((U) args[0]);
        return null;
    }

}
