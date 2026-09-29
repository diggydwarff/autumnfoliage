package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes otherwise-untinted vegetation quads participate in the standard BlockColor pipeline.
 * Because the decision is based on the runtime BlockState, all baked models can be wrapped safely;
 * non-vegetation and item renders are returned untouched.
 */
public final class TintForcingBakedModel extends BakedModelWrapper<BakedModel> {
    public TintForcingBakedModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        List<BakedQuad> quads = originalModel.getQuads(state, side, rand);
        return maybeForceTint(state, quads);
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData extraData,
            @Nullable RenderType renderType
    ) {
        List<BakedQuad> quads = originalModel.getQuads(state, side, rand, extraData, renderType);
        return maybeForceTint(state, quads);
    }

    private static List<BakedQuad> maybeForceTint(@Nullable BlockState state, List<BakedQuad> quads) {
        if (state == null || quads.isEmpty() || !AutumnConfig.runtime().forceTintUntintedModels()) {
            return quads;
        }

        VegetationType type = VegetationClassifier.classify(state);
        if (!VegetationClassifier.canForceTintWholeModel(state, type)) {
            return quads;
        }

        boolean needsCopy = false;
        for (BakedQuad quad : quads) {
            if (!quad.isTinted()) {
                needsCopy = true;
                break;
            }
        }
        if (!needsCopy) {
            return quads;
        }

        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (quad.isTinted()) {
                out.add(quad);
            } else {
                out.add(new BakedQuad(
                        quad.getVertices(),
                        0,
                        quad.getDirection(),
                        quad.getSprite(),
                        quad.isShade(),
                        quad.hasAmbientOcclusion()
                ));
            }
        }
        return out;
    }
}
