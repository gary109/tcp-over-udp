/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author dipacs
 */
final class TdpExecutor {
    
    private final ExecutorService executor = Executors.newFixedThreadPool(32);
    private final ExecutorService dataExecutor = Executors.newFixedThreadPool(1);
    
    public void execute(Runnable task) {
        executor.execute(task);
    }
    
    public void executeData(Runnable task) {
        dataExecutor.execute(task);
    }

    TdpExecutor() {
    }
    
}
