1.step与step之间的数据传递是通过RowSet做到的。
  RowSet主要是BlockingRowSet, RowMeta放在BlockingRowSet的rowMeta变量里面，rowData放在
  ArrayBlockingQueue<Object[]>里面。
2.数据从上一个step传递到下一个step就是生产者消费者模型，
上一个Step  首先从前一个RowSet中getRowMeta，然后根据Step的配置情况setOutputMeta,
getInputRow根据Step的逻辑，setOutPutRow.
最后PutRow(outPutMeta,outPutRow),后一个Step从这两个Step之间的RowSet中就能get到最新的rowMeta和rowData.
