package lk.sabri.inventory.data;

import androidx.annotation.StringDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PaymentMethodAnnotation implements Serializable {

    public static final String CASH = "cash";
    public static final String CREDIT = "credit";
    public static final String CHEQUE = "cheque";

    @StringDef({CASH, CREDIT, CHEQUE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Method {}
}
