/*
 * History
 * Created on Nov 4, 2004 by kofke
 */
package etomica.action;

import etomica.AccumulatorManager;
import etomica.Action;
import etomica.utility.java2.Iterator;
import etomica.utility.java2.LinkedList;

/**
 * Action that performs a call to the reset() method of a set
 * of accumulators, as specified via a list of AccumulatorManager
 * instances.
 */
public class ResetAccumulators implements Action {

	/**
	 * 
	 */
	public ResetAccumulators(LinkedList accumulatorManagerList) {
		this.accumulatorManagerList = accumulatorManagerList;
	}

	public void actionPerformed() {
		Iterator iterator = accumulatorManagerList.iterator();
		while (iterator.hasNext()) {
			((AccumulatorManager)iterator.next()).resetAccumulators();
		}
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	private final LinkedList accumulatorManagerList;
	private String label = "Reset Accumulators";
}
