package proj.zoie.mbean;

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
import java.util.Date;

import proj.zoie.api.ZoieException;

public interface ZoieSystemAdminMBean
{
  int getDiskIndexSize();

  long getDiskIndexSizeBytes();

  long getDiskFreeSpaceBytes();

  String getCurrentDiskVersion() throws IOException;

  int getRamAIndexSize();

  String getRamAVersion();

  int getRamBIndexSize();

  String getRamBVersion();

  String getDiskIndexerStatus();

  long getBatchDelay();

  void setBatchDelay(long delay);

  int getBatchSize();

  void setBatchSize(int batchSize);

  boolean isRealtime();

  String getIndexDir();

  void refreshDiskReader() throws IOException;

  Date getLastDiskIndexModifiedTime();

  Date getLastOptimizationTime();

  void optimize(int numSegs) throws IOException;

  void flushToDiskIndex() throws ZoieException;

  void flushToMemoryIndex() throws ZoieException;

  void purgeIndex() throws IOException;

  int getMaxBatchSize();

  void setMaxBatchSize(int maxBatchSize);

  void setMergeFactor(int mergeFactor);

  int getMergeFactor();

  void setNumLargeSegments(int numLargeSegments);

  int getNumLargeSegments();

  void setMaxSmallSegments(int maxSmallSegments);

  public int getMaxSmallSegments();

  void setMaxMergeDocs(int maxMergeDocs);

  int getMaxMergeDocs();

  void expungeDeletes() throws IOException;

  void setUseCompoundFile(boolean useCompoundFile);

  boolean isUseCompoundFile();

  int getDiskIndexSegmentCount() throws IOException;

  int getRAMASegmentCount();

  int getRAMBSegmentCount();

  int getCurrentMemBatchSize();

  int getCurrentDiskBatchSize();

  long getMinUID() throws IOException;

  long getMaxUID() throws IOException;
  
  /**
   * @return the response time threshold for getIndexReaders
   */
  long getSLA();
  
  /**
   * @param sla set the response time threshold (expected max response time) for getIndexReaders
   */
  void setSLA(long sla);
  
  /**
   * @return heahth of the system. Non-zero value means the system need immediate attention and the logs need to be checked.
   */
  long getHealth();
  
  void resetHealth();
  
  public long getFreshness();
  
  public void setFreshness(long freshness);
  
  /**
   * @return a String representation of the search result given the string representations of query arguments.
   */
  public String search(String field, String query);

  /**
   * @return the a String representation of the Document(s) referred to by the given UID
   */
  public String getDocument(long UID);
}
