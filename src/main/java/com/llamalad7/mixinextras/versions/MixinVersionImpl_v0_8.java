package com.llamalad7.mixinextras.versions;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

@SuppressWarnings("unused")
public class MixinVersionImpl_v0_8 extends MixinVersion {
    @Override
    public RuntimeException makeInvalidInjectionException(InjectionInfo info, String message) {
        return new InvalidInjectionException(info, message);
    }

    @Override
    public IMixinContext getMixin(InjectionInfo info) {
        return info.getMixin();
    }

    @Override
    public Context makeLvtContext(InjectionInfo info, Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
        return new Context(info, returnType, argsOnly, target, node);
    }

    @Override
    public void preInject(InjectionInfo info) {
        throw new AssertionError("Cannot preInject until 0.8.3");
    }

    @Override
    public AnnotationNode getAnnotation(InjectionInfo info) {
        return info.getAnnotationNode();
    }
}
