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

import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.Directory;

import proj.zoie.api.ZoieVersion;
import proj.zoie.api.DocIDMapper;
import proj.zoie.api.ZoieIndexReader;
import proj.zoie.api.ZoieMultiReader;
import proj.zoie.api.impl.ZoieMergePolicy;
import proj.zoie.api.impl.ZoieMergePolicy.MergePolicyParams;
import proj.zoie.api.impl.util.FileUtil;
import proj.zoie.api.impl.util.IndexUtil;
import proj.zoie.api.indexing.IndexReaderDecorator;

public class RAMSearchIndex<R extends IndexReader, V extends ZoieVersion> extends BaseSearchIndex<R, V>
{
  private V _version;
  private final Directory _directory;
  private final File _backingdir;
  private final IndexReaderDecorator<R> _decorator;

  // a consistent pair of reader and deleted set
  private volatile ZoieIndexReader<R> _currentReader;
  private final MergePolicyParams _mergePolicyParams;

  public static final Logger log = Logger.getLogger(RAMSearchIndex.class);

  public RAMSearchIndex(V version, IndexReaderDecorator<R> decorator, SearchIndexManager<R, V> idxMgr, Directory ramIdxDir,
      File backingdir)
  {
    super(idxMgr, true);
    _directory = ramIdxDir;
    _backingdir = backingdir;
    _version = version;
    _decorator = decorator;
    _currentReader = null;
    _mergeScheduler = new SerialMergeScheduler();
    _mergePolicyParams = new MergePolicyParams();
    _mergePolicyParams.setNumLargeSegments(3);
    _mergePolicyParams.setMergeFactor(3);
    _mergePolicyParams.setMaxSmallSegments(4);
  }

  public void close()
  {
    super.close();
    if (_currentReader != null)
    {
      _currentReader.decZoieRef();
    }
    if (_directory != null)
    {
      try
      {
        _directory.close();
        if (_backingdir != null)
          FileUtil.rmDir(_backingdir);
      } catch (IOException e)
      {
        log.error(e);
      }
    }
  }

  public V getVersion()
  {
    return _version;
  }

  public void setVersion(V version) throws IOException
  {
    _version = version;
  }

  public int getNumdocs()
  {
    ZoieIndexReader<R> reader = null;
    try
    {
      reader = openIndexReader();
    } catch (IOException e)
    {
      log.error(e.getMessage(), e);
    }

    if (reader != null)
    {
      return reader.numDocs();
    } else
    {
      return 0;
    }
  }

  @Override
  public ZoieIndexReader<R> openIndexReader() throws IOException
  {
    return _currentReader;
  }

  @Override
  protected IndexReader openIndexReaderForDelete() throws IOException
  {
    if (IndexReader.indexExists(_directory))
    {
      return IndexReader.open(_directory, false);
    } else
    {
      return null;
    }
  }

  private ZoieIndexReader<R> openIndexReaderInternal() throws IOException
  {
    if (IndexReader.indexExists(_directory))
    {
      IndexReader srcReader = null;
      ZoieIndexReader<R> finalReader = null;
      try
      {
        // for RAM indexes, just get a new index reader
        srcReader = IndexReader.open(_directory, true);
        finalReader = ZoieIndexReader.open(srcReader, _decorator);
        DocIDMapper mapper = _idxMgr._docIDMapperFactory.getDocIDMapper((ZoieMultiReader<R>) finalReader);
        finalReader.setDocIDMapper(mapper);
        return finalReader;
      } catch (IOException ioe)
      {
        // if reader decoration fails, still need to close the source reader
        if (srcReader != null)
        {
          srcReader.close();
        }
        throw ioe;
      }
    } else
    {
      return null; // null indicates no index exist, following the contract
    }
  }

  public IndexWriter openIndexWriter(Analyzer analyzer, Similarity similarity) throws IOException
  {
    if (_indexWriter != null)
      return _indexWriter;

    // if index does not exist, create empty index
    boolean create = !IndexReader.indexExists(_directory);
    // hao: autocommit is set to false with this constructor
    IndexWriter idxWriter = new IndexWriter(_directory, analyzer, create, MaxFieldLength.UNLIMITED);
    // TODO disable compound file for RAMDirecory when lucene bug is fixed
    idxWriter.setUseCompoundFile(false);
    idxWriter.setMergeScheduler(_mergeScheduler);
    ZoieMergePolicy mergePolicy = new ZoieMergePolicy(idxWriter);
    mergePolicy.setMergePolicyParams(_mergePolicyParams);
    idxWriter.setMergePolicy(mergePolicy);
    idxWriter.setRAMBufferSizeMB(3);

    if (similarity != null)
    {
      idxWriter.setSimilarity(similarity);
    }
    _indexWriter = idxWriter;
    return idxWriter;
  }

  @Override
  public void refresh() throws IOException
  {
    synchronized (this)
    {
      ZoieIndexReader<R> reader = null;
      if (_currentReader == null)
      {
        reader = openIndexReaderInternal();
      } else
      {
        reader = (ZoieIndexReader<R>) _currentReader.reopen(true);
        if (reader != _currentReader)
        {
          DocIDMapper mapper = _idxMgr._docIDMapperFactory.getDocIDMapper((ZoieMultiReader<R>) reader);
          reader.setDocIDMapper(mapper);
        }
      }

      if (_currentReader != reader)
      {
        ZoieIndexReader<R> oldReader = _currentReader;
        _currentReader = reader;
        if (oldReader != null)
          ((ZoieIndexReader) oldReader).decZoieRef();// .decRef();
      }
      LongSet delDocs = _delDocs;
      clearDeletes();
      markDeletes(delDocs); // re-mark deletes
      commitDeletes();
    }
  }

  public int getSegmentCount() throws IOException
  {
    return _directory == null ? -1 : IndexUtil.getNumSegments(_directory);
  }
}
