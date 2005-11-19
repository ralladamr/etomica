package etomica.action;

import etomica.exception.ConfigurationOverlapException;
import etomica.integrator.Integrator;


/**
 * Action that calls the reset method of an integrator.
  */

/*
 * History
 * Created on Feb 5, 2005 by kofke
 */
public class IntegratorReset extends IntegratorActionAdapter {

    public IntegratorReset() {
        super();
    }
    
    public IntegratorReset(Integrator integrator, boolean ignoreOverlap) {
        super(integrator,"Reset integrator");
        this.ignoreOverlap = ignoreOverlap;
    }

    public void actionPerformed() {
        if(integrator != null) {
            try {
                integrator.reset();
            }
            catch (ConfigurationOverlapException e) {
                if (!ignoreOverlap) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * @return Returns the ignoreOverlap.
     */
    public boolean isIgnoreOverlap() {
        return ignoreOverlap;
    }

    /**
     * @param ignoreOverlap The ignoreOverlap to set.
     */
    public void setIgnoreOverlap(boolean ignoreOverlap) {
        this.ignoreOverlap = ignoreOverlap;
    }

    private boolean ignoreOverlap;
}
