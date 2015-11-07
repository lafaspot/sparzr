package com.lafaspot.sparzr;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Defines the different events that can be triggered when looking for Schema.org data using {@code Parser}.
 *
 * @author akajla
 *
 */
public interface Listener {

    /**
     * Notifies listener that parsing has begun.
     */
    void startParsing();

    /**
     * Notifies listener that parsing has finished.
     *
     * @param numItemtypes total number of itemtypes found
     */
    void endParsing(int numItemtypes);

    /**
     * Notifies listener of every new itemtype found during parsing.
     *
     * @param itemtype Type of item found
     * @param format Format of item (Microdata, Json-ld etc)
     */
    void foundItemtype(final String itemtype, final Format format);

    /**
     * Notifies listener of each new top-level item found during parsing.
     *
     * @param item Full item in json format
     */
    void foundItem(final JsonNode item);

    /**
     * Notify if the corresponding parsing is finished.
     *
     * @return the parsing finished boolean value.
     */
    boolean isParsingFinished();
}
