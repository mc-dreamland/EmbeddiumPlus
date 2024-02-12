package me.srrapero720.embeddiumplus.foundation.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32C;

import javax.annotation.Nullable;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class TimerQuery {
    private int nextQueryName;

    public TimerQuery() {
    }

    public static Optional<TimerQuery> getInstance() {
        return TimerQuery.TimerQueryLazyLoader.INSTANCE;
    }

    public void beginProfile() {
        RenderSystem.assertOnRenderThread();
        if (this.nextQueryName != 0) {
            throw new IllegalStateException("Current profile not ended");
        } else {
            this.nextQueryName = GL32C.glGenQueries();
            GL32C.glBeginQuery(35007, this.nextQueryName);
        }
    }

    public FrameProfile endProfile() {
        RenderSystem.assertOnRenderThread();
        if (this.nextQueryName == 0) {
            throw new IllegalStateException("endProfile called before beginProfile");
        } else {
            GL32C.glEndQuery(35007);
            FrameProfile $$0 = new FrameProfile(this.nextQueryName);
            this.nextQueryName = 0;
            return $$0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class TimerQueryLazyLoader {
        static final Optional<TimerQuery> INSTANCE = Optional.ofNullable(instantiate());

        private TimerQueryLazyLoader() {
        }

        @Nullable
        private static TimerQuery instantiate() {
            return !GL.getCapabilities().GL_ARB_timer_query ? null : new TimerQuery();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class FrameProfile {
        private static final long NO_RESULT = 0L;
        private static final long CANCELLED_RESULT = -1L;
        private final int queryName;
        private long result;

        FrameProfile(int pQueryName) {
            this.queryName = pQueryName;
        }

        public void cancel() {
            RenderSystem.assertOnRenderThread();
            if (this.result == 0L) {
                this.result = -1L;
                GL32C.glDeleteQueries(this.queryName);
            }
        }

        public boolean isDone() {
            RenderSystem.assertOnRenderThread();
            if (this.result != 0L) {
                return true;
            } else if (1 == GL32C.glGetQueryObjecti(this.queryName, 34919)) {
                this.result = ARBTimerQuery.glGetQueryObjecti64(this.queryName, 34918);
                GL32C.glDeleteQueries(this.queryName);
                return true;
            } else {
                return false;
            }
        }

        public long get() {
            RenderSystem.assertOnRenderThread();
            if (this.result == 0L) {
                this.result = ARBTimerQuery.glGetQueryObjecti64(this.queryName, 34918);
                GL32C.glDeleteQueries(this.queryName);
            }

            return this.result;
        }
    }
}