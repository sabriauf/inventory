package lk.sabri.inventory.adapter.item;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import lk.sabri.inventory.R;

public class InvoiceItemViewHolder extends RecyclerView.ViewHolder {

    TextView txtName, txtPrice, txtUnit;
    Button btnAddItem;

    public InvoiceItemViewHolder(@NonNull View itemView) {
        super(itemView);
        txtName = itemView.findViewById(R.id.text_invoice_item_name);
        txtPrice = itemView.findViewById(R.id.text_invoice_item_price);
        txtUnit = itemView.findViewById(R.id.text_invoice_item_unit);
        btnAddItem = itemView.findViewById(R.id.btn_invoice_add_item);
    }
}
