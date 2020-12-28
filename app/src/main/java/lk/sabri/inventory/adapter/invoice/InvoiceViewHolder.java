package lk.sabri.inventory.adapter.invoice;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import lk.sabri.inventory.R;

public class InvoiceViewHolder extends RecyclerView.ViewHolder {

    TextView txtName, txtPrice, txtDate, txtId;

    public InvoiceViewHolder(@NonNull View itemView) {
        super(itemView);
        txtId = itemView.findViewById(R.id.text_home_invoice_id);
        txtName = itemView.findViewById(R.id.text_home_invoice_name);
        txtPrice = itemView.findViewById(R.id.text_home_invoice_price);
        txtDate = itemView.findViewById(R.id.text_home_invoice_date);
    }
}
