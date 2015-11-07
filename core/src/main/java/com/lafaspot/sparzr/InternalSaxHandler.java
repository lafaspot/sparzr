package com.lafaspot.sparzr;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Encompasses all SAX logic required for parsing schema.org annotations. Currently supports Json-ld and Microdata formats with all data normalized to
 * json.
 *
 * Microdata to json conversion based on <a href="http://www.w3.org/TR/microdata/#json">W3 spec</a>
 *
 * @author akajla
 *
 */
public class InternalSaxHandler extends DefaultHandler {

    /**
     * Script type defining json-ld format.
     */
    private static final String JSONLD_SCRIPT_TYPE = "application/ld+json";

    /**
     * script tag.
     */
    private static final String SCRIPT_TAG = "script";

    /**
     * Microdata itemscope attribute.
     */
    private static final String ITEMSCOPE_ATTRIBUTE = "itemscope";

    /**
     * Microdata itemtype attribute.
     */
    private static final String ITEMTYPE_ATTRIBUTE = "itemtype";

    /**
     * Microdata itemprop attribute.
     */
    private static final String ITEMPROP_ATTRIBUTE = "itemprop";

    /**
     * Generic "type" attribute.
     */
    private static final String TYPE_ATTRIBUTE = "type";

    /**
     * List of references to items for traversing item hierarchy.
     */
    private final LinkedList<ObjectNode> items;

    /**
     * Stack to keep track of item state for every tag.
     */
    private final Stack<Format> tagTypes;

    /**
     * Currently registered list of listeners called during parsing.
     */
    private final List<Listener> listeners;

    /**
     * Indicate whether current tag contains Json-ld data.
     */
    private boolean containsJsonld;

    /**
     * Total items processed by parser during run.
     */
    private int totalItemtypes;

    /**
     * Buffer to use for reading string data in between tags.
     */
    private StringBuffer stringBuffer;

    /**
     * Current item property being parsed.
     */
    private String propertyName;


    /**
     * JsonNode factory to be used to create respective types of nodes.
     */
    private final JsonNodeFactory jsonNodeFactory;

    /**
     * The mapper to convert string to JSON and vice versa.
     */
    private final ObjectMapper mapper;

    /**
     * Create an object that will use provided listeners to notify caller of events.
     *
     * @param listeners
     *            Client provided list of listeners to be called on parser events
     */
    public InternalSaxHandler(final List<Listener> listeners) {
        this.items = new LinkedList<ObjectNode>();
        this.tagTypes = new Stack<Format>();
        this.listeners = listeners;
        this.containsJsonld = false;
        this.totalItemtypes = 0;
        this.stringBuffer = new StringBuffer();
        this.propertyName = null;
        this.jsonNodeFactory = JsonNodeFactory.instance;
        this.mapper = new ObjectMapper();
    }

    /**
     * Notify listeners that parsing has begun.
     */
    @Override
    public void startDocument() {
        for (final Listener listener : listeners) {
            listener.startParsing();
        }
    }

    /**
     * Notify listeners that parsing has finished.
     */
    @Override
    public void endDocument() {
        for (final Listener listener : listeners) {
            listener.endParsing(totalItemtypes);
        }
    }

    /**
     * Encompasses all logic to be executed upon discovery of start tags containing JSON-LD and Microdata schema.org.
     */
    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attrs) {
        if (SCRIPT_TAG.equalsIgnoreCase(qName) || SCRIPT_TAG.equalsIgnoreCase(localName)) {
            /* Look for JSON-LD data embedded within <script type="application/ld+json"> tag */
            final String scriptType = attrs.getValue(TYPE_ATTRIBUTE);
            if (JSONLD_SCRIPT_TYPE.equalsIgnoreCase(scriptType)) {
                containsJsonld = true;
            }
            tagTypes.push(Format.JSONLD);
        } else {
            /* Else, look for itemscope, itemtype & itemprop attrs and create json object (Microdata) */
            final String itemscopeAttr = attrs.getValue(ITEMSCOPE_ATTRIBUTE);
            final String itemtypeAttr = attrs.getValue(ITEMTYPE_ATTRIBUTE);
            final String itempropAttr = attrs.getValue(ITEMPROP_ATTRIBUTE);
            ObjectNode currentItem = null;
            if (!items.isEmpty()) {
                currentItem = items.getLast();
            }
            // Found a new item
            if (itemscopeAttr != null && itemtypeAttr != null) {
                final String[] itemtypeArray = itemtypeAttr.split("/");
                final String itemtype = itemtypeArray[itemtypeArray.length - 1];
                final ObjectNode newItem = jsonNodeFactory.objectNode();
                final ArrayNode newItemTypeArray = jsonNodeFactory.arrayNode();
                newItemTypeArray.add(new TextNode(itemtype));
                newItem.set("type", newItemTypeArray);
                foundItemtype(itemtype, Format.MICRODATA);
                tagTypes.push(Format.MICRODATA);
                // Add new item to parent item if required
                if (itempropAttr != null && currentItem != null) {
                    if (currentItem.get(itempropAttr) != null && currentItem.get(itempropAttr).isArray()) {
                        // Here we need to cast because, ObjectNode (or JsonNode) don't provide any convenience method to get an array directly.
                        // Even the convenience method of JsonObject (of gson) does the same thing.
                        ArrayNode jArrayNode = (ArrayNode) (currentItem.get(itempropAttr));
                        jArrayNode.add(newItem);
                    } else {
                        final ArrayNode itemProp = jsonNodeFactory.arrayNode();
                        itemProp.add(newItem);
                        currentItem.set(itempropAttr, itemProp);
                    }
                }
                items.add(newItem);
            } else if (itempropAttr != null && currentItem != null) {
                // Found an item property
                ArrayNode itemprop = null;
                if (currentItem.get(itempropAttr) != null && currentItem.get(itempropAttr).isArray()) {
                    itemprop = (ArrayNode) currentItem.get(itempropAttr);
                } else {
                    itemprop = jsonNodeFactory.arrayNode();
                }
                switch (localName) {
                case "a":
                case "area":
                case "link":
                    itemprop.add(new TextNode(attrs.getValue("href")));
                    currentItem.set(itempropAttr, itemprop);
                    break;
                case "img":
                    itemprop.add(new TextNode(attrs.getValue("src")));
                    currentItem.set(itempropAttr, itemprop);
                    break;
                case "time":
                    itemprop.add(new TextNode(attrs.getValue("datetime")));
                    currentItem.set(itempropAttr, itemprop);
                    break;
                case "iframe":
                case "embed":
                case "object":
                    itemprop.add(new TextNode(attrs.getValue("data")));
                    currentItem.set(itempropAttr, itemprop);
                    break;
                default:
                    final String content = attrs.getValue("content");
                    if (content != null) {
                        itemprop.add(new TextNode(content));
                        currentItem.set(itempropAttr, itemprop);
                    } else {
                        // Need to extract content in between tags
                        propertyName = itempropAttr;
                    }
                }
                tagTypes.push(Format.NONE);
            } else {
                tagTypes.push(Format.NONE);
            }
        }
    }

    /**
     * Encompasses all logic to be executed upon discovery of end tags containing JSON-LD and Microdata schema.org.
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        /* Return JSON-LD data collected from within any script tags */
        if ((SCRIPT_TAG.equalsIgnoreCase(qName) || SCRIPT_TAG.equalsIgnoreCase(localName)) && containsJsonld) {
            final String potentialJson = stringBuffer.toString();
            JsonNode element = null;
            try {
                element = mapper.readTree(potentialJson);
            } catch (JsonProcessingException e) {
                // The internal json-ld element is not well formed. So let's skip this potential json schema.org and continue with other
                // schema.org in the same html. Just clean up and move on. No need to report it. Also, if we throw the SAXException, from
                // here it will just stop processing the remaining document. Not a good idea. We still want to extract whatever it well
                // formed.
                containsJsonld = false;
                stringBuffer = new StringBuffer();
                tagTypes.pop();
                return;
            } catch (IOException e) {
                throw new SAXException("I/O failed.", e);
            }
            parseJson(element);
            for (final Listener listener : listeners) {
                listener.foundItem(element);
            }
            containsJsonld = false;
            stringBuffer = new StringBuffer();
            tagTypes.pop();
        } else {
            /* Else, return top-level items if possible (Microdata) */
            ObjectNode currentItem = null;
            if (!items.isEmpty()) {
                currentItem = items.getLast();
            }
            // Get property content from in-between tags if required
            if (propertyName != null && currentItem != null) {
                final String content = stringBuffer.toString().trim();
                if (currentItem.get(propertyName) != null && currentItem.get(propertyName).isArray()) {
                    ArrayNode jArray = (ArrayNode) currentItem.get(propertyName);
                    jArray.add(new TextNode(content));
                } else {
                    final ArrayNode itemArray = jsonNodeFactory.arrayNode();
                    itemArray.add(new TextNode(content));
                    currentItem.set(propertyName, itemArray);
                }
                propertyName = null;
                stringBuffer = new StringBuffer();
            }
            // If closing tag of an item, move up a level and return top-level item if necessary
            final Format type = tagTypes.pop();
            if (type.equals(Format.MICRODATA)) {
                items.removeLast();
                if (items.isEmpty() && currentItem != null) {
                    for (final Listener listener : listeners) {
                        listener.foundItem(currentItem);
                    }
                }
            }
        }
    }

    /**
     * Buffer characters from in-between tags.
     */
    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        if (containsJsonld || propertyName != null) {
            stringBuffer.append(ch, start, length);
        }
    }

    /**
     * Recursively parse json to find all itemtypes.
     *
     * @param element Root json element
     */
    private void parseJson(final JsonNode element) {
        if (element == null || element.isNull() || element.isValueNode() || element.isMissingNode()) {
            return;
        } else if (element.isObject()) {
            final ObjectNode object = (ObjectNode) element;
            final Iterator<Entry<String, JsonNode>> iterator = object.fields();
            while (iterator.hasNext()) {
                final Entry<String, JsonNode> entry = iterator.next();
                final String key = entry.getKey();
                final JsonNode value = entry.getValue();
                if ("@type".equalsIgnoreCase(key)) {
                    final String itemtype = value.textValue();
                    foundItemtype(itemtype, Format.JSONLD);
                } else {
                    parseJson(value);
                }
            }
        } else if (element.isArray()) {
            final ArrayNode jsonArray = (ArrayNode) element;
            final Iterator<JsonNode> jsonIterator = jsonArray.iterator();
            while (jsonIterator.hasNext()) {
                final JsonNode json = jsonIterator.next();
                parseJson(json);
            }
        }
    }

    /**
     * Alert listeners about a new item providing type and format information.
     *
     * @param type the specific itemtype
     * @param format format of itemtype (Json-ld, Microdata etc)
     */
    private void foundItemtype(final String type, final Format format) {
        totalItemtypes++;
        for (final Listener listener : listeners) {
            listener.foundItemtype(type, format);
        }
    }
}
