package com.sastraxi.playground.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.lang.reflect.Field;

/**
 * A shader program that will automatically add #defines from constant ints/floats/doubles/colours to the beginning of the source.
 */
public class DefinedShaderProgram {

    public static ShaderProgram create(ConstRef[] constRefs, FileHandle vertexSourceFile, FileHandle fragmentSourceFile) {
        return create(constRefs, vertexSourceFile, fragmentSourceFile, "");
    }


    public static ShaderProgram create(ConstRef[] constRefs, FileHandle vertexSourceFile, FileHandle fragmentSourceFile, String prefix)
    {
        String[] defineStatements = generateDefineStatements(constRefs);
        StringBuffer buf;

        // build the vertex shader ///////////////////////////////////////////////////
        buf = new StringBuffer();
        buf.append(prefix);
        for (String defineStatement: defineStatements) {
            buf.append(defineStatement);
            Gdx.app.log("DefinedShaderProgram", defineStatement);
            buf.append("\n");
        }
        buf.append(vertexSourceFile.readString());
        String vertexShader = buf.toString();

        // build the fragment shader //////////////////////////////////////////////////
        buf = new StringBuffer();
        buf.append(prefix);
        for (String defineStatement: defineStatements) {
            buf.append(defineStatement);
            buf.append("\n");
        }
        buf.append(fragmentSourceFile.readString());
        String fragmentShader = buf.toString();

        System.out.println(vertexShader);
        System.out.println(fragmentShader);
        return new ShaderProgram(vertexShader, fragmentShader);
    }

    private static String[] generateDefineStatements(ConstRef[] constRefs)
    {
        String[] defineStatements = new String[constRefs.length];
        for (int i = 0; i < constRefs.length; ++i)
        {
            String variableName = constRefs[i].getName();
            Class sourceClass = constRefs[i].getKlass();
            try {
                Field constantField = sourceClass.getDeclaredField(variableName);
                Object constant = constantField.get(null);
                if (constant instanceof Color) {
                    Color x = (Color) constant;
                    defineStatements[i] = "const vec4 " + variableName + " = vec4(" + x.r + ", " + x.g + ", " + x.b + ", " + x.a + ");";
                } else {
                    Double x = Double.valueOf(constant.toString());
                    defineStatements[i] = "const float " + variableName + " = " + String.format("%.5f", x) + "f;";
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }

        }
        return defineStatements;
    }

}