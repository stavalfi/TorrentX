package main;

import java.nio.ByteBuffer;

/**
 * save positive number in bytes and convert them back to numbers.
 */
public class NaturalX {

    // LITTLE_INDIAN
    private ByteBuffer bytes;

    public NaturalX(int bytesLength, long value) {
        assert (value >> (bytesLength) * 8) == 0;
        this.bytes = ByteBuffer.allocate(bytesLength); // each of its elements will be initialized to zero
        for (int i = 0; i < bytesLength; i++) {
            this.bytes.put((byte) ((value >> i * 8) | 0x00));
        }
    }

    public NaturalX(byte[] bytes) {
        this.bytes = ByteBuffer.wrap(bytes);
    }

    public ByteBuffer buffer() {
        return this.bytes.asReadOnlyBuffer();
    }

    public long getNumber() {
        long value = 0;
        for (int i = 0; i < this.bytes.capacity(); i++) {
            value += ((long) this.bytes.get(i) & 0xffL) << (8 * i);
        }
        return value;
    }

    public static class Natural1 extends NaturalX {
        public Natural1(long value) {
            super(1, value);
        }

        public Natural1(byte[] bytes) {
            super(bytes);
            assert bytes.length==1;
        }
    }

    public static class Natural2 extends NaturalX {
        public Natural2(long value) {
            super(2, value);
        }

        public Natural2(byte[] bytes) {
            super(bytes);
            assert bytes.length==2;
        }
    }

    public static class Natural4 extends NaturalX {
        public Natural4(long value) {
            super(4, value);
        }

        public Natural4(byte[] bytes) {
            super(bytes);
            assert bytes.length==4;
        }
    }

    public static class Natural5 extends NaturalX {
        public Natural5(long value) {
            super(5, value);
        }

        public Natural5(byte[] bytes) {
            super(bytes);
            assert bytes.length==5;
        }
    }

    public static class Natural6 extends NaturalX {
        public Natural6(long value) {
            super(6, value);
        }

        public Natural6(byte[] bytes) {
            super(bytes);
            assert bytes.length==6;
        }
    }

    public static class Natural7 extends NaturalX {
        public Natural7(long value) {
            super(7, value);
        }

        public Natural7(byte[] bytes) {
            super(bytes);
            assert bytes.length==7;
        }
    }
}
