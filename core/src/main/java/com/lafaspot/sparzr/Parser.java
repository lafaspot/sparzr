package com.lafaspot.sparzr;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A parser encompassing the logic required to parse schema.org data within a given string. A list of {@code Listener} can be registered with the
 * parser to listen for various events.
 *
 * @author akajla
 *
 */
public class Parser {

    /**
     * Create a schema.org parser object with empty list of listeners. And a default implementation of the handler, InternalSaxHandler.
     */
    public Parser() {
        listeners = new ArrayList<Listener>();
        handler = new InternalSaxHandler(listeners);
    }

    /**
     * If you have a custom handler, then use this constructor to drive the handling of the tags in the document. This way you can have more control
     * of the parsing.
     *
     * @param theHandler
     *            The <b>Nonnull</b> handler, that would be responsible for handling the document and calling back the listener.
     */
    public Parser(final DefaultHandler theHandler) {
        listeners = new ArrayList<Listener>();
        handler = theHandler;
    }

    /**
     * Register a new listener with the parser.
     *
     * @param listener Listener to register
     */
    public void registerListener(final Listener listener) {
        listeners.add(listener);
    }

    /**
     * Parse given string and issue event callbacks for registered listeners using logic defined in {@link InternalSaxHandler}.
     *
     * @param html
     *            string to parse for content
     * @throws IOException
     *             when cannot create stream from input
     * @throws SAXException
     *             is thrown due to parsing issues
     */
    public void parse(final String html) throws IOException,
            SAXException {
        final org.ccil.cowan.tagsoup.Parser parser = new org.ccil.cowan.tagsoup.Parser();
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);

        final InputSource htmlInputSource = new InputSource(new StringReader(html));
        parser.parse(htmlInputSource);
    }

    /**
     * This method returns the list of the registered listeners for this parser.
     *
     * @return the list of the registered listeners.
     */
    public List<Listener> getListeners() {
        return listeners;
    }

    /**
     * Listeners that will be called back by the Handler.
     */
    private final List<Listener> listeners;

    /**
     * The handler that will be called by the internal parser to handle the document tags. By default it uses the InternalSaxHandler, if not provided.
     * The user can have their own handler.
     */
    private final DefaultHandler handler;
}
