package etomica.graphics;

import java.awt.Color;

import etomica.api.IAtomLeaf;
import etomica.api.IAtomTypeLeaf;
import etomica.api.ISimulation;
import etomica.atom.AtomTypeAgentManager;
import etomica.atom.AtomTypeAgentManager.AgentSource;

/**
 * Colors the atom according to the color given by its type field.
 *
 * @author David Kofke
 */
public class ColorSchemeByType extends ColorScheme implements AgentSource {
    
    public ColorSchemeByType(ISimulation sim) {
    	super();
        colorMap = new AtomTypeAgentManager(this, sim.getSpeciesManager(),
                                            sim.getEventManager(), false);
    }

    public Object makeAgent(IAtomTypeLeaf atom) {
    	return null;
    }

    public void releaseAgent(Object obj, IAtomTypeLeaf atom) {
    }

    public Class getSpeciesAgentClass() {
    	return Color.class;
    }
    
    public void setColor(IAtomTypeLeaf type, Color c) {
    	colorMap.setAgent(type, c);
    }
    
    public Color getAtomColor(IAtomLeaf a) {
        return getColor(a.getType());
    }
    
    public Color getColor(IAtomTypeLeaf type) {
        Color color = (Color)colorMap.getAgent(type);
        if (color == null) {
            if (defaultColorsUsed < moreDefaultColors.length) {
                color = moreDefaultColors[defaultColorsUsed];
                defaultColorsUsed++;
                setColor(type, color);
            }
            else {
                color = defaultColor;
                setColor(type, color);
            }
        }
        return color;
    }
    
    private final AtomTypeAgentManager colorMap;
    protected final Color[] moreDefaultColors = new Color[]{ColorScheme.DEFAULT_ATOM_COLOR, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE};
    protected int defaultColorsUsed = 0;
}
