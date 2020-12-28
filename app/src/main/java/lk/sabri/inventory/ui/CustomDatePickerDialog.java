package lk.sabri.inventory.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;

public class CustomDatePickerDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    //instances
    private Context context;
    private OnDateSetListener listener;

    public CustomDatePickerDialog(Context context) {
        this.context = context;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(context, this, year, month, day);
    }

    @Override
    public void onDateSet(android.widget.DatePicker datePicker, int year, int month, int date) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, date);

        if (listener != null)
            listener.onDateSet(c.getTime());
    }

    public void setOnDateSetListener(OnDateSetListener listener) {
        this.listener = listener;
    }

    public interface OnDateSetListener {
        void onDateSet(Date date);
    }
}
