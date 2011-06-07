package proj.zoie.hourglass.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;

import proj.zoie.api.ZoieException;
import proj.zoie.api.ZoieIndexReader;
import proj.zoie.api.ZoieVersion;
import proj.zoie.api.indexing.IndexReaderDecorator;
import proj.zoie.impl.indexing.ZoieSystem;

public class Box<R extends IndexReader, D, V extends ZoieVersion> {
	public static final Logger log = Logger.getLogger(Box.class.getName());
	List<ZoieIndexReader<R>> _archives;
	List<ZoieSystem<R, D, V>> _retiree;
	List<ZoieSystem<R, D, V>> _actives;
	IndexReaderDecorator<R> _decorator;

	/**
	 * Copy the given lists to have immutable behavior.
	 * 
	 * @param archives
	 * @param retiree
	 * @param actives
	 * @param decorator
	 */
	public Box(List<ZoieIndexReader<R>> archives,
			List<ZoieSystem<R, D, V>> retiree,
			List<ZoieSystem<R, D, V>> actives, IndexReaderDecorator<R> decorator) {
		_archives = new LinkedList<ZoieIndexReader<R>>(archives);
		_retiree = new LinkedList<ZoieSystem<R, D, V>>(retiree);
		_actives = new LinkedList<ZoieSystem<R, D, V>>(actives);
		_decorator = decorator;
		if (log.isDebugEnabled()) {
			for (ZoieIndexReader<R> r : _archives) {
				log.debug("archive " + r.directory() + " refCount: "
						+ r.getRefCount());
			}
		}
	}

	public void shutdown() {
		for (ZoieIndexReader<R> r : _archives) {
			r.decZoieRef();
			log.info("refCount at shutdown: " + r.getRefCount() + " "
					+ r.directory());
		}
		for (ZoieSystem<R, D, V> zoie : _retiree) {
			zoie.shutdown();
		}
		// add the active index readers
		for (ZoieSystem<R, D, V> zoie : _actives) {
			while (true) {
				long flushwait = 200000L;
				try {
					zoie.flushEvents(flushwait);
					zoie.getAdminMBean().setUseCompoundFile(true);
					zoie.getAdminMBean().optimize(1);
					break;
				} catch (IOException e) {
					log.error("pre-shutdown optimization "
							+ zoie.getAdminMBean().getIndexDir()
							+ " Should investigate. But move on now.", e);
					break;
				} catch (ZoieException e) {
					if (e.getMessage().indexOf("timed out") < 0) {
						break;
					} else {
						log.info("pre-shutdown optimization "
								+ zoie.getAdminMBean().getIndexDir()
								+ " flushing processing " + flushwait
								+ "ms elapsed");
					}
				}
			}
			zoie.shutdown();
		}
	}
}