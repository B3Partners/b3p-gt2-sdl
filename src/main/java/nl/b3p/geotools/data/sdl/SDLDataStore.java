/*
 * $Id: SDLDataStore.java 8672 2008-07-17 16:37:57Z Matthijs $
 */
package nl.b3p.geotools.data.sdl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.ServiceInfo;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;


/**
 * DataStore for reading a SDL file produced by Autodesk SDF Loader which
 * supports the legacy SDF format (which FDO/MapGuide Open Source can't read).
 * The SDF component toolkit can read those but COM objects are perhaps a lesser
 * evil than Runtime.exec()'ing the SDF Loader.
 *
 * Note that a single SDL file can contain point, line and polygon
 * SimpleFeatures. Although many files will only contain a single type, the
 * parser can only determine this by looking through the entire file - which is
 * not advisable in a streaming API. The same is true for a file containing only
 * polygons or also multipolygons etc.
 *
 * Therefore always the same SimpleFeature schema is used: the_geom_point: Point
 * (SDL does not contains MultiPoints) the_geom_line: MultiLineString
 * (getNumGeometries() can be 1) the_geom_polygon: MultiPolygons
 * (getNumGeometries() can be 1, can contain holes) Where only one of three is
 * not null. The attributes are always the same: key: String name: String
 * urlLink: String entryLineNumber: Integer parseError: Boolean error: String
 *
 * Note that especially polygons can contain parse errors due to randomly
 * duplicated coordinates. Not much that can be done about that, because
 * sometimes it is not possible to determine if the coordinate is duplicated or
 * a closing coordinate of a subgeometry.
 *
 * See the SDF Loader Help for the description of the SDL file format.
 *
 * @author Matthijs Laan, B3Partners
 * @author mprins
 */
public class SDLDataStore implements FileDataStore {

    private static final Log log = LogFactory.getLog(SDLDataStore.class);
    private URL url;
    private String typeName;
    private FeatureReader FeatureReader;
    private String srs;

    public SDLDataStore(URL url, String srs) throws IOException {
        this.url = url;
        this.typeName = getURLTypeName(url);
        this.srs = srs;
    }

    public String[] getTypeNames() throws IOException {
        return new String[]{getURLTypeName(url)};
    }

    public List<Name> getNames() throws IOException {
        return Arrays.asList((Name) new NameImpl(getSchema().getTypeName()));
    }

    static String getURLTypeName(URL url) throws IOException {
        String file = url.getFile();
        if (file.length() == 0) {
            return "unknown_sdl";
        } else {
            int i = file.lastIndexOf('/');
            if (i != -1) {
                file = file.substring(i + 1);
            }
            if (file.toLowerCase().endsWith(".sdl")) {
                file = file.substring(0, file.length() - 4);
            }
            return file;
        }
    }

    public SimpleFeatureType getSchema(String typeName) throws IOException {
        /* only one type */
        return getSchema();
    }

    public SimpleFeatureType getSchema(Name name) throws IOException {
        return getSchema();
    }

    public SimpleFeatureType getSchema() throws IOException {
        return (SimpleFeatureType) getFeatureReader().getFeatureType();
    }

    public FeatureReader getFeatureReader(String typeName) throws IOException {
        /* only one type */
        return getFeatureReader();
    }

    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query, Transaction transaction) throws IOException {
        return getFeatureReader();
    }

    public FeatureReader getFeatureReader() throws IOException {
        if (FeatureReader == null) {
            try {
                FeatureReader = new SDLFeatureReader(url, typeName, srs);
            } catch (SDLParseException e) {
                throw new IOException("SDL parse exception" + e.getLocalizedMessage());
            }
        }
        return FeatureReader;
    }


    public SimpleFeatureSource getFeatureSource() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void updateSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void removeSchema(String typeName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Filter filter, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Filter filter, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }

    public LockingManager getLockingManager() {
        throw new UnsupportedOperationException("Functie niet ondersteund voor alleen-lezen databron.");
    }


    public ServiceInfo getInfo() {
        DefaultServiceInfo serviceInfo = new DefaultServiceInfo();
        serviceInfo.setTitle("SDL DataStore");
        try {
            serviceInfo.setSource(this.url.toURI());
        } catch (URISyntaxException ex) {

        }
        return serviceInfo;
    }
    public void dispose() {
        try {
            this.FeatureReader.close();
        } catch (IOException | NullPointerException ex) {
            log.debug("Mogelijk probleem met sluiten van featureReader", ex);
        }
        this.FeatureReader = null;
    }
}
