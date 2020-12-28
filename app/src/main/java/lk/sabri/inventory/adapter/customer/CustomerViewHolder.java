package lk.sabri.inventory.adapter.customer;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import lk.sabri.inventory.R;

public class CustomerViewHolder extends RecyclerView.ViewHolder {

    TextView txtName, txtAddress, btnAdd;

    public CustomerViewHolder(@NonNull View itemView) {
        super(itemView);
        btnAdd = itemView.findViewById(R.id.txt_customer_add);
        txtName = itemView.findViewById(R.id.text_customer_name);
        txtAddress = itemView.findViewById(R.id.text_customer_address);
    }
}
