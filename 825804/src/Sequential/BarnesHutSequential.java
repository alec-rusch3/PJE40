package Sequential;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * @author carpenb1 modified by 825804
 */
public class BarnesHutSequential {

    // Size of simulation
    //final static int N = 2000 ;  // Number of "stars"
    final static int N = 2000;  // Number of "stars"
    final static double BOX_WIDTH = 100.0;

    //final static int P = 8;
    // Initial state
    final static double RADIUS = 20.0;  // of randomly populated sphere

    //final static double ANGULAR_VELOCITY = 0.4 ;
    final static double ANGULAR_VELOCITY = 3;
    // controls total angular momentum

    // Simulation
    final static double DT = 0.0005;  // Time step

    // Display
    final static int WINDOW_SIZE = 1000;
    final static int DELAY = 0;
    final static int OUTPUT_FREQ = 1;

    // Star positions
    static double[] x = new double[N];
    static double[] y = new double[N];
    static double[] z = new double[N];

    // Star velocities
    static double[] vx = new double[N];
    static double[] vy = new double[N];
    static double[] vz = new double[N];

    // Star accelerations
    static double[] ax = new double[N];
    static double[] ay = new double[N];
    static double[] az = new double[N];

    // Barnes Hut tree
    static Node tree;

    static Display display = new Display();

    public static void main(String args[]) throws Exception {

        // Define initial state of stars
        /*
        // Randomly choose plane for net angular velocity

        double nx = 2 * Math.random() - 1 ;
        double ny = 2 * Math.random() - 1 ;
        double nz = 2 * Math.random() - 1 ;
        double norm = 1.0 / Math.sqrt(nx * nx + ny * ny + nz * nz) ;
        nx *= norm ;
        ny *= norm ;
        nz *= norm ;
         */
        // ... or just rotate in x, y plane
        //double nx = 0, ny = 0, nz = 1.0;
        // ... or just rotate in x, z plane
        double nx = 0, ny = 1.0, nz = 0;
        for (int i = 0; i < N; i++) {

            // Place star randomly in sphere of specified radius
            double rx, ry, rz, r;
            do {
                rx = (2 * Math.random() - 1) * RADIUS;
                ry = (2 * Math.random() - 1) * RADIUS;
                rz = (2 * Math.random() - 1) * RADIUS;
                r = Math.sqrt(rx * rx + ry * ry + rz * rz);
            } while (r > RADIUS);

            x[i] = 0.5 * BOX_WIDTH + rx;
            y[i] = 0.5 * BOX_WIDTH + ry;
            z[i] = 0.5 * BOX_WIDTH + rz;

            vx[i] = ANGULAR_VELOCITY * (ny * rz - nz * ry);
            vy[i] = ANGULAR_VELOCITY * (nz * rx - nx * rz);
            vz[i] = ANGULAR_VELOCITY * (nx * ry - ny * rx);
        }
        int iter = 0;
        long startTime = System.currentTimeMillis();
        while (iter <= 4000) {
            double dtOver2 = 0.5 * DT;
            double dtSquaredOver2 = 0.5 * DT * DT;

            if (iter % OUTPUT_FREQ == 0) {
                System.out.println("iter = " + iter + ", time = " + iter * DT);
                display.repaint();
                //Thread.sleep(DELAY) ;
            }

            // Verlet integration:
            // http://en.wikipedia.org/wiki/Verlet_integration#Velocity_Verlet
            for (int i = 0; i < N; i++) {
                // update position
                // mod implements periodic box
                x[i] = mod(x[i] + (vx[i] * DT) + (ax[i] * dtSquaredOver2),
                        BOX_WIDTH);
                y[i] = mod(y[i] + (vy[i] * DT) + (ay[i] * dtSquaredOver2),
                        BOX_WIDTH);
                z[i] = mod(z[i] + (vz[i] * DT) + (az[i] * dtSquaredOver2),
                        BOX_WIDTH);
                // update velocity halfway
                vx[i] += (ax[i] * dtOver2);
                vy[i] += (ay[i] * dtOver2);
                vz[i] += (az[i] * dtOver2);
            }

            computeAccelerations();

            for (int i = 0; i < N; i++) {
                // finish updating velocity with new acceleration
                vx[i] += (ax[i] * dtOver2);
                vy[i] += (ay[i] * dtOver2);
                vz[i] += (az[i] * dtOver2);
            }

            iter++;

        }
        long endTime = System.currentTimeMillis();
        System.out.println("Calculation completed in " + (endTime - startTime) + " milliseconds");
    }

    // Compute accelerations of all stars from current positions:
    static void computeAccelerations() {

        tree = new Node(0.0, BOX_WIDTH, 0.0, BOX_WIDTH, 0.0, BOX_WIDTH);
        for (int i = 0; i < N; i++) {
            tree.addParticle(x[i], y[i], z[i]);
        }
        tree.preCompute();

        // Interaction forces (gravity)
        // This is where the program spends most of its time.
        long startForceTime = System.currentTimeMillis();

        for (int i = 0; i < N; i++) {
            Vector acc = new Vector();
            tree.calcForce(acc, x[i], y[i], z[i]);
            ax[i] = acc.x;
            ay[i] = acc.y;
            az[i] = acc.z;
        }

    }

    static class Display extends JPanel {

        static final double SCALE = WINDOW_SIZE / BOX_WIDTH;

        Display() {

            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));

            JFrame frame = new JFrame("MD");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, WINDOW_SIZE, WINDOW_SIZE);
            g.setColor(Color.WHITE);
            for (int i = 0; i < N; i++) {
                int gx = (int) (SCALE * x[i]);
                int gy = (int) (SCALE * y[i]);
                if (0 <= gx && gx < WINDOW_SIZE && 0 < gy && gy < WINDOW_SIZE) {
                    g.fillRect(gx, gy, 1, 1);
                }
            }
        }
    }

    static double mod(double x, double box) {
        double reduced = x - ((int) (x / box) * box);
        return reduced >= 0 ? reduced : reduced + box;
    }

    static class Node {

        // Barnes-Hut
        final static double OPENING_ANGLE = 1.0;
        final static double SQRT3 = Math.sqrt(3.0);

        double xLo, xHi, yLo, yHi, zLo, zHi;
        double xMid, yMid, zMid;
        double ax, ay, az;
        int nParticles;
        double xCent, yCent, zCent;  // centre of mass
        Node[] children;

        double threshold;

        //static int nCellsOpened ; // debug
        Node(double xLo, double xHi, double yLo, double yHi, double zLo, double zHi) {
            this.xLo = xLo;
            this.xHi = xHi;
            this.yLo = yLo;
            this.yHi = yHi;
            this.zLo = zLo;
            this.zHi = zHi;
            xMid = 0.5 * (xLo + xHi);
            yMid = 0.5 * (yLo + yHi);
            zMid = 0.5 * (zLo + zHi);
        }

        void addParticle(double x, double y, double z) {
            if (x < xLo || x >= xHi || y < yLo || y >= yHi || z < zLo || z >= zHi) {
                System.out.println("x = " + x + ", y = " + y + ", z = " + z);  // debug
                throw new IllegalArgumentException("particle position outside "
                        + "bounding box of Node");
            }
            if (nParticles == 0) {
                xCent = x;
                yCent = y;
                zCent = z;
                nParticles = 1;
                return;
            }
            if (nParticles == 1) {
                children = new Node[8];
                addParticleToChild(xCent, yCent, zCent);
            }
            addParticleToChild(x, y, z);
            nParticles++;
        }

        void addParticleToChild(double x, double y, double z) {

            int childIdx = ((x < xMid) ? 0 : 4) + ((y < yMid) ? 0 : 2)
                    + ((z < zMid) ? 0 : 1);

            Node child = children[childIdx];
            if (child == null) {
                switch (childIdx) {
                    case 0:
                        child = new Node(xLo, xMid, yLo, yMid, zLo, zMid);
                        break;
                    case 1:
                        child = new Node(xLo, xMid, yLo, yMid, zMid, zHi);
                        break;
                    case 2:
                        child = new Node(xLo, xMid, yMid, yHi, zLo, zMid);
                        break;
                    case 3:
                        child = new Node(xLo, xMid, yMid, yHi, zMid, zHi);
                        break;
                    case 4:
                        child = new Node(xMid, xHi, yLo, yMid, zLo, zMid);
                        break;
                    case 5:
                        child = new Node(xMid, xHi, yLo, yMid, zMid, zHi);
                        break;
                    case 6:
                        child = new Node(xMid, xHi, yMid, yHi, zLo, zMid);
                        break;
                    case 7:
                        child = new Node(xMid, xHi, yMid, yHi, zMid, zHi);
                        break;
                }
                children[childIdx] = child;
            }
            child.addParticle(x, y, z);
        }

        void preCompute() {

            // Precompute Centre of Mass of this node (where non-leaf node)
            // and opening threshold for force calculation.
            if (children != null) {
                double xSum = 0, ySum = 0, zSum = 0;

                for (int i = 0; i < 8; i++) {
                    Node child = children[i];
                    if (child != null) {
                        child.preCompute();
                        int nChild = child.nParticles;
                        xSum += nChild * child.xCent;
                        ySum += nChild * child.yCent;
                        zSum += nChild * child.zCent;
                    }
                }
                xCent = xSum / nParticles;
                yCent = ySum / nParticles;
                zCent = zSum / nParticles;
            }

            double delta = distance(xCent, yCent, zCent);
            double cellSize = xHi - xLo;
            threshold = cellSize / OPENING_ANGLE + delta;
        }

        void calcForce(Vector a, double x, double y, double z) {

            //nCellsOpened.incrementAndGet() ;  // debug - significantly impacts performance
            if (nParticles == 0) {
                return;
            }
            if (nParticles == 1) {
                if (x == xCent && y == yCent && z == zCent) {
                    return;
                } else {
                    forceLaw(a, x, y, z);
                    return;
                }
            }

            double r = distance(x, y, z);
            if (r > threshold) {
                forceLaw(a, x, y, z);
            } else {
                for (int n = 0; n < 8; n++) {
                    Node child = children[n];
                    if (child != null) {
                        child.calcForce(a, x, y, z);
                    }
                }
            }
        }

        double distance(double x, double y, double z) {

            // Distance from mid-point of this node.
            // This version assumes periodic box.
            double dx, dy, dz;  // separations in x and y directions
            double dx2, dy2, dz2, rSquared;
            dx = x - xMid;
            if (dx > BOX_WIDTH / 2) {
                dx -= BOX_WIDTH;
            }
            if (dx < -BOX_WIDTH / 2) {
                dx += BOX_WIDTH;
            }
            dy = y - yMid;
            if (dy > BOX_WIDTH / 2) {
                dy -= BOX_WIDTH;
            }
            if (dy < -BOX_WIDTH / 2) {
                dy += BOX_WIDTH;
            }
            dz = z - zMid;
            if (dz > BOX_WIDTH / 2) {
                dz -= BOX_WIDTH;
            }
            if (dz < -BOX_WIDTH / 2) {
                dz += BOX_WIDTH;
            }
            dx2 = dx * dx;
            dy2 = dy * dy;
            dz2 = dz * dz;
            rSquared = dx2 + dy2 + dz2;
            return Math.sqrt(rSquared);
        }

        void forceLaw(Vector a, double x, double y, double z) {

            // Force exerted by effective mass at CM of this node.
            double dx, dy, dz;  // separations in x and y directions
            double dx2, dy2, dz2, rSquared, r, massRCubedInv;

            // Vector version of inverse square law
            // This version assumes periodic box.
            dx = x - xCent;
            if (dx > BOX_WIDTH / 2) {
                dx -= BOX_WIDTH;
            }
            if (dx < -BOX_WIDTH / 2) {
                dx += BOX_WIDTH;
            }
            dy = y - yCent;
            if (dy > BOX_WIDTH / 2) {
                dy -= BOX_WIDTH;
            }
            if (dy < -BOX_WIDTH / 2) {
                dy += BOX_WIDTH;
            }
            dz = z - zCent;
            if (dz > BOX_WIDTH / 2) {
                dz -= BOX_WIDTH;
            }
            if (dz < -BOX_WIDTH / 2) {
                dz += BOX_WIDTH;
            }
            dx2 = dx * dx;
            dy2 = dy * dy;
            dz2 = dz * dz;
            rSquared = dx2 + dy2 + dz2;
            r = Math.sqrt(rSquared);
            massRCubedInv = nParticles / (rSquared * r);
            a.x -= massRCubedInv * dx;
            a.y -= massRCubedInv * dy;
            a.z -= massRCubedInv * dz;
        }
    }

    static class Vector {

        double x, y, z;

        Vector() {
        }

        Vector(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
