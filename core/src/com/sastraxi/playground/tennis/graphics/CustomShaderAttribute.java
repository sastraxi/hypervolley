package com.sastraxi.playground.tennis.graphics;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.MathUtils;

/**
 * Created by sastr on 2015-09-23.
 */
public class CustomShaderAttribute extends Attribute {
    public final static String Alias = "ShaderID";
    public final static long ID = register(Alias);

    public final ShaderType shaderType;

    public enum ShaderType {
        BOUNCE_MARKER, PLAYER_POWER,
        WORLD_DYNAMIC,
        WORLD_STATIC,
        REFLECTIVE_SURFACE
    }
    public CustomShaderAttribute (final ShaderType shaderType) {
        super(ID);
        this.shaderType = shaderType;
    }

    @Override
    public Attribute copy () {
        return new CustomShaderAttribute(shaderType);
    }

    @Override
    protected boolean equals (Attribute other) {
        return ((CustomShaderAttribute)other).shaderType == shaderType;
    }

    @Override
    public int compareTo (Attribute o) {
        if (type != o.type) return type < o.type ? -1 : 1;
        ShaderType otherType = ((CustomShaderAttribute)o).shaderType;
        return Integer.compare(shaderType.ordinal(), otherType.ordinal());
    }
}
