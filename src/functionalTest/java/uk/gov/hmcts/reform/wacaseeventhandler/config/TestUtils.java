package uk.gov.hmcts.reform.wacaseeventhandler.config;

import java.time.LocalDateTime;

public final class TestUtils {

    private TestUtils() {

    }

    //In some situations a timestamp as a String is compared to another one.
    //Some timestamps do not have any trailing zeros but others do, so you get
    //an Assertion error because "54930" milliseconds does not equal "5493" even
    //though from a timestamp persepective these two values are the same

    public static String removeTrailingZeroes(LocalDateTime ts) {
        StringBuilder sb = new StringBuilder(ts.toString());
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '0') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
