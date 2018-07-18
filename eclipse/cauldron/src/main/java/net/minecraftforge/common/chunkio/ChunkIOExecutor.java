package net.minecraftforge.common.chunkio;

import net.minecraftforge.common.util.AsynchronousExecutor;

public class ChunkIOExecutor {
    static final int BASE_THREADS = 1;
    static final int PLAYERS_PER_THREAD = 50;

    private static final AsynchronousExecutor<QueuedChunk, net.minecraft.world.chunk.Chunk, kcauldron.ChunkCallback, RuntimeException> instance = new AsynchronousExecutor<QueuedChunk, net.minecraft.world.chunk.Chunk, kcauldron.ChunkCallback, RuntimeException>(new ChunkIOProvider(), BASE_THREADS); // KCauldron

    public static net.minecraft.world.chunk.Chunk syncChunkLoad(net.minecraft.world.World world, net.minecraft.world.chunk.storage.AnvilChunkLoader loader, net.minecraft.world.gen.ChunkProviderServer provider, int x, int z) {
        return instance.getSkipQueue(new QueuedChunk(x, z, loader, world, provider));
    }

    public static void queueChunkLoad(net.minecraft.world.World world, net.minecraft.world.chunk.storage.AnvilChunkLoader loader, net.minecraft.world.gen.ChunkProviderServer provider, int x, int z, Runnable runnable) {
        instance.add(new QueuedChunk(x, z, loader, world, provider), new RunnableCallback(runnable)); // KCauldron
    }

    // Abuses the fact that hashCode and equals for QueuedChunk only use world and coords
    public static void dropQueuedChunkLoad(net.minecraft.world.World world, int x, int z, Runnable runnable) {
        instance.drop(new QueuedChunk(x, z, null, world, null), new RunnableCallback(runnable));
    }

    public static void adjustPoolSize(int players) {
        int size = Math.max(BASE_THREADS, (int) Math.ceil((double) players / PLAYERS_PER_THREAD));
        instance.setActiveThreads(size);
    }

    public static void tick() {
        instance.finishActive();
    }
    
    // KCauldron start
    public static void queueChunkLoad(net.minecraft.world.World world,
            net.minecraft.world.chunk.storage.AnvilChunkLoader loader,
            net.minecraft.world.gen.ChunkProviderServer provider, int x, int z, kcauldron.ChunkCallback runnable) {
        instance.add(new QueuedChunk(x, z, loader, world, provider), runnable);
    }

    public static final class RunnableCallback implements kcauldron.ChunkCallback {
        private final Runnable runnable;

        public RunnableCallback(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void onChunkLoaded(net.minecraft.world.chunk.Chunk chunk) {
            runnable.run();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            Runnable runnable = obj instanceof Runnable ? (Runnable) obj
                    : obj instanceof RunnableCallback ? ((RunnableCallback) obj).runnable : null;
            return this.runnable == runnable;
        }
    }
    // KCauldron end
}