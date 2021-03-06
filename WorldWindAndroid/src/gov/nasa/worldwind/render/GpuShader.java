/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import android.opengl.GLES20;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.WorldWindowImpl;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;

/**
 * @author dcollins
 * @version $Id: GpuShader.java 733 2012-09-02 17:15:09Z dcollins $
 */
public class GpuShader implements Cacheable, Disposable
{
	protected int type;
	protected int shaderId;
	protected long estimatedMemorySize;

	public GpuShader(int type, String source)
	{
		if (type != GLES20.GL_VERTEX_SHADER && type != GLES20.GL_FRAGMENT_SHADER)
		{
			String msg = Logging.getMessage("generic.TypeIsInvalid", type);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (WWUtil.isEmpty(source))
		{
			String msg = Logging.getMessage("nullValue.SourceIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		int shader = GLES20.glCreateShader(type);
		WorldWindowImpl.glCheckError("glCreateShader");
		if (shader <= 0)
		{
			String msg = Logging.getMessage("GL.UnableToCreateObject", this.nameFromShaderType(type));
			Logging.error(msg);
			throw new WWRuntimeException(msg);
		}

		if (!this.compile(shader, source))
		{
			// Get the info log before deleting the shader object.
			String infoLog = GLES20.glGetShaderInfoLog(shader);
			WorldWindowImpl.glCheckError("glGetShaderInfoLog");
			GLES20.glDeleteShader(shader);
			WorldWindowImpl.glCheckError("glDeleteShader");

			String msg = Logging.getMessage("GL.UnableToCompileShader", this.nameFromShaderType(type), infoLog);
			Logging.error(msg);
			throw new WWRuntimeException(msg);
		}

		this.type = type;
		this.shaderId = shader;
		this.estimatedMemorySize = this.estimateMemorySize(source);
	}

	public GpuShader(int type, int shaderId, long estimatedMemorySize)
	{
		if (type != GLES20.GL_VERTEX_SHADER && type != GLES20.GL_FRAGMENT_SHADER)
		{
			String msg = Logging.getMessage("generic.TypeIsInvalid", type);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (shaderId <= 0)
		{
			String msg = Logging.getMessage("GL.GLObjectIsInvalid", shaderId);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (estimatedMemorySize <= 0)
		{
			String msg = Logging.getMessage("generic.SizeIsInvalid", estimatedMemorySize);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.type = type;
		this.shaderId = shaderId;
		this.estimatedMemorySize = estimatedMemorySize;
	}

	public int getType()
	{
		return this.type;
	}

	public int getShaderId()
	{
		return this.shaderId;
	}

	public long getSizeInBytes()
	{
		return this.estimatedMemorySize;
	}

	public void dispose()
	{
		if (this.shaderId != 0)
		{
			GLES20.glDeleteShader(this.shaderId);
			WorldWindowImpl.glCheckError("glDeleteShader");
			this.shaderId = 0;
		}
	}

	protected boolean compile(int shader, String source)
	{
		int[] compileStatus = new int[1];

		GLES20.glShaderSource(shader, source);
		WorldWindowImpl.glCheckError("glShaderSource");
		GLES20.glCompileShader(shader);
		WorldWindowImpl.glCheckError("glCompileShader");
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
		WorldWindowImpl.glCheckError("glGetShaderiv");

		return compileStatus[0] == GLES20.GL_TRUE;
	}

	protected long estimateMemorySize(String source)
	{
		return 2 * source.length();
	}

	protected String nameFromShaderType(int type)
	{
		if (type == GLES20.GL_VERTEX_SHADER)
		{
			return Logging.getMessage("term.VertexShader");
		}
		else if (type == GLES20.GL_FRAGMENT_SHADER)
		{
			return Logging.getMessage("term.FragmentShader");
		}

		return null;
	}
}
