package lk.sabri.inventory.adapter.invoice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import lk.sabri.inventory.R;
import lk.sabri.inventory.adapter.OnItemClickListener;
import lk.sabri.inventory.data.Invoice;
import lk.sabri.inventory.util.TimeFormatter;

public class InvoiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<Invoice> invoiceList;
    private OnItemClickListener listener;

    public InvoiceAdapter(Context context, List<Invoice> invoiceList) {
        this.context = context;
        this.invoiceList = invoiceList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.component_invoice_row, parent, false);
        return new InvoiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof InvoiceViewHolder) {
            InvoiceViewHolder viewHolder = (InvoiceViewHolder) holder;
            final Invoice invoice = invoiceList.get(position);

            viewHolder.txtId.setText(invoice.getInvoiceNo());
            if (invoice.getCustomer() != null)
                viewHolder.txtName.setText(invoice.getCustomer().getCustName());
            else
                viewHolder.txtName.setText(R.string.unknown);
            viewHolder.txtPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", invoice.getTotal()));
            viewHolder.txtDate.setText(TimeFormatter.getFormattedDate("yyyy-MM-dd HH:mm", invoice.getDate()));

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null)
                        listener.onItemClick(invoice);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (invoiceList == null)
            return 0;
        else
            return invoiceList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
