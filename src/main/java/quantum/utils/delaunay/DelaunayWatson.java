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
//DelaunayWatson.java
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

import java.util.*;

/* The Delaunay triangulation/tetrahedralization algorithm in this class is
* originally from nnsort.c by David F. Watson:
*
* nnsort() finds the Delaunay triangulation of the two- or three-component
* vectors in 'data_list' and returns a list of simplex vertices in
* 'vertices' with the corresponding circumcentre and squared radius in the
* rows of 'circentres'.  nnsort() also can be used to find the ordered
* convex hull of the two- or three-component vectors in 'data_list' and
* returns a list of (d-1)-facet vertices in 'vertices' (dummy filename for
* 'circentres' must be used).
* nnsort() was written by Dave Watson and uses the algorithm described in -
*    Watson, D.F., 1981, Computing the n-dimensional Delaunay tessellation
*          with application to Voronoi polytopes:
*                                      The Computer J., 24(2), p. 167-172.
*
* additional information about this algorithm can be found in -
*    CONTOURING: A guide to the analysis and display of spatial data,
*    by David F. Watson, Pergamon Press, 1992, ISBN 0 08 040286 0
*                                                                        */

/**
DelaunayWatson represents an O(N^2) method with low overhead
to find the Delaunay triangulation or tetrahedralization of
a set of samples of R^2 or R^3.<P>
*/
public  strictfp class DelaunayWatson extends Delaunay {

/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
private static final float BIGNUM = (float) 1E37;
private static final float EPSILON = 0.00001f;
// temporary storage size factor
private static final int TSIZE = 75;
// factor (>=1) for radius of control points
private static final float RANGE = 10.0f;

/**
* construct a Delaunay triangulation of the points in the
* samples array using Watson's algorithm
* @param samples locations of points for topology - dimensioned
*                float[dimension][number_of_points]
* @throws VisADException a VisAD error occurred
*/
public DelaunayWatson(float[][] samples) throws Exception {
 int dim = samples.length;
 int nrs = samples[0].length;

 float xx, bgs;
 int i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, i11;
 int[] ii = new int[3];
 int dm, dim1, nts, tsz;

 float[][] mxy = new float[2][dim];
 for (i0=0; i0<dim; i0++) mxy[0][i0] = - (mxy[1][i0] = BIGNUM);
 dim1 = dim + 1;
 float[][] wrk = new float[dim][dim1];
 for (i0=0; i0<dim; i0++) for (i1=0; i1<dim1; i1++) wrk[i0][i1] = -RANGE;
 for (i0=0; i0<dim; i0++) wrk[i0][i0] = RANGE * (3 * dim - 1);

 float[][] pts = new float[nrs + dim1][dim];
 for (i0=0; i0<nrs; i0++) {
   if (dim < 3) {
     pts[i0][0] = samples[0][i0];
     pts[i0][1] = samples[1][i0];
   }
   else {
     pts[i0][0] = samples[0][i0];
     pts[i0][1] = samples[1][i0];
     pts[i0][2] = samples[2][i0];
   }
   // compute bounding box
   for (i1=0; i1<dim; i1++) {
     if (mxy[0][i1] < pts[i0][i1]) mxy[0][i1] = pts[i0][i1]; // max
     if (mxy[1][i1] > pts[i0][i1]) mxy[1][i1] = pts[i0][i1]; // min
   }
 }

 for (bgs=0, i0=0; i0<dim; i0++)  {
   mxy[0][i0] -= mxy[1][i0];
   if (bgs < mxy[0][i0]) bgs = mxy[0][i0];
 }
 // now bgs = largest range
 // add random perturbations to points
 bgs *= EPSILON;

 Random rand = new Random(367);
 for (i0=0; i0<nrs; i0++) for (i1=0; i1<dim; i1++) {
   // random numbers [0, 1]
   pts[i0][i1] += bgs * (0.5 - rand.nextDouble() / 0x7fffffff);
 }
 for (i0=0; i0<dim1; i0++) for (i1=0; i1<dim; i1++) {
   pts[nrs+i0][i1] = mxy[1][i1] + wrk[i1][i0] * mxy[0][i1];
 }
 for (i1=1, i0=2; i0<dim1; i0++) i1 *= i0;
 tsz = TSIZE * i1;
 int[][] tmp = new int[tsz + 1][dim];
 // storage allocation - increase value of `i1' for 3D if necessary
 i1 *= (nrs + 50 * i1);
/* WLH 4 Nov 97 */
 if (dim == 3) i1 *= 10;
/* end WLH 4 Nov 97 */
 int[] id = new int[i1];
 for (i0=0; i0<i1; i0++) id[i0] = i0;
 int[][] a3s = new int[i1][dim1];
 float[][] ccr = new float[i1][dim1];
 for (a3s[0][0]=nrs, i0=1; i0<dim1; i0++) a3s[0][i0] = a3s[0][i0-1] + 1;
 for (ccr[0][dim]=BIGNUM, i0=0; i0<dim; i0++) ccr[0][i0] = 0;
 nts = i4 = 1;
 dm = dim - 1;
 for (i0=0; i0<nrs; i0++) {
   i1 = i7 = -1;
   i9 = 0;
Loop3:
   for (i11=0; i11<nts; i11++) {
     i1++;
     while (a3s[i1][0] < 0) i1++;
     xx = ccr[i1][dim];
     for (i2=0; i2<dim; i2++) {
       xx -= (pts[i0][i2] - ccr[i1][i2]) * (pts[i0][i2] - ccr[i1][i2]);
       if (xx<0) continue Loop3;
     }
     i9--;
     i4--;
     id[i4] = i1;
Loop2:
     for (i2=0; i2<dim1; i2++) {
       ii[0] = 0;
       if (ii[0] == i2) ii[0]++;
       for (i3=1; i3<dim; i3++) {
         ii[i3] = ii[i3-1] + 1;
         if (ii[i3] == i2) ii[i3]++;
       }
       if (i7>dm) {
         i8 = i7;
Loop1:
         for (i3=0; i3<=i8; i3++) {
           for (i5=0; i5<dim; i5++) {
             if (a3s[i1][ii[i5]] != tmp[i3][i5]) continue Loop1;
           }
           for (i6=0; i6<dim; i6++) tmp[i3][i6] = tmp[i8][i6];
           i7--;
           continue Loop2;
         }
       }
       if (++i7 > tsz) {
         int newtsz = 2 * tsz;
         int[][] newtmp = new int[newtsz + 1][dim];
         System.arraycopy(tmp, 0, newtmp, 0, tsz);
         tsz = newtsz;
         tmp = newtmp;
         // WLH 23 july 97
         // throw new VisADException(
         //                "DelaunayWatson: Temporary storage exceeded");
       }
       for (i3=0; i3<dim; i3++) tmp[i7][i3] = a3s[i1][ii[i3]];
     }
     a3s[i1][0] = -1;
   }
   for (i1=0; i1<=i7; i1++) {
     for (i2=0; i2<dim; i2++) {
       for (wrk[i2][dim]=0, i3=0; i3<dim; i3++) {
         wrk[i2][i3] = pts[tmp[i1][i2]][i3] - pts[i0][i3];
         wrk[i2][dim] += wrk[i2][i3] * (pts[tmp[i1][i2]][i3]
                                     + pts[i0][i3]) / 2;
       }
     }
     if (dim < 3) {
       xx = wrk[0][0] * wrk[1][1] - wrk[1][0] * wrk[0][1];
       ccr[id[i4]][0] = (wrk[0][2] * wrk[1][1]
                      - wrk[1][2] * wrk[0][1]) / xx;
       ccr[id[i4]][1] = (wrk[0][0] * wrk[1][2]
                      - wrk[1][0] * wrk[0][2]) / xx;
     }
     else {
       xx = (wrk[0][0] * (wrk[1][1] * wrk[2][2] - wrk[2][1] * wrk[1][2]))
          - (wrk[0][1] * (wrk[1][0] * wrk[2][2] - wrk[2][0] * wrk[1][2]))
          + (wrk[0][2] * (wrk[1][0] * wrk[2][1] - wrk[2][0] * wrk[1][1]));
       ccr[id[i4]][0] = ((wrk[0][3] * (wrk[1][1] * wrk[2][2]
                        - wrk[2][1] * wrk[1][2]))
                       - (wrk[0][1] * (wrk[1][3] * wrk[2][2]
                        - wrk[2][3] * wrk[1][2]))
                       + (wrk[0][2] * (wrk[1][3] * wrk[2][1]
                        - wrk[2][3] * wrk[1][1]))) / xx;
       ccr[id[i4]][1] = ((wrk[0][0] * (wrk[1][3] * wrk[2][2]
                        - wrk[2][3] * wrk[1][2]))
                       - (wrk[0][3] * (wrk[1][0] * wrk[2][2]
                        - wrk[2][0] * wrk[1][2]))
                       + (wrk[0][2] * (wrk[1][0] * wrk[2][3]
                        - wrk[2][0] * wrk[1][3]))) / xx;
       ccr[id[i4]][2] = ((wrk[0][0] * (wrk[1][1] * wrk[2][3]
                        - wrk[2][1] * wrk[1][3]))
                       - (wrk[0][1] * (wrk[1][0] * wrk[2][3]
                        - wrk[2][0] * wrk[1][3]))
                       + (wrk[0][3] * (wrk[1][0] * wrk[2][1]
                        - wrk[2][0] * wrk[1][1]))) / xx;
     }
     for (ccr[id[i4]][dim]=0, i2=0; i2<dim; i2++) {
       ccr[id[i4]][dim] += (pts[i0][i2] - ccr[id[i4]][i2])
                         * (pts[i0][i2] - ccr[id[i4]][i2]);
       a3s[id[i4]][i2] = tmp[i1][i2];
     }
     a3s[id[i4]][dim] = i0;
     i4++;
     i9++;
   }
   nts += i9;
 }

/* OUTPUT is in a3s ARRAY
needed output is:
  Tri      - array of pointers from triangles or tetrahedra to their
             corresponding vertices
  Vertices - array of pointers from vertices to their
             corresponding triangles or tetrahedra
  Walk     - array of pointers from triangles or tetrahedra to neighboring
             triangles or tetrahedra
  Edges    - array of pointers from each triangle or tetrahedron's edges
             to their corresponding triangles or tetrahedra

helpers:
  nverts - number of triangles or tetrahedra per vertex
*/

 // compute number of triangles or tetrahedra
 int[] nverts = new int[nrs];
 for (int i=0; i<nrs; i++) nverts[i] = 0;
 int ntris = 0;
 i0 = -1;
 for (i11=0; i11<nts; i11++) {
   i0++;
   while (a3s[i0][0] < 0) i0++;
   if (a3s[i0][0] < nrs) {
     ntris++;
     if (dim < 3) {
       nverts[a3s[i0][0]]++;
       nverts[a3s[i0][1]]++;
       nverts[a3s[i0][2]]++;
     }
     else {
       nverts[a3s[i0][0]]++;
       nverts[a3s[i0][1]]++;
       nverts[a3s[i0][2]]++;
       nverts[a3s[i0][3]]++;
     }
   }
 }
 Vertices = new int[nrs][];
 for (int i=0; i<nrs; i++) Vertices[i] = new int[nverts[i]];
 for (int i=0; i<nrs; i++) nverts[i] = 0;

 // build Tri & Vertices components
 Tri = new int[ntris][dim1];
 int a, b, c, d;
 int itri = 0;
 i0 = -1;
 for (i11=0; i11<nts; i11++) {
   i0++;
   while (a3s[i0][0] < 0) i0++;
   if (a3s[i0][0] < nrs) {
     if (dim < 3) {
       a = a3s[i0][0];
       b = a3s[i0][1];
       c = a3s[i0][2];
       Vertices[a][nverts[a]] = itri;
       nverts[a]++;
       Vertices[b][nverts[b]] = itri;
       nverts[b]++;
       Vertices[c][nverts[c]] = itri;
       nverts[c]++;
       Tri[itri][0] = a;
       Tri[itri][1] = b;
       Tri[itri][2] = c;
     }
     else {
       a = a3s[i0][0];
       b = a3s[i0][1];
       c = a3s[i0][2];
       d = a3s[i0][3];
       Vertices[a][nverts[a]] = itri;
       nverts[a]++;
       Vertices[b][nverts[b]] = itri;
       nverts[b]++;
       Vertices[c][nverts[c]] = itri;
       nverts[c]++;
       Vertices[d][nverts[d]] = itri;
       nverts[d]++;
       Tri[itri][0] = a;
       Tri[itri][1] = b;
       Tri[itri][2] = c;
       Tri[itri][3] = d;
     }
     itri++;
   }
 }

 // call more generic method for constructing Walk and Edges arrays
 finish_triang(samples);
}

/*
DO NOT DELETE THIS COMMENTED CODE
IT CONTAINS ALGORITHM DETAILS NOT CAST INTO JAVA (YET?)
i0 = -1;
for (i11=0; i11<nts; i11++) {
 i0++;
 while (a3s[i0][0] < 0) i0++;
 if (a3s[i0][0] < nrs) {
   for (i1=0; i1<dim; i1++) for (i2=0; i2<dim; i2++) {
     wrk[i1][i2] = pts[a3s[i0][i1]][i2] - pts[a3s[i0][dim]][i2];
   }
   if (dim < 3) {
     xx = wrk[0][0] * wrk[1][1] - wrk[0][1] * wrk[1][0];
     if (fabs(xx) > EPSILON) {
       if (xx < 0)
         fprintf(afile,"%d %d %d\n",a3s[i0][0],a3s[i0][2],a3s[i0][1]);
       else fprintf(afile,"%d %d %d\n",a3s[i0][0],a3s[i0][1],a3s[i0][2]);
       fprintf(bfile,"%e %e %e\n",ccr[i0][0],ccr[i0][1],ccr[i0][2]);
     }
   }
   else {
     xx = ((wrk[0][0] * (wrk[1][1] * wrk[2][2] - wrk[2][1] * wrk[1][2]))
        -  (wrk[0][1] * (wrk[1][0] * wrk[2][2] - wrk[2][0] * wrk[1][2]))
        +  (wrk[0][2] * (wrk[1][0] * wrk[2][1] - wrk[2][0] * wrk[1][1])));
     if (fabs(xx) > EPSILON) {
       if (xx < 0)
         fprintf(afile,"%d %d %d %d\n",
                 a3s[i0][0],a3s[i0][1],a3s[i0][3],a3s[i0][2]);
       else fprintf(afile,"%d %d %d %d\n",
                    a3s[i0][0],a3s[i0][1],a3s[i0][2],a3s[i0][3]);
       fprintf(bfile,"%e %e %e %e\n",
               ccr[i0][0],ccr[i0][1],ccr[i0][2],ccr[i0][3]);
     }
   }
 }
}
*/

}


