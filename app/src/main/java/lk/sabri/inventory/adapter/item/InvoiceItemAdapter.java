package lk.sabri.inventory.adapter.item;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import lk.sabri.inventory.R;
import lk.sabri.inventory.activity.invoice.InvoiceActivity;
import lk.sabri.inventory.adapter.OnItemClickListener;
import lk.sabri.inventory.data.InventoryDatabase;
import lk.sabri.inventory.data.InvoiceItem;
import lk.sabri.inventory.data.Payment;
import lk.sabri.inventory.data.PaymentDAO;
import lk.sabri.inventory.data.PaymentMethodAnnotation;

public class InvoiceItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //constants
    private static final int VIEW_TYPE_ITEM = 1;
    public static final int VIEW_TYPE_ADD = 2;
    public static final int VIEW_TYPE_TOTAL = 3;
    public static final int VIEW_TYPE_BALANCE = 4;
    public static final int VIEW_TYPE_PAYMENT = 5;
    public static final int VIEW_TYPE_PAYMENT_AMOUNT = 6;

    //instances
    private final Context context;
    private final List<InvoiceItem> itemList;
    private OnItemClickListener listener;
    private InvoiceItem mRecentlyDeletedItem;
    private List<Payment> payments = null;

    //primary data
    private int mRecentlyDeletedItemPosition;

    public InvoiceItemAdapter(final Context context, final List<InvoiceItem> itemList) {
        this.context = context;
        this.itemList = itemList;

        addFooterItems();
    }

    private void addFooterItems() {

        int itemsSize = itemList.size();

        if (context instanceof InvoiceActivity) { //Add item add row
            InvoiceItem addItem = new InvoiceItem();
            addItem.setInvoiceId(VIEW_TYPE_ADD);
            itemList.add(addItem);
        }

        InvoiceItem totalItem = new InvoiceItem(); //Add total row
        totalItem.setInvoiceId(VIEW_TYPE_TOTAL);
        itemList.add(totalItem);

        InvoiceItem paymentItem = new InvoiceItem(); //Add Payment row
        paymentItem.setInvoiceId(VIEW_TYPE_PAYMENT);
        itemList.add(paymentItem);

        if (itemsSize > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PaymentDAO paymentDAO = InventoryDatabase.getInstance(context).paymentDAO();
                    payments = paymentDAO.getPaymentsForInvoice(String.valueOf(itemList.get(0).getInvoiceId()));

                    for (Payment payment : payments) {
                        InvoiceItem balanceItem = new InvoiceItem(); //Add Payment amount row
                        balanceItem.setInvoiceId(VIEW_TYPE_PAYMENT_AMOUNT);
                        balanceItem.setItemName(payment.getMethod().equals(PaymentMethodAnnotation.CHEQUE) ?
                                String.format(Locale.getDefault(), PaymentMethodAnnotation.CHEQUE + " %s", payment.getDetails()) : payment.getMethod());
                        balanceItem.setUnitPrice(payment.getAmount());
                        balanceItem.setQuantity(1);
                        balanceItem.setPrice(payment.getDate().getTime());
                        itemList.add(itemList.size() - 1, balanceItem);
                    }

                    if (payments.size() > 0) {
                        InvoiceItem balanceItem = new InvoiceItem(); //Add balance row
                        balanceItem.setInvoiceId(VIEW_TYPE_BALANCE);
                        itemList.add(itemList.size() - 1, balanceItem);
                    }
                    notifyDataSetChanged();
                }
            }).start();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_ITEM)
            view = LayoutInflater.from(context).inflate(R.layout.component_invoice_item_row, parent, false);
        else if (viewType == VIEW_TYPE_ADD) {
            view = LayoutInflater.from(context).inflate(R.layout.component_invoice_add_item_row, parent, false);
            return new InvoiceFooterViewHolder().new InvoiceItemAddViewHolder(view);
        } else if (viewType == VIEW_TYPE_TOTAL) {
            view = LayoutInflater.from(context).inflate(R.layout.component_invoice_total_row, parent, false);
            return new InvoiceFooterViewHolder().new InvoiceTotalViewHolder(view);
        } else if (viewType == VIEW_TYPE_PAYMENT_AMOUNT) {
            view = LayoutInflater.from(context).inflate(R.layout.component_invoice_payment_amount_row, parent, false);
            return new InvoiceFooterViewHolder().new InvoicePaymentAmountViewHolder(view);
        } else if (viewType == VIEW_TYPE_BALANCE) {
            view = LayoutInflater.from(context).inflate(R.layout.component_invoice_balance_row, parent, false);
            return new InvoiceFooterViewHolder().new InvoiceBalanceViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.component_invoice_payment_row, parent, false);
            return new InvoiceFooterViewHolder().new InvoicePaymentViewHolder(view);
        }
        return new InvoiceItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof InvoiceItemViewHolder) {
            InvoiceItemViewHolder viewHolder = (InvoiceItemViewHolder) holder;
            final InvoiceItem item = itemList.get(position);

            if (item != null) {
                viewHolder.txtName.setText(String.format(Locale.getDefault(), "#%d-%s",
                        item.getItem().getItemId(), item.getItem().getItemName()));
                double totalPrice = item.getUnitPrice() * item.getQuantity();
                viewHolder.txtPrice.setText(String.format(Locale.getDefault(), "Rs. %,.2f", totalPrice));
                viewHolder.txtUnit.setText(String.format(Locale.getDefault(), "Rs. %,.2f \t\t Qty:%d",
                        item.getUnitPrice(), item.getQuantity()));
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null)
                        listener.onItemClick(item);
                }
            });

        } else if (holder instanceof InvoiceFooterViewHolder.InvoiceItemAddViewHolder) {
            InvoiceFooterViewHolder.InvoiceItemAddViewHolder viewHolder = (InvoiceFooterViewHolder.InvoiceItemAddViewHolder) holder;

            viewHolder.btnAddItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null)
                        listener.onItemClick(null);
                }
            });

        } else if (holder instanceof InvoiceFooterViewHolder.InvoiceTotalViewHolder) {
            InvoiceFooterViewHolder.InvoiceTotalViewHolder viewHolder = (InvoiceFooterViewHolder.InvoiceTotalViewHolder) holder;

            viewHolder.txtValue.setText(String.format(Locale.getDefault(), "Rs. %,.2f", getTotal()));
            viewHolder.txtTitle.setText(context.getString(R.string.total));

        } else if (holder instanceof InvoiceFooterViewHolder.InvoicePaymentAmountViewHolder) {
            InvoiceFooterViewHolder.InvoicePaymentAmountViewHolder viewHolder = (InvoiceFooterViewHolder.InvoicePaymentAmountViewHolder) holder;
            InvoiceItem item = itemList.get(position);

            viewHolder.txtValue.setText(String.format(Locale.getDefault(), "Rs. %,.2f", item.getUnitPrice()));
            viewHolder.txtTitle.setText(item.getItemName());

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis((long) item.getPrice());
            viewHolder.txtDate.setText(DateFormat.format("dd-MM-yyyy", cal).toString());

        } else if (holder instanceof InvoiceFooterViewHolder.InvoiceBalanceViewHolder) {
            InvoiceFooterViewHolder.InvoiceBalanceViewHolder viewHolder = (InvoiceFooterViewHolder.InvoiceBalanceViewHolder) holder;

            viewHolder.txtValue.setText(String.format(Locale.getDefault(), "Rs. %,.2f", getTotal() - addTotalType(VIEW_TYPE_PAYMENT_AMOUNT)));

        } else if (holder instanceof InvoiceFooterViewHolder.InvoicePaymentViewHolder) {
            InvoiceFooterViewHolder.InvoicePaymentViewHolder viewHolder = (InvoiceFooterViewHolder.InvoicePaymentViewHolder) holder;

            viewHolder.btnPayment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null)
                        listener.onItemClick(getTotal());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (itemList == null)
            return 0;
        else
            return itemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        int typeId = itemList.get(position).getInvoiceId();
        if (typeId < 10 && typeId > 0) {
            return itemList.get(position).getInvoiceId();
        } else return VIEW_TYPE_ITEM;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    private double getTotal() {
        return addTotalType(VIEW_TYPE_ITEM);
    }

    private double addTotalType(int type) {
        double total = 0.0;

        int i = 0;
        for (InvoiceItem item : itemList) {
            if (getItemViewType(i) == type) {
                total += item.getUnitPrice() * item.getQuantity();
            }
            i++;
        }

        return total;
    }

    public void deleteItem(int position) {
        mRecentlyDeletedItem = itemList.get(position);
        mRecentlyDeletedItemPosition = position;
        itemList.remove(position);
        notifyItemRemoved(position);
        notifyItemChanged(itemList.size() - 1);
        showUndoSnackbar();
    }

    private void showUndoSnackbar() {
        View view = ((InvoiceActivity) context).findViewById(R.id.layout_invoice_parent);
        Snackbar snackbar = Snackbar.make(view, R.string.item_remove, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                undoDelete();
            }
        });
        snackbar.show();
    }

    private void undoDelete() {
        itemList.add(mRecentlyDeletedItemPosition, mRecentlyDeletedItem);
        notifyItemInserted(mRecentlyDeletedItemPosition);
        notifyItemChanged(itemList.size() - 1);
    }
}
