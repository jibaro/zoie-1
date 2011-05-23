package bit.lin.myimpl;

import java.io.File;

import proj.zoie.api.DataProvider;
import proj.zoie.api.DefaultZoieVersion;
import proj.zoie.impl.indexing.FileDataProvider;
import proj.zoie.impl.indexing.FileIndexableInterpreter;
import proj.zoie.impl.indexing.SimpleZoieSystem;
import proj.zoie.impl.indexing.ZoieSystem;

public class MyImpl {
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

	public static void main(String[] args) {
		ZoieSystem zSystem = ZoieSystemFactory.getMySpecifiedInstance();
		MyDataProvider dProvider = new MyDataProvider(new File("datasource"));
		dProvider.setDataConsumer(zSystem);
		dProvider.start();
//		zSystem.consume(data);
	}
}
