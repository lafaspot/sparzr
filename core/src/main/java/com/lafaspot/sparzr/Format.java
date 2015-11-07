package com.lafaspot.sparzr;

/**
 * Formats that can be parsed for schema.org annotations.
 * 
 * @author akajla
 * 
 */
public enum Format {
    /**
     * Schema.org annotations defined in json within script tags.
     */
    JSONLD,

    /**
     * Schema.org annotations defined within tag attributes.
     */
    MICRODATA,

    /**
     * Schema.org annotations defined within tag attributes (similar to Microdata).
     */
    RDFA,

    /**
     * No schema.org annotation.
     */
    NONE;
}
