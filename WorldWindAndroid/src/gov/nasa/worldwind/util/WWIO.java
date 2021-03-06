/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import android.content.Context;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.render.GpuProgram;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: WWIO.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class WWIO {
	public static final String DELETE_ON_EXIT_PREFIX = "WWJDeleteOnExit";
	protected static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
	public static final String ILLEGAL_FILE_PATH_PART_CHARACTERS = "[" + "?/\\\\=+<>:;\\,\"\\|^\\[\\]" + "]";
	protected static final int DEFAULT_PAGE_SIZE = 2 << 15;
	protected static final Map<String, String> mimeTypeToSuffixMap = new HashMap<String, String>();
	protected static final Map<String, String> suffixToMimeTypeMap = new HashMap<String, String>();
    protected static WeakReference<Context> context;

	static {
		mimeTypeToSuffixMap.put("application/acad", "dwg");
		mimeTypeToSuffixMap.put("application/bil", "bil");
		mimeTypeToSuffixMap.put("application/bil16", "bil");
		mimeTypeToSuffixMap.put("application/bil32", "bil");
		mimeTypeToSuffixMap.put("application/dxf", "dxf");
		mimeTypeToSuffixMap.put("application/octet-stream", "bin");
		mimeTypeToSuffixMap.put("application/pdf", "pdf");
		mimeTypeToSuffixMap.put("application/rss+xml", "xml");
		mimeTypeToSuffixMap.put("application/rtf", "rtf");
		mimeTypeToSuffixMap.put("application/sla", "slt");
		mimeTypeToSuffixMap.put("application/vnd.google-earth.kmz", "kmz");
		mimeTypeToSuffixMap.put("application/vnd.google-earth.kml+xml", "kml");
		mimeTypeToSuffixMap.put("application/vnd.ogc.gml+xml", "gml");
		mimeTypeToSuffixMap.put("application/x-gzip", "gz");
		mimeTypeToSuffixMap.put("application/xml", "xml");
		mimeTypeToSuffixMap.put("application/zip", "zip");
		mimeTypeToSuffixMap.put("audio/x-aiff", "aif");
		mimeTypeToSuffixMap.put("audio/x-midi", "mid");
		mimeTypeToSuffixMap.put("audio/x-wav", "wav");
		mimeTypeToSuffixMap.put("image/bmp", "bmp");
		mimeTypeToSuffixMap.put("image/dds", "dds");
		mimeTypeToSuffixMap.put("image/geotiff", "gtif");
		mimeTypeToSuffixMap.put("image/gif", "gif");
		mimeTypeToSuffixMap.put("image/jp2", "jp2");
		mimeTypeToSuffixMap.put("image/jpeg", "jpg");
		mimeTypeToSuffixMap.put("image/jpg", "jpg");
		mimeTypeToSuffixMap.put("image/png", "png");
		mimeTypeToSuffixMap.put("image/pkm", "pkm");
		mimeTypeToSuffixMap.put("image/svg+xml", "svg");
		mimeTypeToSuffixMap.put("image/tiff", "tif");
		mimeTypeToSuffixMap.put("image/x-imagewebserver-ecw", "ecw");
		mimeTypeToSuffixMap.put("image/x-mrsid", "sid");
		mimeTypeToSuffixMap.put("image/x-rgb", "rgb");
		mimeTypeToSuffixMap.put("model/collada+xml", "dae");
		mimeTypeToSuffixMap.put("multipart/zip", "zip");
		mimeTypeToSuffixMap.put("multipart/x-gzip", "gzip");
		mimeTypeToSuffixMap.put("text/html", "html");
		mimeTypeToSuffixMap.put("text/plain", "txt");
		mimeTypeToSuffixMap.put("text/richtext", "rtx");
		mimeTypeToSuffixMap.put("text/tab-separated-values", "tsv");
		mimeTypeToSuffixMap.put("text/xml", "xml");
		mimeTypeToSuffixMap.put("video/mpeg", "mpg");
		mimeTypeToSuffixMap.put("video/quicktime", "mov");
		mimeTypeToSuffixMap.put("world/x-vrml", "wrl");

		suffixToMimeTypeMap.put("aif", "audio/x-aiff");
		suffixToMimeTypeMap.put("aifc", "audio/x-aiff");
		suffixToMimeTypeMap.put("aiff", "audio/x-aiff");
		suffixToMimeTypeMap.put("bil", "application/bil");
		suffixToMimeTypeMap.put("bil16", "application/bil16");
		suffixToMimeTypeMap.put("bil32", "application/bil32");
		suffixToMimeTypeMap.put("bin", "application/octet-stream");
		suffixToMimeTypeMap.put("bmp", "image/bmp");
		suffixToMimeTypeMap.put("dds", "image/dds");
		suffixToMimeTypeMap.put("dwg", "application/acad");
		suffixToMimeTypeMap.put("dxf", "application/dxf");
		suffixToMimeTypeMap.put("ecw", "image/x-imagewebserver-ecw");
		suffixToMimeTypeMap.put("gif", "image/gif");
		suffixToMimeTypeMap.put("gml", "application/vnd.ogc.gml+xml");
		suffixToMimeTypeMap.put("gtif", "image/geotiff");
		suffixToMimeTypeMap.put("gz", "application/x-gzip");
		suffixToMimeTypeMap.put("gzip", "multipart/x-gzip");
		suffixToMimeTypeMap.put("htm", "text/html");
		suffixToMimeTypeMap.put("html", "text/html");
		suffixToMimeTypeMap.put("jp2", "image/jp2");
		suffixToMimeTypeMap.put("jpeg", "image/jpeg");
		suffixToMimeTypeMap.put("jpg", "image/jpeg");
		suffixToMimeTypeMap.put("pkm", "image/pkm");
		suffixToMimeTypeMap.put("kml", "application/vnd.google-earth.kml+xml");
		suffixToMimeTypeMap.put("kmz", "application/vnd.google-earth.kmz");
		suffixToMimeTypeMap.put("mid", "audio/x-midi");
		suffixToMimeTypeMap.put("midi", "audio/x-midi");
		suffixToMimeTypeMap.put("mov", "video/quicktime");
		suffixToMimeTypeMap.put("mp3", "audio/x-mpeg");
		suffixToMimeTypeMap.put("mpe", "video/mpeg");
		suffixToMimeTypeMap.put("mpeg", "video/mpeg");
		suffixToMimeTypeMap.put("mpg", "video/mpeg");
		suffixToMimeTypeMap.put("pdf", "application/pdf");
		suffixToMimeTypeMap.put("png", "image/png");
		suffixToMimeTypeMap.put("rgb", "image/x-rgb");
		suffixToMimeTypeMap.put("rtf", "application/rtf");
		suffixToMimeTypeMap.put("rtx", "text/richtext");
		suffixToMimeTypeMap.put("sid", "image/x-mrsid");
		suffixToMimeTypeMap.put("slt", "application/sla");
		suffixToMimeTypeMap.put("svg", "image/svg+xml");
		suffixToMimeTypeMap.put("tif", "image/tiff");
		suffixToMimeTypeMap.put("tiff", "image/tiff");
		suffixToMimeTypeMap.put("tsv", "text/tab-separated-values");
		suffixToMimeTypeMap.put("txt", "text/plain");
		suffixToMimeTypeMap.put("wav", "audio/x-wav");
		suffixToMimeTypeMap.put("wbmp", "image/vnd.wap.wbmp");
		suffixToMimeTypeMap.put("wrl", "world/x-vrml");
		suffixToMimeTypeMap.put("xml", "application/xml");
		suffixToMimeTypeMap.put("zip", "application/zip");
	}

    public static void setContext(Context c) {
        context = new WeakReference<Context>(c);
    }

    public static Context getContext() {
        return context.get();
    }

	public static String formPath(String... pathParts) {
		StringBuilder sb = new StringBuilder();

		for (String pathPart : pathParts) {
			if (pathPart == null) continue;

			if (sb.length() > 0) sb.append(File.separator);
			sb.append(pathPart.replaceAll(ILLEGAL_FILE_PATH_PART_CHARACTERS, "_"));
		}

		return sb.toString();
	}

	/**
	 * Returns the file path's parent directory path, or null if the file path does not have a parent.
	 * 
	 * @param filePath
	 *            a file path String.
	 * @return the file path's parent directory, or null if the path does not have a parent.
	 * @throws IllegalArgumentException
	 *             if the file path is null.
	 */
	public static String getParentFilePath(String filePath) {
		if (filePath == null) {
			String message = Logging.getMessage("nullValue.FilePathIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		filePath = stripTrailingSeparator(filePath);

		int len = filePath.length();
		int p = filePath.lastIndexOf("/");
		if (p < 0) p = filePath.lastIndexOf("\\");
		return (p > 0 && p < len) ? filePath.substring(0, p) : null;
	}

	/**
	 * Creates an {@link InputStream} for the contents of a {@link ByteBuffer}. The method creates a copy of the
	 * buffer's contents and passes a steam reference to that copy.
	 * 
	 * @param buffer
	 *            the buffer to create a stream for.
	 * @return an {@link InputStream} for the buffer's contents.
	 * @throws IllegalArgumentException
	 *             if <code>buffer</code> is null.
	 */
	public static InputStream getInputStreamFromByteBuffer(ByteBuffer buffer) {
		if (buffer == null) {
			String message = Logging.getMessage("nullValue.ByteBufferIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (buffer.hasArray() && buffer.limit() == buffer.capacity()) // otherwise bytes beyond the limit are included
		return new ByteArrayInputStream(buffer.array());

		byte[] byteArray = new byte[buffer.limit()];
		buffer.get(byteArray);
		return new ByteArrayInputStream(byteArray);
	}

	public static BufferedInputStream getFileOrResourceAsBufferedStream(String path, Class<?> c) {
		InputStream stream = getFileOrResourceAsStream(path, c);
		if(stream!=null && !(stream instanceof BufferedInputStream))
			stream = new BufferedInputStream(stream);
		return (BufferedInputStream)stream;
	}

	public static InputStream getFileOrResourceAsStream(String path, Class<?> c) {
		if (path == null) {
			String msg = Logging.getMessage("nullValue.PathIsNull");
			Logging.error(msg);
			throw new IllegalStateException(msg);
		}

		try {
			if (new File(path).exists())
				return new FileInputStream(new File(path));

			return (c==null ? WWIO.class : c).getResourceAsStream("/" + path);
		} catch (Exception e) {
			throw new WWRuntimeException(Logging.getMessage("generic.UnableToOpenPath", path), e);
		}
	}

	public static String getSuffix(String filePath) {
		if (filePath == null) {
			String message = Logging.getMessage("nullValue.FilePathIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		int len = filePath.length();
		int p = filePath.lastIndexOf(".");
		String suffix = (p >= 0 && p + 1 < len) ? filePath.substring(p + 1, len) : null;

		// handle .bil.gz extensions
		if (null != suffix && p > 0 && "gz".equals(suffix)) {
			int idx = filePath.lastIndexOf(".", p - 1);
			suffix = (idx >= 0 && idx + 1 < len) ? filePath.substring(idx + 1, len) : suffix;
		}

		return suffix;
	}

	/**
	 * Indicates whether a {@link File} contains content of a specified mime type.
	 * <p/>
	 * Only the filename suffix is consulted to determine the file's content type.
	 * 
	 * @param file
	 *            the file to test.
	 * @param mimeTypes
	 *            the mime types to test for.
	 * @return true if the file contains a specified content type, false if the file does not contain a specified
	 *         content type, the specified file is null, or no content types are specified.
	 */
	public static boolean isContentType(File file, String... mimeTypes) {
		if (file == null || mimeTypes == null) return false;

		for (String mimeType : mimeTypes) {
			if (mimeType == null) continue;

			String typeSuffix = WWIO.makeSuffixForMimeType(mimeType);
			String fileSuffix = WWIO.getSuffix(file.getName());

			if (fileSuffix == null || typeSuffix == null) continue;

			if (!fileSuffix.startsWith(".")) fileSuffix = "." + fileSuffix;

			if (fileSuffix.equalsIgnoreCase(typeSuffix)) return true;
		}

		return false;
	}

	/**
	 * Returns the mime type string corresponding to the specified file suffix string.
	 * 
	 * @param suffix
	 *            the suffix who's mime type is returned.
	 * @return the mime type for the specified file suffix.
	 * @throws IllegalArgumentException
	 *             if the file suffix is null.
	 */
	public static String makeMimeTypeForSuffix(String suffix) {
		if (WWUtil.isEmpty(suffix)) {
			String msg = Logging.getMessage("nullValue.SuffixIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Strip the starting period from the suffix string, if any exists.
		if (suffix.startsWith(".")) suffix = suffix.substring(1, suffix.length());

		return suffixToMimeTypeMap.get(suffix);
	}

	/**
	 * Returns the file suffix string corresponding to the specified mime type string. The returned suffix starts with
	 * the period character '.' followed by the mime type's subtype, as in: ".[subtype]".
	 * 
	 * @param mimeType
	 *            the mime type who's suffix is returned.
	 * @return the file suffix for the specified mime type, with a leading ".".
	 * @throws IllegalArgumentException
	 *             if the mime type is null or malformed.
	 */
	public static String makeSuffixForMimeType(String mimeType) {
		if (WWUtil.isEmpty(mimeType)) {
			String msg = Logging.getMessage("nullValue.MimeTypeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (!mimeType.contains("/") || mimeType.endsWith("/")) {
			String msg = Logging.getMessage("generic.MimeTypeIsInvalid", mimeType);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Remove any parameters appended to this mime type before using it as a key in the mimeTypeToSuffixMap. Mime
		// parameters do not change the mapping from mime type to suffix.
		int paramIndex = mimeType.indexOf(";");
		if (paramIndex != -1) mimeType = mimeType.substring(0, paramIndex);

		String suffix = mimeTypeToSuffixMap.get(mimeType);

		if (suffix == null) suffix = mimeType.substring(mimeType.lastIndexOf("/") + 1);

		suffix = suffix.replaceFirst("bil32", "bil"); // if bil32, replace with "bil" suffix.
		suffix = suffix.replaceFirst("bil16", "bil"); // if bil16, replace with "bil" suffix.

		return "." + suffix;
	}

	/**
	 * Creates a URL from an object.
	 * 
	 * @param path
	 *            the object from which to create a URL, typically a string.
	 * @return a URL for the specified object, or null if a URL could not be created.
	 * @see #makeURL(Object, String)
	 */
	public static URL makeURL(Object path) {
		try {
			if (path instanceof String) return new URL((String) path);
			else if (path instanceof File) return ((File) path).toURI().toURL();
			else if (path instanceof URL) return (URL) path;
			else return null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Creates a URL from an object. If the object does not already convert directly to a URL, a URL with a specified
	 * protocol is created.
	 * 
	 * @param path
	 *            the object from which to create a URL, typically a string.
	 * @param defaultProtocol
	 *            if non-null, a protocol to use if the specified path does not yet include a protocol.
	 * @return a URL for the specified object, or null if a URL could not be created.
	 * @see #makeURL(Object)
	 */
	public static URL makeURL(Object path, String defaultProtocol) {
		try {
			URL url = makeURL(path);

			if (url == null && !WWUtil.isEmpty(path.toString()) && !WWUtil.isEmpty(defaultProtocol)) url = new URL(defaultProtocol, null, path.toString());

			return url;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Converts a string to a URL.
	 * 
	 * @param path
	 *            the string to convert to a URL.
	 * @return a URL for the specified object, or null if a URL could not be created.
	 * @see #makeURL(Object)
	 * @see #makeURL(Object, String)
	 */
	public static URL makeURL(String path) {
		try {
			return new URL(path);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Creates a URI from an object.
	 * 
	 * @param path
	 *            the object from which to create a URI, typically a string.
	 * @return a URI for the specified object, or null if a URI could not be created.
	 * @see #makeURL(String)
	 * @see #makeURL(Object)
	 * @see #makeURL(Object, String)
	 */
	public static URI makeURI(Object path) {
		try {
			if (path instanceof String) return new URI((String) path);
			else if (path instanceof File) return ((File) path).toURI();
			else if (path instanceof URL) return ((URL) path).toURI();
			else return null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Open and XML document from a general source. The source type may be one of the following:
	 * <ul>
	 * <li>a {@link java.net.URL}</li>
	 * <li>an {@link java.io.InputStream}</li>
	 * <li>a {@link java.io.File}</li>
	 * <li>a {@link String} containing a valid URL description or a file or resource name available on the classpath.</li>
	 * </ul>
	 * 
	 * @param source
	 *            the source of the XML document.
	 * @return the source document as a {@link org.w3c.dom.Document}, or null if the source object is a string that does
	 *         not identify a URL, a file or a resource available on the classpath.
	 */
	public static InputStream openStream(Object source) {
		if (WWUtil.isEmpty(source)) {
			throw new IllegalArgumentException(Logging.getMessage("nullValue.SourceIsNull"));
		}

		if (source instanceof URL) {
			return openURLStream((URL) source);
		} else if (source instanceof InputStream) {
			return (InputStream) source;
		} else if (source instanceof File) {
            return openFileOrResourceStream(((File) source).getPath(), null);
        } else if (source instanceof Integer && context!=null) {
            return getContext().getResources().openRawResource((Integer) source);
		} else if (!(source instanceof String)) {
			throw new IllegalArgumentException(Logging.getMessage("generic.SourceTypeUnrecognized", source));
		}

		String sourceName = (String) source;

		URL url = WWIO.makeURL(sourceName);
		if (url != null) return openURLStream(url);

		return openFileOrResourceStream(sourceName, null);
	}

	public static BufferedInputStream openBufferedStream(Object source) {
		InputStream stream = openStream(source);
		if(stream!=null && !(stream instanceof BufferedInputStream))
			stream = new BufferedInputStream(stream);
		return (BufferedInputStream)stream;
	}

	/**
	 * Opens a file located via an absolute path or a path relative to the classpath.
	 * 
	 * @param path
	 *            the path of the file to open, either absolute or relative to the classpath.
	 * @param c
	 *            the class that will be used to find a path relative to the classpath.
	 * @return an {@link java.io.InputStream} to the open file
	 * @throws IllegalArgumentException
	 *             if the file name is null.
	 * @throws WWRuntimeException
	 *             if an exception occurs or the file can't be found. The causing exception is
	 *             available via this exception's {@link Throwable#initCause(Throwable)} method.
	 */
	public static InputStream openFileOrResourceStream(String path, Class<?> c) {
		if (path == null) {
			String msg = Logging.getMessage("nullValue.PathIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return WWIO.getFileOrResourceAsStream(path, c);
	}

	public static InputStream openURLStream(URL url) {
		if (url == null) {
			String msg = Logging.getMessage("nullValue.UrlIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		try {
			return url.openStream();
		} catch (IOException e) {
			throw new WWRuntimeException(Logging.getMessage("generic.UnableToOpenURL", url), e);
		}
	}

	/**
	 * Close a stream and catch any {@link IOException} generated in the process. This supports any object that
	 * implements the {@link java.io.Closeable} interface.
	 * 
	 * @param stream
	 *            the stream to close. If null, this method does nothing.
	 * @param name
	 *            the name of the stream to place in the log message if an exception is encountered.
	 */
	public static void closeStream(Object stream, String name) {
		if (stream == null) return;

		try {
			if (stream instanceof Closeable) {
				((Closeable) stream).close();
			} else {
				Logging.warning(Logging.getMessage("WWIO.StreamTypeNotSupported", stream, name != null ? name : Logging.getMessage("term.Unknown")));
			}
		} catch (IOException e) {
			Logging.error(Logging.getMessage("generic.ExceptionClosingStream", e, name != null ? name : Logging.getMessage("term.Unknown")));
		}
	}

	/**
	 * Reads the available bytes from the specified {@link java.nio.channels.ReadableByteChannel} up to the number of
	 * bytes remaining in the buffer. Bytes read from the specified channel are copied to the specified {@link ByteBuffer}. Upon returning the specified
	 * buffer's limit is set to the number of bytes read, and its position is
	 * set to zero.
	 * 
	 * @param channel
	 *            the channel to read bytes from.
	 * @param buffer
	 *            the buffer to receive the bytes.
	 * @return the specified buffer.
	 * @throws IllegalArgumentException
	 *             if the channel or the buffer is null.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static ByteBuffer readChannelToBuffer(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
		if (channel == null) {
			String msg = Logging.getMessage("nullValue.ChannelIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (buffer == null) {
			String msg = Logging.getMessage("nullValue.BufferIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		int count = 0;
		while (count >= 0 && buffer.hasRemaining()) {
			count = channel.read(buffer);
		}

		buffer.flip();

		return buffer;
	}

	public static ByteBuffer readStreamToBuffer(InputStream stream) throws IOException {
		return readStreamToBuffer(stream, false);
	}

	public static ByteBuffer readStreamToBuffer(InputStream stream, boolean allocateDirect) throws IOException {
		if (stream == null) {
			String msg = Logging.getMessage("nullValue.InputStreamIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Create a channel that redirects reads to the specified stream. We do not close this channel because doing so
		// would close the stream, which the caller owns.
		ReadableByteChannel channel = Channels.newChannel(stream);
		ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_PAGE_SIZE);

		int count = 0;
		while (count >= 0) {
			count = channel.read(buffer);
			if (count > 0 && !buffer.hasRemaining()) {
				ByteBuffer biggerBuffer = allocateDirect ? ByteBuffer.allocateDirect(buffer.limit() + DEFAULT_PAGE_SIZE) : ByteBuffer.allocate(buffer.limit() + DEFAULT_PAGE_SIZE);
				biggerBuffer.put((ByteBuffer) buffer.rewind());
				buffer = biggerBuffer;
			}
		}

		if (buffer != null) buffer.flip();

		return buffer;
	}

	// TODO: rename as readStreamToString
	public static String readTextStream(InputStream stream, String encoding) throws IOException {
		if (stream == null) {
			String msg = Logging.getMessage("nullValue.InputStreamIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		StringBuilder sb = new StringBuilder();

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(stream, encoding != null ? encoding : DEFAULT_CHARACTER_ENCODING));
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} finally {
			WWIO.closeStream(reader, null);
		}

		return sb.toString();
	}

	/**
	 * Reads all the bytes from the specified {@link URL}, returning the bytes as a non-direct {@link ByteBuffer} with
	 * the current JVM byte order. Non-direct buffers are backed by JVM heap memory.
	 * 
	 * @param url
	 *            the URL to read.
	 * @return the bytes from the specified URL, with the current JVM byte order.
	 * @throws IllegalArgumentException
	 *             if the URL is null.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static ByteBuffer readURLContentToBuffer(URL url) throws IOException {
		if (url == null) {
			String message = Logging.getMessage("nullValue.URLIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		return readURLContentToBuffer(url, false);
	}

	/**
	 * Reads all the bytes from the specified {@link URL}, returning the bytes as a {@link ByteBuffer} with the current
	 * JVM byte order. This returns a direct ByteBuffer if allocateDirect is true, and returns a non-direct ByteBuffer
	 * otherwise. Direct buffers are backed by native memory, and may resite outside of the normal garbage-collected
	 * heap. Non-direct buffers are backed by JVM heap memory.
	 * 
	 * @param url
	 *            the URL to read.
	 * @param allocateDirect
	 *            true to allocate and return a direct buffer, false to allocate and return a non-direct
	 *            buffer.
	 * @return the bytes from the specified URL, with the current JVM byte order.
	 * @throws IllegalArgumentException
	 *             if the URL is null.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static ByteBuffer readURLContentToBuffer(URL url, boolean allocateDirect) throws IOException {
		if (url == null) {
			String message = Logging.getMessage("nullValue.URLIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		InputStream is = null;
		try {
			is = url.openStream();
			return readStreamToBuffer(is, allocateDirect);
		} finally {
			WWIO.closeStream(is, url.toString());
		}
	}

	/**
	 * Converts a specified URL as to a path in the local file system. If the URL cannot be converted to a file path for
	 * any reason, this returns null.
	 * 
	 * @param url
	 *            the URL to convert to a local file path.
	 * @return a local File path, or null if the URL could not be converted.
	 * @throws IllegalArgumentException
	 *             if the url is null.
	 */
	public static File convertURLToFile(URL url) {
		if (url == null) {
			String message = Logging.getMessage("nullValue.URLIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		try {
			return new File(url.toURI());
		} catch (IllegalArgumentException e) {
			// Thrown if the URI cannot be interpreted as a path on the local filesystem.
			return null;
		} catch (URISyntaxException e) {
			// Thrown if the URL cannot be converted to a URI.
			return null;
		}
	}

	public static boolean saveBuffer(ByteBuffer buffer, File file, boolean forceFilesystemWrite) throws IOException {
		if (buffer == null) {
			String message = Logging.getMessage("nullValue.BufferNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (file == null) {
			String message = Logging.getMessage("nullValue.FileIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		FileOutputStream fos = null;
		FileChannel channel = null;
		FileLock lock;
		int numBytesWritten = 0;
		try {
			fos = new FileOutputStream(file);
			channel = fos.getChannel();

			lock = channel.tryLock();
			if (lock == null) {
				// The file is being written to, or some other process is keeping it to itself.
				// This is an okay condition, but worth noting.
				Logging.verbose(Logging.getMessage("WWIO.UnableToAcquireLockFor", file.getPath()));
				return false;
			}

			for (buffer.rewind(); buffer.hasRemaining();) {
				numBytesWritten += channel.write(buffer);
			}

			// Optionally force writing to the underlying storage device. Doing so ensures that all contents are
			// written to the device (and not in the I/O cache) in the event of a system failure.
			if (forceFilesystemWrite) channel.force(true);
			fos.flush();
			return true;
		} catch (ClosedByInterruptException e) {
			Logging.verbose(Logging.getMessage("generic.interrupted", "WWIO.saveBuffer", file.getPath()), e);

			if (numBytesWritten > 0) // don't leave behind incomplete files
			file.delete();

			throw e;
		} catch (IOException e) {
			Logging.error(Logging.getMessage("WWIO.ErrorSavingBufferTo", file.getPath()), e);

			if (numBytesWritten > 0) // don't leave behind incomplete files
			file.delete();

			throw e;
		} finally {
			WWIO.closeStream(channel, file.getPath()); // also releases the lock
			WWIO.closeStream(fos, file.getPath());
		}
	}

	public static boolean saveBuffer(ByteBuffer buffer, File file) throws IOException {
		// By default, force changes to be written to the underlying storage device.
		return saveBuffer(buffer, file, true);
	}

	/**
	 * Create a {@link String} of limited size from a {@link ByteBuffer}.
	 * 
	 * @param buffer
	 *            the byte buffer to convert.
	 * @param length
	 *            the maximum number of characters to read from the buffer. Must be greater than 0.
	 * @param encoding
	 *            the encoding do use. If null is specified then UTF-8 is used.
	 * @return the string representation of the bytes in the buffer decoded according to the specified encoding.
	 * @throws IllegalArgumentException
	 *             if the buffer is null or the length is less than 1.
	 * @throws java.nio.charset.IllegalCharsetNameException
	 *             if the specified encoding name is illegal.
	 * @throws java.nio.charset.UnsupportedCharsetException
	 *             if no support for the named encoding is available.
	 */
	public static String byteBufferToString(ByteBuffer buffer, int length, String encoding) {
		if (buffer == null) {
			String msg = Logging.getMessage("nullValue.BufferIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (length < 1) {
			String msg = Logging.getMessage("generic.LengthIsInvalid", length);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		CharBuffer charBuffer = Charset.forName(encoding != null ? encoding : DEFAULT_CHARACTER_ENCODING).decode(buffer);
		if (charBuffer.remaining() > length) {
			charBuffer = charBuffer.slice();
			charBuffer.limit(length);
		}

		return charBuffer.toString();
	}

	public static boolean isFileOutOfDate(URL url, long expiryTime) {
		if (url == null) {
			String message = Logging.getMessage("nullValue.URLIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		try {
			// Determine whether the file can be treated like a File, e.g., a jar entry.
			URI uri = url.toURI();
			if (uri.isOpaque()) return false; // TODO: Determine how to check the date of non-Files

			File file = new File(uri);

			return file.exists() && file.lastModified() < expiryTime;
		} catch (URISyntaxException e) {
			Logging.error(Logging.getMessage("WWIO.ExceptionValidatingFileExpiration", url));
			return false;
		}
	}

	/**
	 * Get file extension from filename
	 * @param filename	the filename
	 * @return	the extension, without period character
	 */
	public static String getFileExtension(String filename) {
		return filename.substring(filename.length()-3);
	}

	public static Proxy configureProxy() {
		String proxyHost = Configuration.getStringValue(AVKey.URL_PROXY_HOST);
		if (proxyHost == null) return null;

		Proxy proxy = null;

		try {
			int proxyPort = Configuration.getIntegerValue(AVKey.URL_PROXY_PORT);
			String proxyType = Configuration.getStringValue(AVKey.URL_PROXY_TYPE);

			SocketAddress address = new InetSocketAddress(proxyHost, proxyPort);
			if (proxyType.equals("Proxy.Type.Http")) proxy = new Proxy(Proxy.Type.HTTP, address);
			else if (proxyType.equals("Proxy.Type.SOCKS")) proxy = new Proxy(Proxy.Type.SOCKS, address);
		} catch (Exception e) {
			Logging.warning(Logging.getMessage("URLRetriever.ErrorConfiguringProxy", proxyHost), e);
		}

		return proxy;
	}

	public static String appendPathPart(String firstPart, String secondPart) {
		if (secondPart == null || secondPart.length() == 0) return firstPart;
		if (firstPart == null || firstPart.length() == 0) return secondPart;

		StringBuilder sb = new StringBuilder();
		sb.append(WWIO.stripTrailingSeparator(firstPart));
		sb.append(File.separator);
		sb.append(WWIO.stripLeadingSeparator(secondPart));

		return sb.toString();
	}

	public static String stripTrailingSeparator(String s) {
		if (s == null) {
			String message = Logging.getMessage("nullValue.StringIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (s.endsWith("/") || s.endsWith("\\")) return s.substring(0, s.length() - 1);
		else return s;
	}

	public static String stripLeadingSeparator(String s) {
		if (s == null) {
			String message = Logging.getMessage("nullValue.StringIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (s.startsWith("/") || s.startsWith("\\")) return s.substring(1, s.length());
		else return s;
	}

	/**
	 * Replaces any illegal filename characters in a specified string with an underscore, "_".
	 * 
	 * @param s
	 *            the string to examine.
	 * @return a new string with illegal filename characters replaced.
	 * @throws IllegalArgumentException
	 *             if the specified string is null.
	 */
	public static String replaceIllegalFileNameCharacters(String s) {
		if (s == null) {
			String message = Logging.getMessage("nullValue.StringIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		return s.replaceAll(ILLEGAL_FILE_PATH_PART_CHARACTERS, "_");
	}

	/**
	 * Ensure that all directories leading the element at the end of a file path exist. Create any nonexistent
	 * directories in the path. A directory is not creared for the last element in the path; it's assumed to be a file
	 * name and is ignored.
	 * 
	 * @param path
	 *            the path whose directories are vefified to exist or be created. The last element of the path is
	 *            ignored.
	 * @return true if all the directories in the path exist or were created.
	 * @throws IllegalArgumentException
	 *             if the path is null.
	 */
	public static boolean makeParentDirs(String path) {
		if (path == null) {
			String message = Logging.getMessage("nullValue.FilePathIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		String fs = File.separator;
		String[] pathParts = path.split("[/" + (fs.equals("/") ? "" : (fs.equals("\\") ? "\\\\" : fs)) + "]");
		if (pathParts.length <= 1) return true;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < pathParts.length - 1; i++) {
			if (pathParts[i].length() == 0) continue;

			sb.append(File.separator);
			sb.append(pathParts[i]);
		}

		return (new File(sb.toString())).mkdirs();
	}

	/**
	 * Create a directory in the computer's temp directory.
	 * 
	 * @return a file reference to the new directory, of null if a directory could not be created.
	 * @throws IOException
	 *             if a directory could not be created.
	 * @throws SecurityException
	 *             if a security manager exists and it does not allow directory creation.
	 */
	public static File makeTempDir() throws IOException {
		// To make a directory in the computer's temp directory, generate the name of a temp file then delete the file
		// and create a directory of the same name.
		File temp = File.createTempFile("wwj", null);

		if (!temp.delete()) return null;

		if (!temp.mkdir()) return null;

		return temp;
	}

	public static GpuProgram getGpuProgram(GpuResourceCache cache, Object programKey, int vert, int frag) {
		GpuProgram program = cache.getProgram(programKey);

		if (program == null) {
			try {
				GpuProgram.GpuProgramSource source = GpuProgram.readProgramSource(vert, frag);
				program = new GpuProgram(source);
				cache.put(programKey, program);
			} catch (Exception e) {
				String msg = Logging.getMessage("GL.ExceptionLoadingProgram",
						getContext().getResources().getResourceName(vert),
						getContext().getResources().getResourceName(frag));
				Logging.error(msg);
				return null;
			}
		}

		return program;
	}
}
