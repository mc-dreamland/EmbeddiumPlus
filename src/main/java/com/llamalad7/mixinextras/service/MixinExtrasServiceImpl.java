package com.llamalad7.mixinextras.service;

import com.llamalad7.mixinextras.injector.*;
import com.llamalad7.mixinextras.injector.v2.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import com.llamalad7.mixinextras.sugar.impl.SugarPostProcessingExtension;
import com.llamalad7.mixinextras.sugar.impl.SugarWrapperInjectionInfo;
import com.llamalad7.mixinextras.transformer.MixinTransformerExtension;
import com.llamalad7.mixinextras.utils.MixinExtrasLogger;
import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.wrapper.factory.FactoryRedirectWrapperInjectionInfo;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class MixinExtrasServiceImpl implements com.llamalad7.mixinextras.service.MixinExtrasService {
    private static final MixinExtrasLogger LOGGER = MixinExtrasLogger.get("Service");

    private final List<com.llamalad7.mixinextras.service.Versioned<String>> offeredPackages = new ArrayList<>();
    private final List<com.llamalad7.mixinextras.service.Versioned<IExtension>> offeredExtensions = new ArrayList<>();
    private final List<com.llamalad7.mixinextras.service.Versioned<Class<? extends InjectionInfo>>> offeredInjectors = new ArrayList<>();
    private final String ownPackage = StringUtils.substringBefore(getClass().getName(), ".service.");
    private final List<com.llamalad7.mixinextras.service.Versioned<String>> allPackages = new ArrayList<>(Collections.singletonList(
            new com.llamalad7.mixinextras.service.Versioned<>(getVersion(), ownPackage)
    ));
    private final List<IExtension> ownExtensions = Arrays.asList(
            new MixinTransformerExtension(), new com.llamalad7.mixinextras.service.ServiceInitializationExtension(this),
            new LateInjectionApplicatorExtension(), new SugarPostProcessingExtension()
    );
    private final List<Class<? extends InjectionInfo>> ownInjectors = Arrays.asList(
            ModifyExpressionValueInjectionInfo.class, ModifyReceiverInjectionInfo.class, ModifyReturnValueInjectionInfo.class,
            WrapOperationInjectionInfo.class, WrapWithConditionV1InjectionInfo.class
    );
    private final List<com.llamalad7.mixinextras.service.Versioned<Class<? extends InjectionInfo>>> ownGatedInjectors = Arrays.asList(
            new com.llamalad7.mixinextras.service.Versioned<>(com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_3_4.getNumber(), WrapWithConditionInjectionInfo.class)
    );
    private final List<Class<? extends InjectionInfo>> internalInjectors = Arrays.asList(
            SugarWrapperInjectionInfo.class, FactoryRedirectWrapperInjectionInfo.class
    );
    private final List<String> registeredInjectors = new ArrayList<>();

    boolean initialized;

    @Override
    public int getVersion() {
        return com.llamalad7.mixinextras.service.MixinExtrasVersion.LATEST.getNumber();
    }

    @Override
    public boolean shouldReplace(Object otherService) {
        return getVersion() > com.llamalad7.mixinextras.service.MixinExtrasService.getFrom(otherService).getVersion();
    }

    @Override
    public void takeControlFrom(Object olderService) {
        LOGGER.debug("{} is taking over from {}", this, olderService);
        ownExtensions.forEach(it -> {
            // Hack to "support" old betas.
            // Our new applicator *must* be present in the list before any old ones, which this ensures.
            // We can then hide the sugar from them, so they remain inactive.
            // We prioritise the initialization extension so it's definitely before the sugar one.
            MixinInternals.registerExtension(it, it instanceof com.llamalad7.mixinextras.service.ServiceInitializationExtension || it instanceof MixinTransformerExtension);
        });
        ownInjectors.forEach(it -> registerInjector(it, ownPackage));
        ownGatedInjectors.forEach(it -> registerInjector(it.value, ownPackage));
    }

    @Override
    public void concedeTo(Object newerService, boolean wasActive) {
        requireNotInitialized();
        LOGGER.debug("{} is conceding to {}", this, newerService);
        com.llamalad7.mixinextras.service.MixinExtrasService newService = MixinExtrasService.getFrom(newerService);

        if (wasActive) {
            deInitialize();
        }

        offeredPackages.forEach(packageName -> newService.offerPackage(packageName.version, packageName.value));
        newService.offerPackage(getVersion(), ownPackage);

        offeredExtensions.forEach(extension -> newService.offerExtension(extension.version, extension.value));
        ownExtensions.forEach(extension -> newService.offerExtension(getVersion(), extension));

        offeredInjectors.forEach(injector -> newService.offerInjector(injector.version, injector.value));
        ownInjectors.forEach(injector -> newService.offerInjector(getVersion(), injector));
    }

    @Override
    public void offerPackage(int version, String packageName) {
        requireNotInitialized();
        offeredPackages.add(new com.llamalad7.mixinextras.service.Versioned<>(version, packageName));
        allPackages.add(new com.llamalad7.mixinextras.service.Versioned<>(version, packageName));
        ownInjectors.forEach(it -> registerInjector(it, packageName));
        for (com.llamalad7.mixinextras.service.Versioned<Class<? extends InjectionInfo>> gatedInjector : ownGatedInjectors) {
            if (version >= gatedInjector.version) {
                registerInjector(gatedInjector.value, packageName);
            }
        }
    }

    @Override
    public void offerExtension(int version, IExtension extension) {
        requireNotInitialized();
        offeredExtensions.add(new com.llamalad7.mixinextras.service.Versioned<>(version, extension));
    }

    @Override
    public void offerInjector(int version, Class<? extends InjectionInfo> injector) {
        requireNotInitialized();
        offeredInjectors.add(new com.llamalad7.mixinextras.service.Versioned<>(version, injector));
    }

    @Override
    public String toString() {
        return String.format(
                "%s(version=%s)",
                getClass().getName(), com.llamalad7.mixinextras.service.MixinExtrasVersion.LATEST
        );
    }

    @Override
    public void initialize() {
        requireNotInitialized();
        LOGGER.info("Initializing MixinExtras via {}.", this);
        detectBetaPackages();
        internalInjectors.forEach(InjectionInfo::register);
        initialized = true;
    }

    private void deInitialize() {
        for (IExtension extension : ownExtensions) {
            MixinInternals.unregisterExtension(extension);
        }
        registeredInjectors.forEach(MixinInternals::unregisterInjector);
    }

    private void registerInjector(Class<? extends InjectionInfo> injector, String packageName) {
        String name = injector.getAnnotation(InjectionInfo.AnnotationType.class).value().getName();
        String suffix = StringUtils.removeStart(name, ownPackage);
        registeredInjectors.add(packageName + suffix);
        MixinInternals.registerInjector(packageName + suffix, injector);
    }

    public Type changePackage(Class<?> ourType, Type theirReference, Class<?> ourReference) {
        String suffix = StringUtils.substringAfter(ourReference.getName(), ownPackage);
        String theirPackage = StringUtils.substringBefore(theirReference.getClassName(), suffix);
        return Type.getObjectType((theirPackage + StringUtils.substringAfter(ourType.getName(), ownPackage)).replace('.', '/'));
    }

    public Set<String> getAllClassNames(String ourName) {
        return getAllClassNamesAtLeast(ourName, Integer.MIN_VALUE);
    }

    public Set<String> getAllClassNamesAtLeast(String ourName, com.llamalad7.mixinextras.service.MixinExtrasVersion minVersion) {
        return getAllClassNamesAtLeast(ourName, minVersion.getNumber());
    }

    private Set<String> getAllClassNamesAtLeast(String ourName, int minVersion) {
        String ourBinaryName = ourName.replace('/', '.');
        return allPackages.stream()
                .filter(it -> it.version >= minVersion)
                .map(it -> it.value)
                .map(it -> StringUtils.replaceOnce(ourBinaryName, ownPackage, it))
                .collect(Collectors.toSet());
    }

    public boolean isClassOwned(String name) {
        return allPackages.stream().map(it -> it.value).anyMatch(name::startsWith);
    }

    private void requireNotInitialized() {
        if (initialized) {
            throw new IllegalStateException("The MixinExtras service has already been selected and is initialized!");
        }
    }

    /**
     * Detects and recognises active packages from versions between 0.2.0-beta.1 and 0.2.0-beta.9
     * We take over the handling of the sugar for these versions, but not the injectors.
     */
    private void detectBetaPackages() {
        for (IExtension extension : MixinInternals.getExtensions().getActiveExtensions()) {
            String name = extension.getClass().getName();
            String suffix = ".sugar.impl.SugarApplicatorExtension";
            if (name.endsWith(suffix) && !isClassOwned(name)) {
                // We have to assume this is from one of the offending versions.
                String packageName = StringUtils.removeEnd(name, suffix);
                com.llamalad7.mixinextras.service.MixinExtrasVersion version = getBetaVersion(packageName);
                allPackages.add(new com.llamalad7.mixinextras.service.Versioned<>(version.getNumber(), packageName));
                LOGGER.warn("Found problematic active MixinExtras instance at {} (version {})", packageName, version);
                LOGGER.warn("Versions from 0.2.0-beta.1 to 0.2.0-beta.9 have limited support and it is strongly recommended to update.");
            }
        }
    }

    private com.llamalad7.mixinextras.service.MixinExtrasVersion getBetaVersion(String packageName) {
        String bootstrapClassName = packageName + ".MixinExtrasBootstrap";
        try {
            Class<?> bootstrapClass = Class.forName(bootstrapClassName);
            Field versionField = bootstrapClass.getDeclaredField("VERSION");
            versionField.setAccessible(true);
            String versionName = (String) versionField.get(null);
            switch (versionName) {
                case "0.2.0-beta.1": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_1;
                case "0.2.0-beta.2": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_2;
                case "0.2.0-beta.3": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_3;
                case "0.2.0-beta.4": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_4;
                case "0.2.0-beta.5": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_5;
                case "0.2.0-beta.6": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_6;
                case "0.2.0-beta.7": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_7;
                case "0.2.0-beta.8": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_8;
                case "0.2.0-beta.9": return com.llamalad7.mixinextras.service.MixinExtrasVersion.V0_2_0_BETA_9;
            }
            throw new IllegalArgumentException("Unrecognized version " + versionName);
        } catch (Exception e) {
            LOGGER.error(
                    String.format("Failed to determine version of MixinExtras instance at %s, assuming 0.2.0-beta.1", packageName),
                    e
            );
            return MixinExtrasVersion.V0_2_0_BETA_1;
        }
    }
}
