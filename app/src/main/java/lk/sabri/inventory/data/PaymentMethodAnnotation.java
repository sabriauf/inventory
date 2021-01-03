package lk.sabri.inventory.data;

import androidx.annotation.StringDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PaymentMethodAnnotation implements Serializable {

    public static final String CASH = "Cash";
    public static final String CREDIT = "Credit";
    public static final String CHEQUE = "Cheque";

    @StringDef({CASH, CREDIT, CHEQUE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Method {}
}
