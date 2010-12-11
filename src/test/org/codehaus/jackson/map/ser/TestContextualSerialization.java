package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JacksonAnnotation;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.module.SimpleModule;

/**
 * Test cases to verify that it is possible to define serializers
 * that can use contextual information (like field/method
 * annotations) for configuration.
 * 
 * @since 1.7
 */
public class TestContextualSerialization extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /* NOTE: important; MUST be considered a 'Jackson' annotation to be seen
     * (or recognized otherwise via AnnotationIntrospect.isHandled())
     */
    @Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotation
    public @interface Prefix {
        public String value();
    }

    static class ContextualBean
    {
        protected final String _value;

        public ContextualBean(String s) { _value = s; }

        @Prefix("see:")
        public String getValue() { return _value; }
    }

    /**
     * Another bean that has class annotations that should be visible for
     * contextualizer, too
     */
    @Prefix("Voila->")
    static class BeanWithClassConfig
    {
        public String value;

        public BeanWithClassConfig(String v) { value = v; }
    }
    
    /**
     * Annotation-based contextual serializer that simply prepends piece of text.
     */
    static class AnnotatedContextualSerializer
        extends JsonSerializer<String>
        implements ContextualSerializer<String>
    {
        protected final String _prefix;
        
        public AnnotatedContextualSerializer() { this(""); }
        public AnnotatedContextualSerializer(String p) {
            _prefix = p;
        }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeString(_prefix + value);
        }

        @Override
        public JsonSerializer<String> createContextual(SerializationConfig config, BeanProperty property)
                throws JsonMappingException
        {
            String prefix = "UNKNOWN";
            Prefix ann = property.getAnnotation(Prefix.class);
            if (ann != null) {
                prefix = ann.value();
            }
            return new AnnotatedContextualSerializer(prefix);
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * Test to verify that contextual serializer can make use of property
     * (method, field) annotations.
     */
    public void testMethodAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        assertEquals("{\"value\":\"see:foobar\"}", mapper.writeValueAsString(new ContextualBean("foobar")));
    }

    /**
     * Test to verify that contextual serializer can also use annotations
     * for enclosing class.
     */
    /*
    public void testClassAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addSerializer(String.class, new AnnotatedContextualSerializer());
        mapper.registerModule(module);
        assertEquals("{\"value\":\"voila->xyz\"}", mapper.writeValueAsString(new BeanWithClassConfig("xyz")));
    }
*/
}
