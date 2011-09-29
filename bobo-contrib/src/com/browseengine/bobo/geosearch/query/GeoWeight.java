/**
 * 
 */
package com.browseengine.bobo.geosearch.query;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.geosearch.IDeletedDocs;
import com.browseengine.bobo.geosearch.impl.IndexReaderDeletedDocs;
import com.browseengine.bobo.geosearch.index.impl.GeoIndexReader;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;

/**
 * @author Shane Detsch
 * @author Ken McCracken
 *
 */
public class GeoWeight extends Weight {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private final GeoQuery geoQuery;

    public GeoWeight(GeoQuery geoQuery) {
        this.geoQuery = geoQuery;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException {
        // TODO: improve this to provide the actual distance component of the score, 
        // and explain how we take smoothed 1/distance^2.
        return new Explanation(doc, geoQuery.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query getQuery() {
        return geoQuery;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public float getValue() {
        // TODO: tune
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void normalize(float norm) {
        // TODO: tune
        // no-op
    }

    /**
     * {@inheritDoc}
     * 
     * The GeoScorer.nextDoc() should give you the capability
     *  to go through all the documents that are within 'rangeInMiles'
     *   of the centroid by increasing order of document id.
     */
    @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
            boolean topScorer) throws IOException {
        if (!(reader instanceof GeoIndexReader)) {
            throw new RuntimeException("attempt to create a "
                    +GeoScorer.class+" with a reader that was not a "
                    +GeoIndexReader.class);
        }
        GeoIndexReader geoIndexReader = (GeoIndexReader) reader;
        List<GeoSegmentReader> segmentsInOrder = geoIndexReader.getGeoSegmentReaders();
        IDeletedDocs wholeIndexDeletedDocs = new IndexReaderDeletedDocs(reader);

        double centroidLongitudeDegrees = geoQuery.getCentroidLongitude();
        double centroidLatitudeDegrees = geoQuery.getCentroidLatitude();
        float rangeInMiles = geoQuery.getRangeInMiles();
        return new GeoScorer(this, segmentsInOrder, wholeIndexDeletedDocs, 
                centroidLongitudeDegrees, centroidLatitudeDegrees, rangeInMiles);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float sumOfSquaredWeights() throws IOException {
        return 1;
    }
    
    
}
