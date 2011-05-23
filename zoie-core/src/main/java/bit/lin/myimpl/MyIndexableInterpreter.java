package bit.lin.myimpl;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import proj.zoie.api.indexing.ZoieIndexable;
import proj.zoie.api.indexing.ZoieIndexableInterpreter;

public class MyIndexableInterpreter implements ZoieIndexableInterpreter<MyDoc> {
	private ThreadLocal<SimpleDateFormat> _formatter = new ThreadLocal<SimpleDateFormat>() {
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss.SSS");
		}
	};

	protected class MyIndexable implements ZoieIndexable {
		private MyDoc _myDoc;

		public MyIndexable(MyDoc myDoc) {
			_myDoc = myDoc;
		}

		@Override
		public long getUID() {
			return _myDoc.getId();
		}

		@Override
		public boolean isDeleted() {
			return false;
		}

		@Override
		public boolean isSkip() {
			return false;
		}

		private Document buildDoc(){
			Document doc = new Document();

			String tmp;
			tmp = _myDoc.getTitle();
			if (tmp != null) {
				doc.add(new Field("title", tmp, Field.Store.NO,
						Field.Index.ANALYZED));
			}

			tmp = _myDoc.getBody();
			if (tmp != null) {
				doc.add(new Field("body", tmp, Field.Store.NO,
						Field.Index.ANALYZED));
			}

			Date date = _myDoc.getDate();
			if (date != null) {
				Field field = new Field("date", _formatter.get().format(date),
						Field.Store.NO, Field.Index.ANALYZED);
				field.setOmitTermFreqAndPositions(true);
				doc.add(field);
			}
			return doc;
		}

		@Override
		public IndexingReq[] buildIndexingReqs() {
			IndexingReq req = new IndexingReq(buildDoc());
			return new IndexingReq[]{req};
		}
	}

	@Override
	public ZoieIndexable convertAndInterpret(MyDoc src) {
		return new MyIndexable(src);
	}

}