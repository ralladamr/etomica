/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.normalmode;

import etomica.data.DataProcessor;
import etomica.data.IData;
import etomica.data.IDataInfo;
import etomica.data.types.DataDouble;
import etomica.data.types.DataDouble.DataInfoDouble;
import etomica.units.dimensions.Null;

public class DataProcessorBoltzmannFactor extends DataProcessor {

    protected final DataDouble data;
    protected final DataInfoDouble dataInfo;
    protected double temperature;

	public DataProcessorBoltzmannFactor() {
		data = new DataDouble();
		dataInfo = new DataInfoDouble("Boltzmann Factor", Null.DIMENSION);
	}

    protected IData processData(IData inputData) {
		data.x = Math.exp(-inputData.getValue(0)/temperature);
		return data;
	}

	protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
		return dataInfo;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

}
