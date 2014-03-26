import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.gui.OverwritePrompter;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

=======>Spoon.java
public void executeTransformation(final TransMeta transMeta, final boolean local, final boolean remote,
      final boolean cluster, final boolean preview, final boolean debug, final Date replayDate, final boolean safe) {
    new Thread() {
      public void run() {
        getDisplay().asyncExec(new Runnable() {
          public void run() {
            try {
              delegates.trans
                  .executeTransformation(transMeta, local, remote, cluster, preview, debug, replayDate, safe);
            } catch (Exception e) {
              new ErrorDialog(shell, "Execute transformation", "There was an error during transformation execution", e);
            }
          }
        });
      }
    }.start();
  }

=========>SpoonTransformationDelegate.java
 
 if (executionConfiguration.isExecutingLocally()) {
				if (debug || preview) {
					activeTransGraph.debug(executionConfiguration, transDebugMeta);
				} else {
					//将配置设置入executionConfiguration后调用TransGraph实例执行
					activeTransGraph.start(executionConfiguration);
==========>TransGraph.java

trans = new Trans(transMeta, spoon.rep, transMeta.getName(), transMeta.getRepositoryDirectory().getPath(), transMeta.getFilename());
----> build Trans对象,transMeta = new TransMeta(filename, false);//build transMeta

=========>TransMeta.java
---->解析xml，把.ktr信息保存到TransMeta对象
public TransMeta(String fname, Repository rep, boolean setInternalVariables, VariableSpace parentVariableSpace, OverwritePrompter prompter ) throws KettleXMLException
{
    // OK, try to load using the VFS stuff...
    Document doc=null;
    try
    {
        doc = XMLHandler.loadXMLFile(KettleVFS.getFileObject(fname, parentVariableSpace));
    }
    catch (KettleFileException e)
    {
        throw new KettleXMLException(BaseMessages.getString(PKG, "TransMeta.Exception.ErrorOpeningOrValidatingTheXMLFile", fname), e);
    }
    
    if (doc != null)
    {
        // Clear the transformation
        clearUndo();
        clear();

        // Root node:
        Node transnode = XMLHandler.getSubNode(doc, XML_TAG); //$NON-NLS-1$

        // Load from this node...
        loadXML(transnode, rep, setInternalVariables, parentVariableSpace, prompter);

        setFilename(fname);
    }
    else
    {
        throw new KettleXMLException(BaseMessages.getString(PKG, "TransMeta.Exception.ErrorOpeningOrValidatingTheXMLFile", fname)); //$NON-NLS-1$
    }
}

------->TransMeta类的loadXML方法处理一些节点信息：
(//Handle connections// Read the notes...// Handle Steps// Read the error handling code of the steps...// Handle Hops)
public void loadXML(Node transnode, Repository rep, boolean setInternalVariables, VariableSpace parentVariableSpace, OverwritePrompter prompter ) throws KettleXMLException
{
    Props props = null;
    if (Props.isInitialized())
    {
        props=Props.getInstance();
    }
    
    initializeVariablesFrom(parentVariableSpace);
    
    try
    {
        // Clear the transformation
        clearUndo();
        clear();
        
        // Read all the database connections from the repository to make sure that we don't overwrite any there by loading from XML.
        try
        {
            sharedObjectsFile = XMLHandler.getTagValue(transnode, "info", "shared_objects_file"); //$NON-NLS-1$ //$NON-NLS-2$
            sharedObjects = rep!=null ? rep.readTransSharedObjects(this) : readSharedObjects();
        }
        catch(Exception e)
        {
            log.logError(BaseMessages.getString(PKG, "TransMeta.ErrorReadingSharedObjects.Message", e.toString()));
            log.logError(Const.getStackTracker(e));
        }

        // Handle connections
   ..........
   ..........
					
加载完TransMeta的信息后，在TransGraph类继续执行
private synchronized void prepareTrans(final Thread parentThread, final String[] args) {
    Runnable runnable = new Runnable() {
      public void run() {
        try {
          trans.prepareExecution(args);//core method
          initialized = true;
        } catch (KettleException e) {
          log.logError(trans.getName()+": preparing transformation execution failed", e); //$NON-NLS-1$
          checkErrorVisuals();
        }
        halted = trans.hasHaltedSteps();
        if (trans.isReadyToStart()) {
          checkStartThreads();// After init, launch the threads.
        } else {
          initialized = false;
          running = false;
          checkErrorVisuals();
        }
      }
    };
    Thread thread = new Thread(runnable);
    thread.start();
  }
   
------->call trans.prepareExecution(args)，这是一个核心方法：
 搭建以下结构结构。
	(1)、对每一个step根据hop信息进行找到下一个step或多个step。
	(2)、对于每一个this step和nex tstep生成一个RowSet对象，作为缓存供this step写，同时供next step读取数据。
	(3)、把此RowSet对象加入到Trans的List<RowSet>成员中保存。
		List<StepMeta> hopsteps=transMeta.getTransHopSteps(false);
		得到step列表
		……
		对每一个step进行如下设置
		for (int i=0;i<hopsteps.size();i++)
		{
			StepMeta thisStep=hopsteps.get(i);
			……
			//找到下一个step的列表
List<StepMeta> nextSteps = transMeta.findNextSteps(thisStep);
			int nrTargets = nextSteps.size();
			for (int n=0;n<nrTargets;n++)
			{
				StepMeta nextStep = nextSteps.get(n);
				…… 对于每一个hop信息生成RowSet,并设置RowSet，把
RowSet rowSet=new RowSet(transMeta.getSizeRowset());
rowSet.setThreadNameFromToCopy(thisStep.getName(),0, nextStep.getName(), 0);
 			rowsets.add(rowSet);
         } 
		} 
……
(4)、根据TransMeta的step信息生成相应的StepMetaDataCombi（即steps）信息，加到steps列表中。
StepMetaDataCombi combi = new StepMetaDataCombi();
combi.stepname = stepMeta.getName();
	combi.copy     = c;
		combi.stepMeta = stepMeta;
		combi.meta = stepMeta.getStepMetaInterface();
		StepDataInterface data = combi.meta.getStepData();
		combi.data = data;
……
		StepInterface step=combi.meta.getStep(stepMeta, data, c, transMeta, this);
		在step初始化时，会把Trans中的List<RowSet>的相应的rowset加入到step的inputRowSets，和outputRowSets中。
		combi.step = step;
		steps.add(combi);

