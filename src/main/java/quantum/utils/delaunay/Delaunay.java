//
// Copyright (c) 2009 Mario Zechner.
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the GNU Lesser Public License v2.1
// which accompanies this distribution, and is available at
// http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
// 
// Contributors:
//     Mario Zechner - initial API and implementation
//
package quantum.utils.delaunay;

//
//Delaunay.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2006 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/


/**
Delaunay represents an abstract class for calculating an
N-dimensional Delaunay triangulation, that can be extended
to allow for various triangulation algorithms.<P>
*/
public abstract strictfp class Delaunay implements java.io.Serializable {

/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/**
* triangles/tetrahedra --> vertices.<p>
* Tri = new int[ntris][dim + 1]
*
* <p>This is the key output, a list of triangles (in two dimensions,
* tetrahedra in three dimensions, etc). ntris is the number of triangles.
*
* <p>In 2-D, Tri[i] is an array of 3 integers, which are three indices into
* the samples[0] and samples[1] arrays to get the x and y values of the
* three vertices of the triangle.
*
* <p>In 3-D, Tri[i] is an array of 4 integers, which are four indices into
* the samples[0], samples[1] and samples[2] arrays to get the x, y and z
* values of the four vertices of the tetrahedron.
*
* <p>This pattern continues for higher dimensionalities.
*/
public int[][] Tri;

/**
* vertices --> triangles/tetrahedra.<p>
* Vertices = new int[nrs][nverts[i]]
*
* <p>nrs is the number of samples (the length of the samples[0] and
* samples[1] arrays. For sample i, Vertices[i] is a (variable length) list
* of indices into the Tri array above, giving the indices of the triangles
* that include vertex i.
*
* <p>nverts is an array as the second index of the Vertices array since
* different vertices may be part of different numbers of triangles.
*
* <p>You can use Tri and Vertices together to traverse the triangulation.
* If you don't need to traverse, then you can probably ignore all arrays
* except Tri.
*/
public int[][] Vertices;

/**
* triangles/tetrahedra --> triangles/tetrahedra.<p>
* Walk = new int[ntris][dim + 1]
*
* <p>Also useful for traversing the triangulation, in this case giving the
* indices of triangles that share edges with the current triangle.
*/
public int[][] Walk;

/**
* tri/tetra edges --> global edge number.<p>
* Edges = new int[ntris][3 * (dim - 1)];
*
* <p>'global edge number' is the number of an edge that is unique among the
* whole triangulation. This number is not an index into any array, but will
* match for a shared edge between two triangles.
*/
public int[][] Edges;

/** number of unique global edge numbers */
public int NumEdges;

private boolean nonConvex = false;

/**
* The abstract constructor initializes the class's data arrays.
* @throws VisADException a VisAD error occurred
*/
public Delaunay() throws Exception {
 Tri = null;
 Vertices = null;
 Walk = null;
 Edges = null;
 NumEdges = 0;
}

/**
* set flag indicating this Delaunay topology is non-convex
*/
public void setNonConvex() {
 nonConvex = true;
}

/**
* @return flag indicating whether this Delaunay topology is non-convex
*/
public boolean getNonConvex() {
 return nonConvex;
}


/**
* The factory class method heuristically decides which extension
* to the Delaunay abstract class to use in order to construct the
* fastest triangulation, and calls that extension, returning the
* finished triangulation. The method chooses from among the Fast,
* Clarkson, and Watson methods.
* @param samples locations of points for topology - dimensioned
*                float[dimension][number_of_points]
* @param exact flag indicating need for exact Delaunay triangulation
* @return a topology using an appropriate sub-class of Delaunay
* @throws VisADException a VisAD error occurred
*/
public static Delaunay triangulate(float[][] samples )
                                               throws Exception {

 /* Note: Clarkson doesn't work well for very closely clumped site values,
          since the algorithm rounds each value to the nearest integer
          before computing the triangulation.  This fact should probably
          be taken into account in this factory algorithm, but as of yet
          is not.  In other words, if you need an exact triangulation
          and have more than 3000 data sites, and they have closely
          clumped values, be sure to scale them up before calling the
          factory method. */

 /* Note: The factory method will not take new Delaunay extensions into
          account unless it is extended as well. */


     DelaunayWatson delan = new DelaunayWatson(samples);     
     return (Delaunay) delan;
}




/**
* check this triangulation in various ways to make sure it is
* constructed correctly. This method is expensive, provided
* mainly for debugging purposes.
* @param samples locations of points for topology - dimensioned
*                float[dimension][number_of_points]
* @return flag that is false to indicate there are problems with
*         the triangulation
*/
public boolean test(float[][] samples) {

 int dim = samples.length;
 int dim1 = dim+1;
 int ntris = Tri.length;
 int nrs = samples[0].length;
 for (int i=1; i<dim; i++) {
   nrs = Math.min(nrs, samples[i].length);
 }

 // verify triangulation dimension
 for (int i=0; i<ntris; i++) {
   if (Tri[i].length < dim1) return false;
 }

 // verify no illegal triangle vertices
 for (int i=0; i<ntris; i++) {
   for (int j=0; j<dim1; j++) {
     if (Tri[i][j] < 0 || Tri[i][j] >= nrs) return false;
   }
 }

 // verify that all points are in at least one triangle
 int[] nverts = new int[nrs];
 for (int i=0; i<nrs; i++) nverts[i] = 0;
 for (int i=0; i<ntris; i++) {
   for (int j=0; j<dim1; j++) nverts[Tri[i][j]]++;
 }
 for (int i=0; i<nrs; i++) {
   if (nverts[i] == 0) return false;
 }

 // test for duplicate triangles
 for (int i=0; i<ntris; i++) {
   for (int j=i+1; j<ntris; j++) {
     boolean[] m = new boolean[dim1];
     for (int mi=0; mi<dim1; mi++) m[mi] = false;
     for (int k=0; k<dim1; k++) {
       for (int l=0; l<dim1; l++) {
         if (Tri[i][k] == Tri[j][l] && !m[l]) {
           m[l] = true;
         }
       }
     }
     boolean mtot = true;
     for (int k=0; k<dim1; k++) {
       if (!m[k]) mtot = false;
     }
     if (mtot) return false;
   }
 }

 // test for errors in Walk array
 for (int i=0; i<ntris; i++) {
   for (int j=0; j<dim1; j++) {
     if (Walk[i][j] != -1) {
       boolean found = false;
       for (int k=0; k<dim1; k++) {
         if (Walk[Walk[i][j]][k] == i) found = true;
       }
       if (!found) return false;

       // make sure two walk'ed triangles share dim vertices
       int sb = 0;
       for (int k=0; k<dim1; k++) {
         for (int l=0; l<dim1; l++) {
           if (Tri[i][k] == Tri[Walk[i][j]][l]) sb++;
         }
       }
       if (sb != dim) return false;
     }
   }
 }

 // Note: Another test that could be performed is one that
 //       makes sure, given a triangle T, all points in the
 //       triangulation that are not part of T are located
 //       outside the bounds of T.  This test would verify
 //       that there are no overlapping triangles.

 // all tests passed
 return true;
}

/**
* use edge-flipping to bring the current triangulation closer
* to the true Delaunay triangulation.
* @param samples locations of points for topology - dimensioned
*                float[dimension][number_of_points]
* @param pass the number of passes the algorithm should take over
*             all edges (however, the algorithm terminates if no
*             edges are flipped for an entire pass). 
* @throws VisADException a VisAD error occurred
*/
public void improve(float[][] samples, int pass) throws Exception {
 int dim = samples.length;
 int dim1 = dim+1;
 if (Tri[0].length != dim1) {
   throw new Exception("Delaunay.improve: samples dimension " +
                          "does not match");
 }
 // only 2-D triangulations supported for now
 if (dim > 2) {
   throw new Exception("Delaunay.improve: dimension " +
                                    "must be 2!");
 }
 int ntris = Tri.length;
 int nrs = samples[0].length;
 for (int i=1; i<dim; i++) {
   nrs = Math.min(nrs, samples[i].length);
 }
 float[] samp0 = samples[0];
 float[] samp1 = samples[1];

 // go through entire triangulation pass times
 boolean eflipped = false;
 for (int p=0; p<pass; p++) {
   eflipped = false;

   // edge keeps track of which edges have been checked
   boolean[] edge = new boolean[NumEdges];
   for (int i=0; i<NumEdges; i++) edge[i] = true;

   // check every edge of every triangle
   for (int t=0; t<ntris; t++) {
     int[] trit = Tri[t];
     int[] walkt = Walk[t];
     int[] edgest = Edges[t];
     for (int e=0; e<2; e++) {
       int curedge = edgest[e];
       // only check the edge if it hasn't been checked yet
       if (edge[curedge]) {
         int t2 = walkt[e];

         // only check edge if it is not part of the outer hull
         if (t2 >= 0) {
           int[] trit2 = Tri[t2];
           int[] walkt2 = Walk[t2];
           int[] edgest2 = Edges[t2];

           // check if the diagonal needs to be flipped
           int f = (walkt2[0] == t) ? 0 :
                   (walkt2[1] == t) ? 1 : 2;
           int A = (e + 2) % 3;
           int B = (A + 1) % 3;
           int C = (B + 1) % 3;
           int D = (f + 2) % 3;
           float ax = samp0[trit[A]];
           float ay = samp1[trit[A]];
           float bx = samp0[trit[B]];
           float by = samp1[trit[B]];
           float cx = samp0[trit[C]];
           float cy = samp1[trit[C]];
           float dx = samp0[trit2[D]];
           float dy = samp1[trit2[D]];
           float abx = ax - bx;
           float aby = ay - by;
           float acx = ax - cx;
           float acy = ay - cy;
           float dbx = dx - bx;
           float dby = dy - by;
           float dcx = dx - cx;
           float dcy = dy - cy;
           float Q = abx*acx + aby*acy;
           float R = dbx*abx + dby*aby;
           float S = acx*dcx + acy*dcy;
           float T = dbx*dcx + dby*dcy;
           boolean QD = abx*acy - aby*acx >= 0;
           boolean RD = dbx*aby - dby*abx >= 0;
           boolean SD = acx*dcy - acy*dcx >= 0;
           boolean TD = dcx*dby - dcy*dbx >= 0;
           boolean sig = (QD ? 1 : 0) + (RD ? 1 : 0)
                       + (SD ? 1 : 0) + (TD ? 1 : 0) < 2;
           boolean d;
           if (QD == sig) d = true;
           else if (RD == sig) d = false;
           else if (SD == sig) d = false;
           else if (TD == sig) d = true;
           else if (Q < 0 && T < 0 || R > 0 && S > 0) d = true;
           else if (R < 0 && S < 0 || Q > 0 && T > 0) d = false;
           else if ((Q < 0 ? Q : T) < (R < 0 ? R : S)) d = true;
           else d = false;
           if (d) {
             // diagonal needs to be swapped
             eflipped = true;
             int n1 = trit[A];
             int n2 = trit[B];
             int n3 = trit[C];
             int n4 = trit2[D];
             int w1 = walkt[A];
             int w2 = walkt[C];
             int e1 = edgest[A];
             int e2 = edgest[C];
             int w3, w4, e3, e4;
             if (trit2[(D+1)%3] == trit[C]) {
               w3 = walkt2[D];
               w4 = walkt2[(D+2)%3];
               e3 = edgest2[D];
               e4 = edgest2[(D+2)%3];
             }
             else {
               w3 = walkt2[(D+2)%3];
               w4 = walkt2[D];
               e3 = edgest2[(D+2)%3];
               e4 = edgest2[D];
             }

             // update Tri array
             trit[0] = n1;
             trit[1] = n2;
             trit[2] = n4;
             trit2[0] = n1;
             trit2[1] = n4;
             trit2[2] = n3;

             // update Walk array
             walkt[0] = w1;
             walkt[1] = w4;
             walkt[2] = t2;
             walkt2[0] = t;
             walkt2[1] = w3;
             walkt2[2] = w2;
             if (w2 >= 0) {
               int val = (Walk[w2][0] == t) ? 0
                       : (Walk[w2][1] == t) ? 1 : 2;
               Walk[w2][val] = t2;
             }
             if (w4 >= 0) {
               int val = (Walk[w4][0] == t2) ? 0
                       : (Walk[w4][1] == t2) ? 1 : 2;
               Walk[w4][val] = t;
             }

             // update Edges array
             edgest[0] = e1;
             edgest[1] = e4;
             // Edges[t][2] and Edges[t2][0] stay the same
             edgest2[1] = e3;
             edgest2[2] = e2;

             // update Vertices array
             int[] vertn1 = Vertices[n1];
             int[] vertn2 = Vertices[n2];
             int[] vertn3 = Vertices[n3];
             int[] vertn4 = Vertices[n4];
             int ln1 = vertn1.length;
             int ln2 = vertn2.length;
             int ln3 = vertn3.length;
             int ln4 = vertn4.length;
             int[] tn1 = new int[ln1 + 1];  // Vertices[n1] adds t2
             int[] tn2 = new int[ln2 - 1];  // Vertices[n2] loses t2
             int[] tn3 = new int[ln3 - 1];  // Vertices[n3] loses t
             int[] tn4 = new int[ln4 + 1];  // Vertices[n4] adds t
             System.arraycopy(vertn1, 0, tn1, 0, ln1);
             tn1[ln1] = t2;
             int c = 0;
             for (int i=0; i<ln2; i++) {
               if (vertn2[i] != t2) tn2[c++] = vertn2[i];
             }
             c = 0;
             for (int i=0; i<ln3; i++) {
               if (vertn3[i] != t) tn3[c++] = vertn3[i];
             }
             System.arraycopy(vertn4, 0, tn4, 0, ln4);
             tn4[ln4] = t;
             Vertices[n1] = tn1;
             Vertices[n2] = tn2;
             Vertices[n3] = tn3;
             Vertices[n4] = tn4;
           }
         }

         // the edge has now been checked
         edge[curedge] = false;
       }
     }
   }

   // if no edges have been flipped this pass, then stop
   if (!eflipped) break;
 }
}

/**
* calculate a triangulation's helper arrays, Walk and Edges, if the
* triangulation algorithm hasn't calculated them already. Any
* extension to the Delaunay class should call finish_triang() at
* the end of its triangulation constructor.
* @param samples locations of points for topology - dimensioned
*                float[dimension][number_of_points]
* @throws VisADException a VisAD error occurred
*/
public void finish_triang(float[][] samples) throws Exception {
 int mdim = Tri[0].length - 1;
 int mdim1 = mdim + 1;
 int dim = samples.length;

 int ntris = Tri.length;
 int nrs = samples[0].length;
 for (int i=1; i<dim; i++) {
   nrs = Math.min(nrs, samples[i].length);
 }

 if (Vertices == null) {
   // build Vertices component
   Vertices = new int[nrs][];
   int[] nverts = new int[nrs];
   for (int i=0; i<ntris; i++) {
     for (int j=0; j<mdim1; j++) nverts[Tri[i][j]]++;
   }
   for (int i=0; i<nrs; i++) {
     Vertices[i] = new int[nverts[i]];
     nverts[i] = 0;
   }
   for (int i=0; i<ntris; i++) {
     for (int j=0; j<mdim1; j++) {
       Vertices[Tri[i][j]][nverts[Tri[i][j]]++] = i;
     }
   }
 }

 if (Walk == null && mdim <= 3) {
   // build Walk component
   Walk = new int[ntris][mdim1];
   for (int i=0; i<ntris; i++) {
   WalkDim:
     for (int j=0; j<mdim1; j++) {
       int v1 = j;
       int v2 = (v1+1)%mdim1;
       Walk[i][j] = -1;
       for (int k=0; k<Vertices[Tri[i][v1]].length; k++) {
         int temp = Vertices[Tri[i][v1]][k];
         if (temp != i) {
           for (int l=0; l<Vertices[Tri[i][v2]].length; l++) {
             if (mdim == 2) {
               if (temp == Vertices[Tri[i][v2]][l]) {
                 Walk[i][j] = temp;
                 continue WalkDim;
               }
             }
             else {    // mdim == 3
               int temp2 = Vertices[Tri[i][v2]][l];
               int v3 = (v2+1)%mdim1;
               if (temp == temp2) {
                 for (int m=0; m<Vertices[Tri[i][v3]].length; m++) {
                   if (temp == Vertices[Tri[i][v3]][m]) {
                     Walk[i][j] = temp;
                     continue WalkDim;
                   }
                 }
               }
             } // end if (mdim == 3)
           } // end for (int l=0; l<Vertices[Tri[i][v2]].length; l++)
         } // end if (temp != i)
       } // end for (int k=0; k<Vertices[Tri[i][v1]].length; k++)
     } // end for (int j=0; j<mdim1; j++)
   } // end for (int i=0; i<Tri.length; i++)
 } // end if (Walk == null && mdim <= 3)

 if (Edges == null && mdim <= 3) {
   // build Edges component

   // initialize all edges to "not yet found"
   int edim = 3*(mdim-1);
   Edges = new int[ntris][edim];
   for (int i=0; i<ntris; i++) {
     for (int j=0; j<edim; j++) Edges[i][j] = -1;
   }

   // calculate global edge values
   NumEdges = 0;
   if (mdim == 2) {
     for (int i=0; i<ntris; i++) {
       for (int j=0; j<3; j++) {
         if (Edges[i][j] < 0) {
           // this edge doesn't have a "global edge number" yet
           int othtri = Walk[i][j];
           if (othtri >= 0) {
             int cside = -1;
             for (int k=0; k<3; k++) {
               if (Walk[othtri][k] == i) cside = k;
             }
             if (cside != -1) {
               Edges[othtri][cside] = NumEdges;
             }
             else {
               throw new Exception("Delaunay.finish_triang: " +
                                      "error in triangulation!");
             }
           }
           Edges[i][j] = NumEdges++;
         }
       }
     }
   }
   else {    // mdim == 3
     int[] ptlook1 = {0, 0, 0, 1, 1, 2};
     int[] ptlook2 = {1, 2, 3, 2, 3, 3};
     for (int i=0; i<ntris; i++) {
       for (int j=0; j<6; j++) {
         if (Edges[i][j] < 0) {
           // this edge doesn't have a "global edge number" yet

           // search through the edge's two end points
           int endpt1 = Tri[i][ptlook1[j]];
           int endpt2 = Tri[i][ptlook2[j]];

           // create an intersection of two sets
           int[] set = new int[Vertices[endpt1].length];
           int setlen = 0;
           for (int p1=0; p1<Vertices[endpt1].length; p1++) {
             int temp = Vertices[endpt1][p1];
             for (int p2=0; p2<Vertices[endpt2].length; p2++) {
               if (temp == Vertices[endpt2][p2]) {
                 set[setlen++] = temp;
                 break;
               }
             }
           }

           // assign global edge number to all members of set
           for (int kk=0; kk<setlen; kk++) {
             int k = set[kk];
             for (int l=0; l<edim; l++) {
               if ((Tri[k][ptlook1[l]] == endpt1
                 && Tri[k][ptlook2[l]] == endpt2)
                || (Tri[k][ptlook1[l]] == endpt2
                 && Tri[k][ptlook2[l]] == endpt1)) {
                 Edges[k][l] = NumEdges;
               }
             }
           }
           Edges[i][j] = NumEdges++;
         } // end if (Edges[i][j] < 0)
       } // end for (int j=0; j<6; j++)
     } // end for (int i=0; i<ntris; i++)
   } // end if (mdim == 3)
 } // end if (Edges == null && mdim <= 3)
}
/*
public void finish_triang(float[][] samples) throws VisADException {
 int dim = samples.length;
 int dim1 = dim+1;
 int ntris = Tri.length;
 int nrs = samples[0].length;
 for (int i=1; i<dim; i++) {
   nrs = Math.min(nrs, samples[i].length);
 }

 if (Vertices == null) {
   // build Vertices component
   Vertices = new int[nrs][];
   int[] nverts = new int[nrs];
   for (int i=0; i<ntris; i++) {
     for (int j=0; j<dim1; j++) nverts[Tri[i][j]]++;
   }
   for (int i=0; i<nrs; i++) {
     Vertices[i] = new int[nverts[i]];
     nverts[i] = 0;
   }
   for (int i=0; i<ntris; i++) {
     for (int j=0; j<dim1; j++) {
       Vertices[Tri[i][j]][nverts[Tri[i][j]]++] = i;
     }
   }
 }

 if (Walk == null && dim <= 3) {
   // build Walk component
   Walk = new int[ntris][dim1];
   for (int i=0; i<Tri.length; i++) {
   WalkDim:
     for (int j=0; j<dim1; j++) {
       int v1 = j;
       int v2 = (v1+1)%dim1;
       Walk[i][j] = -1;
       for (int k=0; k<Vertices[Tri[i][v1]].length; k++) {
         int temp = Vertices[Tri[i][v1]][k];
         if (temp != i) {
           for (int l=0; l<Vertices[Tri[i][v2]].length; l++) {
             if (dim == 2) {
               if (temp == Vertices[Tri[i][v2]][l]) {
                 Walk[i][j] = temp;
                 continue WalkDim;
               }
             }
             else {    // dim == 3
               int temp2 = Vertices[Tri[i][v2]][l];
               int v3 = (v2+1)%dim1;
               if (temp == temp2) {
                 for (int m=0; m<Vertices[Tri[i][v3]].length; m++) {
                   if (temp == Vertices[Tri[i][v3]][m]) {
                     Walk[i][j] = temp;
                     continue WalkDim;
                   }
                 }
               }
             } // end if (dim == 3)
           } // end for (int l=0; l<Vertices[Tri[i][v2]].length; l++)
         } // end if (temp != i)
       } // end for (int k=0; k<Vertices[Tri[i][v1]].length; k++)
     } // end for (int j=0; j<dim1; j++)
   } // end for (int i=0; i<Tri.length; i++)
 } // end if (Walk == null && dim <= 3)

 if (Edges == null && dim <= 3) {
   // build Edges component

   // initialize all edges to "not yet found"
   int edim = 3*(dim-1);
   Edges = new int[ntris][edim];
   for (int i=0; i<ntris; i++) {
     for (int j=0; j<edim; j++) Edges[i][j] = -1;
   }

   // calculate global edge values
   NumEdges = 0;
   if (dim == 2) {
     for (int i=0; i<ntris; i++) {
       for (int j=0; j<3; j++) {
         if (Edges[i][j] < 0) {
           // this edge doesn't have a "global edge number" yet
           int othtri = Walk[i][j];
           if (othtri >= 0) {
             int cside = -1;
             for (int k=0; k<3; k++) {
               if (Walk[othtri][k] == i) cside = k;
             }
             if (cside != -1) {
               Edges[othtri][cside] = NumEdges;
             }
             else {
               throw new SetException("Delaunay.finish_triang: " +
                                      "error in triangulation!");
             }
           }
           Edges[i][j] = NumEdges++;
         }
       }
     }
   }
   else {    // dim == 3
     int[] ptlook1 = {0, 0, 0, 1, 1, 2};
     int[] ptlook2 = {1, 2, 3, 2, 3, 3};
     for (int i=0; i<ntris; i++) {
       for (int j=0; j<6; j++) {
         if (Edges[i][j] < 0) {
           // this edge doesn't have a "global edge number" yet

           // search through the edge's two end points
           int endpt1 = Tri[i][ptlook1[j]];
           int endpt2 = Tri[i][ptlook2[j]];

           // create an intersection of two sets
           int[] set = new int[Vertices[endpt1].length];
           int setlen = 0;
           for (int p1=0; p1<Vertices[endpt1].length; p1++) {
             int temp = Vertices[endpt1][p1];
             for (int p2=0; p2<Vertices[endpt2].length; p2++) {
               if (temp == Vertices[endpt2][p2]) {
                 set[setlen++] = temp;
                 break;
               }
             }
           }

           // assign global edge number to all members of set
           for (int kk=0; kk<setlen; kk++) {
             int k = set[kk];
             for (int l=0; l<edim; l++) {
               if ((Tri[k][ptlook1[l]] == endpt1
                 && Tri[k][ptlook2[l]] == endpt2)
                || (Tri[k][ptlook1[l]] == endpt2
                 && Tri[k][ptlook2[l]] == endpt1)) {
                 Edges[k][l] = NumEdges;
               }
             }
           }
           Edges[i][j] = NumEdges++;
         } // end if (Edges[i][j] < 0)
       } // end for (int j=0; j<6; j++)
     } // end for (int i=0; i<ntris; i++)
   } // end if (dim == 3)
 } // end if (Edges == null && dim <= 3)
}
*/

/**
* @return a String representation of this
*/
public String toString() {
 return sampleString(null);
}

/**
* @param samples locations of points for topology - dimensioned
*                float[dimension][number_of_points] - may be null
* @return a String representation of this, including samples if
*         it is non-null
*/
public String sampleString(float[][] samples) {
 StringBuffer s = new StringBuffer("");
 if (samples != null) {
   s.append("\nsamples " + samples[0].length + "\n");
   for (int i=0; i<samples[0].length; i++) {
     s.append("  " + i + " -> " + samples[0][i] + " " +
              samples[1][i] + " " + samples[2][i] + "\n");
   }
   s.append("\n");
 }

 s.append("\nTri (triangles -> vertices) " + Tri.length + "\n");
 for (int i=0; i<Tri.length; i++) {
   s.append("  " + i + " -> ");
   for (int j=0; j<Tri[i].length; j++) {
     s.append(" " + Tri[i][j]);
   }
   s.append("\n");
 }

 s.append("\nVertices (vertices -> triangles) " + Vertices.length + "\n");
 for (int i=0; i<Vertices.length; i++) {
   s.append("  " + i + " -> ");
   for (int j=0; j<Vertices[i].length; j++) {
     s.append(" " + Vertices[i][j]);
   }
   s.append("\n");
 }

 s.append("\nWalk (triangles -> triangles) " + Walk.length + "\n");
 for (int i=0; i<Walk.length; i++) {
   s.append("  " + i + " -> ");
   for (int j=0; j<Walk[i].length; j++) {
     s.append(" " + Walk[i][j]);
   }
   s.append("\n");
 }

 s.append("\nEdges (triangles -> global edges) " + Edges.length + "\n");
 for (int i=0; i<Edges.length; i++) {
   s.append("  " + i + " -> ");
   for (int j=0; j<Edges[i].length; j++) {
     s.append(" " + Edges[i][j]);
   }
   s.append("\n");
 }
 return s.toString();
}

}
