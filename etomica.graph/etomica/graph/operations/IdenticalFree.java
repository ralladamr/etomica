package etomica.graph.operations;

import java.util.HashSet;
import java.util.Set;

import etomica.graph.iterators.IteratorWrapper;
import etomica.graph.iterators.filters.IdenticalGraphFilter;
import etomica.graph.model.Graph;
import etomica.graph.model.GraphIterator;

/**
 * Operation that removes identical graphs
 * 
 * @author Andrew Schultz
 */
public class IdenticalFree implements Unary {

  public Set<Graph> apply(Set<Graph> argument, Parameters params) {

    IteratorWrapper wrapper = new IteratorWrapper(argument.iterator(), true);
    GraphIterator isomorphs = new IdenticalGraphFilter(wrapper);
    Set<Graph> result = new HashSet<Graph>();
    while (isomorphs.hasNext()) {
      result.add(isomorphs.next());
    }
    return result;
  }
}