package com.fasterxml.jackson.dataformat.csv;

import java.io.*;
import java.net.URL;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.dataformat.csv.impl.CsvParserBootstrapper;
import com.fasterxml.jackson.dataformat.csv.impl.UTF8Reader;
import com.fasterxml.jackson.dataformat.csv.impl.UTF8Writer;

public class CsvFactory extends JsonFactory
{
    private static final long serialVersionUID = -4036356829870939719L;

    /**
     * Name used to identify CSV format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_CSV = "CSV";
    
    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_CSV_PARSER_FEATURE_FLAGS = CsvParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */
    final static int DEFAULT_CSV_GENERATOR_FEATURE_FLAGS = CsvGenerator.Feature.collectDefaults();

    // could make it use Platform default too but...
    protected final static char[] DEFAULT_LF = { '\n' };

    protected final static CsvSchema DEFAULT_SCHEMA = CsvSchema.emptySchema();
    
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected CsvSchema _schema = DEFAULT_SCHEMA;
    
    protected int _csvParserFeatures = DEFAULT_CSV_PARSER_FEATURE_FLAGS;

    protected int _csvGeneratorFeatures = DEFAULT_CSV_GENERATOR_FEATURE_FLAGS;

    /*
    protected char _cfgColumnSeparator = ',';

    protected char _cfgQuoteCharacter = '"';
    
    protected char[] _cfgLineSeparator = DEFAULT_LF;
    */
    
    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public CsvFactory() { this(null); }

    public CsvFactory(ObjectCodec oc) { super(oc); }

    /**
     * @since 2.2.1
     */
    protected CsvFactory(CsvFactory src, ObjectCodec oc)
    {
        super(src, oc);
        _csvParserFeatures = src._csvParserFeatures;
        _csvGeneratorFeatures = src._csvGeneratorFeatures;
        _schema = src._schema;
    }
    
    @Override
    public CsvFactory copy()
    {
        _checkInvalidCopy(CsvFactory.class);
        return new CsvFactory(this, null);
    }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Method that we need to override to actually make restoration go
     * through constructors etc.
     * Also: must be overridden by sub-classes as well.
     */
    @Override
    protected Object readResolve() {
        return new CsvFactory(this, _objectCodec);
    }

    /*                                                                                       
    /**********************************************************                              
    /* Versioned                                                                             
    /**********************************************************                              
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    // Yes; CSV is positional
    @Override
    public boolean requiresPropertyOrdering() {
        return true;
    }

    // No, we can't make use of char[] optimizations
    @Override
    public boolean canUseCharArrays() { return false; }

    /*
    /**********************************************************
    /* Format detection functionality
    /**********************************************************
     */
    
    @Override
    public String getFormatName() {
        return FORMAT_NAME_CSV;
    }
    
    /**
     * Sub-classes need to override this method (as of 1.8)
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        // !!! TBI [Issue#2] -- but how?
        return MatchStrength.INCONCLUSIVE;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return (schema instanceof CsvSchema);
    }

    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link CsvParser.Feature} for list of features)
     */
    public final CsvFactory configure(CsvParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link CsvParser.Feature} for list of features)
     */
    public CsvFactory enable(CsvParser.Feature f) {
        _csvParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link CsvParser.Feature} for list of features)
     */
    public CsvFactory disable(CsvParser.Feature f) {
        _csvParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(CsvParser.Feature f) {
        return (_csvParserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link CsvGenerator.Feature} for list of features)
     */
    public final CsvFactory configure(CsvGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified generator features
     * (check {@link CsvGenerator.Feature} for list of features)
     */
    public CsvFactory enable(CsvGenerator.Feature f) {
        _csvGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link CsvGenerator.Feature} for list of features)
     */
    public CsvFactory disable(CsvGenerator.Feature f) {
        _csvGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(CsvGenerator.Feature f) {
        return (_csvGeneratorFeatures & f.getMask()) != 0;
    }
    
    /*
    /**********************************************************
    /* Overridden parser factory methods, 2.1
    /**********************************************************
     */

    @SuppressWarnings("resource")
    @Override
    public CsvParser createParser(File f) throws IOException {
        return _createParser(new FileInputStream(f), _createContext(f, true));
    }

    @Override
    public CsvParser createParser(URL url) throws IOException {
        return _createParser(_optimizedStreamFromURL(url), _createContext(url, true));
    }

    @Override
    public CsvParser createParser(InputStream in) throws IOException {
        return _createParser(in, _createContext(in, false));
    }

    @Override
    public CsvParser createParser(Reader r) throws IOException {
        return _createParser(r, _createContext(r, false));
    }

    @Override
    public CsvParser createParser(String doc) throws IOException {
        return _createParser(new StringReader(doc), _createContext(doc, true));
    }
    
    @Override
    public CsvParser createParser(byte[] data) throws IOException {
        return _createParser(data, 0, data.length, _createContext(data, true));
    }
    
    @Override
    public CsvParser createParser(byte[] data, int offset, int len) throws IOException {
        return _createParser(data, offset, len, _createContext(data, true));
    }

    @Override
    public CsvParser createParser(char[] data) throws IOException {
        return _createParser(data, 0, data.length, _createContext(data, true), false);
    }
    
    @Override
    public CsvParser createParser(char[] data, int offset, int len) throws IOException {
        return _createParser(data, offset, len, _createContext(data, true), false);
    }

    /*
    /**********************************************************
    /* Overridden generator factory methods, 2.1+
    /**********************************************************
     */

    @SuppressWarnings("resource")
    @Override
    public CsvGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        // [JACKSON-512]: allow wrapping with _outputDecorator
        if (_outputDecorator != null) {
            out = _outputDecorator.decorate(ctxt, out);
        }
        return _createGenerator(ctxt, _createWriter(out, JsonEncoding.UTF8, ctxt));
    }

    /**
     * This method assumes use of UTF-8 for encoding.
     */
    @Override
    public CsvGenerator createGenerator(OutputStream out) throws IOException {
        return createGenerator(out, JsonEncoding.UTF8);
    }

    @SuppressWarnings("resource")
    @Override
    public CsvGenerator createGenerator(Writer out) throws IOException
    {
        IOContext ctxt = _createContext(out, false);
        // [JACKSON-512]: allow wrapping with _outputDecorator
        if (_outputDecorator != null) {
            out = _outputDecorator.decorate(ctxt, out);
        }
        return _createGenerator(out, ctxt);
    }

    @SuppressWarnings("resource")
    @Override
    public CsvGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // Important: make sure that we always auto-close stream we create:
        IOContext ctxt = _createContext(out, false);
        // [JACKSON-512]: allow wrapping with _outputDecorator
        if (_outputDecorator != null) {
            out = _outputDecorator.decorate(ctxt, out);
        }
        return _createGenerator(ctxt, _createWriter(out, JsonEncoding.UTF8, ctxt));
    }

    /*
    /******************************************************
    /* Overridden internal factory methods
    /******************************************************
     */

    //protected IOContext _createContext(Object srcRef, boolean resourceManaged)

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected CsvParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        BufferRecycler rec = _getBufferRecycler();
        return new CsvParserBootstrapper(ctxt, rec, _objectCodec, in)
            .constructParser(_parserFeatures, _csvParserFeatures);
    }

    @Override
    protected CsvParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        BufferRecycler rec = _getBufferRecycler();
        return new CsvParserBootstrapper(ctxt, rec, _objectCodec, data, offset, len)
            .constructParser(_parserFeatures, _csvParserFeatures);
    }

    /**
     * Overridable factory method that actually instantiates desired
     * parser.
     */
    @Override
    protected CsvParser _createParser(Reader r, IOContext ctxt) throws IOException {
        return new CsvParser(ctxt, _getBufferRecycler(), _parserFeatures, _csvParserFeatures,
                _objectCodec, r);
    }
    
    @Override
    protected CsvParser _createParser(char[] data, int offset, int len, IOContext ctxt,
            boolean recyclable) throws IOException {
        return new CsvParser(ctxt, _getBufferRecycler(), _parserFeatures, _csvParserFeatures,
                _objectCodec, new CharArrayReader(data, offset, len));
    }

    @Override
    protected CsvGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        return _createGenerator(ctxt, out);
    }

    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (enc == JsonEncoding.UTF8) {
            return new UTF8Writer(ctxt, out);
        }
        return new OutputStreamWriter(out, enc.getJavaName());
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected CsvGenerator _createGenerator(IOContext ctxt, Writer out) throws IOException
    {
        CsvGenerator gen = new CsvGenerator(ctxt, _generatorFeatures, _csvGeneratorFeatures,
                _objectCodec, out, _schema);
        // any other initializations? No?
        return gen;
    }

//    protected final Charset UTF8 = Charset.forName("UTF-8");
    
    protected Reader _createReader(InputStream in, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        // default to UTF-8 if encoding missing
        if (enc == null || enc == JsonEncoding.UTF8) {
            // 28-May-2012, tatu: Custom UTF8 reader should be faster, esp for small input:
//            return new InputStreamReader(in, UTF8);
            boolean autoClose = ctxt.isResourceManaged() || isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE);
            return new UTF8Reader(ctxt, in, autoClose);
        }
        return new InputStreamReader(in, enc.getJavaName());
    }

    protected Reader _createReader(byte[] data, int offset, int len,
            JsonEncoding enc, IOContext ctxt) throws IOException
    {
        // default to UTF-8 if encoding missing
        if (enc == null || enc == JsonEncoding.UTF8) {
            return new UTF8Reader(null, data, offset, len);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(data, offset, len);
        return new InputStreamReader(in, enc.getJavaName());
    }
}
