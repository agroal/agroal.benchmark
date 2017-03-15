// Copyright (C) 2017 Red Hat, Inc. and individual contributors as indicated by the @author tags.
// You may not use this file except in compliance with the Apache License, Version 2.0.

package io.agroal.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import sun.misc.Contended;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
@Warmup( iterations = 3 )
@Measurement( iterations = 8 )
@BenchmarkMode( Mode.Throughput )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@State( Scope.Benchmark )
public class ConnectionBenchmark {

    private static final Random RANDOM = new Random();

    private static DataSource DS;

    @Param( {"agroal", "hikari"} )
    public String poolType;

    @Param( {"32"} )
    public int maxPoolSize;

    @Param( {"jdbc:stub"} )
    public String jdbcUrl;

    @Benchmark
    @CompilerControl( CompilerControl.Mode.INLINE )
    public static Connection cycleConnection(ThreadState state) throws SQLException {
        Connection connection = DS.getConnection();

        // Do some work
        //doWork( false, state.random.nextInt() );

        // Yeld!
        //doYeld( false );

        // Wait some time (5ms average)
        //doSleep( false, state.random.nextInt( 2 ) );

        // Do some work
        //doWork( false, state.random.nextInt( 1000 * 1 ) );

        connection.close();
        return connection;
    }

    public static void doWork(boolean b, long amount) {
        if ( b ) {
            Blackhole.consumeCPU( amount );
        }
    }

    public static void doYeld(boolean b) {
        if ( b ) {
            Thread.yield();
        }
    }

    public static void doSleep(boolean b, long amount) {
        if ( b ) {
            try {
                Thread.sleep( amount );
            } catch ( InterruptedException ignore ) {
            }
        }
    }

    // --- //

    @Setup( Level.Trial )
    public void setup(BenchmarkParams params) {
    }

    @TearDown( Level.Trial )
    public void teardown() throws SQLException {
    }

    @State( Scope.Thread )
    public static class ThreadState {

        @Contended
        private volatile Random random;

        @Setup
        public void setupContext(ConnectionBenchmark state) throws Throwable {
            random = new Random( ConnectionBenchmark.RANDOM.nextLong() );
        }
    }
}
