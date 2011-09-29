package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexOutput;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.browseengine.bobo.geosearch.GeoVersion;
import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;


/**
 * @author Geoff Cooney
 */
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration( { "/TEST-servlet.xml" }) 
@IfProfileValue(name = "test-suite", values = { "unit", "all" }) 
public class GeoSegmentWriterTest {
    Mockery context = new Mockery() {{ 
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    Sequence outputSequence = context.sequence("output");
    
    Directory directory;
    IndexOutput mockOutput;
    
    //@Resource(type = GeoSearchConfig.class)
    GeoSearchConfig config = new GeoSearchConfig();
    
    TreeSet<GeoRecord> treeSet;
    GeoSegmentInfo info;

    IFieldNameFilterConverter fieldNameFilterConverter;
    
    @Before
    public void setUp() {
        directory = context.mock(Directory.class);
        mockOutput = context.mock(IndexOutput.class);
        fieldNameFilterConverter = context.mock(IFieldNameFilterConverter.class);
        
        config.setGeoFileExtension("gto");
        config.setFieldNameFilterConverter(fieldNameFilterConverter);
        
        treeSet = new TreeSet<GeoRecord>(new Comparator<GeoRecord>() {

            @Override
            public int compare(GeoRecord o1, GeoRecord o2) {
                return o1.lowOrder - o2.lowOrder;
            }
        });
        
        String segmentName = "01";
        info = new GeoSegmentInfo();
        info.setFieldNameFilterConverter(fieldNameFilterConverter);
        info.setGeoVersion(GeoVersion.CURRENT_VERSION);
        info.setSegmentName(segmentName);
    }
    
    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
    
    @Test
    public void createOutputBTree() throws IOException {
        final int docsToAdd = 20;
        
        for (int i=0; i<docsToAdd; i++) {
            long highOrder = 0;
            int lowOrder = i;
            byte filterByte = GeoRecord.DEFAULT_FILTER_BYTE; 
            
            GeoRecord record = new GeoRecord(highOrder, lowOrder, filterByte);
            treeSet.add(record);
        }
        
        doCreateAndTest(docsToAdd);
    }
    
    @Test
    public void createOutputBTree_WithValueMapping() throws IOException {
        info.setFieldNameFilterConverter(fieldNameFilterConverter);
        
        final int docsToAdd = 20;
        
        for (int i=0; i<docsToAdd; i++) {
            long highOrder = 0;
            int lowOrder = i;
            byte filterByte = GeoRecord.DEFAULT_FILTER_BYTE; 
            
            GeoRecord record = new GeoRecord(highOrder, lowOrder, filterByte);
            treeSet.add(record);
        }
        
        doCreateAndTest(docsToAdd);
    }
    
    private void doCreateAndTest(final int docsToAdd) throws IOException {
        final byte[] byteBuf = new byte[10];
        final Class<?> clazz = byteBuf.getClass();
        
        context.assertIsSatisfied();  //we should have no calls to mock Objects before we flush
        context.checking(new Expectations() {
            {
                ignoring(mockOutput).getFilePointer();
                will(returnValue(1L));
                
                //get output
                one(directory).createOutput(info.getSegmentName() + "." + config.getGeoFileExtension());
                will(returnValue(mockOutput));
                inSequence(outputSequence);

                //write file header
                one(mockOutput).writeVInt(GeoVersion.CURRENT_VERSION);
                inSequence(outputSequence);
                one(mockOutput).writeInt(0);
                inSequence(outputSequence);
                one(mockOutput).writeVInt(docsToAdd);
                inSequence(outputSequence);
                one(fieldNameFilterConverter).writeToOutput(mockOutput);
                inSequence(outputSequence);
                
                one(mockOutput).seek(1);
                inSequence(outputSequence);
                one(mockOutput).writeInt(1);
                inSequence(outputSequence);
                
                // fill zeroes
                one(mockOutput).length();
                will(returnValue(7L));
                inSequence(outputSequence);
                one(mockOutput).seek(with(any(Long.class)));
                inSequence(outputSequence);
                one(mockOutput).writeBytes(with(any(byteBuf.getClass())), with(any(Integer.TYPE)), with(any(Integer.TYPE)));
                inSequence(outputSequence);
                one(mockOutput).seek(with(any(Long.class)));
                inSequence(outputSequence);
                one(mockOutput).length();
                will(returnValue((long)(7+13*docsToAdd)));
                inSequence(outputSequence);
                
                //write actual tree
                for (int i = 0; i < docsToAdd; i++) {
                    
                    one(mockOutput).seek(with(any(Long.class)));
                    inSequence(outputSequence);
                    one(mockOutput).writeLong(0);
                    inSequence(outputSequence);
                    one(mockOutput).writeInt(i);
                    inSequence(outputSequence);
                    one(mockOutput).writeByte(GeoRecord.DEFAULT_FILTER_BYTE);
                    inSequence(outputSequence);
                }
                
                //close
                one(mockOutput).close();
            }
        });
        
        String fileName = config.getGeoFileName(info.getSegmentName());
        GeoSegmentWriter bTree = new GeoSegmentWriter(treeSet, directory, fileName, info);
        bTree.close();
        context.assertIsSatisfied();
    }
}
