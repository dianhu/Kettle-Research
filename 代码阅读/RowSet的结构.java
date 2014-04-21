import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.pentaho.di.core.BaseRowSet;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.row.RowMetaInterface;

====>BlockingRowSet是RowSet的一种实现方式。
BlockingRowSet实际上是一个ArrayBlockingQueue阻塞队列。
每一个Step都是生产者和消费者，往ArrayBlockingQueue里面putRow和getDate。



public class BlockingRowSet extends BaseRowSet implements Comparable<RowSet>, RowSet
{
    private BlockingQueue<Object[]> queArray;
    
    /**
     * Create new non-blocking-queue with maxSize capacity.
     * @param maxSize
     */
    public BlockingRowSet(int maxSize)
    {
    	super();

    	// create an empty queue 
        queArray = new ArrayBlockingQueue<Object[]>(maxSize, false);
    }
    
    /* (non-Javadoc)
	 * @see org.pentaho.di.core.RowSetInterface#putRow(org.pentaho.di.core.row.RowMetaInterface, java.lang.Object[])
	 */
    public boolean putRow(RowMetaInterface rowMeta, Object[] rowData)
    {
    	return putRowWait(rowMeta, rowData, Const.TIMEOUT_PUT_MILLIS, TimeUnit.MILLISECONDS);
    }
    
    /* (non-Javadoc)
	 * @see org.pentaho.di.core.RowSetInterface#putRowWait(org.pentaho.di.core.row.RowMetaInterface, java.lang.Object[], long, java.util.concurrent.TimeUnit)
	 */
    public boolean putRowWait(RowMetaInterface rowMeta, Object[] rowData, long time, TimeUnit tu) {
    	this.rowMeta = rowMeta;
    	try{
    		
    		return queArray.offer(rowData, time, tu);
    	}
    	catch (InterruptedException e)
	    {
    		return false;
	    }
    	catch (NullPointerException e)
	    {
    		return false;
	    }    	
    	
    }
    
    // default getRow with wait time = 100ms
    //
    /* (non-Javadoc)
	 * @see org.pentaho.di.core.RowSetInterface#getRow()
	 */
    public Object[] getRow(){
    	return getRowWait(Const.TIMEOUT_GET_MILLIS, TimeUnit.MILLISECONDS);
    }
    
    
    /* (non-Javadoc)
	 * @see org.pentaho.di.core.RowSetInterface#getRowImmediate()
	 */       
    public Object[] getRowImmediate(){

    	return queArray.poll();	    	
    }
    
    /* (non-Javadoc)
	 * @see org.pentaho.di.core.RowSetInterface#getRowWait(long, java.util.concurrent.TimeUnit)
	 */
    public Object[] getRowWait(long timeout, TimeUnit tu){

    	try{
    		return queArray.poll(timeout, tu);
    	}
    	catch(InterruptedException e){
    		return null;
    	}
    }
    
    @Override
    public int size() {
    	return queArray.size();
    }
}

