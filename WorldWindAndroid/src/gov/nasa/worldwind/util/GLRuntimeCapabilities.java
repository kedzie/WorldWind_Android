/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;


import java.util.Arrays;
import java.util.List;

import static android.opengl.GLES20.*;

/**
 * GLRuntimeCapabilities describes the GL capabilities supported by the current GL runtime. It provides the caller with
 * the current GL version, with information about which GL features are available, and with properties defining the
 * capabilities of those features.
 * <p/>
 * For each GL feature, there are three key pieces of information available through GLRuntimeCapabilities: <ul> <li>The
 * property <code>is[Feature]Available</code> defines whether or not the feature is supported by the current GL runtime.
 * This is an attribute of the GL runtime, and is typically configured automatically by a call to {@link
 * #initialize()}.</li> <li>The property <code>is[Feature]Enabled</code> defines whether or
 * not this feature should be used, and must be configured by the caller. </li> <li>The convenience method
 * <code>isUse[Feature]()</code>. This returns whether or not the feature is available and is enabled for use (it is
 * simply a conjunction of the "available" and "enabled" properties).</li> </ul>
 * <p/>
 * GLRuntimeCapabilities is designed to automatically configure itself with information about the current GL runtime. To
 * invoke this behavior, call {@link #initialize} with a valid GLContext at the beginning
 * of each rendering pass.
 *
 * @author dcollins
 * @version $Id: GLRuntimeCapabilities.java 1933 2014-04-14 22:54:19Z dcollins $
 */
public class GLRuntimeCapabilities
{
	int[] mParam = new int[1];

	protected String glVendor;
	protected String glRenderer;
	protected double glVersion;
	protected int mGLES_Major_Version;
	protected int mGLES_Minor_Version;
	protected double glslVersion;
	protected int mGLSL_Major_Version;
	protected int mGLSL_Minor_Version;

	protected boolean isAnisotropicTextureFilterAvailable;
	protected boolean isAnisotropicTextureFilterEnabled=true;
	protected boolean isFramebufferObjectAvailable;
	protected boolean isFramebufferObjectEnabled=true;
	protected boolean isVertexBufferObjectAvailable;
	protected boolean isVertexBufferObjectEnabled=true;

	protected double mMaxTextureAnisotropy=-1d;
	protected int mDepthBits;
	private int mMaxTextureSize;
	private int mMaxCombinedTextureImageUnits;
	private int mMaxCubeMapTextureSize;
	private int mMaxFragmentUniformVectors;
	private int mMaxRenderbufferSize;
	private int mMaxTextureImageUnits;
	private int mMaxVaryingVectors;
	private int mMaxVertexAttribs;
	private int mMaxVertexTextureImageUnits;
	private int mMaxVertexUniformVectors;
	private int mMaxViewportWidth;
	private int mMaxViewportHeight;
	private int mMinAliasedLineWidth;
	private int mMaxAliasedLineWidth;
	private int mMinAliasedPointSize;
	private int mMaxAliasedPointSize;

	protected List<String> mExtensions;

	private static GLRuntimeCapabilities instance;

	public static GLRuntimeCapabilities getInstance() {
		if (instance == null) {
			synchronized (GLRuntimeCapabilities.class) {
				if(instance==null) {
					instance = new GLRuntimeCapabilities();
				}
			}
		}
		return instance;
	}

	/**
	 * Constructs a new GLAtttributes, enabling framebuffer objects, anisotropic texture filtering, and vertex buffer
	 * objects. Note that these properties are marked as enabled, but they are not known to be available yet. All other
	 * properties are set to default values which may be set explicitly by the caller.
	 * <p/>
	 * Note: The default vertex-buffer usage flag can be set via {@link gov.nasa.worldwind.Configuration} using the key
	 * "gov.nasa.worldwind.avkey.VBOUsage". If that key is not specified in the configuration then vertex-buffer usage
	 * defaults to <code>true</code>.
	 */
	private GLRuntimeCapabilities() {
		mParam = new int[1];

		String[] versionString = (glGetString(GL_VERSION)).split(" ");
		if (versionString.length >= 3) {
			String[] versionParts = versionString[2].split("\\.");
			if (versionParts.length >= 2) {
				mGLES_Major_Version = Integer.parseInt(versionParts[0]);
				if (versionParts[1].endsWith(":") || versionParts[1].endsWith("-")) {
					versionParts[1] = versionParts[1].substring(0, versionParts[1].length() - 1);
				}
				mGLES_Minor_Version = Integer.parseInt(versionParts[1]);
				glVersion = Double.parseDouble(mGLES_Major_Version + "." + mGLES_Minor_Version);
			}
		}
		versionString = (glGetString(GL_SHADING_LANGUAGE_VERSION)).split(" ");
		if (versionString.length >= 3) {
			String[] versionParts = versionString[2].split("\\.");
			if (versionParts.length >= 2) {
				mGLSL_Major_Version = Integer.parseInt(versionParts[0]);
				if (versionParts[1].endsWith(":") || versionParts[1].endsWith("-")) {
					versionParts[1] = versionParts[1].substring(0, versionParts[1].length() - 1);
				}
				mGLSL_Minor_Version = Integer.parseInt(versionParts[1]);
				glslVersion = Double.parseDouble(mGLSL_Major_Version + "." + mGLSL_Minor_Version);
			}
		}

		// Determine whether or not the OpenGL implementation is provided by the VMware SVGA 3D driver. This flag is
		// used to work around bugs and unusual behavior in the VMware SVGA 3D driver. The VMware drivers tested on
		// 7 August 2013 report the following strings for GL_VENDOR and GL_RENDERER:
		// - GL_VENDOR: "VMware, Inc."
		// - GL_RENDERER: "Gallium 0.4 on SVGA3D; build: RELEASE;"
		glVendor = glGetString(GL_VENDOR);
		glRenderer = glGetString(GL_RENDERER);

		mExtensions = Arrays.asList(glGetString(GL_EXTENSIONS).split(" "));

		isAnisotropicTextureFilterAvailable = isExtensionAvailable("GL_EXT_texture_filter_anisotropic");
		isFramebufferObjectAvailable = true;//isExtensionAvailable(GL_EXT_framebuffer_object) || isExtensionAvailable("OES_fbo_render_mipmap");
		// Vertex Buffer Objects are supported in version 1.5 or greater only. we are using 2.0
		isVertexBufferObjectAvailable = true;

		// Documentation on the anisotropic texture filter is available at
		// http://www.opengl.org/registry/specs/EXT/texture_filter_anisotropic.txt
		if (isAnisotropicTextureFilterAvailable)
		{
			// The maxAnisotropy value can be any real value. A value less than 2.0 indicates that the graphics
			// context does not support texture anisotropy.
//                float[] params = new float[1];
//                glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, params, 0);
//                this.maxTextureAnisotropy = params[0];
		}
		mDepthBits = getInt(GL_DEPTH_BITS);
		mMaxCombinedTextureImageUnits = getInt(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
		mMaxCubeMapTextureSize = getInt(GL_MAX_CUBE_MAP_TEXTURE_SIZE);
		mMaxFragmentUniformVectors = getInt(GL_MAX_FRAGMENT_UNIFORM_VECTORS);
		mMaxRenderbufferSize = getInt(GL_MAX_RENDERBUFFER_SIZE);
		mMaxTextureImageUnits = getInt(GL_MAX_TEXTURE_IMAGE_UNITS);
		mMaxTextureSize = getInt(GL_MAX_TEXTURE_SIZE);
		mMaxVaryingVectors = getInt(GL_MAX_VARYING_VECTORS);
		mMaxVertexAttribs = getInt(GL_MAX_VERTEX_ATTRIBS);
		mMaxVertexTextureImageUnits = getInt(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);
		mMaxVertexUniformVectors = getInt(GL_MAX_VERTEX_UNIFORM_VECTORS);
		mMaxViewportWidth = getInt(GL_MAX_VIEWPORT_DIMS, 2, 0);
		mMaxViewportHeight = getInt(GL_MAX_VIEWPORT_DIMS, 2, 1);
		mMinAliasedLineWidth = getInt(GL_ALIASED_LINE_WIDTH_RANGE, 2, 0);
		mMaxAliasedLineWidth = getInt(GL_ALIASED_LINE_WIDTH_RANGE, 2, 1);
		mMinAliasedPointSize = getInt(GL_ALIASED_POINT_SIZE_RANGE, 2, 0);
		mMaxAliasedPointSize = getInt(GL_ALIASED_POINT_SIZE_RANGE, 2, 1);
	}

	private int getInt(int pname)
	{
		glGetIntegerv(pname, mParam, 0);
		return mParam[0];
	}

	private int getInt(int pname, int length, int index)
	{
		int[] params = new int[length];
		glGetIntegerv(pname, params, 0);
		return params[index];
	}

	public boolean isExtensionAvailable(CharSequence extension) {
		return this.mExtensions.contains(extension);
	}

	/**
	 * Returns the current GL runtime version as a real number. For example, if the GL version is 1.5, this returns the
	 * floating point number equivalent to 1.5.
	 *
	 * @return GL version as a number.
	 */
	public double getGLVersion()
	{
		return this.glVersion;
	}

	public double getGLSLVersion()
	{
		return this.glslVersion;
	}

	/**
	 * Returns true if anisotropic texture filtering is available in the current GL runtime, and is enabled. Otherwise
	 * this returns false. For details on GL anisotropic texture filtering, see <a href="http://www.opengl.org/registry/specs/EXT/texture_filter_anisotropic.txt">http://www.opengl.org/registry/specs/EXT/texture_filter_anisotropic.txt</a>.
	 *
	 * @return true if anisotropic texture filtering is available and enabled, and false otherwise.
	 */
	public boolean isUseAnisotropicTextureFilter()
	{
		return this.isAnisotropicTextureFilterAvailable && this.isAnisotropicTextureFilterEnabled;
	}

	/**
	 * Returns true if framebuffer objects are available in the current GL runtime, and are enabled. Otherwise this
	 * returns false. For details on GL framebuffer objects, see <a href="http://www.opengl.org/registry/specs/EXT/framebuffer_object.txt">http://www.opengl.org/registry/specs/EXT/framebuffer_object.txt</a>.
	 *
	 * @return true if framebuffer objects are available and enabled, and false otherwise.
	 */
	public boolean isUseFramebufferObject()
	{
		return this.isFramebufferObjectAvailable && this.isFramebufferObjectEnabled;
	}

	public void setAnisotropicTextureFilterEnabled(boolean enable)
	{
		this.isAnisotropicTextureFilterEnabled = enable;
	}

	/**
	 * Sets whether or not framebuffer objects should be used if they are available in the current GL runtime.
	 *
	 * @param enable true to enable framebuffer objects, false to disable them.
	 */
	public void setFramebufferObjectEnabled(boolean enable)
	{
		this.isFramebufferObjectEnabled = enable;
	}

	/**
	 * Returns the number of bitplanes in the current GL depth buffer. The number of bitplanes is directly proportional
	 * to the accuracy of the GL renderer's hidden surface removal. The returned value is typically 16, 24 or 32. For
	 * more information on OpenGL depth buffering, see <a href="http://www.opengl.org/archives/resources/faq/technical/depthbuffer.htm"
	 * target="_blank">http://www.opengl.org/archives/resources/faq/technical/depthbuffer.htm</a>.
	 *
	 * @return the number of bitplanes in the current GL depth buffer.
	 */
	public int getDepthBits()
	{
		return mDepthBits;
	}

	/**
	 * Returns a real number defining the maximum degree of texture anisotropy supported by the current GL runtime. This
	 * defines the maximum ratio of the anisotropic texture filter. So 2.0 would define a maximum ratio of 2:1. If the
	 * degree is less than 2, then the anisotropic texture filter is not supported by the current GL runtime.
	 *
	 * @return the maximum degree of texture anisotropy supported.
	 */
	public double getMaxTextureAnisotropy() {
		return mMaxTextureAnisotropy;
	}

	/**
	 * A rough estimate of the largest texture that OpenGL can handle.
	 * @return
	 */
	public int getMaxTextureSize()
	{
		return mMaxTextureSize;
	}

	/**
	 * The maximum supported texture image units that can be used to access texture maps from the vertex shader
	 * and the fragment processor combined. If both the vertex shader and the fragment processing stage access
	 * the same texture image unit, then that counts as using two texture image units against this limit.
	 * @return
	 */
	public int getMaxCombinedTextureUnits()
	{
		return mMaxCombinedTextureImageUnits;
	}

	/**
	 * The value gives a rough estimate of the largest cube-map texture that the GL can handle.
	 * The value must be at least 1024.
	 * @return
	 */
	public int getMaxCubeMapTextureSize()
	{
		return mMaxCubeMapTextureSize;
	}

	/**
	 * The maximum number of individual 4-vectors of floating-point, integer, or boolean values that can be held
	 * in uniform variable storage for a fragment shader.
	 * @return
	 */
	public int getMaxFragmentUniformVectors()
	{
		return mMaxFragmentUniformVectors;
	}

	/**
	 * Indicates the maximum supported size for renderbuffers.
	 * @return
	 */
	public int getMaxRenderbufferSize()
	{
		return mMaxRenderbufferSize;
	}

	/**
	 * The maximum supported texture image units that can be used to access texture maps from the fragment shader.
	 * @return
	 */
	public int getMaxTextureImageUnits()
	{
		return mMaxTextureImageUnits;
	}

	/**
	 * The maximum number of 4-vectors for varying variables.
	 * @return
	 */
	public int getMaxVaryingVectors()
	{
		return mMaxVaryingVectors;
	}

	/**
	 * The maximum number of 4-component generic vertex attributes accessible to a vertex shader.
	 * @return
	 */
	public int getMaxVertexAttribs()
	{
		return mMaxVertexAttribs;
	}

	/**
	 * The maximum supported texture image units that can be used to access texture maps from the vertex shader.
	 * @return
	 */
	public int getMaxVertexTextureImageUnits()
	{
		return mMaxVertexTextureImageUnits;
	}

	/**
	 * The maximum number of 4-vectors that may be held in uniform variable storage for the vertex shader.
	 * @return
	 */
	public int getMaxVertexUniformVectors()
	{
		return mMaxVertexUniformVectors;
	}

	/**
	 * The maximum supported viewport width
	 * @return
	 */
	public int getMaxViewportWidth()
	{
		return mMaxViewportWidth;
	}

	/**
	 * The maximum supported viewport height
	 * @return
	 */
	public int getMaxViewportHeight()
	{
		return mMaxViewportHeight;
	}

	/**
	 * Indicates the minimum width supported for aliased lines
	 * @return
	 */
	public int getMinAliasedLineWidth()
	{
		return mMinAliasedLineWidth;
	}

	/**
	 * Indicates the maximum width supported for aliased lines
	 * @return
	 */
	public int getMaxAliasedLineWidth()
	{
		return mMaxAliasedLineWidth;
	}

	/**
	 * Indicates the minimum size supported for aliased points
	 * @return
	 */
	public int getMinAliasedPointSize()
	{
		return mMinAliasedPointSize;
	}

	/**
	 * Indicates the maximum size supported for aliased points
	 * @return
	 */
	public int getMaxAliasedPointSize()
	{
		return mMaxAliasedPointSize;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("GL v%d.%d", mGLES_Major_Version, mGLES_Minor_Version);
		sb.append("GLSL v%d.%d", mGLSL_Major_Version, mGLSL_Minor_Version);
		sb.append("-=-=-=- OpenGL Capabilities -=-=-=-\n");
		sb.append("Max Cube Map Texture Size          : ").append(mMaxCubeMapTextureSize).append("\n");
		sb.append("Max Fragment Uniform Vectors       : ").append(mMaxFragmentUniformVectors).append("\n");
		sb.append("Max Renderbuffer Size              : ").append(mMaxRenderbufferSize).append("\n");
		sb.append("Max Texture Image Units            : ").append(mMaxTextureImageUnits).append("\n");
		sb.append("Max Texture Size                   : ").append(mMaxTextureSize).append("\n");
		sb.append("Max Varying Vectors                : ").append(mMaxVaryingVectors).append("\n");
		sb.append("Max Vertex Attribs                 : ").append(mMaxVertexAttribs).append("\n");
		sb.append("Max Vertex Texture Image Units     : ").append(mMaxVertexTextureImageUnits).append("\n");
		sb.append("Max Vertex Uniform Vectors         : ").append(mMaxVertexUniformVectors).append("\n");
		sb.append("Max Viewport Width                 : ").append(mMaxViewportWidth).append("\n");
		sb.append("Max Viewport Height                : ").append(mMaxViewportHeight).append("\n");
		sb.append("Min Aliased Line Width             : ").append(mMinAliasedLineWidth).append("\n");
		sb.append("Max Aliased Line Width             : ").append(mMaxAliasedLineWidth).append("\n");
		sb.append("Min Aliased Point Size             : ").append(mMinAliasedPointSize).append("\n");
		sb.append("Max Aliased Point Width            : ").append(mMaxAliasedPointSize).append("\n");
		sb.append("Depth Bits                         : ").append(mDepthBits).append("\n");
		sb.append("Is Anisotropic Texture Filter Avail: ").append(isAnisotropicTextureFilterAvailable).append("\n");
		sb.append("Max Texture Anisotropy             : ").append(mMaxTextureAnisotropy).append("\n");
		sb.append("-=-=-=- /OpenGL Capabilities -=-=-=-\n");
		sb.append("-=-=-=- OpenGL Extensions -=-=-=-\n");
		for(String extension : mExtensions)
			sb.append(extension).append("\n");
		sb.append("-=-=-=- /OpenGL Extensions -=-=-=-\n");
		return sb.toString();
	}
}
