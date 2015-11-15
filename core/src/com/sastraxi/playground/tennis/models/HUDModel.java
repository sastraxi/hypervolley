package com.sastraxi.playground.tennis.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.sastraxi.playground.tennis.Constants;
import com.sastraxi.playground.tennis.graphics.Materials;

/**
 * Created by sastr on 2015-11-15.
 */
public class HUDModel {

    private static final long vertexAttributes = VertexAttributes.Usage.Position;

    private static ModelBuilder builder = new ModelBuilder();

    /**
     * The score marker is a slanted rhombus with a shadow.
     * @param slantDirection true = right
     */
    public static Model buildScoreMarker(boolean slantDirection, Color colour)
    {
        float h_w = 0.5f * Constants.HUD_SCORE_MARKER_SIZE;
        float h_h = 0.5f * Constants.HUD_SCORE_MARKER_SIZE;
        float slant = slantDirection ? 0.2f : -0.2f;
        float shadow_offset = -h_h * 0.2f;
        float shadow_scale = 1.1f;

        // doesn't matter what colour we set here, it will be set from the player's colour.
        builder.begin();
        builder.node();

        Material material;

        // the shadow
        material = new Material(ColorAttribute.createDiffuse(0f, 0f, 0f, 1f), new BlendingAttribute(Constants.HUD_SHADOW_ALPHA));
        builder.part("shadow", GL20.GL_TRIANGLES, vertexAttributes, material)
                .rect(shadow_scale * (-h_w + slant * h_w), shadow_scale * (-h_h + shadow_offset), 0f,
                      shadow_scale * ( h_w + slant * h_w), shadow_scale * (-h_h + shadow_offset), 0f,
                      shadow_scale * ( h_w - slant * h_w), shadow_scale * ( h_h + shadow_offset), 0f,
                      shadow_scale * (-h_w - slant * h_w), shadow_scale * ( h_h + shadow_offset), 0f,
                      0f, 0f, 1f);

        // the main part
        material = new Material(Materials.ID_MARKER_COLOUR, ColorAttribute.createDiffuse(colour), new BlendingAttribute(1f));
        builder.part("scoreMarker", GL20.GL_TRIANGLES, vertexAttributes, material)
                .rect(-h_w + slant * h_w, -h_h, 0f,
                       h_w + slant * h_w, -h_h, 0f,
                       h_w - slant * h_w, h_h, 0f,
                      -h_w - slant * h_w, h_h, 0f,
                       0f, 0f, 1f);

        return builder.end();
    }

}
