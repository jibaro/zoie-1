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

public class MySearchServiceImpl {
	private static final Logger log = Logger
			.getLogger(MySearchServiceImpl.class);

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

	public static void searchAndDisplay(String queryString,
			IndexReaderFactory _idxReaderFactory) throws ZoieException {
		Analyzer analyzer = _idxReaderFactory.getAnalyzer();
		QueryParser qparser = new QueryParser(Version.LUCENE_CURRENT, "body",
				analyzer);

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

			Document document = new Document();

			for (ScoreDoc scoreDoc : docs.scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc);
				System.out.println("DOCID:" + scoreDoc.doc + " ; " + "SCORE:"
						+ scoreDoc.score + " TITLE:" + doc.get("title")
						+ " DATE:" + doc.get("date"));
				System.out.println("body:\n" + doc.get("body"));
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
	}

	public static void main(String[] args) throws ZoieException,
			InterruptedException {
		ZoieSystem zSystem = ZoieSystemFactory.getMySpecifiedInstance();
		MyDataProvider dProvider = new MyDataProvider(new File("datasource"));
		dProvider.setDataConsumer(zSystem);
		dProvider.start();
		zSystem.start();
		while (true) {
			searchAndDisplay("book", zSystem);
			//Thread.sleep(10000);
		}
		// zSystem.consume(data);

	}
}
