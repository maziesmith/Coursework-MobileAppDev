package com.example.zoeoeh.ETuner;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.renderscript.Float2;
import android.renderscript.Float3;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Zoe Wall on 03/03/2016.
 * Last modified 24/03/16.
 * Handles opengl calls and renders image
 *
 */
public class TestRenderer implements GLSurfaceView.Renderer {

    // debug log TAG
    private static final String TAG = "Test Renderer";

    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private float[] mLightModelMatrix = new float[16];

    // float buffers for model data. (memory allocated for vertex positions (3 floats per vertex) etc..
    private final FloatBuffer cubePositions;
    private final FloatBuffer cubeColoursHead;
    private final FloatBuffer cubeNormals;
    private final FloatBuffer cubeColoursStrings;
    private final FloatBuffer cubeColoursNeck;

    private static FloatBuffer pegNormals;
    private static FloatBuffer pegPositions;

    private static FloatBuffer stringNormals;
    private static FloatBuffer stringPositions;
    private static FloatBuffer cylinderColours;


    private int numberOfVerticesCube = 0;
    private int numberOfVerticesCylinder = 0;


    // Handle used for passing in full MVP transformation matrix to the shader program
    private int mMVPMatrixHandle;

    // Handle for Model View matrix used for transforming normals in the vertex shader
    private int mMVMatrixHandle;

    // Handles for passing in light + model data
    private int mLightPosHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mNormalHandle;

    // sizes for calculation.
    private final int mBytesPerFloat = 4;
    private final int mPositionDataSize = 3;
    private final int mColorDataSize = 4;
    private final int mNormalDataSize = 3;

    // float arrays for translation vectors
    private float[] maxTranslation = { 0.1f, 0.0f, 0.0f};

    // translation/adjustements in x for each string
    private float[] totalTranslationx = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private float[] adjustmentX = {0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f};

    // hold initial x translations for each string
    private final float[] xTranslationsStrings = {-1.5f,-0.9f,-0.3f, 0.3f, 0.9f, 1.5f};
    private static boolean[] chosenStringBool = {false, false, false, false, false, false};
    float[] scaleXStrings = {1.0f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f};
    float[] scaleYStrings = {1.0f, 1.45f, 2.0f, 2.0f, 1.45f, 1.0f};
    float[] yTranslationsStrings = {-3.0f, -1.85f ,-1.5f ,-1.5f ,-1.85f ,-3.0f};

    // hold normal data
    private ArrayList<Float> normals = new ArrayList<>();

    /**
     * Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     * we multiply this by our transformation matrices.
     */
    private final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    /**
     * Used to hold the current position of the light in world space (after transformation via model matrix).
     */
    private final float[] mLightPosInWorldSpace = new float[4];

    /**
     * Used to hold the transformed position of the light in eye space (after transformation via modelview matrix)
     */
    private final float[] mLightPosInEyeSpace = new float[4];

    // handle to regular phong shading program
    private int programHandle;

    // handle to vertex displacement shading program
    private int dispProgramHandle;

    // constructor used to initialise all model data/ vertex/colour/normals
    public TestRenderer() {

        // Define points for a cube.

        // X, Y, Z
        final float[] cubePositionData =
                {
                        // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
                        // if the points are counter-clockwise we are looking at the "front". If not we are looking at
                        // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
                        // usually represent the backside of an object and aren't visible anyways.

                        // Front face
                        -1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f,
                        1.0f, -1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f,

                        // Right face
                        1.0f, 1.0f, 1.0f,
                        1.0f, -1.0f, 1.0f,
                        1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, 1.0f,
                        1.0f, -1.0f, -1.0f,
                        1.0f, 1.0f, -1.0f,

                        // Back face
                        1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, -1.0f,
                        -1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, -1.0f,
                        -1.0f, -1.0f, -1.0f,
                        -1.0f, 1.0f, -1.0f,

                        // Left face
                        -1.0f, 1.0f, -1.0f,
                        -1.0f, -1.0f, -1.0f,
                        -1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, -1.0f,
                        -1.0f, -1.0f, 1.0f,
                        -1.0f, 1.0f, 1.0f,

                        // Top face
                        -1.0f, 1.0f, -1.0f,
                        -1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, -1.0f,
                        -1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, -1.0f,

                        // Bottom face
                        1.0f, -1.0f, -1.0f,
                        1.0f, -1.0f, 1.0f,
                        -1.0f, -1.0f, -1.0f,
                        1.0f, -1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f,
                        -1.0f, -1.0f, -1.0f,
                };

        float[] neckColourData = new float[36];

        for (int i = 0; i < neckColourData.length - 4; i += 4) {
            neckColourData[i] = 0.58f;
            neckColourData[i + 1] = 0.243f;
            neckColourData[i + 2] = 0.14f;
            neckColourData[i + 3] = 1.0f;

        }

        float[] stringColourData = new float[144];

        for (int i = 0; i < stringColourData.length - 4; i += 4) {
            stringColourData[i] = 0.9f;
            stringColourData[i + 1] = 0.9f;
            stringColourData[i + 2] = 0.9f;
            stringColourData[i + 3] = 1.0f;

        }

        float[] headColourData = new float[144];

        for (int i = 0; i < headColourData.length - 4; i += 4) {
            headColourData[i] = 0.35f;
            headColourData[i + 1] = 0.0f;
            headColourData[i + 2] = 0.0f;
            headColourData[i + 3] = 1.0f;

        }

        // X, Y, Z
        // The normal is used in light calculations and is a vector which points
        // orthogonal to the plane of the surface. For a cube model, the normals
        // should be orthogonal to the points of each face.
        final float[] cubeNormalData =
                {
                        // Front face
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,

                        // Right face
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,
                        1.0f, 0.0f, 0.0f,

                        // Back face
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,
                        0.0f, 0.0f, -1.0f,

                        // Left face
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,
                        -1.0f, 0.0f, 0.0f,

                        // Top face
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f,

                        // Bottom face
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f,
                        0.0f, -1.0f, 0.0f
                };

        // Initialize the buffers.
        cubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cubePositions.put(cubePositionData).position(0);

        cubeColoursNeck = ByteBuffer.allocateDirect(neckColourData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cubeColoursNeck.put(neckColourData).position(0);

        cubeColoursHead = ByteBuffer.allocateDirect(headColourData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cubeColoursHead.put(headColourData).position(0);

        cubeColoursStrings = ByteBuffer.allocateDirect(stringColourData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cubeColoursStrings.put(stringColourData).position(0);

        cubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        cubeNormals.put(cubeNormalData).position(0);

        // bool is peg? false
        createCylinderGeometry(false, new Float3(0.25f, 5.0f, 0.25f)); // create string
        createCylinderGeometry(true, new Float3(0.5f, 0.75f, 1.0f));  // create peg

        // how many vertices length / size of data
        numberOfVerticesCube = cubePositionData.length;
        numberOfVerticesCube /= mPositionDataSize;

    }



    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // clear colour is background grey from resources
        GLES20.glClearColor(0.368f, 0.365f, 0.361f, 1.0f);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -0.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -1.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // set view matrix
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = getVertexShader();
        final String fragmentShader = getFragmentShader();

        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        // link compiled shaders to create default phong shader program
        programHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Colour", "a_Normal"});

    }

    protected String getVertexShader() {
        final String vertexShader =
                "uniform mat4 u_MVPMatrix;      \n"        // A constant representing the combined model/view/projection matrix.
                        + "uniform mat4 u_MVMatrix;       \n"        // A constant representing the combined model/view matrix.

                        + "attribute vec4 a_Position;     \n"        // Per-vertex position information we will pass in.
                        + "attribute vec4 a_Colour;        \n"        // Per-vertex color information we will pass in.
                        + "attribute vec3 a_Normal;       \n"        // Per-vertex normal information we will pass in.

                        + "varying vec3 v_Position;       \n"        // This will be passed into the fragment shader.
                        + "varying vec4 v_Colour;          \n"        // This will be passed into the fragment shader.
                        + "varying vec3 v_Normal;         \n"        // This will be passed into the fragment shader.

                        // The entry point for our vertex shader.
                        + "void main()                                                \n"
                        + "{                                                          \n"
                        // Transform the vertex into eye space.
                        + "   v_Position = vec3(u_MVMatrix * a_Position);             \n"
                        // Pass through the color.
                        + "   v_Colour = a_Colour;                                      \n"
                        // Transform the normal's orientation into eye space.
                        + "   v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));      \n"
                        // gl_Position is a special variable used to store the final position.
                        // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
                        + "   gl_Position = u_MVPMatrix * a_Position;                 \n"
                        + "}                                                          \n";

        return vertexShader;
    }

    protected String getFragmentShader() {
        final String fragmentShader =
                "precision mediump float;       \n"        // Set the default precision to medium. We don't need as high of a
                        // precision in the fragment shader.
                        + "uniform vec3 u_LightPos;       \n"        // The position of the light in eye space.

                        + "varying vec3 v_Position;		\n"        // Interpolated position for this fragment.
                        + "varying vec4 v_Colour;          \n"        // This is the color from the vertex shader interpolated across the
                        // triangle per fragment.
                        + "varying vec3 v_Normal;         \n"        // Interpolated normal for this fragment.

                        // The entry point for our fragment shader.
                        + "void main()                    \n"
                        + "{                              \n"
                        // Will be used for attenuation.
                        //+ "   float distance = length(u_LightPos - v_Position);                  \n"
                        // Get a lighting direction vector from the light to the vertex.
                        + "   vec3 lightVector = normalize(u_LightPos - v_Position);             \n"
                        // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                        // pointing in the same direction then it will get max illumination.
                        + "   float diffuse = max(dot(v_Normal, lightVector), 0.1);              \n"
                        // Add attenuation.
                        // + "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  \n"
                        // Multiply the color by the diffuse illumination level to get final output color.
                        + "   gl_FragColor = v_Colour * diffuse;                                  \n"
                        + "}                                                                     \n";
        return fragmentShader;
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(programHandle);

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(programHandle, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Colour");
        mNormalHandle = GLES20.glGetAttribLocation(programHandle, "a_Normal");

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -3.0f);
        //  Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);


        // guitar head
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 1.5f, -7.0f);
        //Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.scaleM(mModelMatrix, 0, 2.5f, 3.25f, 1.0f);
        drawGeometry(cubePositions, cubeColoursHead, cubeNormals, numberOfVerticesCube);

        // guitar neck
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, -3.0f, -7.0f);
        Matrix.scaleM(mModelMatrix, 0, 1.75f, 2.5f, 1.0f);
        drawGeometry(cubePositions, cubeColoursNeck, cubeNormals, numberOfVerticesCube);

        //GLES20.glUseProgram(dispProgramHandle);

        // pass in string index, iterate through number of strings calling draw on each
        for (int i = 0; i < chosenStringBool.length; ++i)
        {
            drawString(i);
        }

        // draw posts, pass in translation information
        drawPosts(0.4f, 3.5f);   // d
        drawPosts(-0.4f, 3.5f);  // a
        drawPosts(-0.9f, 1.60f); // g
        drawPosts(0.9f, 1.60f);  // b
        drawPosts(-1.5f, -0.5f); // low E
        drawPosts(1.5f, -0.5f);  // high e

        drawPegs(0, angleInDegrees, -0.5f);
        drawPegs(1, angleInDegrees, 1.6f);
        drawPegs(2, angleInDegrees, 3.5f);
        drawPegs(3, angleInDegrees, 3.5f);
        drawPegs(4, angleInDegrees, 1.6f);
        drawPegs(5, angleInDegrees, -0.5f);
    }

    private void drawPosts(float transX, float transY)
    {
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, transX, transY, -8.0f);
        Matrix.scaleM(mModelMatrix, 0, 2.0f, 2.0f, 1.0f);

        // rotate by 90 degrees to face screen
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 1.0f, 0.0f, 0.0f);
        drawGeometry(stringPositions, cylinderColours, stringNormals, numberOfVerticesCylinder);
    }

    private void drawPegs(int stringIndex, float angleInDegrees, float transY)
    {
        float transX = 3.0f;
        float tiltAngle = 30.0f;

        if (stringIndex > 2)
        {
            transX = -transX;
            tiltAngle = -tiltAngle;
        }

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, -transX, transY, -6.0f);
        Matrix.scaleM(mModelMatrix, 0, 0.5f, 1.0f, 1.0f);
        Matrix.rotateM(mModelMatrix, 0, 90, 0.0f, 0.0f, 1.0f);
        Matrix.rotateM(mModelMatrix, 0, tiltAngle, 1.0f, 0.0f, 0.0f); // tilt
        if (chosenStringBool[stringIndex])
        {
            Matrix.rotateM(mModelMatrix, 0, angleInDegrees*10.0f, 0.0f, 1.0f, 0.0f);
        }
        drawGeometry(pegPositions, cylinderColours, pegNormals, numberOfVerticesCylinder);
    }

    // return length squared of vector
    private float length2(float[] myVector) {
        float result = 0.0f;

        for (float a : myVector) {
            result += (a * a);
        }

        return result;
    }

    private void drawString(int stringIndex)
    {
        float[] initialTranslation = { xTranslationsStrings[stringIndex], yTranslationsStrings[stringIndex], -6.0f };
        float[] translateBy = { 0.0f, 0.0f, 0.0f };
        float[] translationAdjustment = { adjustmentX[stringIndex], 0.0f, 0.0f};
        float[] totalTranslation = { totalTranslationx[stringIndex], 0.0f, 0.0f};

        if (!chosenStringBool[stringIndex])
        {
            // don't move
            translationAdjustment[0] = 0.0f;
            totalTranslation[0] = 0.0f;
        }
        // update translation

        for (int i = 0; i < totalTranslation.length; i++) {
            totalTranslation[i] += (translationAdjustment[i]* 0.05f);
            totalTranslationx[stringIndex] += (translationAdjustment[i]* 0.05f);
        }

        // if the length squared is bigger than the max trans len2 OR smaller than zero, Swap sign (changes direction of translation)
        // length squared is used as sqrt is expensive and not needed for this inequality
        if ((length2(totalTranslation) > length2(maxTranslation)) || length2(totalTranslation) < 0) {
            // swap signs for each element of array
            translationAdjustment[0] = -translationAdjustment[0];
            adjustmentX[stringIndex] = -adjustmentX[stringIndex];
        }

        for (int i = 0; i < translateBy.length; i++) {
            translateBy[i] = initialTranslation[i] + totalTranslation[i];
        }

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, translateBy[0], translateBy[1], translateBy[2]);
        Matrix.scaleM(mModelMatrix, 0, scaleXStrings[stringIndex], scaleYStrings[stringIndex], 1.0f);
        drawGeometry(stringPositions, cylinderColours, stringNormals, numberOfVerticesCylinder);

    }


    private void drawGeometry(FloatBuffer positions, FloatBuffer colours, FloatBuffer normals, int numberOfVertices) {
        // Pass in the position information
        positions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, positions);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Pass in the color information
        colours.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                0, colours);

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Pass in the normal information
        normals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                0, normals);

        GLES20.glEnableVertexAttribArray(mNormalHandle);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // Draw the cube.

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numberOfVertices);
    }

    /**
     * Helper function to compile a shader.
     *
     * @param shaderType   The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader.
     */
    private int compileShader(final int shaderType, final String shaderSource) {
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shaderHandle;
    }

    /**
     * Helper function to compile and link a program.
     *
     * @param vertexShaderHandle   An OpenGL handle to an already-compiled vertex shader.
     * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
     * @param attributes           Attributes that need to be bound to the program.
     * @return An OpenGL handle to the program.
     */
    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error  compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    public void createCylinderGeometry(Boolean isPeg, Float3 dims) {
        //geometry geometry_builder::create_cylinder(const unsigned int stacks,const unsigned
        //int slices,const glm::vec3 & dims)

        // Declare required buffers - positions, normals and colour
        ArrayList<Float> positions = new ArrayList<>();
        ArrayList<Float> colours = new ArrayList<>();

        // vars for calculations default dimensions
        int stacks = 20;
        int slices = 20;

        // Create top
        Float3 centre = new Float3(0.0f, 0.5f*dims.y, 0.0f);

        Float3 prev_vert = new Float3(0.5f*dims.x, 0.5f*dims.y, 0.0f);

        // current vertex
        Float3 curr_vert;

        // Angle per slice
        float delta_angle = (2.0f * (float)Math.PI)/(float)slices;

        // Iterate through each slice
        for (int i = 1; i <= slices; ++i)
        {
            // Calculate unit length vertex
            curr_vert = new Float3((float)Math.cos(i * delta_angle), 1.0f, -(float)Math.sin(i * delta_angle));

            // We want radius to be 1, so half THEN multiply by dimensions
            curr_vert.x = (curr_vert.x / 2.0f) * dims.x;
            curr_vert.y = (curr_vert.y / 2.0f) * dims.y;
            curr_vert.z = (curr_vert.z / 2.0f) * dims.z;

            // Push back vertices
            positions.addAll(Arrays.asList(centre.x, centre.y, centre.z));
            positions.addAll(Arrays.asList(prev_vert.x, prev_vert.y, prev_vert.z));
            positions.addAll(Arrays.asList(curr_vert.x, curr_vert.y, curr_vert.z));

            // Push back normals and colours
            for (int j = 0; j< 3; ++j)
            {
                normals.addAll(Arrays.asList(0.0f, 1.0f, 0.0f));
                colours.addAll(Arrays.asList(0.7f, 0.7f, 0.7f, 1.0f));
            }

            // Set previous as current
            prev_vert = curr_vert;
        }

        // Create bottom - same process as top
        centre = new Float3 (0.0f, -0.5f * dims.y, 0.0f);

        prev_vert = new Float3 (0.5f*dims.x, -0.5f*dims.y, 0.0f);

        // Iterate through each slice
        for (int i = 1; i <= slices; ++i)
        {
            // Calculate unit length vertex
            curr_vert = new Float3((float)Math.cos(i * delta_angle), -1.0f, (float)Math.sin(i * delta_angle));

            // We want radius to be 1, so half THEN multiply by dimensions
            curr_vert.x = (curr_vert.x / 2.0f) * dims.x;
            curr_vert.y = (curr_vert.y / 2.0f) * dims.y;
            curr_vert.z = (curr_vert.z / 2.0f) * dims.z;

            // Push back vertices
            positions.addAll(Arrays.asList(centre.x, centre.y, centre.z));
            positions.addAll(Arrays.asList(prev_vert.x, prev_vert.y, prev_vert.z));
            positions.addAll(Arrays.asList(curr_vert.x, curr_vert.y, curr_vert.z));

            // Push back normals and colours
            for (int j = 0; j< 3; ++j)
            {
                normals.addAll(Arrays.asList(0.0f, -1.0f, 0.0f));
                colours.addAll(Arrays.asList(0.7f, 0.7f, 0.7f, 1.0f));
            }

            // Set previous as current
            prev_vert = curr_vert;
        }

        // Create stacks
        ArrayList<Float3> verts = new ArrayList<>();
        ArrayList<Float2> coords = new ArrayList<>();

        Float3 tempFloat = new Float3(0.0f, 0.0f, 0.0f);
        verts.add(tempFloat);
        verts.add(tempFloat);
        verts.add(tempFloat);
        verts.add(tempFloat);


        // Delta height - scaled during vertex creation
        float delta_height = 2.0f / (float)stacks;

        // Calculate circumference - could be ellipitical
        float circ = (float)Math.PI * ((3.0f * (dims.x + dims.z)) - ((float)Math.sqrt((3.0f * dims.x + dims.z) * (dims.x + 3.0f * dims.z))));

        // Delta width is the circumference divided into slices
        float delta_width = circ / (float)slices;

        // Iterate through each stack
        for (int i = 0; i<stacks; ++i)
        {
            // Iterate through each slice
            for (int j = 0; j<slices; ++j)
            {
                // Calc vertices
                verts.set(0, new Float3((float)Math.cos(j * delta_angle), 1.0f - (delta_height * i), (float)Math.sin(j * delta_angle)));
                verts.set(1, new Float3((float)Math.cos((j + 1) * delta_angle), 1.0f - (delta_height * i),(float)Math.sin((j + 1) * delta_angle)));
                verts.set(2, new Float3((float)Math.cos(j * delta_angle), 1.0f - (delta_height * (i + 1)), (float)Math.sin(j * delta_angle)));
                verts.set(3, new Float3((float)Math.cos((j + 1) * delta_angle), 1.0f - (delta_height * (i + 1)), (float)Math.sin((j + 1) * delta_angle)));             ;

                // Scale by 0.5 * dims
                for (Float3 v :verts)
                {
                    v.x *= dims.x * 0.5f;
                    v.y *= dims.y * 0.5f;
                    v.z *= dims.z * 0.5f;
                }

                // Triangle 1
                positions.addAll(Arrays.asList(verts.get(0).x, verts.get(0).y, verts.get(0).z));

                normaliseAddToList(new Float3(verts.get(0).x, 0.0f, verts.get(0).z));


                positions.addAll(Arrays.asList(verts.get(3).x, verts.get(3).y, verts.get(3).z));
                normaliseAddToList(new Float3(verts.get(3).x, 0.0f, verts.get(3).z));
                positions.addAll(Arrays.asList(verts.get(2).x, verts.get(2).y, verts.get(2).z));
                normaliseAddToList(new Float3(verts.get(2).x, 0.0f, verts.get(2).z));

                // Triangle 2
                positions.addAll(Arrays.asList(verts.get(0).x, verts.get(0).y, verts.get(0).z));
                normaliseAddToList(new Float3(verts.get(0).x, 0.0f, verts.get(0).z));
                positions.addAll(Arrays.asList(verts.get(1).x, verts.get(1).y, verts.get(1).z));
                normaliseAddToList(new Float3(verts.get(1).x, 0.0f, verts.get(1).z));
                positions.addAll(Arrays.asList(verts.get(3).x, verts.get(3).y, verts.get(3).z));
                normaliseAddToList(new Float3(verts.get(3).x, 0.0f, verts.get(3).z));

                // Colours
                for (int k = 0; k< 6; ++k)
                colours.addAll(Arrays.asList(0.7f, 0.7f, 0.7f, 1.0f));
            }
        }



        // Add buffers
        float[] arrayNormals = new float[normals.size()];
        int i = 0;

        for (Float f : normals) {
            arrayNormals[i++] = (f != null ? f : Float.NaN);
        }

        if (isPeg)
        {
            pegNormals = addToBuffer(pegNormals, normals);
            pegPositions = addToBuffer(pegPositions, positions);
        }
        else
        {
            stringNormals = addToBuffer(stringNormals, normals);
            stringPositions = addToBuffer(stringPositions, positions);
        }

        cylinderColours = addToBuffer(cylinderColours, colours);

        // how many vertices length / size of data
        numberOfVerticesCylinder = positions.size();
        numberOfVerticesCylinder /= mPositionDataSize;

    }

    private FloatBuffer addToBuffer(FloatBuffer myBuffer, ArrayList<Float> myList)
    {
        float[] myListAsPrimArray = new float[myList.size()];
        int i = 0;

        for (Float f : myList)
        {
            myListAsPrimArray[i++] = f;//(f != null ? f : Float.NaN);
        }

        myBuffer = ByteBuffer.allocateDirect(myList.size() * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        myBuffer.put(myListAsPrimArray).position(0);

        return myBuffer;
    }

    public void normaliseAddToList(Float3 myVec)
    {
        // normalises input
        Float3 result = myVec;

        float[] vec = {myVec.x, myVec.y, myVec.z};

        // length squared.
        float len2 = length2(vec);

        // magnitude of vector is sqrt of each component squared
        float mag = Math.abs((float) Math.sqrt(len2));

        if (mag > 0)
        {
            result.x /= mag;
            result.y /= mag;
            result.z /= mag;
        }

        normals.add(result.x);
        normals.add(result.y);
        normals.add(result.z);
    }


    public static void setChosenString(int stringIndex)
    {
        // iterate through array set all to false
        for (int i = 0; i < chosenStringBool.length; ++i)
            chosenStringBool[i] = false;

        // set new chosen string
        if (stringIndex >= 0)
            chosenStringBool[stringIndex] = true;
    }

}

