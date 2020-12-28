package lk.sabri.inventory.data;

import java.util.List;

public class DataBundle {

    private List<Item> items;
    private List<Customer> customers;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }
}
