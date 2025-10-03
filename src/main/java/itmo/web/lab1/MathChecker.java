package itmo.web.lab1;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathChecker {
    public static boolean hitCheck(BigDecimal x, BigDecimal y, BigDecimal r) {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal halfR = r.divide(new BigDecimal("2"), 12, RoundingMode.HALF_UP);

        boolean inRectangle =
                x.compareTo(r.negate()) >= 0 && x.compareTo(zero) <= 0 &&
                y.compareTo(halfR.negate()) >= 0 && y.compareTo(zero) <= 0;
        
        boolean inTriangle =
                x.compareTo(halfR) <= 0 && x.compareTo(zero) >= 0 &&
                y.compareTo(zero) >= 0 && y.compareTo(r) <= 0 &&
                y.compareTo(x.multiply(new BigDecimal("-2")).add(halfR)) <= 0;

        boolean inCircle =
                x.compareTo(BigDecimal.ZERO) <= 0 &&
                y.compareTo(BigDecimal.ZERO) >= 0 &&
                x.multiply(x).add(y.multiply(y)).compareTo(r.multiply(r)) <= 0;

        return inRectangle || inTriangle || inCircle;
    }
}



