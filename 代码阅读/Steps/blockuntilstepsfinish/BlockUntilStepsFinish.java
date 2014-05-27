 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/
 
package org.pentaho.di.trans.steps.blockuntilstepsfinish;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.BaseStepData.StepExecutionStatus;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;



/**
 * Block all incoming rows until defined steps finish processing rows.
 * 
 * @author Samatar
 * @since 30-06-2008
 */

public class BlockUntilStepsFinish extends BaseStep implements StepInterface
{
	private static Class<?> PKG = BlockUntilStepsFinishMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private BlockUntilStepsFinishMeta meta;
	private BlockUntilStepsFinishData data;
	
	public BlockUntilStepsFinish(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(BlockUntilStepsFinishMeta)smi;
		data=(BlockUntilStepsFinishData)sdi;

		if(first)  {
			first=false;
			String[] stepnames= null;
			int stepnrs=0;
			if(meta.getStepName()!=null && meta.getStepName().length>0)  {
				stepnames=meta.getStepName();
				stepnrs=stepnames.length;
			}else  {
				throw new KettleException(BaseMessages.getString(PKG, "BlockUntilStepsFinish.Error.NotSteps"));
			}
			// Get target stepnames
			String[] targetSteps= getTransMeta().getNextStepNames(getStepMeta());
			
			data.stepInterfaces= new ConcurrentHashMap<Integer, StepInterface>();
			for(int i=0;i<stepnrs;i++) {
				// We can not get metrics from current step
				if(stepnames[i].equals(getStepname())) throw new KettleException("You can not wait for step ["+stepnames[i]+"] to finish!");
				if(targetSteps!=null) {
					// We can not metrics from the target steps
					for(int j=0; j<targetSteps.length; j++) {
						if(stepnames[i].equals(targetSteps[j])) throw new KettleException("You can not get metrics for the target step ["+targetSteps[i]+"]!");
					}
				}
				
				int CopyNr=Const.toInt(meta.getStepCopyNr()[i],0);
				StepInterface step = getDispatcher().findBaseSteps(stepnames[i]).get(CopyNr);
				if(step==null) throw new KettleException("Erreur finding step ["+stepnames[i]+"] nr copy="+CopyNr+"!");
				
				data.stepInterfaces.put(i, getDispatcher().findBaseSteps(stepnames[i]).get(CopyNr));
			}
		} // end if first
		
	     // Wait until all specified steps have finished!
	      while (data.continueLoop && !isStopped())  {
	    	  data.continueLoop = false;
	    	  Iterator<Entry<Integer, StepInterface>> it= data.stepInterfaces.entrySet().iterator();
	    	  while(it.hasNext())  {
	    		  Entry<Integer, StepInterface> e = it.next();
	    		  StepInterface step = e.getValue();
	    		  if (step.getStatus()!=StepExecutionStatus.STATUS_FINISHED)  {//判定step的运行状态
	    			  // This step is still running...
	    			  data.continueLoop=true;
	    		  }else {
	    			  // We have done with this step.
	    			  // remove it from the map
	    			  data.stepInterfaces.remove(e.getKey());
	    			  if(log.isDetailed()) logDetailed("Finished running step ["+ step.getStepname()+"(" + step.getCopy() +")].");
	    		  }
	    	  }
	        
		      if (data.continueLoop)  {
		       	try {
					Thread.sleep(200);//通过sleep线程的方式达到block的效果
				   } catch (Exception e){}
		      }
	      }
	     
	    // All steps we are waiting for are ended
	    // let's now free all incoming rows 
		Object[] r=getRow();
		
		if (r==null) {
			// no more input to be expected...
			setOutputDone();
			return false;
		}
		
		 putRow(getInputRowMeta(), r);

		return true;
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(BlockUntilStepsFinishMeta)smi;
		data=(BlockUntilStepsFinishData)sdi;
		
		if (super.init(smi, sdi))
		{
		    return true;
		}
		return false;
	}
	
}
