/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import gov.nasa.worldwind.WorldWindowImpl;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLUtil;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.dds.DDSCompressor;
import gov.nasa.worldwind.util.dds.DDSTextureReader;
import gov.nasa.worldwind.util.dds.DXTCompressionAttributes;
import gov.nasa.worldwind.util.pkm.ETC1Compressor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * @author dcollins
 * @version $Id: GpuTextureData.java 764 2012-09-11 00:17:17Z tgaskins $
 */
public class GpuTextureData implements Cacheable
{
	private static final int MAX_MIP_LEVELS = 12;

    public static class BitmapData
    {
        public final Bitmap bitmap;

        public BitmapData(Bitmap bitmap)
        {
            if (bitmap == null)
            {
                String msg = Logging.getMessage("nullValue.BitmapIsNull");
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            this.bitmap = bitmap;
        }
    }

    public static class CompressedData
    {
        public final int format;
        public final MipmapData[] levelData;
		public final MipmapData[] alphaData;

		public CompressedData(int format, MipmapData[] levelData) {
			this(format, levelData, null);
		}

        public CompressedData(int format, MipmapData[] levelData, MipmapData[] alphaData)
        {
            if (levelData == null || levelData.length == 0)
            {
                String msg = Logging.getMessage("nullValue."); // TODO
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            this.format = format;
            this.levelData = levelData;
			this.alphaData = alphaData;
        }
    }

    public static class MipmapData
    {
        public final int width;
        public final int height;
        public final ByteBuffer buffer;

        public MipmapData(int width, int height, ByteBuffer buffer)
        {
            if (width < 0)
            {
                String msg = Logging.getMessage("generic.WidthIsInvalid", width);
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            if (height < 0)
            {
                String msg = Logging.getMessage("generic.HeightIsInvalid", height);
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            if (buffer == null)
            {
                String msg = Logging.getMessage("nullValue.BufferIsNull");
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            this.width = width;
            this.height = height;
            this.buffer = buffer;
        }
    }


	/**
	 * Create GpuTextureData
	 * @param source	Bitmap, InputStream, URL, or String path
	 * @param textureFormat	Desired texture format.  If input is uncompressed and format is compressed, compression will be performed
	 * @param useMipMaps	Whether to load/create mipmaps
	 * @return	The GpuTextureData
	 */
	public static GpuTextureData createTextureData(Object source, String textureFormat, boolean useMipMaps) {
		if(source instanceof String)
			return createTextureData(source, (String)source, textureFormat, useMipMaps);
		return createTextureData(source, null, textureFormat, useMipMaps);
	}

	/**
	 * Create GpuTextureData
	 * @param source	Bitmap, InputStream, URL, or String path
	 * @param url		Filename, if applicable.  Used for format detection and alpha/mip location
	 * @param textureFormat	Desired texture format.  If input is uncompressed and format is compressed, compression will be performed
	 * @param useMipMaps	Whether to load/create mipmaps
	 * @return	The GpuTextureData
	 */
    public static GpuTextureData createTextureData(Object source, String url, String textureFormat, boolean useMipMaps)
    {
        if (WWUtil.isEmpty(source))
        {
            String msg = Logging.getMessage("nullValue.SourceIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        GpuTextureData data = null;

        try
        {
            if (source instanceof Bitmap)
            {
                data = new GpuTextureData((Bitmap) source, estimateMemorySize((Bitmap) source));
            }
            else
            {
                // Attempt to open the source as an InputStream. This handle URLs, Files, InputStreams, a String
                // containing a valid URL, a String path to a file on the local file system, and a String path to a
                // class path resource.
                InputStream stream = WWIO.openBufferedStream(source);
                try
                {
                    if (stream != null)
                    {
                        // Wrap the stream in a BufferedInputStream to provide the mark/reset capability required to
                        // avoid destroying the stream when it is read more than once. BufferedInputStream also improves
                        // file read performance.
                        data = fromStream(stream, url, textureFormat, useMipMaps);
                    }
                }
                finally
                {
                    WWIO.closeStream(stream, source.toString()); // This method call is benign if the stream is null.
                }
            }
        }
        catch (Exception e)
        {
			Logging.error(Logging.getMessage("GpuTextureFactory.TextureDataCreationFailed", source), e);
        }

        return data;
    }

    protected static final int DEFAULT_MARK_LIMIT = 1024;

    protected static GpuTextureData fromStream(InputStream stream, String url, String textureFormat, boolean useMipMaps) throws IOException {
		GpuTextureData data = null;
		stream.mark(DEFAULT_MARK_LIMIT);

		if(textureFormat==null && url!=null)
//			textureFormat = "image/"+WWIO.getFileExtension(url);
			textureFormat = WWIO.makeMimeTypeForSuffix(WWIO.getSuffix(url));

		if ("image/dds".equalsIgnoreCase(textureFormat) && url!=null && !url.toString().toLowerCase().endsWith("dds"))
		{
			if(WorldWindowImpl.DEBUG)
				Logging.verbose("Compressing DDS texture " + url);
			// Configure a DDS compressor to generate mipmaps based according to the 'useMipMaps' parameter, and
			// convert the image URL to a compressed DDS format.
			DXTCompressionAttributes attributes = DDSCompressor.getDefaultCompressionAttributes();
			attributes.setBuildMipmaps(useMipMaps);
			DDSTextureReader ddsReader = new DDSTextureReader();
			return ddsReader.read(WWIO.getInputStreamFromByteBuffer(DDSCompressor.compressImageStream(stream, attributes)));
		}
		else if("image/dds".equalsIgnoreCase(textureFormat))
		{
			if(WorldWindowImpl.DEBUG)
				Logging.verbose("Loading DDS texture " + url);
			DDSTextureReader ddsReader = new DDSTextureReader();
			return ddsReader.read(stream);
		}
		else if ("image/pkm".equalsIgnoreCase(textureFormat) && url!=null && !url.toString().toLowerCase().endsWith("pkm"))
		{
			if(WorldWindowImpl.DEBUG)
				Logging.verbose("Compressing ETC1 texture " + url);
			ETC1Util.ETC1Texture[] etc1tex = ETC1Compressor.compressImage(BitmapFactory.decodeStream(stream));

			MipmapData mipmapData = new MipmapData(etc1tex[0].getWidth(), etc1tex[0].getHeight(), etc1tex[0].getData());
			MipmapData []alphaMipmap = etc1tex.length==1 ? null :
				new MipmapData[] {new MipmapData(etc1tex[1].getWidth(), etc1tex[1].getHeight(), etc1tex[1].getData())};

			return new GpuTextureData(ETC1.ETC1_RGB8_OES, new MipmapData[] {mipmapData}, alphaMipmap, etc1tex[0].getData().remaining());
		}
		else if ("image/pkm".equalsIgnoreCase(textureFormat))
		{
			if(WorldWindowImpl.DEBUG)
				Logging.verbose("Loading ETC1 texture " + url);

			List<MipmapData> colorData = new LinkedList<MipmapData>();
			List<MipmapData> alphaData = new LinkedList<MipmapData>();

			ETC1Util.ETC1Texture etc1tex = ETC1Util.createTexture(stream);
			MipmapData mipmapData = new MipmapData(etc1tex.getWidth(), etc1tex.getHeight(), etc1tex.getData());
			colorData.add(mipmapData);
			int dataSize = etc1tex.getData().limit();

			MipmapData alphaLevel0 = readETC1(url.substring(0, url.lastIndexOf(".pkm")) + "_alpha.pkm");
			if(alphaLevel0 != null) {
				alphaData.add(alphaLevel0);
				dataSize += alphaLevel0.buffer.limit();
			}

			if(useMipMaps) {
				for(int i=0; i<MAX_MIP_LEVELS; i++) {
					String mipUrl = url.replace("mip_0", "mip_"+i);
					MipmapData mipData = readETC1(mipUrl);
					if(mipData==null)
						break;
					colorData.add(mipData);
					dataSize += mipData.buffer.limit();

					mipData = readETC1(mipUrl.substring(0, mipUrl.lastIndexOf(".pkm")) + "_alpha.pkm");
					if(mipData!=null) {
						alphaData.add(mipData);
						dataSize += mipData.buffer.limit();
					}
				}
			}
			MipmapData[] colors = colorData.isEmpty() ? null : colorData.toArray(new MipmapData[colorData.size()]);
			MipmapData[] alphas = alphaData.isEmpty() ? null : alphaData.toArray(new MipmapData[alphaData.size()]);
			return new GpuTextureData(ETC1.ETC1_RGB8_OES, colors, alphas, dataSize);
		} else {
			if(WorldWindowImpl.DEBUG)
				Logging.verbose("Loading bitmap texture "+ url);
			Bitmap bitmap = BitmapFactory.decodeStream(stream);
			return bitmap != null ? new GpuTextureData(bitmap, estimateMemorySize(bitmap)) : null;
		}
	}

	public static MipmapData readETC1(String url) throws IOException {
		if(WorldWindowImpl.DEBUG)
			Logging.verbose("Loading ETC1 texture " + url);

		InputStream is = WWIO.getFileOrResourceAsBufferedStream(url, GpuTextureData.class);
		if(is==null)
			return null;
		try {
			ETC1Util.ETC1Texture tex = ETC1Util.createTexture(is);
			return new MipmapData(tex.getWidth(), tex.getHeight(), tex.getData());
		} finally {
			WWIO.closeStream(is, url);
		}
	}

    public GpuTextureData(Bitmap bitmap, long estimatedMemorySize)
    {
        if (bitmap == null)
        {
            String msg = Logging.getMessage("nullValue.BitmapIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (estimatedMemorySize <= 0)
        {
            String msg = Logging.getMessage("generic.SizeIsInvalid", estimatedMemorySize);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.bitmapData = new BitmapData(bitmap);
        this.estimatedMemorySize = estimatedMemorySize;
    }

	public GpuTextureData(int format, MipmapData[] levelData, long estimatedMemorySize)
	{
		this(format, levelData, null, estimatedMemorySize);
	}

    public GpuTextureData(int format, MipmapData[] levelData, MipmapData[] alphaData, long estimatedMemorySize)
    {
        if (levelData == null || levelData.length == 0)
        {
            String msg = Logging.getMessage("nullValue."); // TODO
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (estimatedMemorySize <= 0)
        {
            String msg = Logging.getMessage("generic.SizeIsInvalid", estimatedMemorySize);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.compressedData = new CompressedData(format, levelData, alphaData);
        this.estimatedMemorySize = estimatedMemorySize;
    }

    protected BitmapData bitmapData;
    protected CompressedData compressedData;
    protected long estimatedMemorySize;

    protected GpuTextureData()
    {
    }

    public BitmapData getBitmapData()
    {
        return this.bitmapData;
    }

    public CompressedData getCompressedData()
    {
        return this.compressedData;
    }

    public long getSizeInBytes()
    {
        return this.estimatedMemorySize;
    }

    protected static long estimateMemorySize(Bitmap bitmap)
    {
		return OGLUtil.estimateMemorySize(GLUtils.getInternalFormat(bitmap), GLUtils.getType(bitmap), bitmap.getWidth(), bitmap.getHeight(), true);
    }
}
