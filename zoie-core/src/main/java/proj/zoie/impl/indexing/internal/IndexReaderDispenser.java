package proj.zoie.impl.indexing.internal;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

import proj.zoie.api.ZoieVersion;
import proj.zoie.api.DirectoryManager;
import proj.zoie.api.DocIDMapper;
import proj.zoie.api.ZoieHealth;
import proj.zoie.api.ZoieIndexReader;
import proj.zoie.api.ZoieMultiReader;
import proj.zoie.api.indexing.IndexReaderDecorator;

public class IndexReaderDispenser<R extends IndexReader, V extends ZoieVersion>
{
  private static final Logger log = Logger.getLogger(IndexReaderDispenser.class);
  
  private static final int INDEX_OPEN_NUM_RETRIES=5;
  
//  public static final String  INDEX_DIRECTORY = "index.directory";
  
  static final class InternalIndexReader<R extends IndexReader, V extends ZoieVersion> extends ZoieMultiReader<R>
  {
    //private IndexSignature _sig;
    private final IndexReaderDispenser<R,V> _dispenser;

    InternalIndexReader(IndexReader in,IndexReaderDecorator<R> decorator,IndexReaderDispenser<R,V> dispenser) throws IOException
    {
      super(in, decorator);
      _dispenser = dispenser;
    }

    public InternalIndexReader(IndexReader in, IndexReader[] subReaders, IndexReaderDecorator<R> decorator,IndexReaderDispenser<R,V> dispenser) throws IOException
    {
      super(in, subReaders, decorator);
      _dispenser = dispenser;
    }

    @Override
    protected ZoieMultiReader<R> newInstance(IndexReader inner, IndexReader[] subReaders) throws IOException
    {
      return new InternalIndexReader<R,V>(inner,subReaders,_decorator,_dispenser);
    }
  }

  private volatile InternalIndexReader<R,V> _currentReader;
  private volatile IndexSignature<V> _currentSignature;
  private final IndexReaderDecorator<R> _decorator;
  private final DirectoryManager<V> _dirMgr;
  private DiskSearchIndex<R,V> _idx;
  
  public IndexReaderDispenser(DirectoryManager<V> dirMgr, IndexReaderDecorator<R> decorator,DiskSearchIndex<R,V> idx)
  {
      _idx = idx;
    _dirMgr = dirMgr;
    _decorator = decorator;
    _currentSignature = null;
    try
    {
      IndexSignature<V> sig = new IndexSignature<V>(_dirMgr.getVersion());
      if(sig != null)
      {
        getNewReader();
      }
    }
    catch (IOException e)
    {
      log.error(e);
    }
  }
  
  public V getCurrentVersion()
  {
    return _currentSignature!=null ? _currentSignature.getVersion(): null;
  }
  
  /**
     * constructs a new IndexReader instance
     * 
     * @param indexPath
     *            Where the index is.
     * @return Constructed IndexReader instance.
     * @throws IOException
     */
    private InternalIndexReader<R,V> newReader(DirectoryManager<V> dirMgr, IndexReaderDecorator<R> decorator, IndexSignature<V> signature)
        throws IOException
    {
      if (!dirMgr.exists()){
        return null;
      }
      
    Directory dir= dirMgr.getDirectory();
    
    if (!IndexReader.indexExists(dir)){
      return null;
    }
    
      int numTries=INDEX_OPEN_NUM_RETRIES;
      InternalIndexReader<R,V> reader=null;
      
      // try max of 5 times, there might be a case where the segment file is being updated
      while(reader==null)
      {
        if (numTries==0)
        {
          log.error("Problem refreshing disk index, all attempts failed.");
          throw new IOException("problem opening new index");
        }
        numTries--;
        
        try{
          if(log.isDebugEnabled())
          {
            log.debug("opening index reader at: "+dirMgr.getPath());
          }
          IndexReader srcReader = IndexReader.open(dir,true);
          
          try
          {
            reader=new InternalIndexReader<R,V>(srcReader, decorator,this);
            _currentSignature = signature;
          }
          catch(IOException ioe)
          {
            // close the source reader if InternalIndexReader construction fails
            if (srcReader!=null)
            {
              srcReader.close();
            }
            throw ioe;
          }
        }
        catch(IOException ioe)
        {
          try
          {
            Thread.sleep(100);
          }
          catch (InterruptedException e)
          {
            log.warn("thread interrupted.");
            continue;
          }
        }
      }
      return reader;
    }

    /**
     * get a fresh new reader instance
     * @return an IndexReader instance, can be null if index does not yet exit
     * @throws IOException
     */
    public ZoieIndexReader<R> getNewReader() throws IOException
    {
        int numTries=INDEX_OPEN_NUM_RETRIES;   
        InternalIndexReader<R,V> reader=null;
              
        // try it for a few times, there is a case where lucene is swapping the segment file, 
        // or a case where the index directory file is updated, both are legitimate,
        // trying again does not block searchers,
        // the extra time it takes to get the reader, and to sync the index, memory index is collecting docs
       
    while(reader==null)
    {
      if (numTries==0)
      {
        break;
      }
      numTries--;
      try{
        IndexSignature<V> sig = new IndexSignature<V>(_dirMgr.getVersion());
  
        if (sig==null)
        {
          throw new IOException("no index exist");
        }
        
        if (_currentReader==null){
          reader = newReader(_dirMgr, _decorator, sig);
            break;
        }
        else{
          reader = (InternalIndexReader<R,V>)_currentReader.reopen(true);
          _currentSignature = sig;
        }
      }
      catch(IOException ioe)
      {
        try
        {
          Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
        log.warn("thread interrupted.");
          continue;
        }
      }
    }

    // swap the internal readers
    if (_currentReader != reader)
    {
      if (reader!=null){
        DocIDMapper mapper = _idx._idxMgr._docIDMapperFactory.getDocIDMapper((ZoieMultiReader<R>)reader);
        reader.setDocIDMapper(mapper);
      }
      // assume that this is the only place that _currentReader gets refreshed 
      IndexReader oldReader = _currentReader;
      _currentReader = reader;
      // we release our hold on the old reader so that it will be closed when
      // all the clients release their hold on it, the reader will be closed
      // automatically.
      log.info("swap disk reader and release old one from system");
      if (oldReader !=null) ((ZoieIndexReader)oldReader).decZoieRef();//.decRef();
    }
    return reader;
  }
  
  public ZoieIndexReader<R> getIndexReader()
  {
    if (_currentReader!=null){
      return _currentReader;
    }
    else{
      return null;
    }
  }
      
  /**
   * Closes the factory.
   * 
   */
  public void close()
  {
    closeReader();
  }
  
  /**
   * Closes the index reader
   */
  public void closeReader()
  {
    if(_currentReader != null)
    {
      try
      {
        _currentReader.decRef();
        int count = _currentReader.getRefCount();
        log.info("final closeReader in dispenser and current refCount: " + count + " at " + _currentReader.directory());
        if (count > 0)
        {
          log.warn("final closeReader call with reference count == " + count + " greater than 0. Potentially, " +
              "the IndexReaders are not properly return to ZoieSystem.");
        }
      }
      catch(IOException e)
      {
        ZoieHealth.setFatal();
        log.error("problem closing reader", e);
      }
      _currentReader = null;
    }
  }
}
