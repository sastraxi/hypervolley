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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengles.GLES31;

/** A {@link TextureData} implementation which should be used to create gl only textures.
 * This TextureData fits perfectly for FrameBuffer. The data is not managed.
 * This is a desktop-only */
public class GLOnlyTextureDataMultisample implements TextureData {

	/** width and height */
	int width = 0;
	int height = 0;
	boolean isPrepared = false;

	/** properties of opengl texture */
	int samples;
	int internalFormat;
	int format;
	int type;

	/** @see "https://www.khronos.org/opengles/sdk/docs/man/xhtml/glTexImage2D.xml"
	 * @param internalFormat Specifies the internal format of the texture. Must be one of the following symbolic constants:
	 *           {@link GL20#GL_ALPHA}, {@link GL20#GL_LUMINANCE}, {@link GL20#GL_LUMINANCE_ALPHA}, {@link GL20#GL_RGB},
	 *           {@link GL20#GL_RGBA}.
	 * @param format Specifies the format of the texel data. Must match internalformat. The following symbolic values are accepted:
	 *           {@link GL20#GL_ALPHA}, {@link GL20#GL_RGB}, {@link GL20#GL_RGBA}, {@link GL20#GL_LUMINANCE}, and
	 *           {@link GL20#GL_LUMINANCE_ALPHA}.
	 * @param type Specifies the data type of the texel data. The following symbolic values are accepted:
	 *           {@link GL20#GL_UNSIGNED_BYTE}, {@link GL20#GL_UNSIGNED_SHORT_5_6_5}, {@link GL20#GL_UNSIGNED_SHORT_4_4_4_4}, and
	 *           {@link GL20#GL_UNSIGNED_SHORT_5_5_5_1}.
	 * @param samples Specifies the number of samples
	 *           */
	public GLOnlyTextureDataMultisample(int width, int height, int internalFormat, int format, int type, int samples) {
		this.width = width;
		this.height = height;
		this.internalFormat = internalFormat;
		this.format = format;
		this.type = type;
		this.samples = samples;
	}

	@Override
	public TextureDataType getType () {
		return TextureDataType.Custom;
	}

	@Override
	public boolean isPrepared () {
		return isPrepared;
	}

	@Override
	public void prepare () {
		if (isPrepared) throw new GdxRuntimeException("Already prepared");
		isPrepared = true;
	}

	@Override
	public void consumeCustomData (int target) {
		if (samples == 1) {
			Gdx.gl.glTexImage2D(target, 0, internalFormat, width, height, 0, format, type, null);
		} else {
			// GL43.glTexStorage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, samples, internalFormat, width, height, true);
			GL32.glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, samples, internalFormat, width, height, true);
		}
	}

	@Override
	public Pixmap consumePixmap () {
		throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
	}

	@Override
	public boolean disposePixmap () {
		throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
	}

	@Override
	public int getWidth () {
		return width;
	}

	@Override
	public int getHeight () {
		return height;
	}

	/**
	 * Note that the format returned here is always RGBA8888 (and thus incorrect).
     */
	@Override
	public Format getFormat () {
		return Format.RGBA8888;
	}

	@Override
	public boolean useMipMaps () {
		return false;
	}

	@Override
	public boolean isManaged () {
		return false;
	}
}
