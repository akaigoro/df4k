package org.df4j.examples.tutorial;

import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.node.Action;
import org.df4j.core.node.messagestream.Actor;
import org.junit.Test;

public class HelloWorld {
    /**
     * collects strings
     * prints collected strings when argument is an empty string
     */
    class Collector extends Actor {

        StreamInput<String> input = new StreamInput<String>(this); // actor's parameter
        StringBuilder sb = new StringBuilder();

        @Action
        protected void act(String message) {
            // empty string indicates the end of stream
            // nulls are not allowed
            if (message == null) {
                System.out.println(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(message);
                sb.append(" ");
            }
        }
    }

    @Test
    public void test() {
        Collector coll = new Collector();
        coll.start();
        coll.input.post("Hello");
        coll.input.post("World");
        coll.input.complete();
    }

}
