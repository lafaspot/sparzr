package com.lafaspot.sparzr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * A generic implementation of {@code Listener} that simply creates an array of Schema.org items found and keeps track of the total number of items
 * found.
 *
 * @author akajla
 *
 */
public class DefaultListener implements Listener {

    /**
     * Create a default listener. It creates an empty array of JsonNode. The corresponding handler stuffs the data in this array. This array is
     * returned at the end of the parsing when the client asks for it.
     */
    public DefaultListener() {
        itemArray = JsonNodeFactory.instance.arrayNode();
    }

    @Override
    public void startParsing() {
        logger.info("Parsing started");
    }

    @Override
    public void endParsing(final int numItemtypes) {
        totalItemtypes = numItemtypes;
        parsingFinished = true;
        logger.info("Parsing finished. Num itemtypes found: {}", totalItemtypes);
    }

    @Override
    public void foundItemtype(final String itemtype, final Format format) {
        logger.info("ItemType: {}", itemtype);
    }

    @Override
    public void foundItem(final JsonNode item) {
        itemArray.add(item);
    }

    /**
     * Ideally the client should wait for this parsing to be finished. And then only pull the result data.
     */
    @Override
    public boolean isParsingFinished() {
        return parsingFinished;
    }

    /**
     * Returns the array of found schema.org extractions. Usually this is the array of size one. This method should be called once the parser has
     * parsed the document. If called before the parsing then, it would return the types encountered so far. Returns the ArrayNode with an array of
     * schema.org extractions while maintaining the hierarchy. For example if a document has two (one microdata and one JSON-LD) schema.org entities
     * in their entirety, then at the end of the entire parsing this method shall return two schema.org extractions.
     *
     *
     * An example could be:
     *    [
     *       {
     *          "@context": "http://schema.org",
     *          "@type": "FoodEstablishmentReservation",
     *          "reservationNumber": "OT12345",
     *          "reservationStatus": "http://schema.org/Confirmed",
     *          "underName": {
     *             "@type": "Person",
     *             "name": "John Smith"
     *          },
     *          "reservationFor": {
     *               "@type": "FoodEstablishment",
     *               "name": "Wagamama",
     *               "address": {
     *                   "@type": "PostalAddress",
     *                   "streetAddress": "1 Tavistock Street",
     *                   "addressLocality": "London",
     *                   "addressRegion": "Greater London",
     *                   "postalCode": "WC2E 7PG",
     *                   "addressCountry": "United Kingdom"
     *               }
     *          },
     *          "startTime": "2017-04-10T08:00:00+00:00",
     *          "partySize": "2"
     *       }
     *    ]
     *
     * @return itemArray containing schema.org extractions.
     */
    public ArrayNode getItems() {
        return itemArray;
    }

    /**
     * Return the number of totalItemTypes encountered so far. If this method is called before the parsing was finished, it does not guarantee that
     * the result would be legal (or at least complete).
     *
     * @return count of item types.
     */
    public int totalItemtypes() {
        return totalItemtypes;
    }

    /**
     * Total items found by schema.org parser.
     */
    private int totalItemtypes = 0;

    /**
     * Array of item objects found by schema.org parser.
     */
    private final ArrayNode itemArray;

    /**
     * State if the corresponding parsing is finished. Typically it should be set to true at the end of endParsing.
     */
    private boolean parsingFinished;

    /**
     * The Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultListener.class);
}
