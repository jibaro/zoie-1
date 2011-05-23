package bit.lin.myimpl;

import java.util.Date;

public class MyDoc {
	private long _id;
	private String _title;
	private String _body;
	private Date _date;

	public MyDoc() {
	}

	public MyDoc(String title, String body, Date date, long id) {
		_title = title;
		_body = body;
		_date = date;
		_id = id;
	}

	public long getId() {
		return _id;
	}

	public void setId(long id) {
		this._id = id;
	}

	public String getTitle() {
		return _title;
	}

	public void setTitle(String title) {
		this._title = title;
	}

	public String getBody() {
		return _body;
	}

	public void setBody(String body) {
		this._body = body;
	}

	public Date getDate() {
		return _date;
	}

	public void setDate(Date date) {
		this._date = date;
	}
}
