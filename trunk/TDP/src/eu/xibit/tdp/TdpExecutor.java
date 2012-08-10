/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *
 * @author dipacs
 */
final class TdpExecutor {
    
    private static final Executor executor = Executors.newFixedThreadPool(32);
    private static final Executor dataExecutor = Executors.newFixedThreadPool(1);
    
    public static void execute(Runnable task) {
        executor.execute(task);
    }
    
    public static void executeData(Runnable task) {
        dataExecutor.execute(task);
    }

    public TdpExecutor() {
    }
    
}
