/*
 * Copyright 2011 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vanilla.java.affinity.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.*;
import com.sun.jna.ptr.LongByReference;


/**
 * Implementation of {@link IAffinityImpl} based on JNA call of
 * sched_setaffinity(3)/sched_getaffinity(3) from 'c' library. Applicable for most
 * linux/unix platforms
 *
 * fixme: currently this class not support systme with more then 64 cores
 *
 * @author peter.lawrey
 * @author BegemoT
 */
public enum PosixJNAAffinity implements IAffinityImpl {
    INSTANCE;
    private static final Logger LOGGER = Logger.getLogger( PosixJNAAffinity.class.getName() );

    public static final boolean LOADED;

    private static final String LIBRARY_NAME = "c";

    /** @author BegemoT */
    private interface CLibrary extends Library {
        public static final CLibrary INSTANCE = ( CLibrary )
                Native.loadLibrary( LIBRARY_NAME, CLibrary.class );

        public int sched_setaffinity( final int pid,
                                      final int cpusetsize,
                                      final PointerType cpuset ) throws LastErrorException;

        public int sched_getaffinity( final int pid,
                                      final int cpusetsize,
                                      final PointerType cpuset ) throws LastErrorException;

        public int sched_getcpu() throws LastErrorException;
    }

    static {
        boolean loaded = false;
        try {
            INSTANCE.getAffinity();
            loaded = true;
        } catch ( UnsatisfiedLinkError e ) {
            LOGGER.log( Level.WARNING, "Unable to load jna library", e );
        }
        LOADED = loaded;
    }

    @Override
    public long getAffinity() {
        final CLibrary lib = CLibrary.INSTANCE;
        //fixme: where are systems with more then 64 cores...
        final LongByReference cpuset = new LongByReference( 0L );
        try {
            final int ret = lib.sched_getaffinity( 0, Long.SIZE / 8, cpuset );
            if ( ret < 0 ) {
                throw new IllegalStateException( "sched_getaffinity((" + Long.SIZE / 8 + ") , &(" + cpuset + ") ) return " + ret );
            }
            return cpuset.getValue();
        } catch ( LastErrorException e ) {
            throw new IllegalStateException( "sched_getaffinity((" + Long.SIZE / 8 + ") , &(" + cpuset + ") ) errorNo=" + e.getErrorCode(), e );
        }
    }

    @Override
    public void setAffinity( final long affinity ) {
        final CLibrary lib = CLibrary.INSTANCE;
        try {
            //fixme: where are systems with more then 64 cores...
            final int ret = lib.sched_setaffinity( 0, Long.SIZE / 8, new LongByReference( affinity ) );
            if ( ret < 0 ) {
                throw new IllegalStateException( "sched_setaffinity((" + Long.SIZE / 8 + ") , &(" + affinity + ") ) return " + ret );
            }
        } catch ( LastErrorException e ) {
            throw new IllegalStateException( "sched_getaffinity((" + Long.SIZE / 8 + ") , &(" + affinity + ") ) errorNo=" + e.getErrorCode(), e );
        }
    }
}
