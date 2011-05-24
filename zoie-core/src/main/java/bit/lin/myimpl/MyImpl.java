package bit.lin.myimpl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import proj.zoie.api.DataProvider;
import proj.zoie.api.DefaultZoieVersion;
import proj.zoie.api.IndexReaderFactory;
import proj.zoie.api.ZoieException;
import proj.zoie.api.ZoieIndexReader;
import proj.zoie.impl.indexing.FileDataProvider;
import proj.zoie.impl.indexing.FileIndexableInterpreter;
import proj.zoie.impl.indexing.SimpleZoieSystem;
import proj.zoie.impl.indexing.ZoieSystem;

public class MyImpl {
	private static final Logger log = Logger.getLogger(MyImpl.class);

	private static class ZoieSystemFactory {
		static ZoieSystem zSystem;

		public static ZoieSystem getMySpecifiedInstance() {
			if (zSystem == null) {
				zSystem = new SimpleZoieSystem(new File("index"),
						new MyIndexableInterpreter(), 1000, 300000,
						new DefaultZoieVersion.DefaultZoieVersionFactory());
			}
			return zSystem;
		}
	}

	public static boolean searchAndDisplay(String queryString,
			IndexReaderFactory _idxReaderFactory) throws ZoieException {
		Analyzer analyzer = _idxReaderFactory.getAnalyzer();
		QueryParser qparser = new QueryParser(Version.LUCENE_CURRENT,
				"content", analyzer);

		List<ZoieIndexReader> readers = null;

		MultiReader multiReader = null;
		Searcher searcher = null;
		try {
			Query q = null;
			if (queryString == null || queryString.length() == 0) {
				q = new MatchAllDocsQuery();
			} else {
				q = qparser.parse(queryString);
			}
			readers = _idxReaderFactory.getIndexReaders();
			multiReader = new MultiReader(
					readers.toArray(new IndexReader[readers.size()]), false);
			searcher = new IndexSearcher(multiReader);

			long start = System.currentTimeMillis();
			TopDocs docs = searcher.search(q, null, 10);
			long end = System.currentTimeMillis();
			
			if(docs==null)
				return false;
			Document document = new Document();

			for (ScoreDoc scoreDoc : docs.scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc);
				System.out.println("DocId:" + scoreDoc.doc + " ; " + "Score:"
						+ scoreDoc.score + "title:" + doc.get("title"));
				System.out.println("DocId:" + scoreDoc.doc + " ; " + "Score:"
						+ scoreDoc.score + "body:" + doc.get("body"));
				System.out.println("DocId:" + scoreDoc.doc + " ; " + "Score:"
						+ scoreDoc.score + "date:" + doc.get("date"));
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new ZoieException(e.getMessage(), e);
		} finally {
			try {
				if (searcher != null) {
					try {
						searcher.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					} finally {
						if (multiReader != null) {
							try {
								multiReader.close();
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
						}
					}
				}
			} finally {
				_idxReaderFactory.returnIndexReaders(readers);
			}
		}
		return true;
	}

	public static void main(String[] args) throws ZoieException {
		ZoieSystem zSystem = ZoieSystemFactory.getMySpecifiedInstance();
		MyDataProvider dProvider = new MyDataProvider(new File("datasource"));
		dProvider.setDataConsumer(zSystem);
		dProvider.start();
		zSystem.start();
		while(!searchAndDisplay("further", zSystem))
			;
		// zSystem.consume(data);

	}
}
