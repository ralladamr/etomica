package etomica.data;

import etomica.data.types.DataDouble;
import etomica.data.types.DataDouble.DataInfoDouble;
import etomica.units.Quantity;

/**
 * Data processor that simply counts the number of times its
 * <code>addData</code> method is invoked. Output is a DataDouble.
 */
public class AccumulatorCounter extends DataAccumulator {

    /**
     * @param parentElement
     * @param dataSource
     */
    public AccumulatorCounter() {
        dataInfo = new DataInfoDouble("Counter", Quantity.DIMENSION);
        data = new DataDouble();
    }
    
    /**
     * Returns null, indicating that any Data type is acceptable for input.
     */
    public DataPipe getDataCaster(IEtomicaDataInfo incomingDataInfo) {
        return null;
    }

    /**
     * Does nothing.
     * 
     * @return the DataInfo for the output DataInteger
     */
    public IEtomicaDataInfo processDataInfo(IEtomicaDataInfo incomingDataInfo) {
        dataInfo.clearTags();
        dataInfo.addTags(incomingDataInfo.getTags());
        dataInfo.addTag(tag);
        return dataInfo;
    }

    /**
     * Increments the counter. Argument is ignored.
     */
    protected boolean addData(IData dummyData) {
        data.x++;
        return true;
    }

    /**
     * Returns the DataInteger with the count.
     */
    public IData getData() {
        return data;
    }

    /**
     * Sets count to zero.
     */
    public void reset() {
        data.x = 0;
    }

    private static final long serialVersionUID = 1L;

    protected final DataDouble data;
}
