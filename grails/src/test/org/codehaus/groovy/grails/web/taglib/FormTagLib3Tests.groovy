package org.codehaus.groovy.grails.web.taglib

import java.io.StringWriter;

import org.codehaus.groovy.grails.plugins.web.taglib.FormTagLib;
import org.w3c.dom.Document
import org.w3c.dom.Element;

/**
 * Tests for the FormTagLib.groovy file which contains tags to help with the
 * creation of HTML forms
 *
 * @author Graeme
 *
 */
public class FormTagLib3Tests extends AbstractGrailsTagTests {

    /** The name used for the datePicker tags created in the test cases. */
    private static final String DATE_PICKER_TAG_NAME = "testDatePicker";

    private static final Collection DATE_PRECISIONS_INCLUDING_MINUTE = Collections.unmodifiableCollection(Arrays.asList( ["minute", null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_HOUR = Collections.unmodifiableCollection(Arrays.asList(["hour", "minute",null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_DAY = Collections.unmodifiableCollection(Arrays.asList(["day", "hour", "minute", null] as String[] ))
    private static final Collection DATE_PRECISIONS_INCLUDING_MONTH = Collections.unmodifiableCollection(Arrays.asList(["month", "day", "hour", "minute", null] as String[] ))

    def lineSep = new String([(char)13,(char)10] as char[])    
    
    public void testHiddenFieldTag() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

		withTag("hiddenField", pw) { tag ->
    		def attributes = [name: "testField", value: "1"]
    		tag.call(attributes)
	
    		assertEquals '<input type="hidden" name="testField" value="1" id="testField" />', sw.toString()
		}
    }

    public void testRadioTag() {
    	StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);

    	withTag("radio", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: "testRadio", checked: "true", value: "1"])
    		tag.call(attributes)

	    	assertEquals "<input type=\"radio\" name=\"testRadio\" checked=\"checked\" value=\"1\" id=\"testRadio\"  />", sw.toString()
    	}

    	sw = new StringWriter();
    	pw = new PrintWriter(sw);

    	withTag("radio", pw) { tag ->
	    	// use sorted map to be able to predict the order in which tag attributes are generated
    		def attributes = new TreeMap([name: "testRadio", value: "2"])
    		tag.call(attributes)

    		assertEquals "<input type=\"radio\" name=\"testRadio\" value=\"2\" id=\"testRadio\"  />", sw.toString()
    	}
    }

    void testRadioUsesExpressionForDisable() {
        def template = '<g:set var="flag" value="${true}"/><g:radio disabled="${flag}" name="foo" value="bar" />'
        assertOutputContains('disabled="disabled"', template)
        template = '<g:set var="flag" value="${false}"/><g:radio disabled="${flag}" name="foo" value="bar" />'
        assertOutputContains('<input type="radio" name="foo" value="bar"', template)
        template = '<g:radio disabled="true" name="foo" value="bar" />'
        assertOutputContains('disabled="disabled"', template)
        template = '<g:radio disabled="false" name="foo" value="bar" />'
        assertOutputContains('<input type="radio" name="foo" value="bar"', template)
    }

    public void testRadioGroupTagWithLabels() {
           StringWriter sw = new StringWriter();
           PrintWriter pw = new PrintWriter(sw);
          withTag("radioGroup", pw) { tag ->
               def attributes = new TreeMap([name: "testRadio", labels:['radio.1', 'radio.2', 'radio.3'],
                                            values:[1,2,3], value: "1"])
               tag.call(attributes, {"<p><g:message code=\"${it.label}\" /> ${it.radio}</p>"})
               assertEquals ("<p><g:message code=\"radio.1\" /> <input type=\"radio\" name=\"testRadio\" checked=\"checked\" value=\"1\" /></p>"
                + lineSep + "<p><g:message code=\"radio.2\" /> <input type=\"radio\" name=\"testRadio\" value=\"2\" /></p>"
                + lineSep + "<p><g:message code=\"radio.3\" /> <input type=\"radio\" name=\"testRadio\" value=\"3\" /></p>"
                + lineSep , sw.toString())
           }
       }

        public void testRadioGroupTagWithoutLabelsAndInvalidValue() {
           StringWriter sw = new StringWriter();
           PrintWriter pw = new PrintWriter(sw);
           withTag("radioGroup", pw) { tag ->
               def attributes = new TreeMap([name: "testRadio2",
                                            values:[3,2], value: "1"])
               tag.call(attributes, {"<p><g:message code=\"${it.label}\" /> ${it.radio}</p>"})
               assertEquals ("<p><g:message code=\"Radio 3\" /> <input type=\"radio\" name=\"testRadio2\" value=\"3\" /></p>"
                + lineSep + "<p><g:message code=\"Radio 2\" /> <input type=\"radio\" name=\"testRadio2\" value=\"2\" /></p>"
                + lineSep , sw.toString())
           }
       }

        public void testRadioGroupTagWithNonStringValue() {
           StringWriter sw = new StringWriter();
           PrintWriter pw = new PrintWriter(sw);
           withTag("radioGroup", pw) { tag ->
               def attributes = new TreeMap([name: "testRadio2",
                                            values:[4,1], value: 1])
               tag.call(attributes, {"<p><g:message code=\"${it.label}\" /> ${it.radio}</p>"})
               assertEquals ("<p><g:message code=\"Radio 4\" /> <input type=\"radio\" name=\"testRadio2\" value=\"4\" /></p>"
                + lineSep + "<p><g:message code=\"Radio 1\" /> <input type=\"radio\" name=\"testRadio2\" checked=\"checked\" value=\"1\" /></p>"
                + lineSep , sw.toString())
           }
       }

        public void testRadioGroupTagWithNoValue() {
           StringWriter sw = new StringWriter();
           PrintWriter pw = new PrintWriter(sw);
           withTag("radioGroup", pw) { tag ->
               def attributes = new TreeMap([name: "testRadio2",
                                            values:[4,1]])
               tag.call(attributes, {"<p><g:message code=\"${it.label}\" /> ${it.radio}</p>"})
               assertEquals ("<p><g:message code=\"Radio 4\" /> <input type=\"radio\" name=\"testRadio2\" value=\"4\" /></p>"
                + lineSep + "<p><g:message code=\"Radio 1\" /> <input type=\"radio\" name=\"testRadio2\" value=\"1\" /></p>"
                + lineSep , sw.toString())
           }
       }

    public void testCheckboxTag() {
        def template = '<g:checkBox name="foo" value="${test}"/>'

        assertOutputEquals('<input type="hidden" name="_foo" /><input type="checkbox" name="foo" checked="checked" value="hello" id="foo"  />', template, [test:"hello"])

        template = '<g:checkBox name="foo" value="${test}" checked="false"/>'

        assertOutputEquals('<input type="hidden" name="_foo" /><input type="checkbox" name="foo" value="hello" id="foo"  />', template, [test:"hello"])

        template = '<g:checkBox name="foo" value="${test}" checked="${true}"/>'

        assertOutputEquals('<input type="hidden" name="_foo" /><input type="checkbox" name="foo" checked="checked" value="hello" id="foo"  />', template, [test:"hello"])

        template = '<g:checkBox name="foo.bar" value="${test}" checked="${true}"/>'

        assertOutputEquals('<input type="hidden" name="foo._bar" /><input type="checkbox" name="foo.bar" checked="checked" value="hello" id="foo.bar"  />', template, [test:"hello"])

        template = '<g:checkBox name="foo.bar" value="${test}" checked="${null}"/>'

        assertOutputEquals('<input type="hidden" name="foo._bar" /><input type="checkbox" name="foo.bar" value="hello" id="foo.bar"  />', template, [test:"hello"])
    }

    void testCheckBoxUsesExpressionForDisable() {
        def template = '<g:set var="flag" value="${true}"/><g:checkBox disabled="${flag}" name="foo"/>'
        assertOutputContains('disabled="disabled"', template)
        template = '<g:set var="flag" value="${false}"/><g:checkBox disabled="${flag}" name="foo"/>'
        assertOutputContains('<input type="checkbox" name="foo" id="foo"', template)
        template = '<g:checkBox disabled="true" name="foo"/>'
        assertOutputContains('disabled="disabled"', template)
        template = '<g:checkBox disabled="false" name="foo"/>'
        assertOutputContains('<input type="checkbox" name="foo" id="foo"', template)
    }

    void testRenderingNoSelectionOption() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);
    	FormTagLib formTagLib = new FormTagLib()
    	formTagLib.renderNoSelectionOptionImpl(pw, '', '', null)
    	assertEquals '<option value=""></option>', sw.toString() 
    }

    public void testNoHtmlEscapingTextAreaTag() throws Exception {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("textArea", pw) { tag ->
	
	        assertNotNull(tag);
	
	        final Map attrs = new HashMap();
	        attrs.put("name","testField");
	        attrs.put("escapeHtml","false");
	        attrs.put("value", "<b>some text</b>");
	
	        tag.call(attrs, {});
	
	        final String result = sw.toString();
	        // need to inspect this as raw text so the DocumentHelper doesn't
	        // unescape anything...
	        assertTrue(result.indexOf("<b>some text</b>") >= 0);
	
	        final Document document = parseText(sw.toString());
	        assertNotNull(document);
	
	        final Element inputElement = document.getDocumentElement();
	        assertFalse("escapeHtml attribute should not exist", inputElement.hasAttribute("escapeHtml"));
    	}
    }
}
