package com.sastraxi.playground.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Created by sastr on 2015-11-04.
 */
public class PostProcessShaderProgram extends ShaderProgram {

    public PostProcessShaderProgram(FileHandle fragmentShader) {
        super(Gdx.files.internal("shaders/post/post.vertex.glsl"), fragmentShader);
        if (!isCompiled()) {
            System.out.println(getLog());
        }
    }
}
