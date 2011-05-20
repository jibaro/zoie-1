package proj.zoie.test.mock;

import java.util.Collection;

import org.apache.log4j.Logger;

import proj.zoie.api.DataConsumer;
import proj.zoie.api.ZoieException;
import proj.zoie.api.ZoieVersion;

public class MockDataLoader<D, V extends ZoieVersion> implements DataConsumer<D,V> {
	private static final Logger log = Logger.getLogger(MockDataLoader.class);
	
	private long _delay;
	private int _count;
	private D _lastConsumed;
	private int _numCalls = 0;
	private int _numEvents = 0;
	private volatile int _maxBatch = 0;
	
	public MockDataLoader()
	{
		_delay=100L;
		_count=0;
		_lastConsumed=null;
	}
	
	public D getLastConsumed()
	{
		return _lastConsumed;
	}
	
	public void setDelay(long delay)
	{
		_delay=delay;
	}
	
	public long getDelay()
	{
		return _delay;
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public void consume(Collection<DataEvent<D,V>> data) throws ZoieException
	{	
	    _numCalls++;
	    if(data != null)
	    {
	      _numEvents += data.size();
	      _maxBatch = Math.max(_maxBatch, data.size());
	    }
	    
		if (data!=null && data.size()>0)
		{
			for (DataEvent<D,V> event : data)
			{
				_lastConsumed=event.getData();
			}
			_count+=data.size();
		}
		try
		{
		      Thread.sleep(_delay);
        
        }
		catch (InterruptedException e)
        {
        }
	}
	
	public int getNumCalls()
	{
	  return _numCalls;
	}
	
    public int getNumEvents()
    {
      return _numEvents;
    }

    public int getMaxBatch()
    {
      return _maxBatch;
    }
    
  public V getVersion()
  {
    throw new UnsupportedOperationException();
  }
}
