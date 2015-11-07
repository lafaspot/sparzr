package com.lafaspot.sparzr;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Unit tests for {@link Parser} to test parser logic.
 *
 * @author akajla
 *
 */
public class ParserTest {

    /** Object mapper for json serialization. **/
    private ObjectMapper oMapper;

    /**
     * The setup required for this test class.
     */
    @BeforeClass
    public void init() {
        oMapper = new ObjectMapper();
    }

    /**
     * Simple html containing microdata.
     */
    private final String htmlWithDivs = "<div itemscope itemtype=\"http://schema.org/FoodEstablishmentReservation\">"
            + "<meta itemprop=\"reservationNumber\" content=\"OT12345\"/>"
            + "<link itemprop=\"reservationStatus\" href=\"http://schema.org/Confirmed\"/>"
            + "<div itemprop=\"underName\" itemscope itemtype=\"http://schema.org/Person\">" + " <meta itemprop=\"name\" content=\"John Smith\"/>"
            + "</div>" + "<div itemprop=\"reservationFor\" itemscope itemtype=\"http://schema.org/FoodEstablishment\">"
            + " <meta itemprop=\"name\" content=\"Wagamama\"/>"
            + " <div itemprop=\"address\" itemscope itemtype=\"http://schema.org/PostalAddress\">"
            + "  <meta itemprop=\"streetAddress\" content=\"1 Tavistock Street\"/>" + "  <meta itemprop=\"addressLocality\" content=\"London\"/>"
            + "  <meta itemprop=\"addressRegion\" content=\"Greater London\"/>" + "  <meta itemprop=\"postalCode\" content=\"WC2E 7PG\"/>"
            + "  <meta itemprop=\"addressCountry\" content=\"United Kingdom\"/>" + "</div>" + "</div>"
            + "<meta itemprop=\"startTime\" content=\"2017-04-10T08:00:00+00:00\"/>" + "<meta itemprop=\"partySize\" content=\"2\"/>" + "</div>";

    /**
     * Simple html example containing json-ld.
     */
    private final String htmlWithJson = "<script type=\"application/ld+json\">\r\n{\r\n  \"@context\": "
            + "\"http://schema.org\",\r\n  \"@type\": \"FoodEstablishmentReservation\",\r\n  \"reservationNumber\": "
            + "\"OT12345\",\r\n  \"reservationStatus\": \"http://schema.org/Confirmed\",\r\n  \"underName\": {\r\n    "
            + "\"@type\": \"Person\",\r\n    \"name\": \"John Smith\"\r\n  },\r\n  \"reservationFor\": {\r\n    \"@type\": "
            + "\"FoodEstablishment\",\r\n    \"name\": \"Wagamama\",\r\n    \"address\": {\r\n      \"@type\": \"PostalAddress\",\r\n      "
            + "\"streetAddress\": \"1 Tavistock Street\",\r\n      \"addressLocality\": \"London\",\r\n      \"addressRegion\": "
            + "\"Greater London\",\r\n      \"postalCode\": \"WC2E 7PG\",\r\n      \"addressCountry\": \"United Kingdom\"\r\n    }\r\n  },\r\n  "
            + "\"startTime\": \"2017-04-10T08:00:00+00:00\",\r\n  \"partySize\": \"2\"\r\n}\r\n</script>";

    /** In the below Json script type, second element is malformed near reservationFor (missing "{" before @type). Used for a negative test case. **/
    private final String htmlWithSecondMalformedJsonScriptType = "<script type=\"application/ld+json\">\r\n{\r\n  \"@context\": "
            + "\"http://schema.org\",\r\n  \"@type\": \"FoodEstablishmentReservation\",\r\n  \"reservationNumber\": "
            + "\"OT12345\",\r\n  \"reservationStatus\": \"http://schema.org/Confirmed\",\r\n  \"underName\": {\r\n    "
            + "\"@type\": \"Person\",\r\n    \"name\": \"John Smith\"\r\n  },\r\n  \"reservationFor\": {\r\n    \"@type\": "
            + "\"FoodEstablishment\",\r\n    \"name\": \"Wagamama\",\r\n    \"address\": {\r\n      \"@type\": \"PostalAddress\",\r\n      "
            + "\"streetAddress\": \"1 Tavistock Street\",\r\n      \"addressLocality\": \"London\",\r\n      \"addressRegion\": "
            + "\"Greater London\",\r\n      \"postalCode\": \"WC2E 7PG\",\r\n      \"addressCountry\": \"United Kingdom\"\r\n    }\r\n  },\r\n  "
            + "\"startTime\": \"2017-04-10T08:00:00+00:00\",\r\n  \"partySize\": \"2\"\r\n}\r\n</script>"
            + "<script type=\"application/ld+json\">\r\n" + "{\r\n  \"@context\": "
            + "\"http://schema.org\",\r\n  \"@type\": \"FoodEstablishmentReservation\",\r\n  \"reservationNumber\": "
            + "\"OT12345\",\r\n  \"reservationStatus\": \"http://schema.org/Confirmed\",\r\n  \"underName\": {\r\n    "
            + "\"@type\": \"Person\",\r\n    \"name\": \"John Smith\"\r\n  },\r\n  \"reservationFor\": \r\n    \"@type\": "
            + "\"FoodEstablishment\",\r\n    \"name\": \"Wagamama\",\r\n    \"address\": {\r\n      \"@type\": \"PostalAddress\",\r\n      "
            + "\"streetAddress\": \"1 Tavistock Street\",\r\n      \"addressLocality\": \"London\",\r\n      \"addressRegion\": "
            + "\"Greater London\",\r\n      \"postalCode\": \"WC2E 7PG\",\r\n      \"addressCountry\": \"United Kingdom\"\r\n    }\r\n  },\r\n  "
            + "\"startTime\": \"2017-04-10T08:00:00+00:00\",\r\n  \"partySize\": \"2\"\r\n}" + "</script>";

    /** In the below Json script type, first element is malformed near reservationFor (missing "{" before @type). Used for a negative test case. **/
    private final String htmlWithFirstMalformedJsonScriptType = "<script type=\"application/ld+json\">\r\n" + "{\r\n  \"@context\": "
            + "\"http://schema.org\",\r\n  \"@type\": \"FoodEstablishmentReservation\",\r\n  \"reservationNumber\": "
            + "\"OT12345\",\r\n  \"reservationStatus\": \"http://schema.org/Confirmed\",\r\n  \"underName\": {\r\n    "
            + "\"@type\": \"Person\",\r\n    \"name\": \"John Smith\"\r\n  },\r\n  \"reservationFor\": \r\n    \"@type\": "
            + "\"FoodEstablishment\",\r\n    \"name\": \"Wagamama\",\r\n    \"address\": {\r\n      \"@type\": \"PostalAddress\",\r\n      "
            + "\"streetAddress\": \"1 Tavistock Street\",\r\n      \"addressLocality\": \"London\",\r\n      \"addressRegion\": "
            + "\"Greater London\",\r\n      \"postalCode\": \"WC2E 7PG\",\r\n      \"addressCountry\": \"United Kingdom\"\r\n    }\r\n  },\r\n  "
            + "\"startTime\": \"2017-04-10T08:00:00+00:00\",\r\n  \"partySize\": \"2\"\r\n}" + "</script>"
            + "<script type=\"application/ld+json\">\r\n{\r\n  \"@context\": "
            + "\"http://schema.org\",\r\n  \"@type\": \"FoodEstablishmentReservation\",\r\n  \"reservationNumber\": "
            + "\"OT12345\",\r\n  \"reservationStatus\": \"http://schema.org/Confirmed\",\r\n  \"underName\": {\r\n    "
            + "\"@type\": \"Person\",\r\n    \"name\": \"John Smith\"\r\n  },\r\n  \"reservationFor\": {\r\n    \"@type\": "
            + "\"FoodEstablishment\",\r\n    \"name\": \"Wagamama\",\r\n    \"address\": {\r\n      \"@type\": \"PostalAddress\",\r\n      "
            + "\"streetAddress\": \"1 Tavistock Street\",\r\n      \"addressLocality\": \"London\",\r\n      \"addressRegion\": "
            + "\"Greater London\",\r\n      \"postalCode\": \"WC2E 7PG\",\r\n      \"addressCountry\": \"United Kingdom\"\r\n    }\r\n  },\r\n  "
            + "\"startTime\": \"2017-04-10T08:00:00+00:00\",\r\n  \"partySize\": \"2\"\r\n}\r\n</script>";

    /** Input for the list flattening test. **/
    private final String listInput = "<div>A Recipe and A Dance:<ul itemscope itemtype=\"http://schema.org/ItemList\">"
            + "<li>Laser Stew</li><ul itemprop=\"itemListElement\" itemtype=\"http://schema.org/Recipe\">"
            + "<li itemprop=\"ingredients\">Tomato</li><li itemprop=\"ingredients\">Corn</li>"
            + "<li itemprop=\"ingredients\">Laser Gun</li></ul><li>Time Warp</li>"
            + "<ol itemprop=\"itemListElement\" itemtype=\"http://schema.org/Action\">"
            + "<li itemprop=\"result\">Jump Left</li><li itemprop=\"result\">Step Right</li>"
            + "<li itemprop=\"result\">Put Hand on Hips</li><li itemprop=\"result\">Bring knees in tight</li></ol></ul></div>";

    /**
     * Test parsing of Microdata.
     *
     * @throws IOException Error with input to parser
     * @throws SAXException thrown by SAX parser
     */
    @Test
    public void testXml() throws IOException, SAXException {
        final DefaultListener defaultListener = new DefaultListener();
        final Parser parser = new Parser();
        parser.registerListener(defaultListener);
        parser.parse(htmlWithDivs);
        Assert.assertEquals(defaultListener.totalItemtypes(), 4);
    }

    /**
     * Test parsing of simple json-ld.
     *
     * @throws IOException Error with input to parser
     * @throws SAXException thrown by SAX parser
     */
    @Test
    public void testJson() throws IOException, SAXException {
        final DefaultListener defaultListener = new DefaultListener();
        final Parser parser = new Parser();
        parser.registerListener(defaultListener);
        parser.parse(htmlWithJson);
        Assert.assertEquals(defaultListener.totalItemtypes(), 4);
    }

    /**
     * Another example testing microdata parsing from an Imdb webpage.
     *
     * @throws IOException Error with input to parser
     * @throws SAXException thrown by SAX parser
     */
    @Test
    public void testImdbMicrodata() throws IOException, SAXException {
        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream stream = classLoader.getResourceAsStream("imdb_sample_schema.txt");
        final String html = IOUtils.toString(stream);
        final DefaultListener defaultListener = new DefaultListener();
        final Parser parser = new Parser();
        parser.registerListener(defaultListener);
        parser.parse(html);
        final JsonNode items = defaultListener.getItems();
        Assert.assertEquals(defaultListener.totalItemtypes(), 28);
        Assert.assertEquals(
                items.toString(),
				"[{\"type\":[\"Movie\"],\"image\":[\"http://ia.media-imdb.com/images/M/MV5BMjA5ODYwNjgxMF5BMl5BanBnXkFtZTgwMTcwNzAyMjE"
				+ "@._V1_SY317_CR0,0,214,317_AL_.jpg\"],\"name\":[\"Sin City: A Dame to Kill For\"],\"contentRating\":[\"R\"],\""
				+ "duration\":[\"PT102M\"],\"genre\":[\"Action\",\"Crime\",\"Thriller\"]},{\"type\":"
						+ "[\"AggregateRating\"],\"ratingValue\":[\"7.3\"],\"bestRating\":[\"10\"],\"ratingCount\":[\"5,108\"],"
						+ "\"reviewCount\":[\"29 user\",\"8 critic\"]},{\""
						+ "type\":[\"Person\"],\"url\":[\"/name/nm0588340/?ref_=tt_ov_dr\",\"/name/nm0001675/?ref_=tt_ov_dr\"],"
						+ "\"name\":[\"Frank Miller\",\"Robert Rodriguez\"]},{\""
						+ "type\":[\"Person\"],\"url\":[\"/name/nm0588340/?ref_=tt_ov_wr\",\"/name/nm0588340/?ref_=tt_ov_wr\"],"
						+ "\"name\":[\"Frank Miller\",\"Frank Miller\"]},"
						+ "{\"type\":[\"Person\"],\"url\":[\"/name/nm0000620/?ref_=tt_ov_st\",\"/name/nm0004695/?ref_=tt_ov_st\","
						+ "\"/name/nm0000982/?ref_=tt_ov_st\",\"fullcredits?"
						+ "ref_=tt_ov_st_sm\"],\"name\":[\"Mickey Rourke\",\"Jessica Alba\",\"Josh Brolin\"]},{\"type\":"
						+ "[\"videoObject\"]},{\"type\":[\"videoObject\"]},{\"type\":"
						+ "[\"Person\"],\"url\":[\"/name/nm0000620/?ref_=tt_cl_t1\"],\"name\":[\"Mickey Rourke\"]},{\"type\":[\"Person\"]"
						+ ",\"url\":[\"/name/nm0004695/?ref_=tt_cl_t2\"]"
						+ ",\"name\":[\"Jessica Alba\"]},{\"type\":[\"Person\"],\"url\":[\"/name/nm0000982/?ref_=tt_cl_t3\"],\"name\":[\"Josh"
						+ " Brolin\"]},{\"type\":[\"Person\"],\"url\""
						+ ":[\"/name/nm0330687/?ref_=tt_cl_t4\"],\"name\":[\"Joseph Gordon-Levitt\"]},{\"type\":[\"Person\"],\"url\":[\"/name"
						+ "/nm0206257/?ref_=tt_cl_t5\"],\"name\":[\""
						+ "Rosario Dawson\"]},{\"type\":[\"Person\"],\"url\":[\"/name/nm0000246/?ref_=tt_cl_t6\"],\"name\":[\"Bruce Willis\"]"
						+ "},{\"type\":[\"Person\"],\"url\":[\"/name/"
						+ "nm1200692/?ref_=tt_cl_t7\"],\"name\":[\"Eva Green\"]},{\"type\":[\"Person\"],\"url\":[\"/name/nm0000959/?ref_=tt_"
						+ "cl_t8\"],\"name\":[\"Powers Boothe\"]},{\""
						+ "type\":[\"Person\"],\"url\":[\"/name/nm0371660/?ref_=tt_cl_t9\"],\"name\":[\"Dennis Haysbert\"]},{\"type\":[\"Per"
						+ "son\"],\"url\":[\"/name/nm0000501/?ref_=tt_"
						+ "cl_t10\"],\"name\":[\"Ray Liotta\"]},{\"type\":[\"Person\"],\"url\":[\"/name/nm0005221/?ref_=tt_cl_t11\"],\"name\":"
						+ "[\"Christopher Meloni\"]},{\"type\":[\""
						+ "Person\"],\"url\":[\"/name/nm0005315/?ref_=tt_cl_t12\"],\"name\":[\"Jeremy Piven\"]},{\"type\":[\"Person\"],\"url\":"
						+ "[\"/name/nm0000502/?ref_=tt_cl_t13\"],\""
						+ "name\":[\"Christopher Lloyd\"]},{\"type\":[\"Person\"],\"url\":[\"/name/nm0454809/?ref_=tt_cl_t14\"],\"name\":[\"Jaime"
						+ " King\"]},{\"type\":[\"Person\"],\"url\""
						+ ":[\"/name/nm1017334/?ref_=tt_cl_t15\"],\"name\":[\"Juno Temple\"]},{\"type\":[\"Audience\"],\"url\":[\"/title/tt0458481"
						+ "/parentalguide?ref_=tt_stry_pg\"]},{\""
						+ "type\":[\"Organization\"],\"url\":[\"/company/co0335687?ref_=tt_dt_co\"],\"name\":[\"Aldamisa Entertainment\"]},{\"type"
						+ "\":[\"Organization\"],\"url\":[\"/company"
						+ "/co0317576?ref_=tt_dt_co\"],\"name\":[\"Demarest Films\"]},{\"type\":[\"Organization\"],\"url\":[\"/company/co0022594?"
						+ "ref_=tt_dt_co\"],\"name\":[\"Miramax Films"
						+ "\"]},{\"type\":[\"Review\"],\"name\":[\"Not as good as the first one but still very solid and entertaining\"],\"review"
						+ "Rating\":[{\"type\":[\"Rating\"]}]}]");
    }

    /**
     * Test for regression on list flattening.
     *
     * @throws IOException
     *             when cannot read from the stream.
     * @throws SAXException
     *             occurs when json is malformed.
     */
    @Test(enabled = false)
    public void testListFlatten() throws IOException, SAXException {
        final Parser parser = new Parser();
        final DefaultListener defaultListener = new DefaultListener();
        parser.registerListener(defaultListener);
        parser.parse(listInput);
        final JsonNode items = defaultListener.getItems();
        Assert.assertEquals(
                items.toString(),
                "[{\"type\":[\"ItemList\"],\"ingredients\":[\"Tomato\",\"Corn\",\"Laser Gun\"],"
                + "\"result\":[\"Jump Left\",\"Step Right\",\"Put Hand on Hips\",\"Bring knees in tight\"]}]");
    }

    /**
     * Test NPE is returned when the html to parse is null.
     *
     * @throws SAXException
     *             occurs when json is malformed.
     * @throws IOException
     *             when cannot read from the stream.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testJsonElementNullHtml() throws IOException, SAXException {
        final Parser parser = new Parser();
        final DefaultListener listener = new DefaultListener();
        parser.registerListener(listener);
        parser.parse(null);
    }

    /**
     * Test if the valid JSON element is returned after parsing. The json used as the expected output is the json in the htmlWithJson html string.
     *
     * @throws SAXException
     *             occurs when json is malformed.
     * @throws IOException
     *             when cannot read from the stream.
     */
    @Test
    public void testJsonEmptyHtml() throws IOException, SAXException {
        final Parser parser = new Parser();
        final DefaultListener listener = new DefaultListener();
        parser.registerListener(listener);
        parser.parse("");

        Assert.assertEquals(listener.getItems(), JsonNodeFactory.instance.arrayNode());
        Assert.assertEquals(listener.totalItemtypes(), 0);
    }

    /**
     * Test if the valid JSON element is returned after parsing. The json used as the expected output is the json in the htmlWithJson html string.
     *
     * @throws SAXException
     *             occurs when json is malformed.
     * @throws IOException
     *             when cannot read from the stream.
     */
    @Test
    public void testJsonElement() throws IOException, SAXException {
        final String json = "{\"@context\": " + "\"http://schema.org\",\"@type\": \"FoodEstablishmentReservation\",\"reservationNumber\": "
                + "\"OT12345\",\"reservationStatus\": \"http://schema.org/Confirmed\",\"underName\": {"
                + "\"@type\": \"Person\",\"name\": \"John Smith\"},\"reservationFor\": {\"@type\": "
                + "\"FoodEstablishment\",\"name\": \"Wagamama\",\"address\": {\"@type\": \"PostalAddress\","
                + "\"streetAddress\": \"1 Tavistock Street\",\"addressLocality\": \"London\",\"addressRegion\": "
                + "\"Greater London\",\"postalCode\": \"WC2E 7PG\",\"addressCountry\": \"United Kingdom\"}},"
                + "\"startTime\": \"2017-04-10T08:00:00+00:00\",\"partySize\": \"2\"}";
        final JsonNode jNode = oMapper.readTree(json);
        final Parser parser = new Parser();
        final DefaultListener listener = new DefaultListener();
        parser.registerListener(listener);
        parser.parse(htmlWithJson);
        final int typesFound = listener.totalItemtypes();

        Assert.assertEquals(listener.getItems().get(0), jNode);
        Assert.assertEquals(typesFound, 4);
    }

    /**
     * Test, if the parser can continue to extract the schema.org even if one of the script types are malformed in the document. We will have more
     * than one script-type element in a single document and one of the script types is malformed. The result of the parsing should be just one
     * schema.org extracted element (script type). However, it can have more than one schema types. The second script type is malformed.
     *
     * @throws SAXException
     *             when parsing fails.
     * @throws IOException
     *             when I/O fails.
     */
    @Test
    public void testSecondMalformedJsonSchemaOrg() throws SAXException, IOException {
        final String json = "{\"@context\": " + "\"http://schema.org\",\"@type\": \"FoodEstablishmentReservation\",\"reservationNumber\": "
                + "\"OT12345\",\"reservationStatus\": \"http://schema.org/Confirmed\",\"underName\": {"
                + "\"@type\": \"Person\",\"name\": \"John Smith\"},\"reservationFor\": {\"@type\": "
                + "\"FoodEstablishment\",\"name\": \"Wagamama\",\"address\": {\"@type\": \"PostalAddress\","
                + "\"streetAddress\": \"1 Tavistock Street\",\"addressLocality\": \"London\",\"addressRegion\": "
                + "\"Greater London\",\"postalCode\": \"WC2E 7PG\",\"addressCountry\": \"United Kingdom\"}},"
                + "\"startTime\": \"2017-04-10T08:00:00+00:00\",\"partySize\": \"2\"}";
        final JsonNode jNode = oMapper.readTree(json);
        final Parser parser = new Parser();
        final DefaultListener listener = new DefaultListener();
        parser.registerListener(listener);
        parser.parse(htmlWithSecondMalformedJsonScriptType);
        final int typesFound = listener.totalItemtypes();

        Assert.assertEquals(listener.getItems().size(), 1);
        Assert.assertEquals(listener.getItems().get(0), jNode);
        Assert.assertEquals(typesFound, 4);
    }

    /**
     * Test, if the parser can continue to extract the schema.org even if one of the script types are malformed in the document. We will have more
     * than one script-type element in a single document and one of the script types is malformed. The result of the parsing should be just one
     * schema.org extracted element (script type). However, it can have more than one schema types. The first script type is malformed.
     *
     * @throws SAXException
     *             when parsing fails.
     * @throws IOException
     *             when I/O fails.
     */
    @Test
    public void testFirstMalformedJsonSchemaOrg() throws SAXException, IOException {
        final String json = "{\"@context\": " + "\"http://schema.org\",\"@type\": \"FoodEstablishmentReservation\",\"reservationNumber\": "
                + "\"OT12345\",\"reservationStatus\": \"http://schema.org/Confirmed\",\"underName\": {"
                + "\"@type\": \"Person\",\"name\": \"John Smith\"},\"reservationFor\": {\"@type\": "
                + "\"FoodEstablishment\",\"name\": \"Wagamama\",\"address\": {\"@type\": \"PostalAddress\","
                + "\"streetAddress\": \"1 Tavistock Street\",\"addressLocality\": \"London\",\"addressRegion\": "
                + "\"Greater London\",\"postalCode\": \"WC2E 7PG\",\"addressCountry\": \"United Kingdom\"}},"
                + "\"startTime\": \"2017-04-10T08:00:00+00:00\",\"partySize\": \"2\"}";
        final JsonNode jNode = oMapper.readTree(json);
        final Parser parser = new Parser();
        final DefaultListener listener = new DefaultListener();
        parser.registerListener(listener);
        parser.parse(htmlWithFirstMalformedJsonScriptType);
        final int typesFound = listener.totalItemtypes();

        Assert.assertEquals(listener.getItems().size(), 1);
        Assert.assertEquals(listener.getItems().get(0), jNode);
        Assert.assertEquals(typesFound, 4);
    }

    /**
     * Test if the handler can process the html with array of schema types inside the json+ld schema.
     *
     * @throws IOException
     *             when I/O fails.
     * @throws SAXException
     *             when JSON cannot be parsed.
     */
    @Test
    public void testJsonSchemaTypesArrayExample1() throws IOException, SAXException {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream("jsonLDTypesArray1.txt");
        final String jsonLdTypes = IOUtils.toString(stream);
        final Parser parser = new Parser();
        final DefaultListener listener = new DefaultListener();
        parser.registerListener(listener);
        parser.parse(jsonLdTypes);
        Assert.assertEquals(listener.totalItemtypes(), 4);
    }

    /**
     * Test if the handler can process the html with array of schema types inside the json+ld schema.
     *
     * @throws IOException
     *             when I/O fails.
     * @throws SAXException
     *             when JSON cannot be parsed.
     */
    @Test
    public void testJsonSchemaTypesArrayExample2() throws IOException, SAXException {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream("jsonLDTypesArray2.txt");
        final String jsonLdTypes = IOUtils.toString(stream);
        final Parser parser = new Parser();
        final DefaultListener listener = new DefaultListener();
        parser.registerListener(listener);
        parser.parse(jsonLdTypes);
        Assert.assertEquals(listener.totalItemtypes(), 7);
    }
}
