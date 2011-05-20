package proj.zoie.test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.junit.After;

import proj.zoie.api.DefaultZoieVersion;
import proj.zoie.api.DocIDMapperFactory;
import proj.zoie.api.ZoieIndexReader;
import proj.zoie.api.ZoieVersionFactory;
import proj.zoie.api.impl.InRangeDocIDMapperFactory;
import proj.zoie.api.indexing.IndexReaderDecorator;
import proj.zoie.impl.indexing.ZoieSystem;
import proj.zoie.test.data.DataInterpreterForTests;
import proj.zoie.test.data.InRangeDataInterpreterForTests;


public class ZoieTestCaseBase
{
  static Logger log = Logger.getLogger(ZoieTestCaseBase.class);
  static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
  public void registerMBean(Object standardmbean, String mbeanname)
  {
    try
    {
      mbeanServer.registerMBean(standardmbean, new ObjectName("Zoie:name=" + mbeanname));
    } catch (Exception e)
    {
      log.warn(e);
    }
  }

  public void unregisterMBean(String mbeanname)
  {
    try
    {
      mbeanServer.unregisterMBean(new ObjectName("Zoie:name=" + mbeanname));
    } catch (Exception e)
    {
      log.warn(e);
    }
  }


  @After
  public void tearDown()
  {
    deleteDirectory(getIdxDir());
  }
  
  protected static File getIdxDir()
  {
    File tmpDir=new File(System.getProperty("java.io.tmpdir"));
    File tempFile = new File(tmpDir, "test-idx");
    int i = 0;
    while (tempFile.exists())
    {
      if (i>10)
      {
        log.info("cannot delete");
        return tempFile;
      }
      log.info("deleting " + tempFile);
      deleteDirectory(tempFile);
//      tempFile.delete();
      try
      {
        Thread.sleep(50);
      } catch(Exception e)
      {
        log.error("thread interrupted in sleep in deleting file" + e);
      }
      i++;
    }
    return tempFile;
  }

  protected static File getTmpDir()
  {
    return new File(System.getProperty("java.io.tmpdir"));
  }

  protected static ZoieSystem<IndexReader,String, DefaultZoieVersion> createZoie(File idxDir,boolean realtime, ZoieVersionFactory<DefaultZoieVersion> zoieVersionFactory)
  {
    return createZoie(idxDir, realtime, 20,zoieVersionFactory);
  }
  
  /**
   * @param idxDir
   * @param realtime
   * @param delay delay for interpreter (simulating a slow interpreter)
   * @param zoieVersionFactory
   * @return
   */
  protected static ZoieSystem<IndexReader,String, DefaultZoieVersion> createZoie(File idxDir,boolean realtime, long delay, ZoieVersionFactory<DefaultZoieVersion> zoieVersionFactory)
  {
    return createZoie(idxDir,realtime,delay,null,null,zoieVersionFactory);
  }

  protected static ZoieSystem<IndexReader,String, DefaultZoieVersion> createZoie(File idxDir,boolean realtime,DocIDMapperFactory docidMapperFactory,ZoieVersionFactory<DefaultZoieVersion> zoieVersionFactory)
  {
    return createZoie(idxDir, realtime, 2,null,docidMapperFactory, zoieVersionFactory);
  }

  /**
   * @param idxDir
   * @param realtime
   * @param delay delay for interpreter (simulating a slow interpreter)
   * @param analyzer
   * @param docidMapperFactory
   * @param zoieVersionFactory
   * @return
   */
  protected static ZoieSystem<IndexReader,String, DefaultZoieVersion> createZoie(File idxDir,boolean realtime, long delay,Analyzer analyzer,DocIDMapperFactory docidMapperFactory, ZoieVersionFactory<DefaultZoieVersion> zoieVersionFactory)
  {
    ZoieSystem<IndexReader,String,DefaultZoieVersion> idxSystem=new ZoieSystem<IndexReader, String, DefaultZoieVersion>(idxDir,new DataInterpreterForTests(delay,analyzer),
        new TestIndexReaderDecorator(),docidMapperFactory, null,null,50,2000,realtime,zoieVersionFactory);
    return idxSystem;
  }


  protected static class TestIndexReaderDecorator implements IndexReaderDecorator<IndexReader>{
    public IndexReader decorate(ZoieIndexReader<IndexReader> indexReader) throws IOException {
      return indexReader;
    }

    public IndexReader redecorate(IndexReader decorated,ZoieIndexReader<IndexReader> copy,boolean withDeletes) throws IOException {
      return decorated;
    }


    public void setDeleteSet(IndexReader reader, DocIdSet docIds)
    {
      // do nothing
    }
  }

  protected static ZoieSystem<IndexReader,String,DefaultZoieVersion> createInRangeZoie(File idxDir,boolean realtime, InRangeDocIDMapperFactory docidMapperFactory, ZoieVersionFactory<DefaultZoieVersion> zoieVersionFactory)
  {
    ZoieSystem<IndexReader,String,DefaultZoieVersion> idxSystem=new ZoieSystem<IndexReader, String,DefaultZoieVersion>(idxDir,new InRangeDataInterpreterForTests(20,null),
        new TestIndexReaderDecorator(),docidMapperFactory,null,null,50,2000,realtime,zoieVersionFactory);
    return idxSystem;
  } 
  protected static boolean deleteDirectory(File path) {
    if( path.exists() ) {
      File[] files = path.listFiles();
      for(int i=0; i<files.length; i++) {
        if(files[i].isDirectory()) {
          deleteDirectory(files[i]);
        }
        else {
          files[i].delete();
        }
      }
    }
    return( path.delete() );
  }

  
  protected static class QueryThread extends Thread
  {
    public volatile boolean stop = false;
    public volatile boolean mismatch = false;
    public volatile String message = null;
    public Exception exception = null;
  }

}
