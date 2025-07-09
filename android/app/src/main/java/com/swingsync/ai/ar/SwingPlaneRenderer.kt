package com.swingsync.ai.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/**
 * High-performance OpenGL ES 3D renderer for swing plane visualization
 * Optimized for 30fps+ rendering on mobile devices with stunning visual effects
 */
class SwingPlaneRenderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "SwingPlaneRenderer"
        
        // Shader sources - vertex shader
        private const val VERTEX_SHADER_CODE = """
            attribute vec4 vPosition;
            attribute vec4 vColor;
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            uniform mat4 uProjectionMatrix;
            uniform float uTime;
            uniform float uAlpha;
            
            varying vec4 vFragColor;
            varying vec3 vWorldPos;
            varying float vDepth;
            
            void main() {
                vec4 worldPos = uModelMatrix * vPosition;
                vec4 viewPos = uViewMatrix * worldPos;
                gl_Position = uProjectionMatrix * viewPos;
                
                vFragColor = vec4(vColor.rgb, vColor.a * uAlpha);
                vWorldPos = worldPos.xyz;
                vDepth = -viewPos.z;
            }
        """
        
        // Fragment shader with advanced effects
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            
            varying vec4 vFragColor;
            varying vec3 vWorldPos;
            varying float vDepth;
            
            uniform float uTime;
            uniform vec3 uCameraPos;
            uniform float uGridSpacing;
            uniform float uGlowIntensity;
            
            void main() {
                vec4 color = vFragColor;
                
                // Distance-based alpha falloff
                float distance = length(vWorldPos - uCameraPos);
                float alphaFalloff = 1.0 - smoothstep(2.0, 8.0, distance);
                
                // Grid effect
                vec2 gridPos = vWorldPos.xz / uGridSpacing;
                vec2 grid = abs(fract(gridPos - 0.5) - 0.5) / fwidth(gridPos);
                float gridLine = 1.0 - min(grid.x, grid.y);
                
                // Glow effect
                float glow = uGlowIntensity * (1.0 + sin(uTime * 2.0) * 0.3);
                
                // Combine effects
                color.a *= alphaFalloff;
                color.rgb += vec3(gridLine * 0.3);
                color.rgb += vec3(glow * 0.2);
                
                gl_FragColor = color;
            }
        """
        
        // Path shader for club path visualization
        private const val PATH_VERTEX_SHADER = """
            attribute vec4 vPosition;
            attribute vec4 vColor;
            attribute float vVelocity;
            
            uniform mat4 uMVPMatrix;
            uniform float uTime;
            uniform float uMaxVelocity;
            
            varying vec4 vFragColor;
            varying float vVelocityNorm;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                
                // Normalize velocity for color coding
                vVelocityNorm = clamp(vVelocity / uMaxVelocity, 0.0, 1.0);
                
                // Dynamic color based on velocity
                vec3 slowColor = vec3(0.0, 1.0, 0.0);    // Green for slow
                vec3 fastColor = vec3(1.0, 0.0, 0.0);    // Red for fast
                vec3 velocityColor = mix(slowColor, fastColor, vVelocityNorm);
                
                vFragColor = vec4(velocityColor, vColor.a);
                
                // Pulsing effect
                float pulse = 1.0 + sin(uTime * 4.0) * 0.2;
                gl_PointSize = 8.0 * pulse;
            }
        """
        
        private const val PATH_FRAGMENT_SHADER = """
            precision mediump float;
            
            varying vec4 vFragColor;
            varying float vVelocityNorm;
            
            uniform float uTime;
            
            void main() {
                // Circular point shape
                vec2 center = gl_PointCoord - vec2(0.5);
                float dist = length(center);
                if (dist > 0.5) discard;
                
                // Smooth edge
                float alpha = 1.0 - smoothstep(0.3, 0.5, dist);
                
                // Velocity-based glow
                float glow = vVelocityNorm * (1.0 + sin(uTime * 6.0) * 0.3);
                
                vec4 color = vFragColor;
                color.a *= alpha;
                color.rgb += vec3(glow * 0.4);
                
                gl_FragColor = color;
            }
        """
    }
    
    // Rendering state
    private var viewMatrix = FloatArray(16)
    private var projectionMatrix = FloatArray(16)
    private var mvpMatrix = FloatArray(16)
    private var modelMatrix = FloatArray(16)
    
    // Shader programs
    private var planeShaderProgram = 0
    private var pathShaderProgram = 0
    
    // Buffers
    private var planeVertexBuffer: FloatBuffer? = null
    private var planeColorBuffer: FloatBuffer? = null
    private var planeIndexBuffer: ShortBuffer? = null
    private var pathVertexBuffer: FloatBuffer? = null
    private var pathColorBuffer: FloatBuffer? = null
    private var pathVelocityBuffer: FloatBuffer? = null
    
    // Animation
    private var startTime = System.currentTimeMillis()
    private var currentTime = 0f
    
    // Camera
    private var cameraPosition = Vector3(0f, 2f, 5f)
    private var cameraTarget = Vector3(0f, 0f, 0f)
    private var cameraUp = Vector3(0f, 1f, 0f)
    
    // Scene data
    private var idealSwingPlane: SwingPlaneCalculator.SwingPlane? = null
    private var actualSwingPlane: SwingPlaneCalculator.SwingPlane? = null
    private var clubPath: ClubPathTracker.ClubPath? = null
    
    // Visual parameters
    private var planeAlpha = 0.6f
    private var gridSpacing = 0.5f
    private var glowIntensity = 0.3f
    private var showIdealPlane = true
    private var showActualPlane = true
    private var showClubPath = true
    
    // Performance monitoring
    private var frameCount = 0
    private var lastFpsTime = 0L
    private var currentFps = 0f
    
    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // Set clear color to transparent
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        
        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        
        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        // Initialize shaders
        initShaders()
        
        // Initialize camera
        initCamera()
        
        Log.d(TAG, "OpenGL ES surface created")
    }
    
    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        val ratio = width.toFloat() / height.toFloat()
        
        // Create perspective projection matrix
        val projectionMat = Matrix4.perspective(
            45f * PI.toFloat() / 180f,  // 45 degree field of view
            ratio,
            0.1f,  // Near plane
            100f   // Far plane
        )
        
        projectionMatrix = projectionMat.toFloatArray()
        
        Log.d(TAG, "Surface changed: ${width}x${height}")
    }
    
    override fun onDrawFrame(unused: GL10?) {
        // Update animation time
        currentTime = (System.currentTimeMillis() - startTime) / 1000f
        
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        // Update camera matrix
        updateCamera()
        
        // Calculate MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        
        // Render swing planes
        if (showIdealPlane) {
            idealSwingPlane?.let { renderSwingPlane(it, 0.6f, floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)) }
        }
        
        if (showActualPlane) {
            actualSwingPlane?.let { renderSwingPlane(it, 0.4f, floatArrayOf(1.0f, 0.5f, 0.0f, 1.0f)) }
        }
        
        // Render club path
        if (showClubPath) {
            clubPath?.let { renderClubPath(it) }
        }
        
        // Update FPS counter
        updateFPS()
        
        // Check for OpenGL errors
        checkGLError("onDrawFrame")
    }
    
    /**
     * Update swing plane data
     */
    fun updateSwingPlanes(
        ideal: SwingPlaneCalculator.SwingPlane?,
        actual: SwingPlaneCalculator.SwingPlane?
    ) {
        idealSwingPlane = ideal
        actualSwingPlane = actual
        
        ideal?.let { generatePlaneBuffers(it) }
        actual?.let { generatePlaneBuffers(it) }
    }
    
    /**
     * Update club path data
     */
    fun updateClubPath(path: ClubPathTracker.ClubPath?) {
        clubPath = path
        path?.let { generatePathBuffers(it) }
    }
    
    /**
     * Set camera position and target
     */
    fun setCamera(position: Vector3, target: Vector3, up: Vector3 = Vector3.UP) {
        cameraPosition = position
        cameraTarget = target
        cameraUp = up
    }
    
    /**
     * Set visual parameters
     */
    fun setVisualParameters(
        planeAlpha: Float = this.planeAlpha,
        gridSpacing: Float = this.gridSpacing,
        glowIntensity: Float = this.glowIntensity
    ) {
        this.planeAlpha = planeAlpha
        this.gridSpacing = gridSpacing
        this.glowIntensity = glowIntensity
    }
    
    /**
     * Toggle plane visibility
     */
    fun setPlaneVisibility(showIdeal: Boolean, showActual: Boolean, showPath: Boolean) {
        showIdealPlane = showIdeal
        showActualPlane = showActual
        showClubPath = showPath
    }
    
    /**
     * Get current FPS
     */
    fun getCurrentFPS(): Float = currentFps
    
    // Private methods
    
    private fun initShaders() {
        // Create plane shader program
        planeShaderProgram = createShaderProgram(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE)
        
        // Create path shader program
        pathShaderProgram = createShaderProgram(PATH_VERTEX_SHADER, PATH_FRAGMENT_SHADER)
        
        if (planeShaderProgram == 0 || pathShaderProgram == 0) {
            throw RuntimeException("Failed to create shader programs")
        }
    }
    
    private fun initCamera() {
        updateCamera()
    }
    
    private fun updateCamera() {
        // Create view matrix
        val viewMat = Matrix4.IDENTITY // Simplified - would normally use lookAt
        viewMatrix = viewMat.toFloatArray()
        
        // For now, use a fixed camera position
        Matrix.setLookAtM(
            viewMatrix, 0,
            cameraPosition.x, cameraPosition.y, cameraPosition.z,
            cameraTarget.x, cameraTarget.y, cameraTarget.z,
            cameraUp.x, cameraUp.y, cameraUp.z
        )
    }
    
    private fun renderSwingPlane(plane: SwingPlaneCalculator.SwingPlane, alpha: Float, color: FloatArray) {
        if (planeVertexBuffer == null) return
        
        GLES20.glUseProgram(planeShaderProgram)
        
        // Get shader locations
        val positionHandle = GLES20.glGetAttribLocation(planeShaderProgram, "vPosition")
        val colorHandle = GLES20.glGetAttribLocation(planeShaderProgram, "vColor")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(planeShaderProgram, "uMVPMatrix")
        val timeHandle = GLES20.glGetUniformLocation(planeShaderProgram, "uTime")
        val alphaHandle = GLES20.glGetUniformLocation(planeShaderProgram, "uAlpha")
        val cameraPosHandle = GLES20.glGetUniformLocation(planeShaderProgram, "uCameraPos")
        val gridSpacingHandle = GLES20.glGetUniformLocation(planeShaderProgram, "uGridSpacing")
        val glowIntensityHandle = GLES20.glGetUniformLocation(planeShaderProgram, "uGlowIntensity")
        
        // Set uniforms
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(timeHandle, currentTime)
        GLES20.glUniform1f(alphaHandle, alpha)
        GLES20.glUniform3f(cameraPosHandle, cameraPosition.x, cameraPosition.y, cameraPosition.z)
        GLES20.glUniform1f(gridSpacingHandle, gridSpacing)
        GLES20.glUniform1f(glowIntensityHandle, glowIntensity)
        
        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(colorHandle)
        
        // Set vertex data
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, planeVertexBuffer)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, planeColorBuffer)
        
        // Draw the plane
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, planeIndexBuffer)
        
        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
        
        // Draw grid lines
        renderPlaneGrid(plane, alpha, color)
    }
    
    private fun renderPlaneGrid(plane: SwingPlaneCalculator.SwingPlane, alpha: Float, color: FloatArray) {
        val gridLines = plane.getGridLines()
        
        // Create line vertices
        val lineVertices = mutableListOf<Float>()
        val lineColors = mutableListOf<Float>()
        
        gridLines.forEach { (start, end) ->
            // Start point
            lineVertices.addAll(listOf(start.x, start.y, start.z))
            lineColors.addAll(listOf(color[0], color[1], color[2], alpha * 0.5f))
            
            // End point
            lineVertices.addAll(listOf(end.x, end.y, end.z))
            lineColors.addAll(listOf(color[0], color[1], color[2], alpha * 0.5f))
        }
        
        if (lineVertices.isEmpty()) return
        
        // Create buffers
        val lineVertexBuffer = ByteBuffer.allocateDirect(lineVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(lineVertices.toFloatArray())
        lineVertexBuffer.position(0)
        
        val lineColorBuffer = ByteBuffer.allocateDirect(lineColors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(lineColors.toFloatArray())
        lineColorBuffer.position(0)
        
        // Get shader locations
        val positionHandle = GLES20.glGetAttribLocation(planeShaderProgram, "vPosition")
        val colorHandle = GLES20.glGetAttribLocation(planeShaderProgram, "vColor")
        
        // Set vertex data
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, lineVertexBuffer)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, lineColorBuffer)
        
        // Draw lines
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, lineVertices.size / 3)
    }
    
    private fun renderClubPath(path: ClubPathTracker.ClubPath) {
        if (pathVertexBuffer == null) return
        
        GLES20.glUseProgram(pathShaderProgram)
        
        // Get shader locations
        val positionHandle = GLES20.glGetAttribLocation(pathShaderProgram, "vPosition")
        val colorHandle = GLES20.glGetAttribLocation(pathShaderProgram, "vColor")
        val velocityHandle = GLES20.glGetAttribLocation(pathShaderProgram, "vVelocity")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(pathShaderProgram, "uMVPMatrix")
        val timeHandle = GLES20.glGetUniformLocation(pathShaderProgram, "uTime")
        val maxVelocityHandle = GLES20.glGetUniformLocation(pathShaderProgram, "uMaxVelocity")
        
        // Set uniforms
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(timeHandle, currentTime)
        GLES20.glUniform1f(maxVelocityHandle, path.maxVelocity)
        
        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glEnableVertexAttribArray(velocityHandle)
        
        // Set vertex data
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, pathVertexBuffer)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, pathColorBuffer)
        GLES20.glVertexAttribPointer(velocityHandle, 1, GLES20.GL_FLOAT, false, 0, pathVelocityBuffer)
        
        // Draw path points
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, path.points.size)
        
        // Draw path lines
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, path.points.size)
        
        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
        GLES20.glDisableVertexAttribArray(velocityHandle)
    }
    
    private fun generatePlaneBuffers(plane: SwingPlaneCalculator.SwingPlane) {
        val corners = plane.getCornerPoints()
        
        // Plane vertices (4 corners)
        val vertices = floatArrayOf(
            corners[0].x, corners[0].y, corners[0].z,
            corners[1].x, corners[1].y, corners[1].z,
            corners[2].x, corners[2].y, corners[2].z,
            corners[3].x, corners[3].y, corners[3].z
        )
        
        // Colors (all same for now)
        val colors = floatArrayOf(
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f
        )
        
        // Indices for triangles
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
        
        // Create buffers
        planeVertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        planeVertexBuffer?.position(0)
        
        planeColorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(colors)
        planeColorBuffer?.position(0)
        
        planeIndexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
        planeIndexBuffer?.position(0)
    }
    
    private fun generatePathBuffers(path: ClubPathTracker.ClubPath) {
        val points = path.points
        if (points.isEmpty()) return
        
        val vertices = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val velocities = mutableListOf<Float>()
        
        points.forEach { point ->
            vertices.addAll(listOf(point.position.x, point.position.y, point.position.z))
            colors.addAll(listOf(1.0f, 1.0f, 1.0f, 1.0f))
            velocities.add(point.velocity.magnitude)
        }
        
        // Create buffers
        pathVertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices.toFloatArray())
        pathVertexBuffer?.position(0)
        
        pathColorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(colors.toFloatArray())
        pathColorBuffer?.position(0)
        
        pathVelocityBuffer = ByteBuffer.allocateDirect(velocities.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(velocities.toFloatArray())
        pathVelocityBuffer?.position(0)
    }
    
    private fun updateFPS() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastFpsTime >= 1000) {
            currentFps = frameCount * 1000f / (currentTime - lastFpsTime)
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
    
    private fun createShaderProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            return 0
        }
        
        return program
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not compile shader: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        
        return shader
    }
    
    private fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$operation: glError $error")
        }
    }
}