import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.step.StepInitThread;
import org.pentaho.di.trans.step.StepMetaDataCombi;



------------>step的init方法是在prepareExecution(String[] arguments)方法里调用的，
对每个step新建一个线程，并调用每个step的初始化方法
 
StepInitThread initThreads[] = new StepInitThread[steps.size()];
        Thread[] threads = new Thread[steps.size()];

        // Initialize all the threads...
        //
		for (int i=0;i<steps.size();i++)
		{
			final StepMetaDataCombi sid=steps.get(i);
            
            // Do the init code in the background!
            // Init all steps at once, but ALL steps need to finish before we can continue properly!
			initThreads[i] = new StepInitThread(sid, log);
            
            // Put it in a separate thread!
			threads[i] = new Thread(initThreads[i]);
            threads[i].setName("init of "+sid.stepname+"."+sid.copy+" ("+threads[i].getName()+")");
            threads[i].start();
		}

