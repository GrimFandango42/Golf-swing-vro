package com.swingsync.ai.ar

import kotlin.math.*

/**
 * 3D Vector utility class for AR swing plane calculations
 * Optimized for mobile performance with efficient math operations
 */
data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    
    // Common vector constants
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val UP = Vector3(0f, 1f, 0f)
        val FORWARD = Vector3(0f, 0f, 1f)
        val RIGHT = Vector3(1f, 0f, 0f)
    }

    /**
     * Vector magnitude (length)
     */
    val magnitude: Float
        get() = sqrt(x * x + y * y + z * z)

    /**
     * Squared magnitude (faster than magnitude for comparisons)
     */
    val sqrMagnitude: Float
        get() = x * x + y * y + z * z

    /**
     * Normalized vector (unit vector)
     */
    val normalized: Vector3
        get() {
            val mag = magnitude
            return if (mag > 0.0001f) {
                Vector3(x / mag, y / mag, z / mag)
            } else {
                ZERO
            }
        }

    /**
     * Vector addition
     */
    operator fun plus(other: Vector3): Vector3 {
        return Vector3(x + other.x, y + other.y, z + other.z)
    }

    /**
     * Vector subtraction
     */
    operator fun minus(other: Vector3): Vector3 {
        return Vector3(x - other.x, y - other.y, z - other.z)
    }

    /**
     * Scalar multiplication
     */
    operator fun times(scalar: Float): Vector3 {
        return Vector3(x * scalar, y * scalar, z * scalar)
    }

    /**
     * Component-wise multiplication
     */
    operator fun times(other: Vector3): Vector3 {
        return Vector3(x * other.x, y * other.y, z * other.z)
    }

    /**
     * Scalar division
     */
    operator fun div(scalar: Float): Vector3 {
        return Vector3(x / scalar, y / scalar, z / scalar)
    }

    /**
     * Dot product
     */
    fun dot(other: Vector3): Float {
        return x * other.x + y * other.y + z * other.z
    }

    /**
     * Cross product
     */
    fun cross(other: Vector3): Vector3 {
        return Vector3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    /**
     * Distance to another vector
     */
    fun distanceTo(other: Vector3): Float {
        return (this - other).magnitude
    }

    /**
     * Squared distance to another vector (faster for comparisons)
     */
    fun sqrDistanceTo(other: Vector3): Float {
        return (this - other).sqrMagnitude
    }

    /**
     * Linear interpolation between two vectors
     */
    fun lerp(other: Vector3, t: Float): Vector3 {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector3(
            x + (other.x - x) * clampedT,
            y + (other.y - y) * clampedT,
            z + (other.z - z) * clampedT
        )
    }

    /**
     * Angle between two vectors in radians
     */
    fun angleTo(other: Vector3): Float {
        val dot = this.normalized.dot(other.normalized)
        return acos(dot.coerceIn(-1f, 1f))
    }

    /**
     * Project this vector onto another vector
     */
    fun projectOnto(other: Vector3): Vector3 {
        val otherNorm = other.normalized
        return otherNorm * this.dot(otherNorm)
    }

    /**
     * Reflect this vector off a surface with given normal
     */
    fun reflect(normal: Vector3): Vector3 {
        return this - normal * (2f * this.dot(normal))
    }

    /**
     * Convert to FloatArray for OpenGL
     */
    fun toFloatArray(): FloatArray {
        return floatArrayOf(x, y, z)
    }

    /**
     * Convert to homogeneous coordinates (w=1)
     */
    fun toHomogeneous(): FloatArray {
        return floatArrayOf(x, y, z, 1f)
    }

    override fun toString(): String {
        return "Vector3(${String.format("%.3f", x)}, ${String.format("%.3f", y)}, ${String.format("%.3f", z)})"
    }
}

/**
 * 3D Plane representation for swing plane calculations
 */
data class Plane(
    val normal: Vector3,
    val distance: Float
) {
    
    constructor(point: Vector3, normal: Vector3) : this(
        normal.normalized,
        normal.normalized.dot(point)
    )

    /**
     * Check if a point is on the plane
     */
    fun containsPoint(point: Vector3, tolerance: Float = 0.001f): Boolean {
        return abs(distanceToPoint(point)) <= tolerance
    }

    /**
     * Distance from a point to the plane
     */
    fun distanceToPoint(point: Vector3): Float {
        return normal.dot(point) - distance
    }

    /**
     * Project a point onto the plane
     */
    fun projectPoint(point: Vector3): Vector3 {
        val dist = distanceToPoint(point)
        return point - (normal * dist)
    }

    /**
     * Find intersection of a line with the plane
     */
    fun intersectLine(lineStart: Vector3, lineDirection: Vector3): Vector3? {
        val denominator = normal.dot(lineDirection)
        if (abs(denominator) < 0.0001f) return null // Line is parallel to plane
        
        val t = (distance - normal.dot(lineStart)) / denominator
        return lineStart + (lineDirection * t)
    }
}

/**
 * Matrix4x4 for 3D transformations
 */
data class Matrix4(
    val m00: Float = 1f, val m01: Float = 0f, val m02: Float = 0f, val m03: Float = 0f,
    val m10: Float = 0f, val m11: Float = 1f, val m12: Float = 0f, val m13: Float = 0f,
    val m20: Float = 0f, val m21: Float = 0f, val m22: Float = 1f, val m23: Float = 0f,
    val m30: Float = 0f, val m31: Float = 0f, val m32: Float = 0f, val m33: Float = 1f
) {
    
    companion object {
        val IDENTITY = Matrix4()
        
        /**
         * Create translation matrix
         */
        fun translation(x: Float, y: Float, z: Float): Matrix4 {
            return Matrix4(
                1f, 0f, 0f, x,
                0f, 1f, 0f, y,
                0f, 0f, 1f, z,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Create rotation matrix around Y axis
         */
        fun rotationY(angleRad: Float): Matrix4 {
            val cos = cos(angleRad)
            val sin = sin(angleRad)
            return Matrix4(
                cos, 0f, sin, 0f,
                0f, 1f, 0f, 0f,
                -sin, 0f, cos, 0f,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Create rotation matrix around X axis
         */
        fun rotationX(angleRad: Float): Matrix4 {
            val cos = cos(angleRad)
            val sin = sin(angleRad)
            return Matrix4(
                1f, 0f, 0f, 0f,
                0f, cos, -sin, 0f,
                0f, sin, cos, 0f,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Create rotation matrix around Z axis
         */
        fun rotationZ(angleRad: Float): Matrix4 {
            val cos = cos(angleRad)
            val sin = sin(angleRad)
            return Matrix4(
                cos, -sin, 0f, 0f,
                sin, cos, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
        }

        /**
         * Create perspective projection matrix
         */
        fun perspective(fovYRad: Float, aspectRatio: Float, near: Float, far: Float): Matrix4 {
            val f = 1f / tan(fovYRad / 2f)
            val rangeInv = 1f / (near - far)
            
            return Matrix4(
                f / aspectRatio, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, (near + far) * rangeInv, 2f * near * far * rangeInv,
                0f, 0f, -1f, 0f
            )
        }
    }

    /**
     * Matrix multiplication
     */
    operator fun times(other: Matrix4): Matrix4 {
        return Matrix4(
            m00 * other.m00 + m01 * other.m10 + m02 * other.m20 + m03 * other.m30,
            m00 * other.m01 + m01 * other.m11 + m02 * other.m21 + m03 * other.m31,
            m00 * other.m02 + m01 * other.m12 + m02 * other.m22 + m03 * other.m32,
            m00 * other.m03 + m01 * other.m13 + m02 * other.m23 + m03 * other.m33,
            
            m10 * other.m00 + m11 * other.m10 + m12 * other.m20 + m13 * other.m30,
            m10 * other.m01 + m11 * other.m11 + m12 * other.m21 + m13 * other.m31,
            m10 * other.m02 + m11 * other.m12 + m12 * other.m22 + m13 * other.m32,
            m10 * other.m03 + m11 * other.m13 + m12 * other.m23 + m13 * other.m33,
            
            m20 * other.m00 + m21 * other.m10 + m22 * other.m20 + m23 * other.m30,
            m20 * other.m01 + m21 * other.m11 + m22 * other.m21 + m23 * other.m31,
            m20 * other.m02 + m21 * other.m12 + m22 * other.m22 + m23 * other.m32,
            m20 * other.m03 + m21 * other.m13 + m22 * other.m23 + m23 * other.m33,
            
            m30 * other.m00 + m31 * other.m10 + m32 * other.m20 + m33 * other.m30,
            m30 * other.m01 + m31 * other.m11 + m32 * other.m21 + m33 * other.m31,
            m30 * other.m02 + m31 * other.m12 + m32 * other.m22 + m33 * other.m32,
            m30 * other.m03 + m31 * other.m13 + m32 * other.m23 + m33 * other.m33
        )
    }

    /**
     * Transform a vector
     */
    fun transform(vector: Vector3): Vector3 {
        return Vector3(
            m00 * vector.x + m01 * vector.y + m02 * vector.z + m03,
            m10 * vector.x + m11 * vector.y + m12 * vector.z + m13,
            m20 * vector.x + m21 * vector.y + m22 * vector.z + m23
        )
    }

    /**
     * Convert to FloatArray for OpenGL (column-major order)
     */
    fun toFloatArray(): FloatArray {
        return floatArrayOf(
            m00, m10, m20, m30,
            m01, m11, m21, m31,
            m02, m12, m22, m32,
            m03, m13, m23, m33
        )
    }
}