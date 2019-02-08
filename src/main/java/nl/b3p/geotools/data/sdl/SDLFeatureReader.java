/*
 * $Id: SDLFeatureReader.java 9066 2008-09-30 15:01:19Z Richard $
 */
package nl.b3p.geotools.data.sdl;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureReader;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.commons.io.input.CountingInputStream;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Matthijs Laan, B3Partners
 */
public class SDLFeatureReader implements FeatureReader {

    private GeometryFactory gf;
    private SimpleFeatureType ft;
    private CountingInputStream cis;
    private LineNumberReader lnr;
    private String version;
    private Map<String, String[]> metadata = new HashMap<String, String[]>();
    private static final int MARK_SIZE = 8 * 1024;
    private int featureID = 0;

    public SDLFeatureReader(URL url, String typeName, String srs) throws IOException, SDLParseException {

        /* TODO for loading large files, obtain a total stream size from somewhere
         * and use an apache commons CountingInputStream to provide current
         * progress info.
         */

        /* Note that a LineNumberReader may read more bytes than are strictly
         * returned as characters of lines read.
         */
        this.cis = new CountingInputStream(url.openStream());

        /* TODO provide param to override encoding! This uses the platform
         * default encoding, SDF Loader Help doesn't specify encoding
         */
        this.lnr = new LineNumberReader(new InputStreamReader(cis));

        parseHeader();
        skipCommentsCheckEOF();
        createFeatureType(typeName, srs);
    }

    private void parseHeader() throws IOException {
        skipCommentsCheckEOF();
        for (;;) {
            /* mark the start of the next line */
            lnr.mark(MARK_SIZE);
            String line = lnr.readLine();
            if (line == null) {
                /* eof in or before header, empty file? */
                break;
            }
            if (line.trim().length() != 0) {
                int firstChar = line.charAt(0);
                if (firstChar != '#') {
                    /* end of headers, reset stream */
                    lnr.reset();
                    break;
                }

                /* handle header line */
                String lcline = line.toLowerCase();
                if (lcline.startsWith("#version")) {
                    version = line.substring(line.indexOf('=') + 1);
                } else if (lcline.startsWith("#metadata_begin")) {
                    /* use the lowercase name as map key, case insensitive */
                    String name = lcline.substring(line.indexOf('=') + 1);
                    List<String> contents = new ArrayList<String>();
                    String headerLine;
                    while ((headerLine = lnr.readLine()) != null) {
                        if (headerLine.toLowerCase().startsWith("#metadata_end")) {
                            break;
                        }
                        contents.add(headerLine.substring(1));
                    }
                    if (!contents.isEmpty()) {
                        metadata.put(name, contents.toArray(new String[]{}));
                    }
                }
            } else {
                /* skip empty line */
            }
        }
    }

    private void createFeatureType(String typeName, String srs) throws DataSourceException {
        CoordinateReferenceSystem crs = null;
        String[] csMetadata = metadata.get("coordinatesystem");
        if (csMetadata != null) {
            String wkt = csMetadata[0];
            try {
                /* parse WKT */
                CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
                crs = crsFactory.createFromWKT(wkt);
            } catch (Exception e) {
                throw new DataSourceException("Error parsing CoordinateSystem WKT: \"" + wkt + "\"");
            }
        }

        /* override srs when provided */
        if (srs != null) {
            try {
                crs = CRS.decode(srs);
            } catch (Exception e) {
                throw new DataSourceException("Error parsing CoordinateSystem srs: \"" + srs + "\"");
            }
        }

        try {

            SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
            ftb.setName(typeName);
            ftb.setCRS(crs);

            ftb.add("the_geom", Geometry.class);
            ftb.add("name", String.class);
            ftb.add("key", String.class);
            ftb.add("urlLink", String.class);
            ftb.add("entryLineNumber", Integer.class);
            ftb.add("parseError", Integer.class);
            ftb.add("error", String.class);

            ft = ftb.buildFeatureType();

            //GeometricAttributeType geometryType = new GeometricAttributeType("the_geom", Geometry.class, true, null, crs, null);
            gf = new GeometryFactory();

            /*
            gf = geometryType.getGeometryFactory();

            ft = FeatureTypes.newFeatureType(
            new AttributeType[]{
            geometryType,
            AttributeTypeFactory.newAttributeType("name", String.class),
            AttributeTypeFactory.newAttributeType("key", String.class),
            AttributeTypeFactory.newAttributeType("urlLink", String.class),
            AttributeTypeFactory.newAttributeType("entryLineNumber", Integer.class),
            AttributeTypeFactory.newAttributeType("parseError", Integer.class),
            AttributeTypeFactory.newAttributeType("error", String.class)
            }, typeName);
             */
        } catch (Exception e) {
            throw new DataSourceException("Error creating SimpleFeatureType", e);
        }
    }

    public SimpleFeatureType getFeatureType() {
        return ft;
    }

    /**
     * Skip empty and comment lines and return EOF status
     * @return true if EOF
     * @throws java.io.IOException
     */
    private boolean skipCommentsCheckEOF() throws IOException {
        String line;
        do {
            /* mark the start of the next line */
            lnr.mark(MARK_SIZE);
            line = lnr.readLine();
            if (line == null) {
                /* skipped comments till end of file */
                return true;
            }
        } while (line.length() == 0 || line.charAt(0) == ';');

        /* EOF or the last line we read wasn't a comment or empty line. reset 
         * the stream so the next readLine() call will return the line we just
         * read
         */
        lnr.reset();
        return false;
    }

    public SimpleFeature next() throws IOException, IllegalAttributeException, NoSuchElementException {
        try {
            SDLEntry entry = new SDLEntry(lnr, gf);
            Geometry g = entry.getGeometry();

            SimpleFeature f = SimpleFeatureBuilder.build(ft, new Object[]{
                        g,
                        entry.getName(),
                        entry.getKey(),
                        entry.getUrlLink(),
                        new Integer(entry.getStartingLineNumber()),
                        new Integer(entry.isParseError() ? 1 : 0),
                        entry.getErrorDescription()
                    }, Integer.toString(featureID++));

            return f;
        } catch (SDLParseException ex) {
            throw new IOException("SDL parse error" + ex.getLocalizedMessage());
        } catch (EOFException e) {
            return null;
        }
    }

    public boolean hasNext() throws IOException {
        /* this method should be fast as it will probably be called before each 
         * next(). skipCommentsCheckEOF will mark()/reset() the stream
         */
        return !skipCommentsCheckEOF();
    }

    public void close() throws IOException {
        lnr.close();
    }

    public long getByteCount() {
        return cis.getByteCount();
    }
}
