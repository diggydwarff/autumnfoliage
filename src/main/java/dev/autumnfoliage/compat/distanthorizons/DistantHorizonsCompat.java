package dev.autumnfoliage.compat.distanthorizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import dev.autumnfoliage.client.AutumnColorizer;
import dev.autumnfoliage.client.VegetationClassifier;
import dev.autumnfoliage.client.VegetationType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicBoolean;

/** Optional Distant Horizons API integration. */
public final class DistantHorizonsCompat {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private DistantHorizonsCompat() {}

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        // DH only fires its high-frequency color event for wrappers explicitly opted in here.
        // Restrict that to vegetation so normal terrain does not pay the event cost.
        DhApiEventRegister.on(
                DhApiBlockStateWrapperCreatedEvent.class,
                new DhApiBlockStateWrapperCreatedEvent() {
                    @Override
                    public void blockStateWrapperCreated(
                            DhApiEventParam<DhApiBlockStateWrapperCreatedEvent.EventParam> event
                    ) {
                        if (event == null || event.value == null) {
                            return;
                        }
                        IDhApiBlockStateWrapper wrapper = event.value.getBlockStateWrapper();
                        BlockState state = unwrapBlockState(wrapper);
                        if (state != null && VegetationClassifier.classify(state) != VegetationType.NONE) {
                            event.value.setAllowApiColorOverride(true);
                        }
                    }
                }
        );

        DhApiEventRegister.on(
                DhApiBlockColorOverrideEvent.class,
                new DhApiBlockColorOverrideEvent() {
                    @Override
                    public void onBlockColorOverridden(
                            DhApiEventParam<DhApiBlockColorOverrideEvent.EventParam> event
                    ) {
                        if (event == null || event.value == null) {
                            return;
                        }

                        DhApiBlockColorOverrideEvent.EventParam value = event.value;
                        BlockState state = unwrapBlockState(value.getBlockStateWrapper());
                        if (state == null) {
                            return;
                        }

                        IDhApiBiomeWrapper biome = value.getBiomeWrapper();
                        String biomeSerial = biomeNameCompat(biome);
                        BlockPos pos = new BlockPos(
                                value.getBlockPosX(),
                                value.getBlockPosY(),
                                value.getBlockPosZ()
                        );

                        int originalColor = value.getColorAsInt();
                        int recolored;
                        try {
                            // DH API 7.1+ exposes the untinted texture/base sample. Using it gives
                            // the closest match to Minecraft/Sodium's normal texture * tint path.
                            recolored = AutumnColorizer.colorForDistantHorizons(
                                    state,
                                    pos,
                                    originalColor,
                                    value.getBaseColorAsInt(),
                                    biomeSerial
                            );
                        } catch (NoSuchMethodError | AbstractMethodError ignored) {
                            // DH 3.2.x / API 7.0.x does not expose getBaseColorAsInt(). Keep the
                            // integration working with a luminance-preserving approximation rather
                            // than disabling autumn LOD colors entirely.
                            recolored = AutumnColorizer.colorForDistantHorizonsLegacy(
                                    state,
                                    pos,
                                    originalColor,
                                    biomeSerial
                            );
                        }

                        if (recolored != value.getColorAsInt()) {
                            value.setColor(
                                    (recolored >>> 24) & 0xFF,
                                    (recolored >>> 16) & 0xFF,
                                    (recolored >>> 8) & 0xFF,
                                    recolored & 0xFF
                            );
                        }
                    }
                }
        );

        // If DH finishes after Autumn Foliage registered, drop any render buffers produced before
        // the color override became active so the visible LODs immediately rebuild with autumn.
        DhApiEventRegister.on(
                DhApiAfterDhInitEvent.class,
                new DhApiAfterDhInitEvent() {
                    @Override
                    public void afterDistantHorizonsInit(DhApiEventParam<Void> event) {
                        // Keep the cache invalidation on Minecraft's client thread. DH may fire
                        // its init event from loader/setup code rather than the render thread.
                        Minecraft.getInstance().execute(DistantHorizonsCompat::refreshRenderData);
                    }
                }
        );
    }

    public static void refreshRenderData() {
        if (DhApi.Delayed.renderProxy != null) {
            DhApi.Delayed.renderProxy.clearRenderDataCache();
        }
    }

    private static String biomeNameCompat(IDhApiBiomeWrapper biome) {
        if (biome == null) {
            return "";
        }
        try {
            // Added by DH API 7.1.0. It normally contains the namespace/path and is the most
            // reliable input for our tropical-biome keyword matcher.
            return biome.getSerialString();
        } catch (NoSuchMethodError | AbstractMethodError ignored) {
            // DH API 7.0.x only exposes getName(). This is still sufficient for names such as
            // jungle/rainforest/tropical and keeps 1.21.4 DH 3.2.x compatible.
            String name = biome.getName();
            return name == null ? "" : name;
        }
    }

    private static BlockState unwrapBlockState(IDhApiBlockStateWrapper wrapper) {
        if (wrapper == null) {
            return null;
        }
        Object wrapped = wrapper.getWrappedMcObject();
        return wrapped instanceof BlockState state ? state : null;
    }
}
