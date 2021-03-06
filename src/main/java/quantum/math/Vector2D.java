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

package quantum.math;

public final strictfp class Vector2D {
	public float x, y;

	public Vector2D () {

	}

	public Vector2D (float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2D (Vector2D v) {
		set(v);
	}

	public Vector2D cpy () {
		return new Vector2D(this);
	}

	public float len () {
		return (float)Math.sqrt(x * x + y * y);
	}

	public float len2 () {
		return x * x + y * y;
	}

	public Vector2D set (Vector2D v) {
		x = v.x;
		y = v.y;
		return this;
	}

	public Vector2D set (float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	public Vector2D sub (Vector2D v) {
		x -= v.x;
		y -= v.y;
		return this;
	}

	public Vector2D nor () {
		float len = len();
		if (len != 0) {
			x /= len;
			y /= len;
		}
		return this;
	}

	public Vector2D add (Vector2D v) {
		x += v.x;
		y += v.y;
		return this;
	}

	public Vector2D add (float x, float y) {
		this.x += x;
		this.y += y;
		return this;
	}

	public float dot (Vector2D v) {
		return x * v.x + y * v.y;
	}

	public Vector2D mul (float scalar) {
		x *= scalar;
		y *= scalar;
		return this;
	}

	public float dst (Vector2D v) {
		float x_d = v.x - x;
		float y_d = v.y - y;
		return (float)Math.sqrt(x_d * x_d + y_d * y_d);
	}

	public float dst (float x, float y) {
		float x_d = x - this.x;
		float y_d = y - this.y;
		return (float)Math.sqrt(x_d * x_d + y_d * y_d);
	}

	public float dst2 (Vector2D v) {
		float x_d = v.x - x;
		float y_d = v.y - y;
		return x_d * x_d + y_d * y_d;
	}

	public String toString () {
		return "[" + x + ":" + y + "]";
	}
}
