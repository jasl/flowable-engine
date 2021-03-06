package org.activiti5.engine.test.jobexecutor;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti5.engine.ActivitiException;

public class RetryFailingDelegate implements JavaDelegate {

  public static final String EXCEPTION_MESSAGE = "Expected exception.";
  
	public static boolean shallThrow = false;
	public static List<Long> times;
	 
	static public void resetTimeList() {
	  times = new ArrayList<Long>();
	}

	@Override
	public void execute(DelegateExecution execution) {
	    	
	  times.add(System.currentTimeMillis());

	  if (shallThrow) {
	    throw new ActivitiException(EXCEPTION_MESSAGE);
	  }
	}
}