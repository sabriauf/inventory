package lk.sabri.inventory.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import lk.sabri.inventory.R;

public class ItemViewHolder extends RecyclerView.ViewHolder {

    TextView txtName, txtDetail;

    public ItemViewHolder(@NonNull View itemView) {
        super(itemView);
        txtName = itemView.findViewById(R.id.text_item_name);
        txtDetail = itemView.findViewById(R.id.text_item_details);
    }
}
