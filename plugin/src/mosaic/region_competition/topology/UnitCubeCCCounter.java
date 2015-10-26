package mosaic.region_competition.topology;


import java.util.LinkedList;
import java.util.Queue;

import mosaic.core.image.Connectivity;
import mosaic.core.image.Point;


class UnitCubeCCCounter {

    boolean[] m_ConnectivityTest;

    private final boolean m_UnitCubeNeighbors[][];

    private char[] m_Image;
    private final Connectivity TConnectivity;
    private final Connectivity TNeighborhoodConnectivity;

    UnitCubeCCCounter(Connectivity TConnectivity) {
        this.TConnectivity = TConnectivity;
        this.TNeighborhoodConnectivity = TConnectivity.getNeighborhoodConnectivity();

        m_ConnectivityTest = CreateConnectivityTest(TConnectivity);

        m_UnitCubeNeighbors = initUnitCubeNeighbors(TConnectivity, TNeighborhoodConnectivity);
    }

    /**
     * Set the sub image (data of unitcube)
     * midpoint has to be 0!
     *
     * @param data of unitcube as linear array
     */
    void SetImage(char[] subImage) {
        m_Image = subImage.clone();
    }

    /**
     * @param conn
     * @return Boolean array, entry at position <tt>i</tt> indicating
     *         if offset i is in neighborhood for <tt>conn</tt>
     */
    private static boolean[] CreateConnectivityTest(Connectivity conn) {
        final int neighborhoodSize = conn.GetNeighborhoodSize();
        final boolean[] result = new boolean[neighborhoodSize];

        for (int i = 0; i < neighborhoodSize; i++) {
            result[i] = conn.isNeighborhoodOfs(i);
        }
        return result;
    }

    int connectedComponents() {

        final int neighborhoodSize = TConnectivity.GetNeighborhoodSize();
        int seed = 0;
        // Find first seed
        while (seed != neighborhoodSize && (m_Image[seed] == 0 || !m_ConnectivityTest[seed])) {
            ++seed;
        }

        final boolean vProcessed_new[] = new boolean[neighborhoodSize];
        final Queue<Integer> q = new LinkedList<Integer>();

        int nbCC = 0;
        while (seed != neighborhoodSize) {
            ++nbCC;
            vProcessed_new[seed] = true;

            q.clear();
            q.add(seed);

            while (!q.isEmpty()) {
                final int current = q.poll();

                // For each neighbor check if m_UnitCubeNeighbors is true.
                for (int neighbor = 0; neighbor < neighborhoodSize; ++neighbor) {
                    if (!vProcessed_new[neighbor] && m_Image[neighbor] != 0 && m_UnitCubeNeighbors[current][neighbor]) {
                        q.add(neighbor);
                        vProcessed_new[neighbor] = true;
                    }
                }
            }

            // Look for next seed
            while (seed != neighborhoodSize && (vProcessed_new[seed] || m_Image[seed] == 0 || !m_ConnectivityTest[seed])) {
                ++seed;
            }
        }
        return nbCC;
    }

    /**
     * Precalculates neighborhood within the unit cube and stores them into
     * boolean array.
     * Access array by the integer offsets for the points to be checked.
     * Array at position idx1, idx2 is true,
     * if idx1, idx2 are unit cube neighbors with respect to their
     * connectivities <br>
     * <b>WARNING!</b> this does not generate symmetric solutions, but it's the
     * lamy solution...
     * eg [0,3] is false but [3, 0] is true in ITK (3,2)
     *
     * @param connectivity Connectivity to be checked
     * @param neighborhoodConnectivity Neighborhood connectivity. This has to be
     *            more lax (reach more neighbors) than connectivity
     * @return
     */
    private static boolean[][] initUnitCubeNeighbors(Connectivity connectivity, Connectivity neighborhoodConnectivity) {
        final int neighborhoodSize = connectivity.GetNeighborhoodSize();
        final boolean neighborsInUnitCube[][] = new boolean[neighborhoodSize][neighborhoodSize];

        final int dim = connectivity.getDim();

        for (int neighbor1 = 0; neighbor1 < neighborhoodSize; neighbor1++) {
            final Point p1 = connectivity.ofsIndexToPoint(neighbor1);

            if (neighborhoodConnectivity.isNeighborhoodOfs(p1)) {
                for (int neighbor2 = 0; neighbor2 < neighborhoodSize; neighbor2++) {
                    final Point p2 = connectivity.ofsIndexToPoint(neighbor2);

                    final Point sum = p1.add(p2);
                    final int sumOffset = connectivity.pointToOffset(sum);

                    boolean inUnitCube = true;
                    for (int d = 0; d < dim && inUnitCube; d++) {
                        if (sum.iCoords[d] < -1 || sum.iCoords[d] > +1) {
                            inUnitCube = false;
                        }
                    }

                    if (inUnitCube && connectivity.areNeighbors(p1, sum)) {
                        neighborsInUnitCube[neighbor1][sumOffset] = true;
                    }
                }
            }
        }
        return neighborsInUnitCube;
    }

    @Override
    public String toString() {
        final String result = "UnitCubeCCCounter " + TConnectivity + " " + TNeighborhoodConnectivity;

        return result;
    }
}
