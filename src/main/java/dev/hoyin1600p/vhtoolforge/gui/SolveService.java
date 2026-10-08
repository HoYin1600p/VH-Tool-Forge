package dev.hoyin1600p.vhtoolforge.gui;

import dev.hoyin1600p.vhtoolforge.VHToolForge;
import dev.hoyin1600p.vhtoolforge.data.Store;
import dev.hoyin1600p.vhtoolforge.plan.Planner;
import dev.hoyin1600p.vhtoolforge.solve.Solver;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs the solver off the render thread. Edits mark the plan dirty; each tick picks up a finished solve and,
 * if still dirty, starts the next one. Also keeps the two tool previews that depend on the latest result.
 */
final class SolveService {
    private static final ExecutorService SOLVER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VH Tool Forge solver");
        t.setDaemon(true);
        return t;
    });

    private final Store store;
    private Solver.Result result;
    private CompletableFuture<Solver.Result> pending;
    private boolean dirty = true;
    private Solver.Input pendingInput, solvedInput;
    private ItemStack enteredTool = ItemStack.EMPTY, builtTool = ItemStack.EMPTY;

    SolveService(Store store) {
        this.store = store;
    }

    void markDirty() {
        dirty = true;
    }

    /** True while a solve is running. */
    boolean isPending() {
        return pending != null;
    }

    Solver.Result result() {
        return result;
    }

    /** The input the current result was solved from, or null before the first result. */
    Solver.Input solvedInput() {
        return solvedInput;
    }

    ItemStack enteredTool() {
        return enteredTool;
    }

    ItemStack builtTool() {
        return builtTool;
    }

    void tick() {
        if (pending != null && pending.isDone()) {
            try {
                result = pending.get();
            } catch (InterruptedException | ExecutionException e) {
                result = null;
            }
            pending = null;
            solvedInput = pendingInput;
            rebuildPreviews();
        }
        if (dirty && pending == null) {
            dirty = false;
            Solver.Input in = Planner.input(store);
            pendingInput = in;
            pending = CompletableFuture.supplyAsync(() -> Solver.solve(in), SOLVER);
        }
    }

    private void rebuildPreviews() {
        try {
            enteredTool = ToolStacks.entered(store.settings);
            builtTool = result == null ? ItemStack.EMPTY : ToolStacks.built(store.settings, result.selected);
        } catch (Exception e) {
            VHToolForge.LOG.error("Tool preview failed", e);
        }
    }
}
