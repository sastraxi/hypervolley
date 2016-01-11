/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.sastraxi.gdx.graphics.glutils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import org.lwjgl.opengl.GL32;

import java.util.HashMap;
import java.util.Map;

/** <p>
 * Encapsulates OpenGL 4.3+ frame buffer objects. This is a simple class that covers the use case of an
 * optionally- It will
 * automatically create a gltexture for the color attachment and a renderbuffer for the depth buffer. You can get a hold of the
 * gltexture by {@link DesktopFrameBuffer#getColorBufferTexture()}. This class will only work with OpenGL ES 2.0.
 * </p>
 *
 * <p>
 * FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to another
 * application or receives an incoming call, the framebuffer will be automatically recreated.
 * </p>
 *
 * <p>
 * A FrameBuffer must be disposed if it is no longer needed
 * </p>
 *
 * @author mzechner, realitix, sastraxi */
public class DesktopFrameBuffer implements Disposable {
	/** the frame buffers **/
	private final static Map<Application, Array<DesktopFrameBuffer>> buffers = new HashMap<Application, Array<DesktopFrameBuffer>>();

	private final static int GL_DEPTH24_STENCIL8_OES = 0x88F0;

	/** the color buffer texture **/
	protected TextureMultisample colorTexture;

	/** the default framebuffer handle, a.k.a screen. On desktop it's always 0 */
	private static int defaultFramebufferHandle = 0;

	/** the framebuffer handle **/
	private int framebufferHandle;

	/** the depth buffer texture **/
	protected TextureMultisample depthTexture;

	/** the stencilbuffer render object handle **/
	private int stencilbufferHandle;

	/** the depth stencil packed render buffer object handle **/
	private int depthStencilPackedBufferHandle;

	/** width **/
	protected final int width;

	/** height **/
	protected final int height;

	/** samples **/
	protected final int samples;

	/** depth **/
	protected final boolean hasDepth;

	/** stencil **/
	protected final boolean hasStencil;

	/** Creates a new FrameBuffer having the given dimensions and potentially a depth buffer attached.
	 *
	 * @param width
	 * @param height
	 * @param hasDepth */
	public DesktopFrameBuffer(int width, int height, int samples, boolean hasDepth) {
		this(width, height, samples, hasDepth, false);
	}

	/** Creates a new FrameBuffer having the given dimensions and potentially a depth and a stencil buffer attached.
	 *
	 * @param width the width of the framebuffer in pixels
	 * @param height the height of the framebuffer in pixels
	 * @param samples number of samples in the color/depth textures
	 * @param hasDepth whether to create and attach a depth texture
	 * @param hasStencil whether to attach a stencil buffer
	 * @throws com.badlogic.gdx.utils.GdxRuntimeException in case the FrameBuffer could not be created */
	public DesktopFrameBuffer(int width, int height, int samples, boolean hasDepth, boolean hasStencil) {
		this.width = width;
		this.height = height;
		this.samples = samples;
		this.hasDepth = hasDepth;
		this.hasStencil = hasStencil;
		build();

		addManagedFrameBuffer(Gdx.app, this);
	}

	protected TextureMultisample createColorTexture () {
		int glFormat = Pixmap.Format.toGlFormat(Pixmap.Format.RGBA8888);
		int glType = Pixmap.Format.toGlType(Pixmap.Format.RGBA8888);
		GLOnlyTextureDataMultisample data = new GLOnlyTextureDataMultisample(width, height, glFormat, glFormat, glType, samples);
		TextureMultisample result = new TextureMultisample(data, samples > 1);
		result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
		return result;
	}

	protected void disposeColorTexture (TextureMultisample colorTexture) {
		colorTexture.dispose();
	}

	protected TextureMultisample createDepthTexture () {
		int glFormat = GL30.GL_DEPTH_COMPONENT;
		int glType = GL30.GL_UNSIGNED_INT;
		GLOnlyTextureDataMultisample data = new GLOnlyTextureDataMultisample(width, height, glFormat, glFormat, glType, samples);
		TextureMultisample result = new TextureMultisample(data, samples > 1);
		result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
		return result;
	}

	protected void disposeDepthTexture (TextureMultisample depthTexture) {
		depthTexture.dispose();
	}

	private void build () {
		GL20 gl = Gdx.gl20;

		colorTexture = createColorTexture();

		framebufferHandle = gl.glGenFramebuffer();

		if (hasDepth) {
			depthTexture = createDepthTexture();
		}

		if (hasStencil) {
			stencilbufferHandle = gl.glGenRenderbuffer();
		}

		int target2D = samples > 1 ? GL32.GL_TEXTURE_2D_MULTISAMPLE : GL20.GL_TEXTURE_2D;

		gl.glBindTexture(target2D, colorTexture.getTextureObjectHandle());

		if (hasStencil) {
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, stencilbufferHandle);
			gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, GL20.GL_STENCIL_INDEX8, colorTexture.getWidth(), colorTexture.getHeight());
		}

		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
		gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, target2D,
			colorTexture.getTextureObjectHandle(), 0);

		if (hasDepth) {
			gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, target2D,
				depthTexture.getTextureObjectHandle(), 0);
		}

		if (hasStencil) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, stencilbufferHandle);
		}

		gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
		gl.glBindTexture(target2D, 0);

		int result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);

		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);

		if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {

			disposeColorTexture(colorTexture);

			if (hasDepth) disposeDepthTexture(depthTexture);
			if (hasStencil) gl.glDeleteRenderbuffer(stencilbufferHandle);

			gl.glDeleteFramebuffer(framebufferHandle);

			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
				throw new IllegalStateException("frame buffer couldn't be constructed: incomplete attachment");
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS)
				throw new IllegalStateException("frame buffer couldn't be constructed: incomplete dimensions");
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)
				throw new IllegalStateException("frame buffer couldn't be constructed: missing attachment");
			if (result == GL20.GL_FRAMEBUFFER_UNSUPPORTED)
				throw new IllegalStateException("frame buffer couldn't be constructed: unsupported combination of formats");
			throw new IllegalStateException("frame buffer couldn't be constructed: unknown error " + result);
		}
	}

	/** Releases all resources associated with the FrameBuffer. */
	@Override
	public void dispose () {
		GL20 gl = Gdx.gl20;

		disposeColorTexture(colorTexture);

		if (hasDepth) disposeDepthTexture(depthTexture);
		if (hasStencil) gl.glDeleteRenderbuffer(stencilbufferHandle);

		gl.glDeleteFramebuffer(framebufferHandle);

		if (buffers.get(Gdx.app) != null) buffers.get(Gdx.app).removeValue(this, true);
	}

	/** Makes the frame buffer current so everything gets drawn to it. */
	public void bind () {
		Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
	}

	/** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
	public static void unbind () {
		Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);
	}

	/** Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it. */
	public void begin () {
		bind();
		setFrameBufferViewport();
	}

	/** Sets viewport to the dimensions of framebuffer. Called by {@link #begin()}. */
	protected void setFrameBufferViewport () {
		Gdx.gl20.glViewport(0, 0, colorTexture.getWidth(), colorTexture.getHeight());
	}

	/** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
	public void end () {
		end(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	/** Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
	 *
	 * @param x the x-axis position of the viewport in pixels
	 * @param y the y-asis position of the viewport in pixels
	 * @param width the width of the viewport in pixels
	 * @param height the height of the viewport in pixels */
	public void end (int x, int y, int width, int height) {
		unbind();
		Gdx.gl20.glViewport(x, y, width, height);
	}

	/** @return the gl texture */
	public TextureMultisample getColorBufferTexture () {
		return colorTexture;
	}

	/** @return the gl texture */
	public TextureMultisample getDepthBufferTexture () {
		return depthTexture;
	}

	/** @return The OpenGL handle of the framebuffer (see {@link GL20#glGenFramebuffer()}) */
	public int getFramebufferHandle () {
		return framebufferHandle;
	}

	/** @return The OpenGL handle of the (optional) stencil buffer (see {@link GL20#glGenRenderbuffer()}). May return 0 even if stencil buffer enabled */
	public int getStencilBufferHandle () {
		return stencilbufferHandle;
	}

	/** @return the height of the framebuffer in pixels */
	public int getHeight () {
		return colorTexture.getHeight();
	}

	/** @return the width of the framebuffer in pixels */
	public int getWidth () {
		return colorTexture.getWidth();
	}

	/** @return the depth of the framebuffer in pixels (if applicable) */
	public int getDepth () {
		return colorTexture.getDepth();
	}

	private static void addManagedFrameBuffer (Application app, DesktopFrameBuffer frameBuffer) {
		Array<DesktopFrameBuffer> managedResources = buffers.get(app);
		if (managedResources == null) managedResources = new Array<DesktopFrameBuffer>();
		managedResources.add(frameBuffer);
		buffers.put(app, managedResources);
	}

	/** Invalidates all frame buffers. This can be used when the OpenGL context is lost to rebuild all managed frame buffers. This
	 * assumes that the texture attached to this buffer has already been rebuild! Use with care. */
	public static void invalidateAllFrameBuffers (Application app) {
		if (Gdx.gl20 == null) return;

		Array<DesktopFrameBuffer> bufferArray = buffers.get(app);
		if (bufferArray == null) return;
		for (int i = 0; i < bufferArray.size; i++) {
			bufferArray.get(i).build();
		}
	}

	public static void clearAllFrameBuffers (Application app) {
		buffers.remove(app);
	}

	public static StringBuilder getManagedStatus (final StringBuilder builder) {
		builder.append("Managed buffers/app: { ");
		for (Application app : buffers.keySet()) {
			builder.append(buffers.get(app).size);
			builder.append(" ");
		}
		builder.append("}");
		return builder;
	}

	public static String getManagedStatus () {
		return getManagedStatus(new StringBuilder()).toString();
	}
}
