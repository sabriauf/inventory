package lk.sabri.inventory.adapter.item;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import lk.sabri.inventory.R;

public class InvoiceFooterViewHolder {

    public class InvoiceItemAddViewHolder extends RecyclerView.ViewHolder {
        Button btnAddItem;

        public InvoiceItemAddViewHolder(@NonNull View itemView) {
            super(itemView);
            btnAddItem = itemView.findViewById(R.id.btn_invoice_add_item);
        }
    }

    public class InvoiceTotalViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle;
        TextView txtValue;

        public InvoiceTotalViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.text_invoice_add_item_total);
            txtValue = itemView.findViewById(R.id.text_invoice_add_item_value);
        }
    }

    public class InvoiceBalanceViewHolder extends RecyclerView.ViewHolder {

        TextView txtValue;

        public InvoiceBalanceViewHolder(@NonNull View itemView) {
            super(itemView);
            txtValue = itemView.findViewById(R.id.text_invoice_add_item_balance);
        }
    }

    public class InvoicePaymentViewHolder extends RecyclerView.ViewHolder {

        Button btnPayment;

        public InvoicePaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            btnPayment = itemView.findViewById(R.id.btn_invoice_payment);
        }
    }
}
