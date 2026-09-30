package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.DelegateBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Makes otherwise-untinted vegetation quads participate in the standard BlockColor pipeline.
 *
 * Minecraft 1.21.4 still uses BakedModel for block rendering. Normal vegetation gets tint index 0
 * only on otherwise-untinted quads. Living bamboo is handled specially: every bamboo quad is routed
 * by its actual sprite so stalks use tint index 1 and leaf/sapling geometry uses tint index 0.
 */
public final class TintForcingBakedModel extends DelegateBakedModel {
    public TintForcingBakedModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        List<BakedQuad> quads = parent.getQuads(state, side, rand);
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
        List<BakedQuad> quads = parent.getQuads(state, side, rand, extraData, renderType);
        return maybeForceTint(state, quads);
    }

    private static List<BakedQuad> maybeForceTint(@Nullable BlockState state, List<BakedQuad> quads) {
        if (state == null || quads.isEmpty() || !AutumnConfig.runtime().forceTintUntintedModels()) {
            return quads;
        }

        VegetationType type = VegetationClassifier.classify(state);

        if (VegetationClassifier.isBambooPlant(state)) {
            if (type == VegetationType.NONE ||
                    (type == VegetationType.TROPICAL && !AutumnConfig.runtime().autumnalTropics())) {
                return quads;
            }
            return tintBambooQuads(quads);
        }

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
                out.add(withTintIndex(quad, 0));
            }
        }
        return out;
    }

    private static List<BakedQuad> tintBambooQuads(List<BakedQuad> quads) {
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            String spritePath = quad.getSprite().contents().name().getPath().toLowerCase(Locale.ROOT);
            int tintIndex = spritePath.contains("bamboo_stalk") ? 1 : 0;
            out.add(withTintIndex(quad, tintIndex));
        }
        return out;
    }

    private static BakedQuad withTintIndex(BakedQuad quad, int tintIndex) {
        return new BakedQuad(
                quad.getVertices(),
                tintIndex,
                quad.getDirection(),
                quad.getSprite(),
                quad.isShade(),
                quad.getLightEmission()
        );
    }
}
