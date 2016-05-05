package com.sastraxi.gdx.graphics.glutils;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter;
import com.badlogic.gdx.utils.Array;
import com.sastraxi.playground.tennis.graphics.CustomShaderAttribute;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Render materials in the order passed into the constructor;
 * don't render anything at all for materials with IDs not in our list.
 */
public class CustomShaderSorter implements RenderableSorter, Comparator<Renderable> {

    private int[] typeIds;

    public CustomShaderSorter(CustomShaderAttribute.ShaderType... types) {
        this.typeIds = new int[types.length];
        for (int i = 0; i < typeIds.length; ++i) {
            this.typeIds[i] = types[i].ordinal();
        }
    }

    /**
     * Simple String[] indexOf function.
     * @param sought needle
     * @param list haystack
     * @return -1 if not found, otherwise the array index i such that list[i].equals(sought).
     */
    private static int find(int sought, int[] list) {
        if (sought == -1) return -1; // invalid oridnal value
        for (int i = 0; i < list.length; ++i) {
            if (list[i] == sought) return i;
        }
        return -1;
    }

    @Override
    public void sort(Camera camera, Array<Renderable> renderables) {
        for (Iterator<Renderable> it = renderables.iterator(); it.hasNext();) {
            int shaderType = getCustomShaderType(it.next());
            if (-1 == find(shaderType, typeIds)) {
                it.remove();
            }
        }
        renderables.sort(this);
    }

    private int getCustomShaderType(Renderable r) {
        CustomShaderAttribute attr = (CustomShaderAttribute) r.material.get(CustomShaderAttribute.ID);
        if (attr != null) {
            return attr.shaderType.ordinal();
        } else {
            return -1;
        }
    }

    @Override
    public int compare(Renderable o1, Renderable o2)
    {
        return Integer.compare(
            find(getCustomShaderType(o1), typeIds),
            find(getCustomShaderType(o2), typeIds));
    }
}
