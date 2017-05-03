// Copyright (C) 2017 Red Hat, Inc. and individual contributors as indicated by the @author tags.
// You may not use this file except in compliance with the Apache License, Version 2.0.

package io.agroal.benchmark;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.benchmark.mock.MockDriver;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
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

import static io.agroal.api.configuration.AgroalConnectionPoolConfiguration.PreFillMode.MAX;
import static io.agroal.api.configuration.AgroalDataSourceConfiguration.DataSourceImplementation.AGROAL;
import static io.agroal.api.configuration.AgroalDataSourceConfiguration.DataSourceImplementation.HIKARI;

/**
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
@Warmup( iterations = 4 )
@Measurement( iterations = 10 )
@Fork( value = 5 )
@BenchmarkMode( Mode.Throughput )
@OutputTimeUnit( TimeUnit.MILLISECONDS )
@State( Scope.Benchmark )
public class ConnectionBenchmark {

    private static final Random RANDOM = new Random();

    private static DataSource dataSource;

    @Param( {"agroal", "hikari"} )
    public String poolType;

    @Param( {"50", "20", "8"} )
    public int poolSize;

    @Param( {"jdbc:stub"} )
    public String jdbcUrl;

    @Benchmark
    @CompilerControl( CompilerControl.Mode.INLINE )
    public static Connection cycleConnection(ThreadState state) throws SQLException {
        Connection connection = dataSource.getConnection();

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
    public void setup(BenchmarkParams params) throws SQLException {
        MockDriver.registerMockDriver();

        AgroalDataSourceConfigurationSupplier supplier = new AgroalDataSourceConfigurationSupplier()
                .metricsEnabled( false )
                .connectionPoolConfiguration( cp -> cp
                        .preFillMode( MAX )
                        .maxSize( poolSize )
                        .connectionFactoryConfiguration( cf -> cf
                                .jdbcUrl( jdbcUrl )
                                .driverClassName( MockDriver.class.getName() )
                        )
                );

        switch ( poolType ) {
            case "hikari":
                supplier.dataSourceImplementation( HIKARI );
            case "agroal":
                supplier.dataSourceImplementation( AGROAL );
        }
        dataSource = AgroalDataSource.from( supplier );
    }

    @TearDown( Level.Trial )
    public void teardown() throws SQLException {
        ( (AgroalDataSource) dataSource ).close();

        MockDriver.deregisterMockDriver();
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
