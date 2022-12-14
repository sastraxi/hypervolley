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

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** This is a {@link FrameBuffer} variant backed by a float texture. */
public class FrameBufferR32F extends FrameBuffer {

    /** Creates a new FrameBuffer with a float backing texture, having the given dimensions and potentially a depth buffer attached.
     *
     * @param width the width of the framebuffer in pixels
     * @param height the height of the framebuffer in pixels
     * @param hasDepth whether to attach a depth buffer
     * @throws GdxRuntimeException in case the FrameBuffer could not be created */
    public FrameBufferR32F(int width, int height, boolean hasDepth) {
        super(width, height);

        Builder builder = new FrameBuffer.Builder(width, height);
        builder.addColorTexture(GL30.GL_R32F, GL30.GL_RED, GL20.GL_FLOAT, TextureFilter.Nearest, TextureFilter.Nearest, TextureWrap.ClampToEdge);

        if (hasDepth) {
            builder.addDepthRenderbuffer(GL30.GL_DEPTH_COMPONENT24, GL30.GL_UNSIGNED_INT);
        }

        this.attachments = builder.buildAttachments();
        build();
    }

}
