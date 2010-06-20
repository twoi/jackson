import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.util.BufferRecycler;

// json.org's reference implementation
import org.json.*;
// Jsontool implementation
import com.sdicons.json.parser.JSONParser;
// Noggit:
//import org.apache.noggit.JSONParser;

public final class TestJsonPerf
{
    private final int REPS;

    private final static int TEST_PER_GC = 15;

    final JsonFactory _jsonFactory;
    
    final ObjectMapper _mapper;

    final SmileFactory _smileFactory;
    
    final byte[] _jsonData;

    final byte[] _smileData;
    
    protected int mBatchSize;

    public TestJsonPerf(File f) throws IOException
    {
        _jsonFactory = new JsonFactory();
        _mapper = new ObjectMapper(_jsonFactory);
        _smileFactory = new SmileFactory();
        _jsonData = readData(f);
        _smileData = convertToSmile(_jsonData);

        // Let's try to guestimate suitable size... to get to 50 megs parsed
        REPS = (int) ((double) (50 * 1000 * 1000) / (double) _jsonData.length);

        System.out.println("Read "+_jsonData.length+" bytes (smile: "+_smileData.length+") from '"+f+"'; will do "+REPS+" reps");
        System.out.println();
    }

    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            // Use 9 to test all...
            int round = (i++ % 5);

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Jackson, stream/byte";
                sum += testJacksonStream(REPS, _jsonFactory, _jsonData, true);
                break;
            case 1:
                msg = "Jackson, stream/char";
                sum += testJacksonStream(REPS, _jsonFactory, _jsonData, false);
                break;
            case 2:
                msg = "Jackson/smile, stream";
                sum += testJacksonStream(REPS, _smileFactory, _smileData, true);
                break;
            case 3:
                msg = "Noggit";
                sum += testNoggit(REPS);
                break;

            case 4:
                msg = "Jackson, Java types";
                sum += testJacksonJavaTypes(_mapper, REPS);
                break;

            case 5:
                msg = "Jackson, JSON types";
                sum += testJacksonJsonTypes(_mapper, REPS);
                break;
            case 6:
                msg = "Json.org";
                sum += testJsonOrg(REPS);
                break;
            case 7:
                msg = "Json-simple";
                sum += testJsonSimple(REPS);
                break;
            case 8:
                msg = "JSONTools (berlios.de)";
                sum += testJsonTools(REPS);
                break;
            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");


            if ((i % TEST_PER_GC) == 0) {
                System.out.println("[GC]");
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
                System.gc();
                try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            }
        }
    }

    private final byte[] readData(File f)
        throws IOException
    {
        int len = (int) f.length();
        byte[] data = new byte[len];
        int offset = 0;
        FileInputStream fis = new FileInputStream(f);
        
        while (len > 0) {
            int count = fis.read(data, offset, len-offset);
            offset += count;
            len -= count;
        }

        return data;
    }

    private byte[] convertToSmile(byte[] json) throws IOException
    {
    	JsonParser jp = _jsonFactory.createJsonParser(json);
    	ByteArrayOutputStream out = new ByteArrayOutputStream(200);
    	JsonGenerator jg = _smileFactory.createJsonGenerator(out);
    	while (jp.nextToken() != null) {
    		jg.copyCurrentEvent(jp);
    	}
    	jp.close();
    	jg.close();
    	return out.toByteArray();
    }
    
    protected int testJsonOrg(int reps)
        throws Exception
    {
        Object ob = null;
        // Json.org's code only accepts Strings:
        String input = new String(_jsonData, "UTF-8");
        for (int i = 0; i < reps; ++i) {
            JSONTokener tok = new JSONTokener(input);
            ob = tok.nextValue();
        }
        return ob.hashCode();
    }

    private int testJsonTools(int reps)
        throws Exception
    {
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            // Json-tools accepts streams, yay!
            JSONParser jp = new JSONParser(new ByteArrayInputStream(_jsonData), "byte stream");
            /* Hmmmh. Will we get just one object for the whole thing?
             * Or a stream? Seems like just one
             */
            //while ((ob = jp.nextValue()) != null) { ; }
            ob = jp.nextValue();
        }
        return ob.hashCode();
    }

    private int testJsonSimple(int reps)
        throws Exception
    {
        // Json.org's code only accepts Strings:
        String input = new String(_jsonData, "UTF-8");
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            ob = org.json.simple.JSONValue.parse(input);
        }
        return ob.hashCode();
    }

    private int testNoggit(int reps)
        throws Exception
    {
        ByteArrayInputStream bin = new ByteArrayInputStream(_jsonData);

        char[] cbuf = new char[_jsonData.length];

        IOContext ctxt = new IOContext(new BufferRecycler(), this, false);
        int sum = 0;

        for (int i = 0; i < reps; ++i) {
            /* This may be unfair advantage (allocating buffer of exact
             * size)? But let's do that for now
             */
            //char[] cbuf = new char[mData.length];
            //InputStreamReader r = new InputStreamReader(bin, "UTF-8");
            byte[] bbuf = ctxt.allocReadIOBuffer();
            /* 13-Jan-2009, tatu: Note: Noggit doesn't use our turbo-charged
             *   UTF8 codec by default. But let's make it as fast as we
             *   possibly can...
             */
            UTF8Reader r = new UTF8Reader(ctxt, bin, bbuf, 0, 0);

            bin.reset();
            org.apache.noggit.JSONParser jp = new org.apache.noggit.JSONParser(r, cbuf);
            int type;
            while ((type = jp.nextEvent()) != org.apache.noggit.JSONParser.EOF) {
                if (type == org.apache.noggit.JSONParser.STRING) {
                    sum += jp.getString().length();
                }
            }
        }
        return sum;
    }

    private int testJacksonStream(int reps, JsonFactory factory, byte[] data, boolean fast)
        throws Exception
    {
        int sum = 0;
        for (int i = 0; i < reps; ++i) {
            // note: fast is not used any more
            JsonParser jp;

            if (fast) {
                jp = factory.createJsonParser(data, 0, data.length);
            } else {
                jp = factory.createJsonParser(new ByteArrayInputStream(data));
            }
            JsonToken t;
            while ((t = jp.nextToken()) != null) {
                // Field names are always constructed
                if (t == JsonToken.VALUE_STRING
                    //|| t == JsonToken.FIELD_NAME
                    ) {
                    sum += jp.getText().length();
                }
            }
            jp.close();
        }
        return sum;
    }

    private int testJacksonJavaTypes(ObjectMapper mapper, int reps)
        throws Exception
    {
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            // This is "untyped"... Maps, Lists etc
            ob = mapper.readValue(_jsonData, 0, _jsonData.length, Object.class);
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    private int testJacksonJsonTypes(ObjectMapper mapper, int reps)
        throws Exception
    {
        Object ob = null;
        for (int i = 0; i < reps; ++i) {
            ob = mapper.readValue(_jsonData, 0, _jsonData.length, JsonNode.class);
        }
        return ob.hashCode(); // just to get some non-optimizable number
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... <file>");
            System.exit(1);
        }
        new TestJsonPerf(new File(args[0])).test();
    }
}

