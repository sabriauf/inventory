package lk.sabri.inventory.adapter.customer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.sabri.inventory.R;
import lk.sabri.inventory.adapter.OnItemClickListener;
import lk.sabri.inventory.data.Customer;

public class CustomerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private Context context;
    private List<Customer> customers;
    private List<Customer> customersFull;
    private OnItemClickListener listener;

    public CustomerAdapter(Context context, List<Customer> invoiceList) {
        this.context = context;
        this.customers = invoiceList;
//        this.customers.add(null);
        this.customersFull = new ArrayList<>(invoiceList);
    }

    public void onItemAdd(Customer customer) {
        customers.add(0, customer);
        this.customersFull.add(0, customer);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.component_customer_row, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CustomerViewHolder) {
            CustomerViewHolder viewHolder = (CustomerViewHolder) holder;
            final Customer customer = customers.get(position);

            if (customer != null) {
                viewHolder.txtName.setVisibility(View.VISIBLE);
                viewHolder.txtAddress.setVisibility(View.VISIBLE);
                viewHolder.btnAdd.setVisibility(View.GONE);

                viewHolder.txtName.setText(customer.getCustName());
                viewHolder.txtAddress.setText(customer.getAddress());
            } else {
                viewHolder.txtName.setVisibility(View.GONE);
                viewHolder.txtAddress.setVisibility(View.GONE);
                viewHolder.btnAdd.setVisibility(View.VISIBLE);
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null)
                        listener.onItemClick(customer);
                    if(customers.get(customers.size() -1) == null)
                        customers.remove(customers.size() -1);
                    notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (customers == null)
            return 0;
        else
            return customers.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public Filter getFilter() {
        return customerFilter;
    }

    private Filter customerFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Customer> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(customersFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Customer item : customersFull) {
                    if (item != null && item.getCustName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            customers.clear();
            if (results.values != null)
                customers.addAll((List) results.values);
            if (results.values == null || ((List) results.values).isEmpty())
                customers.add(null);
            notifyDataSetChanged();
        }
    };
}