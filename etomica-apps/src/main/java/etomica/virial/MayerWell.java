/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

import etomica.box.Box;
import etomica.molecule.IMoleculeList;
import etomica.potential.IPotential;

public class MayerWell implements MayerFunction {

    protected double sigma2, well2;
    
    public MayerWell(double sigma, double lambda) {
        sigma2 = sigma*sigma;
        well2 = sigma2*lambda*lambda;
    }

    public double f(IMoleculeList pair, double r2, double beta) {
        if (r2 < sigma2 || r2 > well2) return 0;
        return 1;
    }

    public IPotential getPotential() {
        return null;
    }

    public void setBox(Box box) {}

}
