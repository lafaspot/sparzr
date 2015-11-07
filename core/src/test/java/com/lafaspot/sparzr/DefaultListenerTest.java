package com.lafaspot.sparzr;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Unit tests for {@link DefaultListener}.
 *
 * @author akajla
 *
 */
public class DefaultListenerTest {

    /**
     * Test basic functionality of {@link DefaulListener}.
     */
    @Test
    public void testListener() {
        final DefaultListener listener = new DefaultListener();
        listener.startParsing();
        listener.foundItem(new TextNode("blah"));
        listener.foundItemtype("HotelReservation", Format.MICRODATA);
        listener.endParsing(2);
        Assert.assertEquals(listener.totalItemtypes(), 2);
        Assert.assertEquals(listener.getItems().toString(), "[\"blah\"]");
    }
}
