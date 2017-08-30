package mosaic.region_competition.DRS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MinimalParticleIndexedSet implements Iterable<MinimalParticle> {
    private HashMap<MinimalParticle, Integer> iMap = new HashMap<>();
    ArrayList<MinimalParticle> iParticles = new ArrayList<>();
    private MinimalParticle iLastRemovedElement = null;
    
    /**
     * @return size of container
     */
    int size() { return iMap.size(); }
    
    /**
     * @return index of aParticle or if not found first not used index (size of container)
     */
    int find(MinimalParticle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) { return iMap.size(); }
        return index;
    }
    
    /**
     * @return true if aParticle is in container
     */
    boolean contains(MinimalParticle aParticle) {
        return iMap.containsKey(aParticle);
    }
    
    /**
     * Inserts particle into container
     * @param aParticle - particle to be inserted
     * @return index of inserted particle
     */
    MinimalParticle insert(MinimalParticle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) {
            index = iMap.size();
            iMap.put(aParticle, index);
            iParticles.add(aParticle);
        }
        else {
            // new replaced particle might have different proposal, so old one need to be removed first
            iLastRemovedElement = iParticles.get(index);
            iMap.remove(aParticle);
            iMap.put(aParticle, index);
            iParticles.set(index, aParticle);
            return iLastRemovedElement;
        }
        return null;
    }
    
    /**
     * @return particle at aIndex
     */
    MinimalParticle elementAt(int aIndex) {
        return iParticles.get(aIndex);
    }
    
    /**
     * Join provided aSet to this one.
     * @param aSet
     * @return this
     */
    MinimalParticleIndexedSet join(MinimalParticleIndexedSet aSet) {
        for(int i = 0; i < aSet.size(); ++i) {
            insert(new MinimalParticle(aSet.elementAt(i)));
        }
        return this;
    }
    
    /**
     * Removes aParticle. Change indices to keep them continues so may invalidate previous index to particle.
     * @return removed MinimalParticle if existed or null otherwise
     */
    MinimalParticle erase(MinimalParticle aParticle) {
        Integer index = iMap.get(aParticle);
        if (index == null) {
            return null;
        }
        iLastRemovedElement = iParticles.get(index);

        /// We move the last element:
        int lastElementIndex = iParticles.size() - 1;
        iMap.replace(iParticles.get(lastElementIndex), index);
        iMap.remove(aParticle);
        
        // Update the vector: move the last element to the element to delete and remove the last element.
        iParticles.set(index, iParticles.get(lastElementIndex));
        iParticles.remove(lastElementIndex);
        
        return iLastRemovedElement;
    }
    
    @Override
    public String toString() {
        return " MAP/VECsize: " + iMap.size() +"/" + iParticles.size() + " mapElements:\n" + iMap;
    }

    @Override
    public Iterator<MinimalParticle> iterator() {
        return new Iterator<MinimalParticle>() {
            private int idx = 0;
            
            @Override
            public boolean hasNext() {
                return idx < iParticles.size();
            }

            @Override
            public MinimalParticle next() {
                return iParticles.get(idx++);
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
