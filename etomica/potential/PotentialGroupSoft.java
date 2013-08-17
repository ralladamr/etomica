package etomica.potential;

import etomica.api.IAtomList;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.AtomPositionGeometricCenter;
import etomica.atom.IAtomPositionDefinition;
import etomica.space.ISpace;
import etomica.space.Tensor;

/** 
 * include virial and gradient for a molecular potential
 * 
 *  @author shu
 *  Aug 2013
 */
public class PotentialGroupSoft extends PotentialGroup implements PotentialMolecularSoft {

	private static final long serialVersionUID = 1L;
	protected final IVectorMutable[] gradients;
	protected final IAtomPositionDefinition positionDefinition;
	protected double truncation;

	public PotentialGroupSoft(int nBody,ISpace space,double truncation) {
		super(nBody,space);
		gradients = new IVectorMutable[2];
        gradients[0] = space.makeVector();
        gradients[1] = space.makeVector();
		// TODO Auto-generated constructor stub
        this.truncation=truncation;
		positionDefinition = new AtomPositionGeometricCenter(space);
	}

	public double getTruncation() {
		return truncation;
	}

	public void setTruncation(double truncation) {
		this.truncation = truncation;
	}
	public double energy(IMoleculeList pair) {
		// TODO Auto-generated method stub
		IMolecule molecule_a = pair.getMolecule(0);//1st molecule in the pair
		IMolecule molecule_b = pair.getMolecule(1);//2nd molecule in the pair
		IVectorMutable r_a = space.makeVector();
		IVectorMutable r_b = space.makeVector();
		r_a.E(positionDefinition.position(molecule_a));
		r_b.E(positionDefinition.position(molecule_b));
		IVectorMutable vector = space.makeVector();
		vector.Ev1Mv2(r_b, r_a);
		box.getBoundary().nearestImage(vector);
		double distance2 = vector.squared();
		if (distance2 > (truncation * truncation)){
			return 0.0;
		}
		
		return super.energy(pair);

	}
	@Override
	public double virial(IMoleculeList pair) {//pass molecular pair
		// COM from atompositiondefinition 
		IMolecule molecule_a = pair.getMolecule(0);//1st molecule in the pair
		IMolecule molecule_b = pair.getMolecule(1);//2nd molecule in the pair
		IVectorMutable r_a = space.makeVector();
		IVectorMutable r_b = space.makeVector();
		r_a.E(positionDefinition.position(molecule_a));
		r_b.E(positionDefinition.position(molecule_b));
		IVectorMutable vector = space.makeVector();
		vector.Ev1Mv2(r_b, r_a);
		box.getBoundary().nearestImage(vector);
		double distance2 = vector.squared();
		if (distance2 > truncation * truncation){
			return 0.0;
		}
		IVector[] grad = gradient(pair);
		return -vector.dot(grad[0]);

	}

	public IVector[] gradient(IMoleculeList basisAtoms) {//pass molecular pair
        if(basisAtoms.getMoleculeCount() != this.nBody()) {
            throw new IllegalArgumentException("Error: number of atoms for energy calculation inconsistent with order of potential");
        }
        gradients[0].E(0.0);      
        for (PotentialLinker link=first; link!= null; link=link.next) {	
        	if(!link.enabled) continue;
            link.iterator.setBasis(basisAtoms);
            link.iterator.reset();
            for (IAtomList atoms = link.iterator.next(); atoms != null; atoms = link.iterator.next()) {
            	IVector[] gradient_ = ((PotentialSoft)link.potential).gradient(atoms);
            	gradients[0].PE(gradient_[0]);
            }
            
        }
        gradients[1].Ea1Tv1(-1, gradients[0]);
        return gradients; 
	}


	public IVector[] gradient(IMoleculeList atoms, Tensor pressureTensor) {
		return null;
	}

}