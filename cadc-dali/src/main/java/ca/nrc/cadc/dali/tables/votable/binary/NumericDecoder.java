/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 2022.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.tables.votable.binary;

import java.io.DataInput;
import java.io.IOException;
import java.util.StringTokenizer;

abstract class NumericDecoder extends Decoder {

    private final int size1;

    /**
     * Does required setup for a NumericDecoder.
     *
     * @param arraysize the dimensions of objects with this type -
     *                  the last element of the array may be negative to
     *                  indicate unknown slowest-varying dimension
     * @param clazz     class to which all return values of <code>decode*</code>
     *                  methods will belong
     * @param size1     number of bytes that a call to
     *                  <code>decodeStream1</code> will read
     */
    NumericDecoder(Class<?> clazz, long[] arraysize, int size1) {
        super(clazz, arraysize);
        this.size1 = size1;
    }

    /**
     * Returns an empty array of the given size, suitable for storing
     * elements decoded by this object.
     *
     * @param size number of elements in array
     * @return the array ready to store things in
     */
    abstract Object getEmptyArray(int size);

    /**
     * Sets one element of a storage array bad.
     *
     * @param array array returned by a previous call to getEmptyArray
     * @param index the element of this array to set bad
     */
    abstract void setBad1(Object array, int index);

    /**
     * Reads a single value from a string and places it in a storage array.
     *
     * @param array array returned by a previous call to getEmptyArray
     * @param index the element of this array to store the result in
     * @param txt   the string to read from
     * @throws IllegalArgumentException if the string did not contain a
     *                                  valid value for this Decoder
     */
    abstract void decodeString1(Object array, int index, String txt);

    /**
     * Reads a single value from a stream and places it in a storage array.
     *
     * @param array array returned by a previous call to getEmptyArray
     * @param index the element of this array to store the result in
     * @param strm  the stream to read from
     * @throws IOException if there was a read error
     */
    abstract void decodeStream1(Object array, int index, DataInput strm)
            throws IOException;

    /**
     * Turns an array representing the data decoded by this object into
     * the form it will be seen by the outside world as.  The default
     * is a no-op, but subclasses may override this to provide different
     * packaging behaviour.
     *
     * @param array the raw value read
     * @return the value to be returned by the outside world by the
     * <code>decode*</code> methods
     */
    Object packageArray(Object array) {
        return array;
    }

    public Object decodeString(String txt) {
        StringTokenizer st = new StringTokenizer(txt);
        int ntok = st.countTokens();
        int ncell = numCells(ntok);
        Object result = getEmptyArray(ncell);
        for (int i = 0; i < ncell; i++) {
            if (i < ntok) {
                String tok = st.nextToken().trim();
                if (!tok.equals(blankString)) {
                    try {
                        decodeString1(result, i, tok);
                    } catch (IllegalArgumentException e) {
                        setBad1(result, i);
                    }
                } else {
                    setBad1(result, i);
                }
            } else {
                setBad1(result, i);
            }
        }
        return packageArray(result);
    }

    public Object decodeStream(DataInput strm) throws IOException {
        int num = getNumItems(strm);
        Object result = getEmptyArray(num);
        for (int i = 0; i < num; i++) {
            decodeStream1(result, i, strm);
        }
        return packageArray(result);
    }

    public void skipStream(DataInput strm) throws IOException {
        int num = getNumItems(strm);
        skipBytes(strm, (long) num * size1);
    }

    /**
     * Decodes a string as a single scalar.  This method is used by the
     * scalar decoders, and would be handled using multiple inheritance
     * if it existed in java.
     *
     * @param txt text string representing a single value
     * @return decoded value of <code>txt</code>
     */
    Object scalarDecodeString(String txt) {
        if (txt == null || txt.length() == 0) {
            return null;
        } else {
            Object array = getEmptyArray(1);
            try {
                decodeString1(array, 0, txt.trim());
            } catch (IllegalArgumentException e) {
                return null;
            }
            return packageArray(array);
        }
    }

    /**
     * Interprets a string as a <code>short</code>.
     *
     * @param txt text string
     * @return numeric value of <code>txt</code>
     * @throws NumberFormatException if the parse fails
     */
    static short parseShort(String txt) {
        int pos = 0;
        int leng = txt.length();
        while (pos < leng && txt.charAt(pos) == ' ') {
            pos++;
        }
        if (leng - pos > 1) {
            if (txt.charAt(pos + 1) == 'x' && txt.charAt(pos) == '0') {
                return Short.parseShort(txt.substring(pos + 2), 16);
            }
        }
        return Short.parseShort(txt);
    }

    /**
     * Interprets a string as an <code>int</code>.
     *
     * @param txt text string
     * @return numeric value of <code>txt</code>
     * @throws NumberFormatException if the parse fails
     */
    static int parseInt(String txt) {
        int pos = 0;
        int leng = txt.length();
        while (pos < leng && txt.charAt(pos) == ' ') {
            pos++;
        }
        if (leng - pos > 1) {
            if (txt.charAt(pos + 1) == 'x' && txt.charAt(pos) == '0') {
                return Integer.parseInt(txt.substring(pos + 2), 16);
            }
        }
        return Integer.parseInt(txt);
    }

    /**
     * Interprets a string as a <code>long</code>.
     *
     * @param txt text string
     * @return numeric value of <code>txt</code>
     * @throws NumberFormatException if the parse fails
     */
    static long parseLong(String txt) {
        int pos = 0;
        int leng = txt.length();
        while (pos < leng && txt.charAt(pos) == ' ') {
            pos++;
        }
        if (leng - pos > 1) {
            if (txt.charAt(pos + 1) == 'x' && txt.charAt(pos) == '0') {
                return Long.parseLong(txt.substring(pos + 2), 16);
            }
        }
        return Long.parseLong(txt);
    }

    /**
     * Interprets a string as a <code>float</code>.
     *
     * @param txt text string
     * @return numeric value of <code>txt</code>
     * @throws NumberFormatException if the parse fails
     */
    static float parseFloat(String txt) {
        int pos = 0;
        int leng = txt.length();
        while (pos < leng && txt.charAt(pos) == ' ') {
            pos++;
        }
        if (leng - pos == 0) {
            return Float.NaN;
        } else if (leng - pos > 1) {
            if (txt.charAt(pos + 1) == 'I' && txt.indexOf("Inf") == 1) {
                return txt.charAt(pos) == '-' ? Float.NEGATIVE_INFINITY
                                              : Float.POSITIVE_INFINITY;
            }
        }
        return Float.parseFloat(txt);
    }

    /**
     * Interprets a string as a <code>double</code>.
     *
     * @param txt text string
     * @return numeric value of <code>txt</code>
     * @throws NumberFormatException if the parse fails
     */
    static double parseDouble(String txt) {
        int pos = 0;
        int leng = txt.length();
        while (pos < leng && txt.charAt(pos) == ' ') {
            pos++;
        }
        if (leng - pos == 0) {
            return Double.NaN;
        } else if (leng - pos > 1) {
            if (txt.charAt(pos + 1) == 'I' && txt.indexOf("Inf") == 1) {
                return txt.charAt(pos) == '-' ? Double.NEGATIVE_INFINITY
                                              : Double.POSITIVE_INFINITY;
            }
        }
        return Double.parseDouble(txt);
    }

    static class ShortDecoder extends NumericDecoder {
        private short bad;
        private boolean hasBad = false;

        ShortDecoder(long[] arraysize) {
            this(short[].class, arraysize, 2);
        }

        ShortDecoder(Class<?> clazz, long[] arraysize, int size1) {
            super(clazz, arraysize, size1);
        }

        void setNullValue(String txt) {
            bad = parseShort(txt);
            hasBad = true;
        }

        Object getEmptyArray(int size) {
            return new short[size];
        }

        void decodeString1(Object array, int index, String txt) {
            ((short[]) array)[index] = parseShort(txt);
        }

        void decodeStream1(Object array, int index, DataInput strm)
                throws IOException {
            ((short[]) array)[index] = strm.readShort();
        }

        void setBad1(Object array, int index) {
            ((short[]) array)[index] = bad;
        }

        public boolean isNull(Object array, int index) {
            return hasBad && ((short[]) array)[index] == bad;
        }
    }

    static class ScalarShortDecoder extends NumericDecoder.ShortDecoder {
        ScalarShortDecoder() {
            super(Short.class, SCALAR_SIZE, 2);
        }

        Object packageArray(Object array) {
            short[] arr = (short[]) array;
            return isNull(arr, 0) ? null : arr[0];
        }

        public Object decodeString(String txt) {
            return scalarDecodeString(txt);
        }
    }

    static class UnsignedByteDecoder extends NumericDecoder.ShortDecoder {
        UnsignedByteDecoder(Class<?> clazz, long[] arraysize) {
            super(clazz, arraysize, 1);
        }

        UnsignedByteDecoder(long[] arraysize) {
            this(short[].class, arraysize);
        }

        void decodeStream1(Object array, int index, DataInput strm)
                throws IOException {
            ((short[]) array)[index] =
                    (short) ((short) 0x00ff & (short) strm.readByte());
        }
    }

    static class ScalarUnsignedByteDecoder extends NumericDecoder.UnsignedByteDecoder {
        ScalarUnsignedByteDecoder() {
            super(Short.class, SCALAR_SIZE);
        }

        Object packageArray(Object array) {
            short[] arr = (short[]) array;
            return isNull(arr, 0) ? null : arr[0];
        }

        public Object decodeString(String txt) {
            return scalarDecodeString(txt);
        }
    }

    static class IntDecoder extends NumericDecoder {
        private int bad;
        private boolean hasBad = false;

        IntDecoder(Class<?> clazz, long[] arraysize) {
            super(clazz, arraysize, 4);
        }

        IntDecoder(long[] arraysize) {
            this(int[].class, arraysize);
        }

        void setNullValue(String txt) {
            bad = parseInt(txt);
            hasBad = true;
        }

        Object getEmptyArray(int size) {
            return new int[size];
        }

        void decodeString1(Object array, int index, String txt) {
            ((int[]) array)[index] = parseInt(txt);
        }

        void decodeStream1(Object array, int index, DataInput strm)
                throws IOException {
            ((int[]) array)[index] = strm.readInt();
        }

        void setBad1(Object array, int index) {
            ((int[]) array)[index] = bad;
        }

        public boolean isNull(Object array, int index) {
            return hasBad && ((int[]) array)[index] == bad;
        }
    }

    static class ScalarIntDecoder extends NumericDecoder.IntDecoder {
        ScalarIntDecoder() {
            super(Integer.class, SCALAR_SIZE);
        }

        Object packageArray(Object array) {
            int[] arr = (int[]) array;
            return isNull(arr, 0) ? null : arr[0];
        }

        public Object decodeString(String txt) {
            return scalarDecodeString(txt);
        }
    }

    static class LongDecoder extends NumericDecoder {
        private long bad;
        private boolean hasBad = false;

        LongDecoder(Class<?> clazz, long[] arraysize) {
            super(clazz, arraysize, 8);
        }

        LongDecoder(long[] arraysize) {
            this(long[].class, arraysize);
        }

        void setNullValue(String txt) {
            bad = parseLong(txt);
            hasBad = true;
        }

        Object getEmptyArray(int size) {
            return new long[size];
        }

        void decodeString1(Object array, int index, String txt) {
            ((long[]) array)[index] = parseLong(txt);
        }

        void decodeStream1(Object array, int index, DataInput strm)
                throws IOException {
            ((long[]) array)[index] = strm.readLong();
        }

        void setBad1(Object array, int index) {
            ((long[]) array)[index] = bad;
        }

        public boolean isNull(Object array, int index) {
            return hasBad && ((long[]) array)[index] == bad;
        }
    }

    static class ScalarLongDecoder extends NumericDecoder.LongDecoder {
        ScalarLongDecoder() {
            super(Long.class, SCALAR_SIZE);
        }

        Object packageArray(Object array) {
            long[] arr = (long[]) array;
            return isNull(arr, 0) ? null : arr[0];
        }

        public Object decodeString(String txt) {
            return scalarDecodeString(txt);
        }
    }

    static class FloatDecoder extends NumericDecoder {
        FloatDecoder(Class<?> clazz, long[] arraysize) {
            super(clazz, arraysize, 4);
        }

        FloatDecoder(long[] arraysize) {
            this(float[].class, arraysize);
        }

        void setNullValue(String txt) {
            // no action
        }

        Object getEmptyArray(int size) {
            return new float[size];
        }

        void decodeString1(Object array, int index, String txt) {
            ((float[]) array)[index] = parseFloat(txt);
        }

        void decodeStream1(Object array, int index, DataInput strm)
                throws IOException {
            ((float[]) array)[index] = strm.readFloat();
        }

        void setBad1(Object array, int index) {
            ((float[]) array)[index] = Float.NaN;
        }

        public boolean isNull(Object array, int index) {
            return Float.isNaN(((float[]) array)[index]);
        }
    }

    static class ScalarFloatDecoder extends NumericDecoder.FloatDecoder {
        ScalarFloatDecoder() {
            super(Float.class, SCALAR_SIZE);
        }

        Object packageArray(Object array) {
            float[] arr = (float[]) array;
            return isNull(arr, 0) ? null : arr[0];
        }

        public Object decodeString(String txt) {
            return scalarDecodeString(txt);
        }
    }

    static class DoubleDecoder extends NumericDecoder {
        DoubleDecoder(Class<?> clazz, long[] arraysize) {
            super(clazz, arraysize, 8);
        }

        DoubleDecoder(long[] arraysize) {
            this(double[].class, arraysize);
        }

        void setNullValue(String txt) {
            // no action
        }

        Object getEmptyArray(int size) {
            return new double[size];
        }

        void decodeString1(Object array, int index, String txt) {
            ((double[]) array)[index] = parseDouble(txt);
        }

        void decodeStream1(Object array, int index, DataInput strm)
                throws IOException {
            ((double[]) array)[index] = strm.readDouble();
        }

        void setBad1(Object array, int index) {
            ((double[]) array)[index] = Double.NaN;
        }

        public boolean isNull(Object array, int index) {
            return Double.isNaN(((double[]) array)[index]);
        }
    }

    static class ScalarDoubleDecoder extends NumericDecoder.DoubleDecoder {
        ScalarDoubleDecoder() {
            super(Double.class, SCALAR_SIZE);
        }

        Object packageArray(Object array) {
            double[] arr = (double[]) array;
            return isNull(arr, 0) ? null : arr[0];
        }

        public Object decodeString(String txt) {
            return scalarDecodeString(txt);
        }
    }
}

