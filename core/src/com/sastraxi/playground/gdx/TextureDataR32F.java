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

package com.sastraxi.playground.gdx;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** A {@link TextureData} implementation which should be used to create float textures. */
public class TextureDataR32F implements TextureData {

    int width = 0;
    int height = 0;
    boolean isPrepared = false;
    FloatBuffer buffer;

    public TextureDataR32F(int w, int h) {
        this.width = w;
        this.height = h;
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
        // TODO this implementation should not create a buffer in system RAM
        this.buffer = BufferUtils.newFloatBuffer(width * height);
        isPrepared = true;
    }

    @Override
    public void consumeCustomData (int target) {
        // FIXME test stopped working in upgrade from 1.6.2 -> 1.7.2
        /*
        if (!Gdx.graphics.supportsExtension("texture_float"))
            throw new GdxRuntimeException("Extension OES_TEXTURE_FLOAT not supported!");
        */

        Gdx.gl.glTexImage2D(target, 0, GL30.GL_R32F, width, height, 0, GL30.GL_RED, GL20.GL_FLOAT, buffer);
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

    @Override
    public Format getFormat () {
        return Format.RGBA8888; // it's not true, but TextureDataR32F.getFormat() isn't used anywhere
    }

    @Override
    public boolean useMipMaps () {
        return false;
    }

    @Override
    public boolean isManaged () {
        return true;
    }
}
