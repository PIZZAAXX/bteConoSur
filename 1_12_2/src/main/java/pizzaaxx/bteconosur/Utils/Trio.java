package pizzaaxx.bteconosur.Utils;

public class Trio<X, Y, Z> {

    private final X x;
    private final Y y;
    private final Z z;

    public Trio(X x, Y y, Z z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public X getX() {
        return x;
    }

    public Y getY() {
        return y;
    }

    public Z getZ() {
        return z;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }

        Trio<?, ?, ?> trio = (Trio<?, ?, ?>) obj;

        return this.x.equals(trio.x) && this.y.equals(trio.y) && this.z.equals(trio.z);
    }
}
