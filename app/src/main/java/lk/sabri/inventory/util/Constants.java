package lk.sabri.inventory.util;

import lk.sabri.inventory.data.PaymentMethodAnnotation;

public class Constants {

    public static final String SHARED_PREFERENCE_NAME = "inventory_app_prefs";

    public static final String PREFERENCE_LAST_SYNC = "pref_last_sync";
    public static final String PREFERENCE_LAST_USER = "pref_last_user";
    public static final String PREFERENCE_BLE_ADDRESS = "pref_ble_address";
    public static final String PREFERENCE_DEVICE_NAME = "pref_device_name";

    public static final String EXTRA_INVOICE_INFO = "extra_invoice_extra";

    public static final double BOTTOM_SHEET_HEIGHT = 0.5;

    public static final String[] PAYMENT_TYPES = {PaymentMethodAnnotation.CREDIT, PaymentMethodAnnotation.CHEQUE, PaymentMethodAnnotation.CASH};
}
